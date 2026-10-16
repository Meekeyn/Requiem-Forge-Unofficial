package ua.meekeyn.requiemunofficial.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.ScreenEffectRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Blaze;
import net.minecraft.world.level.block.state.BlockState;
import org.apache.commons.lang3.tuple.Pair;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.gen.Invoker;
import ua.meekeyn.requiemunofficial.common.soul.SoulManager;
import ua.meekeyn.requiemunofficial.common.soul.VagrantRules;
import ua.meekeyn.requiemunofficial.client.ClientPossessionRenderHelper;

@Mixin(ScreenEffectRenderer.class)
public abstract class ScreenEffectRendererMixin {

    @Invoker(value = "getOverlayBlock", remap = false)
    private static Pair<BlockState, BlockPos> requiemunofficial$getOverlayBlock(net.minecraft.world.entity.player.Player player) {
        throw new AssertionError();
    }

    @Redirect(
            method = "renderScreenEffect",
            remap = false,
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/renderer/ScreenEffectRenderer;getOverlayBlock(Lnet/minecraft/world/entity/player/Player;)Lorg/apache/commons/lang3/tuple/Pair;"
            )
    )
    private static Pair<BlockState, BlockPos> requiemunofficial$hideLeafViewBlockingOverlay(net.minecraft.world.entity.player.Player player) {
        Pair<BlockState, BlockPos> overlay = requiemunofficial$getOverlayBlock(player);
        if (overlay != null
                && VagrantRules.isRestricted(player)
                && VagrantRules.shouldSuppressCollisionWith(player, overlay.getLeft())) {
            return null;
        }
        return overlay;
    }

    @Redirect(
            method = "renderScreenEffect",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/player/LocalPlayer;isOnFire()Z"
            )
    )
    private static boolean requiemunofficial$usePossessedHostFireState(LocalPlayer player,
                                                                       Minecraft minecraft,
                                                                       PoseStack poseStack) {
        LivingEntity host = ClientPossessionRenderHelper.getPossessedHost(player);
        if (host != null) {
            return host.isOnFire() || host instanceof Blaze blaze
                    && ((BlazeAccessor) blaze).requiemunofficial$isCharged();
        }
        return player.isOnFire();
    }

    @ModifyArg(
            method = "renderFire",
            at = @At(value = "INVOKE",
                    target = "Lcom/mojang/blaze3d/vertex/VertexConsumer;color(FFFF)Lcom/mojang/blaze3d/vertex/VertexConsumer;"),
            index = 3
    )
    private static float requiemunofficial$thinControlledBlazeFire(float alpha) {
        LocalPlayer player = Minecraft.getInstance().player;
        LivingEntity host = player == null ? null : ClientPossessionRenderHelper.getPossessedHost(player);
        return host instanceof Blaze blaze
                && ((BlazeAccessor) blaze).requiemunofficial$isCharged()
                ? 0.2F : alpha;
    }
}
