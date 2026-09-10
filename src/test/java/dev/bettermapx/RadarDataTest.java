package dev.bettermapx;

import java.util.*;
import com.google.gson.JsonParser;

public final class RadarDataTest {
    public static final class Species {}
    public static final class Pokemon {
        final Set<String> aspects=new HashSet<>(Set.of("female"));
        public int getLevel(){return 42;}
        public boolean getShiny(){return true;}
        public Species getSpecies(){return new Species();}
        public Set<String> getAspects(){return aspects;}
    }
    public static final class Entity {
        final Pokemon pokemon=new Pokemon();
        public Pokemon getPokemon(){return pokemon;}
    }
    public static final class Factory {
        public static Object from(Pokemon pokemon){return pokemon;}
        public static Object from(Pokemon pokemon,int count){throw new AssertionError("wrong overload");}
    }
    static void check(boolean ok,String message){if(!ok)throw new AssertionError(message);}
    public static void run()throws Exception{
        CobblemonAccess bridge=new CobblemonAccess(Factory.class.getName());
        Entity entity=new Entity();var data=bridge.read(entity);
        check(data.level()==42 && data.shiny(),"Pokemon metadata");
        entity.pokemon.aspects.clear();check(data.look().aspects().contains("female"),"immutable icon cache key");
        check(bridge.icon(data)==entity.pokemon,"static one argument item factory");
        try{bridge.read(new Object());throw new AssertionError("unsupported entity accepted");}catch(NoSuchMethodException expected){}
        check(RadarRules.isPokemon("cobblemon","pokemon") && !RadarRules.isPokemon("other","pokemon"),"entity namespace isolation");
        check(RadarRules.inLayer(Integer.MAX_VALUE,-200,32) && RadarRules.inLayer(-16,16,32) && !RadarRules.inLayer(-16,17,32),"layer boundary");
        check(RadarRules.screen(-24,-16,2,10,100)==44,"negative world projection");
        check(Rarity.classify(true,List.of("legendary"),null)==Rarity.LEGENDARY,"shiny stays independent from base rarity");
        check(Rarity.classify(false,List.of("legendary"),null)==Rarity.LEGENDARY,"species label");
        check(Rarity.classify(false,List.of(),"rare")==Rarity.RARE,"rarity override");
        check(Rarity.classify(false,List.of(),"invalid")==Rarity.UNKNOWN,"unknown is not common");
        check(SpawnAreas.normalize("Pikachu shiny=true").equals("pikachu") && SpawnAreas.display("iron_valiant").equals("Iron Valiant"),"spawn area Pokemon normalization");
        Map<String,String> buckets=new HashMap<>();
        RarityCatalog.collect(JsonParser.parseString("""
            {"spawns":[{"pokemon":"caterpie","bucket":"rare"},
            {"pokemon":"caterpie valencian","bucket":"common"},
            {"pokemon":"foo","bucket":"custom"}]}
            """).getAsJsonObject(),buckets);
        check(buckets.equals(Map.of("cobblemon:caterpie","COMMON")),"least rare local species bucket and unsupported bucket");
        RarityCatalog.collect(JsonParser.parseString("{\"enabled\":false,\"spawns\":[{\"pokemon\":\"foo\",\"bucket\":\"rare\"}]}").getAsJsonObject(),buckets);
        check(!buckets.containsKey("cobblemon:foo"),"disabled pools ignored");
        System.out.println("PASS: 14 radar/rarity/spawn-area data checks (not a game integration test)");
    }
}
