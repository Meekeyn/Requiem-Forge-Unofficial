package ua.meekeyn.requiemunofficial.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import ua.meekeyn.requiemunofficial.common.soul.SoulManager;
import ua.meekeyn.requiemunofficial.common.soul.SoulState;

import java.util.UUID;

public final class ClientPossessionRenderHelper {

    private ClientPossessionRenderHelper() {
    }

    public static LivingEntity getPossessedHost(AbstractClientPlayer player) {
        UUID possessedId = SoulManager.getPossessedEntityId(player);
        if (possessedId == null || Minecraft.getInstance().level == null) {
            return null;
        }

        for (Entity entity : Minecraft.getInstance().level.getEntities(
                player,
                player.getBoundingBox().inflate(128.0D),
                entity -> possessedId.equals(entity.getUUID())
        )) {
            if (entity instanceof LivingEntity living) {
                return living;
            }
        }
        return null;
    }

    public static boolean isLocalPlayersPossessedHost(Entity entity) {
        if (Minecraft.getInstance().player == null) {
            return false;
        }

        UUID possessedId = SoulManager.getPossessedEntityId(Minecraft.getInstance().player);
        return possessedId != null && possessedId.equals(entity.getUUID());
    }

    public static boolean shouldHideLocalPossessedHost(Entity entity) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || !minecraft.options.getCameraType().isFirstPerson()) {
            return false;
        }

        return SoulManager.getState(minecraft.player) == SoulState.VAGRANT
                && isLocalPlayersPossessedHost(entity);
    }

    public static boolean shouldHideLocalSoulPlayer(AbstractClientPlayer player) {
        return player == Minecraft.getInstance().player
                && SoulManager.getState(player) == SoulState.VAGRANT
                && SoulManager.getPossessedEntityId(player) != null;
    }

    /**
     * Remote player whose soul is out of their own body (server-synced via
     * {@code SoulVisibilityPacket}). Must be invisible to this client: model,
     * armor, held items, cape/elytra layers, name tag and shadow.
     */
    public static boolean shouldHideRemoteSoulPlayer(AbstractClientPlayer player) {
        return player != null
                && player != Minecraft.getInstance().player
                && ClientSoulVisibility.isHidden(player);
    }

    /** Local possessing soul (existing behaviour) or any remote player in soul form. */
    public static boolean shouldHideSoulPlayer(AbstractClientPlayer player) {
        return shouldHideLocalSoulPlayer(player) || shouldHideRemoteSoulPlayer(player);
    }

    public static boolean isFreeLocalSoulPlayer(AbstractClientPlayer player) {
        return player == Minecraft.getInstance().player
                && SoulManager.getState(player) == SoulState.VAGRANT
                && SoulManager.getPossessedEntityId(player) == null;
    }
}
