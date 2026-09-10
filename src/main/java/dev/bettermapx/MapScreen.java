package dev.bettermapx;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;

public final class MapScreen extends Screen {
    private final BetterMapX mod;
    private final boolean expanded;
    private double centerX,centerZ,targetCenterX,targetCenterZ,zoom,targetZoom;
    private long lastFrame=System.nanoTime();
    private final long openedAt=System.nanoTime();
    private long closingAt=-1;
    private int mapX=14,mapY=42,mapW,mapH;

    // Context Menu State (Stitch Tactical Right-Click Menu)
    private boolean contextOpen;
    private int contextScreenX,contextScreenY;
    private int contextWorldX,contextWorldY,contextWorldZ;
    private Waypoints.Point contextWaypoint;
    private String feedbackText;
    private long feedbackUntil;

    // Toolbar Buttons
    private SilverButton btnPokemon;
    private SilverButton btnSpawnAreas;
    private SilverButton btnLayer;

    public MapScreen(BetterMapX mod){this(mod,false);}
    public MapScreen(BetterMapX mod,boolean expanded){
        super(Text.literal("BetterMapX"));
        this.mod=mod;
        this.expanded=expanded;
        zoom=targetZoom=expanded?mod.config.worldMapZoom:Math.max(1.0,mod.config.worldMapZoom);
        var p=net.minecraft.client.MinecraftClient.getInstance().player;
        if(p!=null){centerX=targetCenterX=p.getX();centerZ=targetCenterZ=p.getZ();}
    }

    @Override protected void init(){
        client.mouse.unlockCursor();
        contextOpen=false;
        mapX=14;
        mapY=38;
        mapW=Math.max(50,width-28);
        mapH=Math.max(40,height-78);

        // Bottom-Center Floating Navigation Toolbar
        int tbY=height-32;
        int tbCenterX=width/2;
        int btnH=20;

        // [+] Zoom In & [-] Zoom Out
        addDrawableChild(SilverButton.make(Text.literal("+"),b->targetZoom=Math.clamp(targetZoom*1.25,0.10,8.0))
                .dimensions(tbCenterX-145,tbY,20,btnH).build());
        addDrawableChild(SilverButton.make(Text.literal("−"),b->targetZoom=Math.clamp(targetZoom/1.25,0.10,8.0))
                .dimensions(tbCenterX-122,tbY,20,btnH).build());

        // Center on Player
        addDrawableChild(SilverButton.make(Text.literal("⌖"),b->{
            if(client.player!=null){targetCenterX=client.player.getX();targetCenterZ=client.player.getZ();}
        }).dimensions(tbCenterX-99,tbY,20,btnH).build());

        // Camadas / Altura Layer Stepper
        String layerLabel=mod.atlas.selectedLayer==Atlas.SURFACE?"Superfície":"Y "+mod.atlas.selectedLayer;
        btnLayer=SilverButton.make(Text.literal(layerLabel),b->{
            if(mod.atlas.selectedLayer==Atlas.SURFACE && client.player!=null){
                mod.atlas.select(client.player.getBlockY()-1);
            }else{
                mod.atlas.select(Atlas.SURFACE);
            }
            updateButtons();
        }).dimensions(tbCenterX-74,tbY,66,btnH).build();
        addDrawableChild(btnLayer);
        // Page the viewport by 70% in each direction, in addition to drag pan.
        addDrawableChild(SilverButton.make(Text.literal("←"),b->targetCenterX-=mapW*.7/targetZoom).dimensions(tbCenterX-74,tbY-22,20,20).build());
        addDrawableChild(SilverButton.make(Text.literal("→"),b->targetCenterX+=mapW*.7/targetZoom).dimensions(tbCenterX-30,tbY-22,20,20).build());
        addDrawableChild(SilverButton.make(Text.literal("↑"),b->targetCenterZ-=mapH*.7/targetZoom).dimensions(tbCenterX-52,tbY-44,20,20).build());
        addDrawableChild(SilverButton.make(Text.literal("↓"),b->targetCenterZ+=mapH*.7/targetZoom).dimensions(tbCenterX-52,tbY-22,20,20).build());

        // Pokémon Radar Toggle
        btnPokemon=SilverButton.make(Text.literal("🔴 Pokémon"),b->{
            mod.config.pokemonRadar=!mod.config.pokemonRadar;
            mod.config.save();
            updateButtons();
        }).dimensions(tbCenterX-5,tbY,66,btnH).selected(mod.config.pokemonRadar).build();
        addDrawableChild(btnPokemon);

        // Spawn Areas Toggle / Access
        btnSpawnAreas=SilverButton.make(Text.literal("🟩 Spawns"),b->{
            mod.config.spawnAreas=!mod.config.spawnAreas;
            mod.config.save();
            updateButtons();
        }).dimensions(tbCenterX+64,tbY,62,btnH).selected(mod.config.spawnAreas).build();
        addDrawableChild(btnSpawnAreas);

        // Waypoints Button
        addDrawableChild(SilverButton.make(Text.literal("📍 Pts"),b->{
            if(client.player!=null){
                client.setScreen(new WaypointScreen(mod,this,null,client.player.getBlockX(),client.player.getBlockY(),client.player.getBlockZ()));
            }
        }).dimensions(tbCenterX+129,tbY,42,btnH).build());

        // Bottom-Right: Settings Cog
        addDrawableChild(SilverButton.make(Text.literal("⚙"),b->client.setScreen(new SettingsScreen(mod,this)))
                .dimensions(width-mapX-22,tbY,22,btnH).build());

        updateButtons();
    }

