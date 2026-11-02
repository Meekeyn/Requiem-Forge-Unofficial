package ua.meekeyn.requiemunofficial.mixin;

import net.minecraft.core.NonNullList;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import ua.meekeyn.requiemunofficial.common.possession.PossessionInventoryManager;

/**
 * Підбір і повернення предметів в інвентар не повинні потрапляти в слоти, яких тіло не має
 * (аналог InventoryLimiter beta.16): інакше фантом чи голем "носить" речі в заблокованих слотах.
 */
@Mixin(Inventory.class)
public abstract class InventoryMixin {

    @Shadow
    @Final
    public Player player;

    @Shadow
    @Final
    public NonNullList<ItemStack> items;

    @Shadow
    @Final
    public NonNullList<ItemStack> offhand;

    @Shadow
    public int selected;

    @Shadow
    private boolean hasRemainingSpaceForItem(ItemStack destination, ItemStack origin) {
        throw new AssertionError();
    }

    @Inject(method = "getFreeSlot", at = @At("HEAD"), cancellable = true)
    private void requiemunofficial$limitFreeSlot(CallbackInfoReturnable<Integer> cir) {
        LivingEntity host = PossessionInventoryManager.getLimitingHost(this.player);
        if (host == null) {
            return;
        }
        for (int slot = 0; slot < this.items.size(); slot++) {
            if (this.items.get(slot).isEmpty() && PossessionInventoryManager.isSlotAccessible(host, slot)) {
                cir.setReturnValue(slot);
                return;
            }
        }
        cir.setReturnValue(-1);
    }

    @Inject(method = "getSlotWithRemainingSpace", at = @At("HEAD"), cancellable = true)
    private void requiemunofficial$limitSlotWithRemainingSpace(ItemStack stack, CallbackInfoReturnable<Integer> cir) {
        LivingEntity host = PossessionInventoryManager.getLimitingHost(this.player);
        if (host == null) {
            return;
        }
        if (PossessionInventoryManager.isSlotAccessible(host, this.selected)
                && this.hasRemainingSpaceForItem(this.items.get(this.selected), stack)) {
            cir.setReturnValue(this.selected);
            return;
        }
        if (PossessionInventoryManager.isSlotAccessible(host, Inventory.SLOT_OFFHAND)
                && this.hasRemainingSpaceForItem(this.offhand.get(0), stack)) {
            cir.setReturnValue(Inventory.SLOT_OFFHAND);
            return;
        }
        for (int slot = 0; slot < this.items.size(); slot++) {
            if (PossessionInventoryManager.isSlotAccessible(host, slot)
                    && this.hasRemainingSpaceForItem(this.items.get(slot), stack)) {
                cir.setReturnValue(slot);
                return;
            }
        }
        cir.setReturnValue(-1);
    }
}
