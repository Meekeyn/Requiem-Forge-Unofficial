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
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import ua.meekeyn.requiemunofficial.RequiemUnofficial;
import ua.meekeyn.requiemunofficial.common.network.PossessionInputPacket;
import ua.meekeyn.requiemunofficial.common.possession.PossessedMountRules;
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
        // Walking pace: the spider's own (non-sprint) speed with the humanoid Player Body scale 0.4.
        double walkSpeed = spider.getAttributeValue(net.minecraft.world.entity.ai.attributes.Attributes.MOVEMENT_SPEED) * 0.4D;
        helper.assertTrue(Math.abs(spider.getSpeed() - walkSpeed) < 1.0E-4,
                "The spider should walk at its normal walking pace, never sprint");
        helper.assertTrue(!spider.isSprinting() && !player.isSprinting(),
                "Riding a spider must never sprint");
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

        LivingIncomingDamageEvent attack = new LivingIncomingDamageEvent(
                spider, new net.neoforged.neoforge.common.damagesource.DamageContainer(helper.getLevel().damageSources().mobAttack(skeleton), 1.0F));
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
        SkeletonSpiderRidingEvents.onLivingAttack(new LivingIncomingDamageEvent(
                spider, new net.neoforged.neoforge.common.damagesource.DamageContainer(helper.getLevel().damageSources().mobAttack(skeleton), 1.0F)));
        helper.assertTrue(!skeleton.isPassenger(), "Setup: the rider should be bucked off");
        helper.assertTrue(spider.getTarget() == skeleton, "Setup: the spider should be aggressive toward the rider");

        PlayerInteractEvent.EntityInteract mount = interact(player, spider);
        SkeletonSpiderRidingEvents.onEntityInteract(mount);
        // Refused, but still consumed so the click never falls through to the held item.
        helper.assertTrue(mount.isCanceled() && !skeleton.isPassenger(),
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

    // --- Павук: власна навігація павука перехоплює керування, після неї ввід гравця повертається. ---

    @GameTest(template = "empty")
    public static void spiderOwnNavigationOverridesRiderInputThenReturnsControl(GameTestHelper helper) {
        floor(helper, 8);
        ServerPlayer player = vagrantPlayer(helper, "ride-spider-own-nav");
        Mob skeleton = spawn(helper, EntityType.SKELETON, new BlockPos(5, 2, 2));
        Spider spider = spawn(helper, EntityType.SPIDER, new BlockPos(5, 2, 2));
        spider.goalSelector.removeAllGoals(goal -> true);
        spider.targetSelector.removeAllGoals(goal -> true);
        possess(player, skeleton);
        SkeletonSpiderRidingEvents.onEntityInteract(interact(player, spider));
        helper.assertTrue(skeleton.getVehicle() == spider, "Setup: the skeleton should ride the spider");

        BlockPos goal = helper.absolutePos(new BlockPos(0, 2, 2));
        helper.assertTrue(spider.getNavigation().moveTo(goal.getX() + 0.5D, goal.getY(), goal.getZ() + 0.5D, 1.0D),
                "Setup: the spider should find a path toward -X");
        // Гравець тисне «вперед» з yaw 180 (= -Z), а павук сам іде до -X. Шлях великого моба
        // зміщений до центру вузла (+Z), тож ввід гравця, якщо б підмішувався, тягнув би в -Z.
        PossessionManager.updatePossessionInput(player,
                new PossessionInputPacket(1.0F, 0.0F, false, false, 180.0F, 0.0F));
        double startX = spider.getX();
        double startZ = spider.getZ();
        for (int tick = 0; tick < 6; tick++) {
            spider.aiStep();
        }
        helper.assertTrue(startX - spider.getX() > 0.3D,
                "The spider's own navigation must move it toward its goal (moved " + (startX - spider.getX()) + ")");
        helper.assertTrue(spider.getZ() - startZ > -0.05D,
                "Rider input must not be mixed into the spider's own movement (drifted "
                        + (spider.getZ() - startZ) + " along the rider's -Z)");

        // Павук більше нікуди не йде: після короткої паузи керування повертається гравцю.
        spider.getNavigation().stop();
        double idleZ = spider.getZ();
        for (int tick = 0; tick < 14; tick++) {
            PossessionManager.updatePossessionInput(player,
                    new PossessionInputPacket(1.0F, 0.0F, false, false, 0.0F, 0.0F));
            spider.aiStep();
        }
        helper.assertTrue(spider.getZ() - idleZ > 0.3D,
                "Once the spider is idle again the rider must drive it forward (moved "
                        + (spider.getZ() - idleZ) + ")");
        helper.succeed();
    }

    // --- Інші транспорти (beta.16 MobRidingType.RIDE): рейдер/спустошувач, дитинча зомбі/курка,
    // піглін/страйдер. Сідання, керування і Shift-злізання - через спільний механізм коня. ---

    @GameTest(template = "empty")
    public static void pillagerMountsRavagerAndDrivesIt(GameTestHelper helper) {
        assertSteerableMount(helper, "ride-ravager", spawn(helper, EntityType.PILLAGER, TEST_POS),
                spawn(helper, EntityType.RAVAGER, new BlockPos(3, 2, 1)));
    }

    @GameTest(template = "empty")
    public static void vindicatorMountsRavagerAndDrivesIt(GameTestHelper helper) {
        assertSteerableMount(helper, "ride-ravager-vindicator", spawn(helper, EntityType.VINDICATOR, TEST_POS),
                spawn(helper, EntityType.RAVAGER, new BlockPos(3, 2, 1)));
    }

    @GameTest(template = "empty")
    public static void babyZombieMountsChickenAndDrivesIt(GameTestHelper helper) {
        Mob zombie = spawn(helper, EntityType.ZOMBIE, TEST_POS);
        zombie.setBaby(true);
        assertSteerableMount(helper, "ride-chicken", zombie, spawn(helper, EntityType.CHICKEN, new BlockPos(2, 2, 1)));
    }

    @GameTest(template = "empty")
    public static void babyHuskMountsChickenAndDrivesIt(GameTestHelper helper) {
        Mob husk = spawn(helper, EntityType.HUSK, TEST_POS);
        husk.setBaby(true);
        assertSteerableMount(helper, "ride-chicken-husk", husk, spawn(helper, EntityType.CHICKEN, new BlockPos(2, 2, 1)));
    }

    @GameTest(template = "empty")
    public static void piglinMountsStriderAndDrivesIt(GameTestHelper helper) {
        assertSteerableMount(helper, "ride-strider", spawn(helper, EntityType.PIGLIN, TEST_POS),
                spawn(helper, EntityType.STRIDER, new BlockPos(2, 2, 1)));
    }

    @GameTest(template = "empty")
    public static void zombifiedPiglinMountsStriderAndDrivesIt(GameTestHelper helper) {
        assertSteerableMount(helper, "ride-strider-zpiglin", spawn(helper, EntityType.ZOMBIFIED_PIGLIN, TEST_POS),
                spawn(helper, EntityType.STRIDER, new BlockPos(2, 2, 1)));
    }

    @GameTest(template = "empty")
    public static void adultZombieCannotMountChickenAndSkeletonCannotMountRavager(GameTestHelper helper) {
        ServerPlayer player = vagrantPlayer(helper, "ride-invalid-pairs");
        Mob zombie = spawn(helper, EntityType.ZOMBIE, TEST_POS);
        Mob chicken = spawn(helper, EntityType.CHICKEN, new BlockPos(2, 2, 1));
        possess(player, zombie);
        PlayerInteractEvent.EntityInteract chickenMount = interact(player, chicken);
        UndeadHorseRidingEvents.onEntityInteract(chickenMount);
        helper.assertTrue(!chickenMount.isCanceled() && !zombie.isPassenger(),
                "An adult zombie must not mount a chicken (vanilla jockeys are babies only)");
        PossessionManager.releaseWithoutInventoryTransfer(player, zombie);

        ServerPlayer second = vagrantPlayer(helper, "ride-invalid-ravager");
        Mob skeleton = spawn(helper, EntityType.SKELETON, new BlockPos(1, 2, 3));
        Mob ravager = spawn(helper, EntityType.RAVAGER, new BlockPos(3, 2, 3));
        possess(second, skeleton);
        PlayerInteractEvent.EntityInteract ravagerMount = interact(second, ravager);
        UndeadHorseRidingEvents.onEntityInteract(ravagerMount);
        helper.assertTrue(!ravagerMount.isCanceled() && !skeleton.isPassenger(),
                "A non-raider must not mount a ravager");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void drivenRavagerMovesForwardOverNativeAiStep(GameTestHelper helper) {
        floor(helper, 6);
        ServerPlayer player = vagrantPlayer(helper, "ride-ravager-move");
        Mob pillager = spawn(helper, EntityType.PILLAGER, TEST_POS);
        Mob ravager = spawn(helper, EntityType.RAVAGER, new BlockPos(2, 2, 2));
        possess(player, pillager);
        UndeadHorseRidingEvents.onEntityInteract(interact(player, ravager));
        helper.assertTrue(pillager.getVehicle() == ravager, "Setup: the pillager should ride the ravager");

        PossessionManager.updatePossessionInput(player,
                new PossessionInputPacket(1.0F, 0.0F, false, false, 0.0F, 0.0F));
        double startX = ravager.getX();
        double startZ = ravager.getZ();
        for (int tick = 0; tick < 5; tick++) {
            ravager.aiStep();
        }
        double travelled = ravager.getZ() - startZ;
        helper.assertTrue(travelled > 0.2D && Math.abs(ravager.getX() - startX) < 0.05D,
                "A driven ravager must actually move forward over a full aiStep (moved " + travelled + ")");
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

    // --- Транспорт, яким керує інший одержимий гравець. ---

    @GameTest(template = "empty")
    public static void possessedHorseCannotBeMountedByAnotherPossessedRider(GameTestHelper helper) {
        ServerPlayer horsePlayer = vagrantPlayer(helper, "ride-possessed-horse-owner");
        ServerPlayer riderPlayer = vagrantPlayer(helper, "ride-possessed-horse-rider");
        Mob skeleton = spawn(helper, EntityType.SKELETON, TEST_POS);
        AbstractHorse horse = spawn(helper, EntityType.SKELETON_HORSE, new BlockPos(2, 2, 1));
        possess(horsePlayer, horse);
        possess(riderPlayer, skeleton);

        helper.assertTrue(!PossessedMountRules.canMount(horse, skeleton),
                "canMount must reject a vehicle that is possessed by another player");
        PlayerInteractEvent.EntityInteract event = interact(riderPlayer, horse);
        UndeadHorseRidingEvents.onEntityInteract(event);
        helper.assertTrue(event.isCanceled(),
                "Interacting with a possessed horse must be consumed so the player's own body does not interact");
        helper.assertTrue(!skeleton.isPassenger() && horse.getPassengers().isEmpty(),
                "A possessed rider must not mount a horse possessed by another player");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void possessedSpiderCannotBeMountedByAnotherPossessedSkeleton(GameTestHelper helper) {
        ServerPlayer spiderPlayer = vagrantPlayer(helper, "ride-possessed-spider-owner");
        ServerPlayer riderPlayer = vagrantPlayer(helper, "ride-possessed-spider-rider");
        Mob skeleton = spawn(helper, EntityType.SKELETON, TEST_POS);
        Spider spider = spawn(helper, EntityType.SPIDER, new BlockPos(2, 2, 1));
        possess(spiderPlayer, spider);
        possess(riderPlayer, skeleton);

        PlayerInteractEvent.EntityInteract event = interact(riderPlayer, spider);
        SkeletonSpiderRidingEvents.onEntityInteract(event);
        helper.assertTrue(!skeleton.isPassenger() && spider.getPassengers().isEmpty(),
                "A possessed skeleton must not mount a spider possessed by another player");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void vehicleCarryingPossessedRiderCannotBePossessed(GameTestHelper helper) {
        ServerPlayer riderPlayer = vagrantPlayer(helper, "ride-carrying-rider");
        ServerPlayer otherPlayer = vagrantPlayer(helper, "ride-carrying-other");
        Mob skeleton = spawn(helper, EntityType.SKELETON, TEST_POS);
        AbstractHorse horse = spawn(helper, EntityType.SKELETON_HORSE, new BlockPos(2, 2, 1));
        possess(riderPlayer, skeleton);
        UndeadHorseRidingEvents.onEntityInteract(interact(riderPlayer, horse));
        helper.assertTrue(skeleton.getVehicle() == horse, "Setup: the possessed skeleton should ride the horse");

        EntitySoulManager.removeSoul(horse);
        helper.assertTrue(!PossessionManager.tryPossess(otherPlayer, horse),
                "A vehicle carrying a possessed rider must not be possessed by another player");
        helper.assertTrue(PossessionManager.getPossessorId(horse) == null,
                "The ridden vehicle must stay unpossessed");
        helper.assertTrue(skeleton.getVehicle() == horse
                        && PossessionManager.shouldSuppressRiddenVehicleAi(horse),
                "The possessed rider must keep steering its vehicle");
        helper.succeed();
    }

    // --- Helpers ---

    private static ServerPlayer vagrantPlayer(GameTestHelper helper, String name) {
        ServerPlayer player = new ServerPlayer(
                helper.getLevel().getServer(),
                helper.getLevel(),
                new GameProfile(UUID.randomUUID(), name)
        , net.minecraft.server.level.ClientInformation.createDefault());
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
        floor(helper, 5);
    }

    private static void floor(GameTestHelper helper, int size) {
        for (int x = 0; x < size; x++) {
            for (int z = 0; z < size; z++) {
                helper.setBlock(new BlockPos(x, 1, z), Blocks.STONE);
            }
        }
    }

    // Сідання на керований транспорт, рух уперед від вводу гравця і злізання по Shift.
    private static void assertSteerableMount(GameTestHelper helper, String name, Mob rider, Mob vehicle) {
        ServerPlayer player = vagrantPlayer(helper, name);
        possess(player, rider);

        PlayerInteractEvent.EntityInteract event = interact(player, vehicle);
        UndeadHorseRidingEvents.onEntityInteract(event);
        helper.assertTrue(event.isCanceled(), "Mounting " + vehicle.getType() + " should be consumed");
        helper.assertTrue(rider.getVehicle() == vehicle,
                "The possessed " + rider.getType() + " should ride the " + vehicle.getType());
        helper.assertTrue(PossessionManager.shouldSuppressRiddenVehicleAi(vehicle),
                "The vehicle's own AI must not fight the rider's steering");

        drive(helper, player, vehicle, 1.0F, 0.0F);
        helper.assertTrue(vehicle.zza > 0.0F, "Player forward input should drive the " + vehicle.getType());

        drive(helper, player, vehicle, 0.0F, 0.0F, false, true);
        helper.assertTrue(!rider.isPassenger(), "Shift should dismount the rider from the " + vehicle.getType());
        helper.succeed();
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
