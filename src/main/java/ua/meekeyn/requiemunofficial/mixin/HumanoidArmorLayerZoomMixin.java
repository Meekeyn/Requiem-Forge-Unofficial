package ua.meekeyn.requiemunofficial.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.Model;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.layers.HumanoidArmorLayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ArmorItem;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import ua.meekeyn.requiemunofficial.client.ClientPossessionFx;

/**
 * Броня сущности во время анимации входа рисуется в цель {@code zoom_focus},
 * как и тело и фич-слои (бета-16: {@code ArmorFeatureRendererMixin} перенаправлял
 * слой через {@code ItemRenderer.getArmorGlintConsumer}; в 1.20.1 тот же слой
 * берёт буфер в {@code renderModel} и {@code renderGlint}).
 */
@Mixin(HumanoidArmorLayer.class)
public abstract class HumanoidArmorLayerZoomMixin {

    @Unique
    private static boolean requiemunofficial$goZoom;

    @Inject(
            method = "render(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;ILnet/minecraft/world/entity/LivingEntity;FFFFFF)V",
            at = @At("HEAD")
    )
    private void requiemunofficial$captureEntity(PoseStack poseStack,
                                                 MultiBufferSource bufferSource,
                                                 int packedLight,
                                                 LivingEntity entity,
                                                 float limbSwing,
                                                 float limbSwingAmount,
                                                 float partialTicks,
                                                 float netHeadYaw,
                                                 float headPitch,
                                                 float scale,
                                                 CallbackInfo ci) {
        requiemunofficial$goZoom = entity != null && entity == ClientPossessionFx.getAnimationEntity();
    }

    @ModifyArg(
            method = "renderModel(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;ILnet/minecraft/world/item/ArmorItem;Lnet/minecraft/client/model/Model;ZFFFLnet/minecraft/resources/ResourceLocation;)V",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/renderer/MultiBufferSource;getBuffer(Lnet/minecraft/client/renderer/RenderType;)Lcom/mojang/blaze3d/vertex/VertexConsumer;",
                    remap = true
            ),
            index = 0,
            // Перегрузка renderModel(...Model...) не входит в официальный
            // маппинг (в SRG-джаре её имя остаётся renderModel), поэтому её
            // селектор нельзя перенаправлять — в отличие от @At выше.
            remap = false
    )
    private RenderType requiemunofficial$zoomArmorRenderType(RenderType base) {
        return requiemunofficial$goZoom ? ClientPossessionFx.getZoomFx(base) : base;
    }

    @ModifyArg(
            method = "renderGlint(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;ILnet/minecraft/client/model/Model;)V",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/renderer/MultiBufferSource;getBuffer(Lnet/minecraft/client/renderer/RenderType;)Lcom/mojang/blaze3d/vertex/VertexConsumer;",
                    remap = true
            ),
            index = 0,
            // Как и renderModel(...Model...), эта перегрузка не входит в
            // официальный маппинг: её имя в SRG-джаре остаётся renderGlint.
            remap = false
    )
    private RenderType requiemunofficial$zoomGlintRenderType(RenderType base) {
        return requiemunofficial$goZoom ? ClientPossessionFx.getZoomFx(base) : base;
    }
}
