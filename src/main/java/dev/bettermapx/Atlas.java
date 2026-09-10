package dev.bettermapx;

import java.io.*;
import java.nio.file.*;
import java.security.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.*;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.client.texture.*;
import net.minecraft.util.Identifier;
import net.minecraft.util.WorldSavePath;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.Heightmap;
import net.minecraft.world.chunk.*;
import net.minecraft.block.*;

/** All world access and tile mutation happens on the client thread; disk IO only uses copies. */
public final class Atlas {
    public static final int SURFACE = Integer.MAX_VALUE;
    public record Key(int x,int z,int layer,int range) { public String filename(){return x+"_"+z+"_"+layer+"_"+range+".bmx";} }
    public static final class Tile {
        public final Key key;
        public final int[] pixels, heights;
        public final String[] biomes;
        private NativeImageBackedTexture texture;
        private Identifier id;
        private boolean upload=true;
        boolean dirty;
        long nextSaveTick;
        Tile(Key k,int[] p,int[] h,String[] biomes,boolean dirty){key=k;pixels=p;heights=h;this.biomes=biomes==null?new String[256]:biomes;this.dirty=dirty;}
        Identifier texture(MinecraftClient mc){
            if(texture==null){
                texture=new NativeImageBackedTexture(new NativeImage(16,16,false));
                // Each texel is exactly one Minecraft block. Linear filtering
                // blends neighbouring blocks when the full map enlarges a tile,
                // producing the blurry result. Keep map texels crisp at every
                // zoom level and avoid mipmaps (which also soften tiny tiles).
                texture.setFilter(false,false);
                id=mc.getTextureManager().registerDynamicTexture("bettermapx",texture);
            }
            if(upload){
                NativeImage im=texture.getImage();
                for(int i=0;i<256;i++) {
                    int c=pixels[i]; // NativeImage 1.21.1 uses ABGR, GUI uses ARGB.
                    im.setColor(i&15,i>>4,TileCodec.abgr(c));
                }
                texture.upload();
                // upload() may recreate/bind the GL texture and restore the
                // default linear sampler. Apply nearest filtering afterwards;
                // otherwise the open map blends hundreds of neighbouring block
                // pixels into the blurred image seen in-game.
                texture.setFilter(false,false);
                upload=false;
            }
            return id;
        }
        void close(MinecraftClient mc){if(id!=null)mc.getTextureManager().destroyTexture(id);texture=null;id=null;}
    }
    private final MinecraftClient mc;
    private final Config config;
    private final LinkedHashMap<Key,Tile> tiles=new LinkedHashMap<>(256,.75f,true);
    private final Set<Key> requested=new HashSet<>();
    private final ThreadPoolExecutor io=new ThreadPoolExecutor(1,1,0,TimeUnit.MILLISECONDS,new ArrayBlockingQueue<>(128),r->{Thread t=new Thread(r,"BetterMapX-IO");t.setDaemon(true);return t;});
    private ClientWorld world;
    private Path folder, worldFolder;
    private long generation=0, ticks=0;
    private int scanIndex=0, playerChunkX=Integer.MIN_VALUE, playerChunkZ;
    private Work work;
    private List<int[]> offsets=List.of();
    private int radius=-1;
    private int diskRequestsThisFrame=0, uploadsThisFrame=0;
    public int selectedLayer=SURFACE;
    public String dimension="";
    public long sampledChunks=0;
    private static final class Work {
        final Key key; final int[] colors=new int[256], heights=new int[256]; final String[] biomes=new String[256]; int cursor;
        Work(Key k){key=k;Arrays.fill(heights,Integer.MIN_VALUE);}
    }
    public Atlas(MinecraftClient mc,Config config){this.mc=mc;this.config=config;}
    public Path worldFolder(){return worldFolder;}
    public int size(){return tiles.size();}
    public void beginFrame(){diskRequestsThisFrame=0;uploadsThisFrame=0;}
    public Identifier texture(Tile t){
        if((t.texture==null || t.upload) && uploadsThisFrame++>=8)return null;
        return t.texture(mc);
    }
    public Tile get(Key k){
        Tile t=tiles.get(k);
        if(t==null && folder!=null && !requested.contains(k) && diskRequestsThisFrame<8 && io.getQueue().size()<64){
            diskRequestsThisFrame++;requested.add(k);
            final Path f=folder.resolve(k.filename());final long g=generation;
            io.execute(()->{
                Tile loaded=null;
                if(Files.isRegularFile(f))try{
                    var data=TileCodec.read(f);loaded=new Tile(k,data.pixels(),data.heights(),data.biomes(),false);
                }catch(IOException e){BetterMapX.LOG.warn("Could not read map tile {}",f,e);}
                Tile result=loaded;
                mc.execute(()->{if(g==generation && result!=null && !tiles.containsKey(k)){tiles.put(k,result);evict();}});
            });
        }
        return t;
    }
    public void tick(){
        if(mc.world!=world){switchWorld();}
        if(world==null || mc.player==null)return;
        ticks++;
        // Two copies per tick at most. Unsaved tiles remain dirty when IO is busy.
        int saves=0;for(Tile t:tiles.values())if(t.dirty && ticks>=t.nextSaveTick && saves++<2)save(t);
        int r=Math.clamp(mc.options.getViewDistance().getValue(),2,8);
        int cx=mc.player.getBlockX()>>4,cz=mc.player.getBlockZ()>>4;
        if(r!=radius){radius=r;ArrayList<int[]> list=new ArrayList<>();for(int x=-r;x<=r;x++)for(int z=-r;z<=r;z++)list.add(new int[]{x,z});list.sort(Comparator.comparingInt(a->a[0]*a[0]+a[1]*a[1]));offsets=list;scanIndex=0;}
        if(cx!=playerChunkX || cz!=playerChunkZ){playerChunkX=cx;playerChunkZ=cz;scanIndex=0;work=null;}
        if(work!=null && work.key.layer!=selectedLayer)work=null;
        long deadline=System.nanoTime()+1_500_000L;
        int columns=0, attempts=0;
        while(columns<64 && System.nanoTime()<deadline){
            if(work==null){
                if(tiles.size()>=2112)return; // Backpressure if disk cannot drain dirty tiles.
                if(attempts++>=8)return;
                int[] off=offsets.get(scanIndex++%offsets.size());
                int x=cx+off[0],z=cz+off[1];
                if(world.getChunkManager().getChunk(x,z,ChunkStatus.FULL,false)==null)continue;
                work=new Work(new Key(x,z,selectedLayer,selectedLayer==SURFACE?0:config.verticalRange));
            }
            WorldChunk chunk=world.getChunkManager().getChunk(work.key.x,work.key.z,ChunkStatus.FULL,false);
            if(chunk==null){work=null;continue;}
            sample(chunk,work,work.cursor++);columns++;
            if(work.cursor==256){finish(work);work=null;}
        }
        evict();
        if(requested.size()>16384)requested.clear();
    }
    private void sample(WorldChunk chunk,Work w,int i){
        int x=(w.key.x<<4)+(i&15),z=(w.key.z<<4)+(i>>4),y;
        BlockPos.Mutable pos=new BlockPos.Mutable();
        if(w.key.layer==SURFACE){
            y=chunk.sampleHeightmap(Heightmap.Type.WORLD_SURFACE,x&15,z&15);
            // Skip transparent map colors (air/plants without a map color), with a strict ceiling.
            for(int n=0;n<64 && y>=world.getBottomY();n++,y--){pos.set(x,y,z);BlockState s=chunk.getBlockState(pos);if(!s.isAir() && s.getMapColor(world,pos).color!=0)break;}
        }else{
            y=Integer.MIN_VALUE;
            int target=w.key.layer;
            for(int d=0;d<=w.key.range;d++){
                int lo=target-d;
                if(floor(chunk,pos,x,lo,z)){y=lo;break;}
                int hi=target+d;
                if(d>0 && floor(chunk,pos,x,hi,z)){y=hi;break;}
            }
            if(y==Integer.MIN_VALUE)return;
        }
        if(y<world.getBottomY() || y>=world.getTopY())return;
        pos.set(x,y,z);BlockState state=chunk.getBlockState(pos);
        w.biomes[i]=world.getBiome(pos).getKey().map(key->key.getValue().toString()).orElse(null);
        int rgb=state.getMapColor(world,pos).color;
        if(rgb==0)return;
        w.colors[i]=0xff000000|rgb;w.heights[i]=y;
    }
    private boolean floor(WorldChunk c,BlockPos.Mutable p,int x,int y,int z){
        if(y<world.getBottomY() || y>=world.getTopY()-2)return false;
        p.set(x,y,z);BlockState s=c.getBlockState(p);
        if(s.isAir() || s.getMapColor(world,p).color==0)return false;
        p.set(x,y+1,z);if(!c.getBlockState(p).isAir())return false;
        p.set(x,y+2,z);return c.getBlockState(p).isAir();
    }
    private void finish(Work w){
        for(int i=0;i<256;i++)if(w.colors[i]!=0){
            int neighbor=i>=16 && w.heights[i-16]!=Integer.MIN_VALUE?w.heights[i-16]:w.heights[i];
            double shade=Math.clamp(1+(w.heights[i]-neighbor)*.045,.65,1.22);
            if(w.key.layer!=SURFACE)shade*=.88;
            int c=w.colors[i];int r=Math.min(255,(int)(((c>>16)&255)*shade)),g=Math.min(255,(int)(((c>>8)&255)*shade)),b=Math.min(255,(int)((c&255)*shade));
            w.colors[i]=0xff000000|r<<16|g<<8|b;
        }
        Tile old=tiles.get(w.key);
        if(old!=null && Arrays.equals(old.pixels,w.colors) && Arrays.equals(old.heights,w.heights) && Arrays.equals(old.biomes,w.biomes))return;
        if(old!=null)old.close(mc);
        Tile t=new Tile(w.key,w.colors,w.heights,w.biomes,true);tiles.put(w.key,t);sampledChunks++;save(t);
    }
    private void save(Tile t){
        if(folder==null || io.getQueue().size()>=100)return;
        final Path f=folder.resolve(t.key.filename());int[] p=t.pixels.clone(),h=t.heights.clone();String[] b=t.biomes.clone();long g=generation;
        t.dirty=false;
        io.execute(()->{try{TileCodec.write(f,p,h,b);}catch(IOException e){
            BetterMapX.LOG.error("Could not save map tile {}",f,e);
            mc.execute(()->{if(g==generation && tiles.get(t.key)==t){t.dirty=true;t.nextSaveTick=ticks+200;}});
        }});
    }
    private void evict(){
        if(tiles.size()<=2048)return;
        Iterator<Tile> it=tiles.values().iterator();
        while(tiles.size()>2048 && it.hasNext()){Tile t=it.next();if(!t.dirty){t.close(mc);requested.remove(t.key);it.remove();}}
    }
    private void switchWorld(){
        // Serialize any remaining snapshots in one task before dropping the previous world.
        if(folder!=null){
            Path oldFolder=folder;List<Tile> dirty=tiles.values().stream().filter(t->t.dirty).toList();
            if(!dirty.isEmpty()){
                Runnable flush=()->{for(Tile t:dirty)try{TileCodec.write(oldFolder.resolve(t.key.filename()),t.pixels,t.heights,t.biomes);}catch(IOException e){BetterMapX.LOG.error("Map flush failed",e);}};
                try{io.execute(flush);}catch(RejectedExecutionException e){try{io.getQueue().put(flush);}catch(InterruptedException interrupted){Thread.currentThread().interrupt();BetterMapX.LOG.error("Interrupted while queuing final map save",interrupted);}}
            }
        }
        for(Tile t:tiles.values())t.close(mc);tiles.clear();requested.clear();work=null;generation++;
        world=mc.world;folder=null;worldFolder=null;selectedLayer=SURFACE;sampledChunks=0;scanIndex=0;OverlayApi.clear();
        if(world==null){dimension="";return;}
        String identity;
        if(mc.getServer()!=null)identity="local:"+mc.getServer().getSavePath(WorldSavePath.ROOT).toAbsolutePath().normalize();
        else if(mc.getCurrentServerEntry()!=null)identity="server:"+mc.getCurrentServerEntry().address.toLowerCase(Locale.ROOT);
        else identity="session:"+UUID.randomUUID(); // Never merge unknown server identities.
        dimension=world.getRegistryKey().getValue().toString();
        worldFolder=mc.runDirectory.toPath().resolve("bettermapx").resolve(hash(identity));
        folder=worldFolder.resolve(hash(dimension));
        BetterMapX.LOG.info("Atlas attached to dimension {}",dimension);
    }
    private static String hash(String value){try{return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8))).substring(0,24);}catch(NoSuchAlgorithmException e){throw new IllegalStateException(e);}}
    public void select(int y){selectedLayer=y==SURFACE?SURFACE:Math.clamp(y,world.getBottomY(),world.getTopY()-1);work=null;scanIndex=0;}
    public void shutdown(){
        // A final bounded batch retains unscheduled dirty tiles until the worker has written them.
        if(folder!=null){
            final Path destination=folder;
            final List<Tile> pending=tiles.values().stream().filter(t->t.dirty).toList();
            Runnable flush=()->{for(Tile t:pending)try{TileCodec.write(destination.resolve(t.key.filename()),t.pixels,t.heights,t.biomes);}catch(IOException e){BetterMapX.LOG.error("Final map flush failed",e);}};
            try{io.execute(flush);}catch(RejectedExecutionException e){try{io.getQueue().put(flush);}catch(InterruptedException interrupted){Thread.currentThread().interrupt();BetterMapX.LOG.error("Interrupted while queuing final map save",interrupted);}}
        }
        io.shutdown();try{if(!io.awaitTermination(5,TimeUnit.SECONDS))BetterMapX.LOG.warn("Map IO still pending at shutdown");}catch(InterruptedException e){Thread.currentThread().interrupt();}
        for(Tile t:tiles.values())t.close(mc);
    }
}
