package ua.meekeyn.requiemunofficial.client.render.entity;

import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.PiglinRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Mob;
import ua.meekeyn.requiemunofficial.RequiemUnofficial;

public class CuredPiglinBruteRenderer extends PiglinRenderer {

    private static final ResourceLocation TEXTURE = new ResourceLocation(
            RequiemUnofficial.MODID,
            "textures/entity/cured_piglin_brute.png"
    );

    public CuredPiglinBruteRenderer(EntityRendererProvider.Context context) {
        super(
                context,
                ModelLayers.PIGLIN_BRUTE,
                ModelLayers.PIGLIN_BRUTE_INNER_ARMOR,
                ModelLayers.PIGLIN_BRUTE_OUTER_ARMOR,
                false
        );
    }

    @Override
    public ResourceLocation getTextureLocation(Mob entity) {
        return TEXTURE;
    }
}
