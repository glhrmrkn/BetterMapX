package dev.bettermapx;
import java.nio.file.*;
import java.util.*;
import java.io.IOException;

/** No game or graphics needed: exercises the production file codec and overlay ownership. */
public final class AtlasDataTest {
    private static void check(boolean ok,String name){if(!ok)throw new AssertionError(name);}
    public static void main(String[] args)throws Exception{
        RadarDataTest.run();
        Path dir=Files.createTempDirectory("bettermapx-test");
        try{
            Path f=dir.resolve("tile.bmx");int[] p=new int[256],h=new int[256];String[] b=new String[256];
            for(int i=0;i<256;i++){p[i]=0xff000000|i*65793;h[i]=i-64;b[i]=i%2==0?"minecraft:plains":"minecraft:ocean";}p[0]=0;h[0]=Integer.MIN_VALUE;b[0]=null;
            TileCodec.write(f,p,h,b);var copy=TileCodec.read(f);
            check(Arrays.equals(p,copy.pixels()) && Arrays.equals(h,copy.heights()) && Arrays.equals(b,copy.biomes()),"round trip including biome palette, unknown and negative Y");
            p[255]=0xffed1234;TileCodec.write(f,p,h,b);check(TileCodec.read(f).pixels()[255]==p[255],"atomic replacement");
            Files.write(f,new byte[]{1,2,3});try{TileCodec.read(f);throw new AssertionError("accepted truncated tile");}catch(IOException expected){}
            check(TileCodec.abgr(0xff112233)==0xff332211,"ARGB to ABGR");
            check(TileCodec.abgr(0)==0,"unknown stays transparent");
            var a=new OverlayApi.Area("minecraft:overworld",-16,-16,-1,-1,-64,32,true,0x55d580ea,0x99c76f75);
            OverlayApi.replace("hunter",List.of(a));OverlayApi.replace("other",List.of(a));OverlayApi.remove("hunter");
            check(OverlayApi.all().size()==1,"owner isolation");OverlayApi.replace("other",List.of());
            check(OverlayApi.all().stream().allMatch(List::isEmpty),"selection clears atomically");
            try{new OverlayApi.Area("x",5,0,4,0,0,1,true,0,0);throw new AssertionError("accepted inverted area");}catch(IllegalArgumentException expected){}
            System.out.println("PASS: 8 atlas data checks");
        }finally{try(var paths=Files.walk(dir)){for(Path f:paths.sorted(Comparator.reverseOrder()).toList())Files.deleteIfExists(f);}}
    }
}
