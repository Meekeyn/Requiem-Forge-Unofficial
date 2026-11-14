package ua.meekeyn.requiemunofficial.mixin;

import net.minecraft.world.entity.monster.EnderMan;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import ua.meekeyn.requiemunofficial.common.possession.PossessionManager;

@Mixin(EnderMan.class)
public abstract class EnderManMixin {

    // Ванільні самотелепорти (від стріли, води, шкоди) рухають лише сутність-тіло, а режим
    // «тіло = гравець» повертає її назад, тож ефекту не було. Для одержимого ендермена
    // телепортуємо тіло гравця.
    @Inject(method = "teleport(DDD)Z", at = @At("HEAD"), cancellable = true)
    private void requiemunofficial$teleportPossessedBody(double x, double y, double z,
                                                         CallbackInfoReturnable<Boolean> cir) {
        if (PossessionManager.redirectPossessedTeleport((EnderMan) (Object) this, x, y, z)) {
            cir.setReturnValue(true);
        }
    }
}
