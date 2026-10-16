package ua.meekeyn.requiemunofficial.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.LevelReader;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import ua.meekeyn.requiemunofficial.client.ClientPossessionRenderHelper;
import ua.meekeyn.requiemunofficial.common.soul.SoulState;
import ua.meekeyn.requiemunofficial.common.soul.SoulManager;

@Mixin(EntityRenderDispatcher.class)
public abstract class EntityRenderDispatcherMixin {

    @Inject(
            method = "render(Lnet/minecraft/world/entity/Entity;DDDFFLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V",
            at = @At("HEAD"),
            cancellable = true
    )
    private <E extends Entity> void requiemunofficial$hidePossessedSoulPlayer(E entity,
                                                                               double x,
                                                                               double y,
                                                                               double z,
                                                                               float rotationYaw,
                                                                               float partialTicks,
                                                                               PoseStack poseStack,
                                                                               MultiBufferSource buffer,
                                                                               int packedLight,
                                                                               CallbackInfo ci) {
        if (entity instanceof net.minecraft.client.player.AbstractClientPlayer player
                && ClientPossessionRenderHelper.shouldHideLocalSoulPlayer(player)) {
            ci.cancel();
            return;
        }

        if (ClientPossessionRenderHelper.shouldHideLocalPossessedHost(entity)) {
            ci.cancel();
        }
    }

    @Inject(method = "renderShadow", at = @At("HEAD"), cancellable = true)
    private static void requiemunofficial$hideVagrantShadow(PoseStack poseStack,
                                                            MultiBufferSource buffer,
                                                            Entity entity,
                                                            float weight,
                                                            float partialTicks,
                                                            LevelReader level,
                                                            float size,
                                                            CallbackInfo ci) {
        if (entity instanceof net.minecraft.world.entity.player.Player player
                && SoulManager.getState(player) == SoulState.VAGRANT) {
            ci.cancel();
        }
    }
}
