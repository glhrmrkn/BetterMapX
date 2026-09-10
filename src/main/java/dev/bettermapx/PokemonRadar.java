package dev.bettermapx;

import java.util.*;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.Entity;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;

/** Reads client-tracked entities on the client thread. No chunk forcing or server packets. */
final class PokemonRadar {
    private record Marker(Entity entity, String name, int level, boolean shiny, ItemStack icon, Rarity rarity, String source) {}
    private record Cached(Object pokemon, CobblemonAccess.Look look, ItemStack icon) {}
    private record Hit(Marker marker,int x,int y) {}
    private static final class VisualState {
        Marker marker;final long firstSeen,phaseSeed;long missingSince=-1,lastRenderNs;boolean present;
        double renderX,renderZ;
        VisualState(Marker marker,long now){
            this.marker=marker;this.firstSeen=now;this.lastRenderNs=now;
            this.phaseSeed=marker.entity.getUuid().getMostSignificantBits()^marker.entity.getUuid().getLeastSignificantBits();
            this.renderX=marker.entity.getX();this.renderZ=marker.entity.getZ();this.present=true;
        }
        void follow(long now){
            double tx=marker.entity.getX(),tz=marker.entity.getZ();
            double dx=tx-renderX,dz=tz-renderZ;
            double dt=Math.clamp((now-lastRenderNs)/1_000_000_000.0,0.0,.10);lastRenderNs=now;
            if(dx*dx+dz*dz>256.0){renderX=tx;renderZ=tz;return;}
            renderX=UiMotion.smooth(renderX,tx,dt,18.0);renderZ=UiMotion.smooth(renderZ,tz,dt,18.0);
        }
    }

    private final List<Hit> hits=new ArrayList<>();
    private final Map<UUID,Marker> indexed=new HashMap<>();
    private final Map<UUID,VisualState> visual=new HashMap<>();
    private final RarityCatalog catalog=new RarityCatalog();
    private UUID hovered;
    private final BetterMapX mod;
    private final CobblemonAccess access = new CobblemonAccess();
    private final Map<UUID, Cached> icons = new HashMap<>();
    private List<Marker> markers = List.of();
    private ClientWorld world;
    private int ticks;
    private boolean metadataWarning, iconWarning, renderWarning;

    PokemonRadar(BetterMapX mod) { this.mod = mod; }

    void tick(MinecraftClient mc) {
        if (mc.world != world) {
            world = mc.world;markers = List.of();indexed.clear();visual.clear();clearHover();icons.clear();ticks = 0;
        }
        if (world == null || mc.player == null || !mod.config.pokemonRadar) {
            markers = List.of();indexed.clear();visual.clear();clearHover();icons.clear();return;
        }
        if (ticks++ % 5 != 0) return;
        long now=System.nanoTime();for(VisualState state:visual.values())state.present=false;

        List<Entity> found = new ArrayList<>();
        for (Entity entity : world.getEntities()) {
            var id = Registries.ENTITY_TYPE.getId(entity.getType());
            if (entity.isAlive() && !entity.isRemoved() && RadarRules.isPokemon(id.getNamespace(), id.getPath())) found.add(entity);
        }
        found.sort(Comparator.comparingDouble(e -> e.squaredDistanceTo(mc.player)));
        List<Marker> next = new ArrayList<>();Set<UUID> retained = new HashSet<>();
        for (int i = 0; i < Math.min(256, found.size()); i++) {
            Entity entity = found.get(i);String name = entity.getDisplayName().getString();int level = 0;boolean shiny = false;
            Rarity rarity=Rarity.UNKNOWN;String source="Sem dados de raridade";ItemStack icon = ItemStack.EMPTY;
            try {
                var data = access.read(entity);level = data.level();shiny = data.shiny();
                try {
                    Object species=data.look().species();
                    String id=String.valueOf(species.getClass().getMethod("getResourceIdentifier").invoke(species));
                    Collection<?> labels=(Collection<?>)species.getClass().getMethod("getLabels").invoke(species);
                    String override=mod.config.rarityOverrides.getProperty(id);
                    rarity=Rarity.classify(shiny,labels,override);source=override!=null?"Configuracao local":"Categoria da especie";
                    if(rarity==Rarity.UNKNOWN){rarity=Rarity.classify(false,List.of(),catalog.bucket(id));source=rarity==Rarity.UNKNOWN?"Sem dados de raridade":"Catalogo local; servidor pode alterar";}
                } catch(ReflectiveOperationException | RuntimeException ignored){}
                if (mod.config.pokemonIcons && !iconWarning && !renderWarning) {
                    Cached old = icons.get(entity.getUuid());
                    if (old != null && old.pokemon == data.pokemon() && old.look.equals(data.look())) icon = old.icon;
                    else try {icon = (ItemStack) access.icon(data);if (icon == null) icon = ItemStack.EMPTY;icons.put(entity.getUuid(), new Cached(data.pokemon(), data.look(), icon));}
                    catch (ReflectiveOperationException | RuntimeException | LinkageError error) {iconWarning = true;BetterMapX.LOG.warn("Pokemon icons unavailable; using named markers", error);}
                    retained.add(entity.getUuid());
                }
            } catch (ReflectiveOperationException | RuntimeException | LinkageError error) {
                if (!metadataWarning) {metadataWarning = true;BetterMapX.LOG.warn("Pokemon details unavailable; entity names remain visible", error);}
            }
            Marker marker=new Marker(entity,name,level,shiny,icon,rarity,source);next.add(marker);
            VisualState state=visual.get(entity.getUuid());
            if(state==null){state=new VisualState(marker,now);visual.put(entity.getUuid(),state);}else{state.marker=marker;state.present=true;state.missingSince=-1;}
        }
        icons.keySet().retainAll(retained);markers = List.copyOf(next);indexed.clear();for(Marker marker:markers)indexed.put(marker.entity.getUuid(),marker);
        Iterator<VisualState> it=visual.values().iterator();
        while(it.hasNext()){
            VisualState state=it.next();
            if(state.present)continue;
            if(state.missingSince<0)state.missingSince=now;
            if(now-state.missingSince>240_000_000L)it.remove();
        }
    }

