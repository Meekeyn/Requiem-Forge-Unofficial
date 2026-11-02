package ua.meekeyn.requiemunofficial.common.possession;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import ua.meekeyn.requiemunofficial.common.soul.SoulManager;

import java.util.UUID;

/**
 * Ванільні правила середовища для одержимих тіл: дихання, повітря, порошковий сніг.
 * Тіло лише телепортується за гравцем (без Entity#move), тож частину стану беремо з гравця.
 */
public final class PossessedEnvironment {

    private PossessedEnvironment() {
    }

    /** Чи є сутність одержимим тілом (лише сервер: позначка живе в ForgeData). */
    public static boolean isPossessedHost(Entity entity) {
        return entity instanceof LivingEntity living
                && !living.level().isClientSide()
                && PossessionManager.getPossessorId(living) != null;
    }

    /** Тіло, у якому зараз гравець (сервер і клієнт), або null. */
    public static LivingEntity findBody(Player player) {
        UUID hostId = SoulManager.getPossessedEntityId(player);
        if (hostId == null) {
            return null;
        }
        if (player instanceof ServerPlayer serverPlayer) {
            LivingEntity host = PossessionManager.findPossessedHost(serverPlayer, hostId);
            return host != null && host.isAlive() ? host : null;
        }
        for (Entity entity : player.level().getEntities(
                player,
                player.getBoundingBox().inflate(128.0D),
                entity -> hostId.equals(entity.getUUID())
        )) {
            if (entity instanceof LivingEntity living && living.isAlive()) {
                return living;
            }
        }
        return null;
    }

    /**
     * Дихання гравця-власника віддається тілу: тіло саме витрачає повітря і тоне за ванільними
     * правилами (нежить, риба, аксолотль), а гравець не тоне вдруге через переадресацію шкоди.
     */
    public static boolean breathesThroughBody(Player player) {
        return findBody(player) != null;
    }

    /** Повітря тіла в масштабі шкали гравця, щоб HUD-бульбашки показували стан тіла. */
    public static void mirrorBodyAir(Player player) {
        LivingEntity host = findBody(player);
        if (host == null) {
            return;
        }
        int hostMax = Math.max(1, host.getMaxAirSupply());
        int air = (int) Math.round((double) host.getAirSupply() * player.getMaxAirSupply() / hostMax);
        player.setAirSupply(Math.max(0, Math.min(player.getMaxAirSupply(), air)));
    }

    /** Чи стоїть власник тіла в порошковому снігу (стан тіла не оновлюється без Entity#move). */
    public static boolean isPossessorInPowderSnow(LivingEntity host) {
        if (!(host.level() instanceof ServerLevel level)) {
            return false;
        }
        UUID possessorId = PossessionManager.getPossessorId(host);
        if (possessorId == null) {
            return false;
        }
        ServerPlayer possessor = PossessionManager.getActivePossessor(level, possessorId);
        return possessor != null
                && possessor.level() == host.level()
                && (possessor.isInPowderSnow || possessor.wasInPowderSnow);
    }
}
