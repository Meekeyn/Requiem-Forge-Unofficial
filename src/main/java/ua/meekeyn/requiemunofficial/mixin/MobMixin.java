package ua.meekeyn.requiemunofficial.mixin;

import net.minecraft.world.entity.Mob;
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
}
