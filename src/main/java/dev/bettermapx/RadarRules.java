package dev.bettermapx;

final class RadarRules {
    static boolean isPokemon(String namespace, String path) {
        return namespace.equals("cobblemon") && path.equals("pokemon");
    }
    static boolean inLayer(int layer, double y, int range) {
        return layer == Integer.MAX_VALUE || Math.abs(y - layer) <= range;
    }
    static int screen(double coordinate, double center, double zoom, int origin, int extent) {
        return origin + (int) Math.round(extent / 2.0 + (coordinate - center) * zoom);
    }
}
