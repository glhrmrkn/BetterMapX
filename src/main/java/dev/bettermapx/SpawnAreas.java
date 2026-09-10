package dev.bettermapx;

import com.google.gson.*;
import java.io.*;
import java.util.*;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.resource.Resource;
import net.minecraft.resource.ResourceManager;
import net.minecraft.util.Identifier;
import net.minecraft.world.biome.Biome;

/**
 * Static spawn geography overlay for active Cobblemon datapacks.
 *
 * It deliberately evaluates only information that can be represented reliably on a
 * cached world map: dimension, biome and Y range. Time, weather, light, structures,
 * nearby blocks and other live conditions remain server-side/dynamic and are not
 * guessed. The result is therefore a spawn-eligible area, not a promise of an
 * immediate spawn at the current tick.
 */
final class SpawnAreas {
    private static final int FILL=0x78c34dff;
    private static final int BORDER=0xb8f14968;
    private record StaticCondition(List<String> biomes,List<String> dimensions,Integer minY,Integer maxY,boolean hasStatic) {}
    private record Rule(List<StaticCondition> positive,List<StaticCondition> negative) {}
    private static final class Mask {
        final Atlas.Tile source;final Atlas.Tile[] neighbors;final Identifier id;
        Mask(Atlas.Tile source,Atlas.Tile[] neighbors,Identifier id){this.source=source;this.neighbors=neighbors;this.id=id;}
    }

    private final BetterMapX mod;
    private final MinecraftClient mc=MinecraftClient.getInstance();
    private final Map<Atlas.Key,Mask> masks=new HashMap<>();
    private final Set<Identifier> owned=new HashSet<>();
    private List<Rule> rules=List.of();
    private List<String> species=List.of();
    private Object worldIdentity;
    private String loadedPokemon="";
    private int ticks;
    private boolean resourcesAvailable;

    SpawnAreas(BetterMapX mod){this.mod=mod;}

    void tick(){
        if(mc.world!=worldIdentity){worldIdentity=mc.world;loadedPokemon="";rules=List.of();species=List.of();resourcesAvailable=false;clearMasks();ticks=0;}
        if(mc.world==null)return;
        String selected=normalize(mod.config.spawnAreaPokemon);
        if(!selected.equals(loadedPokemon)){
            loadedPokemon=selected;clearMasks();reloadRules();
        } else if(!resourcesAvailable && ticks++%100==0) reloadRules();
    }

    List<String> species(){
        if(species.isEmpty() && mc.world!=null)reloadIndex();
        return species;
    }
    String selected(){return loadedPokemon;}
    int ruleCount(){return rules.size();}
    boolean resourcesAvailable(){return resourcesAvailable;}

    void select(String pokemon){
        String normalized=normalize(pokemon);
        mod.config.spawnAreaPokemon=normalized;
        mod.config.save();
        if(!normalized.equals(loadedPokemon)){loadedPokemon=normalized;clearMasks();reloadRules();}
    }

    void render(DrawContext d,int x,int y,int w,int h,double centerX,double centerZ,double zoom,boolean full){
        Config c=mod.config;
        if(!c.spawnAreas || loadedPokemon.isBlank() || rules.isEmpty())return;
        if(full?!c.spawnAreasFullMap:!c.spawnAreasMinimap)return;
        if(mod.atlas.selectedLayer!=Atlas.SURFACE)return;
        double left=centerX-w/(2.0*zoom),top=centerZ-h/(2.0*zoom);
        int minX=(int)Math.floor(left/16),minZ=(int)Math.floor(top/16);
        int maxX=(int)Math.floor((left+w/zoom)/16),maxZ=(int)Math.floor((top+h/zoom)/16);
        for(int cz=minZ;cz<=maxZ;cz++)for(int cx=minX;cx<=maxX;cx++){
            Atlas.Key key=new Atlas.Key(cx,cz,Atlas.SURFACE,0);
            Atlas.Tile tile=mod.atlas.get(key);if(tile==null)continue;
            Identifier id=mask(key,tile);if(id==null)continue;
            int sx=x+(int)Math.floor((cx*16.0-left)*zoom),sz=y+(int)Math.floor((cz*16.0-top)*zoom);
            int ex=x+(int)Math.floor(((cx+1)*16.0-left)*zoom),ez=y+(int)Math.floor(((cz+1)*16.0-top)*zoom);
            d.drawTexture(id,sx,sz,ex-sx,ez-sz,0f,0f,16,16,16,16);
        }
    }

