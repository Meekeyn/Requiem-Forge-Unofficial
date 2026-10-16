package ua.meekeyn.requiemunofficial.client.render.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import ua.meekeyn.requiemunofficial.RequiemUnofficial;
import ua.meekeyn.requiemunofficial.common.entity.ObeliskSoulEntity;

public class ObeliskSoulRenderer extends EntityRenderer<ObeliskSoulEntity> {

    private static final ResourceLocation TEXTURE =
            new ResourceLocation(RequiemUnofficial.MODID, "textures/entity/soul.png");
    private static final int FULL_BRIGHT = 0xF000F0;

    public ObeliskSoulRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public ResourceLocation getTextureLocation(ObeliskSoulEntity entity) {
        return TEXTURE;
    }

    @Override
    public void render(ObeliskSoulEntity entity, float entityYaw, float partialTick, PoseStack poseStack,
                       MultiBufferSource buffer, int packedLight) {
        float scale = 0.45F + Math.min(entity.getSoulAmount(), 10) * 0.025F;
        float[] color = colorFor(entity.getSoulType());

        poseStack.pushPose();
        poseStack.translate(0.0D, 0.25D, 0.0D);
        poseStack.mulPose(this.entityRenderDispatcher.cameraOrientation());
        poseStack.mulPose(Axis.YP.rotationDegrees(180.0F));
        poseStack.scale(scale, scale, scale);

        PoseStack.Pose pose = poseStack.last();
        VertexConsumer vertices = buffer.getBuffer(RenderType.entityTranslucentEmissive(TEXTURE));
        vertex(vertices, pose, -0.5F, -0.5F, 0.0F, 1.0F, color);
        vertex(vertices, pose, 0.5F, -0.5F, 1.0F, 1.0F, color);
        vertex(vertices, pose, 0.5F, 0.5F, 1.0F, 0.0F, color);
        vertex(vertices, pose, -0.5F, 0.5F, 0.0F, 0.0F, color);
        poseStack.popPose();

        super.render(entity, entityYaw, partialTick, poseStack, buffer, packedLight);
    }

    private static void vertex(VertexConsumer vertices, PoseStack.Pose pose, float x, float y,
                               float u, float v, float[] color) {
        vertices.vertex(pose.pose(), x, y, 0.0F)
                .color(color[0], color[1], color[2], 0.85F)
                .uv(u, v)
                .overlayCoords(OverlayTexture.NO_OVERLAY)
                .uv2(FULL_BRIGHT)
                .normal(pose.normal(), 0.0F, 1.0F, 0.0F)
                .endVertex();
    }

    private static float[] colorFor(String soulType) {
        if (soulType.contains("wither") || soulType.contains("aggregate")) {
            return new float[]{0.65F, 0.35F, 0.85F};
        }
        if (soulType.contains("zombie") || soulType.contains("skeleton")
                || soulType.contains("undead")) {
            return new float[]{0.45F, 0.85F, 0.65F};
        }
        if (soulType.contains("villager") || soulType.contains("human")
                || soulType.contains("player")) {
            return new float[]{0.55F, 0.8F, 1.0F};
        }
        return new float[]{0.75F, 0.9F, 1.0F};
    }
}
