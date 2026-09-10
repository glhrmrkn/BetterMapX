package dev.bettermapx;

import java.nio.file.*;
import java.io.*;
import java.util.Properties;
import net.fabricmc.loader.api.FabricLoader;

public final class Config {
    public boolean minimap = true;
    /** 0 off, 1 hover, 2 premium rarities + hover, 3 all world outlines. */
    public int glowMode=2;
    public final Properties rarityOverrides=new Properties();
    public boolean pokemonRadar = true, pokemonIcons = true;

    // Premium map marker effects. Shiny is intentionally independent of rarity.
    public boolean animatedGlow = true;
    public boolean glowUnknown=true, glowCommon=true, glowUncommon=true, glowRare=true, glowUltraRare=true, glowLegendary=true, glowMythical=true, glowUltraBeast=true, glowShiny=true;
    public boolean espUnknown=false, espCommon=false, espUncommon=false, espRare=true, espUltraRare=true, espLegendary=true, espMythical=true, espUltraBeast=true, espShiny=true;
    public double glowIntensity = 1.60;

    // Spawn Areas overlay. The initial resolver intentionally uses static spawn geography
    // (biome/dimension/Y) from the active integrated-server datapacks. Dynamic conditions
    // such as weather/time/nearby blocks are not guessed.
    public boolean spawnAreas = false, spawnAreasFullMap = true, spawnAreasMinimap = false;
    public String spawnAreaPokemon = "";

    public int size = 144, corner = 0, margin = 12, layerStep = 16, verticalRange = 32;
    public double zoom=1.0, worldMapZoom=1.5, opacity=0.92, playerMarkerScaleMinimap=.55, playerMarkerScaleFullMap=.65;
    private final Path file = FabricLoader.getInstance().getConfigDir().resolve("bettermapx.properties");

