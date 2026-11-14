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

    /** Увімкнено, поки екран інвентаря малює превʼю вселеного тіла: тоді тіло не ховаємо. */
    private static boolean renderingInventoryPreview;

    public static void setRenderingInventoryPreview(boolean rendering) {
        renderingInventoryPreview = rendering;
    }

    public static boolean shouldHideLocalPossessedHost(Entity entity) {
        Minecraft minecraft = Minecraft.getInstance();
        if (renderingInventoryPreview
                || minecraft.player == null || !minecraft.options.getCameraType().isFirstPerson()) {
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
     * Чи треба повністю прибрати гравця з рендеру (модель, шари, предмети, нікнейм).
     * Локальний гравець: як і раніше, лише коли він у вселеному тілі (вільна душа малюється
     * тіньовим ефектом). Інші гравці: коли сервер повідомив, що вони у формі душі.
     */
    public static boolean shouldHideSoulPlayer(AbstractClientPlayer player) {
        if (player == Minecraft.getInstance().player) {
            return shouldHideLocalSoulPlayer(player);
        }
        return ClientSoulVisibility.isHidden(player);
    }

    public static boolean isFreeLocalSoulPlayer(AbstractClientPlayer player) {
        return player == Minecraft.getInstance().player
                && SoulManager.getState(player) == SoulState.VAGRANT
                && SoulManager.getPossessedEntityId(player) == null;
    }
}