    private Identifier mask(Atlas.Key key,Atlas.Tile tile){
        Atlas.Tile[] neighbors=neighborTiles(key);
        Mask old=masks.get(key);if(old!=null && old.source==tile && Arrays.equals(old.neighbors,neighbors))return old.id;
        if(old!=null){mc.getTextureManager().destroyTexture(old.id);owned.remove(old.id);masks.remove(key);}
        boolean[] valid=new boolean[256];boolean any=false;
        for(int i=0;i<256;i++){
            String biome=tile.biomes[i];int height=tile.heights[i];
            if(biome==null || height==Integer.MIN_VALUE)continue;
            valid[i]=valid(biome,height+1);any|=valid[i];
        }
        if(!any)return null;
        NativeImage image=new NativeImage(16,16,false);
        for(int i=0;i<256;i++){
            if(!valid[i]){image.setColor(i&15,i>>4,0);continue;}
            int lx=i&15,lz=i>>4;
            boolean edge=(lx>0?!valid[i-1]:!validAt(key.x()*16-1,key.z()*16+lz))
                    ||(lx<15?!valid[i+1]:!validAt(key.x()*16+16,key.z()*16+lz))
                    ||(lz>0?!valid[i-16]:!validAt(key.x()*16+lx,key.z()*16-1))
                    ||(lz<15?!valid[i+16]:!validAt(key.x()*16+lx,key.z()*16+16));
            image.setColor(lx,lz,TileCodec.abgr(edge?BORDER:FILL));
        }
        NativeImageBackedTexture texture=new NativeImageBackedTexture(image);texture.setFilter(false,false);
        Identifier id=mc.getTextureManager().registerDynamicTexture("bettermapx_spawn_"+key.x()+"_"+key.z(),texture);
        texture.upload();texture.setFilter(false,false);
        masks.put(key,new Mask(tile,neighbors,id));owned.add(id);return id;
    }

    private Atlas.Tile[] neighborTiles(Atlas.Key key){
        Atlas.Tile[] out=new Atlas.Tile[9];int n=0;
        for(int dz=-1;dz<=1;dz++)for(int dx=-1;dx<=1;dx++)out[n++]=mod.atlas.get(new Atlas.Key(key.x()+dx,key.z()+dz,Atlas.SURFACE,0));
        return out;
    }
    private boolean validAt(int worldX,int worldZ){
        Atlas.Tile tile=mod.atlas.get(new Atlas.Key(Math.floorDiv(worldX,16),Math.floorDiv(worldZ,16),Atlas.SURFACE,0));
        if(tile==null)return false;int i=(Math.floorMod(worldZ,16)<<4)|Math.floorMod(worldX,16);
        String biome=tile.biomes[i];int height=tile.heights[i];
        return biome!=null && height!=Integer.MIN_VALUE && valid(biome,height+1);
    }

    private boolean valid(String biomeId,int y){
        String dimension=mod.atlas.dimension;
        for(Rule rule:rules){
            boolean positive=rule.positive.isEmpty();
            for(StaticCondition condition:rule.positive)if(matches(condition,biomeId,dimension,y)){positive=true;break;}
            if(!positive)continue;
            boolean blocked=false;
            for(StaticCondition anti:rule.negative)if(anti.hasStatic && matches(anti,biomeId,dimension,y)){blocked=true;break;}
            if(!blocked)return true;
        }
        return false;
    }