    void render(DrawContext d, int x, int y, int w, int h, double cx, double cz, double zoom, boolean full) {
        hits.clear();MinecraftClient mc = MinecraftClient.getInstance();
        if (!mod.config.pokemonRadar || mc.world != world || mc.player == null) return;
        long now=System.nanoTime();double seconds=now/1_000_000_000.0;
        List<VisualState> visible = new ArrayList<>();
        for (VisualState state : visual.values()) {
            Marker marker=state.marker;Entity e = marker.entity;state.follow(now);
            if (!RadarRules.inLayer(mod.atlas.selectedLayer, e.getY(), mod.config.verticalRange)) continue;
            int px = RadarRules.screen(state.renderX, cx, zoom, x, w), py = RadarRules.screen(state.renderZ, cz, zoom, y, h);
            if (px >= x + 5 && px < x + w - 5 && py >= y + 5 && py < y + h - 5) visible.add(state);
        }
        visible.sort(Comparator.comparingDouble(s->-s.marker.entity.squaredDistanceTo(mc.player)));
        // PASS 1: Render all glows with alpha blending
        d.draw();
        com.mojang.blaze3d.systems.RenderSystem.enableBlend();
        com.mojang.blaze3d.systems.RenderSystem.defaultBlendFunc();
        for (VisualState state : visible) {
            Marker marker = state.marker;
            int px = RadarRules.screen(state.renderX, cx, zoom, x, w);
            int py = RadarRules.screen(state.renderZ, cz, zoom, y, h);

            float appear = UiMotion.easeOutCubic((now - state.firstSeen) / 150_000_000f);
            float vanish = state.missingSince < 0 ? 1f : 1f - UiMotion.easeOutCubic((now - state.missingSince) / 180_000_000f);
            float life = Math.clamp(appear * vanish, 0f, 1f);
            mod.glow.draw(d, marker.rarity, marker.shiny, px, py, state.phaseSeed, seconds, life, mod.config);
        }
        d.draw(); // Flush all glows so they render cleanly under icons

        // PASS 2: Render all Pokémon icons on top
        for (VisualState state : visible) {
            Marker marker = state.marker;
            int px = RadarRules.screen(state.renderX, cx, zoom, x, w);
            int py = RadarRules.screen(state.renderZ, cz, zoom, y, h);
            int color = marker.rarity.color;
            if (full && state.present) hits.add(new Hit(marker, px, py));

            float appear = UiMotion.easeOutCubic((now - state.firstSeen) / 150_000_000f);
            float vanish = state.missingSince < 0 ? 1f : 1f - UiMotion.easeOutCubic((now - state.missingSince) / 180_000_000f);
            float hover = marker.entity.getUuid().equals(hovered) ? 1.04f : 1f;
            float iconScale = (.96f + .04f * appear) * (.97f + .03f * vanish) * hover;
            drawPokemonIcon(d, marker.icon, px, py, color, marker.shiny, iconScale);
        }
        d.draw(); // Flush all icons
    }

