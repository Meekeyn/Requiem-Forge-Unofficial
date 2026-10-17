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
        for (EntityType<? extends Mob> type : List.<EntityType<? extends Mob>>of(EntityType.ZOMBIE, EntityType.CREEPER,
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
        body.aiStep();
        helper.assertTrue(body.position().distanceToSqr(before) < 1.0E-6D,
                "Shulker must retain anchored movement");
        PossessionManager.release(player, body);
        body.discard();
        helper.succeed();
    }

    @GameTest(template = "empty", batch = "possession_movement")
    public static void possessedTravelRunsOnceAndNeutralInputKeepsFriction(GameTestHelper helper) {
        CountingZombie body = new CountingZombie(helper);
        place(helper, body, false);
        body.travels = 0;
        ServerPlayer player = possess(helper, body);
        input(player, 1.0F, false);
        body.aiStep();
        helper.assertTrue(body.travels == 1, "One aiStep must invoke native travel exactly once");
        double before = body.getDeltaMovement().horizontalDistance();
        input(player, 0.0F, false);
        body.aiStep();
        double after = body.getDeltaMovement().horizontalDistance();
        helper.assertTrue(body.travels == 2, "Neutral input must still run one native travel");
        helper.assertTrue(after > 0.0D && after < before,
                "Released movement must decay through vanilla friction rather than be erased");
        PossessionManager.release(player, body);
        helper.assertTrue(body.isNoAi(), "Release must restore the soulless body's original NoAI state");
        body.discard();
        helper.succeed();
    }

    @GameTest(template = "empty", batch = "possession_movement", timeoutTicks = 40)
    public static void possessionInputExpiresWithoutFurtherPackets(GameTestHelper helper) {
        Mob body = EntityType.ZOMBIE.create(helper.getLevel());
        place(helper, body, false);
        ServerPlayer player = possess(helper, body);
        input(player, 1.0F, false);
        helper.runAtTickTime(12, () -> {
            PossessionManager.preparePossessedHostForAiStep(body);
            helper.assertTrue(body.zza == 0.0F && body.xxa == 0.0F,
                    "A missing client must not leave movement keys held indefinitely");
            PossessionManager.release(player, body);
            body.discard();
            helper.succeed();
        });
    }

    @GameTest(template = "empty", batch = "possession_movement")
    public static void possessedZombieJumpsFallsAndCollides(GameTestHelper helper) {
        Mob body = EntityType.ZOMBIE.create(helper.getLevel());
        place(helper, body, false);
        ServerPlayer player = possess(helper, body);
        double floor = body.getY();
        input(player, 0.0F, true);
        body.aiStep();
        helper.assertTrue(body.getY() > floor + 0.2D, "Ground jump must lift the body");
        input(player, 0.0F, false);
        for (int tick = 0; tick < 35; tick++) {
            body.aiStep();
        }
        helper.assertTrue(body.onGround() && Math.abs(body.getY() - floor) < 0.01D,
                "Vanilla gravity and floor collision must land the body");
        for (int y = 2; y < 5; y++) {
            helper.setBlock(new BlockPos(2, y, 3), Blocks.STONE);
        }
        input(player, 1.0F, false);
        for (int tick = 0; tick < 20; tick++) {
            body.aiStep();
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
            body.aiStep();
            helper.assertTrue((boolean) getter.invoke(body), "Crow must enable its native flying state");
            input(player, 0.0F, false);
            body.setOnGround(false);
            body.aiStep();
            helper.assertTrue((boolean) getter.invoke(body), "Neutral airborne input must retain flight animation state");
            body.setOnGround(true);
            body.setDeltaMovement(Vec3.ZERO);
            body.aiStep();
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
        Mob body = EntityType.ZOMBIE.create(helper.getLevel());
        place(helper, body, false);
        ServerPlayer player = possess(helper, body);
        input(player, 1.0F, true);
        body.aiStep();
        helper.assertTrue(PossessionManager.suspendForDisconnect(player, body), "Disconnect must save the host");
        var restored = PossessionManager.restoreDisconnectedHost(player);
        helper.assertTrue(restored != null && PossessionManager.resumeAfterDisconnect(player, restored), "Reconnect must restore possession");
        PossessionManager.preparePossessedHostForAiStep(restored);
        helper.assertTrue(restored.zza == 0.0F && restored.yya == 0.0F && restored.getDeltaMovement().lengthSqr() == 0.0D,
                "Reconnect must not replay axes, jump or saved velocity");
        PossessionManager.release(player, restored);
        helper.assertTrue(PossessionManager.tryPossess(player, restored), "A released body must remain possessable");
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
            body.tick();
            helper.assertTrue(body.getZ() > before.z && body.getY() > before.y, "Full tick must apply forward/jump once: " + type);
            input(player, 0.0F, false);
            for (int i = 0; i < 50; i++) {
                body.tick();
            }
            helper.assertTrue(body.zza == 0.0F && body.yya == 0.0F, "Full tick must retain neutral axes: " + type);
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
        CountingZombie body = new CountingZombie(helper);
        place(helper, body, false);
        ServerPlayer player = possess(helper, body);
        body.jumps = 0;
        body.setJumping(true);
        input(player, 0.0F, true);
        body.tick();
        helper.assertTrue(body.jumps == 1, "Stale AI jumping must not add a second vanilla jump");
        PossessionManager.release(player, body);
        body.discard();
        helper.succeed();
    }

    @GameTest(template = "empty", batch = "possession_movement")
    public static void dimensionEventClearsHeldInput(GameTestHelper helper) {
        Mob body = EntityType.ZOMBIE.create(helper.getLevel());
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

    @GameTest(template = "empty", batch = "possession_movement")
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
        body.aiStep();
        helper.assertTrue(body.getZ() > before.z, "Native travel must move forward: " + type);
        if (vertical) {
            helper.assertTrue(body.getY() > before.y, "Native travel must ascend: " + type
                    + " before=" + before + " after=" + body.position() + " velocity=" + body.getDeltaMovement());
        }
        helper.assertTrue(!body.isNoAi(), "Possession must preserve physics without NoAI: " + type);
        input(player, 0.0F, false);
        body.aiStep();
        helper.assertTrue(body.zza == 0.0F && body.yya == 0.0F, "Neutral input must clear axes: " + type);
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
        ServerPlayer player = new ServerPlayer(helper.getLevel().getServer(), helper.getLevel(),
                new GameProfile(UUID.randomUUID(), "movement-test"));
        SoulManager.setState(player, SoulState.VAGRANT);
        EntitySoulManager.removeSoul(body);
        helper.assertTrue(PossessionManager.tryPossess(player, body), "Possession must succeed: " + body.getType());
        return player;
    }

    private static void input(ServerPlayer player, float forward, boolean jump) {
        PossessionManager.updatePossessionInput(player,
                new PossessionInputPacket(forward, 0.0F, jump, false, 0.0F, 0.0F));
    }

    private static final class CountingZombie extends Zombie {
        private int travels;
        private int jumps;

        @Override
        protected void jumpFromGround() {
            jumps++;
            super.jumpFromGround();
        }

        private CountingZombie(GameTestHelper helper) {
            super(EntityType.ZOMBIE, helper.getLevel());
        }

        @Override
        public void travel(Vec3 input) {
            travels++;
            super.travel(input);
        }
    }
}
