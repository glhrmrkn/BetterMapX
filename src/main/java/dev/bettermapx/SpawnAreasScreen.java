package dev.bettermapx;

import java.util.*;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;

final class SpawnAreasScreen extends Screen {
    private final BetterMapX mod;
    private final Screen parent;
    private TextFieldWidget search;
    private List<String> suggestions=List.of();
    private int listX,listY,listW;
    private static final int ROW=18,MAX=7;

    SpawnAreasScreen(BetterMapX mod,Screen parent){
        super(Text.literal("Spawn Areas"));
        this.mod=mod;
        this.parent=parent;
    }

    @Override protected void init(){
        int x=width/2-150,y=Math.max(30,height/2-145);
        Config c=mod.config;
        listX=x;
        listY=y+105;
        listW=300;

        addToggle("Spawn Areas",c.spawnAreas,x,y,146,()->c.spawnAreas=!c.spawnAreas);
        addToggle("Mapa cheio",c.spawnAreasFullMap,x+154,y,146,()->c.spawnAreasFullMap=!c.spawnAreasFullMap);
        addToggle("Minimapa",c.spawnAreasMinimap,x,y+24,146,()->c.spawnAreasMinimap=!c.spawnAreasMinimap);

        addDrawableChild(SilverButton.make(Text.literal("Remover selecao"),b->{
            mod.spawnAreas.select("");
            refresh("");
        }).dimensions(x+154,y+24,146,20).build());

        search=new TextFieldWidget(textRenderer,x,y+64,300,20,Text.literal("Buscar Pokemon"));
        search.setMaxLength(48);
        search.setPlaceholder(Text.literal("Buscar Pokemon..."));
        search.setChangedListener(this::refresh);
        addDrawableChild(search);

        refresh("");

        addDrawableChild(SilverButton.make(Text.literal("Concluir"),b->close())
                .dimensions(x,y+244,300,20).build());
    }

    private void refresh(String query){
        String q=SpawnAreas.normalize(query);
        List<String> all=mod.spawnAreas.species();
        ArrayList<String> out=new ArrayList<>();
        for(String id:all){
            if(q.isBlank()||id.contains(q)){
                out.add(id);
                if(out.size()>=MAX)break;
            }
        }
        suggestions=List.copyOf(out);
    }

    private void addToggle(String label,boolean state,int x,int y,int w,Runnable action){
        addDrawableChild(SilverButton.make(Text.literal(label+": "+(state?"ON":"OFF")),b->{
            action.run();
            mod.config.save();
            clearAndInit();
        }).dimensions(x,y,w,20).selected(state).build());
    }

    @Override public void renderBackground(DrawContext d,int mouseX,int mouseY,float delta){}

    @Override public void render(DrawContext d,int mx,int my,float delta){
        d.fill(0,0,width,height,0xbb080b10);
        int x=width/2-150,y=Math.max(30,height/2-145);

        // Stitch Bevel Panel
        StitchTheme.drawBevelPanel(d,x-12,y-28,324,298,0xf8121620,StitchTheme.BORDER_OUTER);
        StitchTheme.drawProceduralPokeball(d,x-4,y-22,14);
        d.drawText(textRenderer,"BetterMapX // Spawn Areas",leftOrRight(x+16),y-19,StitchTheme.TEXT_WHITE,true);
        d.fill(x-8,y-7,x+308,y-6,0xff2d3440);

        String selected="Selecionado: "+SpawnAreas.display(mod.spawnAreas.selected());
        d.drawText(textRenderer,textRenderer.trimToWidth(selected,300),x,y+50,StitchTheme.CYAN_PRIMARY,false);

        super.render(d,mx,my,delta);

        for(int i=0;i<suggestions.size();i++){
            int ry=listY+i*ROW;
            boolean hover=mx>=listX && mx<listX+listW && my>=ry && my<ry+ROW-1;
            StitchTheme.drawBevelButton(d,listX,ry,listW,ROW-1,hover,true,false);
            String label=SpawnAreas.display(suggestions.get(i));
            d.drawText(textRenderer,label,listX+7,ry+4,hover?StitchTheme.CYAN_PRIMARY:StitchTheme.TEXT_ON_SURFACE,false);
        }

        String status;
        if(!mod.spawnAreas.resourcesAvailable()) status="Dados: indisponiveis neste servidor";
        else if(mod.spawnAreas.selected().isBlank()) status="Escolha um Pokemon na lista acima";
        else status="Regras ativas: "+mod.spawnAreas.ruleCount()+" (bioma / dimensao / altura)";

        int stColor=mod.spawnAreas.ruleCount()>0?StitchTheme.EMERALD_GREEN:StitchTheme.TEXT_MUTED;
        d.drawCenteredTextWithShadow(textRenderer,status,width/2,listY+MAX*ROW+10,stColor);
    }

    private int leftOrRight(int v){return v;}

    @Override public boolean mouseClicked(double mx,double my,int button){
        if(button==0 && mx>=listX && mx<listX+listW){
            int i=(int)((my-listY)/ROW);
            if(i>=0 && i<suggestions.size() && my>=listY){
                mod.spawnAreas.select(suggestions.get(i));
                search.setText(SpawnAreas.display(suggestions.get(i)));
                refresh(search.getText());
                return true;
            }
        }
        return super.mouseClicked(mx,my,button);
    }

    @Override public void close(){client.setScreen(parent);}
    @Override public boolean shouldPause(){return false;}
}
