package ua.meekeyn.requiemunofficial.common.diagnostic;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.BooleanOp;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import ua.meekeyn.requiemunofficial.RequiemUnofficial;
import ua.meekeyn.requiemunofficial.common.soul.VagrantRules;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class VagrantDiagnostics {
    private static final boolean ENABLED = Boolean.getBoolean("requiem.vagrantDiagnostics");
    private static final Map<String, PhaseSnapshot> LAST_PHASE = new HashMap<>();
    private static final Map<String, Integer> LAST_EVENT_SAMPLE_TICK = new HashMap<>();

    private VagrantDiagnostics() {
    }

    public static boolean enabled() {
        return ENABLED;
    }

    public static void logPhaseSnapshot(String side, Player player, String reason) {
        if (!ENABLED || player == null || !VagrantRules.isRestricted(player)) {
            return;
        }

        PhaseSnapshot snapshot = PhaseSnapshot.capture(side, player);
        String key = side + ":" + player.getUUID();
        PhaseSnapshot previous = LAST_PHASE.get(key);
        boolean sampled = player.tickCount % 20 == 0;
        if (!sampled && snapshot.sameState(previous)) {
            return;
        }

        LAST_PHASE.put(key, snapshot);
        RequiemUnofficial.LOGGER.info(
                "[REQ-VAGRANT-DIAG phase] side={} tick={} player={} uuid={} reason={} phaseChargeTicks={} leafPhaseChargeTicks={} leafPhaseGraceTicks={} isPhaseCharged={} isLeafPhaseActive={} insideLeaves={} insideNonLeafPhaseableBlock={} pos={} delta={} aabb={}",
                side,
                player.tickCount,
                player.getGameProfile().getName(),
                player.getUUID(),
                reason,
                snapshot.phaseChargeTicks,
                snapshot.leafPhaseChargeTicks,
                snapshot.leafPhaseGraceTicks,
                snapshot.phaseCharged,
                snapshot.leafPhaseActive,
                snapshot.insideLeaves,
                snapshot.insideNonLeafPhaseableBlock,
                formatVec(player.position()),
                formatVec(player.getDeltaMovement()),
                formatAabb(player.getBoundingBox())
        );
    }

    public static void logServerMove(String reason,
                                     Player player,
                                     AABB oldAabb,
                                     Vec3 requestedPos,
                                     Vec3 beforePos,
                                     Vec3 afterPos,
                                     Vec3 beforeDelta,
                                     Vec3 afterDelta,
                                     int awaitingTeleportBefore,
                                     int awaitingTeleportAfter,
                                     Boolean newCollision,
                                     String extra) {
        if (!ENABLED || player == null || !VagrantRules.isRestricted(player)) {
            return;
        }

        boolean corrected = distanceSqr(afterPos, requestedPos) > 1.0E-6D;
        boolean rejected = distanceSqr(afterPos, beforePos) < 1.0E-6D && distanceSqr(requestedPos, beforePos) > 1.0E-6D;
        boolean teleportStarted = awaitingTeleportAfter != awaitingTeleportBefore;
        boolean significant = Boolean.TRUE.equals(newCollision) || rejected || corrected || teleportStarted;
        if (!significant && !sampleEvent("server-move:" + reason, player, 20)) {
            return;
        }
        RequiemUnofficial.LOGGER.info(
                "[REQ-VAGRANT-DIAG server-move] side=SERVER tick={} player={} uuid={} reason={} newCollision={} rejected={} corrected={} teleportStarted={} beforePos={} requestedPos={} afterPos={} beforeDelta={} afterDelta={} oldAabb={} newAabb={} collisions={} extra={}",
                player.tickCount,
                player.getGameProfile().getName(),
                player.getUUID(),
                reason,
                newCollision,
                rejected,
                corrected,
                teleportStarted,
                formatVec(beforePos),
                formatVec(requestedPos),
                formatVec(afterPos),
                formatVec(beforeDelta),
                formatVec(afterDelta),
                formatAabb(oldAabb),
                formatAabb(player.getBoundingBox()),
                describePhaseableCollisionDelta(player.level(), oldAabb, player.getBoundingBox()),
                extra
        );
    }

    public static void logClientPush(Player player,
                                     String reason,
                                     BlockPos blockPos,
                                     BlockState blockState,
                                     boolean suffocates,
                                     Vec3 beforePos,
                                     Vec3 afterPos,
                                     Vec3 beforeDelta,
                                     Vec3 afterDelta) {
        if (!ENABLED || player == null || !VagrantRules.isRestricted(player)) {
            return;
        }

        boolean phaseGrant = VagrantRules.isPhaseCharged(player) || VagrantRules.isLeafPhaseActive(player);
        boolean movementChanged = distanceSqr(beforeDelta, afterDelta) > 1.0E-12D;
        if (!suffocates && !movementChanged && !phaseGrant && !sampleEvent("client-push:" + blockPos, player, 20)) {
            return;
        }
        RequiemUnofficial.LOGGER.info(
                "[REQ-VAGRANT-DIAG client-push] side=CLIENT tick={} player={} uuid={} reason={} block={} blockPos={} suffocates={} phaseGrant={} movementChanged={} beforePos={} afterPos={} beforeDelta={} afterDelta={} aabb={}",
                player.tickCount,
                player.getGameProfile().getName(),
                player.getUUID(),
                reason,
                blockState == null ? "unknown" : blockId(blockState),
                blockPos,
                suffocates,
                phaseGrant,
                movementChanged,
                formatVec(beforePos),
                formatVec(afterPos),
                formatVec(beforeDelta),
                formatVec(afterDelta),
                formatAabb(player.getBoundingBox())
        );
    }

    public static void logClientSuffocationCheck(Player player, BlockPos pos, boolean result) {
        if (!ENABLED || player == null || !VagrantRules.isRestricted(player)) {
            return;
        }
        BlockState state = player.level().getBlockState(pos);
        boolean phaseGrant = VagrantRules.isPhaseCharged(player) || VagrantRules.isLeafPhaseActive(player);
        if (!result && !phaseGrant && !sampleEvent("client-suffocatesAt:" + pos, player, 20)) {
            return;
        }
        RequiemUnofficial.LOGGER.info(
                "[REQ-VAGRANT-DIAG client-suffocatesAt] side=CLIENT tick={} player={} uuid={} block={} blockPos={} result={} isLeaves={} phaseGrant={} aabb={}",
                player.tickCount,
                player.getGameProfile().getName(),
                player.getUUID(),
                blockId(state),
                pos,
                result,
                VagrantRules.isLeaves(state),
                phaseGrant,
                formatAabb(player.getBoundingBox())
        );
    }

    public static String describePhaseableCollisionDelta(Level level, AABB oldAabb, AABB newAabb) {
        if (oldAabb == null || newAabb == null) {
            return "oldOrNewAabbMissing";
        }
        StringBuilder builder = new StringBuilder();
        int matches = 0;
        AABB scan = oldAabb.minmax(newAabb);
        int minX = Mth.floor(scan.minX);
        int minY = Mth.floor(scan.minY);
        int minZ = Mth.floor(scan.minZ);
        int maxX = Mth.floor(scan.maxX);
        int maxY = Mth.floor(scan.maxY);
        int maxZ = Mth.floor(scan.maxZ);
        VoxelShape oldShape = Shapes.create(oldAabb);
        VoxelShape newShape = Shapes.create(newAabb);

        for (BlockPos pos : BlockPos.betweenClosed(minX, minY, minZ, maxX, maxY, maxZ)) {
            BlockState state = level.getBlockState(pos);
            if (state.isAir() || !VagrantRules.canPassThrough(state)) {
                continue;
            }
            VoxelShape blockShape = state.getCollisionShape(level, pos);
            if (blockShape.isEmpty()) {
                continue;
            }
            VoxelShape moved = blockShape.move(pos.getX(), pos.getY(), pos.getZ());
            boolean oldHit = Shapes.joinIsNotEmpty(moved, oldShape, BooleanOp.AND);
            boolean newHit = Shapes.joinIsNotEmpty(moved, newShape, BooleanOp.AND);
            if (!oldHit && !newHit) {
                continue;
            }
            if (matches > 0) {
                builder.append("; ");
            }
            builder.append(blockId(state)).append('@').append(pos).append("(old=").append(oldHit).append(",new=").append(newHit).append(')');
            matches++;
            if (matches >= 4) {
                builder.append("; ...");
                break;
            }
        }
        return matches == 0 ? "none" : builder.toString();
    }

    private static String blockId(BlockState state) {
        ResourceLocation key = BuiltInRegistries.BLOCK.getKey(state.getBlock());
        return key == null ? state.getBlock().toString() : key.toString();
    }

    private static double distanceSqr(Vec3 first, Vec3 second) {
        if (first == null || second == null) {
            return Double.POSITIVE_INFINITY;
        }
        return first.distanceToSqr(second);
    }

    private static boolean sampleEvent(String eventKey, Player player, int intervalTicks) {
        String key = eventKey + ":" + player.getUUID();
        int tick = player.tickCount;
        Integer previous = LAST_EVENT_SAMPLE_TICK.get(key);
        if (previous != null && tick - previous < intervalTicks) {
            return false;
        }
        LAST_EVENT_SAMPLE_TICK.put(key, tick);
        return true;
    }

    private static String formatVec(Vec3 vec) {
        if (vec == null) {
            return "null";
        }
        return String.format(java.util.Locale.ROOT, "%.5f,%.5f,%.5f", vec.x, vec.y, vec.z);
    }

    private static String formatAabb(AABB aabb) {
        if (aabb == null) {
            return "null";
        }
        return String.format(
                java.util.Locale.ROOT,
                "%.5f,%.5f,%.5f -> %.5f,%.5f,%.5f",
                aabb.minX,
                aabb.minY,
                aabb.minZ,
                aabb.maxX,
                aabb.maxY,
                aabb.maxZ
        );
    }

    private record PhaseSnapshot(int phaseChargeTicks,
                                 int leafPhaseChargeTicks,
                                 int leafPhaseGraceTicks,
                                 boolean phaseCharged,
                                 boolean leafPhaseActive,
                                 boolean insideLeaves,
                                 boolean insideNonLeafPhaseableBlock) {
        static PhaseSnapshot capture(String side, Player player) {
            return new PhaseSnapshot(
                    player.getPersistentData().getInt(VagrantRules.PHASE_CHARGE_TICKS),
                    player.getPersistentData().getInt(VagrantRules.LEAF_PHASE_CHARGE_TICKS),
                    player.getPersistentData().getInt(VagrantRules.LEAF_PHASE_GRACE_TICK_DATA),
                    VagrantRules.isPhaseCharged(player),
                    VagrantRules.isLeafPhaseActive(player),
                    VagrantRules.isInsideLeaves(player),
                    VagrantRules.isInsideNonLeafPhaseableBlock(player)
            );
        }

        boolean sameState(PhaseSnapshot other) {
            return other != null
                    && phaseChargeTicks == other.phaseChargeTicks
                    && leafPhaseChargeTicks == other.leafPhaseChargeTicks
                    && leafPhaseGraceTicks == other.leafPhaseGraceTicks
                    && phaseCharged == other.phaseCharged
                    && leafPhaseActive == other.leafPhaseActive
                    && insideLeaves == other.insideLeaves
                    && insideNonLeafPhaseableBlock == other.insideNonLeafPhaseableBlock;
        }
    }
}
