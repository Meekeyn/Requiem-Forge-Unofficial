package ua.meekeyn.requiemunofficial.mixin;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.Cat;
import net.minecraft.world.entity.animal.Ocelot;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import ua.meekeyn.requiemunofficial.common.possession.PossessionManager;

@Mixin({Cat.class, Ocelot.class})
public abstract class CatLikeEntityMixin {

    @Inject(method = "customServerAiStep", at = @At("HEAD"), cancellable = true)
    private void requiemunofficial$keepPossessedSprintState(CallbackInfo ci) {
        if (PossessionManager.getPossessorId((LivingEntity) (Object) this) != null) {
            ci.cancel();
        }
    }
}
