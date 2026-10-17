package ua.meekeyn.requiemunofficial.gametest;

import com.mojang.authlib.GameProfile;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.animal.Cow;
import net.minecraft.world.entity.animal.horse.AbstractHorse;
import net.minecraft.world.entity.monster.Spider;
import net.minecraft.world.level.block.Blocks;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
import ua.meekeyn.requiemunofficial.RequiemUnofficial;
import ua.meekeyn.requiemunofficial.common.network.PossessionInputPacket;
import ua.meekeyn.requiemunofficial.common.possession.PossessionManager;
import ua.meekeyn.requiemunofficial.common.possession.SkeletonSpiderRidingEvents;
import ua.meekeyn.requiemunofficial.common.possession.UndeadHorseRidingEvents;
import ua.meekeyn.requiemunofficial.common.soul.EntitySoulManager;
import ua.meekeyn.requiemunofficial.common.soul.SoulManager;
import ua.meekeyn.requiemunofficial.common.soul.SoulState;

import java.util.UUID;

@GameTestHolder(RequiemUnofficial.MODID)
@PrefixGameTestTemplate(false)
public final class PossessionRidingGameTests {

    private static final BlockPos TEST_POS = new BlockPos(1, 2, 1);

    private PossessionRidingGameTests() {
    }

    // --- Mounting: all three combos mount the possessed host onto the vehicle. ---

