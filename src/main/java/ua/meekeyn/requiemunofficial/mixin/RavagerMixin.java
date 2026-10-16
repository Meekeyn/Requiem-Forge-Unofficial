package ua.meekeyn.requiemunofficial.mixin;

import net.minecraft.world.entity.monster.Ravager;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Ravager.class)
public abstract class RavagerMixin {

    @Unique
    private @Nullable Boolean requiemunofficial$clientHorizontalCollision;

    @Inject(method = "aiStep", at = @At("HEAD"))
    private void requiemunofficial$disableClientsideGriefing(CallbackInfo ci) {
        Ravager self = (Ravager) (Object) this;
        if (self.level().isClientSide() && self.horizontalCollision) {
            requiemunofficial$clientHorizontalCollision = true;
            self.horizontalCollision = false;
        }
    }

    @Inject(method = "aiStep", at = @At("RETURN"))
    private void requiemunofficial$restoreClientsideCollision(CallbackInfo ci) {
        if (requiemunofficial$clientHorizontalCollision != null) {
            ((Ravager) (Object) this).horizontalCollision = requiemunofficial$clientHorizontalCollision;
            requiemunofficial$clientHorizontalCollision = null;
        }
    }
}
