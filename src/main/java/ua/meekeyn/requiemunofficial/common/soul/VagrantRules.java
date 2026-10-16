package ua.meekeyn.requiemunofficial.common.soul;

import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.BooleanOp;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import ua.meekeyn.requiemunofficial.common.util.ModTags;

import java.util.Collection;

public final class VagrantRules {
    public static final String PHASE_ATTEMPTING = "RequiemVagrantPhaseAttempting";
    public static final String PHASE_CHARGE_TICKS = "RequiemVagrantPhaseChargeTicks";
    public static final String LEAF_PHASE_CHARGE_TICKS = "RequiemVagrantLeafPhaseChargeTicks";
    public static final String LEAF_PHASE_ACTIVE = "RequiemVagrantLeafPhaseActive";
    public static final String LEAF_PHASE_GRACE_TICK_DATA = "RequiemVagrantLeafPhaseGraceTicks";
    public static final String PHASE_LOWERED = "RequiemVagrantPhaseLowered";
    public static final int PHASE_REQUIRED_TICKS = 60;
    public static final int LEAF_PHASE_REQUIRED_TICKS = 15;
    public static final int LEAF_PHASE_GRACE_TICKS = 20;
    public static final double PHASE_SPEED = 0.0850D;
    public static final double PHASE_LOWERING = 0.0D;

    private VagrantRules() {
    }

    public static boolean isRestricted(Player player) {
        return !player.isCreative()
                && !player.isSpectator()
                && SoulManager.getState(player) == SoulState.VAGRANT
                && SoulManager.getPossessedEntityId(player) == null;
    }

    public static boolean isRestrictedVagrantEntity(Entity entity) {
        return entity instanceof Player player && isRestricted(player);
    }

    public static boolean shouldBeIgnoredByOrdinaryTargeting(Entity entity) {
        return isRestrictedVagrantEntity(entity);
    }

    public static boolean shouldBeIgnoredByOrdinarySensors(Entity entity) {
        return isRestrictedVagrantEntity(entity);
    }

    public static int weightedPressureSignal(Collection<? extends Entity> entities, int maxWeight) {
        int count = 0;
        for (Entity entity : entities) {
            if (!entity.isIgnoringBlockTriggers() && !isRestrictedVagrantEntity(entity)) {
                count++;
            }
        }
        if (count <= 0 || maxWeight <= 0) {
            return 0;
        }

        int capped = Math.min(count, maxWeight);
        return Mth.ceil((float) capped / (float) maxWeight * 15.0F);
    }

    public static boolean canInteractWith(BlockState state) {
        return state.is(ModTags.SOUL_INTERACTABLE);
    }

    public static boolean canPassThrough(BlockState state) {
        if (state.is(ModTags.SOUL_IMPERMEABLE)) {
            return false;
        }

        return state.isAir()
                || state.is(BlockTags.LEAVES)
                || state.is(ModTags.WANDERING_SOUL_TRAVERSABLE)
                || !state.is(ModTags.SOUL_IMPERMEABLE);
    }

    public static boolean isLeaves(BlockState state) {
        return state.is(BlockTags.LEAVES);
    }

    public static boolean shouldPhaseThroughBlocks(Player player) {
        return isRestricted(player)
                && (isInsidePhaseableBlock(player)
                || isDescendingIntoPhaseableBlock(player)
                || player.horizontalCollision && isLookingIntoPhaseableBlock(player)
                || player.getDeltaMovement().y > 0.0D && player.verticalCollision && isLookingIntoPhaseableBlock(player)
                || player.getXRot() < -45.0F && !player.onGround() && player.verticalCollision && isLookingIntoPhaseableBlock(player)
                || isLookingIntoPhaseableBlock(player));
    }

    public static boolean shouldSuppressBlockCollision(Player player) {
        return isRestricted(player)
                && (isPhaseCharged(player)
                || isLeafPhaseActive(player) && !isInsideNonLeafPhaseableBlock(player));
    }

    public static boolean shouldUsePhaseMovement(Player player) {
        return isRestricted(player)
                && (isPhaseCharged(player)
                || isLeafPhaseActive(player) && isInsideLeaves(player)
                || isInsideNonLeafPhaseableBlock(player));
    }

    public static boolean shouldSuppressOnlyLeafCollision(Player player) {
        return isRestricted(player)
                && !isPhaseCharged(player)
                && isLeafPhaseActive(player);
    }

    public static boolean shouldSuppressCollisionWith(Player player, BlockState state) {
        if (!isRestricted(player) || state.isAir()) {
            return false;
        }
        if (isPhaseCharged(player)) {
            return canPassThrough(state);
        }
        return isLeafPhaseActive(player) && isLeaves(state);
    }

    public static boolean shouldChargePhaseEntry(Player player) {
        return isRestricted(player)
                && !isInsidePhaseableBlock(player)
                && (isTouchingSidePhaseableBlock(player)
                || isLookingIntoPhaseableBlock(player));
    }

    public static boolean isInsidePhaseableBlock(Player player) {
        return hasPhaseableCollision(player.level(), player.getBoundingBox());
    }

    public static boolean isInsideNonLeafPhaseableBlock(Player player) {
        return hasPhaseableCollision(player.level(), player.getBoundingBox(), false);
    }

    public static boolean isFacingPhaseableBlock(Player player) {
        return isLookingIntoPhaseableBlock(player);
    }

    public static boolean isTouchingSidePhaseableBlock(Player player) {
        AABB box = player.getBoundingBox();
        AABB sideProbe = new AABB(
                box.minX - 0.25D,
                box.minY,
                box.minZ - 0.25D,
                box.maxX + 0.25D,
                box.maxY,
                box.maxZ + 0.25D
        );
        return hasPhaseableCollision(player.level(), sideProbe);
    }