    private void updateButtons(){
        if(btnPokemon!=null) btnPokemon.setSelected(mod.config.pokemonRadar);
        if(btnSpawnAreas!=null) btnSpawnAreas.setSelected(mod.config.spawnAreas);
        if(btnLayer!=null){
            String l=mod.atlas.selectedLayer==Atlas.SURFACE?"Superfície":"Y "+mod.atlas.selectedLayer;
            btnLayer.setMessage(Text.literal(l));
        }
    }

    @Override public void renderBackground(DrawContext d,int mouseX,int mouseY,float delta){
        // Intentionally empty: preserves the map sharpness and avoids vanilla blur
    }

    @Override public void render(DrawContext d,int mx,int my,float delta){
        double dt=Math.min(.1,(System.nanoTime()-lastFrame)/1e9);lastFrame=System.nanoTime();
        zoom=UiMotion.smooth(zoom,targetZoom,dt,16);
        centerX=UiMotion.smooth(centerX,targetCenterX,dt,22);
        centerZ=UiMotion.smooth(centerZ,targetCenterZ,dt,22);
        float motion=expanded?transition():1f;

        // Dark ambient background
        d.fill(0,0,width,height,UiMotion.alpha(0xff090c12,.92f));

        // 1. Outer Stitch Bevel Panel enclosing the map viewport
        StitchTheme.drawBevelPanel(d,mapX-3,mapY-3,mapW+6,mapH+6,0xf4121620,StitchTheme.BORDER_OUTER);

        // 2. Render Map Surface
        mod.renderer.render(d,mapX,mapY,mapW,mapH,centerX,centerZ,zoom,true);
        if(expanded && motion<.999f)d.fill(mapX,mapY,mapX+mapW,mapY+mapH,UiMotion.alpha(0xff071018,1f-motion));

        // 3. Top-Left Floating HUD Header: Pokéball logo + BetterMapX
        StitchTheme.drawBevelPanel(d,mapX,10,144,24);
        StitchTheme.drawProceduralPokeball(d,mapX+5,14,16);
        d.drawText(textRenderer,"BetterMapX",mapX+26,14,StitchTheme.TEXT_WHITE,true);
        d.drawText(textRenderer,expanded?"Expanded Atlas":"World Map",mapX+26,22,StitchTheme.TEXT_MUTED,false);

        // Dimension / Layer status chip on top right of header
        String dimensionTag=mod.atlas.dimension;
        if(dimensionTag.contains(":")) dimensionTag=dimensionTag.substring(dimensionTag.indexOf(':')+1).toUpperCase();
        int dimW=textRenderer.getWidth(dimensionTag);
        StitchTheme.drawChip(d,textRenderer,mapX+mapW-dimW-12,12,dimensionTag,StitchTheme.VIOLET_ARCANE,0x338b5cf6,0x668b5cf6);

        // 4. Bottom-Left Telemetry Box
        int telemW=175;
        StitchTheme.drawBevelPanel(d,mapX,height-32,telemW,20);
        int curX=worldX(mx),curZ=worldZ(my);
        String coords="X: "+curX+"  Z: "+curZ+"  ("+String.format(java.util.Locale.ROOT,"%.1fx",zoom)+")";
        d.drawText(textRenderer,coords,mapX+6,height-26,StitchTheme.TEXT_CYAN,false);

        // 5. Elevation controls info (PgUp/PgDn)
        int elevX=width-mapX-112;
        StitchTheme.drawBevelPanel(d,elevX,height-32,86,20);
        d.drawText(textRenderer,"PgUp/Dn Y",elevX+6,height-26,StitchTheme.TEXT_MUTED,false);

        // Super widgets (Buttons)
        super.render(d,mx,my,delta);

        // 6. Hover Pokémon Inspect Card (if not opening context menu)
        if(!contextOpen){
            if(!mod.radar.hover(d,mx,my,inside(mx,my)) && inside(mx,my)){
                for(var point:mod.waypoints.all()){
                    if(!point.dimension().equals(mod.atlas.dimension) || !RadarRules.inLayer(mod.atlas.selectedLayer,point.y(),mod.config.verticalRange))continue;
                    int px=RadarRules.screen(point.x(),centerX,zoom,mapX,mapW),py=RadarRules.screen(point.z(),centerZ,zoom,mapY,mapH);
                    if(Math.hypot(mx-px,my-py)>9)continue;
                    d.drawTooltip(textRenderer,java.util.List.of(Text.literal(point.name()),Text.literal(point.x()+" / "+point.y()+" / "+point.z()),Text.literal(point.id().startsWith("xaero:")?"Clique direito: copiar para BetterMapX":"Clique direito: opções")),mx,my);break;
                }
            }
        }

        // 7. Tactical Context Menu (rendered on top)
        if(contextOpen){
            drawContextMenu(d,mx,my);
        }

        // 8. Temporary Feedback Toast (e.g. "Coordenadas copiadas!")
        if(feedbackText!=null && System.nanoTime()<feedbackUntil){
            int fbW=textRenderer.getWidth(feedbackText)+16;
            int fbX=width/2-fbW/2,fbY=height/2-10;
            StitchTheme.drawBevelPanel(d,fbX,fbY,fbW,20,0xf418221c,StitchTheme.EMERALD_ACCENT);
            d.drawCenteredTextWithShadow(textRenderer,feedbackText,width/2,fbY+6,StitchTheme.TEXT_WHITE);
        }
    }

