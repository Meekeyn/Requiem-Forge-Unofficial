package ua.meekeyn.requiemunofficial.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import ua.meekeyn.requiemunofficial.client.ClientPossessionFx;

/**
 * Фич-слои сущности рисуются через статический хелпер
 * {@code RenderLayer.renderColoredCutoutModel} (бета-16: метод
 * {@code FeatureRenderer.renderModel}). Во время анимации входа их нужно
 * отправлять в ту же цель {@code zoom_focus}, что и тело сущности.
 */
@Mixin(RenderLayer.class)
public abstract class RenderLayerZoomMixin {

    @Unique
    private static boolean requiemunofficial$goZoom;

    @Inject(
            method = "renderColoredCutoutModel(Lnet/minecraft/client/model/EntityModel;Lnet/minecraft/resources/ResourceLocation;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;ILnet/minecraft/world/entity/LivingEntity;FFF)V",
            at = @At("HEAD")
    )
    private static void requiemunofficial$captureEntity(EntityModel<?> model,
                                                        ResourceLocation texture,
                                                        PoseStack poseStack,
                                                        MultiBufferSource bufferSource,
                                                        int packedLight,
                                                        LivingEntity entity,
                                                        float red,
                                                        float green,
                                                        float blue,
                                                        CallbackInfo ci) {
        requiemunofficial$goZoom = entity != null && entity == ClientPossessionFx.getAnimationEntity();
    }

    @ModifyArg(
            method = "renderColoredCutoutModel(Lnet/minecraft/client/model/EntityModel;Lnet/minecraft/resources/ResourceLocation;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;ILnet/minecraft/world/entity/LivingEntity;FFF)V",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/renderer/MultiBufferSource;getBuffer(Lnet/minecraft/client/renderer/RenderType;)Lcom/mojang/blaze3d/vertex/VertexConsumer;"
            ),
            index = 0
    )
    private static RenderType requiemunofficial$zoomRenderType(RenderType base) {
        return requiemunofficial$goZoom ? ClientPossessionFx.getZoomFx(base) : base;
    }
}
