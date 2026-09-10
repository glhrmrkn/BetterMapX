package dev.bettermapx;

import com.google.gson.*;
import java.nio.file.*;
import java.io.*;
import java.util.*;

public final class Waypoints {
    public record Point(String id,String name,String dimension,int x,int y,int z,int color,String icon) {}
    private final List<Point> points=new ArrayList<>();
    private Path file;
    private List<Point> external=List.of();
    public void external(List<Point> points){external=List.copyOf(points);}
    public void attach(Path folder){
        Path next=folder==null?null:folder.resolve("waypoints.json");
        if(Objects.equals(file,next))return;
        points.clear();external=List.of();file=next;
        if(file!=null && Files.exists(file))try(Reader r=Files.newBufferedReader(file)){
            Point[] found=new Gson().fromJson(r,Point[].class);
            if(found!=null)for(Point p:found)if(p!=null && p.id()!=null && p.name()!=null && p.dimension()!=null && p.icon()!=null && points.size()<1024)points.add(p);
        }catch(IOException | JsonParseException e){BetterMapX.LOG.error("Could not read waypoints; existing file preserved until next edit",e);}
    }
    public List<Point> all(){List<Point> result=new ArrayList<>(points);result.addAll(external);return List.copyOf(result);}
    public void put(Point p){points.removeIf(old->old.id().equals(p.id()));points.add(p);save();}
    public void remove(Point p){points.removeIf(old->old.id().equals(p.id()));save();}
    private void save(){
        if(file==null)return;
        try{
            Files.createDirectories(file.getParent());Path tmp=file.resolveSibling("waypoints.json.tmp");
            try(Writer w=Files.newBufferedWriter(tmp)){new GsonBuilder().setPrettyPrinting().create().toJson(points,w);}
            try{Files.move(tmp,file,StandardCopyOption.REPLACE_EXISTING,StandardCopyOption.ATOMIC_MOVE);}catch(AtomicMoveNotSupportedException e){Files.move(tmp,file,StandardCopyOption.REPLACE_EXISTING);}
        }catch(IOException e){BetterMapX.LOG.error("Could not save waypoints",e);}
    }
}