    private void drawContextMenu(DrawContext d,int mx,int my){
        int x=contextScreenX,y=contextScreenY;
        int w=186,h=76;

        // Outer Shadow
        d.fill(x+3,y+h,x+w,y+h+4,0x77000000);
        d.fill(x+w,y+3,x+w+4,y+h,0x77000000);

        // Tactical Bevel Frame
        StitchTheme.drawBevelPanel(d,x,y,w,h,0xf8121620,StitchTheme.BORDER_OUTER);

        // Header: Coordinates + ESC
        String pos="POS: "+contextWorldX+", "+contextWorldY+", "+contextWorldZ;
        pos=textRenderer.trimToWidth(pos,w-36);
        d.drawText(textRenderer,pos,x+7,y+6,StitchTheme.TEXT_MUTED,false);
        d.drawText(textRenderer,"ESC",x+w-24,y+6,StitchTheme.TEXT_DARK,false);

        // Divider
        d.fill(x+4,y+17,x+w-4,y+18,0xff2d3440);

        // Item 1: ⚡ Teleportar Aqui (/tp)
        int y1=y+20;
        boolean h1=mx>=x+2 && mx<x+w-2 && my>=y1 && my<y1+17;
        if(h1){
            d.fill(x+2,y1,x+w-2,y1+17,0x4406b6d4);
            d.drawBorder(x+2,y1,w-4,17,StitchTheme.CYAN_ACCENT);
        }
        d.drawText(textRenderer,"⚡ Teleportar Aqui (/tp)",x+7,y1+5,h1?StitchTheme.CYAN_PRIMARY:StitchTheme.TEXT_ON_SURFACE,false);

        // Item 2: 🚩 Waypoint
        int y2=y+38;
        boolean h2=mx>=x+2 && mx<x+w-2 && my>=y2 && my<y2+17;
        if(h2){
            d.fill(x+2,y2,x+w-2,y2+17,0x33f59e0b);
            d.drawBorder(x+2,y2,w-4,17,StitchTheme.AMBER_GOLD);
        }
        String wpLabel=contextWaypoint!=null?"🚩 Editar Waypoint":"🚩 Adicionar Waypoint";
        d.drawText(textRenderer,wpLabel,x+7,y2+5,h2?StitchTheme.SHINY_GOLD:StitchTheme.TEXT_ON_SURFACE,false);

        // Item 3: 📋 Copiar Coordenadas
        int y3=y+56;
        boolean h3=mx>=x+2 && mx<x+w-2 && my>=y3 && my<y3+17;
        if(h3){
            d.fill(x+2,y3,x+w-2,y3+17,0x3364748b);
            d.drawBorder(x+2,y3,w-4,17,StitchTheme.BORDER_HIGHLIGHT);
        }
        d.drawText(textRenderer,"📋 Copiar Coordenadas",x+7,y3+5,h3?StitchTheme.TEXT_WHITE:StitchTheme.TEXT_MUTED,false);
    }

