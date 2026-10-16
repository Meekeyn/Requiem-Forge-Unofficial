package ua.meekeyn.requiemunofficial.mixin;

import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.Goal;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import ua.meekeyn.requiemunofficial.common.possession.PossessionManager;

@Mixin(Mob.class)
public abstract class MobMixin {

    @Inject(method = "serverAiStep", at = @At("HEAD"), cancellable = true)
    private void requiemunofficial$skipAutonomousAiWhilePossessed(CallbackInfo ci) {
        Mob self = (Mob) (Object) this;
        if (PossessionManager.getPossessorId(self) != null) {
            ci.cancel();
        }
    }

    // A ridden spider carries a possessed skeleton whose Mob-ness would otherwise clear the spider's
    // MOVE/JUMP/LOOK control flags (Mob#getControllingPassenger returns the Mob rider). Keep them
    // enabled so the spider's own chase/flee AI can still drive it while the rider is aboard.
    @Inject(method = "updateControlFlags", at = @At("HEAD"), cancellable = true)
    private void requiemunofficial$preserveRiddenSpiderAiFlags(CallbackInfo ci) {
        Mob self = (Mob) (Object) this;
        if (PossessionManager.shouldPreserveVehicleAi(self)) {
            self.goalSelector.setControlFlag(Goal.Flag.MOVE, true);
            self.goalSelector.setControlFlag(Goal.Flag.JUMP, true);
            self.goalSelector.setControlFlag(Goal.Flag.LOOK, true);
            ci.cancel();
        }
    }
}