    private boolean matches(StaticCondition c,String biomeId,String dimension,int y){
        if(c.minY!=null && y<c.minY)return false;if(c.maxY!=null && y>c.maxY)return false;
        if(!c.dimensions.isEmpty() && c.dimensions.stream().noneMatch(d->d.equals(dimension)))return false;
        if(c.biomes.isEmpty())return true;
        if(mc.world==null)return false;
        Registry<Biome> registry=mc.world.getRegistryManager().get(RegistryKeys.BIOME);
        Identifier biomeIdentifier=parseId(biomeId);if(biomeIdentifier==null)return false;
        Optional<RegistryEntry.Reference<Biome>> entry=registry.getEntry(biomeIdentifier);if(entry.isEmpty())return false;
        for(String requirement:c.biomes){
            if(requirement.startsWith("#")){
                Identifier tagId=parseId(requirement.substring(1));
                if(tagId!=null && entry.get().isIn(TagKey.of(RegistryKeys.BIOME,tagId)))return true;
            }else{
                Identifier id=parseId(requirement);
                if(id!=null && biomeIdentifier.equals(id))return true;
            }
        }
        return false;
    }

    private void reloadIndex(){
        Map<Identifier,Resource> resources=resources();
        TreeSet<String> found=new TreeSet<>();
        // Index the actual spawn entries instead of trusting file names. Addon datapacks
        // frequently group many species in a single arbitrarily named JSON file.
        for(var e:resources.entrySet())try(Reader reader=e.getValue().getReader()){
            JsonElement rootElement=JsonParser.parseReader(reader);if(!rootElement.isJsonObject())continue;
            JsonObject root=rootElement.getAsJsonObject();if(root.has("enabled") && !asBoolean(root.get("enabled"),true))continue;
            JsonArray spawns=array(root,"spawns");if(spawns==null)continue;
            for(JsonElement element:spawns){
                if(!element.isJsonObject())continue;JsonObject spawn=element.getAsJsonObject();
                if(!spawn.has("pokemon") || !spawn.get("pokemon").isJsonPrimitive())continue;
                String pokemon=normalize(spawn.get("pokemon").getAsString());if(!pokemon.isBlank())found.add(pokemon);
            }
        }catch(Exception ex){BetterMapX.LOG.debug("Could not index spawn resource {}",e.getKey(),ex);}
        species=List.copyOf(found);resourcesAvailable=!resources.isEmpty();
    }

    private void reloadRules(){
        if(loadedPokemon.isBlank()){rules=List.of();return;}
        Map<Identifier,Resource> resources=resources();resourcesAvailable=!resources.isEmpty();
        if(species.isEmpty())reloadIndex();
        List<Rule> found=parseMatching(resources,true);
        // Addon packs are allowed to group multiple species in arbitrarily named files.
        // Only fall back to scanning everything when the normal numbered species file
        // did not produce a rule, keeping the common path fast.
        if(found.isEmpty())found=parseMatching(resources,false);
        rules=List.copyOf(found);clearMasks();
        BetterMapX.LOG.info("Spawn Areas loaded {} static rules for {}",rules.size(),loadedPokemon);
    }

    private List<Rule> parseMatching(Map<Identifier,Resource> resources,boolean filenameFilter){
        List<Rule> found=new ArrayList<>();
        for(var e:resources.entrySet()){
            String path=e.getKey().getPath().toLowerCase(Locale.ROOT);
            if(filenameFilter && !path.contains(loadedPokemon))continue;
            try(Reader reader=e.getValue().getReader()){
                JsonElement rootElement=JsonParser.parseReader(reader);if(!rootElement.isJsonObject())continue;
                JsonObject root=rootElement.getAsJsonObject();if(root.has("enabled") && !asBoolean(root.get("enabled"),true))continue;
                JsonArray spawns=array(root,"spawns");if(spawns==null)continue;
                for(JsonElement element:spawns){
                    if(!element.isJsonObject())continue;JsonObject spawn=element.getAsJsonObject();
                    String pokemon=spawn.has("pokemon")?normalize(spawn.get("pokemon").getAsString()):"";
                    if(!pokemon.equals(loadedPokemon))continue;
                    List<StaticCondition> positives=conditions(spawn,"condition","conditions");
                    List<StaticCondition> negatives=conditions(spawn,"anticondition","anticonditions");
                    found.add(new Rule(positives,negatives));
                }
            }catch(Exception ex){BetterMapX.LOG.debug("Could not inspect spawn resource {}",e.getKey(),ex);}
        }
        return found;
    }