    private boolean inside(double x,double y){return x>=mapX && x<mapX+mapW && y>=mapY && y<mapY+mapH;}
    private int worldX(double x){return (int)Math.floor(centerX+(x-mapX-mapW/2.0)/zoom);}
    private int worldZ(double y){return (int)Math.floor(centerZ+(y-mapY-mapH/2.0)/zoom);}

    @Override public boolean mouseDragged(double x,double y,int button,double dx,double dy){
        if(contextOpen)return false;
        if(button==0 && inside(x,y)){targetCenterX-=dx/zoom;targetCenterZ-=dy/zoom;return true;}
        return super.mouseDragged(x,y,button,dx,dy);
    }

    @Override public boolean mouseScrolled(double x,double y,double horizontal,double vertical){
        if(inside(x,y)){targetZoom=Math.clamp(targetZoom*Math.pow(1.25,vertical),.10,8);return true;}
        return super.mouseScrolled(x,y,horizontal,vertical);
    }

    @Override public boolean mouseClicked(double x,double y,int button){
        if(contextOpen){
            int cx=contextScreenX,cy=contextScreenY,cw=186;
            if(x>=cx && x<cx+cw){
                if(y>=cy+20 && y<cy+37){
                    // Teleportar Aqui (/tp)
                    if(client.player!=null){
                        client.player.networkHandler.sendChatCommand("teleport @s "+(contextWorldX+.5)+" "+contextWorldY+" "+(contextWorldZ+.5));
                        contextOpen=false;
                        close();
                        return true;
                    }
                }else if(y>=cy+38 && y<cy+55){
                    // Adicionar / Editar Waypoint
                    client.setScreen(new WaypointScreen(mod,this,contextWaypoint,contextWorldX,contextWorldY,contextWorldZ));
                    contextOpen=false;
                    return true;
                }else if(y>=cy+56 && y<cy+73){
                    // Copiar Coordenadas
                    client.keyboard.setClipboard(contextWorldX+" "+contextWorldY+" "+contextWorldZ);
                    feedbackText="Coordenadas copiadas!";
                    feedbackUntil=System.nanoTime()+2_000_000_000L;
                    contextOpen=false;
                    return true;
                }
            }
            if(button!=1){
                contextOpen=false;
                return true;
            }
        }

        if(button==1 && inside(x,y)){
            int wx=worldX(x),wz=worldZ(y);
            Waypoints.Point nearest=null;double best=100;
            for(var p:mod.waypoints.all())if(p.dimension().equals(mod.atlas.dimension)){
                if(mod.atlas.selectedLayer!=Atlas.SURFACE && Math.abs(p.y()-mod.atlas.selectedLayer)>mod.config.verticalRange)continue;
                double distance=Math.hypot((p.x()-wx)*zoom,(p.z()-wz)*zoom);if(distance<best && distance<10){nearest=p;best=distance;}
            }
            int wy=client.player!=null?client.player.getBlockY():64;
            Atlas.Tile tile=mod.atlas.get(new Atlas.Key(wx>>4,wz>>4,mod.atlas.selectedLayer,mod.atlas.selectedLayer==Atlas.SURFACE?0:mod.config.verticalRange));
            if(tile!=null){int h=tile.heights[(Math.floorMod(wz,16)<<4)|Math.floorMod(wx,16)];if(h!=Integer.MIN_VALUE)wy=h+1;}

            // Open Stitch Tactical Context Menu
            contextOpen=true;
            contextWorldX=wx;contextWorldY=wy;contextWorldZ=wz;
            contextWaypoint=nearest;
            contextScreenX=Math.clamp((int)x,4,Math.max(4,width-190));
            contextScreenY=Math.clamp((int)y,4,Math.max(4,height-80));
            return true;
        }

        return super.mouseClicked(x,y,button);
    }

