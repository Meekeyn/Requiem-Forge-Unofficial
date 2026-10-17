package ua.meekeyn.requiemunofficial.common.possession;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.Rabbit;
import net.minecraft.world.entity.animal.Squid;
import net.minecraft.world.entity.ambient.Bat;
import net.minecraft.world.phys.Vec3;
import ua.meekeyn.requiemunofficial.common.util.ModTags;
import ua.meekeyn.requiemunofficial.mixin.LivingEntityAccessor;

/** Видові параметри застосовуються до фізичного гравця перед єдиним vanilla travel. */
public final class PlayerBodyMovement {
    private PlayerBodyMovement() { }

    public static LivingEntity host(Player player) {
        LivingEntity host = PossessionManager.getBeta16PlayerBodyHost(player);
        return PossessionManager.usesExtendedPlayerBody(host) ? host : null;
    }

    public static void prepareStep(Player player) {
        LivingEntity host = host(player);
        var state = player.getPersistentData();
        String active = "RequiemExtendedClientBody";
        if (player.level().isClientSide()) {
            if (host == null && state.contains(active)) {
                player.noPhysics = state.getBoolean(active + "NoPhysics");
                player.setNoGravity(state.getBoolean(active + "NoGravity"));
                state.remove(active);
                state.remove(active + "NoPhysics");
                state.remove(active + "NoGravity");
                player.refreshDimensions();
            } else if (host != null && !state.contains(active)) {
                state.putBoolean(active, true);
                state.putBoolean(active + "NoPhysics", player.noPhysics);
                state.putBoolean(active + "NoGravity", player.isNoGravity());
                player.refreshDimensions();
            }
        }
        if (host == null) return;
        var dimensions = host.getDimensions(player.getPose());
        if (player.getBbWidth() != dimensions.width || player.getBbHeight() != dimensions.height) {
            player.refreshDimensions();
        }
        var profile = EntityMobilityManager.get(host);
        boolean jump = ((LivingEntityAccessor) player).requiemunofficial$isJumping();
        boolean shift = player.isShiftKeyDown();
        if (player instanceof net.minecraft.server.level.ServerPlayer serverPlayer && (profile.canFly() || profile.canSwim())) {
            float vertical = PossessionManager.playerBodyVerticalInput(serverPlayer);
            jump = vertical > 0;
            shift = vertical < 0;
        }
        boolean swim = (player.isInWater() || player.isInLava()) && profile.canSwim();
        boolean fly = !player.isInWater() && !player.isInLava() && (profile.forcesFlight()
                || profile.canFly() && (!player.onGround() || jump));
        boolean anchored = host.getType().is(ModTags.IMMOVABLE);
        player.noPhysics = profile.phasesThroughWalls();
        player.setNoGravity(fly || anchored || swim);
        player.yya = (fly || swim) && jump != shift
                ? (jump ? 1.0F : -1.0F) : 0.0F;
        if (fly || swim || anchored) {
            player.setJumping(false);
        } else if (profile.walkMode() == EntityMobilityManager.WalkMode.JUMPY
                && player.onGround() && Math.abs(player.xxa) + Math.abs(player.zza) > 0.0F) {
            player.setJumping(true);
        }
        if (fly && host instanceof Bat bat) bat.setResting(false);
        PossessionMovementController.updateFlightState(host, profile, fly);
    }

    public static Vec3 prepareTravel(Player player, Vec3 input) {
        LivingEntity host = host(player);
        if (host == null) return input;
        var profile = EntityMobilityManager.get(host);
        if (host.getType().is(ModTags.IMMOVABLE)) {
            player.setDeltaMovement(Vec3.ZERO);
            player.setNoGravity(true);
            return Vec3.ZERO;
        }
        boolean fluid = player.isInWater() || player.isInLava();
        boolean swim = fluid && profile.canSwim();
        boolean fly = !fluid && player.isNoGravity() && profile.canFly();
        double modifier = fluid ? profile.waterSpeedModifier()
                : player.onGround() ? profile.landedSpeedModifier() : 0.0D;
        double multiplier = Math.max(0.05D, 1.0D + modifier);
        if (fly || swim) {
            // Нахил камери ванільного плавця не додає друге вертикальне керування.
            player.setSwimming(false);
            Vec3 requested = new Vec3(input.x, player.yya, input.z);
            if (requested.lengthSqr() > 1.0D) requested = requested.normalize();
            double speed = Math.max(0.02D, player.getAttributeValue(Attributes.MOVEMENT_SPEED)) * multiplier;
            double yaw = Math.toRadians(player.getYRot());
            Vec3 current = player.getDeltaMovement();
            double inertia = Math.max(0.0D, Math.min(1.0D, profile.inertia()));
            boolean horizontal = Math.abs(input.x) + Math.abs(input.z) > 1.0E-5D;
            double y = player.yya != 0 ? requested.y * speed : current.y * inertia;
            if (profile.swimMode() == EntityMobilityManager.MobilityMode.FLOATING && player.yya == 0 && fluid) {
                y = Math.max(y, 0.04D);
            }
            player.setDeltaMovement(horizontal ? (requested.x * Math.cos(yaw) - requested.z * Math.sin(yaw)) * speed : current.x * inertia,
                    y, horizontal ? (requested.z * Math.cos(yaw) + requested.x * Math.sin(yaw)) * speed : current.z * inertia);
            player.fallDistance = 0;
            return Vec3.ZERO;
        }
        Vec3 current = player.getDeltaMovement();
        if (fluid && profile.swimMode() == EntityMobilityManager.MobilityMode.SINKING) {
            player.setDeltaMovement(current.x, Math.min(current.y, -Math.max(0.02D, profile.gravity())), current.z);
        } else if (!player.onGround() && current.y < 0 && profile.fallSpeedModifier() > 0) {
            player.setDeltaMovement(current.x, current.y * Math.max(0, 1 - profile.fallSpeedModifier()), current.z);
            player.fallDistance = 0;
        } else if (profile.flopsOnLand() && !fluid && player.onGround()) {
            player.setDeltaMovement(current.x, Math.max(current.y, input.horizontalDistanceSqr() > 0 ? 0.18D : 0.08D), current.z);
        }
        return input.multiply(multiplier, 1, multiplier);
    }

    public static float jumpPower(Player player, float original) {
        LivingEntity host = host(player);
        return host == null ? original : original * (float) (PossessionManager.jumpPower(host, EntityMobilityManager.get(host)) / 0.42D);
    }

    public static void afterJump(Player player) {
        LivingEntity host = host(player);
        if (host instanceof Rabbit rabbit) rabbit.startJumping();
    }

    public static boolean climbs(Player player) {
        LivingEntity host = host(player);
        return host != null && EntityMobilityManager.get(host).climbsWalls() && player.horizontalCollision;
    }

    public static void syncAnimation(Player player, LivingEntity host) {
        if (!PossessionManager.usesExtendedPlayerBody(host)) return;
        if (host instanceof Squid squid) {
            Vec3 velocity = player.getDeltaMovement();
            squid.setMovementVector((float) velocity.x, (float) velocity.y, (float) velocity.z);
        }
    }
}