    private Map<Identifier,Resource> resources(){
        ResourceManager manager=null;
        if(mc.getServer()!=null)manager=mc.getServer().getResourceManager();
        else manager=mc.getResourceManager();
        try{return manager.findResources("spawn_pool_world",id->id.getPath().endsWith(".json"));}
        catch(RuntimeException e){BetterMapX.LOG.debug("Spawn Areas resource scan unavailable",e);return Map.of();}
    }

    private static List<StaticCondition> conditions(JsonObject spawn,String singular,String plural){
        List<StaticCondition> out=new ArrayList<>();
        if(spawn.has(singular) && spawn.get(singular).isJsonObject())out.add(condition(spawn.getAsJsonObject(singular)));
        if(spawn.has(plural) && spawn.get(plural).isJsonArray())for(JsonElement e:spawn.getAsJsonArray(plural))if(e.isJsonObject())out.add(condition(e.getAsJsonObject()));
        return List.copyOf(out);
    }
    private static StaticCondition condition(JsonObject o){
        List<String> biomes=strings(o,"biomes"),dimensions=strings(o,"dimensions");
        Integer minY=integer(o,"minY"),maxY=integer(o,"maxY");
        return new StaticCondition(biomes,dimensions,minY,maxY,!biomes.isEmpty()||!dimensions.isEmpty()||minY!=null||maxY!=null);
    }
    private static List<String> strings(JsonObject o,String key){
        if(!o.has(key))return List.of();JsonElement value=o.get(key);ArrayList<String> out=new ArrayList<>();
        if(value.isJsonArray()){
            for(JsonElement e:value.getAsJsonArray())if(e.isJsonPrimitive())out.add(e.getAsString());
        }else if(value.isJsonPrimitive())out.add(value.getAsString());
        return List.copyOf(out);
    }
    private static Integer integer(JsonObject o,String key){try{return o.has(key)?o.get(key).getAsInt():null;}catch(RuntimeException e){return null;}}
    private static JsonArray array(JsonObject o,String key){return o.has(key)&&o.get(key).isJsonArray()?o.getAsJsonArray(key):null;}
    private static boolean asBoolean(JsonElement e,boolean d){try{return e.getAsBoolean();}catch(RuntimeException ex){return d;}}
    private static Identifier parseId(String value){try{return Identifier.of(value);}catch(RuntimeException e){return null;}}
    static String normalize(String pokemon){
        if(pokemon==null)return "";String s=pokemon.trim().toLowerCase(Locale.ROOT);int space=s.indexOf(' ');if(space>=0)s=s.substring(0,space);int colon=s.indexOf(':');if(colon>=0)s=s.substring(colon+1);return s.replace(' ','_');
    }
    static String display(String id){if(id==null||id.isBlank())return "Nenhum";String[] parts=id.replace('_',' ').split(" ");StringBuilder b=new StringBuilder();for(String p:parts){if(p.isBlank())continue;if(!b.isEmpty())b.append(' ');b.append(Character.toUpperCase(p.charAt(0))).append(p.substring(1));}return b.toString();}

    private void clearMasks(){for(Identifier id:owned)mc.getTextureManager().destroyTexture(id);owned.clear();masks.clear();}
    void close(){clearMasks();}
}
