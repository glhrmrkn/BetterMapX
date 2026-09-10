package dev.bettermapx;

import java.io.*;
import java.nio.file.*;
import java.util.*;

/** Versioned binary tile format. Writes never replace the valid file until complete. */
final class TileCodec {
    private static final int V1=0x424d5801,V2=0x424d5802;
    record Data(int[] pixels,int[] heights,String[] biomes) {}

    static Data read(Path f)throws IOException{
        try(DataInputStream in=new DataInputStream(new BufferedInputStream(Files.newInputStream(f)))){
            int version=in.readInt();
            if(version!=V1 && version!=V2)throw new IOException("Invalid tile version");
            int[] p=new int[256],h=new int[256];
            for(int i=0;i<256;i++){p[i]=in.readInt();h[i]=in.readInt();}
            if(version==V1)return new Data(p,h,new String[256]);
            int paletteSize=in.readUnsignedShort();
            if(paletteSize>512)throw new IOException("Invalid biome palette");
            String[] palette=new String[paletteSize];
            for(int i=0;i<paletteSize;i++)palette[i]=in.readUTF();
            String[] biomes=new String[256];
            for(int i=0;i<256;i++){
                int idx=in.readUnsignedShort();
                if(idx!=0xffff){if(idx>=paletteSize)throw new IOException("Invalid biome index");biomes[i]=palette[idx];}
            }
            return new Data(p,h,biomes);
        }
    }

    static void write(Path f,int[] p,int[] h,String[] biomes)throws IOException{
        if(p.length!=256 || h.length!=256 || biomes.length!=256)throw new IllegalArgumentException("A chunk contains 256 columns");
        Files.createDirectories(f.getParent());Path temp=f.resolveSibling(f.getFileName()+".tmp");
        LinkedHashMap<String,Integer> paletteMap=new LinkedHashMap<>();
        for(String biome:biomes)if(biome!=null && !biome.isBlank())paletteMap.computeIfAbsent(biome,k->paletteMap.size());
        if(paletteMap.size()>512)throw new IOException("Biome palette overflow");
        try(DataOutputStream out=new DataOutputStream(new BufferedOutputStream(Files.newOutputStream(temp)))){
            out.writeInt(V2);
            for(int i=0;i<256;i++){out.writeInt(p[i]);out.writeInt(h[i]);}
            out.writeShort(paletteMap.size());
            for(String biome:paletteMap.keySet())out.writeUTF(biome);
            for(String biome:biomes){Integer idx=biome==null?null:paletteMap.get(biome);out.writeShort(idx==null?0xffff:idx);}
        }
        try{Files.move(temp,f,StandardCopyOption.REPLACE_EXISTING,StandardCopyOption.ATOMIC_MOVE);}catch(AtomicMoveNotSupportedException e){Files.move(temp,f,StandardCopyOption.REPLACE_EXISTING);}
    }
    static int abgr(int argb){return(argb&0xff00ff00)|((argb>>16)&255)|((argb&255)<<16);}
}
