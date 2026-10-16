package ua.meekeyn.requiemunofficial.gametest;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.ZombieVillager;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.npc.VillagerProfession;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.nbt.CompoundTag;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
import ua.meekeyn.requiemunofficial.RequiemUnofficial;
import ua.meekeyn.requiemunofficial.common.transformation.TransformationManager;
import ua.meekeyn.requiemunofficial.common.init.ModEntities;
import ua.meekeyn.requiemunofficial.common.possession.PossessionManager;
import ua.meekeyn.requiemunofficial.common.soul.EntitySoulManager;
import ua.meekeyn.requiemunofficial.common.soul.SoulManager;
import ua.meekeyn.requiemunofficial.common.soul.SoulState;

@GameTestHolder(RequiemUnofficial.MODID)
@PrefixGameTestTemplate(false)
public final class TransformationGameTests {

    private static final BlockPos TEST_POS = new BlockPos(1, 2, 1);

    private TransformationGameTests() {
    }

    @GameTest(template = "empty")
    public static void skeletonizesZombie(GameTestHelper helper) {
        Mob zombie = spawn(helper, EntityType.ZOMBIE);
        helper.assertTrue(
                TransformationManager.skeletonize(zombie) != null,
                "Skeletonization should create a replacement"
        );
        helper.succeedIf(() -> helper.assertEntityPresent(EntityType.SKELETON, TEST_POS));
    }

    @GameTest(template = "empty")
    public static void decaysSkeletonToZombie(GameTestHelper helper) {
        Mob skeleton = spawn(helper, EntityType.SKELETON);
        helper.assertTrue(
                TransformationManager.decayToZombie(skeleton) != null,
                "Zombie decay should create a replacement"
        );
        helper.succeedIf(() -> helper.assertEntityPresent(EntityType.ZOMBIE, TEST_POS));
    }

    @GameTest(template = "empty")
    public static void decaysZombieToSkeleton(GameTestHelper helper) {
        Mob zombie = spawn(helper, EntityType.ZOMBIE);
        helper.assertTrue(
                TransformationManager.decayToSkeleton(zombie) != null,
                "Skeleton decay should create a replacement"
        );
        helper.succeedIf(() -> helper.assertEntityPresent(EntityType.SKELETON, TEST_POS));
    }

