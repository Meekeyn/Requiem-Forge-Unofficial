package ua.meekeyn.requiemunofficial.mixin;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.Fox;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import ua.meekeyn.requiemunofficial.common.possession.PossessionManager;

@Mixin(Fox.class)
public abstract class FoxMixin {

    @Unique
    private boolean requiemunofficial$wasPossessed;

    @Shadow
    public abstract void setIsCrouching(boolean crouching);

    @Shadow
    abstract void setSleeping(boolean sleeping);

    @Shadow
    abstract void clearStates();

    @Inject(method = "tick", at = @At("HEAD"))
    private void requiemunofficial$syncPossessedFoxState(CallbackInfo ci) {
        Fox self = (Fox) (Object) this;
        boolean possessed = PossessionManager.getPossessorId((LivingEntity) (Object) this) != null;
        if (!possessed) {
            requiemunofficial$wasPossessed = false;
            return;
        }

        if (!requiemunofficial$wasPossessed) {
            clearStates();
            requiemunofficial$wasPossessed = true;
        }
        setIsCrouching(self.isShiftKeyDown());
    }
}
