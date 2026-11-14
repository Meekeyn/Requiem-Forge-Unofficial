package ua.meekeyn.requiemunofficial.common.possession;

import net.neoforged.fml.common.EventBusSubscriber;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;
import ua.meekeyn.requiemunofficial.RequiemUnofficial;
import ua.meekeyn.requiemunofficial.common.soul.SoulManager;

import java.util.UUID;

// Посадка одержимого тіла на керований транспорт (кінь-нежить, спустошувач, курка, страйдер).
// Справжній пасажир - тіло-носій; власне тіло гравця нікуди не сідає. Керування йде через
// travel транспорту (див. PossessionManager), пари визначає PossessedMountRules.
@EventBusSubscriber(modid = RequiemUnofficial.MODID)
public final class UndeadHorseRidingEvents {

    private UndeadHorseRidingEvents() {
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onEntityInteract(PlayerInteractEvent.EntityInteract event) {
        if (event.getLevel().isClientSide()
                || event.getHand() != InteractionHand.MAIN_HAND
                || !(event.getEntity() instanceof ServerPlayer player)
                || !(event.getTarget() instanceof LivingEntity vehicle)) {
            return;
        }

        LivingEntity host = possessedHost(player);
        if (host == null || host == vehicle || host.isPassenger() || !vehicle.getPassengers().isEmpty()) {
            return;
        }

        // Павука обробляє SkeletonSpiderRidingEvents (провокація/бан), тут лише керовані пари.
        // Для коня AbstractHorseMixin уже вважає такого вершника осідланим.
        if (!PossessedMountRules.isSteerableRide(vehicle, host)) {
            return;
        }
        // Транспорт сам одержимий іншим гравцем: не садимо і не даємо власному тілу гравця
        // ванільно взаємодіяти з ним (клієнт пропустив клік, бо не знає про чужу одержимість).
        if (PossessedMountRules.isPossessedVehicle(vehicle)) {
            event.setCanceled(true);
            event.setCancellationResult(InteractionResult.FAIL);
            return;
        }

        if (host.startRiding(vehicle, true)) {
            event.setCanceled(true);
            event.setCancellationResult(InteractionResult.CONSUME);
        }
    }

    private static LivingEntity possessedHost(ServerPlayer player) {
        UUID hostId = SoulManager.getPossessedEntityId(player);
        if (hostId == null) {
            return null;
        }
        return PossessionManager.findPossessedHost(player, hostId);
    }
}
