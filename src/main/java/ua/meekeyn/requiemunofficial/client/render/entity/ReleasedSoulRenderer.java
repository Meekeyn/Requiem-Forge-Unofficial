package ua.meekeyn.requiemunofficial.client.render.entity;

import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import ua.meekeyn.requiemunofficial.RequiemUnofficial;
import ua.meekeyn.requiemunofficial.common.entity.ReleasedSoulEntity;

public class ReleasedSoulRenderer extends EntityRenderer<ReleasedSoulEntity> {

    private static final ResourceLocation TEXTURE =
            new ResourceLocation(RequiemUnofficial.MODID, "textures/entity/soul.png");

    public ReleasedSoulRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public ResourceLocation getTextureLocation(ReleasedSoulEntity entity) {
        return TEXTURE;
    }

    @Override
    public void render(ReleasedSoulEntity entity, float entityYaw, float partialTick, PoseStack poseStack,
                       MultiBufferSource buffer, int packedLight) {
        poseStack.pushPose();
        poseStack.translate(0.0D, 0.35D, 0.0D);
        poseStack.mulPose(this.entityRenderDispatcher.cameraOrientation());
        poseStack.mulPose(Axis.YP.rotationDegrees(180.0F));

        float scale = 0.65F + Math.min(entity.getSoulAmount(), 3) * 0.08F;
        poseStack.scale(scale, scale, scale);

        PoseStack.Pose pose = poseStack.last();
        VertexConsumer consumer = buffer.getBuffer(RenderType.entityTranslucent(TEXTURE));
        vertex(consumer, pose, -0.5F, -0.5F, 0.0F, 1.0F, packedLight);
        vertex(consumer, pose, 0.5F, -0.5F, 1.0F, 1.0F, packedLight);
        vertex(consumer, pose, 0.5F, 0.5F, 1.0F, 0.0F, packedLight);
        vertex(consumer, pose, -0.5F, 0.5F, 0.0F, 0.0F, packedLight);
        poseStack.popPose();
        super.render(entity, entityYaw, partialTick, poseStack, buffer, packedLight);
    }

    private static void vertex(VertexConsumer consumer, PoseStack.Pose pose,
                               float x, float y, float u, float v, int packedLight) {
        consumer.vertex(pose.pose(), x, y, 0.0F)
                .color(180, 235, 255, 180)
                .uv(u, v)
                .overlayCoords(0)
                .uv2(packedLight)
                .normal(pose.normal(), 0.0F, 1.0F, 0.0F)
                .endVertex();
    }
}
