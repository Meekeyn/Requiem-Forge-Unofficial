package ua.meekeyn.requiemunofficial.common.possession;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Spider;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.event.entity.living.LivingChangeTargetEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import ua.meekeyn.requiemunofficial.RequiemUnofficial;
import ua.meekeyn.requiemunofficial.common.soul.SoulManager;
import ua.meekeyn.requiemunofficial.common.util.ModTags;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Mod.EventBusSubscriber(modid = RequiemUnofficial.MODID)
public final class SkeletonSpiderRidingEvents {
    private static final String PROVOKED_BY_RIDER = "RequiemSkeletonRiderProvoked";
    private static final String PROVOKED_BY_RIDER_UNTIL = "RequiemSkeletonRiderProvokedUntil";
    private static final int RIDER_PROVOKE_TICKS = 100;
    // Tracks spiders whose rider provoked them so a lightweight tick pass can let the aggression
    // expire naturally (which re-allows mounting) without scanning every entity each tick.
    private static final Map<UUID, Long> PROVOKED_SPIDERS = new HashMap<>();

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

        // A spider that is aggressive toward this very skeleton must not be mounted until that
        // aggression expires. Aggression toward unrelated third mobs does NOT block mounting.
        if (spider.getTarget() == host) {
            return;
        }

        if (host.startRiding(spider, true)) {
            event.setCanceled(true);
            event.setCancellationResult(InteractionResult.CONSUME);
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onLivingChangeTarget(LivingChangeTargetEvent event) {
        if (!(event.getEntity() instanceof Spider spider)
                || event.getNewTarget() == null
                || !isPossessedSkeleton(event.getNewTarget())) {
            return;
        }

        // Never let a spider acquire its own (unprovoked) possessed-skeleton rider as a target.
        // When the rider provokes the spider, onLivingAttack marks it provoked first, so this
        // guard is skipped and the spider is allowed to turn on the rider.
        if (!spider.getPersistentData().getBoolean(PROVOKED_BY_RIDER)
                && event.getNewTarget().isPassengerOfSameVehicle(spider)) {
            event.setCanceled(true);
            event.setNewTarget(null);
            spider.setTarget(null);
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onLivingAttack(LivingAttackEvent event) {
        Entity attacker = event.getSource().getEntity();
        if (event.getEntity() instanceof Spider spider
                && attacker instanceof LivingEntity living
                && isPossessedSkeleton(living)
                && living.isPassengerOfSameVehicle(spider)) {
            // The rider struck its own spider: mark the spider provoked (so targeting the rider is
            // allowed), turn it on the rider, and buck the rider off. Re-mounting stays banned until
            // the provoke window below expires.
            long until = spider.level().getGameTime() + RIDER_PROVOKE_TICKS;
            spider.getPersistentData().putBoolean(PROVOKED_BY_RIDER, true);
            spider.getPersistentData().putLong(PROVOKED_BY_RIDER_UNTIL, until);
            PROVOKED_SPIDERS.put(spider.getUUID(), until);
            PossessionManager.markDetectedByPossessedHost(living, spider);
            living.stopRiding();
            return;
        }

        // A spider must never retaliate against its own unprovoked rider.
        if (event.getSource().getEntity() instanceof Spider spider
                && !spider.getPersistentData().getBoolean(PROVOKED_BY_RIDER)
                && isUnprovokedSkeletonRider(spider, event.getEntity())) {
            event.setCanceled(true);
            spider.setTarget(null);
        }
    }

    @SubscribeEvent
    public static void onWorldTick(TickEvent.LevelTickEvent event) {
        if (event.phase != TickEvent.Phase.END || event.level.isClientSide() || PROVOKED_SPIDERS.isEmpty()) {
            return;
        }
        long now = event.level.getGameTime();
        PROVOKED_SPIDERS.entrySet().removeIf(entry -> {
            if (entry.getValue() > now) {
                return false;
            }
            if (event.level instanceof net.minecraft.server.level.ServerLevel serverLevel) {
                net.minecraft.world.entity.Entity entity = serverLevel.getEntity(entry.getKey());
                if (entity instanceof Spider spider) {
                    spider.getPersistentData().remove(PROVOKED_BY_RIDER);
                    spider.getPersistentData().remove(PROVOKED_BY_RIDER_UNTIL);
                    if (spider.getTarget() != null && isPossessedSkeleton(spider.getTarget())) {
                        spider.setTarget(null);
                    }
                }
            }
            return true;
        });
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
