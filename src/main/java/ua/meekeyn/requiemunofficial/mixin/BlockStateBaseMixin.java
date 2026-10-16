package ua.meekeyn.requiemunofficial.mixin;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockBehaviour;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import ua.meekeyn.requiemunofficial.common.soul.VagrantRules;

@Mixin(BlockBehaviour.BlockStateBase.class)
public abstract class BlockStateBaseMixin {

    @Inject(method = "entityInside", at = @At("HEAD"), cancellable = true)
    private void requiemunofficial$cancelVagrantBlockCollisionEffects(Level level,
                                                                      BlockPos pos,
                                                                      Entity entity,
                                                                      CallbackInfo ci) {
        if (entity instanceof Player player && VagrantRules.shouldSuppressBlockCollision(player)) {
            ci.cancel();
        }
    }
}
