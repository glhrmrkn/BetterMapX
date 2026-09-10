package dev.bettermapx;

import java.util.*;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.util.Identifier;

/**
 * Compact premium Pokemon glows. Animation happens inside a fixed footprint
 * (travelling light / shimmer / spark detail), never by inflating circles.
 */
final class GlowRenderer {
    private static final int TEX=48, FRAMES=32;
    private static final int PROFILE_NORMAL=0, PROFILE_ELITE=1, PROFILE_SHINY=2;
    private final MinecraftClient mc=MinecraftClient.getInstance();
    private final Map<Integer,Identifier[]> sets=new HashMap<>();
    private final List<Identifier> owned=new ArrayList<>();

    static final int GLOW_SIZE_SUBTLE = 30;
    static final int GLOW_SIZE_STANDARD = 34;
    static final int GLOW_SIZE_ELITE = 40;

    void draw(DrawContext d,Rarity rarity,boolean shiny,int x,int y,long phaseSeed,double seconds,float appear,Config config){
        if(!config.animatedGlow)return;
        boolean base=rarity.premiumGlow(config);
        if(base){
            int size=switch(rarity){
                case LEGENDARY, MYTHICAL -> GLOW_SIZE_ELITE;
                case RARE, ULTRA_RARE -> GLOW_SIZE_STANDARD;
                default -> GLOW_SIZE_SUBTLE;
            };
            float power=rarity.glowPower()*(float)config.glowIntensity*1.55f*appear;
            int profile=(rarity==Rarity.LEGENDARY || rarity==Rarity.MYTHICAL)?PROFILE_ELITE:PROFILE_NORMAL;
            Identifier aura=frame(rarity.color,power,seconds,phaseSeed,profile);
            draw(d,aura,x,y,size);
        }
        if(shiny && config.glowShiny){
            int size=GLOW_SIZE_ELITE;
            float power=1.65f*(float)config.glowIntensity*appear;
            Identifier prism=frame(Rarity.SHINY_GLOW,power,seconds*1.12,phaseSeed^0x9e3779b97f4a7c15L,PROFILE_SHINY);
            draw(d,prism,x,y,size);
        }
    }

    private static void draw(DrawContext d,Identifier texture,int x,int y,int size){
        d.drawTexture(texture,x-size/2,y-size/2,size,size,0f,0f,TEX,TEX,TEX,TEX);
    }

    private Identifier frame(int color,float power,double seconds,long seed,int profile){
        int key=(color&0x00ffffff)^Float.floatToIntBits(power)^(profile*0x3119ab31);
        Identifier[] frames=sets.computeIfAbsent(key,k->build(color,power,profile,k));
        double offset=(seed&0xffffL)/65535.0;
        double cycle=(seconds/4.2+offset)%1.0;if(cycle<0)cycle+=1.0;
        return frames[Math.min(FRAMES-1,(int)(cycle*FRAMES))];
    }

    private Identifier[] build(int argb,float power,int profile,int key){
        Identifier[] out=new Identifier[FRAMES];
        int rr=(argb>>16)&255,gg=(argb>>8)&255,bb=argb&255;
        for(int f=0;f<FRAMES;f++){
            double phase=f*(Math.PI*2.0/FRAMES);
            NativeImage image=new NativeImage(TEX,TEX,false);
            for(int py=0;py<TEX;py++)for(int px=0;px<TEX;px++){
                double nx=(px+.5-TEX/2.0)/(TEX/2.0),ny=(py+.5-TEX/2.0)/(TEX/2.0);
                double r=Math.hypot(nx,ny);if(r>=0.96){image.setColor(px,py,0);continue;}
                double theta=Math.atan2(ny,nx);

                // Stable footprint: light bloom with dense core and smooth exponential falloff
                double envelope=Math.exp(-r*r*5.4);
                // Internal shimmer: traveling wave harmonics
                double shimmer=0.82 + 0.10*Math.cos(theta*3.0 + phase*0.75) + 0.08*Math.sin(theta*5.0 - phase*0.50);
                // Internal intensity breathing: subtle luminance oscillation
                double breathing=0.92 + 0.08*Math.sin(phase*2.0);
                // Intense bright core
                double core=Math.exp(-r*r*16.5)*0.45;
                double alpha=(envelope*shimmer*breathing*0.62 + core)*power;

                if(profile==PROFILE_ELITE){
                    // Denser highlights and orbital sparks for Legendary/Mythical prominence
                    alpha+=spark(nx,ny,0.38*Math.cos(phase*0.85),0.38*Math.sin(phase*0.85))*0.30*power;
                    alpha+=spark(nx,ny,0.52*Math.cos(-phase*0.65+2.1),0.52*Math.sin(-phase*0.65+2.1))*0.22*power;
                    alpha+=spark(nx,ny,0.44*Math.cos(phase*1.10+4.0),0.44*Math.sin(phase*1.10+4.0))*0.18*power;
                }else if(profile==PROFILE_SHINY){
                    // Prismatic cosmic aurora sparkles
                    alpha+=spark(nx,ny,0.42*Math.cos(phase*1.15),0.42*Math.sin(phase*1.15))*0.35*power;
                    alpha+=spark(nx,ny,0.56*Math.cos(-phase*0.90+1.9),0.56*Math.sin(-phase*0.90+1.9))*0.26*power;
                    alpha+=spark(nx,ny,0.48*Math.cos(phase*0.75+3.5),0.48*Math.sin(phase*0.75+3.5))*0.22*power;
                }
                if(alpha<=0.012){image.setColor(px,py,0);continue;}
                alpha=Math.clamp(alpha,0.0,0.95);

                int rOut=rr,gOut=gg,bOut=bb;
                double whiteCore=Math.clamp(Math.exp(-r*r*18.0)*0.35,0.0,0.35);

                if(profile==PROFILE_SHINY){
                    // Stitch Cosmic Aurora: sweep between stellar azure, gold, and magenta/white
                    double sweep=0.5 + 0.5*Math.sin(theta*2.0 + phase);
                    int sr=(int)Math.round(255*(0.75 + 0.25*sweep));
                    int sg=(int)Math.round(210 + 45*(1.0 - sweep));
                    int sb=(int)Math.round(255*(0.85 + 0.15*Math.cos(phase)));
                    rOut=(int)Math.round(rr*0.42 + sr*0.58);
                    gOut=(int)Math.round(gg*0.42 + sg*0.58);
                    bOut=(int)Math.round(bb*0.42 + sb*0.58);
                }

                rOut=(int)Math.round(rOut+(255-rOut)*whiteCore);
                gOut=(int)Math.round(gOut+(255-gOut)*whiteCore);
                bOut=(int)Math.round(bOut+(255-bOut)*whiteCore);

                int a=(int)Math.round(alpha*255.0);
                int color=a<<24|rOut<<16|gOut<<8|bOut;
                image.setColor(px,py,TileCodec.abgr(color));
            }
            NativeImageBackedTexture texture=new NativeImageBackedTexture(image);
            texture.setFilter(true,false);
            Identifier id=mc.getTextureManager().registerDynamicTexture("bettermapx_glow_"+Integer.toHexString(key)+"_"+f,texture);
            texture.upload();texture.setFilter(true,false);out[f]=id;owned.add(id);
        }
        return out;
    }

    private static double spark(double x,double y,double sx,double sy){
        double dx=x-sx,dy=y-sy;return Math.exp(-(dx*dx+dy*dy)*160.0);
    }
    void close(){for(Identifier id:owned)mc.getTextureManager().destroyTexture(id);owned.clear();sets.clear();}
}
