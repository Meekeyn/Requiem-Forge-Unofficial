package ua.meekeyn.requiemunofficial.common.possession;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.NeutralMob;
import net.minecraft.world.entity.monster.Monster;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import ua.meekeyn.requiemunofficial.RequiemUnofficial;
import ua.meekeyn.requiemunofficial.common.gamerule.PossessionDetection;
import ua.meekeyn.requiemunofficial.common.init.ModGameRules;

import java.util.List;

@Mod.EventBusSubscriber(modid = RequiemUnofficial.MODID)
public final class PossessionDetectionManager {

    private static final double ALERT_RANGE = 50.0D;

    private PossessionDetectionManager() {
    }

    @SubscribeEvent
    public static void onLivingAttack(LivingAttackEvent event) {
        if (event.getEntity().level().isClientSide()) {
            return;
        }

        Entity attacker = event.getSource().getEntity();
        if (attacker instanceof net.minecraft.server.level.ServerPlayer player) {
            java.util.UUID hostId = ua.meekeyn.requiemunofficial.common.soul.SoulManager.getPossessedEntityId(player);
            LivingEntity host = hostId == null ? null : PossessionManager.findPossessedHost(player, hostId);
            if (host != null) {
                attacker = host;
            }
        }
        if (event.getEntity() instanceof Mob victim && attacker instanceof LivingEntity livingAttacker) {
            if (PossessionManager.getPossessorId(livingAttacker) != null) {
                PossessionManager.markProvokedByPossessedHost(livingAttacker, victim);
            } else {
                attemptDetection(livingAttacker, victim, DetectionReason.ATTACKED);
            }
        }
    }

    public static void detectBumps(LivingEntity host) {
        List<Monster> touching = host.level().getEntitiesOfClass(
                Monster.class,
                host.getBoundingBox().inflate(0.15D),
                mob -> mob != host && mob.isAlive()
        );
        for (Monster sensor : touching) {
            attemptDetection(host, sensor, DetectionReason.BUMP);
        }
    }

    public static void attemptDetection(LivingEntity sensed, Mob sensor, DetectionReason reason) {
        if (!(sensor instanceof Monster)
                || sensor instanceof NeutralMob
                || PossessionManager.getPossessorId(sensed) == null) {
            return;
        }

        DetectionResult result = resultFor(
                ModGameRules.getPossessionDetection(sensed.level().getGameRules()),
                reason
        );
        if (result == DetectionResult.DETECTED) {
            PossessionManager.markDetectedByPossessedHost(sensed, sensor);
            sensor.setTarget(sensed);
        } else if (result == DetectionResult.CROWD_DETECTED) {
            inciteCrowd(sensed, sensor);
        }
    }

    public static DetectionResult resultFor(PossessionDetection mode, DetectionReason reason) {
        if (mode == PossessionDetection.DISABLED) {
            return DetectionResult.UNDETECTED;
        }
        if (reason == DetectionReason.ATTACKED
                || mode == PossessionDetection.HARD) {
            return DetectionResult.CROWD_DETECTED;
        }
        return DetectionResult.UNDETECTED;
    }

    private static void inciteCrowd(LivingEntity sensed, Mob sensor) {
        PossessionManager.markDetectedByPossessedHost(sensed, sensor);
        for (Monster witness : sensed.level().getEntitiesOfClass(
                Monster.class,
                sensor.getBoundingBox().inflate(ALERT_RANGE),
                mob -> mob != sensor
                        && !(mob instanceof NeutralMob)
                        && mob.isAlive()
                        && mob.hasLineOfSight(sensed)
        )) {
            PossessionManager.markDetectedByPossessedHost(sensed, witness);
        }
    }

    public enum DetectionReason {
        BUMP,
        ATTACKING,
        ATTACKED
    }

    public enum DetectionResult {
        UNDETECTED,
        DETECTED,
        CROWD_DETECTED
    }
}
