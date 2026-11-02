package ua.meekeyn.requiemunofficial.mixin;

import net.minecraft.world.entity.monster.Skeleton;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import ua.meekeyn.requiemunofficial.common.possession.PossessedEnvironment;

@Mixin(Skeleton.class)
public abstract class SkeletonMixin {

    // Одержиме тіло має NoAI, але замерзання в порошковому снігу (скелет -> зимогор) має тривати.
    @Redirect(method = "tick", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/entity/monster/Skeleton;isNoAi()Z"))
    private boolean requiemunofficial$convertWhilePossessed(Skeleton self) {
        return self.isNoAi() && !PossessedEnvironment.isPossessedHost(self);
    }

    // Тіло телепортується за гравцем без Entity#move, тож isInPowderSnow для нього не ставиться;
    // беремо стан власника, який стоїть у тому самому блоці.
    @Redirect(method = "tick", at = @At(value = "FIELD", opcode = org.objectweb.asm.Opcodes.GETFIELD,
            target = "Lnet/minecraft/world/entity/monster/Skeleton;isInPowderSnow:Z"))
    private boolean requiemunofficial$possessorPowderSnow(Skeleton self) {
        return self.isInPowderSnow
                || (PossessedEnvironment.isPossessedHost(self) && PossessedEnvironment.isPossessorInPowderSnow(self));
    }
}
