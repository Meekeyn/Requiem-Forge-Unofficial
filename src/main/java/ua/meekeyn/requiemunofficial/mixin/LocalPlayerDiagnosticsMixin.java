package ua.meekeyn.requiemunofficial.mixin;

import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import ua.meekeyn.requiemunofficial.common.diagnostic.VagrantDiagnostics;
import ua.meekeyn.requiemunofficial.common.soul.VagrantRules;

@Mixin(LocalPlayer.class)
public abstract class LocalPlayerDiagnosticsMixin {
    @Unique private Vec3 requiemunofficial$pushBeforePos;
    @Unique private Vec3 requiemunofficial$pushBeforeDelta;
    @Unique private BlockPos requiemunofficial$pushBlockPos;
    @Unique private BlockState requiemunofficial$pushBlockState;
    @Unique private boolean requiemunofficial$pushStartSuffocates;

    @Inject(method = "moveTowardsClosestSpace", at = @At("HEAD"), cancellable = true)
    private void requiemunofficial$captureClosestSpaceStart(double x, double z, CallbackInfo ci) {
        LocalPlayer player = (LocalPlayer) (Object) this;
        if (VagrantRules.isRestricted(player) && VagrantRules.isInsidePhaseableBlock(player)) {
            ci.cancel();
            return;
        }
        if (!VagrantDiagnostics.enabled() || !VagrantRules.isRestricted(player)) {
            return;
        }
        this.requiemunofficial$pushBeforePos = player.position();
        this.requiemunofficial$pushBeforeDelta = player.getDeltaMovement();
        this.requiemunofficial$pushBlockPos = BlockPos.containing(x, player.getY(), z);
        this.requiemunofficial$pushBlockState = player.level().getBlockState(this.requiemunofficial$pushBlockPos);
        this.requiemunofficial$pushStartSuffocates = false;
    }

    @Inject(method = "moveTowardsClosestSpace", at = @At("RETURN"))
    private void requiemunofficial$logClosestSpaceEnd(double x, double z, CallbackInfo ci) {
        LocalPlayer player = (LocalPlayer) (Object) this;
        if (!VagrantDiagnostics.enabled()
                || !VagrantRules.isRestricted(player)
                || this.requiemunofficial$pushBeforePos == null) {
            return;
        }
        VagrantDiagnostics.logClientPush(
                player,
                "moveTowardsClosestSpaceReturn",
                this.requiemunofficial$pushBlockPos,
                this.requiemunofficial$pushBlockState,
                this.requiemunofficial$pushStartSuffocates,
                this.requiemunofficial$pushBeforePos,
                player.position(),
                this.requiemunofficial$pushBeforeDelta,
                player.getDeltaMovement()
        );
    }

    @Inject(method = "suffocatesAt", at = @At("RETURN"))
    private void requiemunofficial$logSuffocatesAt(BlockPos pos, CallbackInfoReturnable<Boolean> cir) {
        LocalPlayer player = (LocalPlayer) (Object) this;
        if (!VagrantDiagnostics.enabled() || !VagrantRules.isRestricted(player)) {
            return;
        }
        if (pos.equals(this.requiemunofficial$pushBlockPos)) {
            this.requiemunofficial$pushStartSuffocates = cir.getReturnValue();
        }
        VagrantDiagnostics.logClientSuffocationCheck(player, pos, cir.getReturnValue());
    }
}
