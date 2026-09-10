package dev.bettermapx;

import com.google.gson.*;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import net.fabricmc.loader.api.FabricLoader;

/** Local default spawn buckets, not a claim about the server's spawn conditions. */
final class RarityCatalog {
    private volatile Map<String,String> buckets=Map.of();
    RarityCatalog(){
        FabricLoader.getInstance().getModContainer("cobblemon").ifPresent(mod->
            mod.findPath("data/cobblemon/spawn_pool_world").ifPresent(root->
                CompletableFuture.runAsync(()->load(root))));
    }
    String bucket(String species){return buckets.get(species);}
    static void collect(JsonObject root,Map<String,String> output){
        if(root.has("enabled") && !root.get("enabled").getAsBoolean())return;
        if(!root.has("spawns"))return;
        for(JsonElement entry:root.getAsJsonArray("spawns")){
            JsonObject spawn=entry.getAsJsonObject();
            if(!spawn.has("pokemon") || !spawn.has("bucket"))continue;
            String species=spawn.get("pokemon").getAsString().trim().split("\\s+")[0];
            if(!species.contains(":"))species="cobblemon:"+species;
            String bucket=spawn.get("bucket").getAsString().toUpperCase(Locale.ROOT).replace('-','_');
            if(!List.of("COMMON","UNCOMMON","RARE","ULTRA_RARE").contains(bucket))continue;
            output.merge(species,bucket,(a,b)->Rarity.valueOf(a).ordinal()<=Rarity.valueOf(b).ordinal()?a:b);
        }
    }
    private void load(Path root){
        Map<String,String> result=new HashMap<>();
        try(var paths=Files.walk(root)){
            for(Path file:paths.filter(p->p.toString().endsWith(".json")).limit(10000).toList()){
                try(var reader=Files.newBufferedReader(file)){collect(JsonParser.parseReader(reader).getAsJsonObject(),result);}
                catch(Exception error){BetterMapX.LOG.debug("Skipping local spawn file {}",file,error);}
            }
            buckets=Map.copyOf(result);
        }catch(Exception error){BetterMapX.LOG.warn("Local rarity catalog unavailable",error);}
    }
}
