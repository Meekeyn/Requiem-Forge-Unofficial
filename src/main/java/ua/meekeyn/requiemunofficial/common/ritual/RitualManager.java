package ua.meekeyn.requiemunofficial.common.ritual;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.AABB;
import ua.meekeyn.requiemunofficial.common.block.entity.RunestoneBlockEntity;
import ua.meekeyn.requiemunofficial.common.block.obelisk.ObeliskMatcher;
import ua.meekeyn.requiemunofficial.common.transformation.TransformationManager;
import ua.meekeyn.requiemunofficial.common.util.ModTags;
import ua.meekeyn.requiemunofficial.common.soul.SoulManager;

import java.util.Comparator;

public final class RitualManager {

    private RitualManager() {
    }

    public static boolean tryStart(ServerPlayer player, BlockPos headPos, RitualType type) {
        ServerLevel level = player.serverLevel();
        if (!(level.getBlockEntity(headPos) instanceof RunestoneBlockEntity runestone)) {
            return fail(player, "message.requiemunofficial.ritual.invalid_rift");
        }

        if (player.isSpectator()
                || !player.mayBuild()
                || !player.mayUseItemAt(headPos, Direction.UP, ItemStack.EMPTY)) {
            return fail(player, "message.requiemunofficial.ritual.no_permission");
        }

        if (player.distanceToSqr(
                headPos.getX() + 0.5D,
                headPos.getY() + 0.5D,
                headPos.getZ() + 0.5D
        ) > 64.0D) {
            return fail(player, "message.requiemunofficial.ritual.too_far");
        }

        if (!runestone.isHeadBlock()
                || !runestone.isStructureValid()
                || !runestone.isActivatedBlock()
                || !runestone.isPowered()
                || ObeliskMatcher.matchObelisk(level, headPos).isEmpty()) {
            return fail(player, "message.requiemunofficial.ritual.invalid_rift");
        }

        if (runestone.hasPendingRitual()) {
            return fail(player, "message.requiemunofficial.ritual.already_active");
        }

        if (runestone.getRuneLevel(type.requiredRune()) < type.requiredRuneLevel()) {
            return fail(player, "message.requiemunofficial.ritual.missing_rune");
        }

        LivingEntity target = findTarget(level, headPos, type);
        if (type != RitualType.RESURRECTION && target == null) {
            return fail(player, "message.requiemunofficial.ritual.no_target");
        }

        if (runestone.getStoredSoulEnergy() < type.energyCost()) {
            player.sendSystemMessage(Component.translatable(
                    "message.requiemunofficial.ritual.insufficient_energy",
                    type.energyCost()
            ));
            return false;
        }

        if (runestone.consumeStoredSoulEnergy(type.energyCost()) != type.energyCost()) {
            return fail(player, "message.requiemunofficial.ritual.insufficient_energy");
        }

        long endGameTime = level.getGameTime() + type.durationTicks();
        runestone.startRitual(
                type.id(),
                endGameTime,
                player.getUUID(),
                target == null ? null : target.getUUID()
        );
        return true;
    }

    public static void completePending(ServerLevel level, BlockPos headPos, RunestoneBlockEntity runestone) {
        if (!runestone.isRitualReady(level.getGameTime())) {
            return;
        }

        RitualType type = RitualType.fromId(runestone.getPendingRitualId()).orElse(null);
        ServerPlayer player = runestone.getRitualPlayerId() == null
                ? null
                : level.getServer().getPlayerList().getPlayer(runestone.getRitualPlayerId());
        Entity entity = runestone.getRitualTargetId() == null
                ? null
                : level.getEntity(runestone.getRitualTargetId());
        LivingEntity target = entity instanceof LivingEntity living ? living : null;

        runestone.clearRitual();

        if (type == null || player == null) {
            return;
        }

        if (!runestone.isHeadBlock()
                || !runestone.isStructureValid()
                || !runestone.isActivatedBlock()
                || !runestone.isPowered()
                || runestone.getRuneLevel(type.requiredRune()) < type.requiredRuneLevel()) {
            player.sendSystemMessage(Component.translatable(
                    "message.requiemunofficial.ritual.interrupted"
            ));
            return;
        }

        if (type != RitualType.RESURRECTION && (target == null || !target.isAlive())) {
            player.sendSystemMessage(Component.translatable(
                    "message.requiemunofficial.ritual.target_lost"
            ));
            return;
        }

        applyResult(player, headPos, type, target);
    }

    private static LivingEntity findTarget(ServerLevel level, BlockPos headPos, RitualType type) {
        if (type == RitualType.RESURRECTION) {
            return null;
        }

        AABB area = new AABB(headPos).inflate(8.0D);
        return level.getEntitiesOfClass(Mob.class, area, entity -> switch (type) {
                    case DECAY_ZOMBIE -> entity.getType().is(ModTags.HUMANOID_SKELETONS);
                    case DECAY_SKELETON -> entity.getType().is(ModTags.HUMANOID_ZOMBIES);
                    default -> false;
                }).stream()
                .min(Comparator.comparingDouble(entity -> entity.distanceToSqr(
                        headPos.getX() + 0.5D,
                        headPos.getY() + 0.5D,
                        headPos.getZ() + 0.5D
                )))
                .orElse(null);
    }

    private static void applyResult(ServerPlayer player, BlockPos headPos, RitualType type, LivingEntity target) {
        switch (type) {
            case RESURRECTION -> SoulManager.setResurrectionPoint(
                    player,
                    player.serverLevel().dimension().location().toString(),
                    headPos
            );
            case DECAY_ZOMBIE -> TransformationManager.decayToZombie(target);
            case DECAY_SKELETON -> TransformationManager.decayToSkeleton(target);
        }
    }

    private static boolean fail(ServerPlayer player, String translationKey) {
        player.sendSystemMessage(Component.translatable(translationKey));
        return false;
    }
}
