package dev.bettermapx;

import java.util.*;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.registry.RegistryKey;

/** Optional public-API bridge. Reads only the active Xaero world and waypoint set. */
final class XaeroBridge {
    private int ticks;
    private boolean warned;
    private final boolean installed=FabricLoader.getInstance().isModLoaded("xaerominimap");
    private static Object call(Object target,String method) throws ReflectiveOperationException {
        return target.getClass().getMethod(method).invoke(target);
    }
    void tick(BetterMapX mod){
        if(net.minecraft.client.MinecraftClient.getInstance().world==null){mod.waypoints.external(List.of());return;}
        if(!installed || ticks++%20!=0)return;
        List<Waypoints.Point> points=new ArrayList<>();
        try{
            Object session=Class.forName("xaero.common.XaeroMinimapSession").getMethod("getCurrentSession").invoke(null);
            if(session!=null){
                Object manager=call(session,"getWaypointsManager"), world=call(manager,"getCurrentWorld");
                if(world!=null){
                    String dimension=((RegistryKey<?>)call(world,"getDimId")).getValue().toString();
                    Object set=call(world,"getCurrentSet");
                    if(set!=null)for(Object p:(Iterable<?>)call(set,"getList")){
                        if(points.size()>=1024)break;
                        if(Boolean.TRUE.equals(call(p,"isDisabled")))continue;
                        int x=((Number)call(p,"getX")).intValue(),y=((Number)call(p,"getY")).intValue(),z=((Number)call(p,"getZ")).intValue();
                        String name=String.valueOf(call(p,"getName")),symbol=String.valueOf(call(p,"getSymbol"));
                        int color;
                        try{color=((Number)call(call(p,"getWaypointColor"),"getHex")).intValue();}
                        catch(NoSuchMethodException old){
                            int[] palette={0,0x0000aa,0x00aa00,0x00aaaa,0xaa0000,0xaa00aa,0xffaa00,0xaaaaaa,0x555555,0x5555ff,0x55ff55,0x55ffff,0xff5555,0xff55ff,0xffff55,0xffffff};
                            color=palette[Math.floorMod(((Number)call(p,"getColor")).intValue(),16)];
                        }
                        points.add(new Waypoints.Point("xaero:"+points.size(),name+" [Xaero]",dimension,x,y,z,color|0xff000000,symbol));
                    }
                }
            }
        }catch(ReflectiveOperationException|RuntimeException|LinkageError error){
            if(!warned){warned=true;BetterMapX.LOG.warn("Xaero waypoint bridge unavailable for this version",error);}
        }
        mod.waypoints.external(points);
    }
}
