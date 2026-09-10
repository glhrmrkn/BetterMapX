package dev.bettermapx;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/** Optional bridge: only Cobblemon's public JVM methods are reflected, never mapped Minecraft names. */
final class CobblemonAccess {
    record Look(Object species, Set<?> aspects, boolean shiny) {}
    record Details(Object pokemon, Class<?> pokemonType, int level, boolean shiny, Look look) {}
    private record Accessor(Method pokemon, Method level, Method shiny, Method species, Method aspects) {}
    private final Map<Class<?>, Accessor> accessors = new HashMap<>();
    private final Map<Class<?>, Method> factories = new HashMap<>();
    private final String factoryClass;

    CobblemonAccess() { this("com.cobblemon.mod.common.item.PokemonItem"); }
    CobblemonAccess(String factoryClass) { this.factoryClass = factoryClass; }

    Details read(Object entity) throws ReflectiveOperationException {
        Accessor a = accessors.get(entity.getClass());
        if (a == null) {
            Method pokemon = entity.getClass().getMethod("getPokemon");
            Class<?> type = pokemon.getReturnType();
            a = new Accessor(pokemon, type.getMethod("getLevel"), type.getMethod("getShiny"),
                    type.getMethod("getSpecies"), type.getMethod("getAspects"));
            accessors.put(entity.getClass(), a);
        }
        Object pokemon = a.pokemon.invoke(entity);
        int level = ((Number) a.level.invoke(pokemon)).intValue();
        boolean shiny = Boolean.TRUE.equals(a.shiny.invoke(pokemon));
        Set<?> aspects = Set.copyOf((Set<?>) a.aspects.invoke(pokemon));
        return new Details(pokemon, a.pokemon.getReturnType(), level, shiny,
                new Look(a.species.invoke(pokemon), aspects, shiny));
    }

    Object icon(Details data) throws ReflectiveOperationException {
        Method factory = factories.get(data.pokemonType);
        if (factory == null) {
            Class<?> type = Class.forName(factoryClass, true, data.pokemonType.getClassLoader());
            factory = type.getMethod("from", data.pokemonType);
            factories.put(data.pokemonType, factory);
        }
        return factory.invoke(null, data.pokemon);
    }
}
