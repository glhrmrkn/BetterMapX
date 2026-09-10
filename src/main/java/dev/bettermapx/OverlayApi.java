package dev.bettermapx;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/** Public integration API. Coordinates are world blocks. Colors are ARGB.
 * Owners replace their entire selection atomically; never touch another owner's entries.
 * Surface overlays require surface=true; underground overlays also filter by Y interval.
 */
public final class OverlayApi {
    public record Area(String dimension, int minX, int minZ, int maxX, int maxZ,
                       int minY, int maxY, boolean surface, int fill, int border) {
        public Area {
            Objects.requireNonNull(dimension);
            if(minX>maxX || minZ>maxZ || minY>maxY) throw new IllegalArgumentException("Inverted area");
        }
    }
    private static final Map<String,List<Area>> AREAS = new ConcurrentHashMap<>();
    private OverlayApi() {}
    public static void replace(String owner,List<Area> areas){
        if(areas.size()>4096) throw new IllegalArgumentException("Maximum 4096 areas per owner");
        AREAS.put(Objects.requireNonNull(owner),List.copyOf(areas));
    }
    public static void remove(String owner){AREAS.remove(owner);}
    public static Collection<List<Area>> all(){return List.copyOf(AREAS.values());}
    static void clear(){AREAS.clear();}
}
