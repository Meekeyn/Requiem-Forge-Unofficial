package ua.meekeyn.requiemunofficial.mixin;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import ua.meekeyn.requiemunofficial.common.possession.OverridablePossessedItemStack;
import ua.meekeyn.requiemunofficial.common.possession.PossessedItemManager;

@Mixin(ItemStack.class)
public abstract class ItemStackMixin implements OverridablePossessedItemStack {
    private Integer requiemunofficial$possessedUseDuration;

    @Override
    public void requiemunofficial$clearPossessedUseDuration() {
        this.requiemunofficial$possessedUseDuration = null;
    }

    @Override
    public void requiemunofficial$overridePossessedUseDuration(int ticks) {
        this.requiemunofficial$possessedUseDuration = ticks;
    }

    @Inject(method = "getUseDuration", at = @At("HEAD"), cancellable = true)
    private void requiemunofficial$getPossessedUseDuration(CallbackInfoReturnable<Integer> cir) {
        if (this.requiemunofficial$possessedUseDuration != null) {
            cir.setReturnValue(this.requiemunofficial$possessedUseDuration);
        }
    }

    @Inject(method = "finishUsingItem", at = @At("HEAD"), cancellable = true)
    private void requiemunofficial$finishPossessedUse(Level level,
                                                      LivingEntity user,
                                                      CallbackInfoReturnable<ItemStack> cir) {
        if (this.requiemunofficial$possessedUseDuration != null) {
            this.requiemunofficial$possessedUseDuration = null;
        }

        InteractionHand hand = user.getUsedItemHand();
        PossessedItemManager.finishPossessedItemUse(level, user, (ItemStack) (Object) this, hand)
                .ifPresent(cir::setReturnValue);
    }
}
