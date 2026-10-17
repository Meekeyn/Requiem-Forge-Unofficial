package ua.meekeyn.requiemunofficial.gametest;

import com.mojang.authlib.GameProfile;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
import ua.meekeyn.requiemunofficial.RequiemUnofficial;
import ua.meekeyn.requiemunofficial.common.network.PossessionInputPacket;
import ua.meekeyn.requiemunofficial.common.possession.PossessionManager;
import ua.meekeyn.requiemunofficial.common.soul.EntitySoulManager;
import ua.meekeyn.requiemunofficial.common.soul.SoulManager;
import ua.meekeyn.requiemunofficial.common.soul.SoulState;

import java.util.UUID;
import java.util.List;

@GameTestHolder(RequiemUnofficial.MODID)
@PrefixGameTestTemplate(false)
public final class PossessionMovementGameTests {
    private PossessionMovementGameTests() {
    }

    @GameTest(template = "empty", batch = "possession_movement")
    public static void possessedGroundBodiesUseNativeTravel(GameTestHelper helper) {
        for (EntityType<? extends Mob> type : List.<EntityType<? extends Mob>>of(EntityType.HUSK, EntityType.CREEPER,
                EntityType.VILLAGER, EntityType.COW, EntityType.IRON_GOLEM, EntityType.PILLAGER,
                EntityType.SPIDER, EntityType.SLIME, EntityType.MAGMA_CUBE, EntityType.RABBIT,
                EntityType.FROG, EntityType.HORSE, EntityType.ENDERMAN, EntityType.STRIDER)) {
            checkMovement(helper, type, false, false);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", batch = "possession_movement")
    public static void possessedFlyingBodiesAscendThroughNativeTravel(GameTestHelper helper) {
        for (EntityType<? extends Mob> type : List.<EntityType<? extends Mob>>of(EntityType.GHAST, EntityType.PHANTOM,
                EntityType.BAT, EntityType.BEE, EntityType.ALLAY, EntityType.PARROT, EntityType.VEX)) {
            checkMovement(helper, type, true, false);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", batch = "possession_movement")
    public static void possessedAquaticBodiesSwimThroughNativeTravel(GameTestHelper helper) {
        for (EntityType<? extends Mob> type : List.<EntityType<? extends Mob>>of(EntityType.SQUID, EntityType.GLOW_SQUID,
                EntityType.GUARDIAN, EntityType.DOLPHIN, EntityType.COD, EntityType.AXOLOTL)) {
            checkMovement(helper, type, true, true);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", batch = "possession_movement")
    public static void possessedAlexBodiesUseCommonInput(GameTestHelper helper) {
        for (String id : List.of("grizzly_bear", "crow", "catfish")) {
            ResourceLocation key = ResourceLocation.tryParse("alexsmobs:" + id);
            if (!BuiltInRegistries.ENTITY_TYPE.containsKey(key)) {
                continue;
            }
            EntityType<?> type = BuiltInRegistries.ENTITY_TYPE.get(key);
            Mob body = (Mob) type.create(helper.getLevel());
            checkMovement(helper, body, id.equals("crow") || id.equals("catfish"), id.equals("catfish"));
        }
        helper.succeed();
    }

    @GameTest(template = "empty", batch = "possession_movement")
    public static void possessedShulkerDoesNotWalk(GameTestHelper helper) {
        Mob body = EntityType.SHULKER.create(helper.getLevel());
        place(helper, body, false);
        ServerPlayer player = possess(helper, body);
        input(player, 1.0F, true);
        Vec3 before = body.position();
        step(player, body);
        helper.assertTrue(body.position().distanceToSqr(before) < 1.0E-6D,
                "Shulker must retain anchored movement");
        PossessionManager.release(player, body);
        body.discard();
        helper.succeed();
    }

    @GameTest(template = "empty", batch = "possession_movement")
    public static void possessedTravelRunsOnceAndNeutralInputKeepsFriction(GameTestHelper helper) {
        CountingGroundMob body = new CountingGroundMob(helper);
        place(helper, body, false);
        body.travels = 0;
        ServerPlayer player = possess(helper, body);
        input(player, 1.0F, false);
        step(player, body);
        helper.assertTrue(body.travels == 0 && ((CountingPlayer) player).travels == 1, "One aiStep must invoke native travel exactly once");
        double before = body.getDeltaMovement().horizontalDistance();
        input(player, 0.0F, false);
        step(player, body);
        double after = body.getDeltaMovement().horizontalDistance();
        helper.assertTrue(body.travels == 0 && ((CountingPlayer) player).travels == 2, "Neutral input must still run one native travel");
        helper.assertTrue(after > 0.0D && after < before,
                "Released movement must decay through vanilla friction rather than be erased");
        PossessionManager.release(player, body);
        helper.assertTrue(body.isNoAi(), "Release must restore the soulless body's original NoAI state");
        body.discard();
        helper.succeed();
    }

    @GameTest(template = "empty", batch = "possession_movement", timeoutTicks = 40)
    public static void possessionInputExpiresWithoutFurtherPackets(GameTestHelper helper) {
        Mob body = EntityType.PHANTOM.create(helper.getLevel());
        place(helper, body, false);
        ServerPlayer player = possess(helper, body);
        input(player, 0.0F, true);
        helper.runAtTickTime(12, () -> {
            player.setJumping(false);
            step(player, body);
            helper.assertTrue(player.yya == 0.0F,
                    "A missing client must not leave movement keys held indefinitely");
            PossessionManager.release(player, body);
            body.discard();
            helper.succeed();
        });
    }

    @GameTest(template = "empty", batch = "possession_movement")
    public static void possessedHuskJumpsFallsAndCollides(GameTestHelper helper) {
        Mob body = EntityType.HUSK.create(helper.getLevel());
        place(helper, body, false);
        ServerPlayer player = possess(helper, body);
        double floor = body.getY();
        input(player, 0.0F, true);
        step(player, body);
        helper.assertTrue(body.getY() > floor + 0.2D, "Ground jump must lift the body");
        input(player, 0.0F, false);
        for (int tick = 0; tick < 35; tick++) {
            step(player, body);
        }
        helper.assertTrue(body.onGround() && Math.abs(body.getY() - floor) < 0.01D,
                "Vanilla gravity and floor collision must land the body");
        for (int y = 2; y < 5; y++) {
            helper.setBlock(new BlockPos(2, y, 3), Blocks.STONE);
        }
        input(player, 1.0F, false);
        for (int tick = 0; tick < 20; tick++) {
            step(player, body);
        }
        helper.assertTrue(body.getBoundingBox().maxZ <= helper.absolutePos(new BlockPos(2, 2, 3)).getZ() + 0.001D,
                "Controlled movement must respect a solid wall");
        helper.assertTrue(body.horizontalCollision, "The body must report vanilla horizontal collision");
        PossessionManager.release(player, body);
        body.discard();
        helper.succeed();
    }

    @GameTest(template = "empty", batch = "possession_movement")
    public static void playerBodyProxiesDoNotRunSecondPhysics(GameTestHelper helper) {
        for (EntityType<? extends Mob> type : List.<EntityType<? extends Mob>>of(EntityType.SKELETON, EntityType.BLAZE)) {
            Mob body = type.create(helper.getLevel());
            place(helper, body, false);
            ServerPlayer player = possess(helper, body);
            player.setDeltaMovement(0.12D, 0.2D, 0.15D);
            PossessionManager.syncBeta16HostToPlayer(player, body);
            Vec3 before = body.position();
            body.aiStep();
            helper.assertTrue(body.position().equals(before), "The visual proxy must not integrate player velocity: " + type);
            PossessionManager.release(player, body);
            body.discard();
        }
        helper.succeed();
    }

    @GameTest(template = "empty", batch = "possession_movement")
    public static void alexFlightRestoresBothOriginalStates(GameTestHelper helper) throws Exception {
        ResourceLocation id = new ResourceLocation("alexsmobs", "crow");
        if (!BuiltInRegistries.ENTITY_TYPE.containsKey(id)) {
            helper.succeed();
            return;
        }
        for (boolean original : List.of(false, true)) {
            Mob body = (Mob) BuiltInRegistries.ENTITY_TYPE.get(id).create(helper.getLevel());
            place(helper, body, false);
            var setter = body.getClass().getMethod("setFlying", boolean.class);
            var getter = body.getClass().getMethod("isFlying");
            setter.invoke(body, original);
            ServerPlayer player = possess(helper, body);
            input(player, 1.0F, true);
            step(player, body);
            helper.assertTrue((boolean) getter.invoke(body), "Crow must enable its native flying state");
            input(player, 0.0F, false);
            player.setOnGround(false);
            step(player, body);
            helper.assertTrue((boolean) getter.invoke(body), "Neutral airborne input must retain flight animation state");
            player.setOnGround(true);
            player.setDeltaMovement(Vec3.ZERO);
            step(player, body);
            helper.assertTrue(!(boolean) getter.invoke(body), "Grounded neutral crow must leave flying state");
            PossessionManager.release(player, body);
            helper.assertTrue((boolean) getter.invoke(body) == original, "Release must restore the original flight state");
            helper.assertTrue(!body.getPersistentData().contains("RequiemOriginalFlying"), "Flight snapshot must be consumed");
            body.discard();
        }
        helper.succeed();
    }

    @GameTest(template = "empty", batch = "possession_movement")
    public static void reconnectDoesNotReplayMovement(GameTestHelper helper) {
        Mob body = EntityType.HUSK.create(helper.getLevel());
        place(helper, body, false);
        ServerPlayer player = possess(helper, body);
        input(player, 1.0F, true);
        step(player, body);
        helper.assertTrue(PossessionManager.suspendForDisconnect(player, body), "Disconnect must save the host");
        var restored = PossessionManager.restoreDisconnectedHost(player);
        helper.assertTrue(restored != null && PossessionManager.resumeAfterDisconnect(player, restored), "Reconnect must restore possession");
        player.zza = 0;
        player.setJumping(false);
        PossessionManager.preparePossessedHostForAiStep(restored);
        helper.assertTrue(restored.zza == 0.0F && restored.yya == 0.0F && restored.getDeltaMovement().lengthSqr() == 0.0D,
                "Reconnect must not replay axes, jump or saved velocity");
        PossessionManager.release(player, restored);
        helper.assertTrue(PossessionManager.tryPossess(player, restored), "A released body must remain possessable");
        player.zza = 0;
        player.setJumping(false);
        PossessionManager.preparePossessedHostForAiStep(restored);
        helper.assertTrue(restored.zza == 0.0F, "Re-possession must start with neutral input");
        PossessionManager.release(player, restored);
        restored.discard();
        helper.succeed();
    }

    @GameTest(template = "empty", batch = "possession_movement")
    public static void fullEntityTicksPreserveSpeciesPhysics(GameTestHelper helper) {
        for (EntityType<? extends Mob> type : List.<EntityType<? extends Mob>>of(EntityType.PHANTOM,
                EntityType.VEX, EntityType.RABBIT, EntityType.FROG, EntityType.SLIME, EntityType.DOLPHIN)) {
            Mob body = type.create(helper.getLevel());
            boolean water = type == EntityType.DOLPHIN;
            place(helper, body, water);
            ServerPlayer player = possess(helper, body);
            Vec3 before = body.position();
            input(player, 1.0F, true);
            step(player, body);
            body.tick();
            helper.assertTrue(body.getZ() > before.z && body.getY() > before.y, "Full tick must apply forward/jump once: " + type);
            input(player, 0.0F, false);
            for (int i = 0; i < 50; i++) {
                step(player, body);
                body.tick();
            }
            helper.assertTrue(player.zza == 0.0F && player.yya == 0.0F, "Full tick must retain neutral axes: " + type);
            if (type == EntityType.RABBIT || type == EntityType.FROG || type == EntityType.SLIME) {
                helper.assertTrue(body.onGround(), "Ground species must fall and land after released jump: " + type);
            }
            PossessionManager.release(player, body);
            body.discard();
        }
        helper.succeed();
    }

    @GameTest(template = "empty", batch = "possession_movement")
    public static void staleAiJumpDoesNotDuplicateControlledJump(GameTestHelper helper) {
        CountingGroundMob body = new CountingGroundMob(helper);
        place(helper, body, false);
        ServerPlayer player = possess(helper, body);
        body.jumps = 0;
        body.setJumping(true);
        input(player, 0.0F, true);
        step(player, body);
        helper.assertTrue(body.jumps == 0 && ((CountingPlayer) player).jumps == 1, "Stale AI jumping must not add a second vanilla jump");
        PossessionManager.release(player, body);
        body.discard();
        helper.succeed();
    }

    @GameTest(template = "empty", batch = "possession_movement")
    public static void dimensionEventClearsHeldInput(GameTestHelper helper) {
        Mob body = EntityType.HUSK.create(helper.getLevel());
        place(helper, body, false);
        ServerPlayer player = possess(helper, body);
        input(player, 1.0F, true);
        PossessionManager.onPossessorChangedDimension(new net.minecraftforge.event.entity.player.PlayerEvent.PlayerChangedDimensionEvent(
                player, net.minecraft.world.level.Level.NETHER, player.level().dimension()));
        PossessionManager.preparePossessedHostForAiStep(body);
        helper.assertTrue(body.zza == 0.0F && body.getDeltaMovement().y <= 0.0D,
                "Dimension event must clear held movement and jump");
        var destination = helper.getLevel().getServer().getLevel(net.minecraft.world.level.Level.NETHER);
        helper.assertTrue(destination != null, "Test server must provide the destination dimension");
        player.setServerLevel(destination);
        PossessionManager.onPossessorChangedDimension(new net.minecraftforge.event.entity.player.PlayerEvent.PlayerChangedDimensionEvent(
                player, helper.getLevel().dimension(), destination.dimension()));
        helper.assertTrue(SoulManager.getPossessedEntityId(player) == null && PossessionManager.getPossessorId(body) == null,
                "Cross-dimension transition must release the old body");
        helper.assertTrue(player.level() == destination, "Release must not teleport the player back to the old dimension");
        body.discard();
        helper.succeed();
    }

    @GameTest(template = "possession_arena", batch = "body_attack")
    public static void differentBodiesAttackAndRespectCooldown(GameTestHelper helper) {
        for (EntityType<? extends Mob> type : List.<EntityType<? extends Mob>>of(EntityType.PHANTOM,
                EntityType.VEX, EntityType.RABBIT, EntityType.FROG, EntityType.SLIME, EntityType.DOLPHIN)) {
            Mob body = type.create(helper.getLevel());
            place(helper, body, false);
            ServerPlayer player = possess(helper, body);
            Mob target = EntityType.COW.create(helper.getLevel());
            target.setNoAi(true);
            target.moveTo(body.getX(), body.getY(), body.getZ() + 1.5D, 0.0F, 0.0F);
            helper.getLevel().addFreshEntity(target);
            Vec3 aim = target.getBoundingBox().getCenter().subtract(body.getEyePosition());
            player.setYRot((float) Math.toDegrees(Math.atan2(-aim.x, aim.z)));
            player.setXRot((float) -Math.toDegrees(Math.atan2(aim.y, aim.horizontalDistance())));
            float health = target.getHealth();
            PossessionManager.handleAction(player, ua.meekeyn.requiemunofficial.common.network.PossessionActionPacket.Action.ATTACK);
            helper.assertTrue(target.getHealth() < health, "Possessed body must attack a visible target: " + type);
            float after = target.getHealth();
            target.invulnerableTime = 0;
            PossessionManager.handleAction(player, ua.meekeyn.requiemunofficial.common.network.PossessionActionPacket.Action.ATTACK);
            helper.assertTrue(target.getHealth() == after, "Cooldown must prevent a second hit even without hurt immunity: " + type);
            PossessionManager.release(player, body);
            helper.assertTrue(!body.getPersistentData().contains("RequiemNextHostAttack"), "Release must clear attack cooldown");
            body.discard();
            target.discard();
        }
        helper.succeed();
    }

    @GameTest(template = "possession_arena", batch = "body_climb")
    public static void extendedBodiesRetainClimbingAndWallPhysics(GameTestHelper helper) {
        for (EntityType<? extends Mob> type : List.<EntityType<? extends Mob>>of(EntityType.SPIDER, EntityType.VEX)) {
            Mob body = type.create(helper.getLevel());
            place(helper, body, false);
            ServerPlayer player = possess(helper, body);
            double startY = player.getY();
            for (int x = 1; x <= 3; x++) {
                for (int y = 2; y < 12; y++) helper.setBlock(new BlockPos(x, y, 4), Blocks.STONE);
            }
            input(player, 1, false);
            for (int i = 0; i < 30; i++) step(player, body);
            double wall = helper.absolutePos(new BlockPos(2, 2, 4)).getZ();
            if (type == EntityType.SPIDER) {
                helper.assertTrue(player.getY() > startY + 0.2D && player.getBoundingBox().maxZ <= wall + 0.001D,
                        "Spider must climb the wall without crossing it: position=" + player.position() + " startY=" + startY + " wall=" + wall);
            } else {
                helper.assertTrue(player.getZ() > wall + 1 && player.noPhysics, "Vex must cross the wall using Player Body");
            }
            PossessionManager.release(player, body);
            helper.assertTrue(!player.noPhysics, "Release must restore player collision");
            helper.assertTrue(player.getBbWidth() == EntityType.PLAYER.getWidth(), "Release must restore player dimensions");
            body.discard();
        }
        helper.succeed();
    }

    @GameTest(template = "possession_arena", batch = "enderman_teleport")
    public static void endermanAbilityTeleportsPlayerBody(GameTestHelper helper) {
        Mob body = EntityType.ENDERMAN.create(helper.getLevel());
        place(helper, body, false);
        ServerPlayer player = possess(helper, body);
        for (int x = 1; x <= 3; x++) {
            for (int y = 2; y <= 8; y++) {
                for (int z = 3; z <= 8; z++) helper.setBlock(new BlockPos(x, y, z), Blocks.AIR);
            }
        }
        helper.setBlock(new BlockPos(2, 4, 5), Blocks.STONE);
        player.setYRot(0);
        player.setXRot(0);
        PossessionManager.syncBeta16HostToPlayer(player, body);
        var hit = helper.getLevel().clip(new net.minecraft.world.level.ClipContext(body.getEyePosition(),
                body.getEyePosition().add(body.getLookAngle().scale(32)),
                net.minecraft.world.level.ClipContext.Block.COLLIDER,
                net.minecraft.world.level.ClipContext.Fluid.NONE, body));
        helper.assertTrue(hit.getType() == net.minecraft.world.phys.HitResult.Type.BLOCK
                        && hit.getBlockPos().equals(helper.absolutePos(new BlockPos(2, 4, 5))),
                "Teleport ray must hit the isolated target: " + hit.getBlockPos() + " look=" + body.getLookAngle());
        Vec3 before = player.position();
        helper.setBlock(new BlockPos(2, 6, 5), Blocks.STONE);
        PossessionManager.handleAction(player, ua.meekeyn.requiemunofficial.common.network.PossessionActionPacket.Action.INTERACT);
        helper.assertTrue(player.position().equals(before) && body.position().equals(before),
                "An obstructed destination must not move either body");
        helper.setBlock(new BlockPos(2, 6, 5), Blocks.AIR);
        PossessionManager.handleAction(player, ua.meekeyn.requiemunofficial.common.network.PossessionActionPacket.Action.INTERACT);
        helper.assertTrue(player.position().distanceToSqr(before) > 1.0D, "Enderman ability must move the physical player");
        Vec3 destination = player.position();
        helper.assertTrue(destination.equals(Vec3.atBottomCenterOf(helper.absolutePos(new BlockPos(2, 5, 5)))),
                "Enderman must arrive above the raycast target: " + destination);
        PossessionManager.syncBeta16HostToPlayer(player, body);
        helper.assertTrue(body.position().equals(destination), "Sync must preserve the ability's destination");
        PossessionManager.release(player, body);
        body.discard();
        helper.succeed();
    }

    @GameTest(template = "empty", batch = "possession_movement", timeoutTicks = 40)
    public static void expiredDescentDoesNotReuseVanillaShift(GameTestHelper helper) {
        Mob body = EntityType.PHANTOM.create(helper.getLevel());
        place(helper, body, false);
        ServerPlayer player = possess(helper, body);
        player.setShiftKeyDown(true);
        PossessionManager.updatePossessionInput(player, new PossessionInputPacket(0, 0, false, true, 0, 0));
        step(player, body);
        helper.assertTrue(player.yya < 0, "Fresh shift must descend");
        helper.runAtTickTime(12, () -> {
            step(player, body);
            helper.assertTrue(player.yya == 0, "Expired input must stop descent even if vanilla shift remains held");
            PossessionManager.release(player, body);
            body.discard();
            helper.succeed();
        });
    }

    @GameTest(template = "empty", batch = "possession_movement")
    public static void mobilityProfilesSurviveNetworkRoundTrip(GameTestHelper helper) {
        var profiles = new java.util.HashMap<ResourceLocation, ua.meekeyn.requiemunofficial.common.possession.EntityMobilityManager.MobilityProfile>();
        for (var type : List.of(EntityType.VEX, EntityType.FROG, EntityType.DOLPHIN, EntityType.SPIDER)) {
            profiles.put(BuiltInRegistries.ENTITY_TYPE.getKey(type), ua.meekeyn.requiemunofficial.common.possession.EntityMobilityManager.get(type));
        }
        var packet = new ua.meekeyn.requiemunofficial.common.network.MobilityProfilesPacket(new com.google.gson.Gson().toJson(profiles));
        var buffer = new net.minecraft.network.FriendlyByteBuf(io.netty.buffer.Unpooled.buffer());
        try {
            ua.meekeyn.requiemunofficial.common.network.MobilityProfilesPacket.encode(packet, buffer);
            var decoded = ua.meekeyn.requiemunofficial.common.network.MobilityProfilesPacket.decode(buffer);
            var parsed = ua.meekeyn.requiemunofficial.common.possession.EntityMobilityManager.parse(
                    com.google.gson.JsonParser.parseString(decoded.json()).getAsJsonObject());
            helper.assertTrue(profiles.equals(parsed), "Network profiles must preserve every species physics parameter");
        } finally {
            buffer.release();
        }
        helper.succeed();
    }

    @GameTest(template = "empty", batch = "possession_movement")
    public static void speciesGravityIsAppliedOnce(GameTestHelper helper) {
        Mob body = EntityType.CAT.create(helper.getLevel());
        place(helper, body, false);
        ServerPlayer player = possess(helper, body);
        player.setPos(player.getX(), player.getY() + 3, player.getZ());
        player.setOnGround(false);
        player.setDeltaMovement(Vec3.ZERO);
        input(player, 0, false);
        step(player, body);
        helper.assertTrue(Math.abs(player.getDeltaMovement().y + 0.06D * 0.98D) < 1.0E-6D,
                "Cat must apply profile gravity exactly once through vanilla travel");
        player.setSprinting(true);
        PossessionManager.controlHostForTest(player, body);
        PossessionManager.controlHostForTest(player, body);
        double catBaseSpeed = body.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.MOVEMENT_SPEED).getBaseValue();
        helper.assertTrue(Math.abs(player.getAttributeValue(net.minecraft.world.entity.ai.attributes.Attributes.MOVEMENT_SPEED)
                - catBaseSpeed * 1.33D * 1.3D) < 1.0E-6D,
                "Cat sprint must use its native controller speed and a single sprint attribute modifier");
        PossessionManager.release(player, body);
        body.discard();
        helper.succeed();
    }

    @GameTest(template = "empty", batch = "possession_movement")
    public static void possessedSwimmerBodyKeepsHostWaterVelocity(GameTestHelper helper) {
        Mob body = EntityType.DOLPHIN.create(helper.getLevel());
        place(helper, body, true);
        ServerPlayer player = possess(helper, body);
        double waterScale = ua.meekeyn.requiemunofficial.common.possession.EntityMobilityManager.get(body)
                .waterSpeedModifier();
        double expected = 0.98D * Math.max(0.02D, body.getAttributeValue(
                net.minecraft.world.entity.ai.attributes.Attributes.MOVEMENT_SPEED))
                * Math.max(0.05D, 1.0D + waterScale);
        input(player, 1.0F, false);
        step(player, body);
        helper.assertTrue(Math.abs(player.getDeltaMovement().horizontalDistance() - expected) < 1.0E-6D,
                "Water hosts must carry the host velocity scale exactly once: delta="
                        + player.getDeltaMovement() + " expected=" + expected);
        step(player, body);
        helper.assertTrue(Math.abs(player.getDeltaMovement().horizontalDistance() - expected) < 1.0E-6D,
                "Repeated synchronization must not grow the possessed swim velocity");
        PossessionManager.release(player, body);
        body.discard();
        helper.succeed();
    }

    @GameTest(template = "empty", batch = "possession_movement")
    public static void swimmingUsesOneVerticalInput(GameTestHelper helper) {
        Mob body = EntityType.DOLPHIN.create(helper.getLevel());
        place(helper, body, true);
        ServerPlayer player = possess(helper, body);
        input(player, 0, false);
        player.setPos(player.getX(), player.getY() + 2, player.getZ());
        player.setSwimming(true);
        player.setXRot(60);
        step(player, body);
        helper.assertTrue(Math.abs(player.getDeltaMovement().y) < 1.0E-6D,
                "Camera pitch must not add a second aquatic vertical input");
        PossessionManager.updatePossessionInput(player, new PossessionInputPacket(0, 0, false, true, 0, 60));
        step(player, body);
        double descent = player.getDeltaMovement().y;
        helper.assertTrue(descent < 0, "Shift must descend in water");
        input(player, 0, false);
        step(player, body);
        helper.assertTrue(player.getDeltaMovement().y > descent && player.getDeltaMovement().y <= 0,
                "Released descent must decay through the species inertia");
        PossessionManager.release(player, body);
        body.discard();
        helper.succeed();
    }

    private static void checkMovement(GameTestHelper helper, EntityType<? extends Mob> type,
                                      boolean vertical, boolean water) {
        Mob body = type.create(helper.getLevel());
        checkMovement(helper, body, vertical, water);
    }

    private static void checkMovement(GameTestHelper helper, Mob body, boolean vertical, boolean water) {
        EntityType<?> type = body.getType();
        place(helper, body, water);
        EntitySoulManager.removeSoul(body);
        boolean noGravity = body.isNoGravity();
        boolean noPhysics = body.noPhysics;
        ServerPlayer player = possess(helper, body);
        input(player, 1.0F, vertical);
        Vec3 before = body.position();
        PossessionManager.preparePossessedHostForAiStep(body);
        helper.assertTrue(body.position().equals(before), "Preparing input must not move " + type);
        step(player, body);
        helper.assertTrue(body.getZ() > before.z, "Native travel must move forward: " + type);
        if (vertical) {
            helper.assertTrue(body.getY() > before.y, "Native travel must ascend: " + type
                    + " before=" + before + " after=" + body.position() + " velocity=" + body.getDeltaMovement());
        }
        helper.assertTrue(body.isNoAi(), "Proxy AI must remain disabled: " + type);
        helper.assertTrue(player.noPhysics == (body.getType() == EntityType.VEX), "Only Vex should bypass collision in this matrix: " + type);
        input(player, 0.0F, false);
        step(player, body);
        helper.assertTrue(player.zza == 0.0F && player.yya == 0.0F, "Neutral input must clear axes: " + type);
        PossessionManager.release(player, body);
        helper.assertTrue(body.isNoGravity() == noGravity && body.noPhysics == noPhysics && body.isNoAi(),
                "Release must restore native physics and original soulless AI state: " + type);
        helper.assertTrue(!body.isSprinting() && !body.isShiftKeyDown(), "Release must clear held buttons: " + type);
        body.discard();
    }

    private static void place(GameTestHelper helper, Mob body, boolean water) {
        for (int x = 0; x < 5; x++) {
            for (int z = 0; z < 5; z++) {
                helper.setBlock(new BlockPos(x, 1, z), Blocks.STONE);
                for (int y = 2; y < 12; y++) {
                    helper.setBlock(new BlockPos(x, y, z), water
                            ? (x == 0 || x == 4 || z == 0 || z == 4 ? Blocks.STONE : Blocks.WATER)
                            : Blocks.AIR);
                }
            }
        }
        BlockPos pos = helper.absolutePos(new BlockPos(2, 2, 2));
        body.moveTo(pos.getX() + 0.5D, pos.getY(), pos.getZ() + 0.5D, 0.0F, 0.0F);
        helper.getLevel().addFreshEntity(body);
        body.tick();
        body.setDeltaMovement(Vec3.ZERO);
        body.setOnGround(!water);
    }

    private static ServerPlayer possess(GameTestHelper helper, Mob body) {
        ServerPlayer player = new CountingPlayer(helper);
        SoulManager.setState(player, SoulState.VAGRANT);
        EntitySoulManager.removeSoul(body);
        boolean grounded = body.onGround();
        helper.assertTrue(PossessionManager.tryPossess(player, body), "Possession must succeed: " + body.getType());
        player.setOnGround(grounded);
        return player;
    }

    private static void step(ServerPlayer player, net.minecraft.world.entity.LivingEntity body) {
        ((CountingPlayer) player).updateFluids();
        player.aiStep();
        PossessionManager.syncBeta16HostToPlayer(player, body);
        Vec3 position = body.position();
        body.aiStep();
        if (!body.position().equals(position)) throw new AssertionError("Proxy performed a second physical step: " + body.getType());
    }

    private static void input(ServerPlayer player, float forward, boolean jump) {
        player.zza = forward;
        player.xxa = 0;
        player.setJumping(jump);
        PossessionManager.updatePossessionInput(player,
                new PossessionInputPacket(forward, 0.0F, jump, false, 0.0F, 0.0F));
    }

    private static final class CountingPlayer extends ServerPlayer {
        int travels;
        int jumps;
        CountingPlayer(GameTestHelper helper) {
            super(helper.getLevel().getServer(), helper.getLevel(), new GameProfile(UUID.randomUUID(), "movement-test"));
        }
        @Override public int awardRecipes(java.util.Collection<net.minecraft.world.item.crafting.Recipe<?>> recipes) { return 0; }
        void updateFluids() { updateInWaterStateAndDoFluidPushing(); }
        @Override public void travel(Vec3 input) { travels++; super.travel(input); }
        @Override public void jumpFromGround() { jumps++; super.jumpFromGround(); }
    }

    private static final class CountingGroundMob extends Zombie {
        private int travels;
        private int jumps;

        @Override
        protected void jumpFromGround() {
            jumps++;
            super.jumpFromGround();
        }

        private CountingGroundMob(GameTestHelper helper) {
            super(EntityType.HUSK, helper.getLevel());
        }

        @Override
        public void travel(Vec3 input) {
            travels++;
            super.travel(input);
        }
    }
}
