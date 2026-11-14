package ua.meekeyn.requiemunofficial.client.render.entity;

import com.mojang.authlib.GameProfile;
import com.mojang.authlib.minecraft.MinecraftProfileTexture;
import com.mojang.authlib.properties.Property;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.resources.DefaultPlayerSkin;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.layers.HumanoidArmorLayer;
import net.minecraft.client.renderer.entity.layers.ItemInHandLayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.PlayerModelPart;
import ua.meekeyn.requiemunofficial.common.entity.PlayerShellEntity;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class PlayerShellRenderer extends LivingEntityRenderer<PlayerShellEntity, PlayerModel<PlayerShellEntity>> {

    private static final String SLIM_MODEL = "slim";
    // Кеш скінів за значенням властивості "textures": декодування й реєстрація текстури лише раз.
    private static final Map<String, Skin> TEXTURE_CACHE = new HashMap<>();

    private final PlayerModel<PlayerShellEntity> wideModel;
    private final PlayerModel<PlayerShellEntity> slimModel;

    public PlayerShellRenderer(EntityRendererProvider.Context context) {
        super(context, new PlayerModel<>(context.bakeLayer(ModelLayers.PLAYER), false), 0.5F);
        this.wideModel = this.model;
        this.slimModel = new PlayerModel<>(context.bakeLayer(ModelLayers.PLAYER_SLIM), true);
        this.addLayer(new HumanoidArmorLayer<>(
                this,
                new HumanoidModel<>(context.bakeLayer(ModelLayers.PLAYER_INNER_ARMOR)),
                new HumanoidModel<>(context.bakeLayer(ModelLayers.PLAYER_OUTER_ARMOR)),
                context.getModelManager()
        ));
        this.addLayer(new ItemInHandLayer<>(this, context.getItemInHandRenderer()));
    }

    @Override
    public void render(PlayerShellEntity entity, float entityYaw, float partialTick, PoseStack poseStack,
                       MultiBufferSource buffer, int packedLight) {
        this.model = resolveSkin(entity).slim() ? slimModel : wideModel;
        int parts = entity.getSkinPartsMask();
        this.model.hat.visible = (parts & PlayerModelPart.HAT.getMask()) != 0;
        this.model.jacket.visible = (parts & PlayerModelPart.JACKET.getMask()) != 0;
        this.model.leftPants.visible = (parts & PlayerModelPart.LEFT_PANTS_LEG.getMask()) != 0;
        this.model.rightPants.visible = (parts & PlayerModelPart.RIGHT_PANTS_LEG.getMask()) != 0;
        this.model.leftSleeve.visible = (parts & PlayerModelPart.LEFT_SLEEVE.getMask()) != 0;
        this.model.rightSleeve.visible = (parts & PlayerModelPart.RIGHT_SLEEVE.getMask()) != 0;
        super.render(entity, entityYaw, partialTick, poseStack, buffer, packedLight);
    }

    @Override
    public ResourceLocation getTextureLocation(PlayerShellEntity entity) {
        return resolveSkin(entity).texture();
    }

    private static Skin resolveSkin(PlayerShellEntity entity) {
        UUID profileId = entity.getOwnerId();
        if (profileId == null) {
            return defaultSkin(entity.getUUID());
        }

        Minecraft minecraft = Minecraft.getInstance();
        // Власник онлайн: вже завантажений скін і модель із таб-листа.
        if (minecraft.getConnection() != null) {
            PlayerInfo playerInfo = minecraft.getConnection().getPlayerInfo(profileId);
            if (playerInfo != null) {
                return new Skin(playerInfo.getSkinLocation(), SLIM_MODEL.equals(playerInfo.getModelName()));
            }
        }

        // Власник офлайн: скін зі збереженої властивості "textures" профілю.
        String textures = entity.getProfileTextures();
        if (textures != null && !textures.isEmpty()) {
            Skin cached = TEXTURE_CACHE.get(textures);
            if (cached == null) {
                cached = loadSkin(minecraft, profileId, entity.getProfileName(), textures);
                // Невдале декодування теж кешуємо (дефолтним скіном), щоб не повторювати щокадру.
                TEXTURE_CACHE.put(textures, cached != null ? cached : defaultSkin(profileId));
            }
            return TEXTURE_CACHE.get(textures);
        }

        return defaultSkin(profileId);
    }

    private static Skin defaultSkin(UUID profileId) {
        return new Skin(DefaultPlayerSkin.getDefaultSkin(profileId),
                SLIM_MODEL.equals(DefaultPlayerSkin.getSkinModelName(profileId)));
    }

    private static Skin loadSkin(Minecraft minecraft, UUID profileId, String name, String textures) {
        GameProfile profile = new GameProfile(profileId, name == null || name.isEmpty() ? null : name);
        profile.getProperties().put("textures", new Property("textures", textures));
        try {
            Map<MinecraftProfileTexture.Type, MinecraftProfileTexture> info =
                    minecraft.getSkinManager().getInsecureSkinInformation(profile);
            MinecraftProfileTexture skin = info.get(MinecraftProfileTexture.Type.SKIN);
            if (skin == null) {
                return null;
            }
            ResourceLocation location = minecraft.getSkinManager()
                    .registerTexture(skin, MinecraftProfileTexture.Type.SKIN);
            return new Skin(location, SLIM_MODEL.equals(skin.getMetadata("model")));
        } catch (RuntimeException exception) {
            return null;
        }
    }

    private record Skin(ResourceLocation texture, boolean slim) {
    }
}
