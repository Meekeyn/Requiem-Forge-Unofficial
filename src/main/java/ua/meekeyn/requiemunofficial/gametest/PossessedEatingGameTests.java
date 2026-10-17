package ua.meekeyn.requiemunofficial.gametest;

import com.mojang.authlib.GameProfile;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.Skeleton;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.entity.monster.piglin.Piglin;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
import ua.meekeyn.requiemunofficial.RequiemUnofficial;
import ua.meekeyn.requiemunofficial.common.possession.PossessedItemManager;
import ua.meekeyn.requiemunofficial.common.possession.PossessionManager;
import ua.meekeyn.requiemunofficial.common.soul.EntitySoulManager;
import ua.meekeyn.requiemunofficial.common.soul.SoulManager;
import ua.meekeyn.requiemunofficial.common.soul.SoulState;
import ua.meekeyn.requiemunofficial.common.util.ModTags;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

/**
 * Механіка їжі й голоду для тіл під контролем блукаючої душі.
 * Ключове правило: чи є тіло "regular eater" визначає тег behavior/regular_eaters
 * (як у beta.16), а не MobType.UNDEAD чи instanceof Monster.
 */
@GameTestHolder(RequiemUnofficial.MODID)
@PrefixGameTestTemplate(false)
public final class PossessedEatingGameTests {
    private PossessedEatingGameTests() { }

    @GameTest(template = "empty", batch = "possessed_eating", timeoutTicks = 120)
    public static void regularEaterIsDrivenByBehaviorTag(GameTestHelper helper) {
        Piglin piglin = spawn(helper, EntityType.PIGLIN);
        Zombie zombie = spawn(helper, EntityType.ZOMBIE);
        Skeleton skeleton = spawn(helper, EntityType.SKELETON);

        helper.assertTrue(piglin.getType().is(ModTags.REGULAR_EATERS),
                "Piglins must be regular eaters through the behavior tag");
        helper.assertTrue(EntityType.VILLAGER.is(ModTags.REGULAR_EATERS),
                "Villagers must be regular eaters through the behavior tag");
        helper.assertTrue(!zombie.getType().is(ModTags.REGULAR_EATERS),
                "Zombies must not be regular eaters");
        helper.assertTrue(!skeleton.getType().is(ModTags.REGULAR_EATERS),
                "Skeletons must not be regular eaters");
        helper.assertTrue(!EntityType.FOX.is(ModTags.REGULAR_EATERS),
                "Foxes must not be regular eaters");

        ServerPlayer undeadPossessor = possessor(helper, "tag-undead");
        helper.assertTrue(PossessionManager.tryPossess(undeadPossessor, zombie),
                "A vagrant should possess a soulless zombie");
        helper.assertTrue(PossessionManager.shouldSkipHungerTick(undeadPossessor),
                "A non-eater body must bypass the hunger tick");
        helper.assertTrue(PossessionManager.shouldForceFullHunger(undeadPossessor),
                "A non-eater body must pin its hunger bar full");

        ServerPlayer eaterPossessor = possessor(helper, "tag-eater");
        helper.assertTrue(PossessionManager.tryPossess(eaterPossessor, piglin),
                "A vagrant should possess a soulless piglin");
        helper.assertTrue(!PossessionManager.shouldSkipHungerTick(eaterPossessor),
                "A regular eater body must keep the vanilla hunger tick");
        helper.assertTrue(!PossessionManager.shouldForceFullHunger(eaterPossessor),
                "A regular eater body must keep its real hunger, not a pinned bar");

        cleanup(helper, undeadPossessor, zombie);
        cleanup(helper, eaterPossessor, piglin);
        skeleton.discard();
        helper.succeed();
    }

    @GameTest(template = "empty", batch = "possessed_eating", timeoutTicks = 120)
    public static void nonEaterBodyIgnoresHungerDrainRegenAndStarvation(GameTestHelper helper) {
        Zombie zombie = spawn(helper, EntityType.ZOMBIE);
        zombie.setHealth(10.0F);
        ServerPlayer player = possessor(helper, "non-eater-hunger");
        helper.assertTrue(PossessionManager.tryPossess(player, zombie),
                "A vagrant should possess a soulless zombie");

        player.getFoodData().setFoodLevel(0);
        player.getFoodData().setSaturation(0.0F);
        player.getFoodData().setExhaustion(40.0F);
        player.setHealth(10.0F);
        for (int i = 0; i < 100; i++) {
            player.getFoodData().tick(player);
        }

        helper.assertTrue(player.getFoodData().getFoodLevel() == 20,
                "A non-eater body must neither drain nor starve its hunger bar");
        helper.assertTrue(player.getFoodData().getExhaustionLevel() == 0.0F,
                "A non-eater body must clear accumulated exhaustion");
        helper.assertTrue(player.getHealth() == 10.0F,
                "A non-eater body must not regenerate or lose health from hunger");
        helper.assertTrue(zombie.getHealth() == 10.0F,
                "A non-eater body must not take starvation damage on the host");

        cleanup(helper, player, zombie);
        helper.succeed();
    }

