package ua.meekeyn.requiemunofficial.mixin;

import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import ua.meekeyn.requiemunofficial.common.possession.PossessionInventoryManager;

@Mixin(Slot.class)
public abstract class SlotMixin {

    @Shadow
    @Final
    public int index;

    @Shadow
    @Final
    public Container container;

    @Inject(method = "mayPlace", at = @At("HEAD"), cancellable = true)
    private void requiemunofficial$limitPossessedSlotPlacement(ItemStack stack, CallbackInfoReturnable<Boolean> cir) {
        if (!requiemunofficial$canUsePlayerSlot()) {
            cir.setReturnValue(false);
        }
    }

    @Inject(method = "mayPickup", at = @At("HEAD"), cancellable = true)
    private void requiemunofficial$limitPossessedSlotPickup(Player player, CallbackInfoReturnable<Boolean> cir) {
        if (!requiemunofficial$canUsePlayerSlot()) {
            cir.setReturnValue(false);
        }
    }

    @Inject(method = "isActive", at = @At("HEAD"), cancellable = true)
    private void requiemunofficial$hideLockedPossessedSlots(CallbackInfoReturnable<Boolean> cir) {
        if (!requiemunofficial$canUsePlayerSlot()) {
            cir.setReturnValue(false);
        }
    }

    private boolean requiemunofficial$canUsePlayerSlot() {
        if (!(this.container instanceof Inventory inventory)) {
            return true;
        }
        return PossessionInventoryManager.canAccessPlayerInventorySlot(inventory.player, this.index);
    }
}
