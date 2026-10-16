package ua.meekeyn.requiemunofficial.mixin;

import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import ua.meekeyn.requiemunofficial.common.diagnostic.VagrantDiagnostics;
import ua.meekeyn.requiemunofficial.common.soul.VagrantRules;

@Mixin(ServerGamePacketListenerImpl.class)
public abstract class ServerGamePacketListenerDiagnosticsMixin {
    @Shadow public ServerPlayer player;
    @Shadow private int awaitingTeleport;

    @Unique private AABB requiemunofficial$moveOldAabb;
    @Unique private Vec3 requiemunofficial$moveBeforePos;
    @Unique private Vec3 requiemunofficial$moveRequestedPos;
    @Unique private Vec3 requiemunofficial$moveBeforeDelta;
    @Unique private int requiemunofficial$moveAwaitingTeleport;
    @Unique private Boolean requiemunofficial$lastNewCollision;

    @Inject(method = "handleMovePlayer", at = @At("HEAD"))
    private void requiemunofficial$captureMoveStart(ServerboundMovePlayerPacket packet, CallbackInfo ci) {
        if (!VagrantDiagnostics.enabled() || !VagrantRules.isRestricted(this.player)) {
            return;
        }
        this.requiemunofficial$moveOldAabb = this.player.getBoundingBox();
        this.requiemunofficial$moveBeforePos = this.player.position();
        this.requiemunofficial$moveBeforeDelta = this.player.getDeltaMovement();
        this.requiemunofficial$moveRequestedPos = new Vec3(
                packet.getX(this.player.getX()),
                packet.getY(this.player.getY()),
                packet.getZ(this.player.getZ())
        );
        this.requiemunofficial$moveAwaitingTeleport = this.awaitingTeleport;
        this.requiemunofficial$lastNewCollision = null;
    }

    @Inject(method = "handleMovePlayer", at = @At("RETURN"))
    private void requiemunofficial$logMoveEnd(ServerboundMovePlayerPacket packet, CallbackInfo ci) {
        if (!VagrantDiagnostics.enabled()
                || !VagrantRules.isRestricted(this.player)
                || this.requiemunofficial$moveBeforePos == null
                || this.requiemunofficial$moveRequestedPos == null) {
            return;
        }

        VagrantDiagnostics.logServerMove(
                "handleMovePlayerReturn",
                this.player,
                this.requiemunofficial$moveOldAabb,
                this.requiemunofficial$moveRequestedPos,
                this.requiemunofficial$moveBeforePos,
                this.player.position(),
                this.requiemunofficial$moveBeforeDelta,
                this.player.getDeltaMovement(),
                this.requiemunofficial$moveAwaitingTeleport,
                this.awaitingTeleport,
                this.requiemunofficial$lastNewCollision,
                "movedTooQuicklyOrWronglyCandidate=" + (this.awaitingTeleport != this.requiemunofficial$moveAwaitingTeleport)
        );
    }

    @Inject(method = "isPlayerCollidingWithAnythingNew", at = @At("RETURN"))
    private void requiemunofficial$logNewCollision(LevelReader level,
                                                   AABB oldAabb,
                                                   double x,
                                                   double y,
                                                   double z,
                                                   CallbackInfoReturnable<Boolean> cir) {
        if (!VagrantDiagnostics.enabled() || !VagrantRules.isRestricted(this.player)) {
            return;
        }
        this.requiemunofficial$lastNewCollision = cir.getReturnValue();
        VagrantDiagnostics.logServerMove(
                "isPlayerCollidingWithAnythingNew",
                this.player,
                oldAabb,
                new Vec3(x, y, z),
                this.requiemunofficial$moveBeforePos,
                this.player.position(),
                this.requiemunofficial$moveBeforeDelta,
                this.player.getDeltaMovement(),
                this.requiemunofficial$moveAwaitingTeleport,
                this.awaitingTeleport,
                cir.getReturnValue(),
                "vanillaNewCollisionResult=" + cir.getReturnValue()
        );
    }
}
