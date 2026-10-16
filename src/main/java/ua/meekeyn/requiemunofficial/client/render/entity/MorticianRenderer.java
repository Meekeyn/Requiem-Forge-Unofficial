package ua.meekeyn.requiemunofficial.client.render.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.layers.CrossedArmsItemLayer;
import net.minecraft.client.renderer.entity.layers.CustomHeadLayer;
import net.minecraft.resources.ResourceLocation;
import ua.meekeyn.requiemunofficial.RequiemUnofficial;
import ua.meekeyn.requiemunofficial.client.render.entity.model.MorticianModel;
import ua.meekeyn.requiemunofficial.common.entity.MorticianEntity;

public class MorticianRenderer
        extends MobRenderer<MorticianEntity, MorticianModel<MorticianEntity>> {

    private static final ResourceLocation TEXTURE =
            new ResourceLocation(RequiemUnofficial.MODID, "textures/entity/mortician.png");
    private static final ResourceLocation PROJECTION_TEXTURE =
            new ResourceLocation(RequiemUnofficial.MODID, "textures/entity/ethereal_mortician.png");

    public MorticianRenderer(EntityRendererProvider.Context context) {
        super(context, new MorticianModel<>(context.bakeLayer(MorticianModel.MODEL_LAYER)), 0.5F);
        addLayer(new CustomHeadLayer<>(this, context.getModelSet(), context.getItemInHandRenderer()));
        addLayer(new CrossedArmsItemLayer<>(this, context.getItemInHandRenderer()));
    }

    @Override
    public void render(MorticianEntity entity, float entityYaw, float partialTicks, PoseStack poseStack,
                       MultiBufferSource buffer, int packedLight) {
        getModel().setBaseAlpha(1.0F - entity.getFadingAmount());
        super.render(entity, entityYaw, partialTicks, poseStack, buffer, packedLight);
    }

    @Override
    public ResourceLocation getTextureLocation(MorticianEntity entity) {
        return entity.isObeliskProjection() ? PROJECTION_TEXTURE : TEXTURE;
    }

    @Override
    protected void scale(MorticianEntity entity, PoseStack poseStack, float partialTickTime) {
        poseStack.scale(0.9375F, 0.9375F, 0.9375F);
    }
}
