package ua.meekeyn.requiemunofficial.client.render.entity;

import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.VillagerRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.npc.Villager;
import ua.meekeyn.requiemunofficial.RequiemUnofficial;

public class CuredVillagerRenderer extends VillagerRenderer {

    private static final ResourceLocation TEXTURE = new ResourceLocation(
            RequiemUnofficial.MODID,
            "textures/entity/cured_villager.png"
    );

    public CuredVillagerRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public ResourceLocation getTextureLocation(Villager entity) {
        return TEXTURE;
    }
}
