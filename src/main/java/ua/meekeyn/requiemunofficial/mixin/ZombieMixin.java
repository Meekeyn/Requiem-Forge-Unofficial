package ua.meekeyn.requiemunofficial.mixin;

import net.minecraft.world.entity.monster.Zombie;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import ua.meekeyn.requiemunofficial.common.possession.PossessedEnvironment;

@Mixin(Zombie.class)
public abstract class ZombieMixin {

    // Одержиме тіло має NoAI, але ванільне перетворення під водою (зомбі -> утопленик,
    // кадавр -> зомбі) повинно тривати, як у beta.16.
    @Redirect(method = "tick", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/entity/monster/Zombie;isNoAi()Z"))
    private boolean requiemunofficial$convertWhilePossessed(Zombie self) {
        return self.isNoAi() && !PossessedEnvironment.isPossessedHost(self);
    }
}