    public Config() {
        if (!Files.exists(file)) return;
        Properties p = new Properties();
        try (Reader r = Files.newBufferedReader(file)) {
            p.load(r);
            glowMode=integer(p,"glowMode",2,0,3);
            for(String key:p.stringPropertyNames())if(key.startsWith("rarity."))rarityOverrides.setProperty(key.substring(7),p.getProperty(key));
            minimap = bool(p,"minimap",true);
            pokemonRadar = bool(p,"pokemonRadar",true);
            pokemonIcons = bool(p,"pokemonIcons",true);
            animatedGlow = bool(p,"animatedGlow",true);
            glowUnknown=bool(p,"glowUnknown",true); glowCommon=bool(p,"glowCommon",true); glowUncommon=bool(p,"glowUncommon",true);
            glowRare = bool(p,"glowRare",true);
            glowUltraRare = bool(p,"glowUltraRare",true);
            glowLegendary = bool(p,"glowLegendary",true);
            glowMythical = bool(p,"glowMythical",true);
            glowUltraBeast=bool(p,"glowUltraBeast",true); glowShiny = bool(p,"glowShiny",true);
            espUnknown=bool(p,"espUnknown",false); espCommon=bool(p,"espCommon",false); espUncommon=bool(p,"espUncommon",false); espRare=bool(p,"espRare",true); espUltraRare=bool(p,"espUltraRare",true); espLegendary=bool(p,"espLegendary",true); espMythical=bool(p,"espMythical",true); espUltraBeast=bool(p,"espUltraBeast",true); espShiny=bool(p,"espShiny",true);
            glowIntensity = decimal(p,"glowIntensity",1.60,0,3);
            spawnAreas = bool(p,"spawnAreas",false);
            spawnAreasFullMap = bool(p,"spawnAreasFullMap",true);
            spawnAreasMinimap = bool(p,"spawnAreasMinimap",false);
            spawnAreaPokemon = p.getProperty("spawnAreaPokemon","").trim().toLowerCase(java.util.Locale.ROOT);
            size = integer(p,"size",144,96,256); corner = integer(p,"corner",0,0,3);
            margin = integer(p,"margin",12,0,100); layerStep = integer(p,"layerStep",16,4,64);
            verticalRange = integer(p,"verticalRange",32,8,64);
            zoom=decimal(p,"zoom",1,.1,8); worldMapZoom=decimal(p,"worldMapZoom",1.5,.1,8); opacity=decimal(p,"opacity",.92,.25,1);
            double legacy=decimal(p,"playerMarkerScale",.65,.25,2); playerMarkerScaleMinimap=decimal(p,"playerMarkerScaleMinimap",legacy,.25,2); playerMarkerScaleFullMap=decimal(p,"playerMarkerScaleFullMap",legacy,.25,2);
        } catch (IOException | RuntimeException e) { BetterMapX.LOG.warn("Could not load configuration", e); }
    }
    private static boolean bool(Properties p,String k,boolean d){return Boolean.parseBoolean(p.getProperty(k,Boolean.toString(d)));}
    private static int integer(Properties p,String k,int d,int min,int max) {
        try { return Math.clamp(Integer.parseInt(p.getProperty(k,""+d)),min,max); }
        catch (NumberFormatException e) { return d; }
    }
    private static double decimal(Properties p,String k,double d,double min,double max) {
        try { double v=Double.parseDouble(p.getProperty(k,""+d)); return Double.isFinite(v)?Math.clamp(v,min,max):d; }
        catch (NumberFormatException e) { return d; }
    }
    public void save() {
        Properties p=new Properties();
        p.setProperty("glowMode",""+glowMode);
        for(String key:rarityOverrides.stringPropertyNames())p.setProperty("rarity."+key,rarityOverrides.getProperty(key));
        p.setProperty("pokemonRadar",""+pokemonRadar); p.setProperty("pokemonIcons",""+pokemonIcons);
        p.setProperty("animatedGlow",""+animatedGlow); p.setProperty("glowUnknown",""+glowUnknown); p.setProperty("glowCommon",""+glowCommon); p.setProperty("glowUncommon",""+glowUncommon); p.setProperty("glowRare",""+glowRare);
        p.setProperty("glowUltraRare",""+glowUltraRare); p.setProperty("glowLegendary",""+glowLegendary);
        p.setProperty("glowMythical",""+glowMythical); p.setProperty("glowUltraBeast",""+glowUltraBeast); p.setProperty("glowShiny",""+glowShiny);
        p.setProperty("espUnknown",""+espUnknown); p.setProperty("espCommon",""+espCommon); p.setProperty("espUncommon",""+espUncommon); p.setProperty("espRare",""+espRare); p.setProperty("espUltraRare",""+espUltraRare); p.setProperty("espLegendary",""+espLegendary); p.setProperty("espMythical",""+espMythical); p.setProperty("espUltraBeast",""+espUltraBeast); p.setProperty("espShiny",""+espShiny);
        p.setProperty("glowIntensity",""+glowIntensity);
        p.setProperty("spawnAreas",""+spawnAreas); p.setProperty("spawnAreasFullMap",""+spawnAreasFullMap);
        p.setProperty("spawnAreasMinimap",""+spawnAreasMinimap); p.setProperty("spawnAreaPokemon",spawnAreaPokemon==null?"":spawnAreaPokemon);
        p.setProperty("minimap",""+minimap); p.setProperty("size",""+size); p.setProperty("corner",""+corner);
        p.setProperty("margin",""+margin); p.setProperty("layerStep",""+layerStep);
        p.setProperty("verticalRange",""+verticalRange); p.setProperty("zoom",""+zoom); p.setProperty("worldMapZoom",""+worldMapZoom); p.setProperty("opacity",""+opacity);
        p.setProperty("playerMarkerScaleMinimap",""+playerMarkerScaleMinimap); p.setProperty("playerMarkerScaleFullMap",""+playerMarkerScaleFullMap);
        try { Files.createDirectories(file.getParent()); try(Writer w=Files.newBufferedWriter(file)){p.store(w,"BetterMapX - vertical distances in blocks, size in GUI pixels");} }
        catch(IOException e){BetterMapX.LOG.error("Could not save configuration",e);}
    }
}
