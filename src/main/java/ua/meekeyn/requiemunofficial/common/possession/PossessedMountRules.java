package ua.meekeyn.requiemunofficial.common.possession;

import net.minecraft.tags.EntityTypeTags;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobType;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.Chicken;
import net.minecraft.world.entity.animal.horse.AbstractHorse;
import net.minecraft.world.entity.monster.Ravager;
import net.minecraft.world.entity.monster.Spider;
import net.minecraft.world.entity.monster.Strider;
import ua.meekeyn.requiemunofficial.common.util.ModTags;

/**
 * Єдині правила пар «одержимий вершник -> транспорт» (аналог beta.16 MobRidingType).
 * Павук: скелет лише сідає верхи, власний ШІ павука має пріоритет (MOUNT).
 * Решта пар керуються гравцем як кінь (RIDE): нежить на коні-нежиті, рейдер на спустошувачі,
 * дитинча зомбі на курці, піглін/зомбований піглін на страйдері.
 */
public final class PossessedMountRules {
    // Ванільні коефіцієнти швидкості страйдера під вершником (Strider#getRiddenSpeed без бусту):
    // у 1.20.1 це isSuffocating() ? 0.35F : 0.55F (звірено з байткодом m_245547_).
    private static final double STRIDER_RIDDEN_SPEED = 0.55D;
    private static final double STRIDER_COLD_RIDDEN_SPEED = 0.35D;

    private PossessedMountRules() {
    }

    public static boolean canMount(Entity vehicle, LivingEntity rider) {
        // Транспорт, яким уже керує інший одержимий гравець, не можна осідлати (на клієнті
        // persistentData не синхронізується, тож остаточно це відсікає сервер).
        if (isPossessedVehicle(vehicle)) {
            return false;
        }
        return isSpiderRide(vehicle, rider) || isSteerableRide(vehicle, rider);
    }

    public static boolean isSpiderRide(Entity vehicle, LivingEntity rider) {
        return vehicle instanceof Spider && rider.getType().is(ModTags.HUMANOID_SKELETONS);
    }

    public static boolean isSteerableRide(Entity vehicle, LivingEntity rider) {
        if (vehicle instanceof AbstractHorse horse) {
            // Лише нежить на коні-нежиті (скелет -> кінь-скелет, зомбі -> кінь-зомбі).
            return horse.getMobType() == MobType.UNDEAD
                    && rider.getMobType() == MobType.UNDEAD
                    && (rider.getType().is(ModTags.HUMANOID_SKELETONS)
                    || rider.getType().is(ModTags.HUMANOID_ZOMBIES));
        }
        if (vehicle instanceof Ravager) {
            // beta.16: будь-який рейдер (ілагери, відьма), окрім самого спустошувача.
            return rider.getType().is(EntityTypeTags.RAIDERS) && !(rider instanceof Ravager);
        }
        if (vehicle instanceof Chicken) {
            // Курячий жокей: лише дитинча зомбі-гуманоїда (зомбі, хаск, утоплений, зомбований піглін...).
            return rider.isBaby() && rider.getType().is(ModTags.HUMANOID_ZOMBIES);
        }
        if (vehicle instanceof Strider) {
            return rider.getType() == EntityType.ZOMBIFIED_PIGLIN || rider.getType().is(ModTags.PIGLINS);
        }
        return false;
    }

    public static boolean isPossessedVehicle(Entity vehicle) {
        return vehicle instanceof LivingEntity living && PossessionManager.getPossessorId(living) != null;
    }

    // Швидкість, з якою транспорт їде під одержимим вершником.
    public static float steeringSpeed(LivingEntity vehicle) {
        double speed = vehicle.getAttributeValue(Attributes.MOVEMENT_SPEED);
        if (vehicle instanceof Strider strider) {
            speed *= strider.isSuffocating() ? STRIDER_COLD_RIDDEN_SPEED : STRIDER_RIDDEN_SPEED;
        }
        return (float) speed;
    }
}