    @GameTest(template = "empty", batch = "possessed_eating", timeoutTicks = 120)
    public static void regularEaterBodyKeepsVanillaHungerTick(GameTestHelper helper) {
        Piglin piglin = spawn(helper, EntityType.PIGLIN);
        ServerPlayer player = possessor(helper, "eater-hunger");
        helper.assertTrue(PossessionManager.tryPossess(player, piglin),
                "A vagrant should possess a soulless piglin");

        player.getFoodData().setSaturation(5.0F);
        player.getFoodData().setExhaustion(40.0F);
        player.getFoodData().tick(player);

        helper.assertTrue(player.getFoodData().getExhaustionLevel() == 36.0F,
                "A regular eater body must keep the vanilla exhaustion drain");

        cleanup(helper, player, piglin);
        helper.succeed();
    }

    @GameTest(template = "empty", batch = "possessed_eating", timeoutTicks = 120)
    public static void releasedEaterBodyRestoresPossessorHungerWithoutResettingIt(GameTestHelper helper) {
        Piglin piglin = spawn(helper, EntityType.PIGLIN);
        ServerPlayer player = possessor(helper, "release-hunger");
        player.getFoodData().setFoodLevel(7);
        helper.assertTrue(PossessionManager.tryPossess(player, piglin),
                "A vagrant should possess a soulless piglin");
        helper.assertTrue(player.getFoodData().getFoodLevel() == 20,
                "Possessed eater bodies should start with their own hunger data");

        player.getFoodData().setFoodLevel(3);
        PossessionManager.release(player, piglin);

        helper.assertTrue(player.getFoodData().getFoodLevel() == 7,
                "Leaving an eater body must restore the possessor's previous hunger");
        helper.assertTrue(PossessionManager.shouldSkipHungerTick(player),
                "A bodiless vagrant must not use the hunger tick");
        helper.assertTrue(!PossessionManager.shouldForceFullHunger(player),
                "A bodiless vagrant must not overwrite its stored hunger");

        player.getFoodData().tick(player);
        helper.assertTrue(player.getFoodData().getFoodLevel() == 7,
                "A bodiless vagrant must freeze, not drain or reset, its stored hunger");

        piglin.discard();
        helper.succeed();
    }

    @GameTest(template = "empty", batch = "possessed_eating", timeoutTicks = 120)
    public static void eaterBodyHungerPersistsAcrossPossessions(GameTestHelper helper) {
        Piglin piglin = spawn(helper, EntityType.PIGLIN);
        ServerPlayer player = possessor(helper, "eater-persist");
        player.getFoodData().setFoodLevel(7);
        helper.assertTrue(PossessionManager.tryPossess(player, piglin),
                "A vagrant should possess a soulless piglin");

        player.getFoodData().setFoodLevel(3);
        PossessionManager.release(player, piglin);
        helper.assertTrue(
                piglin.getPersistentData().getCompound("RequiemPossessedFood").getInt("foodLevel") == 3,
                "The eater body must keep its own hunger when the possessor leaves");

        player.getFoodData().setFoodLevel(9);
        helper.assertTrue(PossessionManager.tryPossess(player, piglin),
                "A vagrant should re-possess the same soulless piglin");
        helper.assertTrue(player.getFoodData().getFoodLevel() == 3,
                "Re-possessing an eater body must restore the body's stored hunger");

        cleanup(helper, player, piglin);
        helper.succeed();
    }

