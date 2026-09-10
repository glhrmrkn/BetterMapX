package dev.bettermapx.mixin;

import dev.bettermapx.BetterMapX;
import net.minecraft.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Entity.class)
public abstract class ColorMixin {
    @Inject(method="getTeamColorValue",at=@At("RETURN"),cancellable=true)
    private void bettermapx$color(CallbackInfoReturnable<Integer> result){
        if(BetterMapX.INSTANCE==null)return;
        Integer color=BetterMapX.INSTANCE.glowColor((Entity)(Object)this);
        if(color!=null)result.setReturnValue(color&0xffffff);
    }
}
