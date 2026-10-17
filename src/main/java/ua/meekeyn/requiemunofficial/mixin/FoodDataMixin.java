package ua.meekeyn.requiemunofficial.mixin;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.food.FoodData;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import ua.meekeyn.requiemunofficial.common.possession.PossessionManager;

/**
 * Тіла, що не їдять (не regular eater), узагалі не користуються механікою голоду:
 * насичення не витрачається, здоров'я не регенерується з їжі й немає шкоди від голоду.
 * Це повторює HungerManagerMixin з beta.16, але рішення ухвалюється за тегом
 * requiemunofficial:behavior/regular_eaters, а не за MobType.UNDEAD чи instanceof Monster.
 */
@Mixin(FoodData.class)
public abstract class FoodDataMixin {

    @Inject(method = "tick", at = @At("HEAD"), cancellable = true)
    private void requiemunofficial$skipHungerForNonEaterBody(Player player, CallbackInfo ci) {
        if (!PossessionManager.shouldSkipHungerTick(player)) {
            return;
        }

        // Тіло-господар без механіки голоду тримає шкалу повною (як у beta.16). Безтілесна
        // блукаюча душа лише заморожує голод, щоб після виходу з тіла зберегти відновлене значення.
        if (PossessionManager.shouldForceFullHunger(player)) {
            FoodData self = (FoodData) (Object) this;
            self.setExhaustion(0.0F);
            self.setFoodLevel(20);
        }
        ci.cancel();
    }
}
