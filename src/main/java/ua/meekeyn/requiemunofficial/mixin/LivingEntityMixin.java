package ua.meekeyn.requiemunofficial.mixin;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import ua.meekeyn.requiemunofficial.common.effect.StickyEffectReapplicationManager;
import ua.meekeyn.requiemunofficial.common.possession.PossessionDetectionManager;
import ua.meekeyn.requiemunofficial.common.possession.PossessionManager;
import ua.meekeyn.requiemunofficial.common.soul.VagrantRules;

@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin {

    @Inject(method = "isPushable", at = @At("HEAD"), cancellable = true)
    private void requiemunofficial$vagrantsAreNotPushable(CallbackInfoReturnable<Boolean> cir) {
        LivingEntity self = (LivingEntity) (Object) this;
        if (VagrantRules.isRestrictedVagrantEntity(self)) {
            cir.setReturnValue(false);
        }
    }

    @Inject(method = "push(Lnet/minecraft/world/entity/Entity;)V", at = @At("HEAD"), cancellable = true)
    private void requiemunofficial$cancelVagrantEntityPush(Entity other, CallbackInfo ci) {
        Entity self = (Entity) (Object) this;
        if (VagrantRules.isRestrictedVagrantEntity(self) || VagrantRules.isRestrictedVagrantEntity(other)) {
            ci.cancel();
        }
    }

    @Inject(method = "push(Lnet/minecraft/world/entity/Entity;)V", at = @At("RETURN"))
    private void requiemunofficial$detectPossessedBump(Entity other, CallbackInfo ci) {
        LivingEntity self = (LivingEntity) (Object) this;
        if (!self.level().isClientSide() && other instanceof Mob sensor) {
            PossessionDetectionManager.attemptDetection(
                    self,
                    sensor,
                    PossessionDetectionManager.DetectionReason.BUMP
            );
        }
    }

    @Inject(method = "onEffectRemoved", at = @At("RETURN"))
    private void requiemunofficial$reapplyStickyEffects(MobEffectInstance effect, CallbackInfo ci) {
        StickyEffectReapplicationManager.onEffectRemoved((LivingEntity) (Object) this, effect);
    }

    @Inject(method = "aiStep", at = @At("HEAD"))
    private void requiemunofficial$preparePossessedHostInput(CallbackInfo ci) {
        LivingEntity self = (LivingEntity) (Object) this;
        if (!self.level().isClientSide()) {
            PossessionManager.preparePossessedHostForAiStep(self);
        }
    }

    @Inject(method = "aiStep", at = @At("RETURN"))
    private void requiemunofficial$restorePossessedHostLookAfterAi(CallbackInfo ci) {
        LivingEntity self = (LivingEntity) (Object) this;
        if (!self.level().isClientSide()) {
            PossessionManager.finishPossessedHostAiStep(self);
        }
    }

    @Redirect(method = "aiStep", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/entity/LivingEntity;travel(Lnet/minecraft/world/phys/Vec3;)V"))
    private void requiemunofficial$usePossessedTravelInput(LivingEntity self, Vec3 travelVector) {
        ua.meekeyn.requiemunofficial.common.possession.PossessionMovementController.travel(self, travelVector);
    }
}