    @GameTest(template = "empty")
    public static void skeletonizesDrownedToStray(GameTestHelper helper) {
        Mob drowned = spawn(helper, EntityType.DROWNED);
        Mob replacement = TransformationManager.skeletonize(drowned);
        helper.assertTrue(replacement != null && replacement.getType() == EntityType.STRAY,
                "Drowned should skeletonize into a stray");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void decaysStrayToDrowned(GameTestHelper helper) {
        Mob stray = spawn(helper, EntityType.STRAY);
        Mob replacement = TransformationManager.decayToZombie(stray);
        helper.assertTrue(replacement != null && replacement.getType() == EntityType.DROWNED,
                "Stray should decay into a drowned");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void skeletonizesZombifiedPiglinToWitherSkeleton(GameTestHelper helper) {
        Mob piglin = spawn(helper, EntityType.ZOMBIFIED_PIGLIN);
        Mob replacement = TransformationManager.skeletonize(piglin);
        helper.assertTrue(replacement != null && replacement.getType() == EntityType.WITHER_SKELETON,
                "Zombified piglin should skeletonize into a wither skeleton");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void decaysWitherSkeletonToZombifiedPiglin(GameTestHelper helper) {
        Mob skeleton = spawn(helper, EntityType.WITHER_SKELETON);
        Mob replacement = TransformationManager.decayToZombie(skeleton);
        helper.assertTrue(replacement != null && replacement.getType() == EntityType.ZOMBIFIED_PIGLIN,
                "Wither skeleton should decay into a zombified piglin");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void doesNotReskeletonizeSkeleton(GameTestHelper helper) {
        Mob skeleton = spawn(helper, EntityType.SKELETON);
        helper.assertTrue(
                TransformationManager.skeletonize(skeleton) == null,
                "An existing skeleton should not be skeletonized again"
        );
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void curesZombieVillager(GameTestHelper helper) {
        ZombieVillager zombieVillager = spawn(helper, EntityType.ZOMBIE_VILLAGER);
        zombieVillager.setVillagerData(
                zombieVillager.getVillagerData()
                        .setProfession(VillagerProfession.LIBRARIAN)
                        .setLevel(3)
        );
        zombieVillager.setVillagerXp(85);
        CompoundTag offers = new CompoundTag();
        offers.put("Recipes", new net.minecraft.nbt.ListTag());
        zombieVillager.setTradeOffers(offers);

        Mob replacement = TransformationManager.cureUndead(zombieVillager);
        helper.assertTrue(replacement != null && replacement.getType() == ModEntities.CURED_VILLAGER.get(),
                "Zombie villager should cure into a cured villager");
        helper.assertTrue(replacement instanceof Villager, "Cured entity should retain villager behavior");
        Villager cured = (Villager) replacement;
        helper.assertTrue(
                cured.getVillagerData().getProfession() == VillagerProfession.LIBRARIAN,
                "Curing should preserve villager profession"
        );
        helper.assertTrue(cured.getVillagerData().getLevel() == 3, "Curing should preserve villager level");
        helper.assertTrue(cured.getVillagerXp() == 85, "Curing should preserve villager experience");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void curesZombifiedPiglin(GameTestHelper helper) {
        Mob zombifiedPiglin = spawn(helper, EntityType.ZOMBIFIED_PIGLIN);
        Mob replacement = TransformationManager.cureUndead(zombifiedPiglin);
        helper.assertTrue(replacement != null && replacement.getType() == ModEntities.CURED_PIGLIN.get(),
                "Zombified piglin should cure into a cured piglin");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void restoresCuredPiglinBruteLineage(GameTestHelper helper) {
        Mob piglinBrute = spawn(helper, EntityType.PIGLIN_BRUTE);
        Mob skeleton = TransformationManager.skeletonize(piglinBrute);
        helper.assertTrue(
                skeleton != null && skeleton.getType() == EntityType.WITHER_SKELETON,
                "Piglin brute should skeletonize into a wither skeleton"
        );

        Mob zombified = TransformationManager.decayToZombie(skeleton);
        helper.assertTrue(
                zombified != null && zombified.getType() == EntityType.ZOMBIFIED_PIGLIN,
                "Wither skeleton should decay into a zombified piglin"
        );

        Mob cured = TransformationManager.cureUndead(zombified);
        helper.assertTrue(
                cured != null && cured.getType() == ModEntities.CURED_PIGLIN_BRUTE.get(),
                "Curing should restore the piglin brute body type"
        );
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void transformationPreservesBodyState(GameTestHelper helper) {
        Mob zombie = spawn(helper, EntityType.ZOMBIE);
        zombie.setCustomName(Component.literal("Persistent Test Body"));
        zombie.setCustomNameVisible(true);
        zombie.setNoAi(true);
        zombie.setLeftHanded(true);
        zombie.setCanPickUpLoot(true);
        zombie.setSilent(true);
        zombie.setNoGravity(true);
        zombie.setGlowingTag(true);
        zombie.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.IRON_SWORD));
        zombie.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 200, 1));
        zombie.setHealth(zombie.getMaxHealth() / 2.0F);

        Mob replacement = TransformationManager.skeletonize(zombie);
        helper.assertTrue(replacement != null, "Transformation should create a replacement");
        helper.assertTrue(replacement.isNoAi(), "No-AI state should persist");
        helper.assertTrue(replacement.isLeftHanded(), "Handedness should persist");
        helper.assertTrue(replacement.canPickUpLoot(), "Loot pickup should persist");
        helper.assertTrue(replacement.isSilent(), "Silent state should persist");
        helper.assertTrue(replacement.isNoGravity(), "Gravity state should persist");
        helper.assertTrue(replacement.hasGlowingTag(), "Glowing state should persist");
        helper.assertTrue(replacement.hasCustomName(), "Custom name should persist");
        helper.assertTrue(
                replacement.getItemBySlot(EquipmentSlot.MAINHAND).is(Items.IRON_SWORD),
                "Equipment should persist"
        );
        helper.assertTrue(replacement.hasEffect(MobEffects.MOVEMENT_SPEED), "Effects should persist");
        helper.assertTrue(
                Math.abs(replacement.getHealth() / replacement.getMaxHealth() - 0.5F) < 0.01F,
                "Health ratio should persist"
        );
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void possessedTransformationTransfersHost(GameTestHelper helper) {
        net.minecraft.server.level.ServerPlayer player = new net.minecraft.server.level.ServerPlayer(
                helper.getLevel().getServer(),
                helper.getLevel(),
                new com.mojang.authlib.GameProfile(java.util.UUID.randomUUID(), "transform-possession-test")
        );
        SoulManager.setState(player, SoulState.VAGRANT);

        Mob zombie = spawn(helper, EntityType.ZOMBIE);
        EntitySoulManager.removeSoul(zombie);
        helper.assertTrue(PossessionManager.tryPossess(player, zombie),
                "A vagrant should possess a soulless zombie");
        helper.assertTrue(zombie.isPersistenceRequired(),
                "Possessed hosts should be persistent like the original");

        Mob skeleton = TransformationManager.skeletonize(zombie);
        helper.assertTrue(skeleton != null && skeleton.getType() == EntityType.SKELETON,
                "Possessed zombie should skeletonize into a skeleton");
        helper.assertTrue(
                skeleton.getUUID().equals(SoulManager.getPossessedEntityId(player)),
                "Possession should transfer to the transformed body"
        );
        helper.assertTrue(player.getUUID().equals(PossessionManager.getPossessorId(skeleton)),
                "Transformed body should keep the possessor id");
        helper.assertTrue(skeleton.isPersistenceRequired(),
                "Transformed possessed body should stay persistent");
        helper.succeed();
    }

    private static <T extends Mob> T spawn(GameTestHelper helper, EntityType<T> type) {
        T entity = type.create(helper.getLevel());
        if (entity == null) {
            throw new IllegalStateException("Failed to create test entity " + type);
        }

        BlockPos absolutePos = helper.absolutePos(TEST_POS);
        entity.moveTo(
                absolutePos.getX() + 0.5D,
                absolutePos.getY(),
                absolutePos.getZ() + 0.5D,
                0.0F,
                0.0F
        );
        helper.getLevel().addFreshEntity(entity);
        return entity;
    }
}
