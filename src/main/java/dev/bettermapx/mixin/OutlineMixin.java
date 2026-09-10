package dev.bettermapx.mixin;

import dev.bettermapx.BetterMapX;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(MinecraftClient.class)
public abstract class OutlineMixin {
    @Inject(method="hasOutline",at=@At("RETURN"),cancellable=true)
    private void bettermapx$outline(Entity entity,CallbackInfoReturnable<Boolean> result){
        if(BetterMapX.INSTANCE!=null && BetterMapX.INSTANCE.glowColor(entity)!=null)result.setReturnValue(true);
    }
}
