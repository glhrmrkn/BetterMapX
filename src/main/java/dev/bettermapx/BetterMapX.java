package dev.bettermapx;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.*;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import org.lwjgl.glfw.GLFW;
import org.slf4j.*;

public final class BetterMapX implements ClientModInitializer {
    public static final Logger LOG=LoggerFactory.getLogger("BetterMapX");
    public Config config;public Atlas atlas;public Waypoints waypoints;public MapRenderer renderer;
    PokemonRadar radar;GlowRenderer glow;SpawnAreas spawnAreas;
    public static BetterMapX INSTANCE;
    public KeyBinding map,expand;
    private boolean zHeld;
    private final XaeroBridge xaero=new XaeroBridge();
    public Integer glowColor(net.minecraft.entity.Entity entity){return radar==null?null:radar.glowColor(entity);}
    @Override public void onInitializeClient(){
        MinecraftClient mc=MinecraftClient.getInstance();
        config=new Config();atlas=new Atlas(mc,config);waypoints=new Waypoints();renderer=new MapRenderer(this);
        glow=new GlowRenderer();spawnAreas=new SpawnAreas(this);radar=new PokemonRadar(this);INSTANCE=this;
        expand=key("expand",GLFW.GLFW_KEY_X);
        map=key("map",GLFW.GLFW_KEY_M);KeyBinding toggle=key("toggle",GLFW.GLFW_KEY_B),waypoint=key("waypoint",GLFW.GLFW_KEY_N);
        ClientTickEvents.END_CLIENT_TICK.register(client->{
            atlas.tick();waypoints.attach(atlas.worldFolder());
            radar.tick(client);spawnAreas.tick();xaero.tick(this);
            // Let Fabric own the binding. The old physical-Z fallback was the
            // source of invalid-scancode handling on some keyboard layouts.
            while(expand.wasPressed()) if(client.player!=null) {
                if(client.currentScreen==null) client.setScreen(new MapScreen(this,true));
                else if(client.currentScreen instanceof MapScreen screen) screen.toggleExpanded();
            }
            while(toggle.wasPressed()){config.minimap=!config.minimap;config.save();}
            while(map.wasPressed())if(client.player!=null && client.currentScreen==null)client.setScreen(new MapScreen(this));
            while(waypoint.wasPressed())if(client.player!=null && client.currentScreen==null)client.setScreen(new WaypointScreen(this,null,null,client.player.getBlockX(),client.player.getBlockY(),client.player.getBlockZ()));
        });
        HudRenderCallback.EVENT.register((context,tickCounter)->renderer.hud(context));
        ClientLifecycleEvents.CLIENT_STOPPING.register(client->{spawnAreas.close();glow.close();atlas.shutdown();});
        LOG.info("BetterMapX 0.4.0-alpha initialized; Google Stitch design integration ready");
    }
    private KeyBinding key(String name,int code){return KeyBindingHelper.registerKeyBinding(new KeyBinding("key.bettermapx."+name,InputUtil.Type.KEYSYM,code,"category.bettermapx"));}
}