    @Override public boolean keyPressed(int key,int scan,int modifiers){
        if(contextOpen && (key==GLFW.GLFW_KEY_ESCAPE)){
            contextOpen=false;
            return true;
        }
        if(mod.map.matchesKey(key,scan)){if(expanded)switchView(false);else close();return true;}
        if(key==GLFW.GLFW_KEY_PAGE_UP || key==GLFW.GLFW_KEY_PAGE_DOWN){
            int base=mod.atlas.selectedLayer==Atlas.SURFACE?(client.player!=null?client.player.getBlockY()-1:64):mod.atlas.selectedLayer;
            mod.atlas.select(base+(key==GLFW.GLFW_KEY_PAGE_UP?1:-1)*mod.config.layerStep);
            updateButtons();
            return true;
        }
        return super.keyPressed(key,scan,modifiers);
    }

    void toggleExpanded(){if(expanded)close();else switchView(true);}
    private void switchView(boolean expanded){
        MapScreen next=new MapScreen(mod,expanded);
        next.centerX=centerX;next.centerZ=centerZ;next.targetCenterX=targetCenterX;next.targetCenterZ=targetCenterZ;next.zoom=zoom;next.targetZoom=targetZoom;
        client.setScreen(next);
    }
    private float transition(){
        long now=System.nanoTime();
        if(closingAt>=0){
            float p=UiMotion.easeOutCubic((now-closingAt)/180_000_000f);
            if(p>=1f){client.setScreen(null);return 0f;}
            return 1f-p;
        }
        return UiMotion.easeOutCubic((now-openedAt)/230_000_000f);
    }
    @Override public void close(){
        if(!expanded){client.setScreen(null);return;}
        if(closingAt<0)closingAt=System.nanoTime();
    }
    private void persistWorldMapZoom(){
        if(!expanded)return;
        double value=Math.clamp(targetZoom,.10,8);
        if(Math.abs(mod.config.worldMapZoom-value)>1.0e-6){mod.config.worldMapZoom=value;mod.config.save();}
    }
    @Override public void removed(){persistWorldMapZoom();mod.radar.clearHover();super.removed();}
    @Override public boolean shouldPause(){return false;}
}