    private static void drawPokemonIcon(DrawContext d,ItemStack icon,int x,int y,int color,boolean shiny,float scale){
        if(icon!=null&&!icon.isEmpty()){
            d.getMatrices().push();d.getMatrices().translate(x,y,0);d.getMatrices().scale(scale,scale,1);d.getMatrices().translate(-8,-8,0);d.drawItem(icon,0,0);d.getMatrices().pop();
        }else{int r=Math.max(1,Math.round(2*scale));d.fill(x-r,y-r,x+r+1,y+r+1,color);}
        if(shiny){int sparkle=Rarity.SHINY_GLOW;d.fill(x+7,y-7,x+9,y-5,sparkle);d.fill(x+6,y-6,x+10,y-5,sparkle);}
    }

    void clearHover(){hovered=null;hits.clear();}
    Integer glowColor(Entity entity){
        MinecraftClient mc=MinecraftClient.getInstance();
        if(!mod.config.pokemonRadar || mod.config.glowMode==0 || mc.world!=world || entity.getWorld()!=world || !entity.isAlive() || entity.isRemoved())return null;
        Marker marker=indexed.get(entity.getUuid());if(marker==null || !RadarRules.inLayer(mod.atlas.selectedLayer,entity.getY(),mod.config.verticalRange))return null;
        boolean hover=mc.currentScreen instanceof MapScreen && entity.getUuid().equals(hovered);

        boolean espEligible=(marker.shiny && mod.config.espShiny) || marker.rarity.espEnabled(mod.config);

        if(!(hover || (mod.config.glowMode>=2 && espEligible)))return null;
        return marker.shiny?Rarity.SHINY_GLOW:marker.rarity.color;
    }

    boolean hover(DrawContext d,int mx,int my,boolean inside){
        hovered=null;if(!inside)return false;Hit closest=null;double best=100;
        for(Hit hit:hits){double distance=(mx-hit.x)*(mx-hit.x)+(my-hit.y)*(my-hit.y);if(distance<=best){best=distance;closest=hit;}}
        if(closest==null)return false;Marker marker=closest.marker;hovered=marker.entity.getUuid();MinecraftClient mc=MinecraftClient.getInstance();
        drawStitchHoverCard(d,mc,marker,mx,my);
        return true;
    }

    private void drawStitchHoverCard(DrawContext d,MinecraftClient mc,Marker marker,int mx,int my){
        var tr=mc.textRenderer;
        int cardW=175;
        int cardH=78;
        int sw=mc.getWindow().getScaledWidth(),sh=mc.getWindow().getScaledHeight();
        // Keep card strictly within the screen boundary
        int cardX=Math.clamp(mx+12,4,Math.max(4,sw-cardW-4));
        int cardY=Math.clamp(my-12,4,Math.max(4,sh-cardH-4));

        // Stitch Bevel Panel container
        StitchTheme.drawBevelPanel(d,cardX,cardY,cardW,cardH,0xf20f131b,StitchTheme.BORDER_OUTER);

        // Header: Pokemon Name
        String name=tr.trimToWidth(marker.name,cardW-(marker.shiny?64:16));
        d.drawText(tr,name,cardX+7,cardY+7,StitchTheme.TEXT_WHITE,true);

        // Shiny Chip Badge if applicable
        if(marker.shiny){
            StitchTheme.drawChip(d,tr,cardX+cardW-54,cardY+5,"★ SHINY",StitchTheme.SHINY_GOLD,0x33f59e0b,0x88f59e0b);
        }

        // Header divider line
        d.fill(cardX+5,cardY+21,cardX+cardW-5,cardY+22,0xff2d3440);

        // Key-Value Rows
        int rowY=cardY+25;
        int rowSpacing=12;

        // Row 1: Level
        drawRow(d,tr,cardX+7,rowY,cardW-14,"Lvl:",marker.level>0?String.valueOf(marker.level):"--",StitchTheme.TEXT_WHITE);
        // Row 2: Rarity
        drawRow(d,tr,cardX+7,rowY+rowSpacing,cardW-14,"Raridade:",marker.rarity.label,marker.rarity.color);
        // Row 3: Distance
        int dist=(int)Math.round(marker.entity.distanceTo(mc.player));
        drawRow(d,tr,cardX+7,rowY+rowSpacing*2,cardW-14,"Distancia:",dist+" m",StitchTheme.EMERALD_GREEN);
        // Row 4: Y Altitude
        drawRow(d,tr,cardX+7,rowY+rowSpacing*3,cardW-14,"Altitude (Y):",String.valueOf(marker.entity.getBlockY()),StitchTheme.TEXT_CYAN);
    }

    private static void drawRow(DrawContext d,net.minecraft.client.font.TextRenderer tr,int x,int y,int width,String key,String value,int valueColor){
        d.drawText(tr,key,x,y,StitchTheme.TEXT_MUTED,false);
        int valW=tr.getWidth(value);
        d.drawText(tr,value,x+width-valW,y,valueColor,false);
    }
}
