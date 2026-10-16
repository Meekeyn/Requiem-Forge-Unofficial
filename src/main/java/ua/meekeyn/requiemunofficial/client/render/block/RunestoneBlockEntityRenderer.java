package ua.meekeyn.requiemunofficial.client.render.block;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.state.BlockState;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import ua.meekeyn.requiemunofficial.RequiemUnofficial;
import ua.meekeyn.requiemunofficial.common.block.InertRunestoneBlock;
import ua.meekeyn.requiemunofficial.common.block.RunestoneBlock;
import ua.meekeyn.requiemunofficial.common.block.entity.RunestoneBlockEntity;

public class RunestoneBlockEntityRenderer implements BlockEntityRenderer<RunestoneBlockEntity> {
    private static final float OUTSET = 0.002F;

    public RunestoneBlockEntityRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    public void render(RunestoneBlockEntity runestone,
                       float partialTick,
                       PoseStack poseStack,
                       MultiBufferSource bufferSource,
                       int packedLight,
                       int packedOverlay) {
        BlockState state = runestone.getBlockState();
        if (!state.hasProperty(InertRunestoneBlock.ACTIVATED)
                || !state.getValue(InertRunestoneBlock.ACTIVATED)
                || !(state.getBlock() instanceof RunestoneBlock block)) {
            return;
        }

        TextureAtlasSprite sprite = Minecraft.getInstance()
                .getTextureAtlas(TextureAtlas.LOCATION_BLOCKS)
                .apply(runeTexture(block.getRuneType()));
        VertexConsumer consumer = bufferSource.getBuffer(RenderType.cutoutMipped());

        renderSide(poseStack, consumer, sprite, Side.NORTH);
        renderSide(poseStack, consumer, sprite, Side.SOUTH);
        renderSide(poseStack, consumer, sprite, Side.WEST);
        renderSide(poseStack, consumer, sprite, Side.EAST);
    }

    public static ResourceLocation runeTexture(String runeType) {
        String texture = "rift".equals(runeType) ? "rift_rune_side" : runeType + "_rune";
        return new ResourceLocation(RequiemUnofficial.MODID, "block/" + texture);
    }

    private static void renderSide(PoseStack poseStack,
                                   VertexConsumer consumer,
                                   TextureAtlasSprite sprite,
                                   Side side) {
        PoseStack.Pose pose = poseStack.last();
        Matrix4f poseMatrix = pose.pose();
        Matrix3f normalMatrix = pose.normal();
        float min = -OUTSET;
        float max = 1.0F + OUTSET;
        float u0 = sprite.getU0();
        float u1 = sprite.getU1();
        float v0 = sprite.getV0();
        float v1 = sprite.getV1();

        switch (side) {
            case NORTH -> {
                float z = -OUTSET;
                vertex(consumer, poseMatrix, normalMatrix, min, min, z, u1, v1, 0.0F, 0.0F, -1.0F);
                vertex(consumer, poseMatrix, normalMatrix, max, min, z, u0, v1, 0.0F, 0.0F, -1.0F);
                vertex(consumer, poseMatrix, normalMatrix, max, max, z, u0, v0, 0.0F, 0.0F, -1.0F);
                vertex(consumer, poseMatrix, normalMatrix, min, max, z, u1, v0, 0.0F, 0.0F, -1.0F);
            }
            case SOUTH -> {
                float z = 1.0F + OUTSET;
                vertex(consumer, poseMatrix, normalMatrix, max, min, z, u1, v1, 0.0F, 0.0F, 1.0F);
                vertex(consumer, poseMatrix, normalMatrix, min, min, z, u0, v1, 0.0F, 0.0F, 1.0F);
                vertex(consumer, poseMatrix, normalMatrix, min, max, z, u0, v0, 0.0F, 0.0F, 1.0F);
                vertex(consumer, poseMatrix, normalMatrix, max, max, z, u1, v0, 0.0F, 0.0F, 1.0F);
            }
            case WEST -> {
                float x = -OUTSET;
                vertex(consumer, poseMatrix, normalMatrix, x, min, max, u1, v1, -1.0F, 0.0F, 0.0F);
                vertex(consumer, poseMatrix, normalMatrix, x, min, min, u0, v1, -1.0F, 0.0F, 0.0F);
                vertex(consumer, poseMatrix, normalMatrix, x, max, min, u0, v0, -1.0F, 0.0F, 0.0F);
                vertex(consumer, poseMatrix, normalMatrix, x, max, max, u1, v0, -1.0F, 0.0F, 0.0F);
            }
            case EAST -> {
                float x = 1.0F + OUTSET;
                vertex(consumer, poseMatrix, normalMatrix, x, min, min, u1, v1, 1.0F, 0.0F, 0.0F);
                vertex(consumer, poseMatrix, normalMatrix, x, min, max, u0, v1, 1.0F, 0.0F, 0.0F);
                vertex(consumer, poseMatrix, normalMatrix, x, max, max, u0, v0, 1.0F, 0.0F, 0.0F);
                vertex(consumer, poseMatrix, normalMatrix, x, max, min, u1, v0, 1.0F, 0.0F, 0.0F);
            }
        }
    }

    private static void vertex(VertexConsumer consumer,
                               Matrix4f pose,
                               Matrix3f normal,
                               float x,
                               float y,
                               float z,
                               float u,
                               float v,
                               float normalX,
                               float normalY,
                               float normalZ) {
        consumer.vertex(pose, x, y, z)
                .color(255, 255, 255, 255)
                .uv(u, v)
                .overlayCoords(0)
                .uv2(LightTexture.FULL_BRIGHT)
                .normal(normal, normalX, normalY, normalZ)
                .endVertex();
    }

    private enum Side {
        NORTH,
        SOUTH,
        WEST,
        EAST
    }
}
