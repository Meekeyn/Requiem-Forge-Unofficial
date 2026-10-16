package ua.meekeyn.requiemunofficial.common.possession;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.Spider;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.event.entity.living.LivingChangeTargetEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import ua.meekeyn.requiemunofficial.RequiemUnofficial;
import ua.meekeyn.requiemunofficial.common.soul.SoulManager;
import ua.meekeyn.requiemunofficial.common.util.ModTags;

import java.util.UUID;

@Mod.EventBusSubscriber(modid = RequiemUnofficial.MODID)
public final class SkeletonSpiderRidingEvents {
    private static final String PROVOKED_BY_RIDER = "RequiemSkeletonRiderProvoked";

    private SkeletonSpiderRidingEvents() {
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onEntityInteract(PlayerInteractEvent.EntityInteract event) {
        if (event.getLevel().isClientSide()
                || event.getHand() != InteractionHand.MAIN_HAND
                || !(event.getEntity() instanceof ServerPlayer player)
                || !(event.getTarget() instanceof Spider spider)) {
            return;
        }

        LivingEntity host = possessedSkeletonHost(player);
        if (host == null || host.isPassenger() || !spider.getPassengers().isEmpty()) {
            return;
        }

        spider.getPersistentData().remove(PROVOKED_BY_RIDER);
        if (host.startRiding(spider, true)) {
            if (spider.getTarget() == host) {
                spider.setTarget(null);
                spider.getNavigation().stop();
            }
            event.setCanceled(true);
            event.setCancellationResult(InteractionResult.CONSUME);
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onLivingChangeTarget(LivingChangeTargetEvent event) {
        if (!(event.getEntity() instanceof Spider spider)
                || spider.getPersistentData().getBoolean(PROVOKED_BY_RIDER)
                || event.getNewTarget() == null
                || !isUnprovokedSkeletonRider(spider, event.getNewTarget())) {
            return;
        }

        event.setCanceled(true);
        event.setNewTarget(null);
        spider.setTarget(null);
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onLivingAttack(LivingAttackEvent event) {
        Entity attacker = event.getSource().getEntity();
        if (event.getEntity() instanceof Spider spider
                && attacker instanceof LivingEntity living
                && isPossessedSkeleton(living)) {
            spider.getPersistentData().putBoolean(PROVOKED_BY_RIDER, true);
            return;
        }

        if (event.getSource().getEntity() instanceof Spider spider
                && !spider.getPersistentData().getBoolean(PROVOKED_BY_RIDER)
                && isUnprovokedSkeletonRider(spider, event.getEntity())) {
            event.setCanceled(true);
            spider.setTarget(null);
        }
    }

    private static LivingEntity possessedSkeletonHost(ServerPlayer player) {
        UUID hostId = SoulManager.getPossessedEntityId(player);
        if (hostId == null) {
            return null;
        }
        LivingEntity host = PossessionManager.findPossessedHost(player, hostId);
        return host != null && isPossessedSkeleton(host) ? host : null;
    }

    private static boolean isUnprovokedSkeletonRider(Spider spider, LivingEntity target) {
        return target.isPassengerOfSameVehicle(spider)
                && isPossessedSkeleton(target);
    }

    private static boolean isPossessedSkeleton(LivingEntity entity) {
        return entity.getType().is(ModTags.HUMANOID_SKELETONS)
                && PossessionManager.getPossessorId(entity) != null;
    }
}
