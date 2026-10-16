package ua.meekeyn.requiemunofficial.mixin;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.targeting.TargetingConditions;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import ua.meekeyn.requiemunofficial.common.soul.VagrantRules;

@Mixin(TargetingConditions.class)
public abstract class TargetingConditionsMixin {

    @Inject(method = "test", at = @At("HEAD"), cancellable = true)
    private void requiemunofficial$ignoreFreeVagrantTargets(LivingEntity attacker,
                                                            LivingEntity target,
                                                            CallbackInfoReturnable<Boolean> cir) {
        if (VagrantRules.shouldBeIgnoredByOrdinaryTargeting(target)) {
            cir.setReturnValue(false);
        }
    }
}