    @GameTest(template = "empty")
    public static void skeletonMountsSpiderAndDrivesIt(GameTestHelper helper) {
        ServerPlayer player = vagrantPlayer(helper, "ride-spider-mount");
        Mob skeleton = spawn(helper, EntityType.SKELETON, TEST_POS);
        Spider spider = spawn(helper, EntityType.SPIDER, new BlockPos(2, 2, 1));
        possess(player, skeleton);

        PlayerInteractEvent.EntityInteract event = interact(player, spider);
        SkeletonSpiderRidingEvents.onEntityInteract(event);

        helper.assertTrue(event.isCanceled(), "Mounting an empty spider should be consumed");
        helper.assertTrue(skeleton.getVehicle() == spider, "The possessed skeleton should ride the spider");
        helper.assertTrue(skeleton.isPassenger(), "The skeleton should be a passenger of the spider");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void skeletonMountsSkeletonHorseAndDrivesIt(GameTestHelper helper) {
        ServerPlayer player = vagrantPlayer(helper, "ride-horse-mount");
        Mob skeleton = spawn(helper, EntityType.SKELETON, TEST_POS);
        AbstractHorse horse = spawn(helper, EntityType.SKELETON_HORSE, new BlockPos(2, 2, 1));
        possess(player, skeleton);

        PlayerInteractEvent.EntityInteract event = interact(player, horse);
        UndeadHorseRidingEvents.onEntityInteract(event);

        helper.assertTrue(event.isCanceled(), "Mounting an empty undead horse should be consumed");
        helper.assertTrue(horse.getControllingPassenger() == skeleton,
                "An undead possessed rider should count as the horse's controlling passenger");
        drive(helper, player, horse, 1.0F, 0.0F);
        helper.assertTrue(horse.zza > 0.0F, "Player forward input should drive the skeleton horse");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void zombieMountsZombieHorseAndDrivesIt(GameTestHelper helper) {
        ServerPlayer player = vagrantPlayer(helper, "ride-zombie-horse-mount");
        Mob zombie = spawn(helper, EntityType.ZOMBIE, TEST_POS);
        AbstractHorse horse = spawn(helper, EntityType.ZOMBIE_HORSE, new BlockPos(2, 2, 1));
        possess(player, zombie);

        PlayerInteractEvent.EntityInteract event = interact(player, horse);
        UndeadHorseRidingEvents.onEntityInteract(event);

        helper.assertTrue(event.isCanceled(), "Mounting an empty zombie horse should be consumed");
        helper.assertTrue(zombie.getVehicle() == horse, "The possessed zombie should ride the zombie horse");
        drive(helper, player, horse, 1.0F, 0.0F);
        helper.assertTrue(horse.zza > 0.0F, "Player forward input should drive the zombie horse");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void drivenHorseMovesForwardOverNativeAiStep(GameTestHelper helper) {
        floor(helper);
        ServerPlayer player = vagrantPlayer(helper, "ride-horse-move");
        Mob skeleton = spawn(helper, EntityType.SKELETON, TEST_POS);
        AbstractHorse horse = spawn(helper, EntityType.SKELETON_HORSE, new BlockPos(2, 2, 2));
        horse.goalSelector.removeAllGoals(goal -> true);
        possess(player, skeleton);
        UndeadHorseRidingEvents.onEntityInteract(interact(player, horse));
        helper.assertTrue(skeleton.getVehicle() == horse, "Setup: the skeleton should ride the horse");

        PossessionManager.updatePossessionInput(player,
                new PossessionInputPacket(1.0F, 0.0F, false, false, 0.0F, 0.0F));
        double startX = horse.getX();
        double startZ = horse.getZ();
        for (int tick = 0; tick < 5; tick++) {
            horse.aiStep();
        }

        double travelled = horse.getZ() - startZ;
        helper.assertTrue(travelled > 0.2D && Math.abs(horse.getX() - startX) < 0.05D,
                "A driven horse must actually move forward over a full aiStep, not just report zza>0 "
                        + "(moved " + travelled + " blocks horizontally)");
        helper.succeed();
    }

    // --- Spider: player drives at normal speed when AI is idle. ---

    @GameTest(template = "empty")
    public static void ridingSkeletonDrivesIdleSpiderAtNormalSpeed(GameTestHelper helper) {
        ServerPlayer player = vagrantPlayer(helper, "ride-spider-drive");
        Mob skeleton = spawn(helper, EntityType.SKELETON, TEST_POS);
        Spider spider = spawn(helper, EntityType.SPIDER, new BlockPos(2, 2, 1));
        possess(player, skeleton);
        SkeletonSpiderRidingEvents.onEntityInteract(interact(player, spider));

        drive(helper, player, spider, 1.0F, 0.0F);

        helper.assertTrue(spider.zza > 0.0F, "Player forward input should drive an idle spider");
        double normalSpeed = spider.getAttributeValue(net.minecraft.world.entity.ai.attributes.Attributes.MOVEMENT_SPEED);
        helper.assertTrue(Math.abs(spider.getSpeed() - normalSpeed) < 1.0E-4,
                "The spider should move at its normal speed with no artificial slowdown");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void drivenIdleSpiderMovesForwardOverNativeAiStep(GameTestHelper helper) {
        floor(helper);
        ServerPlayer player = vagrantPlayer(helper, "ride-spider-move");
        Mob skeleton = spawn(helper, EntityType.SKELETON, TEST_POS);
        Spider spider = spawn(helper, EntityType.SPIDER, new BlockPos(2, 2, 2));
        spider.goalSelector.removeAllGoals(goal -> true);
        possess(player, skeleton);
        SkeletonSpiderRidingEvents.onEntityInteract(interact(player, spider));
        helper.assertTrue(skeleton.getVehicle() == spider, "Setup: the skeleton should ride the spider");
        helper.assertTrue(spider.getTarget() == null && !spider.getNavigation().isInProgress(),
                "Setup: the spider AI should be idle so the player is driving");

        PossessionManager.updatePossessionInput(player,
                new PossessionInputPacket(1.0F, 0.0F, false, false, 0.0F, 0.0F));
        double startX = spider.getX();
        double startZ = spider.getZ();
        for (int tick = 0; tick < 5; tick++) {
            spider.aiStep();
        }

        double travelled = spider.getZ() - startZ;
        helper.assertTrue(travelled > 0.2D && Math.abs(spider.getX() - startX) < 0.05D,
                "A manually driven idle spider must actually move forward over a full aiStep "
                        + "(moved " + travelled + " blocks horizontally)");
        helper.succeed();
    }

    // --- Spider: AI (chase/flee) has full priority and blocks manual movement. ---

    @GameTest(template = "empty")
    public static void spiderAiTakesPriorityOverManualMovement(GameTestHelper helper) {
        ServerPlayer player = vagrantPlayer(helper, "ride-spider-ai-priority");
        Mob skeleton = spawn(helper, EntityType.SKELETON, TEST_POS);
        Spider spider = spawn(helper, EntityType.SPIDER, new BlockPos(2, 2, 1));
        Cow prey = spawn(helper, EntityType.COW, new BlockPos(3, 2, 1));
        possess(player, skeleton);
        SkeletonSpiderRidingEvents.onEntityInteract(interact(player, spider));

        // Make the spider aggressive so its AI is active.
        spider.setTarget(prey);
        helper.assertTrue(spider.getTarget() == prey, "Setup: the spider should be aggressive toward the prey");

        drive(helper, player, spider, 1.0F, 0.0F);

        helper.assertTrue(spider.zza == 0.0F,
                "Manual movement must be fully blocked while the spider AI is driving");
        helper.succeed();
    }

    // --- Spider: aggression toward the rider provokes it, bucks it off, and bans re-mounting. ---

    @GameTest(template = "empty")
    public static void riderHittingSpiderBucksAndProvokesIt(GameTestHelper helper) {
        ServerPlayer player = vagrantPlayer(helper, "ride-spider-provoke");
        Mob skeleton = spawn(helper, EntityType.SKELETON, TEST_POS);
        Spider spider = spawn(helper, EntityType.SPIDER, new BlockPos(2, 2, 1));
        possess(player, skeleton);
        SkeletonSpiderRidingEvents.onEntityInteract(interact(player, spider));

        LivingAttackEvent attack = new LivingAttackEvent(
                spider,
                helper.getLevel().damageSources().mobAttack(skeleton),
                1.0F
        );
        SkeletonSpiderRidingEvents.onLivingAttack(attack);

        helper.assertTrue(!skeleton.isPassenger(), "The provoked spider should buck its rider off");
        helper.assertTrue(spider.getTarget() == skeleton,
                "The spider should turn on the skeleton that struck it");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void provokedSpiderBansRemountUntilAggressionExpires(GameTestHelper helper) {
        ServerPlayer player = vagrantPlayer(helper, "ride-spider-remount-ban");
        Mob skeleton = spawn(helper, EntityType.SKELETON, TEST_POS);
        Spider spider = spawn(helper, EntityType.SPIDER, new BlockPos(2, 2, 1));
        possess(player, skeleton);
        SkeletonSpiderRidingEvents.onEntityInteract(interact(player, spider));
        helper.assertTrue(skeleton.getVehicle() == spider, "Setup: the skeleton should be mounted");

        // The rider strikes its own spider: it is provoked, bucks the rider, and turns on it.
        SkeletonSpiderRidingEvents.onLivingAttack(new LivingAttackEvent(
                spider,
                helper.getLevel().damageSources().mobAttack(skeleton),
                1.0F
        ));
        helper.assertTrue(!skeleton.isPassenger(), "Setup: the rider should be bucked off");
        helper.assertTrue(spider.getTarget() == skeleton, "Setup: the spider should be aggressive toward the rider");

        PlayerInteractEvent.EntityInteract mount = interact(player, spider);
        SkeletonSpiderRidingEvents.onEntityInteract(mount);
        helper.assertTrue(!mount.isCanceled() && !skeleton.isPassenger(),
                "Re-mounting must be refused while the spider is aggressive toward the rider");

        // Let the aggression expire (target cleared) -> mounting is allowed again.
        spider.setTarget(null);
        PlayerInteractEvent.EntityInteract retry = interact(player, spider);
        SkeletonSpiderRidingEvents.onEntityInteract(retry);
        helper.assertTrue(retry.isCanceled() && skeleton.getVehicle() == spider,
                "Re-mounting must be allowed once the aggression expires");
        helper.succeed();
    }

    // --- Spider: aggression toward a third mob keeps the rider aboard but blocks movement. ---

    @GameTest(template = "empty")
    public static void spiderAggressionTowardThirdMobKeepsRiderButBlocksMovement(GameTestHelper helper) {
        ServerPlayer player = vagrantPlayer(helper, "ride-spider-third-mob");
        Mob skeleton = spawn(helper, EntityType.SKELETON, TEST_POS);
        Spider spider = spawn(helper, EntityType.SPIDER, new BlockPos(2, 2, 1));
        Cow bystander = spawn(helper, EntityType.COW, new BlockPos(3, 2, 1));
        possess(player, skeleton);
        SkeletonSpiderRidingEvents.onEntityInteract(interact(player, spider));

        spider.setTarget(bystander);
        drive(helper, player, spider, 1.0F, 0.0F);

        helper.assertTrue(skeleton.isPassenger() && skeleton.getVehicle() == spider,
                "Aggression toward a third mob must never eject the rider");
        helper.assertTrue(spider.zza == 0.0F,
                "The rider must have no movement control while the spider is aggressive toward a third mob");
        helper.succeed();
    }

    // --- Horse: shift dismounts the rider. ---

    @GameTest(template = "empty")
    public static void shiftingDismountsRiderFromHorse(GameTestHelper helper) {
        ServerPlayer player = vagrantPlayer(helper, "ride-horse-dismount");
        Mob skeleton = spawn(helper, EntityType.SKELETON, TEST_POS);
        AbstractHorse horse = spawn(helper, EntityType.SKELETON_HORSE, new BlockPos(2, 2, 1));
        possess(player, skeleton);
        UndeadHorseRidingEvents.onEntityInteract(interact(player, horse));
        helper.assertTrue(skeleton.isPassenger(), "Setup: the skeleton should start mounted");

        drive(helper, player, horse, 0.0F, 0.0F, false, true);
        helper.assertTrue(!skeleton.isPassenger(), "Shift should dismount the rider from the horse");
        helper.succeed();
    }

    // --- Helpers ---

    private static ServerPlayer vagrantPlayer(GameTestHelper helper, String name) {
        ServerPlayer player = new ServerPlayer(
                helper.getLevel().getServer(),
                helper.getLevel(),
                new GameProfile(UUID.randomUUID(), name)
        );
        SoulManager.setState(player, SoulState.VAGRANT);
        return player;
    }

    private static <T extends Mob> T spawn(GameTestHelper helper, EntityType<T> type, BlockPos pos) {
        T entity = type.create(helper.getLevel());
        if (entity == null) {
            throw new IllegalStateException("Failed to create test entity " + type);
        }
        BlockPos absolute = helper.absolutePos(pos);
        entity.moveTo(absolute.getX() + 0.5D, absolute.getY(), absolute.getZ() + 0.5D, 0.0F, 0.0F);
        entity.setOnGround(true);
        helper.getLevel().addFreshEntity(entity);
        return entity;
    }

    private static void floor(GameTestHelper helper) {
        for (int x = 0; x < 5; x++) {
            for (int z = 0; z < 5; z++) {
                helper.setBlock(new BlockPos(x, 1, z), Blocks.STONE);
            }
        }
    }

    private static void possess(ServerPlayer player, LivingEntity host) {
        EntitySoulManager.removeSoul(host);
        if (!PossessionManager.tryPossess(player, host)) {
            throw new IllegalStateException("Setup possession should succeed");
        }
    }

    private static PlayerInteractEvent.EntityInteract interact(ServerPlayer player, net.minecraft.world.entity.Entity target) {
        return new PlayerInteractEvent.EntityInteract(player, InteractionHand.MAIN_HAND, target);
    }

    private static void drive(GameTestHelper helper, ServerPlayer player, LivingEntity vehicle,
                              float forward, float strafe) {
        drive(helper, player, vehicle, forward, strafe, false, false);
    }

    private static void drive(GameTestHelper helper, ServerPlayer player, LivingEntity vehicle,
                              float forward, float strafe, boolean jumping, boolean shifting) {
        PossessionManager.updatePossessionInput(player, new PossessionInputPacket(
                forward,
                strafe,
                jumping,
                shifting,
                0.0F,
                0.0F
        ));
        PossessionManager.preparePossessedHostForAiStep(vehicle);
    }
}
