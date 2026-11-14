package ua.meekeyn.requiemunofficial.common.soul;

import net.minecraft.world.entity.player.Player;
import net.minecraft.server.level.ServerPlayer;
import ua.meekeyn.requiemunofficial.common.network.ModNetwork;

import java.util.UUID;
import net.minecraft.core.BlockPos;

public final class SoulManager {

    private SoulManager() {
    }

    public static PlayerSoulData get(Player player) {
        PlayerSoulData data = new PlayerSoulData();

        ModCapabilities.soul(player).ifPresent(soul -> {
            // Копіює дані з capability у тимчасовий контейнер для читання
            data.setSoulPower(soul.getSoulPower());
            data.setIchorLevel(soul.getIchorLevel());
            data.setState(soul.getState());
        });

        return data;
    }

    public static int getSoulPower(Player player) {
        return ModCapabilities.soul(player)
                .map(IPlayerSoul::getSoulPower)
                .orElse(0);
    }

    public static int getIchorLevel(Player player) {
        return ModCapabilities.soul(player)
                .map(IPlayerSoul::getIchorLevel)
                .orElse(0);
    }

    public static void addSoul(Player player, int amount) {
        ModCapabilities.soul(player).ifPresent(soul -> {
            // Додає силу душі безпосередньо в capability
            soul.addSoulPower(amount);
        });
        sync(player);
    }

    public static void addIchor(Player player, int amount) {
        ModCapabilities.soul(player).ifPresent(soul -> {
            // Додає іхор безпосередньо в capability
            soul.addIchorLevel(amount);
        });
        sync(player);
    }

    public static void setSoul(Player player, int amount) {
        ModCapabilities.soul(player).ifPresent(soul -> {
            // Встановлює точне значення сили душі
            soul.setSoulPower(amount);
        });
        sync(player);
    }

    public static void setIchor(Player player, int amount) {
        ModCapabilities.soul(player).ifPresent(soul -> {
            // Встановлює точне значення іхору
            soul.setIchorLevel(amount);
        });
        sync(player);
    }

    public static SoulState getState(Player player) {
        return ModCapabilities.soul(player)
                .map(IPlayerSoul::getState)
                .orElse(SoulState.MORTAL);
    }

    public static void setState(Player player, SoulState state) {
        ModCapabilities.soul(player).ifPresent(soul -> soul.setState(state));
        sync(player);
        if (player instanceof ServerPlayer serverPlayer) {
            VagrantEvents.refreshAfterSoulStateChange(serverPlayer);
        }
    }

    /**
     * Душа поза власним тілом: вільна (VAGRANT) або у вселеному тілі (VAGRANT/спадкова
     * REMNANT-посесія з possessedEntityId). REMNANT у власному тілі та MORTAL — звичайний гравець.
     */
    public static boolean isInSoulForm(Player player) {
        return getState(player) == SoulState.VAGRANT || getPossessedEntityId(player) != null;
    }

    public static UUID getPossessedEntityId(Player player) {
        return ModCapabilities.soul(player)
                .map(IPlayerSoul::getPossessedEntityId)
                .orElse(null);
    }

    public static void setPossessedEntity(Player player, UUID entityId) {
        ModCapabilities.soul(player).ifPresent(soul -> {
            soul.setPossessedEntityId(entityId);
        });
        sync(player);
    }

    public static void clearPossession(Player player) {
        ModCapabilities.soul(player).ifPresent(soul -> {
            boolean wasLegacyRemnantPossession = soul.getPossessedEntityId() != null
                    && soul.getState() == SoulState.REMNANT;
            soul.clearPossessedEntityId();
            if (wasLegacyRemnantPossession) {
                soul.setState(SoulState.VAGRANT);
            }
        });
        sync(player);
    }

    public static UUID getPlayerShellId(Player player) {
        return ModCapabilities.soul(player)
                .map(IPlayerSoul::getPlayerShellId)
                .orElse(null);
    }

    public static void setPlayerShell(Player player, UUID shellId) {
        ModCapabilities.soul(player)
                .ifPresent(soul -> soul.setPlayerShellId(shellId));
        sync(player);
    }

    public static void clearPlayerShell(Player player) {
        ModCapabilities.soul(player)
                .ifPresent(IPlayerSoul::clearPlayerShellId);
        sync(player);
    }

    public static void setResurrectionPoint(Player player, String dimension, BlockPos pos) {
        ModCapabilities.soul(player)
                .ifPresent(soul -> soul.setResurrectionPoint(dimension, pos));
        sync(player);
    }

    public static String getResurrectionDimension(Player player) {
        return ModCapabilities.soul(player)
                .map(IPlayerSoul::getResurrectionDimension)
                .orElse("");
    }

    public static BlockPos getResurrectionPos(Player player) {
        return ModCapabilities.soul(player)
                .map(IPlayerSoul::getResurrectionPos)
                .orElse(null);
    }

    public static void clearResurrectionPoint(Player player) {
        ModCapabilities.soul(player).ifPresent(IPlayerSoul::clearResurrectionPoint);
        sync(player);
    }

    public static void sync(Player player) {
        if (player instanceof ServerPlayer serverPlayer
                && serverPlayer.connection != null
                && serverPlayer.connection.getConnection() != null) {
            ModNetwork.sendSoulData(serverPlayer);
        }
    }
}
