package ua.meekeyn.requiemunofficial.common.remnant;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import ua.meekeyn.requiemunofficial.common.entity.PlayerShellEntity;
import ua.meekeyn.requiemunofficial.common.possession.PossessionManager;
import ua.meekeyn.requiemunofficial.common.soul.SoulLifecycleManager;
import ua.meekeyn.requiemunofficial.common.soul.SoulManager;
import ua.meekeyn.requiemunofficial.common.soul.SoulState;

import java.util.UUID;

public final class DroppedVesselTracker {

    private static final String TRACKER_TAG = "RequiemDroppedVessel";
    private static final String ANCHOR = "Anchor";
    private static final String DIMENSION = "Dimension";
    private static final String X = "X";
    private static final String Y = "Y";
    private static final String Z = "Z";

    private DroppedVesselTracker() {
    }

    public static boolean dropVessel(ServerPlayer player) {
        UUID hostId = SoulManager.getPossessedEntityId(player);
        if (hostId != null) {
            LivingEntity host = PossessionManager.findPossessedHost(player, hostId);
            if (host != null && PossessionManager.suspendForDisconnect(player, host)) {
                saveAnchor(player, host);
                return true;
            }
        }

        SoulState state = SoulManager.getState(player);
        if (state != SoulState.REMNANT && state != SoulState.VAGRANT) {
            return false;
        }

        PlayerShellEntity shell = findShell(player);
        boolean createdShell = false;
        if (shell == null) {
            shell = SoulLifecycleManager.createShell(player);
            createdShell = true;
        }
        if (shell == null) {
            return false;
        }

        if (createdShell) {
            player.stopUsingItem();
            SoulLifecycleManager.finishBodySplitAfterCapture(player, shell);
        }
        SoulManager.setState(player, SoulState.VAGRANT);
        player.setInvisible(true);
        saveAnchor(player, shell);
        return true;
    }

    public static boolean tryMergeWithVessel(ServerPlayer player) {
        CompoundTag anchor = getAnchor(player);
        if (anchor == null) {
            return false;
        }

        Entity entity = findAnchorEntity(player, anchor);
        boolean restored = false;
        if (entity instanceof PlayerShellEntity shell) {
            if (SoulManager.getState(player) == SoulState.REMNANT) {
                SoulManager.setState(player, SoulState.VAGRANT);
            }
            restored = SoulLifecycleManager.tryReclaimShell(player, shell);
        } else if (entity instanceof LivingEntity host) {
            restored = PossessionManager.resumeAfterDisconnect(player, host);
        }

        if (!restored) {
            teleportToAnchor(player, anchor);
            SoulManager.clearPossession(player);
            player.setInvisible(SoulManager.getState(player) == SoulState.VAGRANT);
        }
        clearAnchor(player);
        return restored;
    }

    public static void saveAnchor(ServerPlayer player, Entity entity) {
        CompoundTag anchor = new CompoundTag();
        anchor.putUUID(ANCHOR, entity.getUUID());
        anchor.putString(DIMENSION, entity.level().dimension().location().toString());
        anchor.putDouble(X, entity.getX());
        anchor.putDouble(Y, entity.getY());
        anchor.putDouble(Z, entity.getZ());
        player.getPersistentData().put(TRACKER_TAG, anchor);
    }

    public static CompoundTag getAnchor(ServerPlayer player) {
        CompoundTag data = player.getPersistentData();
        return data.contains(TRACKER_TAG, Tag.TAG_COMPOUND)
                ? data.getCompound(TRACKER_TAG)
                : null;
    }

    public static void clearAnchor(ServerPlayer player) {
        player.getPersistentData().remove(TRACKER_TAG);
    }

    private static Entity findAnchorEntity(ServerPlayer player, CompoundTag anchor) {
        if (!anchor.hasUUID(ANCHOR)) {
            return null;
        }
        UUID anchorId = anchor.getUUID(ANCHOR);
        for (ServerLevel level : player.getServer().getAllLevels()) {
            Entity entity = level.getEntity(anchorId);
            if (entity != null) {
                return entity;
            }
        }
        return null;
    }

    private static PlayerShellEntity findShell(ServerPlayer player) {
        UUID shellId = SoulManager.getPlayerShellId(player);
        if (shellId == null) {
            return null;
        }
        for (ServerLevel level : player.getServer().getAllLevels()) {
            Entity entity = level.getEntity(shellId);
            if (entity instanceof PlayerShellEntity shell) {
                return shell;
            }
        }
        return null;
    }

    private static void teleportToAnchor(ServerPlayer player, CompoundTag anchor) {
        if (!anchor.contains(DIMENSION) || !anchor.contains(X)
                || !anchor.contains(Y) || !anchor.contains(Z)) {
            return;
        }

        ResourceLocation dimensionId = ResourceLocation.tryParse(anchor.getString(DIMENSION));
        if (dimensionId == null) {
            return;
        }
        ResourceKey<Level> dimension = ResourceKey.create(
                net.minecraft.core.registries.Registries.DIMENSION,
                dimensionId
        );
        ServerLevel level = player.getServer().getLevel(dimension);
        if (level != null) {
            player.teleportTo(
                    level,
                    anchor.getDouble(X),
                    anchor.getDouble(Y),
                    anchor.getDouble(Z),
                    player.getYRot(),
                    player.getXRot()
            );
        }
    }
}
