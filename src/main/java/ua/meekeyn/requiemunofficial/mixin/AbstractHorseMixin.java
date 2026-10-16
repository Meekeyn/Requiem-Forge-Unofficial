package ua.meekeyn.requiemunofficial.mixin;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobType;
import net.minecraft.world.entity.animal.horse.AbstractHorse;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import ua.meekeyn.requiemunofficial.common.possession.PossessionManager;

@Mixin(AbstractHorse.class)
public abstract class AbstractHorseMixin {

    @Inject(method = "isSaddled", at = @At("HEAD"), cancellable = true)
    private void requiemunofficial$undeadPossessedRiderCountsAsSaddled(CallbackInfoReturnable<Boolean> cir) {
        AbstractHorse self = (AbstractHorse) (Object) this;
        Entity passenger = self.getFirstPassenger();
        if (self.getMobType() != MobType.UNDEAD
                || !(passenger instanceof LivingEntity livingPassenger)
                || livingPassenger.getMobType() != MobType.UNDEAD) {
            return;
        }

        if (PossessionManager.getPossessorId(livingPassenger) != null) {
            cir.setReturnValue(true);
        }
    }
}
