package dev.bettermapx;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.util.Identifier;
import com.mojang.blaze3d.systems.RenderSystem;

public final class MapRenderer {
    private final BetterMapX mod;
    public MapRenderer(BetterMapX mod){this.mod=mod;}
    public static void panel(DrawContext d,int x,int y,int w,int h,int c){
        // A small pixel radius stays sharp at every GUI scale.
        d.fill(x+3,y,x+w-3,y+h,c);d.fill(x+1,y+1,x+w-1,y+h-1,c);d.fill(x,y+3,x+w,y+h-3,c);
    }
    public void render(DrawContext d,int x,int y,int w,int h,double centerX,double centerZ,double zoom,boolean full){
        MinecraftClient mc=MinecraftClient.getInstance();Atlas atlas=mod.atlas;
        atlas.beginFrame();
        for(int edge=1;edge<=3;edge++)d.drawBorder(x-edge,y-edge,w+edge*2,h+edge*2,edge==3?0xff333944:0xff444952);
        d.fill(x,y,x+w,y+h,full?0xff12151b:0x2012151b);
        d.enableScissor(x,y,x+w,y+h);
        try{
            double left=centerX-w/(2.0*zoom),top=centerZ-h/(2.0*zoom);
            int minX=(int)Math.floor(left/16),minZ=(int)Math.floor(top/16);
            int maxX=(int)Math.floor((left+w/zoom)/16),maxZ=(int)Math.floor((top+h/zoom)/16);
            // Texture alpha is global state: flush GUI quads before changing it, then restore.
            d.draw();RenderSystem.enableBlend();RenderSystem.defaultBlendFunc();
            RenderSystem.setShaderColor(1,1,1,full?1:(float)mod.config.opacity);
            for(int cz=minZ;cz<=maxZ;cz++)for(int cx=minX;cx<=maxX;cx++){
                int sx=x+(int)Math.floor((cx*16.0-left)*zoom),sz=y+(int)Math.floor((cz*16.0-top)*zoom);
                int ex=x+(int)Math.floor(((cx+1)*16.0-left)*zoom),ez=y+(int)Math.floor(((cz+1)*16.0-top)*zoom);
                Atlas.Tile tile=atlas.get(new Atlas.Key(cx,cz,atlas.selectedLayer,atlas.selectedLayer==Atlas.SURFACE?0:mod.config.verticalRange));
                if(tile!=null){Identifier id=atlas.texture(tile);if(id!=null)d.drawTexture(id,sx,sz,ex-sx,ez-sz,0f,0f,16,16,16,16);}
            }
            d.draw();RenderSystem.setShaderColor(1,1,1,1);
            mod.spawnAreas.render(d,x,y,w,h,centerX,centerZ,zoom,full);
            for(var areas:OverlayApi.all())for(var a:areas){
                if(!a.dimension().equals(atlas.dimension))continue;
                if(atlas.selectedLayer==Atlas.SURFACE?!a.surface():atlas.selectedLayer<a.minY() || atlas.selectedLayer>a.maxY())continue;
                int ax=x+(int)Math.floor((a.minX()-left)*zoom),az=y+(int)Math.floor((a.minZ()-top)*zoom);
                int bx=x+(int)Math.ceil((a.maxX()+1.0-left)*zoom),bz=y+(int)Math.ceil((a.maxZ()+1.0-top)*zoom);
                if(bx<x || bz<y || ax>x+w || az>y+h)continue;
                d.fill(Math.max(x,ax),Math.max(y,az),Math.min(x+w,bx),Math.min(y+h,bz),a.fill());
                d.drawBorder(ax,az,bx-ax,bz-az,a.border());
            }
            mod.radar.render(d,x,y,w,h,centerX,centerZ,zoom,full);
            for(var p:mod.waypoints.all()){
                if(!p.dimension().equals(atlas.dimension))continue;
                if(atlas.selectedLayer!=Atlas.SURFACE && Math.abs(p.y()-atlas.selectedLayer)>mod.config.verticalRange)continue;
                int px=x+(int)Math.round((p.x()+.5-left)*zoom),py=y+(int)Math.round((p.z()+.5-top)*zoom);
                if(px<x || px>=x+w || py<y || py>=y+h)continue;
                panel(d,px-4,py-4,9,9,0xff171a20);
                d.drawCenteredTextWithShadow(mc.textRenderer,p.icon(),px,py-3,p.color()|0xff000000);
                if(full)d.drawTextWithShadow(mc.textRenderer,p.name(),px+8,py-3,0xffe2e5ec);
            }
            if(mc.player!=null){
                int px=x+(int)Math.round((mc.player.getX()-left)*zoom),py=y+(int)Math.round((mc.player.getZ()-top)*zoom);
                if(px>=x && px<x+w && py>=y && py<y+h)drawPlayerMarker(d,px,py,mc.player.getYaw(),full?mod.config.playerMarkerScaleFullMap:mod.config.playerMarkerScaleMinimap);
            }
        }finally{d.draw();RenderSystem.setShaderColor(1,1,1,1);d.disableScissor();}
        d.drawTextWithShadow(mc.textRenderer,"N",x+w/2-3,y+5,0xffdce2ed);
        if(full && atlas.size()==0)d.drawCenteredTextWithShadow(mc.textRenderer,"Lendo terreno recebido pelo cliente...",x+w/2,y+h/2,0xffacb5c6);
    }
    private void drawPlayerMarker(DrawContext d,int x,int y,float yawDegrees,double markerScale){
        d.draw();
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();

        float scale=(float)markerScale;

        // Exact Stitch Directional Chevron Arrow (M12 2L3 21L12 17L21 21L12 2Z)
        d.getMatrices().push();
        d.getMatrices().translate(x,y,0);
        d.getMatrices().scale(scale,scale,1f);
        d.getMatrices().multiply(net.minecraft.util.math.RotationAxis.POSITIVE_Z.rotationDegrees(yawDegrees-180f));
        renderChevron(d);
        d.getMatrices().pop();

        d.draw();
    }

