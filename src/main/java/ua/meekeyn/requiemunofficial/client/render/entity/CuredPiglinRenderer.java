package ua.meekeyn.requiemunofficial.client.render.entity;

import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.PiglinRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Mob;
import ua.meekeyn.requiemunofficial.RequiemUnofficial;

public class CuredPiglinRenderer extends PiglinRenderer {

    private static final ResourceLocation TEXTURE = new ResourceLocation(
            RequiemUnofficial.MODID,
            "textures/entity/cured_piglin.png"
    );

    public CuredPiglinRenderer(EntityRendererProvider.Context context) {
        super(
                context,
                ModelLayers.PIGLIN,
                ModelLayers.PIGLIN_INNER_ARMOR,
                ModelLayers.PIGLIN_OUTER_ARMOR,
                false
        );
    }

    @Override
    public ResourceLocation getTextureLocation(Mob entity) {
        return TEXTURE;
    }
}
