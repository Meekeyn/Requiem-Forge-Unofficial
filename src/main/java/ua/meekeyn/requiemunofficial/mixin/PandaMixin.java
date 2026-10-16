package ua.meekeyn.requiemunofficial.mixin;

import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.animal.Panda;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import ua.meekeyn.requiemunofficial.common.possession.PossessionManager;

@Mixin(Panda.class)
public abstract class PandaMixin {

    @Unique
    private boolean requiemunofficial$lastPossessedSneaking;

    @Shadow
    public abstract boolean isFood(ItemStack stack);

    @Shadow
    public abstract void setOnBack(boolean onBack);

    @ModifyArg(
            method = "handleEating",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/entity/animal/Panda;eat(Z)V",
                    ordinal = 0
            )
    )
    private boolean requiemunofficial$onlyEatHeldFood(boolean eating) {
        Panda self = (Panda) (Object) this;
        ItemStack held = self.getItemBySlot(EquipmentSlot.MAINHAND);
        return eating && (isFood(held) || held.is(Blocks.CAKE.asItem()));
    }

    @Inject(method = "tick", at = @At("HEAD"))
    private void requiemunofficial$syncPossessedScaredState(CallbackInfo ci) {
        Panda self = (Panda) (Object) this;
        boolean possessed = PossessionManager.getPossessorId(self) != null;
        if (possessed) {
            setOnBack(false);
        }

        boolean sneaking = possessed && self.isShiftKeyDown();
        if (requiemunofficial$lastPossessedSneaking != sneaking) {
            requiemunofficial$lastPossessedSneaking = sneaking;
            self.refreshDimensions();
        }
    }

    @Inject(method = "isScared", at = @At("HEAD"), cancellable = true)
    private void requiemunofficial$possessedSneakingScaresPanda(CallbackInfoReturnable<Boolean> cir) {
        Panda self = (Panda) (Object) this;
        if (PossessionManager.getPossessorId(self) != null && self.isShiftKeyDown()) {
            cir.setReturnValue(true);
        }
    }
}
