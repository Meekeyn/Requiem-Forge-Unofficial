package ua.meekeyn.requiemunofficial.mixin;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.WeightedPressurePlateBlock;
import net.minecraft.world.phys.AABB;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import ua.meekeyn.requiemunofficial.common.soul.VagrantRules;

@Mixin(WeightedPressurePlateBlock.class)
public abstract class WeightedPressurePlateBlockMixin {

    private static final AABB REQUIEM_TOUCH_AABB = new AABB(0.125D, 0.0D, 0.125D, 0.875D, 0.25D, 0.875D);

    @Shadow
    @Final
    private int maxWeight;

    @Inject(method = "getSignalStrength", at = @At("HEAD"), cancellable = true)
    private void requiemunofficial$ignoreVagrantPlayers(Level level, BlockPos pos,
                                                        CallbackInfoReturnable<Integer> cir) {
        cir.setReturnValue(VagrantRules.weightedPressureSignal(
                level.getEntities((Entity) null, REQUIEM_TOUCH_AABB.move(pos), entity -> true),
                this.maxWeight
        ));
    }
}
