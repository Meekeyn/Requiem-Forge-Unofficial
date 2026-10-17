package ua.meekeyn.requiemunofficial.common.possession;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.Squid;
import net.minecraft.world.entity.ambient.Bat;
import net.minecraft.world.phys.Vec3;
import ua.meekeyn.requiemunofficial.common.util.ModTags;
import net.minecraft.core.registries.BuiltInRegistries;
import java.lang.reflect.Method;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import ua.meekeyn.requiemunofficial.RequiemUnofficial;

/** Єдина підготовка руху перед штатним викликом travel, без додаткової фізики. */
public final class PossessionMovementController {
    private static final String ORIGINAL_FLYING = "RequiemOriginalFlying";
    private static final Set<Class<?>> FAILED_FLIGHT_SETTERS = ConcurrentHashMap.newKeySet();
    private static final ClassValue<Optional<Method>> FLIGHT_SETTERS = new ClassValue<>() {
        @Override
        protected Optional<Method> computeValue(Class<?> type) {
            try {
                return Optional.of(type.getMethod("setFlying", boolean.class));
            } catch (NoSuchMethodException exception) {
                return Optional.empty();
            }
        }
    };
    private PossessionMovementController() {
    }

    public static void travel(LivingEntity body, Vec3 input) {
        // Відображуване тіло копіює позицію гравця; друга фізика зсунула б його повторно.
        boolean clientProxy = body.level().isClientSide() && PossessionManager.usesExtendedPlayerBody(body)
                && body.level().players().stream().anyMatch(player -> PossessionManager.isBeta16HostForPlayer(player, body));
        if (PossessionManager.usesBeta16PlayerBody(body)
                && (PossessionManager.getPossessorId(body) != null || clientProxy)) {
            body.calculateEntityAnimation(false);
            return;
        }
        body.travel(body.level().isClientSide() ? input
                : PossessionManager.redirectPossessedTravelVector(body, input));
    }

    public static double verticalSpeed(double current, double speed, double inertia,
                                       boolean jumping, boolean shifting) {
        return jumping != shifting ? (jumping ? speed : -speed)
                : current * Math.max(0.0D, Math.min(1.0D, inertia));
    }

    public static void restoreFlightState(LivingEntity body) {
        if (!body.getPersistentData().contains(ORIGINAL_FLYING)) {
            return;
        }
        FLIGHT_SETTERS.get(body.getClass()).ifPresent(setter -> {
            try {
                setter.invoke(body, body.getPersistentData().getBoolean(ORIGINAL_FLYING));
                body.getPersistentData().remove(ORIGINAL_FLYING);
            } catch (ReflectiveOperationException exception) {
                RequiemUnofficial.LOGGER.warn("Cannot restore flight state for {}", body.getType(), exception);
            }
        });
    }

    public static void updateFlightState(LivingEntity body, EntityMobilityManager.MobilityProfile profile, boolean flying) {
        if (profile.canFly() && !FAILED_FLIGHT_SETTERS.contains(body.getClass())
                && "alexsmobs".equals(BuiltInRegistries.ENTITY_TYPE.getKey(body.getType()).getNamespace())) {
            // Використовуємо власний стан польоту мода для навігації та анімації крил.
            FLIGHT_SETTERS.get(body.getClass()).ifPresent(setter -> {
                try {
                    if (!body.getPersistentData().contains(ORIGINAL_FLYING)) {
                        boolean original = (boolean) body.getClass().getMethod("isFlying").invoke(body);
                        body.getPersistentData().putBoolean(ORIGINAL_FLYING, original);
                    }
                    setter.invoke(body, flying);
                } catch (ReflectiveOperationException exception) {
                    FAILED_FLIGHT_SETTERS.add(body.getClass());
                    RequiemUnofficial.LOGGER.warn("Cannot update possessed flight state for {}", body.getType(), exception);
                }
            });
        }
    }

    public static Vec3 prepareTravel(LivingEntity body, Vec3 input,
                                     EntityMobilityManager.MobilityProfile profile) {
        if (body.getType().is(ModTags.IMMOVABLE)) {
            body.xxa = body.yya = body.zza = 0.0F;
            return Vec3.ZERO;
        }
        boolean swimming = profile.canSwim() && body.isInWater();
        boolean flying = !swimming && (profile.forcesFlight()
                || profile.canFly() && (!body.onGround() || input.y > 0.0D));
        updateFlightState(body, profile, flying);
        if (flying && body instanceof Bat bat) {
            bat.setResting(false);
        }
        if (!flying && !(swimming && body instanceof Squid)) {
            return input;
        }

        // Squid і FlyingMob мають власний travel; передаємо швидкість, а не викликаємо move.
        double modifier = body.isInWater() ? profile.waterSpeedModifier()
                : body.onGround() ? profile.landedSpeedModifier() : 0.0D;
        double speed = Math.max(0.02D, body.getAttributeValue(Attributes.MOVEMENT_SPEED))
                * Math.max(0.05D, 1.0D + modifier) * 0.4D;
        Vec3 requested = input.lengthSqr() > 1.0D ? input.normalize() : input;
        double yaw = Math.toRadians(body.getYRot());
        Vec3 world = new Vec3(requested.x * Math.cos(yaw) - requested.z * Math.sin(yaw),
                requested.y, requested.z * Math.cos(yaw) + requested.x * Math.sin(yaw));
        Vec3 current = body.getDeltaMovement();
        double inertia = Math.max(0.0D, Math.min(1.0D, profile.inertia()));
        body.setDeltaMovement(
                Math.abs(input.x) + Math.abs(input.z) > 1.0E-5D ? world.x * speed : current.x * inertia,
                input.y != 0.0D ? world.y * speed : current.y * inertia,
                Math.abs(input.x) + Math.abs(input.z) > 1.0E-5D ? world.z * speed : current.z * inertia);
        if (body instanceof Squid squid) {
            // Після travel кальмар оновлює інерцію та анімацію зі свого вектора руху.
            Vec3 velocity = body.getDeltaMovement();
            squid.setMovementVector((float) velocity.x, (float) velocity.y, (float) velocity.z);
        }
        body.setNoGravity(flying);
        body.fallDistance = 0.0F;
        return Vec3.ZERO;
    }
}