    public static boolean isTouchingLeaves(Player player) {
        return hasPhaseableCollision(player.level(), player.getBoundingBox().inflate(0.25D), true, true);
    }

    public static boolean isInsideLeaves(Player player) {
        return hasPhaseableCollision(player.level(), player.getBoundingBox(), true, true);
    }

    public static boolean isPhaseAttempting(Player player) {
        return player.getPersistentData().getBoolean(PHASE_ATTEMPTING);
    }

    public static boolean isPhaseCharged(Player player) {
        return player.getPersistentData().getInt(PHASE_CHARGE_TICKS) >= PHASE_REQUIRED_TICKS;
    }

    public static boolean isLeafPhaseCharged(Player player) {
        return player.getPersistentData().getInt(LEAF_PHASE_CHARGE_TICKS) >= LEAF_PHASE_REQUIRED_TICKS;
    }

    public static boolean isLeafPhaseActive(Player player) {
        return isLeafPhaseCharged(player)
                || player.getPersistentData().getBoolean(LEAF_PHASE_ACTIVE)
                || player.getPersistentData().getInt(LEAF_PHASE_GRACE_TICK_DATA) > 0;
    }

    public static void setLeafPhaseActive(Player player, boolean active) {
        if (active) {
            player.getPersistentData().putBoolean(LEAF_PHASE_ACTIVE, true);
        } else {
            player.getPersistentData().remove(LEAF_PHASE_ACTIVE);
        }
    }

    public static void clearPhaseState(Player player) {
        restorePhaseLowering(player);
        player.getPersistentData().remove(PHASE_ATTEMPTING);
        player.getPersistentData().remove(PHASE_CHARGE_TICKS);
        player.getPersistentData().remove(LEAF_PHASE_CHARGE_TICKS);
        player.getPersistentData().remove(LEAF_PHASE_ACTIVE);
        player.getPersistentData().remove(LEAF_PHASE_GRACE_TICK_DATA);
    }

    public static void updatePhaseLowering(Player player, boolean active) {
        boolean lowered = player.getPersistentData().getBoolean(PHASE_LOWERED);
        if (active == lowered) {
            return;
        }
        if (active) {
            player.setPosRaw(player.getX(), player.getY() - PHASE_LOWERING, player.getZ());
            player.getPersistentData().putBoolean(PHASE_LOWERED, true);
        } else {
            restorePhaseLowering(player);
        }
    }

    private static void restorePhaseLowering(Player player) {
        if (!player.getPersistentData().getBoolean(PHASE_LOWERED)) {
            return;
        }
        player.setPosRaw(player.getX(), player.getY() + PHASE_LOWERING, player.getZ());
        player.getPersistentData().remove(PHASE_LOWERED);
    }

    private static boolean isDescendingIntoPhaseableBlock(Player player) {
        if (!player.isShiftKeyDown()) {
            return false;
        }
        return isInsidePhaseableBlock(player)
                || hasPhaseableCollision(player.level(), player.getBoundingBox().move(0.0D, -0.2D, 0.0D));
    }

    private static boolean isLookingIntoPhaseableBlock(Player player) {
        Vec3 look = player.getLookAngle();
        if (look.lengthSqr() < 1.0E-5D) {
            return false;
        }

        Vec3 probe = look.normalize().scale(0.65D);
        if (!player.isShiftKeyDown() && probe.y < 0.0D) {
            probe = new Vec3(probe.x, 0.0D, probe.z);
        }
        if (probe.lengthSqr() < 1.0E-5D) {
            return false;
        }

        AABB probeBox = player.getBoundingBox().move(probe);
        return hasPhaseableCollision(player.level(), probeBox);
    }

    public static boolean isLookingIntoLeaves(Player player) {
        Vec3 look = player.getLookAngle();
        if (look.lengthSqr() < 1.0E-5D) {
            return false;
        }

        Vec3 probe = look.normalize().scale(0.65D);
        if (!player.isShiftKeyDown() && probe.y < 0.0D) {
            probe = new Vec3(probe.x, 0.0D, probe.z);
        }
        if (probe.lengthSqr() < 1.0E-5D) {
            return false;
        }

        return hasPhaseableCollision(player.level(), player.getBoundingBox().move(probe), true, true);
    }

    private static boolean hasPhaseableCollision(Level level, AABB box) {
        return hasPhaseableCollision(level, box, true);
    }

    private static boolean hasPhaseableCollision(Level level, AABB box, boolean includeLeaves) {
        return hasPhaseableCollision(level, box, includeLeaves, false);
    }

    private static boolean hasPhaseableCollision(Level level, AABB box, boolean includeLeaves, boolean leavesOnly) {
        int minX = Mth.floor(box.minX);
        int minY = Mth.floor(box.minY);
        int minZ = Mth.floor(box.minZ);
        int maxX = Mth.floor(box.maxX);
        int maxY = Mth.floor(box.maxY);
        int maxZ = Mth.floor(box.maxZ);
        VoxelShape entityShape = Shapes.create(box);

        for (BlockPos pos : BlockPos.betweenClosed(minX, minY, minZ, maxX, maxY, maxZ)) {
            BlockState state = level.getBlockState(pos);
            if (state.isAir()
                    || !canPassThrough(state)
                    || !includeLeaves && isLeaves(state)
                    || leavesOnly && !isLeaves(state)) {
                continue;
            }

            VoxelShape blockShape = state.getCollisionShape(level, pos);
            if (!blockShape.isEmpty()
                    && Shapes.joinIsNotEmpty(blockShape.move(pos.getX(), pos.getY(), pos.getZ()),
                    entityShape,
                    BooleanOp.AND)) {
                return true;
            }
        }
        return false;
    }
}