    @GameTest(template = "empty", batch = "possessed_eating", timeoutTicks = 120)
    public static void zombieHealingHonoursFoodUseTimeCooldownAndSingleConsumption(GameTestHelper helper) {
        Zombie zombie = spawn(helper, EntityType.ZOMBIE);
        zombie.setHealth(zombie.getMaxHealth() - 8.0F);
        ServerPlayer player = possessor(helper, "zombie-heal");
        helper.assertTrue(PossessionManager.tryPossess(player, zombie),
                "A vagrant should possess a soulless zombie");

        ItemStack beef = new ItemStack(Items.BEEF);
        player.setItemInHand(InteractionHand.MAIN_HAND, beef);
        float before = zombie.getHealth();
        helper.assertTrue(PossessedItemManager.tryUsePossessedItemForTest(player, zombie, beef),
                "Raw meat must start a possessed healing use for the zombie");
        helper.assertTrue(player.isUsingItem(), "Zombie raw meat must be held for its configured use time");
        helper.assertTrue(player.getUseItemRemainingTicks() > 0,
                "Zombie raw meat use time must be applied to the stack");

        int nutrition = beef.getFoodProperties(player).getNutrition();
        beef.finishUsingItem(helper.getLevel(), player);
        helper.assertTrue(zombie.getHealth() == before + (float) nutrition,
                "Zombie raw meat must heal by the food's nutrition");
        helper.assertTrue(beef.isEmpty(), "Zombie raw meat must be consumed once");
        helper.assertTrue(player.getCooldowns().isOnCooldown(Items.BEEF),
                "Zombie raw meat must apply its cooldown");

        float afterHeal = zombie.getHealth();
        ItemStack secondBeef = new ItemStack(Items.BEEF);
        secondBeef.finishUsingItem(helper.getLevel(), player);
        helper.assertTrue(zombie.getHealth() == afterHeal,
                "A cooldown-blocked second use must not double-heal the zombie");
        helper.assertTrue(!secondBeef.isEmpty(),
                "A cooldown-blocked second use must not consume the food");

        cleanup(helper, player, zombie);
        helper.succeed();
    }

    @GameTest(template = "empty", batch = "possessed_eating", timeoutTicks = 120)
    public static void skeletonBoneReplacementHealsFourOnceWithCooldown(GameTestHelper helper) {
        Skeleton skeleton = spawn(helper, EntityType.SKELETON);
        skeleton.setHealth(skeleton.getMaxHealth() - 10.0F);
        ServerPlayer player = possessor(helper, "skeleton-bone");
        helper.assertTrue(PossessionManager.tryPossess(player, skeleton),
                "A vagrant should possess a soulless skeleton");

        ItemStack bone = new ItemStack(Items.BONE);
        player.setItemInHand(InteractionHand.MAIN_HAND, bone);
        float before = skeleton.getHealth();
        helper.assertTrue(PossessedItemManager.tryUsePossessedItemForTest(player, skeleton, bone),
                "A bone must be usable on the possessed skeleton");
        helper.assertTrue(skeleton.getHealth() == before + 4.0F,
                "Bone replacement must heal exactly four health");
        helper.assertTrue(bone.isEmpty(), "Bone replacement must consume one bone");
        helper.assertTrue(player.getCooldowns().isOnCooldown(Items.BONE),
                "Bone replacement must apply its cooldown");

        float afterHeal = skeleton.getHealth();
        ItemStack secondBone = new ItemStack(Items.BONE);
        helper.assertTrue(PossessedItemManager.tryUsePossessedItemForTest(player, skeleton, secondBone),
                "A second bone use must be blocked by the cooldown without error");
        helper.assertTrue(skeleton.getHealth() == afterHeal,
                "A second bone use must not double-heal the skeleton");
        helper.assertTrue(!secondBone.isEmpty(),
                "A cooldown-blocked bone must not be consumed");

        cleanup(helper, player, skeleton);
        helper.succeed();
    }

