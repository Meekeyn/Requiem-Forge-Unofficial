package ua.meekeyn.requiemunofficial.client.render.entity;

import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.client.resources.DefaultPlayerSkin;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.layers.HumanoidArmorLayer;
import net.minecraft.client.renderer.entity.layers.ItemInHandLayer;
import net.minecraft.resources.ResourceLocation;
import ua.meekeyn.requiemunofficial.common.entity.PlayerShellEntity;

import java.util.UUID;

public class PlayerShellRenderer extends LivingEntityRenderer<PlayerShellEntity, PlayerModel<PlayerShellEntity>> {

    public PlayerShellRenderer(EntityRendererProvider.Context context) {
        super(context, new PlayerModel<>(context.bakeLayer(ModelLayers.PLAYER), false), 0.5F);
        this.addLayer(new HumanoidArmorLayer<>(
                this,
                new HumanoidModel<>(context.bakeLayer(ModelLayers.PLAYER_INNER_ARMOR)),
                new HumanoidModel<>(context.bakeLayer(ModelLayers.PLAYER_OUTER_ARMOR)),
                context.getModelManager()
        ));
        this.addLayer(new ItemInHandLayer<>(this, context.getItemInHandRenderer()));
    }

    @Override
    public ResourceLocation getTextureLocation(PlayerShellEntity entity) {
        UUID ownerId = entity.getOwnerId();
        if (ownerId == null) {
            return DefaultPlayerSkin.getDefaultSkin(entity.getUUID());
        }

        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.getConnection() != null) {
            PlayerInfo playerInfo = minecraft.getConnection().getPlayerInfo(ownerId);
            if (playerInfo != null) {
                return playerInfo.getSkinLocation();
            }
        }
        return DefaultPlayerSkin.getDefaultSkin(ownerId);
    }
}
