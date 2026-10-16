package ua.meekeyn.requiemunofficial.common.possession;

import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingExperienceDropEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import ua.meekeyn.requiemunofficial.RequiemUnofficial;
import ua.meekeyn.requiemunofficial.common.soul.SoulManager;
import ua.meekeyn.requiemunofficial.common.util.ModTags;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Mod.EventBusSubscriber(modid = RequiemUnofficial.MODID)
public final class HumanityKillManager {

    private static final Map<UUID, Integer> HUMANITY_DEATHS = new HashMap<>();

    private HumanityKillManager() {
    }

    @SubscribeEvent
    public static void onLivingDeath(LivingDeathEvent event) {
        if (event.getEntity().level().isClientSide()) {
            return;
        }

        int humanityLevel = getHumanityLevel(event.getSource());
        if (humanityLevel > 0) {
            HUMANITY_DEATHS.put(event.getEntity().getUUID(), humanityLevel);
        }
    }

    @SubscribeEvent
    public static void onExperienceDrop(LivingExperienceDropEvent event) {
        if (event.getEntity().level().isClientSide()) {
            return;
        }

        int humanityLevel = HUMANITY_DEATHS.getOrDefault(event.getEntity().getUUID(), 0);
        HUMANITY_DEATHS.remove(event.getEntity().getUUID());
        if (shouldSuppressExperience(humanityLevel)) {
            event.setDroppedExperience(0);
        }
    }

    public static int getHumanityLevel(DamageSource source) {
        LivingEntity possessedAttacker = getPossessedAttacker(source);
        if (possessedAttacker == null || !possessedAttacker.getType().is(ModTags.ITEM_USERS)) {
            return 0;
        }
        return PossessedItemManager.getHumanityLevel(possessedAttacker);
    }

    public static boolean shouldSuppressExperience(int humanityLevel) {
        return humanityLevel == 1;
    }

    public static int adjustExperienceDropForTest(int humanityLevel, int originalExperience) {
        return shouldSuppressExperience(humanityLevel) ? 0 : originalExperience;
    }

    private static LivingEntity getPossessedAttacker(DamageSource source) {
        Entity attacker = source.getEntity();
        if (attacker instanceof ServerPlayer player) {
            UUID hostId = SoulManager.getPossessedEntityId(player);
            Entity vehicle = player.getVehicle();
            if (hostId != null
                    && vehicle instanceof LivingEntity host
                    && hostId.equals(host.getUUID())
                    && player.getUUID().equals(PossessionManager.getPossessorId(host))) {
                return host;
            }
        }

        if (attacker instanceof LivingEntity living && PossessionManager.getPossessorId(living) != null) {
            return living;
        }
        return null;
    }
}