    private static void renderChevron(DrawContext d){
        org.joml.Matrix4f mat=d.getMatrices().peek().getPositionMatrix();
        RenderSystem.setShader(net.minecraft.client.render.GameRenderer::getPositionColorProgram);
        net.minecraft.client.render.Tessellator tessellator=net.minecraft.client.render.Tessellator.getInstance();
        net.minecraft.client.render.BufferBuilder buf=tessellator.begin(net.minecraft.client.render.VertexFormat.DrawMode.TRIANGLES,net.minecraft.client.render.VertexFormats.POSITION_COLOR);

        // 1. Outer subtle cyan bloom halo (scaled appropriately, sleek and compact)
        drawChevronTriangles(buf,mat,0f,-8.8f,-7.2f,7.2f,0f,1.8f,7.2f,7.2f,0x4000f0ff);
        drawChevronTriangles(buf,mat,0f,-7.8f,-6.2f,6.4f,0f,2.2f,6.2f,6.4f,0x6606b6d4);

        // 2. Drop Shadow Silhouette (offset +1.2px down for high contrast against snow/water/sand)
        drawChevronTriangles(buf,mat,0f,-5.6f,-5.6f,6.8f,0f,3.6f,5.6f,6.8f,0xcc05080e);

        // 3. Crisp White Outline
        drawChevronTriangles(buf,mat,0f,-6.8f,-5.2f,5.8f,0f,2.4f,5.2f,5.8f,0xffffffff);

        // 4. Vibrant Stitch Primary Cyan Core
        drawChevronTriangles(buf,mat,0f,-5.8f,-4.2f,4.8f,0f,2.7f,4.2f,4.8f,StitchTheme.CYAN_PRIMARY);

        // 5. Inner Bright Spine Highlight
        drawChevronTriangles(buf,mat,0f,-5.0f,-1.4f,3.4f,0f,2.8f,1.4f,3.4f,0xffd9fbff);

        net.minecraft.client.render.BufferRenderer.drawWithGlobalProgram(buf.end());
    }

    private static void drawChevronTriangles(net.minecraft.client.render.BufferBuilder buf,org.joml.Matrix4f mat,float tipX,float tipY,float leftX,float leftY,float notchX,float notchY,float rightX,float rightY,int color){
        int a=(color>>>24)&255,r=(color>>>16)&255,g=(color>>>8)&255,b=color&255;
        // Left wing triangle: Tip -> Left -> Notch
        buf.vertex(mat,tipX,tipY,0).color(r,g,b,a);
        buf.vertex(mat,leftX,leftY,0).color(r,g,b,a);
        buf.vertex(mat,notchX,notchY,0).color(r,g,b,a);
        // Right wing triangle: Tip -> Notch -> Right
        buf.vertex(mat,tipX,tipY,0).color(r,g,b,a);
        buf.vertex(mat,notchX,notchY,0).color(r,g,b,a);
        buf.vertex(mat,rightX,rightY,0).color(r,g,b,a);
    }

    public void hud(DrawContext d){
        MinecraftClient mc=MinecraftClient.getInstance();
        if(!mod.config.minimap || mc.player==null || mc.world==null || mc.options.hudHidden || mc.currentScreen!=null)return;
        int sw=mc.getWindow().getScaledWidth(),sh=mc.getWindow().getScaledHeight();
        int s=Math.min(mod.config.size,Math.min(sw-24,sh-48));if(s<48)return;
        int margin=Math.min(mod.config.margin,Math.max(4,Math.min((sw-s)/2,(sh-s-24)/2)));
        int x=(mod.config.corner%2==0)?sw-s-margin:margin;
        int y=(mod.config.corner<2)?margin:sh-s-margin-22;

        // Outer Stitch Bevel Frame for minimap
        StitchTheme.drawBevelPanel(d,x-3,y-3,s+6,s+6);
        render(d,x,y,s,s,mc.player.getX(),mc.player.getZ(),mod.config.zoom,false);

        // Stitch Telemetry Coordinate Pod
        StitchTheme.drawBevelPanel(d,x-3,y+s+4,s+6,20);
        String coords=mc.player.getBlockX()+"  /  "+mc.player.getBlockY()+"  /  "+mc.player.getBlockZ();
        d.drawCenteredTextWithShadow(mc.textRenderer,coords,x+s/2,y+s+9,StitchTheme.TEXT_ON_SURFACE);
    }
}
