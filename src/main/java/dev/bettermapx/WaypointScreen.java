package dev.bettermapx;

import java.util.UUID;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.*;
import net.minecraft.text.Text;

public final class WaypointScreen extends Screen {
    private final BetterMapX mod;private final Screen parent;private final Waypoints.Point existing;
    private final int x,y,z;private TextFieldWidget name;private int color=0,icon=0;
    private final int[] colors={0xffb9caea,0xffcd91ed,0xffeea3b9,0xffa8d6b6,0xffe5cf8d};
    private final String[] icons={"+","H","!","*","O"};
    private String draft;
    public WaypointScreen(BetterMapX mod,Screen parent,Waypoints.Point existing,int x,int y,int z){
        super(Text.literal("Waypoint"));this.mod=mod;this.parent=parent;this.existing=existing;
        this.x=existing==null?x:existing.x();this.y=existing==null?y:existing.y();this.z=existing==null?z:existing.z();
        draft=existing==null?"Waypoint":existing.name();
        if(existing!=null && existing.id().startsWith("xaero:") && draft.endsWith(" [Xaero]"))draft=draft.substring(0,draft.length()-8);
        if(existing!=null){
            boolean knownColor=false,knownIcon=false;
            for(int i=0;i<colors.length;i++)if(colors[i]==existing.color()){color=i;knownColor=true;}
            for(int i=0;i<icons.length;i++)if(icons[i].equals(existing.icon())){icon=i;knownIcon=true;}
            if(!knownColor)colors[0]=existing.color();if(!knownIcon)icons[0]=existing.icon();
        }
    }
    @Override protected void init(){
        int left=width/2-130,top=Math.max(42,height/2-66);
        name=new TextFieldWidget(textRenderer,left,top,260,20,Text.literal("Nome"));name.setMaxLength(48);name.setText(draft);name.setChangedListener(s->draft=s);addDrawableChild(name);setInitialFocus(name);
        addDrawableChild(SilverButton.make(Text.literal("Cor: "+(color+1)),b->{color=(color+1)%colors.length;b.setMessage(Text.literal("Cor: "+(color+1)));}).dimensions(left,top+28,126,20).build());
        addDrawableChild(SilverButton.make(Text.literal("Icone: "+icons[icon]),b->{icon=(icon+1)%icons.length;b.setMessage(Text.literal("Icone: "+icons[icon]));}).dimensions(left+134,top+28,126,20).build());
        addDrawableChild(SilverButton.make(Text.literal(existing!=null && existing.id().startsWith("xaero:")?"Copiar para BetterMapX":"Salvar"),b->{
            if(name.getText().isBlank())return;
            mod.waypoints.put(new Waypoints.Point(existing==null || existing.id().startsWith("xaero:")?UUID.randomUUID().toString():existing.id(),name.getText().strip(),existing==null?mod.atlas.dimension:existing.dimension(),x,y,z,colors[color],icons[icon]));close();
        }).dimensions(left,top+56,126,20).build());
        addDrawableChild(SilverButton.make(Text.literal(existing==null || existing.id().startsWith("xaero:")?"Cancelar":"Excluir"),b->{if(existing!=null && !existing.id().startsWith("xaero:"))mod.waypoints.remove(existing);close();}).dimensions(left+134,top+56,126,20).build());
        String targetDimension=existing==null?mod.atlas.dimension:existing.dimension();
        ButtonWidget tp=SilverButton.make(Text.literal("Teleportar"),b->{
            if(client.player==null || !targetDimension.equals(mod.atlas.dimension))return;
            // Always send the command and let the server enforce permissions. The old
            // client-side permission gate produced false negatives and disabled teleport
            // for a fresh right-click position because existing == null.
            client.player.networkHandler.sendChatCommand("teleport @s "+(x+.5)+" "+y+" "+(z+.5));close();
        }).dimensions(left,top+84,260,20).build();
        tp.active=client.player!=null && targetDimension.equals(mod.atlas.dimension);addDrawableChild(tp);
    }
    @Override public void renderBackground(DrawContext d,int mouseX,int mouseY,float delta){}
    @Override public void render(DrawContext d,int mx,int my,float delta){
        d.fill(0,0,width,height,0xbb080b10);
        int left=width/2-130,top=Math.max(42,height/2-66);
        StitchTheme.drawBevelPanel(d,left-12,top-32,284,146,0xf8121620,StitchTheme.BORDER_OUTER);
        StitchTheme.drawProceduralPokeball(d,left-4,top-26,14);
        d.drawText(textRenderer,"BetterMapX // Waypoint",left+16,top-23,StitchTheme.TEXT_WHITE,true);
        String coords=x+" / "+y+" / "+z;
        int coordW=textRenderer.getWidth(coords);
        d.drawText(textRenderer,coords,left+260-coordW,top-23,colors[color],false);
        d.fill(left-8,top-10,left+268,top-9,0xff2d3440);
        super.render(d,mx,my,delta);
    }
    @Override public void close(){client.setScreen(parent);}
    @Override public boolean shouldPause(){return false;}
}
