package ua.meekeyn.requiemunofficial.common.ritual;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.level.block.RespawnAnchorBlock;
import net.minecraft.world.level.Level;
import ua.meekeyn.requiemunofficial.common.block.RiftRunestoneBlock;
import ua.meekeyn.requiemunofficial.common.block.entity.RunestoneBlockEntity;
import ua.meekeyn.requiemunofficial.common.config.RequiemConfig;
import ua.meekeyn.requiemunofficial.common.effect.AttritionMobEffect;
import ua.meekeyn.requiemunofficial.common.init.ModEffects;
import ua.meekeyn.requiemunofficial.common.record.ObeliskRecordData;
import ua.meekeyn.requiemunofficial.common.remnant.PlayerRiftTracker;

import java.util.Comparator;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class RiftTeleportManager {

    private static final Map<ResourceKey<Level>, Set<BlockPos>> RIFTS = new ConcurrentHashMap<>();

    private RiftTeleportManager() {
    }

    public static void register(ServerLevel level, BlockPos pos) {
        RIFTS.computeIfAbsent(level.dimension(), key -> ConcurrentHashMap.newKeySet())
                .add(pos.immutable());
    }

    public static void unregister(ServerLevel level, BlockPos pos) {
        Set<BlockPos> positions = RIFTS.get(level.dimension());
        if (positions != null) {
            positions.remove(pos);
        }
    }

    public static boolean teleport(ServerPlayer player, BlockPos sourcePos) {
        return teleport(player, sourcePos, null);
    }

    public static boolean teleport(ServerPlayer player, BlockPos sourcePos, UUID targetRecordId) {
        ServerLevel sourceLevel = player.serverLevel();
        int energyCost = RequiemConfig.RIFT_TELEPORT_ENERGY_COST.get();
        if (!(sourceLevel.getBlockEntity(sourcePos) instanceof RunestoneBlockEntity source)
                || !RiftRunestoneBlock.canUseRift(player, source)
                || !isUsableRift(sourceLevel, sourcePos, source)
                || source.getStoredSoulEnergy() < energyCost) {
            return fail(player, "message.requiemunofficial.rift.teleport_unavailable");
        }

        RiftTarget target = targetRecordId == null
                ? findKnownTarget(player, sourceLevel, sourcePos)
                : findTargetByRecord(player, sourceLevel, sourcePos, targetRecordId);
        if (target == null) {
            return fail(player, "message.requiemunofficial.rift.no_destination");
        }

        if (source.consumeStoredSoulEnergy(energyCost) != energyCost) {
            return fail(player, "message.requiemunofficial.ritual.insufficient_energy");
        }

        BlockPos arrival = findSafeArrival(target.level, target.pos);
        float yaw = yawTowardObelisk(arrival, target.pos);
        if (player.connection == null && target.level == player.serverLevel()) {
            player.moveTo(arrival.getX() + 0.5D, arrival.getY(), arrival.getZ() + 0.5D, yaw, player.getXRot());
        } else {
            player.teleportTo(
                    target.level,
                    arrival.getX() + 0.5D,
                    arrival.getY(),
                    arrival.getZ() + 0.5D,
                    yaw,
                    player.getXRot()
            );
        }
        AttritionMobEffect.apply(player, 1, getAttritionLength(sourcePos, target.pos));
        return true;
    }

    public static int getAttritionLength(BlockPos source, BlockPos target) {
        double distanceTraveled = Math.sqrt(source.distSqr(target));
        int minLength = 20 * 60 * 15;
        if (distanceTraveled <= 1.0D) {
            return minLength;
        }
        double logBase = Math.log(Math.pow(10, 0.003));
        int distancePenalty = (int) (Math.log(distanceTraveled) / logBase * 6);
        return minLength + Math.max(0, distancePenalty);
    }

    public static Optional<BlockPos> resolveTeleportTargetForTest(ServerPlayer player, BlockPos sourcePos,
                                                                  UUID targetRecordId) {
        RiftTarget target = findTargetByRecord(player, player.serverLevel(), sourcePos, targetRecordId);
        return target == null ? Optional.empty() : Optional.of(target.pos());
    }

    private static RiftTarget findKnownTarget(ServerPlayer player, ServerLevel sourceLevel, BlockPos sourcePos) {
        return PlayerRiftTracker.fetchKnownObelisks(player).stream()
                .filter(descriptor -> !descriptor.dimension().equals(sourceLevel.dimension().location())
                        || !descriptor.pos().equals(sourcePos))
                .map(descriptor -> findTargetByRecord(player, sourceLevel, sourcePos, descriptor.recordId()))
                .filter(java.util.Objects::nonNull)
                .min(Comparator.comparingDouble(target -> target.pos.distSqr(sourcePos)))
                .orElse(null);
    }

    private static RiftTarget findTargetByRecord(ServerPlayer player, ServerLevel sourceLevel,
                                                 BlockPos sourcePos, UUID recordId) {
        ObeliskRecordData data = ObeliskRecordData.get(player.getServer());
        Optional<ObeliskRecordData.ObeliskRecord> optionalRecord = data.getRecord(recordId);
        if (optionalRecord.isEmpty()) {
            return null;
        }
        ObeliskRecordData.ObeliskRecord record = optionalRecord.get();
        if (!record.dimension().equals(sourceLevel.dimension().location())
                || record.pos().equals(sourcePos)) {
            return null;
        }
        if (!data.validateLoadedDestination(player.getServer(), recordId)) {
            return null;
        }
        ServerLevel targetLevel = player.getServer().getLevel(ResourceKey.create(
                net.minecraft.core.registries.Registries.DIMENSION,
                record.dimension()
        ));
        if (targetLevel == null || !targetLevel.isLoaded(record.pos())) {
            return null;
        }
        if (!(targetLevel.getBlockEntity(record.pos()) instanceof RunestoneBlockEntity target)
                || !isUsableRift(targetLevel, record.pos(), target)) {
            data.invalidate(recordId);
            return null;
        }
        return new RiftTarget(targetLevel, record.pos());
    }

    private static boolean isUsableRift(ServerLevel level, BlockPos pos, RunestoneBlockEntity runestone) {
        boolean valid = runestone.isHeadBlock()
                && runestone.isStructureValid()
                && runestone.isActivatedBlock()
                && runestone.isPowered()
                && runestone.getRuneLevel("rift") > 0;
        if (!valid) {
            unregister(level, pos);
        }
        return valid;
    }

    private static BlockPos findSafeArrival(ServerLevel level, BlockPos headPos) {
        Optional<net.minecraft.world.phys.Vec3> respawn =
                RespawnAnchorBlock.findStandUpPosition(net.minecraft.world.entity.EntityType.PLAYER, level, headPos);
        if (respawn.isPresent()) {
            net.minecraft.world.phys.Vec3 pos = respawn.get();
            return BlockPos.containing(pos.x, pos.y, pos.z);
        }
        int top = headPos.getY();
        if (level.getBlockEntity(headPos) instanceof RunestoneBlockEntity runestone) {
            top += Math.max(1, runestone.getObeliskCoreHeight()) + 1;
        }
        BlockPos.MutableBlockPos checked = new BlockPos(headPos.getX(), top, headPos.getZ()).mutable();
        for (int offset = 0; offset < 8; offset++) {
            if (level.getBlockState(checked).getCollisionShape(level, checked).isEmpty()
                    && level.getBlockState(checked.above()).getCollisionShape(level, checked.above()).isEmpty()) {
                return checked.immutable();
            }
            checked.move(0, 1, 0);
        }
        return checked.immutable();
    }

    private static float yawTowardObelisk(BlockPos arrival, BlockPos target) {
        double x = target.getX() + 0.5D - (arrival.getX() + 0.5D);
        double z = target.getZ() + 0.5D - (arrival.getZ() + 0.5D);
        return (float) Mth.wrapDegrees(Mth.atan2(z, x) * 180.0D / Math.PI - 90.0D);
    }

    private static boolean fail(ServerPlayer player, String key) {
        player.sendSystemMessage(Component.translatable(key));
        return false;
    }

    private record RiftTarget(ServerLevel level, BlockPos pos) {
    }
}
