package dev.bettermapx;

import java.util.*;

enum Rarity {
    UNKNOWN("Desconhecida",0xff94a3b8), COMMON("Comum",0xff94a3b8),
    UNCOMMON("Incomum",0xff4edea3), RARE("Raro",0xff06b6d4),
    ULTRA_RARE("Ultra Raro",0xff8b5cf6), LEGENDARY("Lendário",0xfff59e0b),
    MYTHICAL("Mítico",0xffd946ef), ULTRA_BEAST("Ultra Beast",0xfff43f5e), SHINY("Shiny",0xfffbbf24);

    static final int SHINY_GLOW=0xfffbbf24;
    final String label; final int color;
    Rarity(String label,int color){this.label=label;this.color=color;}

    /** Shiny is intentionally not returned here: it is an independent visual trait. */
    static Rarity classify(boolean shiny, Collection<?> labels, String override){
        if(override!=null)try{return valueOf(override.trim().toUpperCase(Locale.ROOT).replace('-','_'));}catch(IllegalArgumentException ignored){}
        for(String label:List.of("mythical","legendary","ultra_rare","ultra-rare","ultra_beast")){
            if(!labels.contains(label))continue;
            String normalized=label.toUpperCase(Locale.ROOT).replace('-','_');
            try{return valueOf(normalized);}catch(IllegalArgumentException ignored){}
        }
        return UNKNOWN;
    }

    boolean premiumGlow(Config c){
        return switch(this){
            case UNKNOWN -> c.glowUnknown;
            case COMMON -> c.glowCommon;
            case UNCOMMON -> c.glowUncommon;
            case RARE -> c.glowRare;
            case ULTRA_RARE -> c.glowUltraRare;
            case LEGENDARY -> c.glowLegendary;
            case MYTHICAL -> c.glowMythical;
            case ULTRA_BEAST -> c.glowUltraBeast;
            default -> false;
        };
    }
    boolean espEnabled(Config c){return switch(this){
        case UNKNOWN -> c.espUnknown; case COMMON -> c.espCommon; case UNCOMMON -> c.espUncommon;
        case RARE -> c.espRare; case ULTRA_RARE -> c.espUltraRare; case LEGENDARY -> c.espLegendary;
        case MYTHICAL -> c.espMythical; case ULTRA_BEAST -> c.espUltraBeast; default -> false;
    };}
    float glowScale(){
        return switch(this){
            case LEGENDARY, MYTHICAL -> 1.08f;
            default -> 1.00f;
        };
    }
    float glowPower(){
        return switch(this){
            case COMMON, UNKNOWN -> 0.70f;
            case UNCOMMON -> 0.85f;
            case RARE -> 1.05f;
            case ULTRA_RARE -> 1.20f;
            case LEGENDARY -> 1.50f;
            case MYTHICAL -> 1.55f;
            default -> 0.75f;
        };
    }
}
