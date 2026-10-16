package ua.meekeyn.requiemunofficial.common.possession;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobType;
import net.minecraft.world.entity.animal.horse.AbstractHorse;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import ua.meekeyn.requiemunofficial.RequiemUnofficial;
import ua.meekeyn.requiemunofficial.common.soul.SoulManager;
import ua.meekeyn.requiemunofficial.common.util.ModTags;

import java.util.UUID;

// Lets a possessed humanoid (skeleton/zombie) mount a matching undead horse. The possessed host is
// the real passenger; the player's own body never mounts anything. Riding a horse gives full
// vanilla-style control through the vehicle's travel pipeline (see PossessionManager).
@Mod.EventBusSubscriber(modid = RequiemUnofficial.MODID)
public final class UndeadHorseRidingEvents {

    private UndeadHorseRidingEvents() {
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onEntityInteract(PlayerInteractEvent.EntityInteract event) {
        if (event.getLevel().isClientSide()
                || event.getHand() != InteractionHand.MAIN_HAND
                || !(event.getEntity() instanceof ServerPlayer player)
                || !(event.getTarget() instanceof AbstractHorse horse)) {
            return;
        }

        LivingEntity host = possessedHumanoidHost(player);
        if (host == null || host.isPassenger() || !horse.getPassengers().isEmpty()) {
            return;
        }

        // Only an undead host may ride an undead horse (skeleton -> skeleton horse,
        // zombie -> zombie horse). AbstractHorseMixin already treats this rider as saddled.
        if (host.getMobType() != MobType.UNDEAD || horse.getMobType() != MobType.UNDEAD) {
            return;
        }

        if (host.startRiding(horse, true)) {
            event.setCanceled(true);
            event.setCancellationResult(InteractionResult.CONSUME);
        }
    }

    private static LivingEntity possessedHumanoidHost(ServerPlayer player) {
        UUID hostId = SoulManager.getPossessedEntityId(player);
        if (hostId == null) {
            return null;
        }
        LivingEntity host = PossessionManager.findPossessedHost(player, hostId);
        if (host == null) {
            return null;
        }
        boolean humanoid = host.getType().is(ModTags.HUMANOID_SKELETONS)
                || host.getType().is(ModTags.HUMANOID_ZOMBIES);
        return humanoid ? host : null;
    }
}
