package dev.bettermapx;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;

/** Compact button with soft, continuous hover response instead of rigid scale pops. */
final class SilverButton extends ButtonWidget {
    private float hoverAmount;
    private long lastFrame=System.nanoTime();
    private boolean selected;

    private SilverButton(int x,int y,int w,int h,Text text,PressAction action,boolean selected){
        super(x,y,w,h,text,action,DEFAULT_NARRATION_SUPPLIER);
        this.selected=selected;
    }
    static Factory make(Text text,PressAction action){return new Factory(text,action);}
    static final class Factory {
        private final Text text;private final PressAction action;private int x,y,w=100,h=20;
        private boolean selected;
        Factory(Text text,PressAction action){this.text=text;this.action=action;}
        Factory dimensions(int x,int y,int w,int h){this.x=x;this.y=y;this.w=w;this.h=h;return this;}
        Factory selected(boolean selected){this.selected=selected;return this;}
        SilverButton build(){return new SilverButton(x,y,w,h,text,action,selected);}
    }
    public void setSelected(boolean selected){this.selected=selected;}
    public boolean isSelected(){return selected;}

    @Override protected void renderWidget(DrawContext d,int mx,int my,float delta){
        long now=System.nanoTime();float dt=(float)Math.min(.08,(now-lastFrame)/1e9);lastFrame=now;
        boolean hovered=active&&(isHovered()||isFocused());
        float target=hovered?1f:0f;
        hoverAmount=UiMotion.smooth(hoverAmount,target,dt,18f);

        int bx=getX(),by=getY(),bw=getWidth(),bh=getHeight();
        StitchTheme.drawBevelButton(d,bx,by,bw,bh,hovered,active,selected);

        var tr=MinecraftClient.getInstance().textRenderer;
        String label=tr.trimToWidth(getMessage().getString(),bw-6);

        int textColor;
        if(!active) textColor=StitchTheme.TEXT_DARK;
        else if(selected) textColor=StitchTheme.TAB_ACTIVE_TEXT;
        else textColor=UiMotion.mix(StitchTheme.TEXT_ON_SURFACE,StitchTheme.CYAN_PRIMARY,hoverAmount);

        d.drawCenteredTextWithShadow(tr,label,bx+bw/2,by+(bh-8)/2,textColor);
    }
}