    @GameTest(template = "empty", batch = "possessed_eating", timeoutTicks = 120)
    public static void eatingDataMatchesBeta16TagsPrioritiesAndFlags(GameTestHelper helper) {
        assertDietFile(helper, "diets/villager_diet.json",
                "\"#requiemunofficial:villagers\"", "\"requiemunofficial:food/villager_diet\"", null, null);
        assertDietFile(helper, "diets/villager_base_diet.json",
                "\"#requiemunofficial:villager_folk\"", "\"requiemunofficial:food/villager_base_diet\"",
                "\"priority\":80", null);
        assertDietFile(helper, "diets/illager_diet.json",
                "\"#requiemunofficial:illagers\"", "\"requiemunofficial:food/illager_diet\"", null, null);
        assertDietFile(helper, "diets/mushroom_diet.json",
                "\"#requiemunofficial:mushroom_folk\"", "\"requiemunofficial:food/mushroom_based\"", null, null);
        assertDietFile(helper, "diets/piglin_diet.json",
                "\"#requiemunofficial:piglins\"", "\"requiemunofficial:food/piglin_diet\"", null, null);
        assertDietFile(helper, "diets/piglin_diet_always_edible.json",
                "\"#requiemunofficial:piglins\"", "\"always_edible\":true", "\"priority\":80", null);
        assertDietFile(helper, "diets/witch_diet.json",
                "\"#requiemunofficial:witches\"", "\"requiemunofficial:food/witch_diet\"",
                null, "\"remove_harmful_effects\"");
        assertDietFile(helper, "diets/witch_diet_always_edible.json",
                "\"#requiemunofficial:witches\"", "\"always_edible\":true", "\"priority\":80", null);

        String zombie = readResource(helper, "healing/zombie_snack.json");
        String zombieCompact = zombie == null ? "" : zombie.replaceAll("\\s+", "");
        helper.assertTrue(zombieCompact.contains("\"use_time\":32") && zombieCompact.contains("\"cooldown\":150"),
                "Zombie raw meat healing must use beta.16 use time and cooldown");
        String drowned = readResource(helper, "healing/drowned_delicacy.json");
        String drownedCompact = drowned == null ? "" : drowned.replaceAll("\\s+", "");
        helper.assertTrue(drownedCompact.contains("\"use_time\":32") && drownedCompact.contains("\"cooldown\":150"),
                "Drowned raw fish healing must use beta.16 use time and cooldown");
        String bone = readResource(helper, "healing/bony_prosthesis.json");
        String boneCompact = bone == null ? "" : bone.replaceAll("\\s+", "");
        helper.assertTrue(boneCompact.contains("\"usage\":\"replace_bone\"")
                        && boneCompact.contains("\"cooldown\":60")
                        && boneCompact.contains("\"use_time\":0"),
                "Skeleton bone replacement must be instant with a 60 tick cooldown");
        String fox = readResource(helper, "healing/foxy_berry.json");
        String foxCompact = fox == null ? "" : fox.replaceAll("\\s+", "");
        helper.assertTrue(foxCompact.contains("\"use_time\":16") && foxCompact.contains("\"cooldown\":48"),
                "Fox sweet berries must use beta.16 16/48 timing");

        helper.succeed();
    }

    private static void assertDietFile(GameTestHelper helper,
                                       String relative,
                                       String expectedMob,
                                       String expectedFood,
                                       String expectedPriority,
                                       String expectedFilter) {
        String text = readResource(helper, relative);
        helper.assertTrue(text != null, "Diet file " + relative + " must be present");
        String compact = text.replaceAll("\\s+", "");
        helper.assertTrue(compact.contains(expectedMob),
                relative + " must target " + expectedMob);
        helper.assertTrue(compact.contains(expectedFood),
                relative + " must reference " + expectedFood);
        if (expectedPriority != null) {
            helper.assertTrue(compact.contains(expectedPriority.replaceAll("\\s+", "")),
                    relative + " must declare " + expectedPriority);
        }
        if (expectedFilter != null) {
            helper.assertTrue(compact.contains(expectedFilter.replaceAll("\\s+", "")),
                    relative + " must declare " + expectedFilter);
        }
    }

    private static String readResource(GameTestHelper helper, String relative) {
        for (Path base = Paths.get("").toAbsolutePath(); base != null; base = base.getParent()) {
            Path path = base.resolve(Paths.get(
                    "src", "main", "resources", "data", "requiemunofficial", "requiem", "mob_items", relative));
            if (Files.exists(path)) {
                try {
                    return Files.readString(path);
                } catch (IOException exception) {
                    helper.fail("Could not read " + relative + ": " + exception.getMessage());
                    return null;
                }
            }
        }
        return null;
    }

    private static <T extends Mob> T spawn(GameTestHelper helper, EntityType<T> type) {
        T mob = type.create(helper.getLevel());
        mob.moveTo(helper.absolutePos(new BlockPos(2, 2, 2)), 0.0F, 0.0F);
        mob.setOnGround(true);
        mob.setDeltaMovement(Vec3.ZERO);
        helper.getLevel().addFreshEntity(mob);
        EntitySoulManager.removeSoul(mob);
        return mob;
    }

    private static ServerPlayer possessor(GameTestHelper helper, String name) {
        ServerPlayer player = new ServerPlayer(helper.getLevel().getServer(), helper.getLevel(),
                new GameProfile(UUID.randomUUID(), name));
        player.moveTo(helper.absolutePos(new BlockPos(2, 2, 2)), 0.0F, 0.0F);
        player.setOnGround(true);
        player.setDeltaMovement(Vec3.ZERO);
        GameTestSupport.attachConnection(helper, player);
        SoulManager.setState(player, SoulState.VAGRANT);
        return player;
    }

    private static void cleanup(GameTestHelper helper, ServerPlayer player, Mob host) {
        if (SoulManager.getPossessedEntityId(player) != null) {
            PossessionManager.release(player, host);
        }
        host.discard();
    }
}
