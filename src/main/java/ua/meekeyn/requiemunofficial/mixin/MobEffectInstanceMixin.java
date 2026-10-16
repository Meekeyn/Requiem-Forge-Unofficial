package ua.meekeyn.requiemunofficial.mixin;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import ua.meekeyn.requiemunofficial.common.effect.StickyMobEffect;

@Mixin(MobEffectInstance.class)
public abstract class MobEffectInstanceMixin {
    @Shadow
    private int duration;

    @Shadow
    public abstract MobEffect getEffect();

    @Inject(
            method = "tick",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/effect/MobEffectInstance;tickDownDuration()I")
    )
    private void requiemunofficial$freezeStickyDuration(LivingEntity entity,
                                                        Runnable onExpiration,
                                                        CallbackInfoReturnable<Boolean> cir) {
        MobEffect effect = getEffect();
        if (effect instanceof StickyMobEffect sticky && sticky.shouldFreezeDuration(entity)) {
            this.duration++;
        }
    }
}
