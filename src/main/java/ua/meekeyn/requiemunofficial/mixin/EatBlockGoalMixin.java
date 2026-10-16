package ua.meekeyn.requiemunofficial.mixin;

import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.EatBlockGoal;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import ua.meekeyn.requiemunofficial.common.soul.EntitySoulManager;

@Mixin(EatBlockGoal.class)
public abstract class EatBlockGoalMixin {

    @Shadow
    @Final
    private Mob mob;

    @Inject(method = "canUse", at = @At("HEAD"), cancellable = true)
    private void requiemunofficial$inertBodiesDoNotStartEatingGrass(CallbackInfoReturnable<Boolean> cir) {
        if (EntitySoulManager.isInertSoullessBody(this.mob)) {
            cir.setReturnValue(false);
        }
    }

    @Inject(method = "canContinueToUse", at = @At("HEAD"), cancellable = true)
    private void requiemunofficial$inertBodiesStopEatingGrass(CallbackInfoReturnable<Boolean> cir) {
        if (EntitySoulManager.isInertSoullessBody(this.mob)) {
            cir.setReturnValue(false);
        }
    }
}
