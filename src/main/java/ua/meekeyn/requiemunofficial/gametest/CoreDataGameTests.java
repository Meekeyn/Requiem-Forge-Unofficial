package ua.meekeyn.requiemunofficial.gametest;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.projectile.SmallFireball;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
import ua.meekeyn.requiemunofficial.RequiemUnofficial;
import ua.meekeyn.requiemunofficial.common.block.RiftRunestoneBlock;
import ua.meekeyn.requiemunofficial.common.block.entity.RunestoneBlockEntity;
import ua.meekeyn.requiemunofficial.common.init.ModBlocks;
import ua.meekeyn.requiemunofficial.common.init.ModItems;
import ua.meekeyn.requiemunofficial.common.init.ModEntities;
import ua.meekeyn.requiemunofficial.common.item.SoulVesselItem;
import ua.meekeyn.requiemunofficial.common.item.RemnantVesselItem;
import ua.meekeyn.requiemunofficial.common.item.IchorVesselItem;
import ua.meekeyn.requiemunofficial.common.item.FilledSoulVesselItem;
import ua.meekeyn.requiemunofficial.common.item.HumanityEnchantedBookItem;
import ua.meekeyn.requiemunofficial.common.entity.PlayerShellEntity;
import ua.meekeyn.requiemunofficial.common.entity.ObeliskSoulEntity;
import ua.meekeyn.requiemunofficial.common.entity.MorticianEntity;
import ua.meekeyn.requiemunofficial.common.init.ModEnchantments;
import ua.meekeyn.requiemunofficial.common.init.ModEffects;
import ua.meekeyn.requiemunofficial.common.possession.HumanityKillManager;
import ua.meekeyn.requiemunofficial.common.possession.PossessionManager;
import ua.meekeyn.requiemunofficial.common.possession.EntityMobilityManager;
import ua.meekeyn.requiemunofficial.common.possession.PossessedItemManager;
import ua.meekeyn.requiemunofficial.common.record.BodyRecordData;
import ua.meekeyn.requiemunofficial.common.record.ObeliskRecordData;
import ua.meekeyn.requiemunofficial.common.remnant.PlayerRiftTracker;
import ua.meekeyn.requiemunofficial.common.ritual.RiftTeleportManager;
import ua.meekeyn.requiemunofficial.common.ritual.RitualType;
import ua.meekeyn.requiemunofficial.common.ritual.DeathResurrectionManager;
import ua.meekeyn.requiemunofficial.common.ritual.TotemResurrectionManager;
import ua.meekeyn.requiemunofficial.common.soul.PlayerSoul;
import ua.meekeyn.requiemunofficial.common.soul.EntitySoulManager;
import ua.meekeyn.requiemunofficial.common.soul.SoulManager;
import ua.meekeyn.requiemunofficial.common.soul.SoulState;
import ua.meekeyn.requiemunofficial.common.soul.SoulKind;
import ua.meekeyn.requiemunofficial.common.soul.SoulLifecycleManager;
import ua.meekeyn.requiemunofficial.common.soul.DeathSuspensionManager;
import ua.meekeyn.requiemunofficial.common.soul.VagrantRules;
import ua.meekeyn.requiemunofficial.common.soul.VagrantEvents;
import ua.meekeyn.requiemunofficial.common.util.ModIds;
import ua.meekeyn.requiemunofficial.common.util.ModTags;
import ua.meekeyn.requiemunofficial.mixin.EntityAccessor;

import java.util.List;
import java.util.UUID;
import com.google.gson.JsonParser;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.item.trading.MerchantOffers;

@GameTestHolder(RequiemUnofficial.MODID)
@PrefixGameTestTemplate(false)
public final class CoreDataGameTests {
    private static final List<ResourceLocation> REQUIRED_RECIPES = List.of(
            ModIds.id("tachylite/tachylite"),
            ModIds.id("tachylite/runestone"),
            ModIds.id("tachylite/rift"),
            ModIds.id("tachylite/attrition"),
            ModIds.id("tachylite/emancipation"),
            ModIds.id("tachylite/penance"),
            ModIds.id("tachylite/reclamation"),
            ModIds.id("tachylite/chiseled"),
            ModIds.id("tachylite/chiseled_pillar"),
            ModIds.id("tachylite/chiseled_slab"),
            ModIds.id("tachylite/chiseled_stairs"),
            ModIds.id("tachylite/stonecutting_chiseled"),
            ModIds.id("tachylite/stonecutting_chiseled_pillar"),
            ModIds.id("tachylite/stonecutting_chiseled_slab"),
            ModIds.id("tachylite/stonecutting_chiseled_stairs"),
            ModIds.id("tachylite/stonecutting_scraped")
    );
    private static final List<ResourceLocation> REQUIRED_REQUIEM_ADVANCEMENTS = List.of(
            ModIds.id("requiem/root"),
            ModIds.id("requiem/first_possession"),
            ModIds.id("requiem/classic_look"),
            ModIds.id("requiem/good_as_new"),
            ModIds.id("requiem/i_hate_sand"),
            ModIds.id("requiem/kung_fu_panda"),
            ModIds.id("requiem/makeshift_makeover"),
            ModIds.id("requiem/piglin_seppukku")
    );
    private static final List<ResourceLocation> REQUIRED_REQUIEM_CRITERIA = List.of(
            ModIds.id("made_remnant_choice"),
            ModIds.id("player_possessed_entity"),
            ModIds.id("entity_died_after_possession"),
            ModIds.id("possessed_hit_entity"),
            ModIds.id("transformed_possessed_entity"),
            ModIds.id("player_resurrected_as_entity")
    );

    private CoreDataGameTests() {
    }

    @GameTest(template = "empty")
    public static void morticianOffersOriginalSoulTrades(GameTestHelper helper) {
        MorticianEntity mortician = ModEntities.MORTICIAN.get().create(helper.getLevel());
        helper.assertTrue(mortician != null, "Mortician entity must be creatable");

        MerchantOffers offers = mortician.getOffers();
        helper.assertTrue(offers.size() == 7, "Mortician must expose beta.16's seven supported trades");
        helper.assertTrue(
                offers.stream().filter(offer ->
                        offer.getResult().is(ModItems.SEALED_REMNANT_VESSEL.get())).count() == 1,
                "Mortician must expose exactly one Liberation/Sealed Remnant Vessel trade"
        );
        helper.assertTrue(
                offers.stream().anyMatch(offer ->
                        offer.getResult().is(ModItems.SEALED_REMNANT_VESSEL.get())),
                "Mortician must sell a sealed remnant vessel"
        );
        helper.assertTrue(
                offers.stream().anyMatch(offer -> hasStoredHumanityLevel(offer.getResult(), 2)
                        && hasStoredHumanityLevel(offer.getBaseCostA(), 1)
                        && offer.getCostB().is(Items.GOLD_INGOT)
                        && offer.getCostB().getCount() == 20),
                "Mortician must upgrade Humanity I books into Humanity II books"
        );
        assertSoulTrade(helper, offers, "minecraft:villager",
                ModItems.ICHOR_VESSEL_EMANCIPATION.get());
        helper.assertTrue(
                offers.stream().filter(offer ->
                        offer.getResult().is(ModItems.ICHOR_VESSEL_EMANCIPATION.get())).count() == 1,
                "Mortician must expose exactly one Emancipation Ichor Vessel trade"
        );
        assertSoulTrade(helper, offers, "minecraft:axolotl",
                ModItems.ICHOR_VESSEL_RECLAMATION.get());
        assertSoulTrade(helper, offers, "minecraft:ghast",
                ModItems.ICHOR_VESSEL_ATTRITION.get());
        assertSoulTrade(helper, offers, "minecraft:pillager",
                ModItems.ICHOR_VESSEL_PENANCE.get());
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void morticianLiberationVesselTradeDoesNotDuplicate(GameTestHelper helper) {
        MorticianEntity mortician = ModEntities.MORTICIAN.get().create(helper.getLevel());
        helper.assertTrue(mortician != null, "Mortician entity must be creatable");

        mortician.getOffers();
        mortician.getOffers();
        long count = mortician.getOffers().stream()
                .filter(offer -> offer.getResult().is(ModItems.ICHOR_VESSEL_EMANCIPATION.get()))
                .count();
        helper.assertTrue(count == 1, "Repeated offer initialization must keep one Emancipation Ichor Vessel trade");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void morticianOffersDoNotGrowAcrossOpenAndLoad(GameTestHelper helper) {
        MorticianEntity mortician = ModEntities.MORTICIAN.get().create(helper.getLevel());
        helper.assertTrue(mortician != null, "Mortician entity must be creatable");

        MerchantOffers first = mortician.getOffers();
        int firstCount = first.size();
        mortician.mobInteract(new net.minecraft.server.level.ServerPlayer(
                helper.getLevel().getServer(),
                helper.getLevel(),
                new com.mojang.authlib.GameProfile(UUID.randomUUID(), "mortician-open-test")
        ), net.minecraft.world.InteractionHand.MAIN_HAND);
        mortician.mobInteract(new net.minecraft.server.level.ServerPlayer(
                helper.getLevel().getServer(),
                helper.getLevel(),
                new com.mojang.authlib.GameProfile(UUID.randomUUID(), "mortician-open-test-2")
        ), net.minecraft.world.InteractionHand.MAIN_HAND);

        CompoundTag saved = mortician.saveWithoutId(new CompoundTag());
        MorticianEntity loaded = ModEntities.MORTICIAN.get().create(helper.getLevel());
        helper.assertTrue(loaded != null, "Mortician entity must load");
        loaded.load(saved);

        helper.assertTrue(firstCount == 7, "Mortician should start with the beta.16 canonical trade count");
        helper.assertTrue(mortician.getOffers().size() == 7,
                "Repeated interactions must not append duplicate Mortician trades");
        helper.assertTrue(loaded.getOffers().size() == 7,
                "Saved and loaded Mortician offers must normalize to the canonical trade count");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void morticianProjectionVisualStatePersists(GameTestHelper helper) {
        MorticianEntity original = ModEntities.MORTICIAN.get().create(helper.getLevel());
        helper.assertTrue(original != null, "Mortician entity must be creatable");
        original.setObeliskProjection(true);
        original.setFadingTicks(37);

        CompoundTag tag = original.saveWithoutId(new CompoundTag());
        MorticianEntity loaded = ModEntities.MORTICIAN.get().create(helper.getLevel());
        helper.assertTrue(loaded != null, "Mortician entity must load");
        loaded.load(tag);

        helper.assertTrue(loaded.isObeliskProjection(), "Obelisk morticians should keep projection visuals");
        helper.assertTrue(loaded.getFadingTicks() == 37, "Mortician fading state should persist");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void morticianProjectionLinkPersists(GameTestHelper helper) {
        MorticianEntity original = ModEntities.MORTICIAN.get().create(helper.getLevel());
        helper.assertTrue(original != null, "Mortician entity must be creatable");
        BlockPos home = helper.absolutePos(new BlockPos(2, 2, 2));
        original.linkToObelisk(helper.getLevel(), home);

        CompoundTag tag = original.saveWithoutId(new CompoundTag());
        helper.assertTrue(tag.getBoolean("obelisk_projection"), "Projection flag should use original-style NBT");
        helper.assertTrue(tag.getInt("linked_obelisk_x") == home.getX(), "Linked obelisk X should persist");
        helper.assertTrue(tag.getInt("linked_obelisk_y") == home.getY(), "Linked obelisk Y should persist");
        helper.assertTrue(tag.getInt("linked_obelisk_z") == home.getZ(), "Linked obelisk Z should persist");

        MorticianEntity loaded = ModEntities.MORTICIAN.get().create(helper.getLevel());
        helper.assertTrue(loaded != null, "Mortician entity must load");
        loaded.load(tag);
        helper.assertTrue(loaded.isObeliskProjection(), "Loaded linked mortician should remain projection");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void morticianCanStealSoulsLikeOriginal(GameTestHelper helper) {
        double oldChance = ua.meekeyn.requiemunofficial.common.config.RequiemConfig.SOUL_CAPTURE_CHANCE.get();
        ua.meekeyn.requiemunofficial.common.config.RequiemConfig.SOUL_CAPTURE_CHANCE.set(1.0D);

        MorticianEntity mortician = ModEntities.MORTICIAN.get().create(helper.getLevel());
        helper.assertTrue(mortician != null, "Mortician entity must be creatable");
        net.minecraft.world.entity.animal.Chicken chicken =
                new net.minecraft.world.entity.animal.Chicken(EntityType.CHICKEN, helper.getLevel());
        chicken.setHealth(1.0F);
        helper.getLevel().addFreshEntity(chicken);

        helper.assertTrue(EntitySoulManager.hasSoul(chicken), "Fresh chicken should have a soul");
        helper.assertTrue(
                SoulVesselItem.tryStealSoul(mortician, chicken),
                "Mortician should be able to steal a weak mob soul"
        );
        helper.assertTrue(!EntitySoulManager.hasSoul(chicken), "Stolen soul should leave the body soulless");
        helper.assertTrue(chicken.isNoAi(), "Soul-stripped body should become inert");

        ua.meekeyn.requiemunofficial.common.config.RequiemConfig.SOUL_CAPTURE_CHANCE.set(oldChance);
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void morticianCapturedSoulStoresBodyRecordIdentity(GameTestHelper helper) {
        double oldChance = ua.meekeyn.requiemunofficial.common.config.RequiemConfig.SOUL_CAPTURE_CHANCE.get();
        ua.meekeyn.requiemunofficial.common.config.RequiemConfig.SOUL_CAPTURE_CHANCE.set(1.0D);

        MorticianEntity mortician = ModEntities.MORTICIAN.get().create(helper.getLevel());
        helper.assertTrue(mortician != null, "Mortician entity must be creatable");
        net.minecraft.world.entity.animal.Cow cow =
                new net.minecraft.world.entity.animal.Cow(EntityType.COW, helper.getLevel());
        cow.moveTo(helper.absolutePos(new BlockPos(1, 2, 1)), 0.0F, 0.0F);
        helper.getLevel().addFreshEntity(cow);

        UUID recordId = SoulVesselItem.tryStealSoulToRecord(mortician, cow);
        helper.assertTrue(recordId != null, "Mortician soul steal should create a body record");
        mortician.addCapturedSoul(recordId);
        helper.assertTrue(mortician.hasCapturedSoul(recordId), "Mortician should keep captured body record identity");
        helper.assertTrue(recordId.equals(BodyRecordData.getEntityRecordId(cow)),
                "Captured soul record should be the stripped body's own record");
        helper.assertTrue(!EntitySoulManager.hasSoul(cow), "Captured body should be soulless");

        ua.meekeyn.requiemunofficial.common.config.RequiemConfig.SOUL_CAPTURE_CHANCE.set(oldChance);
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void morticianDeathReleasesCapturedBodyRecordSoul(GameTestHelper helper) {
        MorticianEntity mortician = ModEntities.MORTICIAN.get().create(helper.getLevel());
        helper.assertTrue(mortician != null, "Mortician entity must be creatable");
        mortician.moveTo(helper.absolutePos(new BlockPos(1, 2, 1)), 0.0F, 0.0F);
        helper.getLevel().addFreshEntity(mortician);

        net.minecraft.world.entity.animal.Cow cow =
                new net.minecraft.world.entity.animal.Cow(EntityType.COW, helper.getLevel());
        cow.moveTo(helper.absolutePos(new BlockPos(3, 2, 1)), 0.0F, 0.0F);
        helper.getLevel().addFreshEntity(cow);
        BodyRecordData.BodyRecord record = BodyRecordData.get(helper.getLevel()).getOrCreateRecord(cow);
        EntitySoulManager.removeSoul(cow);
        mortician.addCapturedSoul(record.recordId());

        mortician.hurt(helper.getLevel().damageSources().genericKill(), Float.MAX_VALUE);

        List<ua.meekeyn.requiemunofficial.common.entity.ReleasedSoulEntity> released =
                helper.getLevel().getEntitiesOfClass(
                        ua.meekeyn.requiemunofficial.common.entity.ReleasedSoulEntity.class,
                        new net.minecraft.world.phys.AABB(mortician.blockPosition()).inflate(3.0D)
                );
        helper.assertTrue(released.size() == 1, "Dying Mortician should release one captured soul");
        helper.assertTrue("minecraft:cow".equals(released.get(0).getSoulType()),
                "Released captured soul should retain captured body type");
        helper.assertTrue(BodyRecordData.get(helper.getLevel())
                        .getRecord(record.recordId())
                        .map(BodyRecordData.BodyRecord::activeReleasedSoul)
                        .orElse(false),
                "Released captured record should be marked active to prevent duplicates");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void morticianCapturedSoulReleasePreventsDuplicates(GameTestHelper helper) {
        MorticianEntity mortician = ModEntities.MORTICIAN.get().create(helper.getLevel());
        helper.assertTrue(mortician != null, "Mortician entity must be creatable");
        mortician.moveTo(helper.absolutePos(new BlockPos(1, 2, 1)), 0.0F, 0.0F);
        helper.getLevel().addFreshEntity(mortician);

        net.minecraft.world.entity.animal.Cow cow =
                new net.minecraft.world.entity.animal.Cow(EntityType.COW, helper.getLevel());
        helper.getLevel().addFreshEntity(cow);
        EntitySoulManager.removeSoul(cow);
        UUID recordId = BodyRecordData.getEntityRecordId(cow);
        mortician.addCapturedSoul(recordId);
        mortician.addCapturedSoul(recordId);

        helper.assertTrue(mortician.releaseCapturedSoul(recordId), "First release should spawn a soul");
        helper.assertTrue(!mortician.releaseCapturedSoul(recordId), "Second release should be rejected");
        long released = helper.getLevel().getEntitiesOfClass(
                ua.meekeyn.requiemunofficial.common.entity.ReleasedSoulEntity.class,
                new net.minecraft.world.phys.AABB(mortician.blockPosition()).inflate(4.0D)
        ).stream().filter(soul -> "minecraft:cow".equals(soul.getSoulType())).count();
        helper.assertTrue(released == 1, "Only one released soul should exist for one captured record");
        helper.assertTrue(mortician.getCapturedSoulCount() == 0,
                "Released captured records should be removed from Mortician storage");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void morticianCapturedSoulReleaseDropsInvalidRecord(GameTestHelper helper) {
        MorticianEntity mortician = ModEntities.MORTICIAN.get().create(helper.getLevel());
        helper.assertTrue(mortician != null, "Mortician entity must be creatable");
        UUID missing = UUID.randomUUID();
        mortician.addCapturedSoul(missing);

        helper.assertTrue(!mortician.releaseCapturedSoul(missing), "Missing body record should not release a soul");
        helper.assertTrue(mortician.getCapturedSoulCount() == 0,
                "Missing captured body record should be removed from Mortician storage");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void morticianAngerPersistsAttackingPlayer(GameTestHelper helper) {
        MorticianEntity mortician = ModEntities.MORTICIAN.get().create(helper.getLevel());
        helper.assertTrue(mortician != null, "Mortician entity must be creatable");
        net.minecraft.server.level.ServerPlayer player = new net.minecraft.server.level.ServerPlayer(
                helper.getLevel().getServer(),
                helper.getLevel(),
                new com.mojang.authlib.GameProfile(UUID.randomUUID(), "mortician-anger-test")
        );

        mortician.hurt(helper.getLevel().damageSources().playerAttack(player), 1.0F);
        CompoundTag saved = mortician.saveWithoutId(new CompoundTag());

        helper.assertTrue(saved.hasUUID("AngryAt"), "Mortician anger target should persist");
        helper.assertTrue(player.getUUID().equals(saved.getUUID("AngryAt")),
                "Mortician should remember the attacking player");
        helper.assertTrue(saved.getInt("AngerTime") > 0, "Mortician anger time should persist");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void morticianRelinksNearbyValidObelisk(GameTestHelper helper) {
        BlockPos validHome = helper.absolutePos(new BlockPos(4, 2, 4));
        createPoweredRift(helper, validHome);

        MorticianEntity mortician = ModEntities.MORTICIAN.get().create(helper.getLevel());
        helper.assertTrue(mortician != null, "Mortician entity must be creatable");
        mortician.moveTo(helper.absolutePos(new BlockPos(5, 2, 4)), 0.0F, 0.0F);
        mortician.linkToObelisk(helper.getLevel(), helper.absolutePos(new BlockPos(1, 2, 1)));
        mortician.aiStep();

        helper.assertTrue(validHome.equals(mortician.getHomePos()),
                "Mortician should relink to the nearest valid rift obelisk within range");
        helper.assertTrue(mortician.getFadingTicks() == 0, "Relinked Mortician should stop fading");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void obeliskDescriptorCreationAndSerialization(GameTestHelper helper) {
        BlockPos pos = helper.absolutePos(new BlockPos(2, 2, 2));
        RunestoneBlockEntity runestone = createPoweredRift(helper, pos);

        ObeliskRecordData.ObeliskRecord record = runestone.getOrCreateObeliskRecord();
        helper.assertTrue(record != null, "Powered rift should create a descriptor record");
        helper.assertTrue(record.pos().equals(pos), "Descriptor should store obelisk origin");
        helper.assertTrue(record.dimension().equals(helper.getLevel().dimension().location()),
                "Descriptor should store dimension");

        CompoundTag saved = ObeliskRecordData.get(helper.getLevel()).save(new CompoundTag());
        ObeliskRecordData loaded = ObeliskRecordData.load(saved);
        helper.assertTrue(loaded.getRecord(record.recordId()).isPresent(),
                "Descriptor record should survive SavedData serialization");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void obeliskDescriptorDuplicatePrevention(GameTestHelper helper) {
        BlockPos pos = helper.absolutePos(new BlockPos(2, 2, 2));
        RunestoneBlockEntity runestone = createPoweredRift(helper, pos);
        ObeliskRecordData.ObeliskRecord first = runestone.getOrCreateObeliskRecord();
        ObeliskRecordData.ObeliskRecord second = runestone.getOrCreateObeliskRecord();

        helper.assertTrue(first.recordId().equals(second.recordId()),
                "Repeated descriptor requests for one obelisk should return the same record");
        long activeAtPos = ObeliskRecordData.get(helper.getLevel()).records().stream()
                .filter(record -> !record.invalid())
                .filter(record -> record.pos().equals(pos))
                .count();
        helper.assertTrue(activeAtPos == 1, "Only one active descriptor should exist per obelisk position");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void obeliskDescriptorInvalidation(GameTestHelper helper) {
        BlockPos pos = helper.absolutePos(new BlockPos(2, 2, 2));
        RunestoneBlockEntity runestone = createPoweredRift(helper, pos);
        UUID recordId = runestone.getOrCreateObeliskRecord().recordId();

        runestone.clearStructureData();

        helper.assertTrue(ObeliskRecordData.get(helper.getLevel()).getRecord(recordId).isEmpty(),
                "Clearing a rift structure should invalidate its descriptor");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void knownRiftDestinationsFilterInvalidRecords(GameTestHelper helper) {
        BlockPos pos = helper.absolutePos(new BlockPos(2, 2, 2));
        RunestoneBlockEntity runestone = createPoweredRift(helper, pos);
        ObeliskRecordData.ObeliskRecord record = runestone.getOrCreateObeliskRecord();
        net.minecraft.server.level.ServerPlayer player = new net.minecraft.server.level.ServerPlayer(
                helper.getLevel().getServer(),
                helper.getLevel(),
                new com.mojang.authlib.GameProfile(UUID.randomUUID(), "known-rift-test")
        );

        helper.assertTrue(PlayerRiftTracker.addKnownRift(player, record),
                "Player should learn a witnessed rift record");
        helper.assertTrue(PlayerRiftTracker.fetchKnownObelisks(player).size() == 1,
                "Known rift list should include valid descriptors");
        ObeliskRecordData.get(helper.getLevel()).invalidate(record.recordId());
        helper.assertTrue(PlayerRiftTracker.fetchKnownObelisks(player).isEmpty(),
                "Known rift list should drop invalid descriptors");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void selectedRiftDestinationResolvesChosenObelisk(GameTestHelper helper) {
        BlockPos sourcePos = helper.absolutePos(new BlockPos(1, 2, 1));
        BlockPos targetPos = helper.absolutePos(new BlockPos(6, 2, 1));
        RunestoneBlockEntity source = createPoweredRift(helper, sourcePos);
        RunestoneBlockEntity target = createPoweredRift(helper, targetPos);
        ObeliskRecordData.ObeliskRecord targetRecord = target.getOrCreateObeliskRecord();

        net.minecraft.server.level.ServerPlayer player = new net.minecraft.server.level.ServerPlayer(
                helper.getLevel().getServer(),
                helper.getLevel(),
                new com.mojang.authlib.GameProfile(UUID.randomUUID(), "selected-rift-test")
        );
        SoulManager.setState(player, SoulState.VAGRANT);
        PlayerRiftTracker.addKnownRift(player, source.getOrCreateObeliskRecord());
        PlayerRiftTracker.addKnownRift(player, targetRecord);

        helper.assertTrue(RiftTeleportManager.resolveTeleportTargetForTest(player, sourcePos, targetRecord.recordId())
                        .filter(targetPos::equals)
                        .isPresent(),
                "Selected known destination should resolve to the chosen obelisk record");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void riftAttritionLengthMatchesOriginalFormula(GameTestHelper helper) {
        int close = RiftTeleportManager.getAttritionLength(new BlockPos(0, 64, 0), new BlockPos(1, 64, 0));
        int thousand = RiftTeleportManager.getAttritionLength(new BlockPos(0, 64, 0), new BlockPos(1000, 64, 0));

        helper.assertTrue(close == 20 * 60 * 15,
                "Very close rift travel should use the original 15 minute base penalty");
        helper.assertTrue(thousand >= 20 * 60 * 20 - 1 && thousand <= 20 * 60 * 20,
                "1000 blocks should add the original 5 minute distance penalty with Java truncation");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void morticianStoresDescriptorLinkWhenLinked(GameTestHelper helper) {
        BlockPos pos = helper.absolutePos(new BlockPos(3, 2, 3));
        createPoweredRift(helper, pos);
        MorticianEntity mortician = ModEntities.MORTICIAN.get().create(helper.getLevel());
        helper.assertTrue(mortician != null, "Mortician entity must be creatable");

        mortician.linkToObelisk(helper.getLevel(), pos);
        CompoundTag tag = mortician.saveWithoutId(new CompoundTag());

        helper.assertTrue(tag.hasUUID("linked_obelisk_record"),
                "Mortician should persist descriptor identity for its linked obelisk");
        helper.succeed();
    }

    private static void assertSoulTrade(GameTestHelper helper, MerchantOffers offers,
                                        String soulType, net.minecraft.world.item.Item result) {
        boolean found = false;
        for (MerchantOffer offer : offers) {
            if (offer.getResult().is(result)
                    && SoulVesselItem.hasSoulData(offer.getBaseCostA())
                    && soulType.equals(SoulVesselItem.getSoulType(offer.getBaseCostA()))) {
                found = true;
                break;
            }
        }
        helper.assertTrue(found, "Missing Mortician trade for soul type " + soulType);
    }

    private static boolean hasStoredHumanityLevel(ItemStack stack, int expectedLevel) {
        String humanityId = BuiltInRegistries.ENCHANTMENT.getKey(ModEnchantments.HUMANITY.get()).toString();
        net.minecraft.nbt.ListTag enchantments = net.minecraft.world.item.EnchantedBookItem.getEnchantments(stack);
        for (int i = 0; i < enchantments.size(); i++) {
            CompoundTag enchantment = enchantments.getCompound(i);
            if (humanityId.equals(enchantment.getString("id"))
                    && enchantment.getInt("lvl") == expectedLevel) {
                return true;
            }
        }
        return false;
    }

    @GameTest(template = "empty")
    public static void requiredRecipesAreLoaded(GameTestHelper helper) {
        helper.assertTrue(
                helper.getLevel().getRecipeManager().getRecipes().size() >= REQUIRED_RECIPES.size(),
                "Expected at least " + REQUIRED_RECIPES.size() + " loaded recipes"
        );
        for (ResourceLocation recipeId : REQUIRED_RECIPES) {
            helper.assertTrue(
                    helper.getLevel().getRecipeManager().byKey(recipeId).isPresent(),
                    "Missing recipe " + recipeId
            );
        }
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void requiemAdvancementsAndCriteriaAreLoaded(GameTestHelper helper) {
        for (ResourceLocation criterionId : REQUIRED_REQUIEM_CRITERIA) {
            helper.assertTrue(
                    net.minecraft.advancements.CriteriaTriggers.getCriterion(criterionId) != null,
                    "Missing criterion " + criterionId
            );
        }
        for (ResourceLocation advancementId : REQUIRED_REQUIEM_ADVANCEMENTS) {
            helper.assertTrue(
                    helper.getLevel().getServer().getAdvancements().getAdvancement(advancementId) != null,
                    "Missing advancement " + advancementId
            );
        }
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void decorativeTachyliteBlocksAreRegistered(GameTestHelper helper) {
        List<ResourceLocation> blockIds = List.of(
                ModIds.id("tachylite/chiseled"),
                ModIds.id("tachylite/chiseled_pillar"),
                ModIds.id("tachylite/chiseled_slab"),
                ModIds.id("tachylite/chiseled_stairs"),
                ModIds.id("tachylite/scraped"),
                ModIds.id("tachylite/derelict")
        );
        for (ResourceLocation blockId : blockIds) {
            helper.assertTrue(BuiltInRegistries.BLOCK.containsKey(blockId), "Missing block " + blockId);
            helper.assertTrue(BuiltInRegistries.ITEM.containsKey(blockId), "Missing block item " + blockId);
        }
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void soulVesselDataRoundTrips(GameTestHelper helper) {
        ItemStack vessel = new ItemStack(ModItems.FILLED_SOUL_VESSEL.get());
        SoulVesselItem.setSoulData(vessel, "test_soul", 7);
        vessel.getOrCreateTag().remove("SoulKind");
        vessel.getOrCreateTag().remove("SoulEnergy");

        ItemStack loaded = ItemStack.of(vessel.save(new CompoundTag()));
        helper.assertTrue(SoulVesselItem.hasSoulData(loaded), "Soul data should be present");
        helper.assertTrue("test_soul".equals(SoulVesselItem.getSoulType(loaded)), "Soul type should persist");
        helper.assertTrue(SoulVesselItem.getSoulAmount(loaded) == 7, "Soul amount should persist");
        helper.assertTrue(SoulVesselItem.getSoulEnergy(loaded) == 7, "Legacy soul energy should match amount");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void remnantVesselsHaveDistinctStateSemantics(GameTestHelper helper) {
        helper.assertTrue(
                ModItems.PURE_SOUL_VESSEL.get() instanceof RemnantVesselItem,
                "Pure soul vessel should use remnant state behavior"
        );
        helper.assertTrue(
                ModItems.SEALED_REMNANT_VESSEL.get() instanceof RemnantVesselItem,
                "Sealed remnant vessel should use remnant state behavior"
        );

        RemnantVesselItem pure = (RemnantVesselItem) ModItems.PURE_SOUL_VESSEL.get();
        RemnantVesselItem sealed = (RemnantVesselItem) ModItems.SEALED_REMNANT_VESSEL.get();
        helper.assertTrue(
                sealed.canApply(SoulState.MORTAL)
                        && !sealed.canApply(SoulState.REMNANT)
                        && !sealed.canApply(SoulState.VAGRANT),
                "Sealed vessel should only curse mortal souls"
        );
        helper.assertTrue(
                !pure.canApply(SoulState.MORTAL)
                        && pure.canApply(SoulState.REMNANT)
                        && pure.canApply(SoulState.VAGRANT),
                "Pure vessel should cure every non-mortal soul state"
        );
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void humanityBookCarriesHumanityEnchantment(GameTestHelper helper) {
        ItemStack book = HumanityEnchantedBookItem.create(2);
        String humanityId = BuiltInRegistries.ENCHANTMENT.getKey(ModEnchantments.HUMANITY.get()).toString();
        int level = 0;
        net.minecraft.nbt.ListTag enchantments = net.minecraft.world.item.EnchantedBookItem.getEnchantments(book);
        for (int i = 0; i < enchantments.size(); i++) {
            CompoundTag enchantment = enchantments.getCompound(i);
            if (humanityId.equals(enchantment.getString("id"))) {
                level = enchantment.getInt("lvl");
            }
        }
        helper.assertTrue(level == 2, "Humanity book should carry the registered Humanity enchantment");
        helper.assertTrue(ModEnchantments.HUMANITY.get().getMaxLevel() == 2,
                "Humanity enchantment should match the original max level");
        helper.assertTrue(ModEnchantments.HUMANITY.get().isTreasureOnly(),
                "Humanity enchantment should be treasure-only like the original");

        net.minecraft.world.entity.monster.Zombie host =
                new net.minecraft.world.entity.monster.Zombie(EntityType.ZOMBIE, helper.getLevel());
        ItemStack sword = new ItemStack(Items.IRON_SWORD);
        sword.enchant(ModEnchantments.HUMANITY.get(), 2);
        host.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, sword);
        helper.assertTrue(
                PossessedItemManager.getHumanityLevel(host) == 2,
                "Humanity level should be read from possessed host equipment"
        );
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void humanityOneSuppressesPossessedKillExperience(GameTestHelper helper) {
        helper.assertTrue(
                HumanityKillManager.adjustExperienceDropForTest(1, 7) == 0,
                "Humanity I should suppress XP drops like the original"
        );
        helper.assertTrue(
                HumanityKillManager.adjustExperienceDropForTest(2, 7) == 7,
                "Humanity II should allow XP drops"
        );
        helper.assertTrue(
                HumanityKillManager.adjustExperienceDropForTest(0, 7) == 7,
                "Non-humanity kills should keep normal XP drops"
        );
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void opusUseOnlyAwardsBookUse(GameTestHelper helper) {
        net.minecraft.server.level.ServerPlayer player = new net.minecraft.server.level.ServerPlayer(
                helper.getLevel().getServer(),
                helper.getLevel(),
                new com.mojang.authlib.GameProfile(UUID.randomUUID(), "opus-test")
        );
        ua.meekeyn.requiemunofficial.common.soul.SoulManager.setState(player, SoulState.MORTAL);
        ItemStack opus = new ItemStack(ModItems.OPUS_DAEMONIUM.get());
        player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, opus);
        int cost = ua.meekeyn.requiemunofficial.common.config.RequiemConfig.OPUS_DAEMONIUM_XP_LEVEL_COST.get();
        player.experienceLevel = cost;

        var result = opus.use(helper.getLevel(), player, net.minecraft.world.InteractionHand.MAIN_HAND);
        helper.assertTrue(result.getObject().is(ModItems.OPUS_DAEMONIUM.get()),
                "Opus use should keep the book item");
        helper.assertTrue(player.experienceLevel == cost,
                "Opus use should not spend conversion XP");
        helper.assertTrue(
                ua.meekeyn.requiemunofficial.common.soul.SoulManager.getState(player) == SoulState.MORTAL,
                "Opus itself should not bind the demon to a mortal soul"
        );
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void opusAndRemnantVesselsCountAsLecternBooks(GameTestHelper helper) {
        helper.assertTrue(
                ua.meekeyn.requiemunofficial.common.item.OpusLecternRules.isLecternReadable(
                        new ItemStack(ModItems.OPUS_DAEMONIUM.get())
                ),
                "Opus Daemonium should be readable from lecterns like the original"
        );
        helper.assertTrue(
                ua.meekeyn.requiemunofficial.common.item.OpusLecternRules.isLecternReadable(
                        new ItemStack(ModItems.PURE_SOUL_VESSEL.get())
                ),
                "Pure soul vessel should be readable from lecterns like the original conversion book"
        );
        helper.assertTrue(
                ua.meekeyn.requiemunofficial.common.item.OpusLecternRules.isLecternReadable(
                        new ItemStack(ModItems.SEALED_REMNANT_VESSEL.get())
                ),
                "Sealed remnant vessel should be readable from lecterns like the original conversion book"
        );
        helper.assertTrue(
                !ua.meekeyn.requiemunofficial.common.item.OpusLecternRules.isLecternReadable(
                        new ItemStack(Items.STICK)
                ),
                "Ordinary items must not become lectern books"
        );
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void remnantVesselConvertsWithoutOpusXpCost(GameTestHelper helper) {
        net.minecraft.server.level.ServerPlayer player = new net.minecraft.server.level.ServerPlayer(
                helper.getLevel().getServer(),
                helper.getLevel(),
                new com.mojang.authlib.GameProfile(UUID.randomUUID(), "remnant-vessel-cost-test")
        );
        ua.meekeyn.requiemunofficial.common.soul.SoulManager.setState(player, SoulState.MORTAL);
        ItemStack sealed = new ItemStack(ModItems.SEALED_REMNANT_VESSEL.get());
        player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, sealed);
        player.experienceLevel = 0;
        var converted = sealed.use(helper.getLevel(), player, net.minecraft.world.InteractionHand.MAIN_HAND);
        helper.assertTrue(converted.getObject().is(ModItems.EMPTY_SOUL_VESSEL.get()),
                "Successful remnant conversion should return an empty vessel");
        helper.assertTrue(player.experienceLevel == 0,
                "Successful remnant conversion should not spend Opus XP levels");
        helper.assertTrue(
                ua.meekeyn.requiemunofficial.common.soul.SoulManager.getState(player) == SoulState.REMNANT,
                "Successful sealed vessel use should make the player a remnant"
        );
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void pureVesselDoesNotBypassPossessionCureLifecycle(GameTestHelper helper) {
        net.minecraft.server.level.ServerPlayer player = new net.minecraft.server.level.ServerPlayer(
                helper.getLevel().getServer(),
                helper.getLevel(),
                new com.mojang.authlib.GameProfile(UUID.randomUUID(), "pure-vessel-possession-test")
        );
        net.minecraft.world.entity.monster.Zombie host =
                new net.minecraft.world.entity.monster.Zombie(EntityType.ZOMBIE, helper.getLevel());
        host.setPos(1.0D, 2.0D, 1.0D);
        helper.getLevel().addFreshEntity(host);

        SoulManager.setState(player, SoulState.VAGRANT);
        helper.assertTrue(PossessionManager.tryPossess(player, host),
                "Vagrant player should possess the test zombie");
        helper.assertTrue(player.getVehicle() == null,
                "Possession should not depend on riding the host");

        ItemStack pure = new ItemStack(ModItems.PURE_SOUL_VESSEL.get());
        player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, pure);
        player.experienceLevel = ua.meekeyn.requiemunofficial.common.config.RequiemConfig
                .OPUS_DAEMONIUM_XP_LEVEL_COST.get();

        var result = pure.use(helper.getLevel(), player, net.minecraft.world.InteractionHand.MAIN_HAND);
        helper.assertTrue(result.getObject().is(ModItems.PURE_SOUL_VESSEL.get()),
                "Pure vessel should remain unused while possession cure has not started");
        helper.assertTrue(SoulManager.getState(player) == SoulState.VAGRANT,
                "Pure vessel should not restore mortal state by bypassing cure lifecycle");
        helper.assertTrue(host.getUUID().equals(SoulManager.getPossessedEntityId(player)),
                "Pure vessel should not clear active possession before cure lifecycle starts");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void emptySoulVesselStartsTimedCapture(GameTestHelper helper) {
        net.minecraft.server.level.ServerPlayer player = new net.minecraft.server.level.ServerPlayer(
                helper.getLevel().getServer(),
                helper.getLevel(),
                new com.mojang.authlib.GameProfile(UUID.randomUUID(), "timed-capture-test")
        );
        net.minecraft.world.entity.animal.Cow cow =
                new net.minecraft.world.entity.animal.Cow(EntityType.COW, helper.getLevel());
        cow.moveTo(helper.absolutePos(new BlockPos(1, 2, 1)), 0.0F, 0.0F);
        helper.getLevel().addFreshEntity(cow);

        ItemStack empty = new ItemStack(ModItems.EMPTY_SOUL_VESSEL.get());
        var result = SoulVesselItem.beginCaptureForTest(
                empty,
                player,
                cow,
                net.minecraft.world.InteractionHand.MAIN_HAND
        );
        helper.assertTrue(result.consumesAction(), "Soul capture should enter active use");
        helper.assertTrue(empty.getTagElement("RequiemSoulCapture") != null,
                "Soul capture should store active capture data on the vessel");
        helper.assertTrue(empty.getUseDuration() >= 96,
                "Soul capture should require a timed channel instead of instant capture");
        helper.assertTrue(EntitySoulManager.hasSoul(cow),
                "Starting capture should not remove the target soul immediately");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void creativeSoulVesselUsesTenTickCapture(GameTestHelper helper) {
        net.minecraft.server.level.ServerPlayer player = new net.minecraft.server.level.ServerPlayer(
                helper.getLevel().getServer(),
                helper.getLevel(),
                new com.mojang.authlib.GameProfile(UUID.randomUUID(), "creative-vessel-time")
        );
        net.minecraft.world.entity.animal.Cow cow =
                new net.minecraft.world.entity.animal.Cow(EntityType.COW, helper.getLevel());
        helper.getLevel().addFreshEntity(cow);

        ItemStack creative = new ItemStack(ModItems.CREATIVE_SOUL_VESSEL.get());
        var result = SoulVesselItem.beginCaptureForTest(
                creative,
                player,
                cow,
                net.minecraft.world.InteractionHand.MAIN_HAND
        );
        helper.assertTrue(result.consumesAction(), "Creative vessel should start soul capture");
        helper.assertTrue(creative.getUseDuration() == SoulVesselItem.CREATIVE_CAPTURE_TIME_TICKS,
                "Creative vessel capture should last exactly 10 ticks");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void offhandCreativeVesselAcceleratesMainHandEmptyVessel(GameTestHelper helper) {
        net.minecraft.server.level.ServerPlayer player = new net.minecraft.server.level.ServerPlayer(
                helper.getLevel().getServer(),
                helper.getLevel(),
                new com.mojang.authlib.GameProfile(UUID.randomUUID(), "dual-vessel-time")
        );
        net.minecraft.world.entity.animal.Cow cow =
                new net.minecraft.world.entity.animal.Cow(EntityType.COW, helper.getLevel());
        helper.getLevel().addFreshEntity(cow);

        ItemStack empty = new ItemStack(ModItems.EMPTY_SOUL_VESSEL.get());
        ItemStack creative = new ItemStack(ModItems.CREATIVE_SOUL_VESSEL.get());
        player.setItemInHand(net.minecraft.world.InteractionHand.OFF_HAND, creative);
        var result = SoulVesselItem.beginCaptureForTest(
                empty,
                player,
                cow,
                net.minecraft.world.InteractionHand.MAIN_HAND
        );

        helper.assertTrue(result.consumesAction(), "Main hand empty vessel should start soul capture");
        helper.assertTrue(empty.getUseDuration() == SoulVesselItem.CREATIVE_CAPTURE_TIME_TICKS,
                "Offhand creative vessel should accelerate the main hand vessel to 10 ticks");
        helper.assertTrue(player.getOffhandItem().is(ModItems.CREATIVE_SOUL_VESSEL.get()),
                "Offhand creative vessel should stay in the offhand");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void offhandCreativeVesselProducesOneMainHandResult(GameTestHelper helper) {
        net.minecraft.server.level.ServerPlayer player = new net.minecraft.server.level.ServerPlayer(
                helper.getLevel().getServer(), helper.getLevel(),
                new com.mojang.authlib.GameProfile(UUID.randomUUID(), "dual-vessel-result")
        );
        net.minecraft.world.entity.animal.Cow cow =
                new net.minecraft.world.entity.animal.Cow(EntityType.COW, helper.getLevel());
        helper.getLevel().addFreshEntity(cow);
        ItemStack empty = new ItemStack(ModItems.EMPTY_SOUL_VESSEL.get());
        ItemStack creative = new ItemStack(ModItems.CREATIVE_SOUL_VESSEL.get());
        player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, empty);
        player.setItemInHand(net.minecraft.world.InteractionHand.OFF_HAND, creative);

        SoulVesselItem.beginCaptureForTest(empty, player, cow, net.minecraft.world.InteractionHand.MAIN_HAND);
        ItemStack result = empty.finishUsingItem(helper.getLevel(), player);

        helper.assertTrue(result.is(ModItems.FILLED_SOUL_VESSEL.get()),
                "Accelerated main hand vessel should become filled");
        helper.assertTrue(player.getOffhandItem() == creative && creative.getTagElement("RequiemSoulCapture") == null,
                "Creative offhand vessel must remain untouched");
        helper.assertTrue(player.getInventory().countItem(ModItems.FILLED_SOUL_VESSEL.get()) == 0,
                "Capture must not add a second filled vessel to inventory");
        helper.assertTrue(!EntitySoulManager.hasSoul(cow), "Capture should remove the target soul once");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void creativeSoulVesselKeepsItsStackAndAddsOneResult(GameTestHelper helper) {
        net.minecraft.server.level.ServerPlayer player = new net.minecraft.server.level.ServerPlayer(
                helper.getLevel().getServer(), helper.getLevel(),
                new com.mojang.authlib.GameProfile(UUID.randomUUID(), "creative-vessel-result")
        );
        net.minecraft.world.entity.animal.Cow cow =
                new net.minecraft.world.entity.animal.Cow(EntityType.COW, helper.getLevel());
        helper.getLevel().addFreshEntity(cow);
        ItemStack creative = new ItemStack(ModItems.CREATIVE_SOUL_VESSEL.get());
        player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, creative);

        SoulVesselItem.beginCaptureForTest(creative, player, cow, net.minecraft.world.InteractionHand.MAIN_HAND);
        ItemStack result = creative.finishUsingItem(helper.getLevel(), player);

        helper.assertTrue(result == creative, "Creative vessel should stay in the used hand");
        helper.assertTrue(player.getInventory().countItem(ModItems.FILLED_SOUL_VESSEL.get()) == 1,
                "Creative vessel should produce exactly one filled vessel");
        helper.assertTrue(!EntitySoulManager.hasSoul(cow), "Capture should remove the target soul once");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void soulCaptureProvokesOnlyNeutralTarget(GameTestHelper helper) {
        net.minecraft.server.level.ServerPlayer player = new net.minecraft.server.level.ServerPlayer(
                helper.getLevel().getServer(), helper.getLevel(),
                new com.mojang.authlib.GameProfile(UUID.randomUUID(), "neutral-capture-test")
        );
        net.minecraft.world.entity.animal.Wolf wolf =
                new net.minecraft.world.entity.animal.Wolf(EntityType.WOLF, helper.getLevel());
        helper.getLevel().addFreshEntity(wolf);
        ItemStack vessel = new ItemStack(ModItems.EMPTY_SOUL_VESSEL.get());

        helper.assertTrue(wolf.getTarget() == null, "Wolf should begin neutral");
        SoulVesselItem.beginCaptureForTest(vessel, player, wolf,
                net.minecraft.world.InteractionHand.MAIN_HAND);
        helper.assertTrue(wolf.getTarget() == player,
                "Neutral target should retaliate against the soul extractor");
        helper.assertTrue(player.getUUID().equals(wolf.getPersistentAngerTarget()),
                "Neutral anger should keep the correct attacker identity");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void emptyVesselPassesOnSoullessPossessionTarget(GameTestHelper helper) {
        net.minecraft.server.level.ServerPlayer player = new net.minecraft.server.level.ServerPlayer(
                helper.getLevel().getServer(), helper.getLevel(),
                new com.mojang.authlib.GameProfile(UUID.randomUUID(), "vessel-pass-test")
        );
        net.minecraft.world.entity.animal.Cow cow =
                new net.minecraft.world.entity.animal.Cow(EntityType.COW, helper.getLevel());
        helper.getLevel().addFreshEntity(cow);
        EntitySoulManager.removeSoul(cow);
        ItemStack vessel = new ItemStack(ModItems.EMPTY_SOUL_VESSEL.get());

        var result = SoulVesselItem.beginCaptureForTest(vessel, player, cow,
                net.minecraft.world.InteractionHand.MAIN_HAND);
        helper.assertTrue(result == net.minecraft.world.InteractionResult.PASS,
                "Vessel should allow possession when the target has no capturable soul");
        helper.assertTrue(vessel.getTagElement("RequiemSoulCapture") == null,
                "Passing vessel must not start a capture");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void ichorVesselsCarveAndClearRunestones(GameTestHelper helper) {
        IchorVesselItem attrition = (IchorVesselItem) ModItems.ICHOR_VESSEL_ATTRITION.get();
        IchorVesselItem emancipation = (IchorVesselItem) ModItems.ICHOR_VESSEL_EMANCIPATION.get();
        IchorVesselItem penance = (IchorVesselItem) ModItems.ICHOR_VESSEL_PENANCE.get();
        IchorVesselItem reclamation = (IchorVesselItem) ModItems.ICHOR_VESSEL_RECLAMATION.get();
        helper.assertTrue(attrition.getEffect() == ua.meekeyn.requiemunofficial.common.init.ModEffects.ATTRITION.get(),
                "Attrition vessel should apply attrition");
        helper.assertTrue(emancipation.getEffect() == ua.meekeyn.requiemunofficial.common.init.ModEffects.EMANCIPATION.get(),
                "Emancipation vessel should apply emancipation");
        helper.assertTrue(penance.getEffect() == ua.meekeyn.requiemunofficial.common.init.ModEffects.PENANCE.get(),
                "Penance vessel should apply penance");
        helper.assertTrue(reclamation.getEffect() == ua.meekeyn.requiemunofficial.common.init.ModEffects.RECLAMATION.get(),
                "Reclamation vessel should apply reclamation");

        BlockPos pos = helper.absolutePos(new BlockPos(1, 1, 1));
        helper.getLevel().setBlock(pos, ModBlocks.TACHYLITE_RUNESTONE.get().defaultBlockState(), 3);
        net.minecraft.server.level.ServerPlayer player = new net.minecraft.server.level.ServerPlayer(
                helper.getLevel().getServer(),
                helper.getLevel(),
                new com.mojang.authlib.GameProfile(UUID.randomUUID(), "rune-vessel-test")
        );
        player.setItemInHand(
                net.minecraft.world.InteractionHand.MAIN_HAND,
                new ItemStack(ModItems.ICHOR_VESSEL_ATTRITION.get())
        );
        net.minecraft.world.phys.BlockHitResult hit = new net.minecraft.world.phys.BlockHitResult(
                net.minecraft.world.phys.Vec3.atCenterOf(pos),
                net.minecraft.core.Direction.UP,
                pos,
                false
        );

        helper.getLevel().getBlockState(pos).use(
                helper.getLevel(),
                player,
                net.minecraft.world.InteractionHand.MAIN_HAND,
                hit
        );
        helper.assertTrue(
                helper.getLevel().getBlockState(pos).is(ModBlocks.RUNIC_TACHYLITE_ATTRITION.get()),
                "Attrition vessel should carve an inert runestone"
        );
        helper.assertTrue(
                player.getMainHandItem().is(ModItems.EMPTY_SOUL_VESSEL.get()),
                "Carving should return an empty vessel"
        );

        helper.getLevel().getBlockState(pos).use(
                helper.getLevel(),
                player,
                net.minecraft.world.InteractionHand.MAIN_HAND,
                hit
        );
        helper.assertTrue(
                helper.getLevel().getBlockState(pos).is(ModBlocks.TACHYLITE_RUNESTONE.get()),
                "Empty vessel should clear a runic runestone"
        );
        helper.assertTrue(
                player.getMainHandItem().is(ModItems.ICHOR_VESSEL_ATTRITION.get()),
                "Clearing should recover the matching ichor vessel"
        );
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void reclamationReducesAttrition(GameTestHelper helper) {
        net.minecraft.world.entity.monster.Zombie target =
                new net.minecraft.world.entity.monster.Zombie(EntityType.ZOMBIE, helper.getLevel());
        target.addEffect(new net.minecraft.world.effect.MobEffectInstance(
                ua.meekeyn.requiemunofficial.common.init.ModEffects.ATTRITION.get(),
                200,
                2
        ));

        ua.meekeyn.requiemunofficial.common.init.ModEffects.RECLAMATION.get()
                .applyEffectTick(target, 1);

        net.minecraft.world.effect.MobEffectInstance attrition =
                target.getEffect(ua.meekeyn.requiemunofficial.common.init.ModEffects.ATTRITION.get());
        helper.assertTrue(attrition == null || attrition.getAmplifier() == 0,
                "Reclamation II should reduce Attrition III by two levels");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void penanceThreeBlocksPossessionStart(GameTestHelper helper) {
        helper.assertTrue(
                !ua.meekeyn.requiemunofficial.common.effect.PenanceMobEffect.blocksPossession(
                        new net.minecraft.world.effect.MobEffectInstance(
                                ua.meekeyn.requiemunofficial.common.init.ModEffects.PENANCE.get(),
                                200,
                                1
                        )
                ),
                "Penance II should not block possession attempts"
        );
        helper.assertTrue(
                ua.meekeyn.requiemunofficial.common.effect.PenanceMobEffect.blocksPossession(
                        new net.minecraft.world.effect.MobEffectInstance(
                                ua.meekeyn.requiemunofficial.common.init.ModEffects.PENANCE.get(),
                                200,
                                2
                        )
                ),
                "Penance III should block possession attempts"
        );
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void typedSoulVesselDataRoundTrips(GameTestHelper helper) {
        ItemStack vessel = new ItemStack(ModItems.FILLED_SOUL_VESSEL.get());
        SoulVesselItem.setSoulData(vessel, "minecraft:wither", SoulKind.AGGREGATE, 20);

        ItemStack loaded = ItemStack.of(vessel.save(new CompoundTag()));
        helper.assertTrue(SoulVesselItem.getSoulAmount(loaded) == SoulVesselItem.CAPACITY,
                "Soul amount should be limited by vessel capacity");
        helper.assertTrue("aggregate".equals(SoulVesselItem.getSoulKind(loaded)), "Soul kind should persist");
        helper.assertTrue(SoulVesselItem.getSoulEnergy(loaded) == SoulVesselItem.CAPACITY * 3,
                "Aggregate energy should use its multiplier");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void filledVesselReleasesTypedSoulAndReturnsEmptyVessel(GameTestHelper helper) {
        helper.assertTrue(
                ModItems.FILLED_SOUL_VESSEL.get() instanceof FilledSoulVesselItem,
                "Filled vessel should use release behavior"
        );
        net.minecraft.server.level.ServerPlayer player = new net.minecraft.server.level.ServerPlayer(
                helper.getLevel().getServer(),
                helper.getLevel(),
                new com.mojang.authlib.GameProfile(UUID.randomUUID(), "filled-vessel-test")
        );
        BlockPos playerPos = helper.absolutePos(new BlockPos(1, 2, 1));
        player.moveTo(playerPos.getX() + 0.5D, playerPos.getY(), playerPos.getZ() + 0.5D, 0.0F, 0.0F);
        ItemStack filled = new ItemStack(ModItems.FILLED_SOUL_VESSEL.get());
        SoulVesselItem.setSoulData(filled, "minecraft:zombie", SoulKind.UNDEAD, 3);
        player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, filled);

        var result = filled.use(
                helper.getLevel(),
                player,
                net.minecraft.world.InteractionHand.MAIN_HAND
        );
        helper.assertTrue(
                result.getObject().is(ModItems.EMPTY_SOUL_VESSEL.get()),
                "Using a filled vessel should return an empty vessel"
        );
        List<ua.meekeyn.requiemunofficial.common.entity.ReleasedSoulEntity> released =
                helper.getLevel().getEntitiesOfClass(
                        ua.meekeyn.requiemunofficial.common.entity.ReleasedSoulEntity.class,
                        new net.minecraft.world.phys.AABB(player.blockPosition()).inflate(3.0D)
                );
        helper.assertTrue(released.size() == 1, "Filled vessel should release one soul entity");
        helper.assertTrue(
                "minecraft:zombie".equals(released.get(0).getSoulType())
                        && released.get(0).getSoulAmount() == 3,
                "Released soul should retain vessel type and amount"
        );

        CompoundTag saved = new CompoundTag();
        released.get(0).save(saved);
        ua.meekeyn.requiemunofficial.common.entity.ReleasedSoulEntity loaded =
                new ua.meekeyn.requiemunofficial.common.entity.ReleasedSoulEntity(
                        ModEntities.RELEASED_SOUL.get(),
                        helper.getLevel()
                );
        loaded.load(saved);
        helper.assertTrue(
                "minecraft:zombie".equals(loaded.getSoulType()) && loaded.getSoulAmount() == 3,
                "Released soul data should persist"
        );
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void filledVesselSingleReleaseDoesNotDuplicateVessels(GameTestHelper helper) {
        net.minecraft.server.level.ServerPlayer player = new net.minecraft.server.level.ServerPlayer(
                helper.getLevel().getServer(),
                helper.getLevel(),
                new com.mojang.authlib.GameProfile(UUID.randomUUID(), "filled-vessel-single-release")
        );
        player.moveTo(helper.absolutePos(new BlockPos(1, 2, 1)), 0.0F, 0.0F);
        net.minecraft.world.entity.monster.Zombie host =
                new net.minecraft.world.entity.monster.Zombie(EntityType.ZOMBIE, helper.getLevel());
        host.moveTo(helper.absolutePos(new BlockPos(3, 2, 1)), 0.0F, 0.0F);
        helper.getLevel().addFreshEntity(host);

        ItemStack filled = new ItemStack(ModItems.FILLED_SOUL_VESSEL.get());
        SoulVesselItem.setSoulData(filled, "minecraft:zombie", SoulKind.UNDEAD, 1);
        player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, filled);

        // Game test structures share a single level: a sibling vessel test that fails early can
        // leave its released soul behind, so only souls created by this use action may be counted.
        java.util.Set<Integer> soulsBeforeRelease = helper.getLevel().getEntitiesOfClass(
                ua.meekeyn.requiemunofficial.common.entity.ReleasedSoulEntity.class,
                new net.minecraft.world.phys.AABB(player.blockPosition()).inflate(8.0D)
        ).stream().map(entity -> entity.getId()).collect(java.util.stream.Collectors.toSet());

        var result = player.getMainHandItem().use(
                helper.getLevel(),
                player,
                net.minecraft.world.InteractionHand.MAIN_HAND
        );
        player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, result.getObject());

        long playerFilled = countInventoryItem(player, ModItems.FILLED_SOUL_VESSEL.get());
        long playerEmpty = countInventoryItem(player, ModItems.EMPTY_SOUL_VESSEL.get());
        long hostVessels = java.util.Arrays.stream(net.minecraft.world.entity.EquipmentSlot.values())
                .map(host::getItemBySlot)
                .filter(stack -> stack.is(ModItems.FILLED_SOUL_VESSEL.get())
                        || stack.is(ModItems.EMPTY_SOUL_VESSEL.get()))
                .count();
        long droppedVessels = helper.getLevel().getEntitiesOfClass(
                net.minecraft.world.entity.item.ItemEntity.class,
                new net.minecraft.world.phys.AABB(player.blockPosition()).inflate(8.0D),
                item -> item.getItem().is(ModItems.FILLED_SOUL_VESSEL.get())
                        || item.getItem().is(ModItems.EMPTY_SOUL_VESSEL.get())
        ).size();
        long releasedSouls = helper.getLevel().getEntitiesOfClass(
                ua.meekeyn.requiemunofficial.common.entity.ReleasedSoulEntity.class,
                new net.minecraft.world.phys.AABB(player.blockPosition()).inflate(8.0D)
        ).stream()
                .filter(entity -> !soulsBeforeRelease.contains(entity.getId()))
                .count();

        helper.assertTrue(result.getResult().consumesAction(), "Filled vessel release should consume the use action");
        helper.assertTrue(playerFilled == 0, "A single release should consume the filled vessel");
        helper.assertTrue(playerEmpty == 1, "A single release should leave exactly one empty vessel with the player");
        helper.assertTrue(hostVessels == 0, "A single release should not copy vessels to the host");
        helper.assertTrue(droppedVessels == 0, "A single release should not drop extra vessels into the world");
        helper.assertTrue(releasedSouls == 1, "A single release should create exactly one released soul");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void filledSoulVesselRestoresMatchingSoullessBody(GameTestHelper helper) {
        net.minecraft.server.level.ServerPlayer player = new net.minecraft.server.level.ServerPlayer(
                helper.getLevel().getServer(),
                helper.getLevel(),
                new com.mojang.authlib.GameProfile(UUID.randomUUID(), "filled-vessel-restore-test")
        );
        net.minecraft.world.entity.animal.Cow cow =
                new net.minecraft.world.entity.animal.Cow(EntityType.COW, helper.getLevel());
        cow.moveTo(helper.absolutePos(new BlockPos(1, 2, 1)), 0.0F, 0.0F);
        helper.getLevel().addFreshEntity(cow);

        EntitySoulManager.removeSoul(cow);
        helper.assertTrue(EntitySoulManager.isInertSoullessBody(cow), "The cow should be an empty body");

        ItemStack filled = new ItemStack(ModItems.FILLED_SOUL_VESSEL.get());
        SoulVesselItem.setSoulData(filled, "minecraft:cow", SoulKind.BEAST, 1);
        player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, filled);

        net.minecraft.world.InteractionResult result = filled.getItem().interactLivingEntity(
                filled,
                player,
                cow,
                net.minecraft.world.InteractionHand.MAIN_HAND
        );
        helper.assertTrue(result.consumesAction(), "Using the matching vessel should restore the body");
        helper.assertTrue(EntitySoulManager.hasSoul(cow), "The restored cow should have a soul again");
        helper.assertTrue(
                player.getItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND)
                        .is(ModItems.EMPTY_SOUL_VESSEL.get()),
                "Restoring a soul should leave an empty vessel"
        );
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void bodyRecordCreatedWhenSoulRemoved(GameTestHelper helper) {
        net.minecraft.world.entity.animal.Cow cow =
                new net.minecraft.world.entity.animal.Cow(EntityType.COW, helper.getLevel());
        cow.moveTo(helper.absolutePos(new BlockPos(1, 2, 1)), 0.0F, 0.0F);
        helper.getLevel().addFreshEntity(cow);

        EntitySoulManager.removeSoul(cow);

        UUID recordId = BodyRecordData.getEntityRecordId(cow);
        helper.assertTrue(recordId != null, "Soul-stripped body should carry a persistent body record id");
        BodyRecordData.BodyRecord record = BodyRecordData.get(helper.getLevel())
                .getRecord(recordId)
                .orElse(null);
        helper.assertTrue(record != null, "Body record should be stored in world saved data");
        helper.assertTrue(cow.getUUID().equals(record.entityId()), "Body record should point at the stripped body");
        helper.assertTrue("minecraft:cow".equals(record.entityType().toString()), "Body record should persist entity type");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void bodyRecordDataSerializesRecordState(GameTestHelper helper) {
        net.minecraft.world.entity.animal.Cow cow =
                new net.minecraft.world.entity.animal.Cow(EntityType.COW, helper.getLevel());
        cow.moveTo(helper.absolutePos(new BlockPos(1, 2, 1)), 0.0F, 0.0F);
        helper.getLevel().addFreshEntity(cow);
        BodyRecordData.BodyRecord record = BodyRecordData.get(helper.getLevel()).getOrCreateRecord(cow);
        BodyRecordData.get(helper.getLevel()).markReleasedSoulPending(record.recordId());

        CompoundTag saved = BodyRecordData.get(helper.getLevel()).save(new CompoundTag());
        BodyRecordData loaded = BodyRecordData.load(saved);
        BodyRecordData.BodyRecord loadedRecord = loaded.getRecord(record.recordId()).orElse(null);

        helper.assertTrue(loadedRecord != null, "Serialized body record should load");
        helper.assertTrue(loadedRecord.releasedSoulPending(), "Released-soul pending flag should persist");
        helper.assertTrue(cow.getUUID().equals(loadedRecord.entityId()), "Entity UUID should persist");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void releasedSoulMergesWithRecordedBody(GameTestHelper helper) {
        net.minecraft.world.entity.animal.Cow cow =
                new net.minecraft.world.entity.animal.Cow(EntityType.COW, helper.getLevel());
        cow.moveTo(helper.absolutePos(new BlockPos(1, 2, 1)), 0.0F, 0.0F);
        helper.getLevel().addFreshEntity(cow);
        EntitySoulManager.removeSoul(cow);
        UUID recordId = BodyRecordData.getEntityRecordId(cow);

        ua.meekeyn.requiemunofficial.common.entity.ReleasedSoulEntity soul =
                new ua.meekeyn.requiemunofficial.common.entity.ReleasedSoulEntity(
                        ModEntities.RELEASED_SOUL.get(),
                        helper.getLevel()
                );
        soul.setOwnerRecord(recordId);
        soul.setSoulData("minecraft:cow", 1);
        soul.moveTo(cow.getX(), cow.getY(), cow.getZ(), 0.0F, 0.0F);
        helper.getLevel().addFreshEntity(soul);

        helper.runAfterDelay(2, () -> {
            helper.assertTrue(!soul.isAlive(), "Released soul should discard after merging with its body");
            helper.assertTrue(EntitySoulManager.hasSoul(cow), "Recorded body should receive its soul back");
            helper.assertTrue(BodyRecordData.get(helper.getLevel()).getRecord(recordId).isEmpty(),
                    "Merged body record should be invalidated");
            helper.succeed();
        });
    }

    @GameTest(template = "empty")
    public static void releasedSoulInvalidatesMissingBodyRecord(GameTestHelper helper) {
        net.minecraft.world.entity.animal.Cow cow =
                new net.minecraft.world.entity.animal.Cow(EntityType.COW, helper.getLevel());
        cow.moveTo(helper.absolutePos(new BlockPos(1, 2, 1)), 0.0F, 0.0F);
        helper.getLevel().addFreshEntity(cow);
        EntitySoulManager.removeSoul(cow);
        UUID recordId = BodyRecordData.getEntityRecordId(cow);
        cow.discard();

        ua.meekeyn.requiemunofficial.common.entity.ReleasedSoulEntity soul =
                new ua.meekeyn.requiemunofficial.common.entity.ReleasedSoulEntity(
                        ModEntities.RELEASED_SOUL.get(),
                        helper.getLevel()
                );
        soul.setOwnerRecord(recordId);
        soul.setSoulData("minecraft:cow", 1);
        soul.moveTo(helper.absolutePos(new BlockPos(2, 2, 1)), 0.0F, 0.0F);
        helper.getLevel().addFreshEntity(soul);

        helper.runAfterDelay(65, () -> {
            helper.assertTrue(!soul.isAlive(), "Missing-body soul should expire");
            helper.assertTrue(BodyRecordData.get(helper.getLevel()).getRecord(recordId).isEmpty(),
                    "Missing body should invalidate the body record");
            helper.succeed();
        });
    }

    @GameTest(template = "empty")
    public static void filledVesselPreventsDuplicateReleasedSoulForRecord(GameTestHelper helper) {
        net.minecraft.server.level.ServerPlayer player = new net.minecraft.server.level.ServerPlayer(
                helper.getLevel().getServer(),
                helper.getLevel(),
                new com.mojang.authlib.GameProfile(UUID.randomUUID(), "duplicate-release-test")
        );
        player.moveTo(helper.absolutePos(new BlockPos(1, 2, 1)), 0.0F, 0.0F);
        net.minecraft.world.entity.animal.Cow cow =
                new net.minecraft.world.entity.animal.Cow(EntityType.COW, helper.getLevel());
        cow.moveTo(helper.absolutePos(new BlockPos(5, 2, 1)), 0.0F, 0.0F);
        helper.getLevel().addFreshEntity(cow);
        EntitySoulManager.removeSoul(cow);
        UUID recordId = BodyRecordData.getEntityRecordId(cow);

        ItemStack first = new ItemStack(ModItems.FILLED_SOUL_VESSEL.get());
        SoulVesselItem.setSoulData(first, "minecraft:cow", SoulKind.BEAST, 1);
        SoulVesselItem.setBodyRecord(first, recordId);
        ItemStack duplicate = first.copy();

        player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, first);
        var firstResult = first.use(helper.getLevel(), player, net.minecraft.world.InteractionHand.MAIN_HAND);
        player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, duplicate);
        var duplicateResult = duplicate.use(helper.getLevel(), player, net.minecraft.world.InteractionHand.MAIN_HAND);

        helper.assertTrue(firstResult.getObject().is(ModItems.EMPTY_SOUL_VESSEL.get()),
                "First record release should consume the vessel");
        helper.assertTrue(!duplicateResult.getResult().consumesAction(),
                "Duplicate record release should fail while a released soul is active");
        long released = helper.getLevel().getEntitiesOfClass(
                ua.meekeyn.requiemunofficial.common.entity.ReleasedSoulEntity.class,
                new net.minecraft.world.phys.AABB(player.blockPosition()).inflate(8.0D)
        ).stream().filter(soul -> "minecraft:cow".equals(soul.getSoulType())).count();
        helper.assertTrue(released == 1, "Only one released soul should exist for one active record");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void filledVesselDoesNotConsumeInvalidBodyRecord(GameTestHelper helper) {
        net.minecraft.server.level.ServerPlayer player = new net.minecraft.server.level.ServerPlayer(
                helper.getLevel().getServer(),
                helper.getLevel(),
                new com.mojang.authlib.GameProfile(UUID.randomUUID(), "invalid-record-vessel-test")
        );
        player.moveTo(helper.absolutePos(new BlockPos(1, 2, 1)), 0.0F, 0.0F);
        net.minecraft.world.entity.animal.Cow cow =
                new net.minecraft.world.entity.animal.Cow(EntityType.COW, helper.getLevel());
        helper.getLevel().addFreshEntity(cow);
        EntitySoulManager.removeSoul(cow);
        UUID recordId = BodyRecordData.getEntityRecordId(cow);
        BodyRecordData.get(helper.getLevel()).invalidate(recordId);

        ItemStack vessel = new ItemStack(ModItems.FILLED_SOUL_VESSEL.get());
        SoulVesselItem.setSoulData(vessel, "minecraft:cow", SoulKind.BEAST, 1);
        SoulVesselItem.setBodyRecord(vessel, recordId);
        player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, vessel);

        var result = vessel.use(helper.getLevel(), player, net.minecraft.world.InteractionHand.MAIN_HAND);

        helper.assertTrue(!result.getResult().consumesAction(),
                "Invalid body records must not consume a filled vessel");
        helper.assertTrue(player.getMainHandItem().is(ModItems.FILLED_SOUL_VESSEL.get()),
                "Failed record release should keep the original filled vessel");
        long released = helper.getLevel().getEntitiesOfClass(
                ua.meekeyn.requiemunofficial.common.entity.ReleasedSoulEntity.class,
                new net.minecraft.world.phys.AABB(player.blockPosition()).inflate(8.0D)
        ).size();
        helper.assertTrue(released == 0, "Invalid body records must not spawn a released soul");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void obeliskSoulDataPersists(GameTestHelper helper) {
        ObeliskSoulEntity original =
                new ObeliskSoulEntity(ModEntities.OBELISK_SOUL.get(), helper.getLevel());
        original.setSoulData("minecraft:wither", 9);

        ObeliskSoulEntity loaded =
                new ObeliskSoulEntity(ModEntities.OBELISK_SOUL.get(), helper.getLevel());
        loaded.load(original.saveWithoutId(new CompoundTag()));

        helper.assertTrue("minecraft:wither".equals(loaded.getSoulType()), "Soul type should persist");
        helper.assertTrue(loaded.getSoulAmount() == 9, "Soul amount should persist");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void pendingRitualPersists(GameTestHelper helper) {
        BlockState state = ModBlocks.RIFT_RUNE.get().defaultBlockState();
        UUID playerId = UUID.randomUUID();
        UUID targetId = UUID.randomUUID();
        RunestoneBlockEntity original = new RunestoneBlockEntity(BlockPos.ZERO, state);
        original.startRitual(RitualType.DECAY_ZOMBIE.id(), 120L, playerId, targetId);

        RunestoneBlockEntity loaded = new RunestoneBlockEntity(BlockPos.ZERO, state);
        loaded.load(original.saveWithFullMetadata());

        helper.assertTrue(loaded.hasPendingRitual(), "Pending ritual should persist");
        helper.assertTrue(
                RitualType.DECAY_ZOMBIE.id().toString().equals(loaded.getPendingRitualId()),
                "Ritual type should persist"
        );
        helper.assertTrue(playerId.equals(loaded.getRitualPlayerId()), "Ritual player should persist");
        helper.assertTrue(targetId.equals(loaded.getRitualTargetId()), "Ritual target should persist");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void riftUseRequiresIncorporealVagrant(GameTestHelper helper) {
        BlockState state = ModBlocks.RIFT_RUNE.get().defaultBlockState()
                .setValue(ua.meekeyn.requiemunofficial.common.block.InertRunestoneBlock.HEAD, true)
                .setValue(ua.meekeyn.requiemunofficial.common.block.InertRunestoneBlock.ACTIVATED, true);
        RunestoneBlockEntity runestone = new RunestoneBlockEntity(BlockPos.ZERO, state);
        runestone.setStructureData(
                3,
                4,
                true,
                java.util.Map.of("rift", 1),
                java.util.Map.of("rift", BlockPos.ZERO)
        );
        runestone.setPowerRate(1.0F);

        net.minecraft.server.level.ServerPlayer player = new net.minecraft.server.level.ServerPlayer(
                helper.getLevel().getServer(),
                helper.getLevel(),
                new com.mojang.authlib.GameProfile(UUID.randomUUID(), "rift-vagrant-test")
        );

        SoulManager.setState(player, SoulState.MORTAL);
        helper.assertTrue(!RiftRunestoneBlock.canUseRift(player, runestone),
                "Mortal players should not use rifts");

        SoulManager.setState(player, SoulState.REMNANT);
        helper.assertTrue(!RiftRunestoneBlock.canUseRift(player, runestone),
                "Embodied remnants should not use rifts");

        SoulManager.setState(player, SoulState.VAGRANT);
        helper.assertTrue(RiftRunestoneBlock.canUseRift(player, runestone),
                "Incorporeal vagrants should use active powered rifts");

        SoulManager.setPossessedEntity(player, UUID.randomUUID());
        helper.assertTrue(!RiftRunestoneBlock.canUseRift(player, runestone),
                "Vagrants inside a body should not use rifts");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void possessionTagsAreConsistent(GameTestHelper helper) {
        helper.assertTrue(EntityType.ZOMBIE.is(ModTags.POSSESSABLES), "Zombies should be possessable");
        helper.assertTrue(EntityType.ZOMBIE.is(ModTags.SOULLESS), "Zombies should be soulless hosts");
        helper.assertTrue(
                EntityType.WITHER.is(ModTags.POSSESSION_BLACKLIST),
                "Bosses should be excluded from possession"
        );
        helper.assertTrue(EntityType.WITHER.is(ModTags.SOUL_AGGREGATES),
                "Withers should be treated as soul aggregates");
        helper.assertTrue(EntityType.WITHER.is(ModTags.SOUL_CAPTURE_BLACKLIST),
                "Withers should retain their boss blacklist tag");
        helper.assertTrue(
                VagrantRules.canInteractWith(ModBlocks.RIFT_RUNE.get().defaultBlockState()),
                "Vagrants should be able to interact with rift runestones"
        );
        helper.assertTrue(
                !VagrantRules.canInteractWith(net.minecraft.world.level.block.Blocks.CHEST.defaultBlockState()),
                "Vagrants should not be able to interact with ordinary containers"
        );
        helper.assertTrue(
                !VagrantRules.canPassThrough(net.minecraft.world.level.block.Blocks.BEDROCK.defaultBlockState()),
                "Vagrants should not pass through soul-impermeable blocks"
        );
        helper.assertTrue(EntityType.SHULKER.is(ModTags.IMMOVABLE),
                "Shulkers should be immovable possession hosts");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void expandedVanillaPossessionTagsAreConsistent(GameTestHelper helper) {
        helper.assertTrue(EntityType.AXOLOTL.is(ModTags.POSSESSABLES), "Axolotls should be possessable");
        helper.assertTrue(EntityType.PANDA.is(ModTags.POSSESSABLES), "Pandas should be possessable");
        helper.assertTrue(!EntityType.WARDEN.is(ModTags.POSSESSABLES), "Wardens should not be possessable");
        helper.assertTrue(EntityType.WARDEN.is(ModTags.POSSESSION_BLACKLIST), "Wardens should stay boss-blocked");
        helper.assertTrue(EntityType.EVOKER.is(ModTags.POSSESSABLES), "Evokers should be possessable");
        helper.assertTrue(EntityType.WITCH.is(ModTags.POSSESSABLES), "Witches should be possessable");
        helper.assertTrue(EntityType.IRON_GOLEM.is(ModTags.POSSESSABLES), "Iron golems should be possessable");
        helper.assertTrue(EntityType.SNOW_GOLEM.is(ModTags.POSSESSABLES), "Snow golems should be possessable");
        helper.assertTrue(EntityType.TRADER_LLAMA.is(ModTags.POSSESSABLES), "Trader llamas should be possessable");
        helper.assertTrue(EntityType.WANDERING_TRADER.is(ModTags.POSSESSABLES),
                "Wandering traders should be possessable");
        helper.assertTrue(EntityType.DONKEY.is(ModTags.INVENTORY_CARRIERS), "Donkeys should carry inventory");
        helper.assertTrue(EntityType.LLAMA.is(ModTags.INVENTORY_CARRIERS), "Llamas should carry inventory");
        helper.assertTrue(EntityType.TRADER_LLAMA.is(ModTags.INVENTORY_CARRIERS),
                "Trader llamas should carry inventory like llamas");
        helper.assertTrue(EntityType.MULE.is(ModTags.INVENTORY_CARRIERS), "Mules should carry inventory");
        helper.assertTrue(EntityType.FOX.is(ModTags.ITEM_USERS), "Foxes should be one-item users");
        helper.assertTrue(EntityType.VEX.is(ModTags.ITEM_USERS), "Vexes should use one held item");
        helper.assertTrue(EntityType.PANDA.is(ModTags.INVENTORY_CARRIERS), "Pandas should carry inventory");
        helper.assertTrue(EntityType.PANDA.is(ModTags.ITEM_USERS), "Pandas should use items");
        helper.assertTrue(!EntityType.PANDA.is(ModTags.ARMOR_USERS), "Pandas should not use armor");
        helper.assertTrue(EntityType.ZOMBIE.is(ModTags.ARMOR_USERS), "Humanoid undead should use armor");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void livingPossessionRequiresSoullessHost(GameTestHelper helper) {
        net.minecraft.world.entity.animal.Cow cow =
                new net.minecraft.world.entity.animal.Cow(EntityType.COW, helper.getLevel());

        helper.assertTrue(EntityType.COW.is(ModTags.POSSESSABLES), "Cows should be allowed host types");
        helper.assertTrue(EntitySoulManager.hasSoul(cow), "Fresh living animals should start with a soul");
        helper.assertTrue(
                !EntitySoulManager.canBePossessionHost(cow),
                "Living animals should not be possessable until their soul is removed"
        );

        EntitySoulManager.removeSoul(cow);
        helper.assertTrue(!EntitySoulManager.hasSoul(cow), "Soul removal should persist on the entity");
        helper.assertTrue(EntitySoulManager.isInertSoullessBody(cow), "Soul-stripped bodies should be inert");
        helper.assertTrue(cow.isNoAi(), "Soul-stripped mobs should stop acting on their own");
        helper.assertTrue(
                EntitySoulManager.canBePossessionHost(cow),
                "A living animal with no soul should become a valid possession body"
        );
        EntitySoulManager.removeSoul(cow);
        helper.assertTrue(!EntitySoulManager.hasSoul(cow), "A soul-stripped body should not gain another soul");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void vagrantPossessionStartsDelayedAttempt(GameTestHelper helper) {
        net.minecraft.server.level.ServerPlayer player = new net.minecraft.server.level.ServerPlayer(
                helper.getLevel().getServer(),
                helper.getLevel(),
                new com.mojang.authlib.GameProfile(UUID.randomUUID(), "pending-possession-test")
        );
        SoulManager.setState(player, SoulState.VAGRANT);

        net.minecraft.world.entity.animal.Cow cow =
                new net.minecraft.world.entity.animal.Cow(EntityType.COW, helper.getLevel());
        cow.moveTo(helper.absolutePos(new BlockPos(1, 2, 1)), 0.0F, 0.0F);
        helper.getLevel().addFreshEntity(cow);
        EntitySoulManager.removeSoul(cow);

        helper.assertTrue(
                PossessionManager.beginPossessionAttemptForTest(player, cow),
                "A vagrant should be able to start a possession attempt on a soulless body"
        );
        helper.assertTrue(
                PossessionManager.hasPendingPossessionForTest(player),
                "Possession should be pending during the original-style delay"
        );
        helper.assertTrue(
                SoulManager.getPossessedEntityId(player) == null,
                "Starting a possession attempt should not possess instantly"
        );
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void vagrantFlightRestoresOriginalPlayerAbilities(GameTestHelper helper) {
        net.minecraft.server.level.ServerPlayer player = new net.minecraft.server.level.ServerPlayer(
                helper.getLevel().getServer(),
                helper.getLevel(),
                new com.mojang.authlib.GameProfile(UUID.randomUUID(), "vagrant-abilities-test")
        );
        player.getAbilities().mayfly = false;
        player.getAbilities().flying = false;
        SoulManager.setState(player, SoulState.VAGRANT);

        VagrantEvents.updateVagrantStateForTest(player);
        helper.assertTrue(player.getAbilities().mayfly, "Vagrant souls should get original-style soul flight");
        helper.assertTrue(!player.getAbilities().flying, "Soul flight should not force creative flying on entry");
        helper.assertTrue(!player.isNoGravity(), "Vagrant souls should keep gravity until creative-style flight is toggled");
        helper.assertTrue(!player.isInvisible(), "Vagrant souls should remain renderable as a gray translucent body");
        helper.assertTrue(VagrantEvents.shouldSuppressDamageForVagrant(player),
                "Free vagrant souls should ignore ordinary incoming damage");

        SoulManager.setState(player, SoulState.MORTAL);
        VagrantEvents.updateVagrantStateForTest(player);
        helper.assertTrue(!player.getAbilities().mayfly, "Leaving vagrant state should restore mayfly");
        helper.assertTrue(!player.getAbilities().flying, "Leaving vagrant state should restore flying");
        helper.assertTrue(!player.isNoGravity(), "Leaving vagrant state should restore gravity");
        helper.assertTrue(!player.isInvisible(), "Leaving vagrant state should reveal an unpossessed player");
        helper.assertTrue(!VagrantEvents.shouldSuppressDamageForVagrant(player),
                "Embodied players should not keep vagrant damage immunity");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void mortalStateCleansStuckVagrantBodyFlags(GameTestHelper helper) {
        net.minecraft.server.level.ServerPlayer player = new net.minecraft.server.level.ServerPlayer(
                helper.getLevel().getServer(),
                helper.getLevel(),
                new com.mojang.authlib.GameProfile(UUID.randomUUID(), "stuck-vagrant-test")
        );
        player.getAbilities().mayfly = true;
        player.getAbilities().flying = true;
        player.setInvisible(true);
        player.noPhysics = true;
        player.setNoGravity(true);

        SoulManager.setState(player, SoulState.MORTAL);

        helper.assertTrue(!player.getAbilities().mayfly, "Mortal survival players should not keep vagrant mayfly");
        helper.assertTrue(!player.getAbilities().flying, "Mortal survival players should not keep vagrant flying");
        helper.assertTrue(!player.isInvisible(), "Mortal unpossessed players should not stay invisible");
        helper.assertTrue(!player.noPhysics, "Mortal unpossessed players should not keep suppressed collision");
        helper.assertTrue(!player.isNoGravity(), "Mortal unpossessed players should not keep suppressed gravity");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void spectatorKeepsNoPhysicsDuringVagrantCleanup(GameTestHelper helper) {
        net.minecraft.server.level.ServerPlayer player = new net.minecraft.server.level.ServerPlayer(
                helper.getLevel().getServer(),
                helper.getLevel(),
                new com.mojang.authlib.GameProfile(UUID.randomUUID(), "spectator-nophysics-test")
        ) {
            @Override
            public boolean isSpectator() {
                return true;
            }
        };
        player.noPhysics = true;

        SoulManager.setState(player, SoulState.MORTAL);
        VagrantEvents.updateVagrantStateForTest(player);

        helper.assertTrue(player.isSpectator(), "Test player should be in spectator mode");
        helper.assertTrue(player.noPhysics, "Requiem cleanup must not disable spectator noPhysics");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void vagrantCollisionIgnoresTraversableButNotImpermeableBlocks(GameTestHelper helper) {
        net.minecraft.server.level.ServerPlayer player = new net.minecraft.server.level.ServerPlayer(
                helper.getLevel().getServer(),
                helper.getLevel(),
                new com.mojang.authlib.GameProfile(UUID.randomUUID(), "vagrant-collision-test")
        );
        SoulManager.setState(player, SoulState.VAGRANT);
        VagrantEvents.updateVagrantStateForTest(player);
        player.getPersistentData().putInt(VagrantRules.PHASE_CHARGE_TICKS, VagrantRules.PHASE_REQUIRED_TICKS);

        BlockPos testPos = helper.absolutePos(new BlockPos(1, 2, 1));
        net.minecraft.world.phys.AABB bodyBox = new net.minecraft.world.phys.AABB(
                testPos.getX() - 1.0D,
                testPos.getY(),
                testPos.getZ(),
                testPos.getX() - 0.1D,
                testPos.getY() + 1.8D,
                testPos.getZ() + 0.9D
        );

        helper.getLevel().setBlock(
                testPos,
                net.minecraft.world.level.block.Blocks.STONE.defaultBlockState(),
                3
        );
        net.minecraft.world.phys.Vec3 throughStone = net.minecraft.world.entity.Entity.collideBoundingBox(
                player,
                new net.minecraft.world.phys.Vec3(1.0D, 0.0D, 0.0D),
                bodyBox,
                helper.getLevel(),
                List.of()
        );
        helper.assertTrue(
                throughStone.x > 0.95D,
                "Vagrant souls should phase through ordinary block collision"
        );

        helper.getLevel().setBlock(
                testPos,
                net.minecraft.world.level.block.Blocks.BEDROCK.defaultBlockState(),
                3
        );
        net.minecraft.world.phys.Vec3 intoBedrock = net.minecraft.world.entity.Entity.collideBoundingBox(
                player,
                new net.minecraft.world.phys.Vec3(1.0D, 0.0D, 0.0D),
                bodyBox,
                helper.getLevel(),
                List.of()
        );
        helper.assertTrue(
                intoBedrock.x < 0.95D,
                "Soul-impermeable blocks should keep collision for vagrant souls"
        );
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void possessionFxPacketRoundTrips(GameTestHelper helper) {
        ua.meekeyn.requiemunofficial.common.network.PossessionFxPacket packet =
                new ua.meekeyn.requiemunofficial.common.network.PossessionFxPacket(
                        ua.meekeyn.requiemunofficial.common.network.PossessionFxPacket.Phase.START,
                        42
                );
        net.minecraft.network.FriendlyByteBuf buffer =
                new net.minecraft.network.FriendlyByteBuf(io.netty.buffer.Unpooled.buffer());
        ua.meekeyn.requiemunofficial.common.network.PossessionFxPacket.encode(packet, buffer);
        ua.meekeyn.requiemunofficial.common.network.PossessionFxPacket decoded =
                ua.meekeyn.requiemunofficial.common.network.PossessionFxPacket.decode(buffer);

        helper.assertTrue(
                decoded.phase() == ua.meekeyn.requiemunofficial.common.network.PossessionFxPacket.Phase.START,
                "Possession FX phase should round-trip"
        );
        helper.assertTrue(decoded.targetEntityId() == 42, "Possession FX target id should round-trip");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void soullessBodiesKeepExternalPhysics(GameTestHelper helper) {
        net.minecraft.world.entity.animal.Cow cow =
                new net.minecraft.world.entity.animal.Cow(EntityType.COW, helper.getLevel());
        cow.moveTo(helper.absolutePos(new BlockPos(1, 4, 1)), 0.0F, 0.0F);
        helper.getLevel().addFreshEntity(cow);
        EntitySoulManager.removeSoul(cow);

        cow.setDeltaMovement(0.35D, 0.0D, 0.0D);
        EntitySoulManager.onLivingTick(new net.minecraftforge.event.entity.living.LivingEvent.LivingTickEvent(cow));
        helper.assertTrue(cow.getDeltaMovement().x > 0.3D,
                "Inert soulless bodies must not erase horizontal external impulses");
        helper.assertTrue(!cow.noPhysics, "Inert soulless bodies must keep block collision physics");
        helper.assertTrue(!cow.isNoGravity(), "Inert soulless bodies must keep gravity");

        double startY = cow.getY();
        helper.runAfterDelay(10, () -> {
            helper.assertTrue(cow.getY() < startY,
                    "A soulless body moved into air should fall instead of hanging");
            helper.succeed();
        });
    }

    @GameTest(template = "empty")
    public static void leafPhasingUsesFifteenTickChargeWithoutChargingStone(GameTestHelper helper) {
        net.minecraft.server.level.ServerPlayer player = new net.minecraft.server.level.ServerPlayer(
                helper.getLevel().getServer(),
                helper.getLevel(),
                new com.mojang.authlib.GameProfile(UUID.randomUUID(), "leaf-phase-test")
        );
        player.moveTo(helper.absolutePos(new BlockPos(1, 2, 1)), 0.0F, 0.0F);
        player.setYRot(-90.0F);
        player.setYHeadRot(-90.0F);
        SoulManager.setState(player, SoulState.VAGRANT);
        helper.getLevel().setBlock(helper.absolutePos(new BlockPos(1, 2, 2)),
                net.minecraft.world.level.block.Blocks.OAK_LEAVES.defaultBlockState(), 3);
        helper.getLevel().setBlock(helper.absolutePos(new BlockPos(1, 2, 3)),
                net.minecraft.world.level.block.Blocks.STONE.defaultBlockState(), 3);

        VagrantEvents.refreshAfterSoulStateChange(player);
        VagrantEvents.setVagrantInput(player, false, false, true);
        for (int i = 0; i < VagrantRules.LEAF_PHASE_REQUIRED_TICKS; i++) {
            VagrantEvents.onPlayerTick(new net.minecraftforge.event.TickEvent.PlayerTickEvent(
                    net.minecraftforge.event.TickEvent.Phase.START,
                    player
            ));
            VagrantEvents.onPlayerTick(new net.minecraftforge.event.TickEvent.PlayerTickEvent(
                    net.minecraftforge.event.TickEvent.Phase.END,
                    player
            ));
        }

        helper.assertTrue(VagrantRules.isLeafPhaseCharged(player), "Leaves should charge phasing after 15 ticks");
        helper.assertTrue(!VagrantRules.isPhaseCharged(player), "Leaf charge must not fill the normal stone charge");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void leafOnlyCollisionDoesNotSuppressAdjacentSlabOrStairs(GameTestHelper helper) {
        net.minecraft.server.level.ServerPlayer player = new net.minecraft.server.level.ServerPlayer(
                helper.getLevel().getServer(),
                helper.getLevel(),
                new com.mojang.authlib.GameProfile(UUID.randomUUID(), "leaf-neighbor-collision-test")
        );
        player.moveTo(helper.absolutePos(new BlockPos(1, 2, 1)), 0.0F, 0.0F);
        SoulManager.setState(player, SoulState.VAGRANT);
        player.getPersistentData().putInt(VagrantRules.LEAF_PHASE_CHARGE_TICKS, VagrantRules.LEAF_PHASE_REQUIRED_TICKS);
        VagrantRules.setLeafPhaseActive(player, true);

        helper.getLevel().setBlock(helper.absolutePos(new BlockPos(1, 2, 1)),
                net.minecraft.world.level.block.Blocks.OAK_LEAVES.defaultBlockState(), 3);
        helper.getLevel().setBlock(helper.absolutePos(new BlockPos(2, 2, 1)),
                net.minecraft.world.level.block.Blocks.STONE_STAIRS.defaultBlockState(), 3);
        helper.getLevel().setBlock(helper.absolutePos(new BlockPos(1, 2, 2)),
                net.minecraft.world.level.block.Blocks.STONE_SLAB.defaultBlockState(), 3);

        net.minecraft.world.phys.AABB crownBox = player.getBoundingBox();
        net.minecraft.world.phys.Vec3 crownStairs = net.minecraft.world.entity.Entity.collideBoundingBox(
                player,
                new net.minecraft.world.phys.Vec3(1.0D, 0.0D, 0.0D),
                crownBox,
                helper.getLevel(),
                List.of()
        );
        net.minecraft.world.phys.Vec3 crownSlab = net.minecraft.world.entity.Entity.collideBoundingBox(
                player,
                new net.minecraft.world.phys.Vec3(0.0D, 0.0D, 1.0D),
                crownBox,
                helper.getLevel(),
                List.of()
        );
        helper.assertTrue(crownStairs.x < 1.0D, "Leaf-only phasing must keep adjacent stair collision");
        helper.assertTrue(crownSlab.z < 1.0D, "Leaf-only phasing must keep adjacent slab collision");

        for (int i = 0; i < VagrantRules.PHASE_REQUIRED_TICKS; i++) {
            tickVagrantPlayer(player);
        }
        helper.assertTrue(VagrantRules.isPhaseCharged(player),
                "Staying inside the leaf crown should fill the 60 tick solid passage charge");

        net.minecraft.world.phys.Vec3 chargedStairs = net.minecraft.world.entity.Entity.collideBoundingBox(
                player,
                new net.minecraft.world.phys.Vec3(1.0D, 0.0D, 0.0D),
                player.getBoundingBox(),
                helper.getLevel(),
                List.of()
        );
        net.minecraft.world.phys.Vec3 chargedSlab = net.minecraft.world.entity.Entity.collideBoundingBox(
                player,
                new net.minecraft.world.phys.Vec3(0.0D, 0.0D, 1.0D),
                player.getBoundingBox(),
                helper.getLevel(),
                List.of()
        );
        helper.assertTrue(chargedStairs.x > 0.95D,
                "After the 60 tick charge the vagrant should pass the adjacent stairs");
        helper.assertTrue(chargedSlab.z > 0.95D,
                "After the 60 tick charge the vagrant should pass the adjacent slab");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void vagrantPhasingUsesOriginalChargeAndMovementScale(GameTestHelper helper) {
        net.minecraft.server.level.ServerPlayer player = new net.minecraft.server.level.ServerPlayer(
                helper.getLevel().getServer(),
                helper.getLevel(),
                new com.mojang.authlib.GameProfile(UUID.randomUUID(), "vagrant-phase-speed-test")
        );
        player.moveTo(helper.absolutePos(new BlockPos(1, 2, 1)), 0.0F, 0.0F);
        SoulManager.setState(player, SoulState.VAGRANT);
        VagrantEvents.refreshAfterSoulStateChange(player);
        VagrantEvents.setVagrantInput(player, false, false, true);
        player.horizontalCollision = true;

        for (int i = 0; i < VagrantRules.PHASE_REQUIRED_TICKS - 1; i++) {
            VagrantEvents.onPlayerTick(new net.minecraftforge.event.TickEvent.PlayerTickEvent(
                    net.minecraftforge.event.TickEvent.Phase.START,
                    player
            ));
            VagrantEvents.onPlayerTick(new net.minecraftforge.event.TickEvent.PlayerTickEvent(
                    net.minecraftforge.event.TickEvent.Phase.END,
                    player
            ));
            player.horizontalCollision = true;
        }
        helper.assertTrue(!player.noPhysics,
                "Vagrant phasing should not enable noPhysics before the original 60 tick charge");

        VagrantEvents.onPlayerTick(new net.minecraftforge.event.TickEvent.PlayerTickEvent(
                net.minecraftforge.event.TickEvent.Phase.START,
                player
        ));
        VagrantEvents.onPlayerTick(new net.minecraftforge.event.TickEvent.PlayerTickEvent(
                net.minecraftforge.event.TickEvent.Phase.END,
                player
        ));

        helper.assertTrue(!player.noPhysics, "Vagrant phasing should keep noPhysics disabled so filtered collisions run");
        helper.assertTrue(VagrantRules.shouldSuppressBlockCollision(player),
                "Vagrant phasing should suppress only allowed block collisions after the original charge");
        helper.assertTrue(Math.abs(player.getDeltaMovement().horizontalDistance() - VagrantRules.PHASE_SPEED) < 0.0001D,
                "Charged phase entry should use the configured movement scale inside blocks");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void vagrantPhasingLowersByTwoPixelsWithoutAccumulating(GameTestHelper helper) {
        net.minecraft.server.level.ServerPlayer player = new net.minecraft.server.level.ServerPlayer(
                helper.getLevel().getServer(),
                helper.getLevel(),
                new com.mojang.authlib.GameProfile(UUID.randomUUID(), "vagrant-phase-lower-test")
        );
        player.setPosRaw(1.0D, 2.0D, 1.0D);
        double startY = player.getY();

        VagrantRules.updatePhaseLowering(player, true);
        VagrantRules.updatePhaseLowering(player, true);
        helper.assertTrue(Math.abs(player.getY() - (startY - VagrantRules.PHASE_LOWERING)) < 0.0001D,
                "Phase lowering should apply exactly two pixels once");

        VagrantRules.updatePhaseLowering(player, false);
        helper.assertTrue(Math.abs(player.getY() - startY) < 0.0001D,
                "Phase lowering should restore the original position");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void vagrantPhaseLoweringRestoresWhenPhaseStateClears(GameTestHelper helper) {
        net.minecraft.server.level.ServerPlayer player = new net.minecraft.server.level.ServerPlayer(
                helper.getLevel().getServer(),
                helper.getLevel(),
                new com.mojang.authlib.GameProfile(UUID.randomUUID(), "vagrant-phase-clear-lower-test")
        );
        player.setPosRaw(1.0D, 2.0D, 1.0D);
        double startY = player.getY();

        VagrantRules.updatePhaseLowering(player, true);
        VagrantRules.clearPhaseState(player);

        helper.assertTrue(Math.abs(player.getY() - startY) < 0.0001D,
                "Clearing phase state should restore the lowered vagrant height");
        helper.assertTrue(!player.getPersistentData().getBoolean(VagrantRules.PHASE_LOWERED),
                "Clearing phase state should remove the lowered marker");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void vagrantLeavesPhaseEntryChargesAfterFifteenTicks(GameTestHelper helper) {
        net.minecraft.server.level.ServerPlayer player = new net.minecraft.server.level.ServerPlayer(
                helper.getLevel().getServer(),
                helper.getLevel(),
                new com.mojang.authlib.GameProfile(UUID.randomUUID(), "vagrant-leaf-phase-test")
        );
        player.moveTo(helper.absolutePos(new BlockPos(1, 2, 1)), 0.0F, 0.0F);
        helper.getLevel().setBlock(helper.absolutePos(new BlockPos(1, 3, 2)),
                net.minecraft.world.level.block.Blocks.OAK_LEAVES.defaultBlockState(), 3);
        SoulManager.setState(player, SoulState.VAGRANT);
        VagrantEvents.setVagrantInput(player, false, false, true);

        for (int i = 0; i < VagrantRules.LEAF_PHASE_REQUIRED_TICKS - 1; i++) {
            VagrantEvents.onPlayerTick(new net.minecraftforge.event.TickEvent.PlayerTickEvent(
                    net.minecraftforge.event.TickEvent.Phase.START,
                    player
            ));
        }
        helper.assertTrue(!VagrantRules.isLeafPhaseCharged(player),
                "Touching leaves should not charge vagrant phasing before 15 ticks");

        VagrantEvents.onPlayerTick(new net.minecraftforge.event.TickEvent.PlayerTickEvent(
                net.minecraftforge.event.TickEvent.Phase.START,
                player
        ));
        helper.assertTrue(VagrantRules.isLeafPhaseCharged(player),
                "Touching leaves should charge vagrant phasing after 15 ticks");
        helper.assertTrue(!VagrantRules.isPhaseCharged(player),
                "Leaf phasing should not complete the normal stone phase charge");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void vagrantStoneMoveRequiresSixtyTicks(GameTestHelper helper) {
        net.minecraft.server.level.ServerPlayer early = createVagrantPhasePlayer(helper, "stone-59", new BlockPos(1, 2, 1));
        helper.setBlock(new BlockPos(2, 2, 1), net.minecraft.world.level.block.Blocks.STONE);
        chargeStonePhase(early, VagrantRules.PHASE_REQUIRED_TICKS - 1);
        double earlyX = early.getX();
        early.move(net.minecraft.world.entity.MoverType.SELF, new net.minecraft.world.phys.Vec3(1.0D, 0.0D, 0.0D));
        helper.assertTrue(early.getX() - earlyX < 0.95D, "Stone phasing must not move through at 59 ticks");

        net.minecraft.server.level.ServerPlayer charged = createVagrantPhasePlayer(helper, "stone-60", new BlockPos(1, 2, 3));
        helper.setBlock(new BlockPos(2, 2, 3), net.minecraft.world.level.block.Blocks.STONE);
        chargeStonePhase(charged, VagrantRules.PHASE_REQUIRED_TICKS);
        double chargedX = charged.getX();
        charged.move(net.minecraft.world.entity.MoverType.SELF, new net.minecraft.world.phys.Vec3(1.0D, 0.0D, 0.0D));
        helper.assertTrue(charged.getX() - chargedX > 0.95D, "Stone phasing must move through after 60 ticks");
        helper.assertTrue(!charged.noPhysics, "Charged movement must use filtered collisions, not noPhysics");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void vagrantPhaseMoveKeepsBedrockBarrierAndCornersSolid(GameTestHelper helper) {
        net.minecraft.server.level.ServerPlayer bedrock = createVagrantPhasePlayer(helper, "phase-bedrock", new BlockPos(1, 2, 1));
        helper.setBlock(new BlockPos(2, 2, 1), net.minecraft.world.level.block.Blocks.BEDROCK);
        chargeStonePhase(bedrock, VagrantRules.PHASE_REQUIRED_TICKS);
        double bedrockX = bedrock.getX();
        bedrock.move(net.minecraft.world.entity.MoverType.SELF, new net.minecraft.world.phys.Vec3(1.0D, 0.0D, 0.0D));
        helper.assertTrue(bedrock.getX() - bedrockX < 0.95D, "Bedrock must remain impermeable during phasing");

        net.minecraft.server.level.ServerPlayer barrier = createVagrantPhasePlayer(helper, "phase-barrier", new BlockPos(1, 2, 3));
        helper.setBlock(new BlockPos(2, 2, 3), net.minecraft.world.level.block.Blocks.BARRIER);
        chargeStonePhase(barrier, VagrantRules.PHASE_REQUIRED_TICKS);
        double barrierX = barrier.getX();
        barrier.move(net.minecraft.world.entity.MoverType.SELF, new net.minecraft.world.phys.Vec3(1.0D, 0.0D, 0.0D));
        helper.assertTrue(barrier.getX() - barrierX < 0.95D, "Barrier must remain impermeable during phasing");

        net.minecraft.server.level.ServerPlayer corner = createVagrantPhasePlayer(helper, "phase-corner", new BlockPos(1, 2, 5));
        helper.setBlock(new BlockPos(2, 2, 5), net.minecraft.world.level.block.Blocks.BEDROCK);
        helper.setBlock(new BlockPos(1, 2, 6), net.minecraft.world.level.block.Blocks.BEDROCK);
        helper.setBlock(new BlockPos(2, 2, 6), net.minecraft.world.level.block.Blocks.STONE);
        chargeStonePhase(corner, VagrantRules.PHASE_REQUIRED_TICKS);
        double cornerX = corner.getX();
        double cornerZ = corner.getZ();
        corner.move(net.minecraft.world.entity.MoverType.SELF, new net.minecraft.world.phys.Vec3(1.0D, 0.0D, 1.0D));
        helper.assertTrue(corner.getX() - cornerX < 0.95D || corner.getZ() - cornerZ < 0.95D,
                "Impermeable corner blocks must prevent diagonal corner phasing");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void vagrantPhaseTickClearsNoPhysicsBeforeFilteredCollisionMove(GameTestHelper helper) {
        net.minecraft.server.level.ServerPlayer bedrock = createVagrantPhasePlayer(helper, "phase-clears-no-physics", new BlockPos(1, 2, 1));
        helper.setBlock(new BlockPos(2, 2, 1), net.minecraft.world.level.block.Blocks.BEDROCK);
        chargeStonePhase(bedrock, VagrantRules.PHASE_REQUIRED_TICKS);
        bedrock.noPhysics = true;

        tickVagrantPlayer(bedrock);
        helper.assertTrue(!bedrock.noPhysics, "Vagrant phase tick must keep noPhysics disabled for collision filtering");
        helper.assertTrue(VagrantRules.shouldSuppressBlockCollision(bedrock),
                "Charged vagrant should still request filtered collision suppression");

        double bedrockX = bedrock.getX();
        bedrock.move(net.minecraft.world.entity.MoverType.SELF, new net.minecraft.world.phys.Vec3(1.0D, 0.0D, 0.0D));
        helper.assertTrue(bedrock.getX() - bedrockX < 0.95D,
                "Filtered movement must keep soul-impermeable bedrock solid after stale noPhysics is cleared");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void vagrantLeafPhaseUsesFilteredCollisionWithoutNoPhysics(GameTestHelper helper) {
        net.minecraft.server.level.ServerPlayer player = createVagrantPhasePlayer(helper, "leaf-filtered-no-physics", new BlockPos(1, 2, 1));
        helper.setBlock(new BlockPos(2, 2, 1), net.minecraft.world.level.block.Blocks.OAK_LEAVES);
        helper.setBlock(new BlockPos(3, 2, 1), net.minecraft.world.level.block.Blocks.STONE);
        chargeLeafPhase(player, VagrantRules.LEAF_PHASE_REQUIRED_TICKS);
        player.noPhysics = true;

        tickVagrantPlayer(player);
        helper.assertTrue(!player.noPhysics, "Leaf phasing must not use noPhysics");
        helper.assertTrue(VagrantRules.shouldSuppressBlockCollision(player),
                "Leaf phasing should still suppress leaf collisions through the filtered pipeline");

        double leavesX = player.getX();
        player.move(net.minecraft.world.entity.MoverType.SELF, new net.minecraft.world.phys.Vec3(1.0D, 0.0D, 0.0D));
        helper.assertTrue(player.getX() - leavesX > 0.95D, "Filtered leaf phasing should pass through leaves");

        double stoneX = player.getX();
        player.move(net.minecraft.world.entity.MoverType.SELF, new net.minecraft.world.phys.Vec3(1.0D, 0.0D, 0.0D));
        helper.assertTrue(player.getX() - stoneX < 0.95D,
                "Leaf-only filtered phasing must not pass through adjacent stone");

        for (int i = 0; i < VagrantRules.PHASE_REQUIRED_TICKS; i++) {
            tickVagrantPlayer(player);
        }
        double chargedStoneX = player.getX();
        player.move(net.minecraft.world.entity.MoverType.SELF, new net.minecraft.world.phys.Vec3(1.0D, 0.0D, 0.0D));
        helper.assertTrue(player.getX() - chargedStoneX > 0.95D,
                "After the 60 tick charge inside the crown the vagrant should cross the stone");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void vagrantLeafMoveUsesFifteenTickChargeAndTwentyTickGrace(GameTestHelper helper) {
        net.minecraft.server.level.ServerPlayer player = createVagrantPhasePlayer(helper, "leaf-move", new BlockPos(1, 2, 1));
        helper.setBlock(new BlockPos(2, 2, 1), net.minecraft.world.level.block.Blocks.OAK_LEAVES);
        chargeLeafPhase(player, VagrantRules.LEAF_PHASE_REQUIRED_TICKS - 1);
        double blockedX = player.getX();
        player.move(net.minecraft.world.entity.MoverType.SELF, new net.minecraft.world.phys.Vec3(1.0D, 0.0D, 0.0D));
        helper.assertTrue(player.getX() - blockedX < 0.95D, "Leaves must not be passable at 14 ticks");

        player.moveTo(helper.absolutePos(new BlockPos(1, 2, 1)), 0.0F, 0.0F);
        chargeLeafPhase(player, 1);
        double passX = player.getX();
        player.move(net.minecraft.world.entity.MoverType.SELF, new net.minecraft.world.phys.Vec3(1.0D, 0.0D, 0.0D));
        helper.assertTrue(player.getX() - passX > 0.95D, "Leaves must be passable at 15 ticks");

        player.moveTo(helper.absolutePos(new BlockPos(1, 2, 3)), 0.0F, 0.0F);
        helper.setBlock(new BlockPos(2, 2, 3), net.minecraft.world.level.block.Blocks.OAK_LEAVES);
        VagrantEvents.setVagrantInput(player, false, false, false);
        for (int i = 0; i < VagrantRules.LEAF_PHASE_GRACE_TICKS; i++) {
            tickVagrantPlayer(player);
        }
        double graceX = player.getX();
        player.move(net.minecraft.world.entity.MoverType.SELF, new net.minecraft.world.phys.Vec3(1.0D, 0.0D, 0.0D));
        helper.assertTrue(player.getX() - graceX > 0.95D, "Leaf re-entry within 20 ticks should not need a new charge");

        player.moveTo(helper.absolutePos(new BlockPos(1, 2, 5)), 0.0F, 0.0F);
        helper.setBlock(new BlockPos(2, 2, 5), net.minecraft.world.level.block.Blocks.OAK_LEAVES);
        for (int i = 0; i < 21; i++) {
            tickVagrantPlayer(player);
        }
        double expiredX = player.getX();
        player.move(net.minecraft.world.entity.MoverType.SELF, new net.minecraft.world.phys.Vec3(1.0D, 0.0D, 0.0D));
        helper.assertTrue(player.getX() - expiredX < 0.95D, "Leaf permission should expire after the grace window");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void vagrantLeafPermissionDoesNotPassStoneOrBedrock(GameTestHelper helper) {
        net.minecraft.server.level.ServerPlayer player = createVagrantPhasePlayer(helper, "leaf-stone-bedrock", new BlockPos(1, 2, 1));
        player.getPersistentData().putInt(VagrantRules.LEAF_PHASE_CHARGE_TICKS, VagrantRules.LEAF_PHASE_REQUIRED_TICKS);
        VagrantRules.setLeafPhaseActive(player, true);
        helper.setBlock(new BlockPos(2, 2, 1), net.minecraft.world.level.block.Blocks.STONE);
        helper.setBlock(new BlockPos(1, 2, 2), net.minecraft.world.level.block.Blocks.BEDROCK);

        double stoneX = player.getX();
        player.move(net.minecraft.world.entity.MoverType.SELF, new net.minecraft.world.phys.Vec3(1.0D, 0.0D, 0.0D));
        helper.assertTrue(player.getX() - stoneX < 0.95D, "Leaf permission must not pass stone");

        player.moveTo(helper.absolutePos(new BlockPos(1, 2, 1)), 0.0F, 0.0F);
        double bedrockZ = player.getZ();
        player.move(net.minecraft.world.entity.MoverType.SELF, new net.minecraft.world.phys.Vec3(0.0D, 0.0D, 1.0D));
        helper.assertTrue(player.getZ() - bedrockZ < 0.95D, "Leaf permission must not pass bedrock");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void clientFreeVagrantPhasePhysicsKeepsNoPhysicsDisabled(GameTestHelper helper) {
        try {
            java.nio.file.Path source = null;
            for (java.nio.file.Path base = java.nio.file.Paths.get("").toAbsolutePath();
                 base != null && source == null;
                 base = base.getParent()) {
                java.nio.file.Path sourceLayout = base.resolve(java.nio.file.Paths.get(
                        "src",
                        "main",
                        "java",
                        "ua",
                        "meekeyn",
                        "requiemunofficial",
                        "client",
                        "ClientVagrantState.java"
                ));
                java.nio.file.Path mainLayout = base.resolve(java.nio.file.Paths.get(
                        "java",
                        "ua",
                        "meekeyn",
                        "requiemunofficial",
                        "client",
                        "ClientVagrantState.java"
                ));
                if (java.nio.file.Files.exists(sourceLayout)) {
                    source = sourceLayout;
                } else if (java.nio.file.Files.exists(mainLayout)) {
                    source = mainLayout;
                }
            }
            helper.assertTrue(source != null, "ClientVagrantState.java should be discoverable from the GameTest run dir");
            String text = java.nio.file.Files.readString(source);
            int methodStart = text.indexOf("private static void applyVagrantPhasePhysics");
            int nextMethod = text.indexOf("private static boolean isPhaseAttemptInput", methodStart);
            helper.assertTrue(methodStart >= 0 && nextMethod > methodStart,
                    "Client vagrant phase physics method should be present");
            String methodBody = text.substring(methodStart, nextMethod);
            helper.assertTrue(methodBody.contains("player.noPhysics = false;"),
                    "Free restricted VAGRANT client phase physics must keep noPhysics disabled");
            helper.assertTrue(!methodBody.contains("player.noPhysics = true;"),
                    "Free restricted VAGRANT client phase physics must not enable noPhysics");
            helper.succeed();
        } catch (java.io.IOException exception) {
            helper.fail("Could not inspect ClientVagrantState.java: " + exception.getMessage());
        }
    }

    @GameTest(template = "empty")
    public static void vagrantLeafExitRestoresNormalMovementImmediately(GameTestHelper helper) {
        net.minecraft.server.level.ServerPlayer player = createVagrantPhasePlayer(helper, "leaf-exit-speed", new BlockPos(1, 2, 1));
        player.getPersistentData().putInt(VagrantRules.LEAF_PHASE_CHARGE_TICKS, VagrantRules.LEAF_PHASE_REQUIRED_TICKS);
        VagrantRules.setLeafPhaseActive(player, true);
        player.getPersistentData().putInt(VagrantRules.LEAF_PHASE_GRACE_TICK_DATA, VagrantRules.LEAF_PHASE_GRACE_TICKS);
        VagrantEvents.setVagrantInput(player, false, false, false);
        tickVagrantPlayer(player);

        player.setDeltaMovement(0.3D, 0.0D, 0.0D);
        tickVagrantPlayer(player);
        helper.assertTrue(Math.abs(player.getDeltaMovement().x - 0.3D) < 0.0001D,
                "Leaving leaves should not clamp ordinary movement speed during grace");
        helper.assertTrue(!player.isNoGravity(), "Leaving leaves should restore gravity immediately during grace");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void vagrantCanEnterPhaseVerticallyUpAndDown(GameTestHelper helper) {
        net.minecraft.server.level.ServerPlayer up = new net.minecraft.server.level.ServerPlayer(
                helper.getLevel().getServer(),
                helper.getLevel(),
                new com.mojang.authlib.GameProfile(UUID.randomUUID(), "vagrant-up-phase-test")
        );
        up.moveTo(helper.absolutePos(new BlockPos(1, 2, 1)), 0.0F, -70.0F);
        helper.getLevel().setBlock(helper.absolutePos(new BlockPos(1, 4, 1)),
                net.minecraft.world.level.block.Blocks.STONE.defaultBlockState(), 3);
        SoulManager.setState(up, SoulState.VAGRANT);
        VagrantEvents.setVagrantInput(up, true, false, true);
        for (int i = 0; i < VagrantRules.PHASE_REQUIRED_TICKS; i++) {
            VagrantEvents.onPlayerTick(new net.minecraftforge.event.TickEvent.PlayerTickEvent(
                    net.minecraftforge.event.TickEvent.Phase.START,
                    up
            ));
        }
        helper.assertTrue(up.getDeltaMovement().y > 0.0D,
                "Jump plus upward look should enter phasing upward");

        net.minecraft.server.level.ServerPlayer down = new net.minecraft.server.level.ServerPlayer(
                helper.getLevel().getServer(),
                helper.getLevel(),
                new com.mojang.authlib.GameProfile(UUID.randomUUID(), "vagrant-down-phase-test")
        );
        down.moveTo(helper.absolutePos(new BlockPos(4, 3, 1)), 0.0F, 70.0F);
        helper.getLevel().setBlock(helper.absolutePos(new BlockPos(4, 2, 1)),
                net.minecraft.world.level.block.Blocks.STONE.defaultBlockState(), 3);
        SoulManager.setState(down, SoulState.VAGRANT);
        VagrantEvents.setVagrantInput(down, false, true, true);
        down.setShiftKeyDown(true);
        for (int i = 0; i < VagrantRules.PHASE_REQUIRED_TICKS; i++) {
            VagrantEvents.onPlayerTick(new net.minecraftforge.event.TickEvent.PlayerTickEvent(
                    net.minecraftforge.event.TickEvent.Phase.START,
                    down
            ));
        }
        helper.assertTrue(down.getDeltaMovement().y < 0.0D,
                "Shift plus downward look should enter phasing downward");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void soulStrippedBodyKeepsPhysicalResponses(GameTestHelper helper) {
        net.minecraft.world.entity.animal.Cow cow =
                new net.minecraft.world.entity.animal.Cow(EntityType.COW, helper.getLevel());
        cow.moveTo(helper.absolutePos(new BlockPos(1, 2, 1)), 0.0F, 0.0F);
        helper.getLevel().addFreshEntity(cow);

        EntitySoulManager.removeSoul(cow);
        float health = cow.getHealth();
        cow.hurt(helper.getLevel().damageSources().generic(), 2.0F);
        cow.knockback(0.5D, 1.0D, 0.0D);

        helper.assertTrue(cow.isNoAi(), "Soul-stripped body should keep autonomous AI disabled");
        helper.assertTrue(!cow.noPhysics, "Soul-stripped body should keep physical collision enabled");
        helper.assertTrue(!cow.isNoGravity(), "Soul-stripped body should keep gravity enabled");
        helper.assertTrue(cow.getHealth() < health, "Soul-stripped body should still take damage");
        helper.assertTrue(cow.getDeltaMovement().horizontalDistanceSqr() > 0.0D,
                "Soul-stripped body should still receive knockback");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void possessionInputPacketRoundTrips(GameTestHelper helper) {
        ua.meekeyn.requiemunofficial.common.network.PossessionInputPacket packet =
                new ua.meekeyn.requiemunofficial.common.network.PossessionInputPacket(
                        1.0F,
                        -1.0F,
                        true,
                        false,
                        45.0F,
                        -12.5F
                );
        net.minecraft.network.FriendlyByteBuf buffer =
                new net.minecraft.network.FriendlyByteBuf(io.netty.buffer.Unpooled.buffer());
        ua.meekeyn.requiemunofficial.common.network.PossessionInputPacket.encode(packet, buffer);
        ua.meekeyn.requiemunofficial.common.network.PossessionInputPacket decoded =
                ua.meekeyn.requiemunofficial.common.network.PossessionInputPacket.decode(buffer);

        helper.assertTrue(decoded.forward() == 1.0F, "Possession forward input should round-trip");
        helper.assertTrue(decoded.strafe() == -1.0F, "Possession strafe input should round-trip");
        helper.assertTrue(decoded.jumping() && !decoded.shifting(), "Possession buttons should round-trip");
        helper.assertTrue(decoded.yRot() == 45.0F && decoded.xRot() == -12.5F, "Possession rotation should round-trip");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void possessionDoesNotUseRidingAndAllowsSoullessVillagers(GameTestHelper helper) {
        net.minecraft.server.level.ServerPlayer player = new net.minecraft.server.level.ServerPlayer(
                helper.getLevel().getServer(),
                helper.getLevel(),
                new com.mojang.authlib.GameProfile(UUID.randomUUID(), "villager-possession-test")
        );
        SoulManager.setState(player, SoulState.VAGRANT);

        net.minecraft.world.entity.npc.Villager villager =
                new net.minecraft.world.entity.npc.Villager(EntityType.VILLAGER, helper.getLevel());
        villager.moveTo(helper.absolutePos(new BlockPos(1, 2, 1)), 0.0F, 0.0F);
        helper.getLevel().addFreshEntity(villager);
        EntitySoulManager.removeSoul(villager);
        player.getAbilities().mayfly = true;
        player.getAbilities().flying = true;

        helper.assertTrue(
                PossessionManager.tryPossess(player, villager),
                "A soulless villager should be a valid possession host"
        );
        helper.assertTrue(
                SoulManager.getPossessedEntityId(player).equals(villager.getUUID()),
                "Possession should store the villager host UUID"
        );
        helper.assertTrue(
                SoulManager.getState(player) == SoulState.VAGRANT,
                "Possession should keep the player as a vagrant soul"
        );
        helper.assertTrue(
                !player.isPassenger() && player.getVehicle() == null,
                "Possession should not mount the player as a passenger"
        );
        helper.assertTrue(!player.noPhysics, "Player Body must retain block collision");
        helper.assertTrue(!player.isNoGravity(), "Walking Player Body must retain gravity");
        helper.assertTrue(!player.getAbilities().mayfly, "Possession should disable vagrant flight while controlling a body");
        helper.assertTrue(!player.getAbilities().flying, "Possession should stop active vagrant flying while controlling a body");
        PossessionManager.release(player, villager);
        helper.assertTrue(VagrantRules.isRestricted(player), "Releasing possession should leave the player as a free vagrant");
        helper.assertTrue(!player.noPhysics, "Released free vagrants should not globally disable block collision outside phasing");
        helper.assertTrue(!player.isNoGravity(), "Released free vagrants should keep gravity outside phasing");
        helper.assertTrue(player.getAbilities().mayfly, "Releasing possession should restore original mayfly");
        helper.assertTrue(player.getAbilities().flying, "Releasing possession should restore original flying");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void possessedWalkingHostKeepsBlockCollision(GameTestHelper helper) {
        net.minecraft.server.level.ServerPlayer player = new net.minecraft.server.level.ServerPlayer(
                helper.getLevel().getServer(),
                helper.getLevel(),
                new com.mojang.authlib.GameProfile(UUID.randomUUID(), "possessed-zombie-collision-test")
        );
        net.minecraft.world.entity.monster.Zombie zombie =
                new net.minecraft.world.entity.monster.Zombie(EntityType.ZOMBIE, helper.getLevel());
        zombie.moveTo(helper.absolutePos(new BlockPos(1, 2, 1)), 0.0F, 0.0F);
        helper.getLevel().addFreshEntity(zombie);

        SoulManager.setState(player, SoulState.VAGRANT);
        EntitySoulManager.removeSoul(zombie);
        helper.assertTrue(PossessionManager.tryPossess(player, zombie),
                "A vagrant should possess a soulless zombie");

        helper.assertTrue(!player.noPhysics, "Zombie Player Body must keep block collision");
        helper.assertTrue(!player.isNoGravity(), "Zombie Player Body must keep gravity");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void possessedHostDeathReleasesAtHostPositionAndDropsXp(GameTestHelper helper) {
        net.minecraft.server.level.ServerPlayer player = new net.minecraft.server.level.ServerPlayer(
                helper.getLevel().getServer(),
                helper.getLevel(),
                new com.mojang.authlib.GameProfile(UUID.randomUUID(), "host-death-release-test")
        );
        net.minecraft.world.entity.monster.Zombie zombie =
                new net.minecraft.world.entity.monster.Zombie(EntityType.ZOMBIE, helper.getLevel());
        BlockPos hostPos = helper.absolutePos(new BlockPos(3, 2, 3));
        zombie.moveTo(hostPos.getX() + 0.5D, hostPos.getY(), hostPos.getZ() + 0.5D, 37.0F, 11.0F);
        helper.getLevel().addFreshEntity(zombie);

        SoulManager.setState(player, SoulState.VAGRANT);
        EntitySoulManager.removeSoul(zombie);
        player.giveExperienceLevels(8);
        helper.assertTrue(PossessionManager.tryPossess(player, zombie),
                "A vagrant should possess a soulless zombie");

        PossessionManager.releaseFromHostDeathForTest(player, zombie);

        helper.assertTrue(SoulManager.getState(player) == SoulState.VAGRANT,
                "Host death should leave the player as a free vagrant");
        helper.assertTrue(SoulManager.getPossessedEntityId(player) == null,
                "Host death should clear possessed host id");
        helper.assertTrue(player.distanceToSqr(zombie.position()) < 0.01D,
                "Host death should release the vagrant at the host death position");
        helper.assertTrue(player.experienceLevel == 0
                        && player.totalExperience == 0
                        && player.experienceProgress == 0.0F,
                "Host death should apply vanilla death XP loss to the possessor");
        helper.assertTrue(
                !helper.getLevel().getEntitiesOfClass(
                        net.minecraft.world.entity.ExperienceOrb.class,
                        zombie.getBoundingBox().inflate(2.0D)
                ).isEmpty(),
                "Host death should drop vanilla XP orbs at the host position"
        );
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void possessedHostDeathLosesXpWhenKeepInventoryIsEnabled(GameTestHelper helper) {
        helper.getLevel().getGameRules().getRule(net.minecraft.world.level.GameRules.RULE_KEEPINVENTORY)
                .set(true, helper.getLevel().getServer());
        net.minecraft.server.level.ServerPlayer player = new net.minecraft.server.level.ServerPlayer(
                helper.getLevel().getServer(),
                helper.getLevel(),
                new com.mojang.authlib.GameProfile(UUID.randomUUID(), "host-death-keepxp-test")
        );
        net.minecraft.world.entity.monster.Zombie zombie =
                new net.minecraft.world.entity.monster.Zombie(EntityType.ZOMBIE, helper.getLevel());
        helper.getLevel().addFreshEntity(zombie);

        SoulManager.setState(player, SoulState.VAGRANT);
        EntitySoulManager.removeSoul(zombie);
        player.giveExperienceLevels(5);
        helper.assertTrue(PossessionManager.tryPossess(player, zombie),
                "A vagrant should possess a soulless zombie");
        PossessionManager.releaseFromHostDeathForTest(player, zombie);

        helper.assertTrue(player.experienceLevel == 0 && player.totalExperience == 0,
                "Possessed host death should remove soul XP even with keepInventory");
        helper.getLevel().getGameRules().getRule(net.minecraft.world.level.GameRules.RULE_KEEPINVENTORY)
                .set(false, helper.getLevel().getServer());
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void possessedHandEquipmentChangesOnlyWhenSelectedStackChanges(GameTestHelper helper) {
        net.minecraft.server.level.ServerPlayer player = new net.minecraft.server.level.ServerPlayer(
                helper.getLevel().getServer(),
                helper.getLevel(),
                new com.mojang.authlib.GameProfile(UUID.randomUUID(), "host-hand-sync-test")
        );
        net.minecraft.world.entity.monster.Zombie zombie =
                new net.minecraft.world.entity.monster.Zombie(EntityType.ZOMBIE, helper.getLevel());
        helper.getLevel().addFreshEntity(zombie);

        SoulManager.setState(player, SoulState.VAGRANT);
        EntitySoulManager.removeSoul(zombie);
        player.getInventory().items.set(0, new ItemStack(Items.STICK));
        player.getInventory().items.set(1, new ItemStack(Items.IRON_SWORD));
        player.getInventory().selected = 0;
        helper.assertTrue(PossessionManager.tryPossess(player, zombie),
                "A vagrant should possess a soulless zombie");
        helper.assertTrue(zombie.getMainHandItem().is(Items.STICK),
                "Initial selected stack should be mirrored to host hand");

        player.getInventory().selected = 1;
        ua.meekeyn.requiemunofficial.common.possession.PossessionInventoryManager
                .syncPlayerEquipmentToHost(player, zombie);
        ItemStack firstMirror = zombie.getMainHandItem();
        ua.meekeyn.requiemunofficial.common.possession.PossessionInventoryManager
                .syncPlayerEquipmentToHost(player, zombie);
        helper.assertTrue(firstMirror == zombie.getMainHandItem(),
                "Repeated sync with unchanged selected stack should not replace host hand stack");
        helper.assertTrue(zombie.getMainHandItem().is(Items.IRON_SWORD),
                "Changing selected slot should mirror exactly one authoritative hand state");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void possessedZombieUsesVanillaMovementAndFallAuthority(GameTestHelper helper) {
        net.minecraft.server.level.ServerPlayer player = new net.minecraft.server.level.ServerPlayer(
                helper.getLevel().getServer(),
                helper.getLevel(),
                new com.mojang.authlib.GameProfile(UUID.randomUUID(), "host-movement-geometry-test")
        );
        net.minecraft.world.entity.monster.Zombie zombie =
                new net.minecraft.world.entity.monster.Zombie(EntityType.ZOMBIE, helper.getLevel());
        BlockPos start = helper.absolutePos(new BlockPos(1, 2, 1));
        zombie.moveTo(start.getX() + 0.5D, start.getY(), start.getZ() + 0.5D, 0.0F, 0.0F);
        zombie.setOnGround(true);
        helper.getLevel().addFreshEntity(zombie);
        helper.getLevel().setBlock(helper.absolutePos(new BlockPos(1, 2, 2)),
                net.minecraft.world.level.block.Blocks.STONE.defaultBlockState(), 3);

        SoulManager.setState(player, SoulState.VAGRANT);
        EntitySoulManager.removeSoul(zombie);
        helper.assertTrue(PossessionManager.tryPossess(player, zombie),
                "A vagrant should possess a soulless zombie");
        PossessionManager.updatePossessionInput(player,
                new ua.meekeyn.requiemunofficial.common.network.PossessionInputPacket(1.0F, 0.0F, false, false, 0.0F, 0.0F));
        double highestY = zombie.getY();
        for (int i = 0; i < 12; i++) {
            zombie.aiStep();
            highestY = Math.max(highestY, zombie.getY());
        }

        helper.assertTrue(highestY < start.getY() + 0.9D,
                "Possessed zombie should not get artificial one-block step-up");

        float healthBefore = zombie.getHealth();
        player.fallDistance = 0.0F;
        zombie.fallDistance = 5.0F;
        zombie.causeFallDamage(5.0F, 1.0F, helper.getLevel().damageSources().fall());
        helper.assertTrue(zombie.getHealth() < healthBefore,
                "Fall damage authority should remain on the possessed host");
        helper.assertTrue(player.fallDistance == 0.0F,
                "Possessor fall distance should not accumulate host fall state");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void nonVagrantStalePossessionIsReleased(GameTestHelper helper) {
        net.minecraft.server.level.ServerPlayer player = new net.minecraft.server.level.ServerPlayer(
                helper.getLevel().getServer(),
                helper.getLevel(),
                new com.mojang.authlib.GameProfile(UUID.randomUUID(), "stale-possession-test")
        );
        SoulManager.setState(player, SoulState.VAGRANT);

        net.minecraft.world.entity.npc.Villager villager =
                new net.minecraft.world.entity.npc.Villager(EntityType.VILLAGER, helper.getLevel());
        villager.moveTo(helper.absolutePos(new BlockPos(1, 2, 1)), 0.0F, 0.0F);
        helper.getLevel().addFreshEntity(villager);
        EntitySoulManager.removeSoul(villager);

        helper.assertTrue(PossessionManager.tryPossess(player, villager), "Setup possession should succeed");
        SoulManager.setState(player, SoulState.MORTAL);

        PossessionManager.onPlayerTick(new net.minecraftforge.event.TickEvent.PlayerTickEvent(
                net.minecraftforge.event.TickEvent.Phase.END,
                player
        ));

        helper.assertTrue(SoulManager.getPossessedEntityId(player) == null,
                "Non-vagrant players should not keep a possessed host id");
        helper.assertTrue(!player.isInvisible(), "Non-vagrant stale possession cleanup should reveal the player");
        helper.assertTrue(!player.noPhysics, "Non-vagrant stale possession cleanup should restore player collision");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void remnantCannotStartPossession(GameTestHelper helper) {
        net.minecraft.server.level.ServerPlayer player = new net.minecraft.server.level.ServerPlayer(
                helper.getLevel().getServer(),
                helper.getLevel(),
                new com.mojang.authlib.GameProfile(UUID.randomUUID(), "remnant-possession-test")
        );
        SoulManager.setState(player, SoulState.REMNANT);

        net.minecraft.world.entity.animal.Cow cow =
                new net.minecraft.world.entity.animal.Cow(EntityType.COW, helper.getLevel());
        helper.getLevel().addFreshEntity(cow);
        EntitySoulManager.removeSoul(cow);

        helper.assertTrue(
                !PossessionManager.beginPossessionAttemptForTest(player, cow),
                "A remnant with a body must not be able to start possession"
        );
        helper.assertTrue(
                SoulManager.getPossessedEntityId(player) == null,
                "Rejected possession should not store a host"
        );
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void releaseActionSplitsEmbodiedRemnant(GameTestHelper helper) {
        net.minecraft.server.level.ServerPlayer player = new net.minecraft.server.level.ServerPlayer(
                helper.getLevel().getServer(),
                helper.getLevel(),
                new com.mojang.authlib.GameProfile(UUID.randomUUID(), "release-split-test")
        );
        player.moveTo(helper.absolutePos(new BlockPos(1, 2, 1)), 0.0F, 0.0F);
        SoulManager.setState(player, SoulState.REMNANT);

        PossessionManager.handleAction(
                player,
                ua.meekeyn.requiemunofficial.common.network.PossessionActionPacket.Action.RELEASE
        );

        helper.assertTrue(SoulManager.getState(player) == SoulState.VAGRANT,
                "Release action should split an embodied remnant into vagrant state");
        helper.assertTrue(SoulManager.getPlayerShellId(player) != null,
                "Release action should leave an abandoned player shell");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void remnantDeathUsesVanillaDeathLifecycle(GameTestHelper helper) {
        net.minecraft.server.level.ServerPlayer player = new net.minecraft.server.level.ServerPlayer(
                helper.getLevel().getServer(),
                helper.getLevel(),
                new com.mojang.authlib.GameProfile(UUID.randomUUID(), "death-shell-test")
        );
        player.moveTo(helper.absolutePos(new BlockPos(1, 2, 1)), 0.0F, 0.0F);
        SoulManager.setState(player, SoulState.REMNANT);
        player.experienceLevel = 7;
        player.totalExperience = 91;
        player.experienceProgress = 0.5F;

        net.minecraftforge.event.entity.living.LivingDeathEvent event =
                new net.minecraftforge.event.entity.living.LivingDeathEvent(
                player,
                player.damageSources().generic()
        );
        SoulLifecycleManager.onLivingDeath(event);

        helper.assertTrue(!event.isCanceled(), "A remnant death should continue through vanilla death");
        helper.assertTrue(SoulManager.getState(player) == SoulState.REMNANT,
                "The old dying player should keep remnant type for clone lifecycle");
        helper.assertTrue(SoulManager.getPlayerShellId(player) == null,
                "A normal remnant death must not create an abandoned player shell");
        helper.assertTrue(player.experienceLevel == 7
                        && player.totalExperience == 91
                        && player.experienceProgress == 0.5F,
                "Death hook must not pre-clear or preserve XP outside vanilla death handling");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void mortalDeathHookDoesNotCreateVagrantPlayer(GameTestHelper helper) {
        net.minecraft.server.level.ServerPlayer player = new net.minecraft.server.level.ServerPlayer(
                helper.getLevel().getServer(),
                helper.getLevel(),
                new com.mojang.authlib.GameProfile(UUID.randomUUID(), "mortal-death-test")
        );
        SoulManager.setState(player, SoulState.MORTAL);
        ua.meekeyn.requiemunofficial.common.soul.InitialSoulChoiceManager.markChosenForTest(player);

        net.minecraftforge.event.entity.living.LivingDeathEvent event =
                new net.minecraftforge.event.entity.living.LivingDeathEvent(
                player,
                player.damageSources().generic()
        );
        SoulLifecycleManager.onLivingDeath(event);

        helper.assertTrue(!event.isCanceled(), "A mortal death should keep vanilla death behavior");
        helper.assertTrue(SoulManager.getState(player) == SoulState.MORTAL,
                "A mortal death should not turn the player into a vagrant");
        helper.assertTrue(SoulManager.getPlayerShellId(player) == null,
                "A mortal death should not create an abandoned player shell");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void firstMortalDeathStartsSuspension(GameTestHelper helper) {
        TestServerPlayer player = new TestServerPlayer(helper, "choice-suspend-test");
        SoulManager.setState(player, SoulState.MORTAL);

        net.minecraftforge.event.entity.living.LivingDeathEvent event =
                new net.minecraftforge.event.entity.living.LivingDeathEvent(
                        player,
                        player.damageSources().generic()
                );
        SoulLifecycleManager.onLivingDeath(event);

        helper.assertTrue(event.isCanceled(), "First lethal mortal death should be suspended");
        helper.assertTrue(DeathSuspensionManager.isSuspended(player), "Player should enter death suspension");
        helper.assertTrue(player.getHealth() >= 1.0F, "Suspended player should be kept alive at one health");
        helper.assertTrue(DeathSuspensionManager.countdownForTest(player) == 20,
                "Suspended death should wait 20 ticks before opening choice");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void suspendedDamageDoesNotCreateSecondSuspension(GameTestHelper helper) {
        TestServerPlayer player = new TestServerPlayer(helper, "choice-repeat-damage-test");
        SoulManager.setState(player, SoulState.MORTAL);

        SoulLifecycleManager.onLivingDeath(new net.minecraftforge.event.entity.living.LivingDeathEvent(
                player,
                player.damageSources().generic()
        ));
        net.minecraftforge.event.entity.living.LivingHurtEvent hurtEvent =
                new net.minecraftforge.event.entity.living.LivingHurtEvent(
                        player,
                        player.damageSources().magic(),
                        10.0F
                );
        DeathSuspensionManager.onLivingHurt(hurtEvent);
        net.minecraftforge.event.entity.living.LivingDeathEvent secondDeath =
                new net.minecraftforge.event.entity.living.LivingDeathEvent(
                        player,
                        player.damageSources().magic()
                );
        SoulLifecycleManager.onLivingDeath(secondDeath);

        helper.assertTrue(hurtEvent.isCanceled(), "Damage during suspension should be canceled");
        helper.assertTrue(secondDeath.isCanceled(), "Repeated death during suspension should be canceled");
        helper.assertTrue(DeathSuspensionManager.countdownForTest(player) == 20,
                "Repeated damage should not restart the death suspension countdown");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void deathSuspensionCountdownOpensChoiceOnce(GameTestHelper helper) {
        TestServerPlayer player = new TestServerPlayer(helper, "choice-countdown-test");
        SoulManager.setState(player, SoulState.MORTAL);
        SoulLifecycleManager.onLivingDeath(new net.minecraftforge.event.entity.living.LivingDeathEvent(
                player,
                player.damageSources().generic()
        ));

        for (int i = 0; i < 25; i++) {
            DeathSuspensionManager.onPlayerTick(new net.minecraftforge.event.TickEvent.PlayerTickEvent(
                    net.minecraftforge.event.TickEvent.Phase.START,
                    player
            ));
        }
        helper.assertTrue(DeathSuspensionManager.hasOpenedChoiceForTest(player),
                "Choice should be marked opened after the 20 tick delay");
        int countdown = DeathSuspensionManager.countdownForTest(player);
        DeathSuspensionManager.onPlayerTick(new net.minecraftforge.event.TickEvent.PlayerTickEvent(
                net.minecraftforge.event.TickEvent.Phase.START,
                player
        ));
        helper.assertTrue(DeathSuspensionManager.countdownForTest(player) == countdown,
                "Already opened choice should not restart or reopen");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void mortalChoiceCompletesSuspension(GameTestHelper helper) {
        TestServerPlayer player = new TestServerPlayer(helper, "choice-mortal-test");
        SoulManager.setState(player, SoulState.MORTAL);
        SoulLifecycleManager.onLivingDeath(new net.minecraftforge.event.entity.living.LivingDeathEvent(
                player,
                player.damageSources().generic()
        ));

        ua.meekeyn.requiemunofficial.common.soul.InitialSoulChoiceManager.choose(player, SoulState.MORTAL);

        helper.assertTrue(!DeathSuspensionManager.isSuspended(player), "Mortal choice should clear suspension");
        helper.assertTrue(SoulManager.getState(player) == SoulState.MORTAL,
                "Mortal choice should keep the player mortal");
        helper.assertTrue(player.requiemTestDied, "Mortal choice should resume the original death");
        helper.assertTrue(ua.meekeyn.requiemunofficial.common.soul.InitialSoulChoiceManager.hasChosen(player),
                "Mortal choice should mark the initial choice complete");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void remnantChoiceCompletesSuspensionWithoutImmediateVagrant(GameTestHelper helper) {
        TestServerPlayer player = new TestServerPlayer(helper, "choice-remnant-test");
        SoulManager.setState(player, SoulState.MORTAL);
        SoulLifecycleManager.onLivingDeath(new net.minecraftforge.event.entity.living.LivingDeathEvent(
                player,
                player.damageSources().generic()
        ));

        ua.meekeyn.requiemunofficial.common.soul.InitialSoulChoiceManager.choose(player, SoulState.REMNANT);

        helper.assertTrue(!DeathSuspensionManager.isSuspended(player), "Remnant choice should clear suspension");
        helper.assertTrue(SoulManager.getState(player) == SoulState.REMNANT,
                "Choice packet should set remnant type but not directly create a vagrant");
        helper.assertTrue(player.requiemTestDied, "Remnant choice should resume the original death");

        ua.meekeyn.requiemunofficial.common.soul.InitialSoulChoiceManager.choose(player, SoulState.MORTAL);
        helper.assertTrue(SoulManager.getState(player) == SoulState.REMNANT,
                "Initial choice packet should not be applicable twice");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void completedInitialChoicePreventsFutureSuspension(GameTestHelper helper) {
        TestServerPlayer player = new TestServerPlayer(helper, "choice-done-test");
        SoulManager.setState(player, SoulState.MORTAL);
        SoulLifecycleManager.onLivingDeath(new net.minecraftforge.event.entity.living.LivingDeathEvent(
                player,
                player.damageSources().generic()
        ));
        ua.meekeyn.requiemunofficial.common.soul.InitialSoulChoiceManager.choose(player, SoulState.MORTAL);
        player.requiemTestDied = false;

        net.minecraftforge.event.entity.living.LivingDeathEvent secondDeath =
                new net.minecraftforge.event.entity.living.LivingDeathEvent(
                        player,
                        player.damageSources().generic()
                );
        SoulLifecycleManager.onLivingDeath(secondDeath);

        helper.assertTrue(!secondDeath.isCanceled(),
                "Player who completed initial choice should not be suspended again");
        helper.assertTrue(!DeathSuspensionManager.isSuspended(player),
                "Completed choice should leave no stale suspension");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void forcedRemnantFirstDeathUsesVanillaDeathPath(GameTestHelper helper) {
        int oldMode = helper.getLevel().getGameRules().getInt(
                ua.meekeyn.requiemunofficial.common.init.ModGameRules.STARTING_REMNANT_TYPE
        );
        helper.getLevel().getGameRules()
                .getRule(ua.meekeyn.requiemunofficial.common.init.ModGameRules.STARTING_REMNANT_TYPE)
                .set(ua.meekeyn.requiemunofficial.common.init.ModGameRules.STARTING_REMNANT_FORCE_REMNANT,
                        helper.getLevel().getServer());
        try {
            TestServerPlayer player = new TestServerPlayer(helper, "choice-forced-remnant-test");
            SoulManager.setState(player, SoulState.MORTAL);

            net.minecraftforge.event.entity.living.LivingDeathEvent event =
                    new net.minecraftforge.event.entity.living.LivingDeathEvent(
                            player,
                            player.damageSources().generic()
                    );
            SoulLifecycleManager.onLivingDeath(event);

            helper.assertTrue(!event.isCanceled(), "Forced remnant first death should continue vanilla death");
            helper.assertTrue(!DeathSuspensionManager.isSuspended(player),
                    "Forced remnant mode should not open the choice UI");
            helper.assertTrue(SoulManager.getState(player) == SoulState.REMNANT,
                    "Forced remnant mode should apply remnant type before vanilla death resumes");
        } finally {
            helper.getLevel().getGameRules()
                    .getRule(ua.meekeyn.requiemunofficial.common.init.ModGameRules.STARTING_REMNANT_TYPE)
                    .set(oldMode, helper.getLevel().getServer());
        }
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void humanityExperienceDropIgnoresUntrackedDeaths(GameTestHelper helper) {
        net.minecraft.world.entity.animal.Cow cow =
                new net.minecraft.world.entity.animal.Cow(EntityType.COW, helper.getLevel());

        net.minecraftforge.event.entity.living.LivingExperienceDropEvent event =
                new net.minecraftforge.event.entity.living.LivingExperienceDropEvent(cow, null, 5);
        HumanityKillManager.onExperienceDrop(event);

        helper.assertTrue(event.getDroppedExperience() == 5,
                "Untracked deaths should keep original experience and must not crash");
        helper.succeed();
    }

    private static final class TestServerPlayer extends net.minecraft.server.level.ServerPlayer {
        private boolean requiemTestDied;

        private TestServerPlayer(GameTestHelper helper, String name) {
            super(
                    helper.getLevel().getServer(),
                    helper.getLevel(),
                    new com.mojang.authlib.GameProfile(UUID.randomUUID(), name)
            );
        }

        @Override
        public boolean hurt(net.minecraft.world.damagesource.DamageSource source, float amount) {
            if (amount == Float.MAX_VALUE) {
                this.requiemTestDied = true;
                return true;
            }
            return super.hurt(source, amount);
        }

        @Override
        public void die(net.minecraft.world.damagesource.DamageSource source) {
            this.requiemTestDied = true;
        }
    }

    @GameTest(template = "empty")
    public static void vagrantVisualsClearWhenStateChanges(GameTestHelper helper) {
        net.minecraft.server.level.ServerPlayer player = new net.minecraft.server.level.ServerPlayer(
                helper.getLevel().getServer(),
                helper.getLevel(),
                new com.mojang.authlib.GameProfile(UUID.randomUUID(), "vagrant-visual-test")
        );

        SoulManager.setState(player, SoulState.VAGRANT);
        VagrantEvents.updateVagrantStateForTest(player);
        helper.assertTrue(player.isInvisible(), "Restricted vagrant souls should be invisible");
        helper.assertTrue(!player.noPhysics, "Restricted vagrant souls should keep noPhysics disabled");

        SoulManager.setState(player, SoulState.REMNANT);
        VagrantEvents.updateVagrantStateForTest(player);
        helper.assertTrue(!player.isInvisible(), "Leaving vagrant state should clear vagrant invisibility");
        helper.assertTrue(!player.noPhysics, "Leaving vagrant state should restore ordinary collision physics");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void possessedVagrantKeepsWorldInteractionProxy(GameTestHelper helper) {
        net.minecraft.server.level.ServerPlayer player = new net.minecraft.server.level.ServerPlayer(
                helper.getLevel().getServer(),
                helper.getLevel(),
                new com.mojang.authlib.GameProfile(UUID.randomUUID(), "possessed-interaction-test")
        );
        net.minecraft.world.entity.animal.Cow cow =
                new net.minecraft.world.entity.animal.Cow(EntityType.COW, helper.getLevel());
        cow.moveTo(helper.absolutePos(new BlockPos(1, 2, 1)), 0.0F, 0.0F);
        helper.getLevel().addFreshEntity(cow);

        SoulManager.setState(player, SoulState.VAGRANT);
        EntitySoulManager.removeSoul(cow);
        helper.assertTrue(PossessionManager.tryPossess(player, cow), "A vagrant should possess a soulless cow");
        helper.assertTrue(
                !VagrantRules.isRestricted(player),
                "Possessed players must not be treated as free incorporeal vagrants"
        );
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void possessedHostMovesFromInputPacket(GameTestHelper helper) {
        net.minecraft.server.level.ServerPlayer player = new net.minecraft.server.level.ServerPlayer(
                helper.getLevel().getServer(),
                helper.getLevel(),
                new com.mojang.authlib.GameProfile(UUID.randomUUID(), "possessed-move-test")
        );
        net.minecraft.world.entity.animal.Cow cow =
                new net.minecraft.world.entity.animal.Cow(EntityType.COW, helper.getLevel());
        cow.moveTo(helper.absolutePos(new BlockPos(1, 2, 1)), 0.0F, 0.0F);
        helper.getLevel().addFreshEntity(cow);

        SoulManager.setState(player, SoulState.VAGRANT);
        EntitySoulManager.removeSoul(cow);
        helper.assertTrue(PossessionManager.tryPossess(player, cow), "A vagrant should possess a soulless cow");

        double before = cow.position().lengthSqr();
        PossessionManager.updatePossessionInput(
                player,
                new ua.meekeyn.requiemunofficial.common.network.PossessionInputPacket(
                        1.0F,
                        0.0F,
                        false,
                        false,
                        0.0F,
                        0.0F
                )
        );
        player.zza = 1.0F;
        player.aiStep();
        PossessionManager.syncBeta16HostToPlayer(player, cow);

        helper.assertTrue(cow.position().lengthSqr() != before,
                "Possessed host should move when forward input is received");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void possessedHostReceivesZeroInputBeforeTravel(GameTestHelper helper) {
        net.minecraft.server.level.ServerPlayer player = new net.minecraft.server.level.ServerPlayer(
                helper.getLevel().getServer(),
                helper.getLevel(),
                new com.mojang.authlib.GameProfile(UUID.randomUUID(), "possessed-zero-input-test")
        );
        net.minecraft.world.entity.monster.Husk husk =
                new net.minecraft.world.entity.monster.Husk(EntityType.HUSK, helper.getLevel());
        husk.moveTo(helper.absolutePos(new BlockPos(1, 2, 1)), 0.0F, 0.0F);
        husk.setOnGround(true);
        helper.getLevel().addFreshEntity(husk);

        SoulManager.setState(player, SoulState.VAGRANT);
        EntitySoulManager.removeSoul(husk);
        helper.assertTrue(PossessionManager.tryPossess(player, husk),
                "A vagrant should possess a soulless husk");

        PossessionManager.updatePossessionInput(
                player,
                new ua.meekeyn.requiemunofficial.common.network.PossessionInputPacket(
                        1.0F,
                        -1.0F,
                        false,
                        false,
                        0.0F,
                        0.0F
                )
        );
        player.zza = 1;
        player.xxa = -1;
        player.aiStep();
        PossessionManager.syncBeta16HostToPlayer(player, husk);
        helper.assertTrue(player.zza > 0.0F && player.xxa < 0.0F,
                "Non-zero packet input should reach the host before travel");

        PossessionManager.updatePossessionInput(
                player,
                new ua.meekeyn.requiemunofficial.common.network.PossessionInputPacket(
                        0.0F,
                        0.0F,
                        false,
                        false,
                        0.0F,
                        0.0F
                )
        );
        player.zza = player.xxa = 0;
        player.aiStep();
        PossessionManager.syncBeta16HostToPlayer(player, husk);
        helper.assertTrue(player.zza == 0.0F && player.xxa == 0.0F,
                "Zero packet input should clear host movement fields before travel");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void possessedHostJumpsFromInputPacket(GameTestHelper helper) {
        net.minecraft.server.level.ServerPlayer player = new net.minecraft.server.level.ServerPlayer(
                helper.getLevel().getServer(),
                helper.getLevel(),
                new com.mojang.authlib.GameProfile(UUID.randomUUID(), "possessed-jump-test")
        );
        net.minecraft.world.entity.animal.Cow cow =
                new net.minecraft.world.entity.animal.Cow(EntityType.COW, helper.getLevel());
        cow.moveTo(helper.absolutePos(new BlockPos(1, 2, 1)), 0.0F, 0.0F);
        cow.setOnGround(true);
        helper.getLevel().addFreshEntity(cow);

        SoulManager.setState(player, SoulState.VAGRANT);
        EntitySoulManager.removeSoul(cow);
        helper.assertTrue(PossessionManager.tryPossess(player, cow), "A vagrant should possess a soulless cow");

        PossessionManager.updatePossessionInput(
                player,
                new ua.meekeyn.requiemunofficial.common.network.PossessionInputPacket(
                        0.0F,
                        0.0F,
                        true,
                        false,
                        0.0F,
                        0.0F
                )
        );
        player.setOnGround(true);
        player.setJumping(true);
        player.aiStep();
        PossessionManager.syncBeta16HostToPlayer(player, cow);

        helper.assertTrue(cow.getDeltaMovement().y > 0.0D,
                "Possessed grounded host should jump when jump input is received");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void possessedHostLookStaysOnControlledRotationAfterAi(GameTestHelper helper) {
        net.minecraft.server.level.ServerPlayer player = new net.minecraft.server.level.ServerPlayer(
                helper.getLevel().getServer(),
                helper.getLevel(),
                new com.mojang.authlib.GameProfile(UUID.randomUUID(), "possessed-look-control-test")
        );
        net.minecraft.world.entity.monster.Zombie zombie =
                new net.minecraft.world.entity.monster.Zombie(EntityType.ZOMBIE, helper.getLevel());
        zombie.moveTo(helper.absolutePos(new BlockPos(1, 2, 1)), 0.0F, 0.0F);
        zombie.setOnGround(true);
        helper.getLevel().addFreshEntity(zombie);

        net.minecraft.world.entity.animal.Cow target =
                new net.minecraft.world.entity.animal.Cow(EntityType.COW, helper.getLevel());
        target.moveTo(helper.absolutePos(new BlockPos(4, 2, 1)), 0.0F, 0.0F);
        helper.getLevel().addFreshEntity(target);

        SoulManager.setState(player, SoulState.VAGRANT);
        EntitySoulManager.removeSoul(zombie);
        helper.assertTrue(PossessionManager.tryPossess(player, zombie),
                "A vagrant should possess a soulless zombie");

        PossessionManager.updatePossessionInput(
                player,
                new ua.meekeyn.requiemunofficial.common.network.PossessionInputPacket(
                        0.0F,
                        0.0F,
                        false,
                        false,
                        75.0F,
                        -12.0F
                )
        );
        zombie.getLookControl().setLookAt(target, 30.0F, 30.0F);
        PossessionManager.syncBeta16HostToPlayer(player, zombie);
        zombie.aiStep();

        helper.assertTrue(Math.abs(zombie.getYRot() - 75.0F) < 0.001F,
                "Possessed host yaw should remain controlled after mob look logic");
        helper.assertTrue(Math.abs(zombie.yHeadRot - 75.0F) < 0.001F,
                "Possessed host head yaw should remain controlled after mob look logic");
        helper.assertTrue(Math.abs(zombie.yBodyRot - 75.0F) < 0.001F,
                "Possessed host body yaw should remain controlled after mob look logic");
        helper.assertTrue(Math.abs(zombie.getXRot() + 12.0F) < 0.001F,
                "Possessed host pitch should remain controlled after mob look logic");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void possessedWalkingHostCannotFlyByHoldingJump(GameTestHelper helper) {
        net.minecraft.server.level.ServerPlayer player = new net.minecraft.server.level.ServerPlayer(
                helper.getLevel().getServer(),
                helper.getLevel(),
                new com.mojang.authlib.GameProfile(UUID.randomUUID(), "possessed-no-flight-test")
        );
        net.minecraft.world.entity.monster.Zombie zombie =
                new net.minecraft.world.entity.monster.Zombie(EntityType.ZOMBIE, helper.getLevel());
        zombie.moveTo(helper.absolutePos(new BlockPos(1, 2, 1)), 0.0F, 0.0F);
        zombie.setOnGround(false);
        zombie.setDeltaMovement(0.0D, 0.2D, 0.0D);
        helper.getLevel().addFreshEntity(zombie);

        SoulManager.setState(player, SoulState.VAGRANT);
        EntitySoulManager.removeSoul(zombie);
        helper.assertTrue(PossessionManager.tryPossess(player, zombie),
                "A vagrant should possess a soulless zombie");

        PossessionManager.updatePossessionInput(
                player,
                new ua.meekeyn.requiemunofficial.common.network.PossessionInputPacket(
                        0.0F,
                        0.0F,
                        true,
                        false,
                        0.0F,
                        0.0F
                )
        );
        player.setJumping(true);
        player.aiStep();
        PossessionManager.syncBeta16HostToPlayer(player, zombie);

        helper.assertTrue(!player.isNoGravity(), "Possessed zombies should keep normal gravity");
        helper.assertTrue(player.getDeltaMovement().y < 0.2D,
                "Holding jump while airborne should not keep boosting a walking host upward; deltaY="
                        + player.getDeltaMovement().y + ", onGround=" + zombie.onGround());
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void possessedWalkingHostFallsWhenMovingOffLedge(GameTestHelper helper) {
        net.minecraft.server.level.ServerPlayer player = new net.minecraft.server.level.ServerPlayer(
                helper.getLevel().getServer(),
                helper.getLevel(),
                new com.mojang.authlib.GameProfile(UUID.randomUUID(), "possessed-ledge-test")
        );
        net.minecraft.world.entity.monster.Zombie zombie =
                new net.minecraft.world.entity.monster.Zombie(EntityType.ZOMBIE, helper.getLevel());
        zombie.moveTo(helper.absolutePos(new BlockPos(1, 6, 1)), 0.0F, 0.0F);
        zombie.setOnGround(false);
        zombie.setDeltaMovement(0.0D, 0.0D, 0.0D);
        helper.getLevel().addFreshEntity(zombie);

        SoulManager.setState(player, SoulState.VAGRANT);
        EntitySoulManager.removeSoul(zombie);
        helper.assertTrue(PossessionManager.tryPossess(player, zombie),
                "A vagrant should possess a soulless zombie");

        PossessionManager.updatePossessionInput(
                player,
                new ua.meekeyn.requiemunofficial.common.network.PossessionInputPacket(
                        1.0F,
                        0.0F,
                        false,
                        false,
                        0.0F,
                        0.0F
                )
        );
        player.zza = 1.0F;
        player.setJumping(false);
        player.aiStep();
        PossessionManager.syncBeta16HostToPlayer(player, zombie);

        helper.assertTrue(!player.isNoGravity(), "Possessed walking hosts should keep gravity near ledges");
        helper.assertTrue(player.getDeltaMovement().y < 0.0D,
                "A possessed walking host should fall when moving without ground support; deltaY="
                        + player.getDeltaMovement().y + ", onGround=" + zombie.onGround()
                        + ", pos=" + zombie.position());
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void possessedWalkingHostUsesVanillaGravity(GameTestHelper helper) {
        net.minecraft.server.level.ServerPlayer player = new net.minecraft.server.level.ServerPlayer(
                helper.getLevel().getServer(),
                helper.getLevel(),
                new com.mojang.authlib.GameProfile(UUID.randomUUID(), "possessed-gravity-test")
        );
        net.minecraft.world.entity.animal.Fox fox =
                new net.minecraft.world.entity.animal.Fox(EntityType.FOX, helper.getLevel());
        net.minecraft.world.entity.npc.Villager villager =
                new net.minecraft.world.entity.npc.Villager(EntityType.VILLAGER, helper.getLevel());
        ua.meekeyn.requiemunofficial.common.entity.CuredVillagerEntity curedVillager =
                new ua.meekeyn.requiemunofficial.common.entity.CuredVillagerEntity(
                        ua.meekeyn.requiemunofficial.common.init.ModEntities.CURED_VILLAGER.get(),
                        helper.getLevel()
                );
        fox.moveTo(helper.absolutePos(new BlockPos(1, 6, 1)), 0.0F, 0.0F);
        fox.setOnGround(false);
        fox.setDeltaMovement(0.0D, 0.0D, 0.0D);
        helper.getLevel().addFreshEntity(fox);

        SoulManager.setState(player, SoulState.VAGRANT);
        EntitySoulManager.removeSoul(fox);
        helper.assertTrue(PossessionManager.tryPossess(player, fox),
                "A vagrant should possess a soulless fox");

        PossessionManager.updatePossessionInput(
                player,
                new ua.meekeyn.requiemunofficial.common.network.PossessionInputPacket(
                        0.0F,
                        0.0F,
                        false,
                        false,
                        0.0F,
                        0.0F
                )
        );
        player.aiStep();
        PossessionManager.syncBeta16HostToPlayer(player, fox);

        helper.assertTrue(!player.isNoGravity(), "Possessed walking hosts should keep gravity enabled");
        helper.assertTrue(Math.abs(fox.getDeltaMovement().y + 0.06D * 0.98D) < 1.0E-6D,
                "Fox profile gravity must be integrated once with vanilla travel drag; deltaY="
                        + fox.getDeltaMovement().y + ", onGround=" + fox.onGround()
                        + ", pos=" + fox.position());
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void possessedItemUseFindsCameraControlledHost(GameTestHelper helper) {
        net.minecraft.server.level.ServerPlayer player = new net.minecraft.server.level.ServerPlayer(
                helper.getLevel().getServer(),
                helper.getLevel(),
                new com.mojang.authlib.GameProfile(UUID.randomUUID(), "possessed-item-test")
        );
        GameTestSupport.attachConnection(helper, player);
        net.minecraft.world.entity.animal.Fox fox =
                new net.minecraft.world.entity.animal.Fox(EntityType.FOX, helper.getLevel());
        fox.moveTo(helper.absolutePos(new BlockPos(1, 2, 1)), 0.0F, 0.0F);
        helper.getLevel().addFreshEntity(fox);

        SoulManager.setState(player, SoulState.VAGRANT);
        EntitySoulManager.removeSoul(fox);
        helper.assertTrue(PossessionManager.tryPossess(player, fox), "A vagrant should possess a soulless fox");
        helper.assertTrue(player.getVehicle() == null, "Camera possession should not mount the player on the host");

        fox.setHealth(Math.max(1.0F, fox.getMaxHealth() - 4.0F));
        float before = fox.getHealth();
        ItemStack berries = new ItemStack(Items.SWEET_BERRIES);
        player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, berries);
        helper.assertTrue(
                PossessedItemManager.tryUsePossessedItemForTest(player, fox, berries),
                "Possessed item use should find the camera-controlled fox and start eating"
        );
        helper.assertTrue(player.isUsingItem(), "Fox berries must be held for their configured use time");
        PossessedItemManager.finishPossessedItemUse(
                helper.getLevel(), player, berries, net.minecraft.world.InteractionHand.MAIN_HAND);
        helper.assertTrue(fox.getHealth() > before, "The possessed fox should be healed by its configured food");
        helper.assertTrue(berries.isEmpty(), "Survival possessed food use should consume the item");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void possessedOffhandItemUseUsesHostOverride(GameTestHelper helper) {
        net.minecraft.server.level.ServerPlayer player = new net.minecraft.server.level.ServerPlayer(
                helper.getLevel().getServer(),
                helper.getLevel(),
                new com.mojang.authlib.GameProfile(UUID.randomUUID(), "possessed-offhand-item-test")
        );
        GameTestSupport.attachConnection(helper, player);
        net.minecraft.world.entity.animal.Fox fox =
                new net.minecraft.world.entity.animal.Fox(EntityType.FOX, helper.getLevel());
        fox.moveTo(helper.absolutePos(new BlockPos(1, 2, 1)), 0.0F, 0.0F);
        helper.getLevel().addFreshEntity(fox);

        SoulManager.setState(player, SoulState.VAGRANT);
        EntitySoulManager.removeSoul(fox);
        helper.assertTrue(PossessionManager.tryPossess(player, fox), "A vagrant should possess a soulless fox");

        fox.setHealth(Math.max(1.0F, fox.getMaxHealth() - 4.0F));
        float before = fox.getHealth();
        ItemStack berries = new ItemStack(Items.SWEET_BERRIES);
        player.setItemInHand(net.minecraft.world.InteractionHand.OFF_HAND, berries);
        helper.assertTrue(
                PossessedItemManager.tryUsePossessedItemForTest(
                        player,
                        fox,
                        berries,
                        net.minecraft.world.InteractionHand.OFF_HAND
                ),
                "Possessed offhand item use should start eating for the host"
        );
        helper.assertTrue(player.isUsingItem(), "Offhand fox berries must be held for their configured use time");
        PossessedItemManager.finishPossessedItemUse(
                helper.getLevel(), player, berries, net.minecraft.world.InteractionHand.OFF_HAND);
        helper.assertTrue(fox.getHealth() > before, "The possessed fox should be healed by offhand food");
        helper.assertTrue(berries.isEmpty(), "Survival possessed offhand food use should consume the item");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void shellSplitDoesNotSpawnDetachedSoulOrb(GameTestHelper helper) {
        net.minecraft.server.level.ServerPlayer player = new net.minecraft.server.level.ServerPlayer(
                helper.getLevel().getServer(),
                helper.getLevel(),
                new com.mojang.authlib.GameProfile(UUID.randomUUID(), "shell-no-orb-test")
        );
        player.moveTo(helper.absolutePos(new BlockPos(1, 2, 1)), 0.0F, 0.0F);

        SoulLifecycleManager.createShell(player);
        java.util.List<ua.meekeyn.requiemunofficial.common.entity.ReleasedSoulEntity> souls =
                helper.getLevel().getEntitiesOfClass(
                        ua.meekeyn.requiemunofficial.common.entity.ReleasedSoulEntity.class,
                        new net.minecraft.world.phys.AABB(helper.absolutePos(new BlockPos(0, 0, 0))).inflate(8.0D)
                );
        helper.assertTrue(souls.isEmpty(), "Splitting a player shell should not spawn a detached soul orb");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void requiemCommandTreeIsRegistered(GameTestHelper helper) {
        com.mojang.brigadier.tree.CommandNode<net.minecraft.commands.CommandSourceStack> root =
                helper.getLevel().getServer().getCommands().getDispatcher().getRoot().getChild("requiem");
        helper.assertTrue(root != null, "The /requiem command root must be registered on the server dispatcher");
        helper.assertTrue(root.getChild("soul") != null, "/requiem soul must be registered");
        helper.assertTrue(root.getChild("remnant") != null, "/requiem remnant must be registered");
        helper.assertTrue(root.getChild("mortal") != null, "/requiem mortal must be registered");
        helper.assertTrue(root.getChild("vagrant") != null, "/requiem vagrant must be registered");
        helper.assertTrue(root.getChild("possession") != null, "/requiem possession must be registered");
        helper.assertTrue(root.getChild("shell") != null, "/requiem shell must be registered");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void survivalVagrantEntityInteractStartsPossession(GameTestHelper helper) {
        net.minecraft.server.level.ServerPlayer player = new net.minecraft.server.level.ServerPlayer(
                helper.getLevel().getServer(),
                helper.getLevel(),
                new com.mojang.authlib.GameProfile(UUID.randomUUID(), "survival-possess-event")
        );
        player.moveTo(helper.absolutePos(new BlockPos(1, 2, 1)), 0.0F, 0.0F);
        SoulManager.setState(player, SoulState.VAGRANT);

        net.minecraft.world.entity.animal.Cow cow =
                new net.minecraft.world.entity.animal.Cow(EntityType.COW, helper.getLevel());
        cow.moveTo(helper.absolutePos(new BlockPos(2, 2, 1)), 0.0F, 0.0F);
        helper.getLevel().addFreshEntity(cow);
        EntitySoulManager.removeSoul(cow);

        net.minecraftforge.event.entity.player.PlayerInteractEvent.EntityInteract event =
                new net.minecraftforge.event.entity.player.PlayerInteractEvent.EntityInteract(
                        player,
                        net.minecraft.world.InteractionHand.MAIN_HAND,
                        cow
                );
        PossessionManager.onEntityInteract(event);
        helper.assertTrue(event.isCanceled(), "Possession interaction should consume the survival vagrant entity use");
        helper.assertTrue(PossessionManager.hasPendingPossessionForTest(player),
                "Survival vagrant entity use should start a pending possession");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void creativeVagrantEntityInteractLetsItemUseWinBeforePossession(GameTestHelper helper) {
        net.minecraft.server.level.ServerPlayer player = new net.minecraft.server.level.ServerPlayer(
                helper.getLevel().getServer(),
                helper.getLevel(),
                new com.mojang.authlib.GameProfile(UUID.randomUUID(), "creative-item-priority")
        );
        player.getAbilities().instabuild = true;
        SoulManager.setState(player, SoulState.VAGRANT);

        net.minecraft.world.entity.animal.Cow cow =
                new net.minecraft.world.entity.animal.Cow(EntityType.COW, helper.getLevel());
        helper.getLevel().addFreshEntity(cow);
        ItemStack creative = new ItemStack(ModItems.CREATIVE_SOUL_VESSEL.get());
        player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, creative);

        net.minecraftforge.event.entity.player.PlayerInteractEvent.EntityInteract event =
                new net.minecraftforge.event.entity.player.PlayerInteractEvent.EntityInteract(
                        player,
                        net.minecraft.world.InteractionHand.MAIN_HAND,
                        cow
                );
        PossessionManager.onEntityInteract(event);

        helper.assertTrue(event.isCanceled(), "Creative item interaction should consume the entity use");
        helper.assertTrue(creative.getTagElement("RequiemSoulCapture") != null,
                "Creative vessel should receive the active capture data");
        helper.assertTrue(!PossessionManager.hasPendingPossessionForTest(player),
                "Possession should not start on top of a handled creative item use");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void possessedHostKeepsArmorEquipment(GameTestHelper helper) {
        net.minecraft.server.level.ServerPlayer player = new net.minecraft.server.level.ServerPlayer(
                helper.getLevel().getServer(),
                helper.getLevel(),
                new com.mojang.authlib.GameProfile(UUID.randomUUID(), "possessed-armor-test")
        );
        SoulManager.setState(player, SoulState.VAGRANT);

        net.minecraft.world.entity.monster.Zombie zombie =
                new net.minecraft.world.entity.monster.Zombie(EntityType.ZOMBIE, helper.getLevel());
        ItemStack helmet = new ItemStack(Items.IRON_HELMET);
        zombie.setItemSlot(net.minecraft.world.entity.EquipmentSlot.HEAD, helmet.copy());
        helper.getLevel().addFreshEntity(zombie);
        EntitySoulManager.removeSoul(zombie);

        helper.assertTrue(PossessionManager.tryPossess(player, zombie), "Possession should start");
        helper.assertTrue(zombie.getItemBySlot(net.minecraft.world.entity.EquipmentSlot.HEAD).is(Items.IRON_HELMET),
                "Possessed host armor must remain equipped on the host for vanilla rendering and sunlight checks");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void possessedArmorSlotsMirrorToHostEquipment(GameTestHelper helper) {
        net.minecraft.server.level.ServerPlayer player = new net.minecraft.server.level.ServerPlayer(
                helper.getLevel().getServer(),
                helper.getLevel(),
                new com.mojang.authlib.GameProfile(UUID.randomUUID(), "possessed-armor-mirror-test")
        );
        SoulManager.setState(player, SoulState.VAGRANT);
        net.minecraft.world.entity.monster.Zombie zombie =
                new net.minecraft.world.entity.monster.Zombie(EntityType.ZOMBIE, helper.getLevel());
        helper.getLevel().addFreshEntity(zombie);
        EntitySoulManager.removeSoul(zombie);

        helper.assertTrue(PossessionManager.tryPossess(player, zombie), "Possession should start");
        ItemStack helmet = new ItemStack(Items.DIAMOND_HELMET);
        player.getInventory().armor.set(3, helmet);
        ua.meekeyn.requiemunofficial.common.possession.PossessionInventoryManager
                .syncPlayerEquipmentToHost(player, zombie);

        helper.assertTrue(zombie.getItemBySlot(net.minecraft.world.entity.EquipmentSlot.HEAD) != helmet
                        && ItemStack.matches(zombie.getItemBySlot(net.minecraft.world.entity.EquipmentSlot.HEAD), helmet),
                "Possessed host HEAD slot should use an independent copy of the body inventory helmet");
        helper.assertTrue(!zombie.getItemBySlot(net.minecraft.world.entity.EquipmentSlot.HEAD).isEmpty(),
                "Possessed host HEAD slot must be non-empty for renderer and sunlight logic");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void possessedMainHandFollowsSelectedHotbarStackOnce(GameTestHelper helper) {
        net.minecraft.server.level.ServerPlayer player = new net.minecraft.server.level.ServerPlayer(
                helper.getLevel().getServer(),
                helper.getLevel(),
                new com.mojang.authlib.GameProfile(UUID.randomUUID(), "possessed-hand-sync-test")
        );
        SoulManager.setState(player, SoulState.VAGRANT);
        net.minecraft.world.entity.monster.Skeleton skeleton =
                new net.minecraft.world.entity.monster.Skeleton(EntityType.SKELETON, helper.getLevel());
        skeleton.setItemSlot(net.minecraft.world.entity.EquipmentSlot.MAINHAND, new ItemStack(Items.BOW));
        helper.getLevel().addFreshEntity(skeleton);
        EntitySoulManager.removeSoul(skeleton);

        player.getInventory().items.set(0, new ItemStack(Items.STONE_SWORD));
        player.getInventory().items.set(1, new ItemStack(Items.IRON_AXE));
        player.getInventory().selected = 0;

        helper.assertTrue(PossessionManager.tryPossess(player, skeleton), "Possession should start");
        helper.assertTrue(skeleton.getMainHandItem().is(Items.STONE_SWORD),
                "Possessed host should mirror the selected hotbar stack on possession start");

        player.getInventory().selected = 1;
        ua.meekeyn.requiemunofficial.common.possession.PossessionInventoryManager
                .syncPlayerEquipmentToHost(player, skeleton);
        ItemStack firstSynced = skeleton.getMainHandItem();
        ua.meekeyn.requiemunofficial.common.possession.PossessionInventoryManager
                .syncPlayerEquipmentToHost(player, skeleton);

        helper.assertTrue(firstSynced.is(Items.IRON_AXE),
                "Changing selected hotbar slot should update the host main hand");
        helper.assertTrue(ItemStack.matches(firstSynced, skeleton.getMainHandItem()),
                "Repeated equipment sync should keep an equivalent authoritative main-hand stack");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void possessedHuskJumpUsesVanillaVerticalImpulse(GameTestHelper helper) {
        net.minecraft.server.level.ServerPlayer player = new net.minecraft.server.level.ServerPlayer(
                helper.getLevel().getServer(),
                helper.getLevel(),
                new com.mojang.authlib.GameProfile(UUID.randomUUID(), "possessed-jump-test")
        );
        SoulManager.setState(player, SoulState.VAGRANT);
        net.minecraft.world.entity.monster.Husk husk =
                new net.minecraft.world.entity.monster.Husk(EntityType.HUSK, helper.getLevel());
        husk.moveTo(helper.absolutePos(new BlockPos(1, 2, 1)), 0.0F, 0.0F);
        husk.setOnGround(true);
        helper.getLevel().addFreshEntity(husk);
        EntitySoulManager.removeSoul(husk);

        helper.assertTrue(PossessionManager.tryPossess(player, husk), "Possession should start");
        ((ua.meekeyn.requiemunofficial.mixin.LivingEntityAccessor) player).requiemunofficial$jumpFromGround();
        PossessionManager.syncBeta16HostToPlayer(player, husk);

        helper.assertTrue(Math.abs(husk.getDeltaMovement().y - 0.42D) < 0.0001D,
                "Possessed Husk jump should use vanilla LivingEntity.jumpFromGround vertical impulse");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void possessionInputJumpUsesSingleVanillaImpulse(GameTestHelper helper) {
        net.minecraft.server.level.ServerPlayer player = new net.minecraft.server.level.ServerPlayer(
                helper.getLevel().getServer(),
                helper.getLevel(),
                new com.mojang.authlib.GameProfile(UUID.randomUUID(), "possessed-input-jump-test")
        );
        SoulManager.setState(player, SoulState.VAGRANT);
        net.minecraft.world.entity.monster.Husk husk =
                new net.minecraft.world.entity.monster.Husk(EntityType.HUSK, helper.getLevel());
        husk.moveTo(helper.absolutePos(new BlockPos(1, 2, 1)), 0.0F, 0.0F);
        husk.setOnGround(true);
        helper.getLevel().addFreshEntity(husk);
        EntitySoulManager.removeSoul(husk);

        helper.assertTrue(PossessionManager.tryPossess(player, husk), "Possession should start");
        ((ua.meekeyn.requiemunofficial.mixin.LivingEntityAccessor) player).requiemunofficial$jumpFromGround();
        PossessionManager.syncBeta16HostToPlayer(player, husk);

        helper.assertTrue(Math.abs(husk.getDeltaMovement().y - 0.42D) < 0.0001D,
                "Possessed walking host input jump should apply exactly one vanilla jump impulse");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void possessionTemporarilyRaisesAndRestoresStepHeight(GameTestHelper helper) {
        net.minecraft.server.level.ServerPlayer player = new net.minecraft.server.level.ServerPlayer(
                helper.getLevel().getServer(),
                helper.getLevel(),
                new com.mojang.authlib.GameProfile(UUID.randomUUID(), "possessed-step-test")
        );
        SoulManager.setState(player, SoulState.VAGRANT);
        net.minecraft.world.entity.monster.Zombie zombie =
                new net.minecraft.world.entity.monster.Zombie(EntityType.ZOMBIE, helper.getLevel());
        zombie.moveTo(helper.absolutePos(new BlockPos(1, 2, 1)), 0.0F, 0.0F);
        helper.getLevel().addFreshEntity(zombie);
        EntitySoulManager.removeSoul(zombie);
        ua.meekeyn.requiemunofficial.mixin.EntityAccessor accessor =
                (ua.meekeyn.requiemunofficial.mixin.EntityAccessor) zombie;
        accessor.requiemunofficial$setMaxUpStep(0.1F);

        helper.assertTrue(PossessionManager.tryPossess(player, zombie), "Possession should start");
        helper.assertTrue(Math.abs(accessor.requiemunofficial$getMaxUpStep() - 0.6F) < 0.0001F,
                "Possession should temporarily raise walking host maxUpStep to the beta-compatible minimum");

        PossessionManager.release(player, zombie);
        helper.assertTrue(Math.abs(accessor.requiemunofficial$getMaxUpStep() - 0.1F) < 0.0001F,
                "Releasing possession should restore the host's original maxUpStep");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void playerShellDeathDropsCapturedBodyInventory(GameTestHelper helper) {
        net.minecraft.server.level.ServerPlayer player = new net.minecraft.server.level.ServerPlayer(
                helper.getLevel().getServer(),
                helper.getLevel(),
                new com.mojang.authlib.GameProfile(UUID.randomUUID(), "shell-drop-test")
        );
        player.moveTo(helper.absolutePos(new BlockPos(1, 2, 1)), 0.0F, 0.0F);
        SoulManager.setState(player, SoulState.REMNANT);
        player.getInventory().items.set(4, new ItemStack(Items.DIAMOND, 3));

        helper.assertTrue(SoulLifecycleManager.splitPlayer(player, false), "Split should create a shell");
        PlayerShellEntity shell = SoulLifecycleManager.findShell(player);
        helper.assertTrue(shell != null, "Shell should exist");
        shell.hurt(shell.damageSources().generic(), 1000.0F);

        java.util.List<net.minecraft.world.entity.item.ItemEntity> drops =
                helper.getLevel().getEntitiesOfClass(
                        net.minecraft.world.entity.item.ItemEntity.class,
                        new net.minecraft.world.phys.AABB(helper.absolutePos(new BlockPos(0, 0, 0))).inflate(8.0D),
                        item -> item.getItem().is(Items.DIAMOND)
                );
        int droppedDiamonds = drops.stream().mapToInt(item -> item.getItem().getCount()).sum();
        helper.assertTrue(droppedDiamonds == 3, "Shell death should drop captured body inventory exactly once");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void possessedBlazeAttackUsesChargeState(GameTestHelper helper) {
        net.minecraft.server.level.ServerPlayer player = new net.minecraft.server.level.ServerPlayer(
                helper.getLevel().getServer(),
                helper.getLevel(),
                new com.mojang.authlib.GameProfile(UUID.randomUUID(), "blaze-charge-test")
        );
        SoulManager.setState(player, SoulState.VAGRANT);

        net.minecraft.world.entity.monster.Blaze blaze =
                new net.minecraft.world.entity.monster.Blaze(EntityType.BLAZE, helper.getLevel());
        net.minecraft.world.entity.animal.Cow cow =
                new net.minecraft.world.entity.animal.Cow(EntityType.COW, helper.getLevel());
        blaze.moveTo(helper.absolutePos(new BlockPos(1, 2, 1)), 0.0F, 0.0F);
        cow.moveTo(helper.absolutePos(new BlockPos(3, 2, 1)), 0.0F, 0.0F);
        helper.getLevel().addFreshEntity(blaze);
        helper.getLevel().addFreshEntity(cow);
        EntitySoulManager.removeSoul(blaze);

        helper.assertTrue(PossessionManager.tryPossess(player, blaze), "Possession should start");
        helper.assertTrue(PossessionManager.trySpecialAttackForTest(player, blaze, cow),
                "Blaze attack should be accepted");
        helper.assertTrue(((ua.meekeyn.requiemunofficial.mixin.BlazeAccessor) blaze).requiemunofficial$isCharged(),
                "Possessed Blaze should enter the vanilla charged visual state before firing");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void possessedBlazeInputDoesNotDirectlyMoveBeforeTravel(GameTestHelper helper) {
        net.minecraft.server.level.ServerPlayer player = new net.minecraft.server.level.ServerPlayer(
                helper.getLevel().getServer(),
                helper.getLevel(),
                new com.mojang.authlib.GameProfile(UUID.randomUUID(), "blaze-travel-authority-test")
        );
        SoulManager.setState(player, SoulState.VAGRANT);
        net.minecraft.world.entity.monster.Blaze blaze =
                new net.minecraft.world.entity.monster.Blaze(EntityType.BLAZE, helper.getLevel());
        blaze.moveTo(helper.absolutePos(new BlockPos(1, 2, 1)), 0.0F, 0.0F);
        helper.getLevel().addFreshEntity(blaze);
        EntitySoulManager.removeSoul(blaze);

        helper.assertTrue(PossessionManager.tryPossess(player, blaze), "Possession should start");
        net.minecraft.world.phys.Vec3 before = blaze.position();
        PossessionManager.updatePossessionInput(
                player,
                new ua.meekeyn.requiemunofficial.common.network.PossessionInputPacket(
                        1.0F,
                        0.0F,
                        false,
                        false,
                        0.0F,
                        0.0F
                )
        );
        PossessionManager.controlHostForTest(player, blaze);

        helper.assertTrue(blaze.position().distanceToSqr(before) == 0.0D,
                "Beta16 Blaze input should not directly move the host; the Player Body owns movement");
        helper.assertTrue(PossessionManager.isUsingBeta16PlayerBody(player) && player.getCamera() == player,
                "Blaze possession should route movement through the beta16 Player Body");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void possessedBlazeReleaseLeavesOnlyVanillaTravelInertia(GameTestHelper helper) {
        net.minecraft.server.level.ServerPlayer player = new net.minecraft.server.level.ServerPlayer(
                helper.getLevel().getServer(),
                helper.getLevel(),
                new com.mojang.authlib.GameProfile(UUID.randomUUID(), "blaze-release-inertia-test")
        );
        SoulManager.setState(player, SoulState.VAGRANT);
        net.minecraft.world.entity.monster.Blaze blaze =
                new net.minecraft.world.entity.monster.Blaze(EntityType.BLAZE, helper.getLevel());
        blaze.moveTo(helper.absolutePos(new BlockPos(1, 2, 1)), 0.0F, 0.0F);
        helper.getLevel().addFreshEntity(blaze);
        EntitySoulManager.removeSoul(blaze);

        helper.assertTrue(PossessionManager.tryPossess(player, blaze), "Possession should start");
        PossessionManager.updatePossessionInput(
                player,
                new ua.meekeyn.requiemunofficial.common.network.PossessionInputPacket(
                        1.0F,
                        0.0F,
                        false,
                        false,
                        0.0F,
                        0.0F
                )
        );
        player.setDeltaMovement(0.12D, 0.0D, 0.0D);
        PossessionManager.controlHostForTest(player, blaze);
        double movingDelta = blaze.getDeltaMovement().horizontalDistance();
        helper.assertTrue(movingDelta > 0.0D,
                "Beta16 Blaze host should follow Player Body horizontal deltaMovement");

        PossessionManager.updatePossessionInput(
                player,
                new ua.meekeyn.requiemunofficial.common.network.PossessionInputPacket(
                        0.0F,
                        0.0F,
                        false,
                        false,
                        0.0F,
                        0.0F
                )
        );
        player.setDeltaMovement(0.02D, 0.0D, 0.0D);
        PossessionManager.controlHostForTest(player, blaze);
        double releasedDelta = blaze.getDeltaMovement().horizontalDistance();
        helper.assertTrue(releasedDelta > 0.0D && releasedDelta < movingDelta,
                "Released Blaze input should leave only the Player Body's remaining inertia");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void possessedShiftDoesNotChangeSkeletonOrBlazePoseDimensions(GameTestHelper helper) {
        net.minecraft.server.level.ServerPlayer player = new net.minecraft.server.level.ServerPlayer(
                helper.getLevel().getServer(),
                helper.getLevel(),
                new com.mojang.authlib.GameProfile(UUID.randomUUID(), "possessed-shift-pose-test")
        );
        SoulManager.setState(player, SoulState.VAGRANT);
        net.minecraft.world.entity.monster.Skeleton skeleton =
                new net.minecraft.world.entity.monster.Skeleton(EntityType.SKELETON, helper.getLevel());
        skeleton.moveTo(helper.absolutePos(new BlockPos(1, 2, 1)), 0.0F, 0.0F);
        helper.getLevel().addFreshEntity(skeleton);
        EntitySoulManager.removeSoul(skeleton);
        net.minecraft.world.entity.Pose skeletonPose = skeleton.getPose();
        net.minecraft.world.entity.EntityDimensions skeletonDimensions = skeleton.getDimensions(skeletonPose);
        float skeletonEyeHeight = skeleton.getEyeHeight();

        helper.assertTrue(PossessionManager.tryPossess(player, skeleton), "Skeleton possession should start");
        PossessionManager.updatePossessionInput(
                player,
                new ua.meekeyn.requiemunofficial.common.network.PossessionInputPacket(
                        0.0F,
                        0.0F,
                        false,
                        true,
                        0.0F,
                        0.0F
                )
        );
        PossessionManager.controlHostForTest(player, skeleton);
        helper.assertTrue(skeleton.isShiftKeyDown(), "Shift input should set the skeleton shared shift flag");
        helper.assertTrue(skeleton.getPose() == skeletonPose,
                "Skeleton shift flag should not switch the mob into CROUCHING pose");
        net.minecraft.world.entity.EntityDimensions shiftedSkeletonDimensions = skeleton.getDimensions(skeleton.getPose());
        helper.assertTrue(shiftedSkeletonDimensions.width == skeletonDimensions.width
                        && shiftedSkeletonDimensions.height == skeletonDimensions.height,
                "Skeleton shift flag should not change dimensions");
        helper.assertTrue(skeleton.getEyeHeight() == skeletonEyeHeight,
                "Skeleton shift flag should not change eye height");

        PossessionManager.release(player, skeleton);
        SoulManager.setState(player, SoulState.VAGRANT);
        net.minecraft.world.entity.monster.Blaze blaze =
                new net.minecraft.world.entity.monster.Blaze(EntityType.BLAZE, helper.getLevel());
        blaze.moveTo(helper.absolutePos(new BlockPos(3, 2, 1)), 0.0F, 0.0F);
        helper.getLevel().addFreshEntity(blaze);
        EntitySoulManager.removeSoul(blaze);
        net.minecraft.world.entity.Pose blazePose = blaze.getPose();
        net.minecraft.world.entity.EntityDimensions blazeDimensions = blaze.getDimensions(blazePose);
        float blazeEyeHeight = blaze.getEyeHeight();

        helper.assertTrue(PossessionManager.tryPossess(player, blaze), "Blaze possession should start");
        PossessionManager.updatePossessionInput(
                player,
                new ua.meekeyn.requiemunofficial.common.network.PossessionInputPacket(
                        0.0F,
                        0.0F,
                        false,
                        true,
                        0.0F,
                        0.0F
                )
        );
        PossessionManager.controlHostForTest(player, blaze);
        helper.assertTrue(blaze.isShiftKeyDown(), "Shift input should set the Blaze shared shift flag");
        helper.assertTrue(blaze.getPose() == blazePose,
                "Blaze shift flag should not switch the mob into CROUCHING pose");
        net.minecraft.world.entity.EntityDimensions shiftedBlazeDimensions = blaze.getDimensions(blaze.getPose());
        helper.assertTrue(shiftedBlazeDimensions.width == blazeDimensions.width
                        && shiftedBlazeDimensions.height == blazeDimensions.height,
                "Blaze shift flag should not change dimensions");
        helper.assertTrue(blaze.getEyeHeight() == blazeEyeHeight,
                "Blaze shift flag should not change eye height");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void beta16SkeletonPossessionUsesPlayerAsMovementAuthority(GameTestHelper helper) {
        net.minecraft.server.level.ServerPlayer player = new net.minecraft.server.level.ServerPlayer(
                helper.getLevel().getServer(),
                helper.getLevel(),
                new com.mojang.authlib.GameProfile(UUID.randomUUID(), "beta16-skeleton-authority-test")
        );
        player.moveTo(helper.absolutePos(new BlockPos(1, 2, 1)), 0.0F, 0.0F);
        SoulManager.setState(player, SoulState.VAGRANT);

        net.minecraft.world.entity.monster.Skeleton skeleton =
                new net.minecraft.world.entity.monster.Skeleton(EntityType.SKELETON, helper.getLevel());
        skeleton.moveTo(helper.absolutePos(new BlockPos(2, 2, 1)), 30.0F, 0.0F);
        helper.getLevel().addFreshEntity(skeleton);
        EntitySoulManager.removeSoul(skeleton);

        float originalStep = ((EntityAccessor) player).requiemunofficial$getMaxUpStep();
        helper.assertTrue(PossessionManager.tryPossess(player, skeleton), "Skeleton possession should start");
        helper.assertTrue(player.getCamera() == player, "Beta16 skeleton possession must keep player camera authority");
        helper.assertTrue(!player.noPhysics, "Beta16 skeleton possession must keep player collision enabled");
        helper.assertTrue(!player.isNoGravity(), "Beta16 skeleton possession must keep player gravity enabled");
        helper.assertTrue(PossessionManager.isUsingBeta16PlayerBody(player),
                "Skeleton should select the beta16 player-body path");

        player.absMoveTo(skeleton.getX() + 0.1D, skeleton.getY(), skeleton.getZ() + 0.1D, 77.0F, 12.0F);
        player.setDeltaMovement(0.1D, -0.08D, 0.2D);
        player.setShiftKeyDown(true);
        double previousSkeletonX = skeleton.getX();
        double previousSkeletonY = skeleton.getY();
        double previousSkeletonZ = skeleton.getZ();
        PossessionManager.controlHostForTest(player, skeleton);

        helper.assertTrue(skeleton.distanceToSqr(player) < 1.0E-4D,
                "Skeleton host should follow the player result position");
        helper.assertTrue(skeleton.xo == previousSkeletonX
                        && skeleton.yo == previousSkeletonY
                        && skeleton.zo == previousSkeletonZ,
                "Skeleton render interpolation should not receive an extra movement step");
        helper.assertTrue(skeleton.getYRot() == player.getYRot() && skeleton.getXRot() == player.getXRot(),
                "Skeleton host should mirror player rotation after movement");
        helper.assertTrue(skeleton.isShiftKeyDown(), "Skeleton host should mirror player shift state without camera takeover");

        PossessionManager.release(player, skeleton);
        helper.assertTrue(!PossessionManager.isUsingBeta16PlayerBody(player),
                "Beta16 body state should clear on release");
        helper.assertTrue(((EntityAccessor) player).requiemunofficial$getMaxUpStep() == originalStep,
                "Player step height should be restored after release");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void beta16SkeletonHostIsIgnoredOnlyForOwnPossessor(GameTestHelper helper) {
        net.minecraft.server.level.ServerPlayer player = new net.minecraft.server.level.ServerPlayer(
                helper.getLevel().getServer(),
                helper.getLevel(),
                new com.mojang.authlib.GameProfile(UUID.randomUUID(), "beta16-pick-owner-test")
        );
        player.moveTo(helper.absolutePos(new BlockPos(1, 2, 1)), 0.0F, 0.0F);
        SoulManager.setState(player, SoulState.VAGRANT);

        net.minecraft.server.level.ServerPlayer other = new net.minecraft.server.level.ServerPlayer(
                helper.getLevel().getServer(),
                helper.getLevel(),
                new com.mojang.authlib.GameProfile(UUID.randomUUID(), "beta16-pick-other-test")
        );
        other.moveTo(helper.absolutePos(new BlockPos(1, 2, 3)), 0.0F, 0.0F);

        net.minecraft.world.entity.monster.Skeleton skeleton =
                new net.minecraft.world.entity.monster.Skeleton(EntityType.SKELETON, helper.getLevel());
        skeleton.moveTo(helper.absolutePos(new BlockPos(2, 2, 1)), 0.0F, 0.0F);
        helper.getLevel().addFreshEntity(skeleton);
        EntitySoulManager.removeSoul(skeleton);

        helper.assertTrue(PossessionManager.tryPossess(player, skeleton), "Skeleton possession should start");
        helper.assertTrue(PossessionManager.isBeta16HostForPlayer(player, skeleton),
                "Own beta16 host should be identifiable for client picking exclusion");
        helper.assertTrue(PossessionManager.isPossessorHostCollisionPair(player, skeleton),
                "Own beta16 host should not push its possessor");
        helper.assertTrue(!PossessionManager.isBeta16HostForPlayer(other, skeleton),
                "Other players must still be able to pick and attack the skeleton");
        helper.assertTrue(!PossessionManager.isPossessorHostCollisionPair(other, skeleton),
                "Other players must keep ordinary collision with the skeleton");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void beta16SkeletonHostIdentityUsesSyncedPossessionUuid(GameTestHelper helper) {
        net.minecraft.server.level.ServerPlayer player = new net.minecraft.server.level.ServerPlayer(
                helper.getLevel().getServer(),
                helper.getLevel(),
                new com.mojang.authlib.GameProfile(UUID.randomUUID(), "beta16-synced-id-test")
        );
        player.moveTo(helper.absolutePos(new BlockPos(1, 2, 1)), 0.0F, 0.0F);
        SoulManager.setState(player, SoulState.VAGRANT);

        net.minecraft.world.entity.monster.Skeleton skeleton =
                new net.minecraft.world.entity.monster.Skeleton(EntityType.SKELETON, helper.getLevel());
        skeleton.moveTo(helper.absolutePos(new BlockPos(2, 2, 1)), 0.0F, 0.0F);
        helper.getLevel().addFreshEntity(skeleton);
        EntitySoulManager.removeSoul(skeleton);

        helper.assertTrue(PossessionManager.tryPossess(player, skeleton), "Skeleton possession should start");
        skeleton.getPersistentData().remove("RequiemPossessor");
        helper.assertTrue(PossessionManager.isBeta16HostForPlayer(player, skeleton),
                "Own beta16 host should be recognized from the synced possession UUID without host-only NBT");

        PossessionManager.release(player, skeleton);
        helper.assertTrue(!PossessionManager.isBeta16HostForPlayer(player, skeleton),
                "Released beta16 host should no longer be recognized as owned");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void possessionInputRejectsNonFiniteRotation(GameTestHelper helper) {
        net.minecraft.server.level.ServerPlayer player = new net.minecraft.server.level.ServerPlayer(
                helper.getLevel().getServer(),
                helper.getLevel(),
                new com.mojang.authlib.GameProfile(UUID.randomUUID(), "finite-input-test")
        );
        player.moveTo(helper.absolutePos(new BlockPos(1, 2, 1)), 0.0F, 0.0F);
        SoulManager.setState(player, SoulState.VAGRANT);

        net.minecraft.world.entity.monster.Skeleton skeleton =
                new net.minecraft.world.entity.monster.Skeleton(EntityType.SKELETON, helper.getLevel());
        skeleton.moveTo(helper.absolutePos(new BlockPos(2, 2, 1)), 0.0F, 0.0F);
        helper.getLevel().addFreshEntity(skeleton);
        EntitySoulManager.removeSoul(skeleton);

        helper.assertTrue(PossessionManager.tryPossess(player, skeleton), "Skeleton possession should start");
        player.setYRot(31.0F);
        player.setXRot(12.0F);
        PossessionManager.updatePossessionInput(
                player,
                new ua.meekeyn.requiemunofficial.common.network.PossessionInputPacket(
                        1.0F,
                        0.0F,
                        false,
                        false,
                        Float.NaN,
                        Float.POSITIVE_INFINITY
                )
        );
        helper.assertTrue(Float.isFinite(player.getYRot()) && Float.isFinite(player.getXRot()),
                "Non-finite packet rotation must not be written to player state");
        helper.assertTrue(player.getYRot() == 31.0F && player.getXRot() == 12.0F,
                "Rejected packet should leave the previous finite rotation intact");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void beta16SkeletonIgnoresCustomAttackPackets(GameTestHelper helper) {
        net.minecraft.server.level.ServerPlayer player = new net.minecraft.server.level.ServerPlayer(
                helper.getLevel().getServer(),
                helper.getLevel(),
                new com.mojang.authlib.GameProfile(UUID.randomUUID(), "beta16-custom-attack-test")
        );
        player.moveTo(helper.absolutePos(new BlockPos(1, 2, 1)), 0.0F, 0.0F);
        SoulManager.setState(player, SoulState.VAGRANT);

        net.minecraft.world.entity.monster.Skeleton skeleton =
                new net.minecraft.world.entity.monster.Skeleton(EntityType.SKELETON, helper.getLevel());
        skeleton.moveTo(helper.absolutePos(new BlockPos(1, 2, 1)), 0.0F, 0.0F);
        helper.getLevel().addFreshEntity(skeleton);
        EntitySoulManager.removeSoul(skeleton);

        net.minecraft.world.entity.monster.Zombie target =
                new net.minecraft.world.entity.monster.Zombie(EntityType.ZOMBIE, helper.getLevel());
        target.moveTo(helper.absolutePos(new BlockPos(2, 2, 1)), 0.0F, 0.0F);
        helper.getLevel().addFreshEntity(target);
        float targetHealth = target.getHealth();

        helper.assertTrue(PossessionManager.tryPossess(player, skeleton), "Skeleton possession should start");
        PossessionManager.handleAction(
                player,
                ua.meekeyn.requiemunofficial.common.network.PossessionActionPacket.Action.ATTACK
        );

        helper.assertTrue(target.getHealth() == targetHealth,
                "Beta16 skeleton possession must ignore custom attack packets and rely on vanilla reach checks");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void beta16SkeletonPossessionUsesScaledPlayerMovementSpeed(GameTestHelper helper) {
        net.minecraft.server.level.ServerPlayer player = new net.minecraft.server.level.ServerPlayer(
                helper.getLevel().getServer(),
                helper.getLevel(),
                new com.mojang.authlib.GameProfile(UUID.randomUUID(), "beta16-skeleton-speed-test")
        );
        player.moveTo(helper.absolutePos(new BlockPos(1, 2, 1)), 0.0F, 0.0F);
        SoulManager.setState(player, SoulState.VAGRANT);

        net.minecraft.world.entity.monster.Skeleton skeleton =
                new net.minecraft.world.entity.monster.Skeleton(EntityType.SKELETON, helper.getLevel());
        skeleton.moveTo(helper.absolutePos(new BlockPos(2, 2, 1)), 0.0F, 0.0F);
        helper.getLevel().addFreshEntity(skeleton);
        EntitySoulManager.removeSoul(skeleton);

        double playerBase = player.getAttributeBaseValue(net.minecraft.world.entity.ai.attributes.Attributes.MOVEMENT_SPEED);
        double skeletonSpeed = skeleton.getAttributeValue(net.minecraft.world.entity.ai.attributes.Attributes.MOVEMENT_SPEED);

        helper.assertTrue(Math.abs(playerBase - 0.1D) < 1.0E-6D,
                "Vanilla player base movement speed should be 0.1");
        helper.assertTrue(Math.abs(skeletonSpeed - 0.25D) < 1.0E-6D,
                "Vanilla skeleton movement speed should be 0.25");

        helper.assertTrue(PossessionManager.tryPossess(player, skeleton), "Skeleton possession should start");
        helper.assertTrue(Math.abs(player.getAttributeValue(net.minecraft.world.entity.ai.attributes.Attributes.MOVEMENT_SPEED) - 0.1D) < 1.0E-6D,
                "Beta16 skeleton possession should use the vanilla player-equivalent walking speed, not raw skeleton speed");

        PossessionManager.release(player, skeleton);
        helper.assertTrue(Math.abs(player.getAttributeValue(net.minecraft.world.entity.ai.attributes.Attributes.MOVEMENT_SPEED) - playerBase) < 1.0E-6D,
                "Player movement speed should restore after release");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void beta16SkeletonSpeedModifierRestoresAndDoesNotDuplicate(GameTestHelper helper) {
        net.minecraft.server.level.ServerPlayer player = new net.minecraft.server.level.ServerPlayer(
                helper.getLevel().getServer(),
                helper.getLevel(),
                new com.mojang.authlib.GameProfile(UUID.randomUUID(), "beta16-skeleton-repeat-speed-test")
        );
        player.moveTo(helper.absolutePos(new BlockPos(1, 2, 1)), 0.0F, 0.0F);
        SoulManager.setState(player, SoulState.VAGRANT);

        net.minecraft.world.entity.monster.Skeleton skeleton =
                new net.minecraft.world.entity.monster.Skeleton(EntityType.SKELETON, helper.getLevel());
        skeleton.moveTo(helper.absolutePos(new BlockPos(2, 2, 1)), 0.0F, 0.0F);
        skeleton.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.MOVEMENT_SPEED).setBaseValue(0.35D);
        helper.getLevel().addFreshEntity(skeleton);
        EntitySoulManager.removeSoul(skeleton);

        double playerBase = player.getAttributeBaseValue(net.minecraft.world.entity.ai.attributes.Attributes.MOVEMENT_SPEED);
        double expectedPossessedSpeed = 0.14D;

        helper.assertTrue(PossessionManager.tryPossess(player, skeleton), "First skeleton possession should start");
        helper.assertTrue(Math.abs(player.getAttributeValue(net.minecraft.world.entity.ai.attributes.Attributes.MOVEMENT_SPEED) - expectedPossessedSpeed) < 1.0E-6D,
                "Scaled skeleton speed should apply once during possession");

        PossessionManager.release(player, skeleton);
        helper.assertTrue(Math.abs(player.getAttributeValue(net.minecraft.world.entity.ai.attributes.Attributes.MOVEMENT_SPEED) - playerBase) < 1.0E-6D,
                "Player movement speed should restore after first release");

        helper.assertTrue(PossessionManager.tryPossess(player, skeleton), "Second skeleton possession should start");
        helper.assertTrue(Math.abs(player.getAttributeValue(net.minecraft.world.entity.ai.attributes.Attributes.MOVEMENT_SPEED) - expectedPossessedSpeed) < 1.0E-6D,
                "Repeated possession should not duplicate the beta16 speed modifier");

        PossessionManager.release(player, skeleton);
        helper.assertTrue(Math.abs(player.getAttributeValue(net.minecraft.world.entity.ai.attributes.Attributes.MOVEMENT_SPEED) - playerBase) < 1.0E-6D,
                "Player movement speed should restore after second release");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void beta16SkeletonFallDamageAppliesToHostOnly(GameTestHelper helper) {
        net.minecraft.server.level.ServerPlayer player = new net.minecraft.server.level.ServerPlayer(
                helper.getLevel().getServer(),
                helper.getLevel(),
                new com.mojang.authlib.GameProfile(UUID.randomUUID(), "beta16-skeleton-fall-test")
        );
        player.moveTo(helper.absolutePos(new BlockPos(1, 2, 1)), 0.0F, 0.0F);
        SoulManager.setState(player, SoulState.VAGRANT);

        net.minecraft.world.entity.monster.Skeleton skeleton =
                new net.minecraft.world.entity.monster.Skeleton(EntityType.SKELETON, helper.getLevel());
        skeleton.moveTo(helper.absolutePos(new BlockPos(2, 2, 1)), 0.0F, 0.0F);
        helper.getLevel().addFreshEntity(skeleton);
        EntitySoulManager.removeSoul(skeleton);

        float playerHealth = player.getHealth();
        float skeletonHealth = skeleton.getHealth();
        helper.assertTrue(PossessionManager.tryPossess(player, skeleton), "Skeleton possession should start");

        net.minecraftforge.event.entity.living.LivingFallEvent event =
                new net.minecraftforge.event.entity.living.LivingFallEvent(player, 6.0F, 1.0F);
        PossessionManager.onBeta16PlayerBodyFall(event);

        helper.assertTrue(event.isCanceled(), "Player fall event should be canceled while the skeleton body owns health");
        helper.assertTrue(player.getHealth() == playerHealth, "Player health should not take possessed fall damage");
        helper.assertTrue(skeleton.getHealth() < skeletonHealth, "Skeleton host should take vanilla fall damage");
        helper.assertTrue(player.fallDistance == 0.0F, "Player fall distance should clear after bridged fall damage");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void beta16SkeletonKnockbackAppliesToPlayerMotion(GameTestHelper helper) {
        net.minecraft.server.level.ServerPlayer player = new net.minecraft.server.level.ServerPlayer(
                helper.getLevel().getServer(),
                helper.getLevel(),
                new com.mojang.authlib.GameProfile(UUID.randomUUID(), "beta16-skeleton-knockback-test")
        );
        player.moveTo(helper.absolutePos(new BlockPos(1, 2, 1)), 0.0F, 0.0F);
        player.setOnGround(true);
        SoulManager.setState(player, SoulState.VAGRANT);

        net.minecraft.world.entity.monster.Skeleton skeleton =
                new net.minecraft.world.entity.monster.Skeleton(EntityType.SKELETON, helper.getLevel());
        skeleton.moveTo(helper.absolutePos(new BlockPos(2, 2, 1)), 0.0F, 0.0F);
        helper.getLevel().addFreshEntity(skeleton);
        EntitySoulManager.removeSoul(skeleton);

        helper.assertTrue(PossessionManager.tryPossess(player, skeleton), "Skeleton possession should start");
        player.setDeltaMovement(net.minecraft.world.phys.Vec3.ZERO);
        skeleton.setDeltaMovement(net.minecraft.world.phys.Vec3.ZERO);

        net.minecraftforge.event.entity.living.LivingKnockBackEvent event =
                new net.minecraftforge.event.entity.living.LivingKnockBackEvent(skeleton, 0.4F, 1.0D, 0.0D);
        PossessionManager.onBeta16PlayerBodyKnockback(event);

        helper.assertTrue(event.isCanceled(), "Host knockback should be redirected to the player body");
        helper.assertTrue(player.getDeltaMovement().lengthSqr() > 0.0D,
                "Player movement should receive the knockback impulse");
        helper.assertTrue(skeleton.getDeltaMovement().distanceTo(player.getDeltaMovement()) < 1.0E-6D,
                "Skeleton render body should mirror the knocked player movement");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void beta16SkeletonPlayerDimensionsDelegateAndRestore(GameTestHelper helper) {
        net.minecraft.server.level.ServerPlayer player = new net.minecraft.server.level.ServerPlayer(
                helper.getLevel().getServer(),
                helper.getLevel(),
                new com.mojang.authlib.GameProfile(UUID.randomUUID(), "beta16-skeleton-size-test")
        );
        player.moveTo(helper.absolutePos(new BlockPos(1, 2, 1)), 0.0F, 0.0F);
        SoulManager.setState(player, SoulState.VAGRANT);

        net.minecraft.world.entity.monster.Skeleton skeleton =
                new net.minecraft.world.entity.monster.Skeleton(EntityType.SKELETON, helper.getLevel());
        skeleton.moveTo(helper.absolutePos(new BlockPos(2, 2, 1)), 0.0F, 0.0F);
        helper.getLevel().addFreshEntity(skeleton);
        EntitySoulManager.removeSoul(skeleton);

        net.minecraft.world.entity.Pose pose = player.getPose();
        net.minecraft.world.entity.EntityDimensions originalDimensions = player.getDimensions(pose);
        float originalEyeHeight = player.getEyeHeight(pose);
        net.minecraft.world.entity.EntityDimensions skeletonDimensions = skeleton.getDimensions(pose);
        float skeletonEyeHeight = skeleton.getEyeHeight(pose);

        helper.assertTrue(PossessionManager.tryPossess(player, skeleton), "Skeleton possession should start");
        helper.assertTrue(player.getDimensions(pose).width == skeletonDimensions.width
                        && player.getDimensions(pose).height == skeletonDimensions.height,
                "Player dimensions should delegate to the skeleton host");
        helper.assertTrue(player.getEyeHeight(pose) == skeletonEyeHeight,
                "Player eye height should delegate to the skeleton host");

        PossessionManager.release(player, skeleton);
        helper.assertTrue(player.getDimensions(pose).width == originalDimensions.width
                        && player.getDimensions(pose).height == originalDimensions.height,
                "Player dimensions should restore after release");
        helper.assertTrue(player.getEyeHeight(pose) == originalEyeHeight,
                "Player eye height should restore after release");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void commandCreatedShellCannotReclaimInventory(GameTestHelper helper) {
        net.minecraft.server.level.ServerPlayer player = new net.minecraft.server.level.ServerPlayer(
                helper.getLevel().getServer(),
                helper.getLevel(),
                new com.mojang.authlib.GameProfile(UUID.randomUUID(), "identity-shell-test")
        );
        player.moveTo(helper.absolutePos(new BlockPos(1, 2, 1)), 0.0F, 0.0F);
        player.getCapability(ua.meekeyn.requiemunofficial.common.soul.ModCapabilities.PLAYER_SOUL)
                .ifPresent(soul -> soul.setState(SoulState.VAGRANT));
        player.getInventory().add(new ItemStack(Items.DIAMOND, 5));

        PlayerShellEntity identityShell = new PlayerShellEntity(ModEntities.PLAYER_SHELL.get(), helper.getLevel());
        identityShell.moveTo(helper.absolutePos(new BlockPos(2, 2, 1)), 0.0F, 0.0F);
        identityShell.setOwner(player.getUUID(), player.getDisplayName());
        helper.getLevel().addFreshEntity(identityShell);

        helper.assertTrue(!identityShell.hasCapturedBodyInventory(),
                "Command-created identity shells must not claim captured body inventory");
        helper.assertTrue(!SoulLifecycleManager.tryReclaimShell(player, identityShell),
                "Identity shells must not be accepted by reclaim/merge");
        helper.assertTrue(SoulManager.getState(player) == SoulState.VAGRANT,
                "Rejected identity-shell reclaim must not change the player state");
        helper.assertTrue(player.getInventory().countItem(Items.DIAMOND) == 5,
                "Rejected identity-shell reclaim must not overwrite player inventory");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void stage8SoulboundStackStaysWithSoulWithoutDuplication(GameTestHelper helper) {
        net.minecraft.server.level.ServerPlayer player = new net.minecraft.server.level.ServerPlayer(
                helper.getLevel().getServer(),
                helper.getLevel(),
                new com.mojang.authlib.GameProfile(UUID.randomUUID(), "stage8-soulbound-test")
        );
        player.moveTo(helper.absolutePos(new BlockPos(1, 2, 1)), 0.0F, 0.0F);
        SoulManager.setState(player, SoulState.REMNANT);
        ItemStack soulbound = new ItemStack(Items.AMETHYST_SHARD);
        soulbound.getOrCreateTag().putBoolean("RequiemSoulbound", true);
        player.getInventory().items.set(5, soulbound.copy());
        player.getInventory().items.set(6, new ItemStack(Items.DIRT, 12));

        helper.assertTrue(SoulLifecycleManager.splitPlayer(player, false), "Split should succeed");
        PlayerShellEntity shell = SoulLifecycleManager.findShell(player);
        helper.assertTrue(shell != null, "Split should create a shell");
        helper.assertTrue(player.getInventory().items.get(5).is(Items.AMETHYST_SHARD),
                "Soulbound stack should stay with the soul");
        helper.assertTrue(player.getInventory().items.get(6).isEmpty(),
                "Normal body stack should leave the soul");
        helper.assertTrue(!shell.hasCapturedStackAt(5, Items.AMETHYST_SHARD),
                "Soulbound stack must be removed from shell storage");
        helper.assertTrue(shell.hasCapturedStackAt(6, Items.DIRT),
                "Normal stack should remain captured by the shell");

        helper.assertTrue(SoulLifecycleManager.tryReclaimShell(player, shell),
                "Reclaim should merge captured shell once");
        helper.assertTrue(player.getInventory().items.get(5).is(Items.AMETHYST_SHARD),
                "Soulbound stack should survive reclaim");
        helper.assertTrue(player.getInventory().items.get(6).is(Items.DIRT)
                        && player.getInventory().items.get(6).getCount() == 12,
                "Normal body stack should restore from shell");
        helper.assertTrue(!SoulLifecycleManager.tryReclaimShell(player, shell),
                "Discarded shell must not reclaim a second time");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void stage8ArmorAndOffhandFollowSoulboundSemantics(GameTestHelper helper) {
        net.minecraft.server.level.ServerPlayer player = new net.minecraft.server.level.ServerPlayer(
                helper.getLevel().getServer(),
                helper.getLevel(),
                new com.mojang.authlib.GameProfile(UUID.randomUUID(), "stage8-equipment-test")
        );
        player.moveTo(helper.absolutePos(new BlockPos(1, 2, 1)), 0.0F, 0.0F);
        SoulManager.setState(player, SoulState.REMNANT);
        player.getInventory().armor.set(3, new ItemStack(Items.IRON_HELMET));
        ItemStack offhand = new ItemStack(Items.TORCH);
        offhand.getOrCreateTag().putBoolean("RequiemSoulbound", true);
        player.getInventory().offhand.set(0, offhand.copy());

        helper.assertTrue(SoulLifecycleManager.splitPlayer(player, false), "Split should succeed");
        PlayerShellEntity shell = SoulLifecycleManager.findShell(player);
        helper.assertTrue(shell != null, "Split should create a shell");
        helper.assertTrue(player.getInventory().armor.get(3).isEmpty(),
                "Normal armor should be body-owned after split");
        helper.assertTrue(player.getInventory().offhand.get(0).is(Items.TORCH),
                "Soulbound offhand should stay with the soul");
        helper.assertTrue(shell.getItemBySlot(net.minecraft.world.entity.EquipmentSlot.HEAD).is(Items.IRON_HELMET),
                "Shell should visually carry body armor");
        helper.assertTrue(shell.getItemBySlot(net.minecraft.world.entity.EquipmentSlot.OFFHAND).isEmpty(),
                "Shell should not mirror soulbound offhand");

        helper.assertTrue(SoulLifecycleManager.tryReclaimShell(player, shell),
                "Reclaim should restore body equipment");
        helper.assertTrue(player.getInventory().armor.get(3).is(Items.IRON_HELMET),
                "Body armor should restore on reclaim");
        helper.assertTrue(player.getInventory().offhand.get(0).is(Items.TORCH),
                "Soulbound offhand should still be present after reclaim");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void stage8KeepInventoryLeavesStacksWithSoulAndNotShell(GameTestHelper helper) {
        helper.getLevel().getGameRules()
                .getRule(ua.meekeyn.requiemunofficial.common.init.ModGameRules.POSSESSION_KEEP_INVENTORY)
                .set(
                        ua.meekeyn.requiemunofficial.common.init.ModGameRules.POSSESSION_KEEP_INVENTORY_ALWAYS,
                        helper.getLevel().getServer()
                );
        net.minecraft.server.level.ServerPlayer player = new net.minecraft.server.level.ServerPlayer(
                helper.getLevel().getServer(),
                helper.getLevel(),
                new com.mojang.authlib.GameProfile(UUID.randomUUID(), "stage8-keepinv-test")
        );
        player.moveTo(helper.absolutePos(new BlockPos(1, 2, 1)), 0.0F, 0.0F);
        SoulManager.setState(player, SoulState.REMNANT);
        player.getInventory().items.set(4, new ItemStack(Items.DIAMOND, 3));

        helper.assertTrue(SoulLifecycleManager.splitPlayer(player, false), "Split should succeed");
        PlayerShellEntity shell = SoulLifecycleManager.findShell(player);
        helper.assertTrue(shell != null, "Split should create a shell");
        helper.assertTrue(player.getInventory().items.get(4).is(Items.DIAMOND)
                        && player.getInventory().items.get(4).getCount() == 3,
                "Keep-inventory split should return normal stacks to the soul");
        helper.assertTrue(!shell.hasCapturedStackAt(4, Items.DIAMOND),
                "Keep-inventory stacks must be removed from shell storage");

        helper.assertTrue(SoulLifecycleManager.tryReclaimShell(player, shell),
                "Keep-inventory shell should still be reclaimable");
        helper.assertTrue(player.getInventory().items.get(4).is(Items.DIAMOND)
                        && player.getInventory().items.get(4).getCount() == 3,
                "Reclaim must not duplicate keep-inventory stacks");
        helper.getLevel().getGameRules()
                .getRule(ua.meekeyn.requiemunofficial.common.init.ModGameRules.POSSESSION_KEEP_INVENTORY)
                .set(
                        ua.meekeyn.requiemunofficial.common.init.ModGameRules.POSSESSION_KEEP_INVENTORY_NEVER,
                        helper.getLevel().getServer()
                );
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void stage8SoulboundEffectsStayWithSoulAndBodyEffectsRestore(GameTestHelper helper) {
        net.minecraft.server.level.ServerPlayer player = new net.minecraft.server.level.ServerPlayer(
                helper.getLevel().getServer(),
                helper.getLevel(),
                new com.mojang.authlib.GameProfile(UUID.randomUUID(), "stage8-effects-test")
        );
        player.moveTo(helper.absolutePos(new BlockPos(1, 2, 1)), 0.0F, 0.0F);
        SoulManager.setState(player, SoulState.REMNANT);
        addEffectForFakePlayer(player, new net.minecraft.world.effect.MobEffectInstance(
                ModEffects.ATTRITION.get(),
                200,
                1
        ));
        addEffectForFakePlayer(player, new net.minecraft.world.effect.MobEffectInstance(
                net.minecraft.world.effect.MobEffects.MOVEMENT_SPEED,
                200,
                0
        ));

        helper.assertTrue(SoulLifecycleManager.splitPlayer(player, false), "Split should succeed");
        PlayerShellEntity shell = SoulLifecycleManager.findShell(player);
        helper.assertTrue(shell != null, "Split should create a shell");
        helper.assertTrue(player.hasEffect(ModEffects.ATTRITION.get()),
                "Soulbound attrition should stay with the soul");
        helper.assertTrue(!player.hasEffect(net.minecraft.world.effect.MobEffects.MOVEMENT_SPEED),
                "Normal body effect should leave the soul");
        helper.assertTrue(!shell.hasEffect(ModEffects.ATTRITION.get()),
                "Soulbound attrition should not be copied to the shell");
        helper.assertTrue(shell.hasEffect(net.minecraft.world.effect.MobEffects.MOVEMENT_SPEED),
                "Normal body effect should be stored on the shell");

        helper.assertTrue(SoulLifecycleManager.tryReclaimShell(player, shell),
                "Reclaim should restore body effects");
        helper.assertTrue(player.hasEffect(ModEffects.ATTRITION.get()),
                "Soulbound effect should survive reclaim");
        helper.assertTrue(player.hasEffect(net.minecraft.world.effect.MobEffects.MOVEMENT_SPEED),
                "Body effect should restore on reclaim");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void stage19StickyRemovalReappliesForVagrantSoul(GameTestHelper helper) {
        net.minecraft.server.level.ServerPlayer player = new net.minecraft.server.level.ServerPlayer(
                helper.getLevel().getServer(),
                helper.getLevel(),
                new com.mojang.authlib.GameProfile(UUID.randomUUID(), "stage19-sticky-remove")
        );
        SoulManager.setState(player, SoulState.VAGRANT);
        addEffectForFakePlayer(player, new net.minecraft.world.effect.MobEffectInstance(ModEffects.ATTRITION.get(), 80, 0));

        net.minecraft.world.effect.MobEffectInstance removed =
                player.getActiveEffectsMap().remove(ModEffects.ATTRITION.get());
        ua.meekeyn.requiemunofficial.common.effect.StickyEffectReapplicationManager
                .onEffectRemoved(player, removed);

        helper.assertTrue(player.hasEffect(ModEffects.ATTRITION.get()),
                "Sticky attrition should reapply after ordinary removal on a vagrant soul");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void stage19StickyDurationFreezesForVagrantSoul(GameTestHelper helper) {
        net.minecraft.server.level.ServerPlayer player = new net.minecraft.server.level.ServerPlayer(
                helper.getLevel().getServer(),
                helper.getLevel(),
                new com.mojang.authlib.GameProfile(UUID.randomUUID(), "stage19-freeze")
        );
        SoulManager.setState(player, SoulState.VAGRANT);
        net.minecraft.world.effect.MobEffectInstance attrition =
                new net.minecraft.world.effect.MobEffectInstance(ModEffects.ATTRITION.get(), 80, 0);

        attrition.tick(player, () -> {
        });

        helper.assertTrue(attrition.getDuration() == 80,
                "Sticky attrition duration should freeze while the soul is vagrant");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void stage19ReclamationClearsOnSoulBodySplit(GameTestHelper helper) {
        net.minecraft.server.level.ServerPlayer player = new net.minecraft.server.level.ServerPlayer(
                helper.getLevel().getServer(),
                helper.getLevel(),
                new com.mojang.authlib.GameProfile(UUID.randomUUID(), "stage19-reclamation-clear")
        );
        SoulManager.setState(player, SoulState.REMNANT);
        addEffectForFakePlayer(player, new net.minecraft.world.effect.MobEffectInstance(
                ModEffects.RECLAMATION.get(),
                200,
                0
        ));

        helper.assertTrue(SoulLifecycleManager.splitPlayer(player, false), "Split should succeed");

        helper.assertTrue(!player.hasEffect(ModEffects.RECLAMATION.get()),
                "Reclamation should be cleared during dissociation/split cleanup");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void stage19PenanceBlocksMergeAndCure(GameTestHelper helper) {
        net.minecraft.server.level.ServerPlayer player = new net.minecraft.server.level.ServerPlayer(
                helper.getLevel().getServer(),
                helper.getLevel(),
                new com.mojang.authlib.GameProfile(UUID.randomUUID(), "stage19-penance-merge")
        );
        SoulManager.setState(player, SoulState.REMNANT);
        helper.assertTrue(SoulLifecycleManager.splitPlayer(player, false), "Split should create a reclaimable shell");
        PlayerShellEntity shell = SoulLifecycleManager.findShell(player);
        helper.assertTrue(shell != null, "Split should create a shell");
        addEffectForFakePlayer(player, new net.minecraft.world.effect.MobEffectInstance(
                ModEffects.PENANCE.get(),
                200,
                ua.meekeyn.requiemunofficial.common.effect.PenanceMobEffect.PLAYER_BAN_THRESHOLD
        ));

        helper.assertTrue(!SoulLifecycleManager.tryReclaimShell(player, shell),
                "Penance II should deny shell merge/reclaim");
        helper.assertTrue(
                ua.meekeyn.requiemunofficial.common.effect.PenanceMobEffect.blocksCure(
                        player.getEffect(ModEffects.PENANCE.get())
                ),
                "Any Penance should deny possessed-body cure");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void stage19AttritionStacksAndKeepsLongerRiftDuration(GameTestHelper helper) {
        net.minecraft.server.level.ServerPlayer player = new net.minecraft.server.level.ServerPlayer(
                helper.getLevel().getServer(),
                helper.getLevel(),
                new com.mojang.authlib.GameProfile(UUID.randomUUID(), "stage19-attrition-update")
        );
        int riftDuration = RiftTeleportManager.getAttritionLength(
                new BlockPos(0, 64, 0),
                new BlockPos(1000, 64, 0)
        );

        ua.meekeyn.requiemunofficial.common.effect.AttritionMobEffect.apply(player, 1, riftDuration);
        ua.meekeyn.requiemunofficial.common.effect.AttritionMobEffect.apply(player, 1, 20);

        net.minecraft.world.effect.MobEffectInstance attrition = player.getEffect(ModEffects.ATTRITION.get());
        helper.assertTrue(attrition != null && attrition.getAmplifier() == 1,
                "A second Attrition application should increase amplifier by one");
        helper.assertTrue(attrition != null && attrition.getDuration() >= riftDuration,
                "Shorter updates must not truncate Rift-produced Attrition duration");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void stage19AttritionMaxHealthModifierClampsHealth(GameTestHelper helper) {
        net.minecraft.server.level.ServerPlayer player = new net.minecraft.server.level.ServerPlayer(
                helper.getLevel().getServer(),
                helper.getLevel(),
                new com.mojang.authlib.GameProfile(UUID.randomUUID(), "stage19-attrition-attribute")
        );
        player.setHealth(player.getMaxHealth());

        ModEffects.ATTRITION.get().addAttributeModifiers(player, player.getAttributes(), 0);

        helper.assertTrue(player.getMaxHealth() < 20.0F,
                "Attrition should reduce max health through its original attribute modifier");
        helper.assertTrue(player.getHealth() <= player.getMaxHealth(),
                "Attrition application should clamp current health to reduced max health");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void vagrantFallEffectsAreSuppressed(GameTestHelper helper) {
        net.minecraft.server.level.ServerPlayer player = new net.minecraft.server.level.ServerPlayer(
                helper.getLevel().getServer(),
                helper.getLevel(),
                new com.mojang.authlib.GameProfile(UUID.randomUUID(), "vagrant-fall-test")
        );

        SoulManager.setState(player, SoulState.REMNANT);
        player.fallDistance = 12.0F;
        helper.assertTrue(!VagrantEvents.suppressFallForVagrant(player),
                "Embodied remnants should keep ordinary fall behavior");
        helper.assertTrue(player.fallDistance == 12.0F,
                "Non-vagrant fall distance should not be modified");

        SoulManager.setState(player, SoulState.VAGRANT);
        helper.assertTrue(VagrantEvents.suppressFallForVagrant(player),
                "Incorporeal vagrants should suppress fall effects");
        helper.assertTrue(player.fallDistance == 0.0F,
                "Incorporeal vagrants should not accumulate fall distance");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void vagrantPlayersDoNotTriggerWeightedPressurePlates(GameTestHelper helper) {
        net.minecraft.server.level.ServerPlayer regular = new net.minecraft.server.level.ServerPlayer(
                helper.getLevel().getServer(),
                helper.getLevel(),
                new com.mojang.authlib.GameProfile(UUID.randomUUID(), "plate-regular-test")
        );
        SoulManager.setState(regular, SoulState.MORTAL);

        net.minecraft.server.level.ServerPlayer vagrant = new net.minecraft.server.level.ServerPlayer(
                helper.getLevel().getServer(),
                helper.getLevel(),
                new com.mojang.authlib.GameProfile(UUID.randomUUID(), "plate-vagrant-test")
        );
        SoulManager.setState(vagrant, SoulState.VAGRANT);

        helper.assertTrue(
                VagrantRules.weightedPressureSignal(java.util.List.of(regular), 15) == 1,
                "Ordinary players should count for weighted pressure plates"
        );
        helper.assertTrue(
                VagrantRules.weightedPressureSignal(java.util.List.of(vagrant), 15) == 0,
                "Incorporeal vagrants should not count for weighted pressure plates"
        );
        helper.assertTrue(
                VagrantRules.weightedPressureSignal(java.util.List.of(regular, vagrant), 15) == 1,
                "Mixed entities should count only non-vagrant bodies"
        );
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void vagrantPlayersDoNotUseBodyCollision(GameTestHelper helper) {
        net.minecraft.server.level.ServerPlayer player = new net.minecraft.server.level.ServerPlayer(
                helper.getLevel().getServer(),
                helper.getLevel(),
                new com.mojang.authlib.GameProfile(UUID.randomUUID(), "vagrant-collision-test")
        );
        net.minecraft.world.entity.animal.Cow cow =
                new net.minecraft.world.entity.animal.Cow(EntityType.COW, helper.getLevel());

        SoulManager.setState(player, SoulState.REMNANT);
        helper.assertTrue(player.isPushable(), "Embodied remnants should keep ordinary pushability");

        SoulManager.setState(player, SoulState.VAGRANT);
        helper.assertTrue(VagrantRules.isRestricted(player), "Test player should be a restricted vagrant");
        helper.assertTrue(!player.isPushable(), "Incorporeal vagrants should not be pushable");
        helper.assertTrue(!cow.canCollideWith(player), "Mobs should not collide with incorporeal vagrants");
        helper.assertTrue(!player.canCollideWith(cow), "Incorporeal vagrants should not collide with mobs");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void vagrantPlayersAreIgnoredByVanillaTargetPredicates(GameTestHelper helper) {
        net.minecraft.server.level.ServerPlayer mortal = new net.minecraft.server.level.ServerPlayer(
                helper.getLevel().getServer(),
                helper.getLevel(),
                new com.mojang.authlib.GameProfile(UUID.randomUUID(), "target-mortal-test")
        );
        net.minecraft.server.level.ServerPlayer vagrant = new net.minecraft.server.level.ServerPlayer(
                helper.getLevel().getServer(),
                helper.getLevel(),
                new com.mojang.authlib.GameProfile(UUID.randomUUID(), "target-vagrant-test")
        );

        SoulManager.setState(mortal, SoulState.MORTAL);
        SoulManager.setState(vagrant, SoulState.VAGRANT);

        helper.assertTrue(
                net.minecraft.world.entity.EntitySelector.NO_CREATIVE_OR_SPECTATOR.test(mortal),
                "Ordinary survival players should pass vanilla non-spectator targeting predicates"
        );
        helper.assertTrue(
                !net.minecraft.world.entity.EntitySelector.NO_CREATIVE_OR_SPECTATOR.test(vagrant),
                "Free vagrant souls should be filtered like creative/spectator players"
        );
        helper.assertTrue(
                !net.minecraft.world.entity.ai.targeting.TargetingConditions.forNonCombat().test(mortal, vagrant),
                "TargetingConditions should reject free vagrant targets before mob AI sees them"
        );
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void possessionEquipmentRespectsBodyAnatomy(GameTestHelper helper) {
        helper.getLevel().getGameRules()
                .getRule(ua.meekeyn.requiemunofficial.common.init.ModGameRules.POSSESSION_KEEP_INVENTORY)
                .set(
                        ua.meekeyn.requiemunofficial.common.init.ModGameRules.POSSESSION_KEEP_INVENTORY_NEVER,
                        helper.getLevel().getServer()
                );

        net.minecraft.server.level.ServerPlayer player = new net.minecraft.server.level.ServerPlayer(
                helper.getLevel().getServer(),
                helper.getLevel(),
                new com.mojang.authlib.GameProfile(UUID.randomUUID(), "anatomy-panda-test")
        );
        net.minecraft.world.entity.animal.Panda panda =
                new net.minecraft.world.entity.animal.Panda(EntityType.PANDA, helper.getLevel());
        player.getInventory().items.set(player.getInventory().selected, new ItemStack(Items.STICK));
        player.getInventory().armor.set(3, new ItemStack(Items.IRON_HELMET));

        ua.meekeyn.requiemunofficial.common.possession.PossessionInventoryManager
                .onPossessionEnded(player, panda);
        helper.assertTrue(panda.getMainHandItem().is(Items.STICK), "Pandas should be able to hold one item");
        helper.assertTrue(
                panda.getItemBySlot(net.minecraft.world.entity.EquipmentSlot.HEAD).isEmpty(),
                "Pandas should not receive player armor"
        );
        helper.assertTrue(
                player.getInventory().armor.get(3).is(Items.IRON_HELMET),
                "Armor should stay with the soul when the host cannot wear it"
        );

        net.minecraft.server.level.ServerPlayer armoredPlayer = new net.minecraft.server.level.ServerPlayer(
                helper.getLevel().getServer(),
                helper.getLevel(),
                new com.mojang.authlib.GameProfile(UUID.randomUUID(), "anatomy-zombie-test")
        );
        net.minecraft.world.entity.monster.Zombie zombie =
                new net.minecraft.world.entity.monster.Zombie(EntityType.ZOMBIE, helper.getLevel());
        armoredPlayer.getInventory().armor.set(3, new ItemStack(Items.IRON_HELMET));

        ua.meekeyn.requiemunofficial.common.possession.PossessionInventoryManager
                .onPossessionEnded(armoredPlayer, zombie);
        helper.assertTrue(
                zombie.getItemBySlot(net.minecraft.world.entity.EquipmentSlot.HEAD).is(Items.IRON_HELMET),
                "Humanoid hosts should receive wearable armor"
        );
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void possessionTransferKeepsIndependentIdenticalEquipment(GameTestHelper helper) {
        helper.getLevel().getGameRules()
                .getRule(ua.meekeyn.requiemunofficial.common.init.ModGameRules.POSSESSION_KEEP_INVENTORY)
                .set(
                        ua.meekeyn.requiemunofficial.common.init.ModGameRules.POSSESSION_KEEP_INVENTORY_NEVER,
                        helper.getLevel().getServer()
                );

        net.minecraft.server.level.ServerPlayer player = new net.minecraft.server.level.ServerPlayer(
                helper.getLevel().getServer(),
                helper.getLevel(),
                new com.mojang.authlib.GameProfile(UUID.randomUUID(), "identical-equipment-test")
        );
        player.moveTo(helper.absolutePos(new BlockPos(1, 2, 1)), 0.0F, 0.0F);
        net.minecraft.world.entity.monster.Zombie zombie =
                new net.minecraft.world.entity.monster.Zombie(EntityType.ZOMBIE, helper.getLevel());
        zombie.moveTo(helper.absolutePos(new BlockPos(1, 2, 2)), 0.0F, 0.0F);
        helper.getLevel().addFreshEntity(zombie);

        player.getInventory().armor.set(3, new ItemStack(Items.IRON_HELMET));
        zombie.setItemSlot(net.minecraft.world.entity.EquipmentSlot.HEAD, new ItemStack(Items.IRON_HELMET));

        ua.meekeyn.requiemunofficial.common.possession.PossessionInventoryManager
                .onPossessionEnded(player, zombie);

        helper.assertTrue(player.getInventory().armor.get(3).isEmpty(),
                "Transferred player helmet should leave the player slot");
        helper.assertTrue(zombie.getItemBySlot(net.minecraft.world.entity.EquipmentSlot.HEAD).is(Items.IRON_HELMET),
                "Existing identical host helmet should remain equipped");
        long droppedHelmets = helper.getLevel().getEntitiesOfClass(
                net.minecraft.world.entity.item.ItemEntity.class,
                zombie.getBoundingBox().inflate(4.0D),
                item -> item.getItem().is(Items.IRON_HELMET)
        ).size();
        helper.assertTrue(droppedHelmets >= 1,
                "Independent identical player helmet should be dropped instead of being treated as the host's item");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void possessionReleaseCyclesDoNotDuplicateMirroredEquipment(GameTestHelper helper) {
        helper.getLevel().getGameRules()
                .getRule(ua.meekeyn.requiemunofficial.common.init.ModGameRules.POSSESSION_KEEP_INVENTORY)
                .set(
                        ua.meekeyn.requiemunofficial.common.init.ModGameRules.POSSESSION_KEEP_INVENTORY_NEVER,
                        helper.getLevel().getServer()
                );

        net.minecraft.server.level.ServerPlayer player = new net.minecraft.server.level.ServerPlayer(
                helper.getLevel().getServer(),
                helper.getLevel(),
                new com.mojang.authlib.GameProfile(UUID.randomUUID(), "mirror-cycle-test")
        );
        player.moveTo(helper.absolutePos(new BlockPos(1, 2, 1)), 0.0F, 0.0F);
        net.minecraft.world.entity.monster.Zombie zombie =
                new net.minecraft.world.entity.monster.Zombie(EntityType.ZOMBIE, helper.getLevel());
        zombie.moveTo(helper.absolutePos(new BlockPos(1, 2, 2)), 0.0F, 0.0F);
        helper.getLevel().addFreshEntity(zombie);

        ItemStack bow = new ItemStack(Items.BOW);
        bow.setDamageValue(17);
        bow.enchant(net.minecraft.world.item.enchantment.Enchantments.POWER_ARROWS, 2);
        CompoundTag bowTag = bow.getOrCreateTag();
        bowTag.putString("requiem_test_payload", "mainhand");
        ItemStack offhand = new ItemStack(Items.SHIELD);
        offhand.setDamageValue(9);
        ItemStack helmet = new ItemStack(Items.IRON_HELMET);
        helmet.enchant(net.minecraft.world.item.enchantment.Enchantments.UNBREAKING, 1);
        ItemStack chest = new ItemStack(Items.IRON_CHESTPLATE);
        chest.setDamageValue(3);
        player.getInventory().items.set(0, bow);
        player.getInventory().selected = 0;
        player.getInventory().offhand.set(0, offhand);
        player.getInventory().armor.set(3, helmet);
        player.getInventory().armor.set(2, chest);

        int before = countTestEquipment(player, zombie, helper);
        for (int i = 0; i < 100; i++) {
            ua.meekeyn.requiemunofficial.common.possession.PossessionInventoryManager
                    .onPossessionStarted(player, zombie);
            ua.meekeyn.requiemunofficial.common.possession.PossessionInventoryManager
                    .onPossessionEnded(player, zombie);
        }
        int after = countTestEquipment(player, zombie, helper);

        helper.assertTrue(before == 4, "Test setup should start with four tracked equipment stacks");
        helper.assertTrue(after == before,
                "Mirrored possession equipment must not create extra real stacks across release cycles");
        helper.assertTrue(player.getInventory().items.get(0).isEmpty()
                        && zombie.getMainHandItem().is(Items.BOW)
                        && zombie.getMainHandItem().getDamageValue() == 17
                        && zombie.getMainHandItem().getEnchantmentLevel(
                        net.minecraft.world.item.enchantment.Enchantments.POWER_ARROWS) == 2
                        && "mainhand".equals(zombie.getMainHandItem().getTag().getString("requiem_test_payload")),
                "Authoritative mainhand stack should move once with NBT, damage and enchantments intact");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void visualMirrorMutationDoesNotOverwriteSoulEquipment(GameTestHelper helper) {
        net.minecraft.server.level.ServerPlayer player = new net.minecraft.server.level.ServerPlayer(
                helper.getLevel().getServer(),
                helper.getLevel(),
                new com.mojang.authlib.GameProfile(UUID.randomUUID(), "mirror-mutation-test")
        );
        net.minecraft.world.entity.monster.Zombie zombie =
                new net.minecraft.world.entity.monster.Zombie(EntityType.ZOMBIE, helper.getLevel());
        helper.getLevel().addFreshEntity(zombie);

        ItemStack playerBow = new ItemStack(Items.BOW);
        playerBow.setDamageValue(4);
        player.getInventory().items.set(0, playerBow);
        player.getInventory().selected = 0;

        ua.meekeyn.requiemunofficial.common.possession.PossessionInventoryManager
                .syncPlayerEquipmentToHost(player, zombie);
        ItemStack mirror = zombie.getMainHandItem();
        mirror.setDamageValue(23);
        mirror.getOrCreateTag().putString("foreign_mod_payload", "changed-host-mirror");

        ua.meekeyn.requiemunofficial.common.possession.PossessionInventoryManager
                .syncPlayerEquipmentToHost(player, zombie);

        helper.assertTrue(player.getMainHandItem().is(Items.BOW)
                        && player.getMainHandItem().getDamageValue() == 4
                        && (player.getMainHandItem().getTag() == null
                        || !player.getMainHandItem().getTag().contains("foreign_mod_payload")),
                "Runtime visual mirror damage/NBT changes must not overwrite the player's real bow");
        helper.assertTrue(zombie.getMainHandItem().is(Items.BOW)
                        && zombie.getMainHandItem().getDamageValue() == 4,
                "Changed visual mirror should be rebuilt from the authoritative player stack");
        helper.succeed();
    }

    private static int countTestEquipment(net.minecraft.server.level.ServerPlayer player,
                                          net.minecraft.world.entity.LivingEntity host,
                                          GameTestHelper helper) {
        int count = 0;
        List<ItemStack> stacks = List.of(
                player.getMainHandItem(),
                player.getOffhandItem(),
                player.getInventory().armor.get(3),
                player.getInventory().armor.get(2),
                host.getMainHandItem(),
                host.getOffhandItem(),
                host.getItemBySlot(net.minecraft.world.entity.EquipmentSlot.HEAD),
                host.getItemBySlot(net.minecraft.world.entity.EquipmentSlot.CHEST)
        );
        for (ItemStack stack : stacks) {
            if (stack.is(Items.BOW)
                    || stack.is(Items.SHIELD)
                    || stack.is(Items.IRON_HELMET)
                    || stack.is(Items.IRON_CHESTPLATE)) {
                count++;
            }
        }
        count += helper.getLevel().getEntitiesOfClass(
                net.minecraft.world.entity.item.ItemEntity.class,
                host.getBoundingBox().inflate(8.0D),
                item -> item.getItem().is(Items.BOW)
                        || item.getItem().is(Items.SHIELD)
                        || item.getItem().is(Items.IRON_HELMET)
                        || item.getItem().is(Items.IRON_CHESTPLATE)
        ).size();
        return count;
    }

    private static long countInventoryItem(net.minecraft.server.level.ServerPlayer player,
                                           net.minecraft.world.item.Item item) {
        long count = 0;
        for (ItemStack stack : player.getInventory().items) {
            if (stack.is(item)) {
                count += stack.getCount();
            }
        }
        for (ItemStack stack : player.getInventory().armor) {
            if (stack.is(item)) {
                count += stack.getCount();
            }
        }
        for (ItemStack stack : player.getInventory().offhand) {
            if (stack.is(item)) {
                count += stack.getCount();
            }
        }
        return count;
    }

    @GameTest(template = "empty")
    public static void possessionInventoryCapabilitiesMatchBodyTags(GameTestHelper helper) {
        net.minecraft.world.entity.animal.Panda panda =
                new net.minecraft.world.entity.animal.Panda(EntityType.PANDA, helper.getLevel());
        net.minecraft.world.entity.monster.piglin.PiglinBrute brute =
                new net.minecraft.world.entity.monster.piglin.PiglinBrute(EntityType.PIGLIN_BRUTE, helper.getLevel());
        net.minecraft.world.entity.animal.Cow cow =
                new net.minecraft.world.entity.animal.Cow(EntityType.COW, helper.getLevel());
        net.minecraft.world.entity.animal.Fox fox =
                new net.minecraft.world.entity.animal.Fox(EntityType.FOX, helper.getLevel());
        net.minecraft.world.entity.npc.Villager villager =
                new net.minecraft.world.entity.npc.Villager(EntityType.VILLAGER, helper.getLevel());
        ua.meekeyn.requiemunofficial.common.entity.CuredVillagerEntity curedVillager =
                new ua.meekeyn.requiemunofficial.common.entity.CuredVillagerEntity(
                        ua.meekeyn.requiemunofficial.common.init.ModEntities.CURED_VILLAGER.get(),
                        helper.getLevel()
                );

        helper.assertTrue(
                ua.meekeyn.requiemunofficial.common.possession.PossessionInventoryManager.canUseItems(panda),
                "Pandas should use held items"
        );
        helper.assertTrue(
                ua.meekeyn.requiemunofficial.common.possession.PossessionInventoryManager.canCraft(panda),
                "Pandas should keep the original-style crafting capability"
        );
        helper.assertTrue(
                ua.meekeyn.requiemunofficial.common.possession.PossessionInventoryManager.canCraft(villager),
                "Villagers should have a crafting grid while possessed"
        );
        helper.assertTrue(
                ua.meekeyn.requiemunofficial.common.possession.PossessionInventoryManager.canCraft(curedVillager),
                "Cured villagers should have a crafting grid while possessed"
        );
        helper.assertTrue(
                ua.meekeyn.requiemunofficial.common.possession.PossessionInventoryManager.canCarryInventory(panda),
                "Pandas should carry an inventory"
        );
        helper.assertTrue(
                ua.meekeyn.requiemunofficial.common.possession.PossessionInventoryManager.canUseItems(brute),
                "Piglin brutes should still use held items"
        );
        helper.assertTrue(
                !ua.meekeyn.requiemunofficial.common.possession.PossessionInventoryManager.canUseArmor(brute),
                "Armor-banned bodies must not receive player armor even if otherwise humanoid"
        );
        helper.assertTrue(
                !ua.meekeyn.requiemunofficial.common.possession.PossessionInventoryManager.canUseItems(cow)
                        && !ua.meekeyn.requiemunofficial.common.possession.PossessionInventoryManager.canCarryInventory(cow),
                "Cows should not gain player-like hands or inventory"
        );
        helper.assertTrue(
                ua.meekeyn.requiemunofficial.common.possession.PossessionInventoryManager.getInventoryShape(cow)
                        == ua.meekeyn.requiemunofficial.common.possession.PossessionInventoryManager.InventoryShape.ALT_LARGE,
                "Bodies without usable inventory should use the large alternate inventory shape"
        );
        helper.assertTrue(
                ua.meekeyn.requiemunofficial.common.possession.PossessionInventoryManager.getInventoryShape(fox)
                        == ua.meekeyn.requiemunofficial.common.possession.PossessionInventoryManager.InventoryShape.ALT,
                "One-item bodies should use the alternate inventory shape without main inventory"
        );
        helper.assertTrue(
                ua.meekeyn.requiemunofficial.common.possession.PossessionInventoryManager.getInventoryShape(panda)
                        == ua.meekeyn.requiemunofficial.common.possession.PossessionInventoryManager.InventoryShape.ALT_SMALL,
                "Inventory-capable possessed bodies should use the small alternate inventory shape"
        );
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void possessedPlayerInventorySlotsFollowBodyAnatomy(GameTestHelper helper) {
        net.minecraft.server.level.ServerPlayer player = new net.minecraft.server.level.ServerPlayer(
                helper.getLevel().getServer(),
                helper.getLevel(),
                new com.mojang.authlib.GameProfile(UUID.randomUUID(), "slot-anatomy-test")
        );
        net.minecraft.world.entity.animal.Cow cow =
                new net.minecraft.world.entity.animal.Cow(EntityType.COW, helper.getLevel());
        cow.moveTo(helper.absolutePos(new BlockPos(1, 2, 1)), 0.0F, 0.0F);
        helper.getLevel().addFreshEntity(cow);

        SoulManager.setState(player, SoulState.VAGRANT);
        EntitySoulManager.removeSoul(cow);
        helper.assertTrue(PossessionManager.tryPossess(player, cow), "A vagrant should possess a soulless cow");
        helper.assertTrue(
                !ua.meekeyn.requiemunofficial.common.possession.PossessionInventoryManager
                        .canAccessPlayerInventorySlot(player, 0),
                "Bodies without hands or hotbar should lock hotbar slots"
        );
        helper.assertTrue(
                !ua.meekeyn.requiemunofficial.common.possession.PossessionInventoryManager
                        .canAccessPlayerInventorySlot(player, 10),
                "Bodies without inventory should lock main inventory slots"
        );
        helper.assertTrue(
                !ua.meekeyn.requiemunofficial.common.possession.PossessionInventoryManager
                        .canAccessPlayerInventorySlot(player, 36),
                "Bodies without armor support should lock armor slots"
        );
        helper.assertTrue(
                !ua.meekeyn.requiemunofficial.common.possession.PossessionInventoryManager
                        .canAccessPlayerInventorySlot(player, 40),
                "Bodies without hands should lock offhand slots"
        );
        net.minecraft.world.inventory.Slot hotbarSlot =
                new net.minecraft.world.inventory.Slot(player.getInventory(), 0, 0, 0);
        helper.assertTrue(!hotbarSlot.isActive(), "Locked possessed slots should be hidden/inactive");
        helper.assertTrue(
                !hotbarSlot.mayPlace(new ItemStack(Items.STICK)),
                "Locked possessed slots should reject item placement"
        );
        helper.assertTrue(
                !hotbarSlot.mayPickup(player),
                "Locked possessed slots should reject item pickup"
        );
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void possessionMobilityProfilesParse(GameTestHelper helper) {
        var profiles = EntityMobilityManager.parse(JsonParser.parseString("""
                {
                  "minecraft:bat": {
                    "flightMode": "FORCED",
                    "swimMode": "DISABLED",
                    "walkMode": "JUMPY",
                    "inertia": 0.5,
                    "gravity": 0.03,
                    "landedSpeedModifier": -0.8,
                    "waterSpeedModifier": 0.4,
                    "fallSpeedModifier": 0.9,
                    "climbsWalls": true,
                    "phasesThroughWalls": true,
                    "flopsOnLand": true
                  }
                }
                """).getAsJsonObject());
        var bat = profiles.get(ResourceLocation.tryParse("minecraft:bat"));

        helper.assertTrue(bat != null && bat.canFly(), "Bat profile should enable flight");
        helper.assertTrue(!bat.canSwim(), "Bat profile should disable swimming");
        helper.assertTrue(bat.inertia() == 0.5D, "Bat inertia should parse");
        helper.assertTrue(bat.walkMode() == EntityMobilityManager.WalkMode.JUMPY, "Walk mode should parse");
        helper.assertTrue(bat.landedSpeedModifier() == -0.8D, "Landed speed modifier should parse");
        helper.assertTrue(bat.waterSpeedModifier() == 0.4D, "Water speed modifier should parse");
        helper.assertTrue(bat.fallSpeedModifier() == 0.9D, "Fall speed modifier should parse");
        helper.assertTrue(bat.climbsWalls(), "Climbing flag should parse");
        helper.assertTrue(bat.phasesThroughWalls(), "Phasing flag should parse");
        helper.assertTrue(bat.flopsOnLand(), "Flopping flag should parse");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void possessedHealingRulesParse(GameTestHelper helper) {
        var rule = PossessedItemManager.parse(JsonParser.parseString("""
                {
                  "schema_version": 1,
                  "mob": {"type": "#requiemunofficial:zombies"},
                  "override": {
                    "type": "requiemunofficial:healing",
                    "item": {"tag": "requiemunofficial:raw_meats"},
                    "cooldown": 150,
                    "usage": "eat_to_heal"
                  }
                }
                """).getAsJsonObject());

        helper.assertTrue(rule != null, "Healing rule should parse");
        helper.assertTrue(rule.cooldown() == 150, "Healing cooldown should parse");
        helper.assertTrue(
                rule.matches(
                        new net.minecraft.world.entity.monster.Zombie(EntityType.ZOMBIE, helper.getLevel()),
                        new ItemStack(Items.BEEF)
                ),
                "Zombie should accept raw meat healing"
        );
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void possessedDietRulesParse(GameTestHelper helper) {
        var rule = PossessedItemManager.parseDiet(JsonParser.parseString("""
                {
                  "mob": {"type": "#requiemunofficial:piglins"},
                  "override": {
                    "type": "requiemunofficial:diet",
                    "food": {"tag": "requiemunofficial:food/piglin_diet"}
                  }
                }
                """).getAsJsonObject());

        helper.assertTrue(rule != null, "Diet rule should parse");
        helper.assertTrue(
                rule.matches(
                        new net.minecraft.world.entity.monster.piglin.Piglin(
                                EntityType.PIGLIN,
                                helper.getLevel()
                        ),
                        new ItemStack(Items.PORKCHOP)
                ),
                "Piglin should accept its configured diet"
        );
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void possessedCureRulesParse(GameTestHelper helper) {
        var rule = PossessedItemManager.parseCure(JsonParser.parseString("""
                {
                  "mob": {"requiemunofficial:can_be_cured": true},
                  "override": {
                    "type": "requiemunofficial:cure",
                    "reagent": {"tag": "requiemunofficial:undead_cures"}
                  }
                }
                """).getAsJsonObject());

        helper.assertTrue(rule != null, "Cure rule should parse");
        helper.assertTrue(
                rule.matches(
                        new net.minecraft.world.entity.monster.ZombieVillager(
                                EntityType.ZOMBIE_VILLAGER,
                                helper.getLevel()
                        ),
                        new ItemStack(Items.GOLDEN_APPLE)
                ),
                "Zombie villager should accept a configured cure"
        );
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void tachyliteRecipesLoad(GameTestHelper helper) {
        var recipes = helper.getLevel().getRecipeManager();
        helper.assertTrue(
                recipes.byKey(ResourceLocation.tryParse("requiemunofficial:tachylite")).isPresent(),
                "Tachylite crafting recipe should load"
        );
        helper.assertTrue(
                recipes.byKey(ResourceLocation.tryParse(
                        "requiemunofficial:stonecutting_chiseled_tachylite"
                )).isPresent(),
                "Tachylite stonecutting recipe should load"
        );
        helper.assertTrue(
                recipes.byKey(ResourceLocation.tryParse(
                        "requiemunofficial:runic_tachylite_attrition"
                )).isPresent(),
                "Runic tachylite recipe should load"
        );
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void curedEntityTypesLoad(GameTestHelper helper) {
        var villager = ModEntities.CURED_VILLAGER.get().create(helper.getLevel());
        var piglin = ModEntities.CURED_PIGLIN.get().create(helper.getLevel());
        var brute = ModEntities.CURED_PIGLIN_BRUTE.get().create(helper.getLevel());

        helper.assertTrue(villager != null, "Cured villager should be creatable");
        helper.assertTrue(piglin != null, "Cured piglin should be creatable");
        helper.assertTrue(brute != null, "Cured piglin brute should be creatable");
        helper.assertTrue(
                ModEntities.CURED_VILLAGER.get().is(
                        ua.meekeyn.requiemunofficial.common.util.ModTags.POSSESSABLES
                ),
                "Cured villager should be possessable"
        );
        helper.assertTrue(
                ModEntities.CURED_PIGLIN.get().is(
                        ua.meekeyn.requiemunofficial.common.util.ModTags.POSSESSABLES
                ),
                "Cured piglin should be possessable"
        );
        helper.assertTrue(!piglin.isConverting(), "Cured piglin should not zombify");
        helper.assertTrue(!brute.isConverting(), "Cured piglin brute should not zombify");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void deathResurrectionRulesParse(GameTestHelper helper) {
        var rule = DeathResurrectionManager.parse(JsonParser.parseString("""
                {
                  "priority": 100,
                  "killing_blow": {
                    "tags": [{"expected": true, "id": "requiemunofficial:is_lava"}]
                  },
                  "player": {"location": {"dimension": "minecraft:the_nether"}},
                  "entity": {"type": "minecraft:wither_skeleton"}
                }
                """).getAsJsonObject());

        helper.assertTrue(rule != null, "Death resurrection rule should parse");
        helper.assertTrue(rule.priority() == 100, "Death resurrection priority should parse");
        helper.assertTrue(rule.result() == EntityType.WITHER_SKELETON, "Result entity should parse");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void totemResurrectionRulesParse(GameTestHelper helper) {
        var rule = TotemResurrectionManager.parse(JsonParser.parseString("""
                {
                  "priority": 60,
                  "player": {"effects": {"minecraft:wither": {}}},
                  "consumable": {
                    "items": ["requiemunofficial:totem_of_skeletonization"]
                  },
                  "entity": {"type": "minecraft:wither_skeleton"}
                }
                """).getAsJsonObject());

        helper.assertTrue(rule != null, "Totem resurrection rule should parse");
        helper.assertTrue(rule.priority() == 60, "Totem resurrection priority should parse");
        helper.assertTrue(rule.result() == EntityType.WITHER_SKELETON, "Totem result should parse");
        helper.succeed();
    }

    /**
     * The zombie death resurrection rule only matches in darkness (light ≤ 8), so tests that
     * resurrect from a zombie killing blow must not depend on the time of day the test happens
     * to run at. Sealing the player cell in opaque blocks makes the light condition stable; the
     * actual test body has to wait a few ticks because the light engine flushes asynchronously
     * and would otherwise be observed with stale sky light.
     */
    private static void sealPlayerCellInDarkness(GameTestHelper helper, BlockPos playerCell) {
        helper.setBlock(playerCell.offset(-1, 0, 0), net.minecraft.world.level.block.Blocks.STONE);
        helper.setBlock(playerCell.offset(1, 0, 0), net.minecraft.world.level.block.Blocks.STONE);
        helper.setBlock(playerCell.offset(0, -1, 0), net.minecraft.world.level.block.Blocks.STONE);
        helper.setBlock(playerCell.offset(0, 1, 0), net.minecraft.world.level.block.Blocks.STONE);
        helper.setBlock(playerCell.offset(0, 0, -1), net.minecraft.world.level.block.Blocks.STONE);
        helper.setBlock(playerCell.offset(0, 0, 1), net.minecraft.world.level.block.Blocks.STONE);
    }

    private static final int SEALED_DARKNESS_SETTLE_TICKS = 5;

    @GameTest(template = "empty")
    public static void stage9DeathResurrectionBeatsShellCreation(GameTestHelper helper) {
        net.minecraft.server.level.ServerPlayer player = new net.minecraft.server.level.ServerPlayer(
                helper.getLevel().getServer(),
                helper.getLevel(),
                new com.mojang.authlib.GameProfile(UUID.randomUUID(), "stage9-death-test")
        );
        player.moveTo(helper.absolutePos(new BlockPos(1, 2, 1)), 0.0F, 0.0F);
        sealPlayerCellInDarkness(helper, new BlockPos(1, 2, 1));
        SoulManager.setState(player, SoulState.REMNANT);
        net.minecraft.world.entity.monster.Zombie zombie =
                new net.minecraft.world.entity.monster.Zombie(EntityType.ZOMBIE, helper.getLevel());
        helper.getLevel().addFreshEntity(zombie);

        helper.runAfterDelay(SEALED_DARKNESS_SETTLE_TICKS, () -> {
            boolean resurrected = DeathResurrectionManager.tryHandleRemnantDeath(
                    player,
                    player.damageSources().mobAttack(zombie)
            );

            helper.assertTrue(resurrected, "Matching death rule should resurrect the remnant");
            helper.assertTrue(SoulManager.getState(player) == SoulState.VAGRANT,
                    "Death resurrection should leave the player vagrant");
            helper.assertTrue(SoulManager.getPlayerShellId(player) == null,
                    "Death resurrection must not create a Stage 8 shell");
            helper.assertTrue(SoulManager.getPossessedEntityId(player) != null,
                    "Death resurrection should possess the resulting body");
            helper.succeed();
        });
    }

    @GameTest(template = "empty")
    public static void stage9DeathResurrectionDuplicateGuard(GameTestHelper helper) {
        net.minecraft.server.level.ServerPlayer player = new net.minecraft.server.level.ServerPlayer(
                helper.getLevel().getServer(),
                helper.getLevel(),
                new com.mojang.authlib.GameProfile(UUID.randomUUID(), "stage9-duplicate-test")
        );
        player.moveTo(helper.absolutePos(new BlockPos(1, 2, 1)), 0.0F, 0.0F);
        sealPlayerCellInDarkness(helper, new BlockPos(1, 2, 1));
        SoulManager.setState(player, SoulState.REMNANT);
        net.minecraft.world.entity.monster.Zombie zombie =
                new net.minecraft.world.entity.monster.Zombie(EntityType.ZOMBIE, helper.getLevel());
        helper.getLevel().addFreshEntity(zombie);

        helper.runAfterDelay(SEALED_DARKNESS_SETTLE_TICKS, () -> {
            helper.assertTrue(DeathResurrectionManager.tryHandleRemnantDeath(player, player.damageSources().mobAttack(zombie)),
                    "First matching death should resurrect");
            helper.assertTrue(!DeathResurrectionManager.tryHandleRemnantDeath(player, player.damageSources().mobAttack(zombie)),
                    "Second same-tick death should not create another body");
            helper.succeed();
        });
    }

    @GameTest(template = "empty")
    public static void stage9DeathResurrectionPreservesSoulboundInventory(GameTestHelper helper) {
        net.minecraft.server.level.ServerPlayer player = new net.minecraft.server.level.ServerPlayer(
                helper.getLevel().getServer(),
                helper.getLevel(),
                new com.mojang.authlib.GameProfile(UUID.randomUUID(), "stage9-inventory-test")
        );
        player.moveTo(helper.absolutePos(new BlockPos(1, 2, 1)), 0.0F, 0.0F);
        sealPlayerCellInDarkness(helper, new BlockPos(1, 2, 1));
        SoulManager.setState(player, SoulState.REMNANT);
        ItemStack soulbound = new ItemStack(Items.AMETHYST_SHARD);
        soulbound.getOrCreateTag().putBoolean("RequiemSoulbound", true);
        player.getInventory().items.set(3, soulbound);
        player.getInventory().items.set(4, new ItemStack(Items.DIRT, 4));
        net.minecraft.world.entity.monster.Zombie zombie =
                new net.minecraft.world.entity.monster.Zombie(EntityType.ZOMBIE, helper.getLevel());
        helper.getLevel().addFreshEntity(zombie);

        helper.runAfterDelay(SEALED_DARKNESS_SETTLE_TICKS, () -> {
            helper.assertTrue(DeathResurrectionManager.tryHandleRemnantDeath(player, player.damageSources().mobAttack(zombie)),
                    "Matching death should resurrect");
            helper.assertTrue(player.getInventory().items.get(3).is(Items.AMETHYST_SHARD),
                    "Soulbound stack should remain soul-owned");
            helper.assertTrue(player.getInventory().items.get(4).is(Items.DIRT)
                            && player.getInventory().items.get(4).getCount() == 4,
                    "Forge death-resurrection adaptation must not duplicate or clear soul inventory");
            helper.assertTrue(SoulLifecycleManager.findShell(player) == null,
                    "No shell should receive a duplicate body inventory");
            helper.succeed();
        });
    }

    @GameTest(template = "empty")
    public static void stage9PossessedHostTotemConsumesItemAndMovesPossession(GameTestHelper helper) {
        net.minecraft.server.level.ServerPlayer player = new net.minecraft.server.level.ServerPlayer(
                helper.getLevel().getServer(),
                helper.getLevel(),
                new com.mojang.authlib.GameProfile(UUID.randomUUID(), "stage9-host-totem-test")
        );
        player.moveTo(helper.absolutePos(new BlockPos(1, 2, 1)), 0.0F, 0.0F);
        SoulManager.setState(player, SoulState.VAGRANT);
        net.minecraft.world.entity.monster.Zombie host =
                new net.minecraft.world.entity.monster.Zombie(EntityType.ZOMBIE, helper.getLevel());
        host.moveTo(helper.absolutePos(new BlockPos(2, 2, 1)), 0.0F, 0.0F);
        host.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND,
                new ItemStack(ModItems.TOTEM_OF_SKELETONIZATION.get()));
        helper.getLevel().addFreshEntity(host);
        helper.assertTrue(PossessionManager.tryPossess(player, host), "Setup possession should succeed");

        helper.assertTrue(DeathResurrectionManager.tryHandlePossessedHostDeath(
                        player,
                        host,
                        player.damageSources().generic()
                ),
                "Possessed host totem rule should resurrect into a skeleton body");
        helper.assertTrue(host.getMainHandItem().isEmpty(), "Totem should be consumed from the host hand");
        helper.assertTrue(SoulManager.getPossessedEntityId(player) != null,
                "Player should still possess a body after totem resurrection");
        helper.assertTrue(!host.getUUID().equals(SoulManager.getPossessedEntityId(player)),
                "Possession should move away from the dying host");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void stage9HostDeathAppliesAttrition(GameTestHelper helper) {
        net.minecraft.server.level.ServerPlayer player = new net.minecraft.server.level.ServerPlayer(
                helper.getLevel().getServer(),
                helper.getLevel(),
                new com.mojang.authlib.GameProfile(UUID.randomUUID(), "stage9-attrition-test")
        );
        player.moveTo(helper.absolutePos(new BlockPos(1, 2, 1)), 0.0F, 0.0F);
        SoulManager.setState(player, SoulState.VAGRANT);
        net.minecraft.world.entity.monster.Zombie host =
                new net.minecraft.world.entity.monster.Zombie(EntityType.ZOMBIE, helper.getLevel());
        host.moveTo(helper.absolutePos(new BlockPos(2, 2, 1)), 0.0F, 0.0F);
        helper.getLevel().addFreshEntity(host);
        helper.assertTrue(PossessionManager.tryPossess(player, host), "Setup possession should succeed");

        PossessionManager.applyHostDeathAttrition(player);

        helper.assertTrue(player.hasEffect(ModEffects.ATTRITION.get()),
                "Possessed host death should apply attrition to the possessor");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void possessionAttackConeRejectsTargetsBehindHost(GameTestHelper helper) {
        net.minecraft.world.entity.monster.Zombie target =
                new net.minecraft.world.entity.monster.Zombie(EntityType.ZOMBIE, helper.getLevel());
        target.setPos(0.0D, 0.0D, 3.0D);

        helper.assertTrue(
                PossessionManager.isInAttackCone(
                        new net.minecraft.world.phys.Vec3(0.0D, target.getEyeY(), 0.0D),
                        new net.minecraft.world.phys.Vec3(0.0D, 0.0D, 1.0D),
                        target,
                        4.5D
                ),
                "Target in front of the host should be attackable"
        );
        helper.assertTrue(
                !PossessionManager.isInAttackCone(
                        new net.minecraft.world.phys.Vec3(0.0D, target.getEyeY(), 0.0D),
                        new net.minecraft.world.phys.Vec3(0.0D, 0.0D, -1.0D),
                        target,
                        4.5D
                ),
                "Target behind the host should not be attackable"
        );
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void possessedIronGolemUsesBodyStrength(GameTestHelper helper) {
        net.minecraft.server.level.ServerPlayer player = new net.minecraft.server.level.ServerPlayer(
                helper.getLevel().getServer(),
                helper.getLevel(),
                new com.mojang.authlib.GameProfile(UUID.randomUUID(), "golem-attack-test")
        );
        net.minecraft.world.entity.animal.IronGolem host =
                new net.minecraft.world.entity.animal.IronGolem(EntityType.IRON_GOLEM, helper.getLevel());
        net.minecraft.world.entity.monster.Zombie target =
                new net.minecraft.world.entity.monster.Zombie(EntityType.ZOMBIE, helper.getLevel());
        host.moveTo(0.0D, 2.0D, 0.0D, 0.0F, 0.0F);
        target.moveTo(0.0D, 2.0D, 2.0D, 0.0F, 0.0F);
        helper.getLevel().addFreshEntity(host);
        helper.getLevel().addFreshEntity(target);

        float healthBefore = target.getHealth();
        helper.assertTrue(
                PossessionManager.trySpecialAttackForTest(player, host, target),
                "Possessed iron golems should use their body slam attack"
        );
        helper.assertTrue(target.getHealth() < healthBefore,
                "Iron golem body attack should deal heavy damage");
        helper.assertTrue(target.getDeltaMovement().y >= 0.8D,
                "Iron golem body attack should launch the target upward");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void possessionReleaseRequiresTwoBlocksOfSpace(GameTestHelper helper) {
        net.minecraft.world.entity.monster.Zombie host =
                new net.minecraft.world.entity.monster.Zombie(EntityType.ZOMBIE, helper.getLevel());
        BlockPos relativePos = new BlockPos(1, 1, 1);
        BlockPos releasePos = helper.absolutePos(relativePos);
        host.setPos(releasePos.getX(), releasePos.getY(), releasePos.getZ());

        helper.setBlock(relativePos, net.minecraft.world.level.block.Blocks.STONE);
        helper.assertTrue(
                !PossessionManager.isSafeReleasePosition(host, releasePos),
                "Occupied positions should not be used for possession release"
        );

        helper.setBlock(relativePos, net.minecraft.world.level.block.Blocks.AIR);
        helper.setBlock(relativePos.above(), net.minecraft.world.level.block.Blocks.AIR);
        helper.assertTrue(
                PossessionManager.isSafeReleasePosition(host, releasePos),
                "Two empty blocks should be valid for possession release"
        );
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void possessionStateCopiesAndClears(GameTestHelper helper) {
        UUID hostId = UUID.randomUUID();
        UUID shellId = UUID.randomUUID();
        PlayerSoul original = new PlayerSoul();
        original.setState(SoulState.REMNANT);
        original.setPossessedEntityId(hostId);
        original.setPlayerShellId(shellId);

        PlayerSoul copy = new PlayerSoul();
        copy.copyFrom(original);
        helper.assertTrue(copy.getState() == SoulState.REMNANT, "Soul state should copy");
        helper.assertTrue(hostId.equals(copy.getPossessedEntityId()), "Possessed host should copy");
        helper.assertTrue(shellId.equals(copy.getPlayerShellId()), "Player shell should copy");

        copy.clearPossessedEntityId();
        copy.setState(SoulState.VAGRANT);
        helper.assertTrue(copy.getPossessedEntityId() == null, "Possessed host should clear");
        helper.assertTrue(copy.getState() == SoulState.VAGRANT, "Released soul should become vagrant");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void playerShellOwnerPersists(GameTestHelper helper) {
        UUID ownerId = UUID.randomUUID();
        PlayerShellEntity original =
                new PlayerShellEntity(ModEntities.PLAYER_SHELL.get(), helper.getLevel());
        original.setOwner(ownerId, net.minecraft.network.chat.Component.literal("Test owner"));
        original.setItemSlot(net.minecraft.world.entity.EquipmentSlot.HEAD, new ItemStack(Items.IRON_HELMET));
        original.setDropChance(net.minecraft.world.entity.EquipmentSlot.HEAD, 0.0F);
        original.setHealth(7.0F);

        PlayerShellEntity loaded =
                new PlayerShellEntity(ModEntities.PLAYER_SHELL.get(), helper.getLevel());
        loaded.load(original.saveWithoutId(new CompoundTag()));

        helper.assertTrue(loaded.belongsTo(ownerId), "Player shell owner should persist");
        helper.assertTrue(loaded.isNoAi(), "Player shell should remain inert");
        helper.assertTrue(loaded.getHealth() == 7.0F, "Player shell health should persist");
        helper.assertTrue(
                loaded.getItemBySlot(net.minecraft.world.entity.EquipmentSlot.HEAD).is(Items.IRON_HELMET),
                "Player shell equipment should persist"
        );
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void requiemGameRulesAreRegistered(GameTestHelper helper) {
        var rules = helper.getLevel().getGameRules();
        var server = helper.getLevel().getServer();
        rules.getRule(ua.meekeyn.requiemunofficial.common.init.ModGameRules.SHOW_POSSESSOR_NAME_TAG)
                .set(false, server);
        rules.getRule(ua.meekeyn.requiemunofficial.common.init.ModGameRules.POSSESSION_KEEP_INVENTORY)
                .set(ua.meekeyn.requiemunofficial.common.init.ModGameRules.POSSESSION_KEEP_INVENTORY_NEVER, server);
        helper.assertTrue(
                !rules.getBoolean(ua.meekeyn.requiemunofficial.common.init.ModGameRules.SHOW_POSSESSOR_NAME_TAG),
                "Possessor name tag gamerule should be writable"
        );
        helper.assertTrue(
                !rules.getBoolean(ua.meekeyn.requiemunofficial.common.init.ModGameRules.DROP_VESSEL_ON_DISCONNECT),
                "Dropped vessel gamerule should be disabled by default"
        );
        helper.assertTrue(
                ua.meekeyn.requiemunofficial.common.init.ModGameRules.getStartingRemnantType(rules)
                        == ua.meekeyn.requiemunofficial.common.init.ModGameRules.STARTING_REMNANT_CHOOSE,
                "Starting remnant type should default to choose"
        );
        helper.assertTrue(
                ua.meekeyn.requiemunofficial.common.init.ModGameRules.getPossessionKeepInventory(rules)
                        == ua.meekeyn.requiemunofficial.common.init.ModGameRules.POSSESSION_KEEP_INVENTORY_NEVER,
                "Possession inventory gamerule should be writable"
        );
        helper.assertTrue(
                ua.meekeyn.requiemunofficial.common.init.ModGameRules.getPossessionDetection(rules)
                        == ua.meekeyn.requiemunofficial.common.init.ModGameRules.POSSESSION_DETECTION_NORMAL,
                "Possession detection should default to normal"
        );
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void droppedVesselAnchorRoundTrips(GameTestHelper helper) {
        net.minecraft.server.level.ServerPlayer player = new net.minecraft.server.level.ServerPlayer(
                helper.getLevel().getServer(),
                helper.getLevel(),
                new com.mojang.authlib.GameProfile(UUID.randomUUID(), "vessel-anchor-test")
        );
        net.minecraft.world.entity.monster.Zombie host =
                new net.minecraft.world.entity.monster.Zombie(EntityType.ZOMBIE, helper.getLevel());
        host.moveTo(2.5D, 3.0D, 4.5D);

        ua.meekeyn.requiemunofficial.common.remnant.DroppedVesselTracker.saveAnchor(player, host);
        CompoundTag anchor =
                ua.meekeyn.requiemunofficial.common.remnant.DroppedVesselTracker.getAnchor(player);

        helper.assertTrue(anchor != null, "Dropped vessel anchor should be stored");
        helper.assertTrue(host.getUUID().equals(anchor.getUUID("Anchor")), "Anchor UUID should persist");
        helper.assertTrue(
                helper.getLevel().dimension().location().toString().equals(anchor.getString("Dimension")),
                "Anchor dimension should persist"
        );
        helper.assertTrue(anchor.getDouble("X") == 2.5D && anchor.getDouble("Z") == 4.5D,
                "Anchor position should persist");

        ua.meekeyn.requiemunofficial.common.remnant.DroppedVesselTracker.clearAnchor(player);
        helper.assertTrue(
                ua.meekeyn.requiemunofficial.common.remnant.DroppedVesselTracker.getAnchor(player) == null,
                "Dropped vessel anchor should clear after reconnect"
        );
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void possessionGameRuleSemanticsMatchOriginal(GameTestHelper helper) {
        var disabled = ua.meekeyn.requiemunofficial.common.init.ModGameRules.POSSESSION_DETECTION_DISABLED;
        var normal = ua.meekeyn.requiemunofficial.common.init.ModGameRules.POSSESSION_DETECTION_NORMAL;
        var hard = ua.meekeyn.requiemunofficial.common.init.ModGameRules.POSSESSION_DETECTION_HARD;

        helper.assertTrue(
                ua.meekeyn.requiemunofficial.common.possession.PossessionDetectionManager.resultFor(
                        disabled,
                        ua.meekeyn.requiemunofficial.common.possession.PossessionDetectionManager.DetectionReason.ATTACKED
                ) == ua.meekeyn.requiemunofficial.common.possession.PossessionDetectionManager.DetectionResult.UNDETECTED,
                "Disabled detection should never reveal possession"
        );
        helper.assertTrue(
                ua.meekeyn.requiemunofficial.common.possession.PossessionDetectionManager.resultFor(
                        normal,
                        ua.meekeyn.requiemunofficial.common.possession.PossessionDetectionManager.DetectionReason.BUMP
                ) == ua.meekeyn.requiemunofficial.common.possession.PossessionDetectionManager.DetectionResult.UNDETECTED,
                "Normal bump detection must not create aggression by possession alone"
        );
        helper.assertTrue(
                ua.meekeyn.requiemunofficial.common.possession.PossessionDetectionManager.resultFor(
                        hard,
                        ua.meekeyn.requiemunofficial.common.possession.PossessionDetectionManager.DetectionReason.ATTACKING
                ) == ua.meekeyn.requiemunofficial.common.possession.PossessionDetectionManager.DetectionResult.CROWD_DETECTED,
                "Hard detection should alert the crowd"
        );
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void possessionInventoryMovesThroughLivingHost(GameTestHelper helper) {
        helper.getLevel().getGameRules()
                .getRule(ua.meekeyn.requiemunofficial.common.init.ModGameRules.POSSESSION_KEEP_INVENTORY)
                .set(
                        ua.meekeyn.requiemunofficial.common.init.ModGameRules.POSSESSION_KEEP_INVENTORY_NEVER,
                        helper.getLevel().getServer()
                );
        net.minecraft.server.level.ServerPlayer player = new net.minecraft.server.level.ServerPlayer(
                helper.getLevel().getServer(),
                helper.getLevel(),
                new com.mojang.authlib.GameProfile(UUID.randomUUID(), "inventory-test")
        );
        net.minecraft.world.entity.monster.Zombie host =
                new net.minecraft.world.entity.monster.Zombie(EntityType.ZOMBIE, helper.getLevel());
        player.getInventory().items.set(
                player.getInventory().selected,
                new ItemStack(Items.DIAMOND_SWORD)
        );
        player.getInventory().items.set(10, new ItemStack(Items.DIRT, 42));

        ua.meekeyn.requiemunofficial.common.possession.PossessionInventoryManager
                .onPossessionEnded(player, host);
        helper.assertTrue(
                player.getMainHandItem().isEmpty() && player.getInventory().items.get(10).isEmpty(),
                "Default inventory rule should move soul items into a living host"
        );
        helper.assertTrue(
                host.getMainHandItem().is(Items.DIAMOND_SWORD),
                "Item users should retain the possessor's equipment"
        );

        ua.meekeyn.requiemunofficial.common.possession.PossessionInventoryManager
                .onPossessionStarted(player, host);
        helper.assertTrue(
                player.getMainHandItem().is(Items.DIAMOND_SWORD),
                "Equipment should return when the host is possessed again"
        );
        helper.assertTrue(
                player.getInventory().items.get(10).is(Items.DIRT)
                        && player.getInventory().items.get(10).getCount() == 42,
                "Stored host inventory should return to its original slot"
        );
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void creativePossessedBodyIgnoresNormalDamageButNotKill(GameTestHelper helper) {
        net.minecraft.server.level.ServerPlayer player = new net.minecraft.server.level.ServerPlayer(
                helper.getLevel().getServer(),
                helper.getLevel(),
                new com.mojang.authlib.GameProfile(UUID.randomUUID(), "creative-host-protection-test")
        );
        player.getAbilities().instabuild = true;
        SoulManager.setState(player, SoulState.VAGRANT);

        net.minecraft.world.entity.monster.Blaze host =
                new net.minecraft.world.entity.monster.Blaze(EntityType.BLAZE, helper.getLevel());
        host.moveTo(helper.absolutePos(new BlockPos(1, 2, 1)), 0.0F, 0.0F);
        helper.getLevel().addFreshEntity(host);
        EntitySoulManager.removeSoul(host);

        helper.assertTrue(PossessionManager.tryPossess(player, host), "Creative vagrant should possess the Blaze");
        float health = host.getHealth();
        host.hurt(host.damageSources().lava(), 1000.0F);
        helper.assertTrue(host.isAlive() && host.getHealth() == health,
                "Creative possessed body should ignore ordinary damage");

        host.hurt(host.damageSources().genericKill(), Float.MAX_VALUE);
        helper.assertTrue(!host.isAlive(), "Generic kill should bypass creative possessed body protection");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void possessedCommandKillSelfKillsHostOnly(GameTestHelper helper) {
        net.minecraft.server.level.ServerPlayer player = createPossessedSkeletonPlayer(helper, "cmdkill");
        net.minecraft.world.entity.LivingEntity host = PossessionManager.findPossessedHost(
                player,
                SoulManager.getPossessedEntityId(player)
        );
        helper.assertTrue(host != null, "Possessed skeleton host should exist");

        int result = helper.getLevel().getServer().getCommands().performPrefixedCommand(
                helper.getLevel().getServer().createCommandSourceStack()
                        .withLevel(helper.getLevel())
                        .withEntity(player)
                        .withPermission(4)
                        .withSuppressedOutput(),
                "kill @s"
        );

        helper.assertTrue(result == 1, "Redirected kill command should report one killed target");
        helper.assertTrue(!host.isAlive(), "Kill @s should kill the possessed skeleton host");
        helper.assertTrue(player.isAlive(), "Kill @s while possessing must not kill the player body");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void possessedCommandEffectTargetsHostAndClearWorks(GameTestHelper helper) {
        net.minecraft.server.level.ServerPlayer player = createPossessedSkeletonPlayer(helper, "cmdeffect");
        net.minecraft.world.entity.LivingEntity host = PossessionManager.findPossessedHost(
                player,
                SoulManager.getPossessedEntityId(player)
        );
        helper.assertTrue(host != null, "Possessed skeleton host should exist");

        int give = helper.getLevel().getServer().getCommands().performPrefixedCommand(
                helper.getLevel().getServer().createCommandSourceStack()
                        .withLevel(helper.getLevel())
                        .withEntity(player)
                        .withPermission(4)
                        .withSuppressedOutput(),
                "effect give @s minecraft:glowing 10 0 true"
        );
        helper.assertTrue(give == 1, "Effect give should succeed once");
        helper.assertTrue(host.hasEffect(net.minecraft.world.effect.MobEffects.GLOWING),
                "Glowing should be applied to the skeleton host");
        helper.assertTrue(!player.hasEffect(net.minecraft.world.effect.MobEffects.GLOWING),
                "Glowing must not be applied to the player body");

        int clear = helper.getLevel().getServer().getCommands().performPrefixedCommand(
                helper.getLevel().getServer().createCommandSourceStack()
                        .withLevel(helper.getLevel())
                        .withEntity(player)
                        .withPermission(4)
                        .withSuppressedOutput(),
                "effect clear @s minecraft:glowing"
        );
        helper.assertTrue(clear == 1, "Effect clear should report the host effect removal");
        helper.assertTrue(!host.hasEffect(net.minecraft.world.effect.MobEffects.GLOWING),
                "Effect clear should remove the effect from the host");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void beta16SkeletonDelegatesDynamicSpeedEffects(GameTestHelper helper) {
        net.minecraft.server.level.ServerPlayer player = createPossessedSkeletonPlayer(helper, "dynspeed");
        net.minecraft.world.entity.LivingEntity host = PossessionManager.findPossessedHost(
                player,
                SoulManager.getPossessedEntityId(player)
        );
        helper.assertTrue(host != null, "Possessed skeleton host should exist");
        helper.assertTrue(PossessionManager.isUsingBeta16PlayerBody(player),
                "Skeleton should use beta16 Player Body");

        net.minecraft.world.entity.ai.attributes.AttributeInstance playerSpeed =
                player.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.MOVEMENT_SPEED);
        helper.assertTrue(playerSpeed != null, "Player movement speed attribute should exist");
        double normal = playerSpeed.getValue();

        host.addEffect(new net.minecraft.world.effect.MobEffectInstance(
                net.minecraft.world.effect.MobEffects.MOVEMENT_SPEED,
                200,
                0
        ));
        PossessionManager.controlHostForTest(player, host);
        double spedUp = playerSpeed.getValue();
        helper.assertTrue(spedUp > normal, "Host Speed should increase Player Body movement speed");

        host.removeEffect(net.minecraft.world.effect.MobEffects.MOVEMENT_SPEED);
        PossessionManager.controlHostForTest(player, host);
        helper.assertTrue(Math.abs(playerSpeed.getValue() - normal) < 1.0E-6D,
                "Removing host Speed should restore normal Player Body movement speed");

        host.addEffect(new net.minecraft.world.effect.MobEffectInstance(
                net.minecraft.world.effect.MobEffects.MOVEMENT_SLOWDOWN,
                200,
                0
        ));
        PossessionManager.controlHostForTest(player, host);
        helper.assertTrue(playerSpeed.getValue() < normal,
                "Host Slowness should reduce Player Body movement speed");

        PossessionManager.release(player, host);
        helper.assertTrue(playerSpeed.getModifier(UUID.fromString("f25420ed-4041-4cc6-87fa-6e5272dddb2b")) == null,
                "Beta16 speed modifier should be removed on release");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void blazeUsesBeta16PlayerBodyAndFlightProfile(GameTestHelper helper) {
        net.minecraft.server.level.ServerPlayer player = createPossessedBlazePlayer(helper, "blaze-beta16-test");
        net.minecraft.world.entity.LivingEntity host = PossessionManager.findPossessedHost(
                player,
                SoulManager.getPossessedEntityId(player)
        );
        helper.assertTrue(host != null, "Possessed Blaze host should exist");
        helper.assertTrue(PossessionManager.isUsingBeta16PlayerBody(player),
                "Blaze should use beta16 Player Body");
        helper.assertTrue(player.getCamera() == player, "Blaze beta16 possession should keep the player camera");
        helper.assertTrue(player.isNoGravity(), "Blaze Player Body should inherit forced flight no-gravity");
        helper.assertTrue(host.isNoGravity(), "Possessed Blaze host should remain no-gravity while following player");

        PossessionManager.updatePossessionInput(player, new ua.meekeyn.requiemunofficial.common.network.PossessionInputPacket(
                0.0F,
                0.0F,
                true,
                false,
                0.0F,
                0.0F
        ));
        PossessionManager.controlHostForTest(player, host);
        helper.assertTrue(player.getDeltaMovement().y > 0.0D,
                "Blaze jump input should move the Player Body upward");

        player.setDeltaMovement(0.04D, player.getDeltaMovement().y, -0.03D);
        PossessionManager.updatePossessionInput(player, new ua.meekeyn.requiemunofficial.common.network.PossessionInputPacket(
                1.0F,
                1.0F,
                false,
                true,
                0.0F,
                0.0F
        ));
        PossessionManager.controlHostForTest(player, host);
        helper.assertTrue(player.getDeltaMovement().y < 0.0D,
                "Blaze shift input should move the Player Body downward");
        helper.assertTrue(Math.abs(player.getDeltaMovement().x - 0.04D) < 1.0E-6D
                        && Math.abs(player.getDeltaMovement().z + 0.03D) < 1.0E-6D,
                "Blaze vertical movement must not erase concurrent horizontal Player Body movement");

        PossessionManager.updatePossessionInput(player, new ua.meekeyn.requiemunofficial.common.network.PossessionInputPacket(
                0.0F,
                0.0F,
                false,
                false,
                0.0F,
                0.0F
        ));
        double previousY = player.getDeltaMovement().y;
        PossessionManager.controlHostForTest(player, host);
        helper.assertTrue(Math.abs(player.getDeltaMovement().y) < Math.abs(previousY),
                "Released Blaze vertical input should decay by the flight inertia profile");

        PossessionManager.release(player, host);
        helper.assertTrue(!player.isNoGravity(), "Release should restore normal player gravity");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void beta16BlazeMovementSpeedIsScaledForPlayerFov(GameTestHelper helper) {
        net.minecraft.server.level.ServerPlayer player = createPossessedBlazePlayer(helper, "blaze-fov-test");
        net.minecraft.world.entity.LivingEntity host = PossessionManager.findPossessedHost(
                player,
                SoulManager.getPossessedEntityId(player)
        );
        helper.assertTrue(host != null, "Possessed Blaze host should exist");

        net.minecraft.world.entity.ai.attributes.AttributeInstance playerSpeed =
                player.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.MOVEMENT_SPEED);
        helper.assertTrue(playerSpeed != null, "Player movement speed attribute should exist");
        double playerBase = player.getAttributeBaseValue(net.minecraft.world.entity.ai.attributes.Attributes.MOVEMENT_SPEED);
        double hostSpeed = host.getAttributeValue(net.minecraft.world.entity.ai.attributes.Attributes.MOVEMENT_SPEED);

        PossessionManager.controlHostForTest(player, host);
        helper.assertTrue(playerSpeed.getValue() < hostSpeed,
                "Blaze Player Body must not use raw Blaze movement speed for vanilla FOV");
        helper.assertTrue(Math.abs(playerSpeed.getValue() - hostSpeed * 0.4D) < 1.0E-6D,
                "Blaze Player Body speed should use the flight mobility scale");

        PossessionManager.controlHostForTest(player, host);
        helper.assertTrue(playerSpeed.getModifier(UUID.fromString("f25420ed-4041-4cc6-87fa-6e5272dddb2b")) != null,
                "Beta16 speed modifier should remain a single replaceable modifier");

        PossessionManager.release(player, host);
        helper.assertTrue(Math.abs(playerSpeed.getValue() - playerBase) < 1.0E-6D,
                "Release should restore the player's base movement speed");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void beta16PlayerBodyFovIgnoresHostSpeedSubstitution(GameTestHelper helper) {
        net.minecraft.server.level.ServerPlayer player = new net.minecraft.server.level.ServerPlayer(
                helper.getLevel().getServer(),
                helper.getLevel(),
                new com.mojang.authlib.GameProfile(UUID.randomUUID(), "beta16-fov-speed-test")
        );
        player.moveTo(helper.absolutePos(new BlockPos(1, 2, 1)), 0.0F, 0.0F);
        SoulManager.setState(player, SoulState.VAGRANT);

        net.minecraft.world.entity.monster.Skeleton skeleton =
                new net.minecraft.world.entity.monster.Skeleton(EntityType.SKELETON, helper.getLevel());
        skeleton.moveTo(helper.absolutePos(new BlockPos(2, 2, 1)), 0.0F, 0.0F);
        skeleton.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.MOVEMENT_SPEED).setBaseValue(0.35D);
        helper.getLevel().addFreshEntity(skeleton);
        EntitySoulManager.removeSoul(skeleton);

        double walkFovSpeed = PossessionManager.beta16BodyFovMovementSpeed(
                player, player.getAttributeValue(net.minecraft.world.entity.ai.attributes.Attributes.MOVEMENT_SPEED));
        helper.assertTrue(Math.abs(walkFovSpeed - player.getAbilities().getWalkingSpeed()) < 1.0E-6D,
                "Unpossessed FOV speed must stay the vanilla player walking speed");

        player.setSprinting(true);
        double sprintFovSpeed = PossessionManager.beta16BodyFovMovementSpeed(
                player, player.getAttributeValue(net.minecraft.world.entity.ai.attributes.Attributes.MOVEMENT_SPEED));
        helper.assertTrue(Math.abs(sprintFovSpeed - player.getAbilities().getWalkingSpeed() * 1.3D) < 1.0E-6D,
                "Unpossessed sprint FOV speed must keep the vanilla sprint multiplier");

        player.setSprinting(false);
        helper.assertTrue(PossessionManager.tryPossess(player, skeleton), "Skeleton possession should start");
        double bodySpeed = player.getAttributeValue(net.minecraft.world.entity.ai.attributes.Attributes.MOVEMENT_SPEED);
        helper.assertTrue(Math.abs(bodySpeed - 0.14D) < 1.0E-6D,
                "Possessed skeleton body should carry the scaled host speed on the player attribute");
        helper.assertTrue(Math.abs(PossessionManager.beta16BodyFovMovementSpeed(player, bodySpeed)
                        - player.getAbilities().getWalkingSpeed()) < 1.0E-6D,
                "Walking possessed FOV speed must ignore the host speed substitution");

        player.setSprinting(true);
        PossessionManager.controlHostForTest(player, skeleton);
        PossessionManager.controlHostForTest(player, skeleton);
        double sprintBodySpeed = player.getAttributeValue(net.minecraft.world.entity.ai.attributes.Attributes.MOVEMENT_SPEED);
        helper.assertTrue(Math.abs(sprintBodySpeed - 0.182D) < 1.0E-6D,
                "Possessed skeleton sprint should apply the player sprint multiplier exactly once");
        helper.assertTrue(Math.abs(PossessionManager.beta16BodyFovMovementSpeed(player, sprintBodySpeed)
                        - player.getAbilities().getWalkingSpeed() * 1.3D) < 1.0E-6D,
                "Sprinting possessed FOV speed must keep only the vanilla sprint multiplier");

        PossessionManager.release(player, skeleton);
        player.setSprinting(false);
        double restoredFovSpeed = PossessionManager.beta16BodyFovMovementSpeed(
                player, player.getAttributeValue(net.minecraft.world.entity.ai.attributes.Attributes.MOVEMENT_SPEED));
        helper.assertTrue(Math.abs(restoredFovSpeed - player.getAbilities().getWalkingSpeed()) < 1.0E-6D,
                "Released player FOV speed must return to the vanilla walking speed");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void beta16SprintSpeedAppliesOncePerBodyCategory(GameTestHelper helper) {
        net.minecraft.server.level.ServerPlayer player = new net.minecraft.server.level.ServerPlayer(
                helper.getLevel().getServer(),
                helper.getLevel(),
                new com.mojang.authlib.GameProfile(UUID.randomUUID(), "beta16-sprint-once-test")
        );
        player.moveTo(helper.absolutePos(new BlockPos(1, 2, 1)), 0.0F, 0.0F);
        SoulManager.setState(player, SoulState.VAGRANT);

        net.minecraft.world.entity.monster.Skeleton skeleton =
                new net.minecraft.world.entity.monster.Skeleton(EntityType.SKELETON, helper.getLevel());
        skeleton.moveTo(helper.absolutePos(new BlockPos(2, 2, 1)), 0.0F, 0.0F);
        skeleton.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.MOVEMENT_SPEED).setBaseValue(0.35D);
        helper.getLevel().addFreshEntity(skeleton);
        EntitySoulManager.removeSoul(skeleton);

        net.minecraft.world.entity.monster.Zombie zombie =
                new net.minecraft.world.entity.monster.Zombie(EntityType.ZOMBIE, helper.getLevel());
        zombie.moveTo(helper.absolutePos(new BlockPos(3, 2, 1)), 0.0F, 0.0F);
        helper.getLevel().addFreshEntity(zombie);
        EntitySoulManager.removeSoul(zombie);

        net.minecraft.world.entity.monster.Spider spider =
                new net.minecraft.world.entity.monster.Spider(EntityType.SPIDER, helper.getLevel());
        spider.moveTo(helper.absolutePos(new BlockPos(4, 2, 1)), 0.0F, 0.0F);
        helper.getLevel().addFreshEntity(spider);
        EntitySoulManager.removeSoul(spider);

        player.setSprinting(true);

        helper.assertTrue(PossessionManager.tryPossess(player, skeleton), "Skeleton possession should start");
        PossessionManager.controlHostForTest(player, skeleton);
        PossessionManager.controlHostForTest(player, skeleton);
        helper.assertTrue(Math.abs(player.getAttributeValue(net.minecraft.world.entity.ai.attributes.Attributes.MOVEMENT_SPEED)
                        - 0.182D) < 1.0E-6D,
                "Humanoid body sprint must not stack the mirrored host sprint modifier");
        PossessionManager.release(player, skeleton);

        helper.assertTrue(PossessionManager.tryPossess(player, zombie), "Zombie possession should start");
        PossessionManager.controlHostForTest(player, zombie);
        PossessionManager.controlHostForTest(player, zombie);
        helper.assertTrue(Math.abs(player.getAttributeValue(net.minecraft.world.entity.ai.attributes.Attributes.MOVEMENT_SPEED)
                        - 0.23D * 0.4D * 1.3D) < 1.0E-6D,
                "Zombie body sprint must not stack the mirrored host sprint modifier");
        PossessionManager.release(player, zombie);

        double spiderBase = spider.getAttributeValue(net.minecraft.world.entity.ai.attributes.Attributes.MOVEMENT_SPEED);
        helper.assertTrue(PossessionManager.tryPossess(player, spider), "Spider possession should start");
        PossessionManager.controlHostForTest(player, spider);
        PossessionManager.controlHostForTest(player, spider);
        double spiderSprintSpeed = player.getAttributeValue(net.minecraft.world.entity.ai.attributes.Attributes.MOVEMENT_SPEED);
        helper.assertTrue(Math.abs(spiderSprintSpeed - spiderBase * 1.3D) < 1.0E-6D,
                "Ground body must carry the native speed with a single sprint attribute modifier");
        PossessionManager.release(player, spider);

        player.setSprinting(false);
        helper.assertTrue(Math.abs(player.getAttributeValue(net.minecraft.world.entity.ai.attributes.Attributes.MOVEMENT_SPEED)
                        - 0.1D) < 1.0E-6D,
                "Release should restore the player base movement speed after every body category");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void beta16BlazeAttackCreatesOneServerFireballBurst(GameTestHelper helper) {
        net.minecraft.server.level.ServerPlayer player = createPossessedBlazePlayer(helper, "blaze-attack-test");
        net.minecraft.world.entity.LivingEntity host = PossessionManager.findPossessedHost(
                player,
                SoulManager.getPossessedEntityId(player)
        );
        helper.assertTrue(host != null, "Possessed Blaze host should exist");

        PossessionManager.handleAction(player, ua.meekeyn.requiemunofficial.common.network.PossessionActionPacket.Action.ATTACK);
        PossessionManager.handleAction(player, ua.meekeyn.requiemunofficial.common.network.PossessionActionPacket.Action.ATTACK);

        helper.runAfterDelay(61, () -> {
            PossessionManager.processHostAbilitiesForTest(host);
            long firstCount = helper.getLevel().getEntitiesOfClass(
                    net.minecraft.world.entity.projectile.SmallFireball.class,
                    new net.minecraft.world.phys.AABB(host.blockPosition()).inflate(16.0D)
            ).size();
            helper.assertTrue(firstCount == 1,
                    "First Blaze attack processing tick should create exactly one server fireball");

            PossessionManager.processHostAbilitiesForTest(host);
            long secondCount = helper.getLevel().getEntitiesOfClass(
                    net.minecraft.world.entity.projectile.SmallFireball.class,
                    new net.minecraft.world.phys.AABB(host.blockPosition()).inflate(16.0D)
            ).size();
            helper.assertTrue(secondCount == firstCount,
                    "Repeated processing before the Blaze burst delay must not duplicate projectiles");
            helper.succeed();
        });
    }

    @GameTest(template = "empty", timeoutTicks = 180, batch = "blaze_projectile_regression")
    public static void beta16BlazeFireballDamagesAndIgnitesTarget(GameTestHelper helper) {
        helper.getLevel().getEntitiesOfClass(net.minecraft.world.entity.LivingEntity.class,
                new net.minecraft.world.phys.AABB(helper.absolutePos(new BlockPos(0, 3, 0)),
                        helper.absolutePos(new BlockPos(4, 7, 10))))
                .forEach(net.minecraft.world.entity.Entity::discard);
        // Траса виходить за межі empty; очищаємо залишки інших тестових конструкцій.
        for (int x = 0; x <= 3; x++) {
            for (int y = 3; y <= 6; y++) {
                for (int z = 0; z <= 9; z++) {
                    helper.setBlock(new BlockPos(x, y, z), net.minecraft.world.level.block.Blocks.AIR);
                }
            }
        }
        net.minecraft.server.level.ServerPlayer player = createPossessedBlazePlayer(helper, "blaze-hit-test");
        net.minecraft.world.entity.LivingEntity host = PossessionManager.findPossessedHost(
                player,
                SoulManager.getPossessedEntityId(player)
        );
        helper.assertTrue(host != null, "Possessed Blaze host should exist");
        helper.getLevel().setWeatherParameters(6000, 0, false, false);

        net.minecraft.world.entity.animal.Cow target =
                new net.minecraft.world.entity.animal.Cow(EntityType.COW, helper.getLevel());
        // Нерухома ціль ізолює влучання від випадкового руху та падіння між перевірками.
        target.setNoAi(true);
        target.setNoGravity(true);
        target.moveTo(helper.absolutePos(new BlockPos(1, 4, 8)), 0.0F, 0.0F);
        helper.getLevel().addFreshEntity(target);
        float initialHealth = target.getHealth();

        helper.assertTrue(PossessionManager.trySpecialAttackForTest(player, host, target),
                "Possessed Blaze should accept a target fireball attack");
        helper.runAfterDelay(61, () -> {
            // Сталий розкид перевіряє зіткнення, а не випадкову влучність одного пострілу.
            helper.assertTrue(target.getHealth() == initialHealth && target.getRemainingFireTicks() <= 0,
                    "Target must be unharmed and unlit before the shot");
            host.getRandom().setSeed(0L);
            helper.assertTrue(host.hasLineOfSight(target), "The fireball test corridor must remain clear");
            PossessionManager.processHostAbilitiesForTest(host);
            helper.assertTrue(!helper.getLevel().getEntitiesOfClass(
                    net.minecraft.world.entity.projectile.SmallFireball.class, host.getBoundingBox().inflate(4.0D),
                    projectile -> projectile.getOwner() == host).isEmpty(),
                    "Attack must spawn a real fireball owned by the possessed Blaze");
        });
        helper.runAfterDelay(90, () -> {
            helper.assertTrue(target.getHealth() < initialHealth,
                    "A real SmallFireball hit should damage the target through vanilla projectile damage");
            helper.assertTrue(target.getRemainingFireTicks() > 0,
                    "A real SmallFireball hit should ignite a burnable target; water=" + target.isInWater()
                            + ", health=" + target.getHealth() + ", fireTicks=" + target.getRemainingFireTicks());
            helper.succeed();
        });
    }

    @GameTest(template = "empty", timeoutTicks = 120)
    public static void beta16BlazeFireballDoesNotHitOwnPlayerBodyOnSpawn(GameTestHelper helper) {
        net.minecraft.server.level.ServerPlayer player = createPossessedBlazePlayer(helper, "blaze-self-test");
        net.minecraft.world.entity.LivingEntity host = PossessionManager.findPossessedHost(
                player,
                SoulManager.getPossessedEntityId(player)
        );
        helper.assertTrue(host != null, "Possessed Blaze host should exist");
        float playerHealth = player.getHealth();
        float hostHealth = host.getHealth();
        player.setYRot(0.0F);
        player.setXRot(0.0F);
        host.setYRot(0.0F);
        host.setXRot(0.0F);

        PossessionManager.handleAction(player, ua.meekeyn.requiemunofficial.common.network.PossessionActionPacket.Action.ATTACK);
        helper.runAfterDelay(61, () -> {
            PossessionManager.processHostAbilitiesForTest(host);
            long fireballs = helper.getLevel().getEntitiesOfClass(
                    net.minecraft.world.entity.projectile.SmallFireball.class,
                    new net.minecraft.world.phys.AABB(host.blockPosition()).inflate(8.0D)
            ).stream().filter(fireball -> fireball.getOwner() == host).count();
            helper.assertTrue(fireballs == 1, "Blaze should spawn one owned server SmallFireball");
            helper.assertTrue(player.getHealth() == playerHealth,
                    "Fresh Blaze fireball must not damage the overlapping Player Body at spawn");
            helper.assertTrue(host.getHealth() == hostHealth,
                    "Fresh Blaze fireball must not damage its own host at spawn");
            helper.succeed();
        });
    }

    @GameTest(template = "empty", timeoutTicks = 180)
    public static void beta16BlazeFireballRespectsMobGriefingForFire(GameTestHelper helper) {
        net.minecraft.server.level.ServerPlayer player = createPossessedBlazePlayer(helper, "blaze-fire-test");
        net.minecraft.world.entity.LivingEntity host = PossessionManager.findPossessedHost(
                player,
                SoulManager.getPossessedEntityId(player)
        );
        helper.assertTrue(host != null, "Possessed Blaze host should exist");
        helper.getLevel().setWeatherParameters(6000, 0, false, false);
        boolean oldMobGriefing = helper.getLevel().getGameRules()
                .getBoolean(net.minecraft.world.level.GameRules.RULE_MOBGRIEFING);
        helper.getLevel().getGameRules().getRule(net.minecraft.world.level.GameRules.RULE_MOBGRIEFING)
                .set(true, helper.getLevel().getServer());
        player.setYRot(0.0F);
        player.setXRot(0.0F);
        host.setYRot(0.0F);
        host.setXRot(0.0F);
        BlockPos wall = helper.absolutePos(new BlockPos(1, 5, 6));
        helper.getLevel().setBlock(wall, net.minecraft.world.level.block.Blocks.STONE.defaultBlockState(), 3);
        helper.getLevel().setBlock(wall.offset(0, -1, -1), net.minecraft.world.level.block.Blocks.STONE.defaultBlockState(), 3);

        PossessionManager.handleAction(player, ua.meekeyn.requiemunofficial.common.network.PossessionActionPacket.Action.ATTACK);
        helper.runAfterDelay(61, () -> PossessionManager.processHostAbilitiesForTest(host));
        helper.runAfterDelay(100, () -> {
            boolean firePlaced = net.minecraft.core.BlockPos.betweenClosedStream(wall.offset(-1, -1, -1), wall.offset(1, 1, 1))
                    .anyMatch(pos -> helper.getLevel().getBlockState(pos).is(net.minecraft.world.level.block.Blocks.FIRE));
            helper.getLevel().getGameRules().getRule(net.minecraft.world.level.GameRules.RULE_MOBGRIEFING)
                    .set(oldMobGriefing, helper.getLevel().getServer());
            helper.assertTrue(firePlaced,
                    "Blaze SmallFireball should place fire near a valid block hit when mobGriefing is enabled");
            helper.succeed();
        });
    }

    @GameTest(template = "empty", timeoutTicks = 180)
    public static void beta16BlazeFireballDoesNotPlaceFireWhenMobGriefingDisabled(GameTestHelper helper) {
        net.minecraft.server.level.ServerPlayer player = createPossessedBlazePlayer(helper, "blaze-nofire-test");
        net.minecraft.world.entity.LivingEntity host = PossessionManager.findPossessedHost(
                player,
                SoulManager.getPossessedEntityId(player)
        );
        helper.assertTrue(host != null, "Possessed Blaze host should exist");
        helper.getLevel().setWeatherParameters(6000, 0, false, false);
        boolean oldMobGriefing = helper.getLevel().getGameRules()
                .getBoolean(net.minecraft.world.level.GameRules.RULE_MOBGRIEFING);
        helper.getLevel().getGameRules().getRule(net.minecraft.world.level.GameRules.RULE_MOBGRIEFING)
                .set(false, helper.getLevel().getServer());
        player.setYRot(0.0F);
        player.setXRot(0.0F);
        host.setYRot(0.0F);
        host.setXRot(0.0F);
        BlockPos wall = helper.absolutePos(new BlockPos(1, 5, 6));
        helper.getLevel().setBlock(wall, net.minecraft.world.level.block.Blocks.STONE.defaultBlockState(), 3);
        helper.getLevel().setBlock(wall.offset(0, -1, -1), net.minecraft.world.level.block.Blocks.STONE.defaultBlockState(), 3);

        PossessionManager.handleAction(player, ua.meekeyn.requiemunofficial.common.network.PossessionActionPacket.Action.ATTACK);
        helper.runAfterDelay(61, () -> PossessionManager.processHostAbilitiesForTest(host));
        helper.runAfterDelay(100, () -> {
            boolean firePlaced = net.minecraft.core.BlockPos.betweenClosedStream(wall.offset(-1, -1, -1), wall.offset(1, 1, 1))
                    .anyMatch(pos -> helper.getLevel().getBlockState(pos).is(net.minecraft.world.level.block.Blocks.FIRE));
            helper.getLevel().getGameRules().getRule(net.minecraft.world.level.GameRules.RULE_MOBGRIEFING)
                    .set(oldMobGriefing, helper.getLevel().getServer());
            helper.assertTrue(!firePlaced,
                    "Blaze SmallFireball must not place fire when mobGriefing is disabled");
            helper.succeed();
        });
    }

    @GameTest(template = "empty")
    public static void beta16SkeletonPossessionDoesNotGrantSurvivalFlight(GameTestHelper helper) {
        net.minecraft.server.level.ServerPlayer player = createPossessedSkeletonPlayer(helper, "skeleton-no-flight-test");
        net.minecraft.world.entity.LivingEntity host = PossessionManager.findPossessedHost(
                player,
                SoulManager.getPossessedEntityId(player)
        );
        helper.assertTrue(host != null, "Possessed Skeleton host should exist");
        player.getAbilities().mayfly = true;
        player.getAbilities().flying = true;

        PossessionManager.controlHostForTest(player, host);

        helper.assertTrue(!player.getAbilities().mayfly, "Survival Skeleton possession must not keep mayfly");
        helper.assertTrue(!player.getAbilities().flying, "Survival Skeleton possession must not keep flying");
        helper.assertTrue(!player.isNoGravity(), "Survival Skeleton possession must keep gravity");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void beta16BlazeFlightDoesNotLeakIntoLaterSkeletonPossession(GameTestHelper helper) {
        net.minecraft.server.level.ServerPlayer player = createPossessedBlazePlayer(helper, "blaze-to-skeleton-flight-test");
        net.minecraft.world.entity.LivingEntity blaze = PossessionManager.findPossessedHost(
                player,
                SoulManager.getPossessedEntityId(player)
        );
        helper.assertTrue(blaze != null, "Possessed Blaze host should exist");
        PossessionManager.controlHostForTest(player, blaze);
        helper.assertTrue(player.getAbilities().mayfly, "Survival Blaze possession should grant controlled flight");
        PossessionManager.release(player, blaze);

        net.minecraft.world.entity.monster.Skeleton skeleton =
                new net.minecraft.world.entity.monster.Skeleton(EntityType.SKELETON, helper.getLevel());
        skeleton.moveTo(helper.absolutePos(new BlockPos(2, 2, 1)), 0.0F, 0.0F);
        helper.getLevel().addFreshEntity(skeleton);
        EntitySoulManager.removeSoul(skeleton);

        helper.assertTrue(PossessionManager.tryPossess(player, skeleton),
                "Skeleton possession should start after Blaze release");
        PossessionManager.controlHostForTest(player, skeleton);

        helper.assertTrue(!player.getAbilities().mayfly, "Blaze mayfly must not leak into Skeleton possession");
        helper.assertTrue(!player.getAbilities().flying, "Blaze flying must not leak into Skeleton possession");
        helper.assertTrue(!player.isNoGravity(), "Blaze no-gravity must not leak into Skeleton possession");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void leafChargeDoesNotCarryIntoStoneCollisionSuppression(GameTestHelper helper) {
        net.minecraft.server.level.ServerPlayer player = new net.minecraft.server.level.ServerPlayer(
                helper.getLevel().getServer(),
                helper.getLevel(),
                new com.mojang.authlib.GameProfile(UUID.randomUUID(), "leaf-stone-bypass-test")
        );
        player.moveTo(helper.absolutePos(new BlockPos(1, 2, 1)), 0.0F, 0.0F);
        SoulManager.setState(player, SoulState.VAGRANT);
        helper.getLevel().setBlock(helper.absolutePos(new BlockPos(1, 2, 1)),
                net.minecraft.world.level.block.Blocks.OAK_LEAVES.defaultBlockState(), 3);
        helper.getLevel().setBlock(helper.absolutePos(new BlockPos(1, 2, 2)),
                net.minecraft.world.level.block.Blocks.STONE.defaultBlockState(), 3);
        player.getPersistentData().putInt(VagrantRules.LEAF_PHASE_CHARGE_TICKS, VagrantRules.LEAF_PHASE_REQUIRED_TICKS);
        VagrantRules.setLeafPhaseActive(player, true);

        helper.assertTrue(VagrantRules.shouldSuppressOnlyLeafCollision(player),
                "Charged leaf entry should use leaf-only collision filtering");
        helper.assertTrue(!VagrantRules.shouldSuppressCollisionWith(
                        player,
                        net.minecraft.world.level.block.Blocks.STONE.defaultBlockState()
                ),
                "Leaf permission must not suppress stone collision before the 60 tick charge");

        for (int i = 0; i < VagrantRules.PHASE_REQUIRED_TICKS - 1; i++) {
            tickVagrantPlayer(player);
        }
        helper.assertTrue(!VagrantRules.isPhaseCharged(player),
                "The crown should not hand out solid passage before the full 60 tick charge");
        helper.assertTrue(!VagrantRules.shouldSuppressCollisionWith(
                        player,
                        net.minecraft.world.level.block.Blocks.STONE.defaultBlockState()
                ),
                "Stone must stay solid inside the crown until the charge is complete");

        tickVagrantPlayer(player);
        helper.assertTrue(VagrantRules.isPhaseCharged(player),
                "Staying inside the leaf crown should complete the 60 tick charge");

        helper.assertTrue(VagrantRules.shouldSuppressCollisionWith(
                        player,
                        net.minecraft.world.level.block.Blocks.STONE.defaultBlockState()
                ),
                "After the charge the vagrant should pass the stone inside the crown");
        double crownZ = player.getZ();
        player.move(net.minecraft.world.entity.MoverType.SELF, new net.minecraft.world.phys.Vec3(0.0D, 0.0D, 1.0D));
        helper.assertTrue(player.getZ() - crownZ > 0.95D,
                "After the charge the vagrant should fly through the stone behind the leaves");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void beta16SkeletonBowUseStateMirrorsToHost(GameTestHelper helper) {
        net.minecraft.server.level.ServerPlayer player = createPossessedSkeletonPlayer(helper, "skeleton-bow-use");
        net.minecraft.world.entity.LivingEntity host = PossessionManager.findPossessedHost(
                player,
                SoulManager.getPossessedEntityId(player)
        );
        helper.assertTrue(host != null, "Possessed skeleton host should exist");
        ItemStack bow = new ItemStack(Items.BOW);
        player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, bow.copy());
        host.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, bow.copy());

        player.startUsingItem(net.minecraft.world.InteractionHand.MAIN_HAND);
        PossessionManager.controlHostForTest(player, host);
        helper.assertTrue(player.isUsingItem(), "Player Body should keep the vanilla bow use state");
        helper.assertTrue(host.isUsingItem(), "Skeleton host should mirror bow use for third-person animation");
        helper.assertTrue(host.getUsedItemHand() == net.minecraft.world.InteractionHand.MAIN_HAND,
                "Skeleton host should mirror the active bow hand");
        helper.assertTrue(host instanceof net.minecraft.world.entity.monster.AbstractSkeleton skeleton
                        && skeleton.isAggressive(),
                "Skeleton host should expose the aggressive bow pose flag while drawing a bow");

        player.stopUsingItem();
        PossessionManager.controlHostForTest(player, host);
        helper.assertTrue(!host.isUsingItem(), "Skeleton host bow use state should stop when the player stops using");
        helper.assertTrue(!(host instanceof net.minecraft.world.entity.monster.AbstractSkeleton skeleton)
                        || !skeleton.isAggressive(),
                "Skeleton host should clear the bow pose flag when bow use stops");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void possessedBlazePlayerIdentityDoesNotCreateFalseMonsterTarget(GameTestHelper helper) {
        net.minecraft.server.level.ServerPlayer player = new net.minecraft.server.level.ServerPlayer(
                helper.getLevel().getServer(),
                helper.getLevel(),
                new com.mojang.authlib.GameProfile(UUID.randomUUID(), "blaze-target-test")
        );
        SoulManager.setState(player, SoulState.VAGRANT);

        net.minecraft.world.entity.monster.Blaze host =
                new net.minecraft.world.entity.monster.Blaze(EntityType.BLAZE, helper.getLevel());
        net.minecraft.world.entity.monster.Zombie zombie =
                new net.minecraft.world.entity.monster.Zombie(EntityType.ZOMBIE, helper.getLevel());
        helper.getLevel().addFreshEntity(host);
        helper.getLevel().addFreshEntity(zombie);
        EntitySoulManager.removeSoul(host);

        helper.assertTrue(PossessionManager.tryPossess(player, host), "Vagrant should possess Blaze");
        helper.assertTrue(PossessionManager.shouldCancelTargetForTest(zombie, player),
                "A monster targeting the hidden player body should be canceled for an unprovoked monster host");

        PossessionManager.markProvokedByPossessedHostForTest(host, zombie);
        helper.assertTrue(!PossessionManager.shouldCancelTargetForTest(zombie, player),
                "Provocation should allow normal aggression toward the possessed monster host");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void possessionPreservesExistingMonsterTarget(GameTestHelper helper) {
        net.minecraft.server.level.ServerPlayer player = new net.minecraft.server.level.ServerPlayer(
                helper.getLevel().getServer(), helper.getLevel(),
                new com.mojang.authlib.GameProfile(UUID.randomUUID(), "existing-hostility")
        );
        SoulManager.setState(player, SoulState.VAGRANT);
        net.minecraft.world.entity.monster.Blaze host =
                new net.minecraft.world.entity.monster.Blaze(EntityType.BLAZE, helper.getLevel());
        net.minecraft.world.entity.monster.Zombie zombie =
                new net.minecraft.world.entity.monster.Zombie(EntityType.ZOMBIE, helper.getLevel());
        host.moveTo(helper.absolutePos(new BlockPos(1, 2, 1)), 0.0F, 0.0F);
        zombie.moveTo(helper.absolutePos(new BlockPos(3, 2, 1)), 0.0F, 0.0F);
        helper.getLevel().addFreshEntity(host);
        helper.getLevel().addFreshEntity(zombie);
        EntitySoulManager.removeSoul(host);
        zombie.setTarget(host);

        helper.assertTrue(PossessionManager.tryPossess(player, host), "Possession should start");
        helper.assertTrue(zombie.getTarget() == host, "Existing hostile target should remain");
        helper.assertTrue(!PossessionManager.shouldCancelTargetForTest(zombie, host),
                "Existing hostility must survive possession filtering");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void projectileHitProvokesPossessedShooterWithDetectionDisabled(GameTestHelper helper) {
        net.minecraft.server.level.ServerPlayer player = createPossessedSkeletonPlayer(helper, "projectile-aggro");
        net.minecraft.world.entity.LivingEntity host = PossessionManager.findPossessedHost(
                player, SoulManager.getPossessedEntityId(player));
        helper.assertTrue(host != null, "Possessed skeleton should exist");
        net.minecraft.world.entity.monster.Zombie zombie =
                new net.minecraft.world.entity.monster.Zombie(EntityType.ZOMBIE, helper.getLevel());
        helper.getLevel().addFreshEntity(zombie);
        net.minecraft.world.entity.projectile.Arrow arrow =
                new net.minecraft.world.entity.projectile.Arrow(helper.getLevel(), host);
        int oldDetection = helper.getLevel().getGameRules().getInt(
                ua.meekeyn.requiemunofficial.common.init.ModGameRules.POSSESSION_DETECTION);
        helper.getLevel().getGameRules().getRule(
                ua.meekeyn.requiemunofficial.common.init.ModGameRules.POSSESSION_DETECTION)
                .set(ua.meekeyn.requiemunofficial.common.init.ModGameRules.POSSESSION_DETECTION_DISABLED,
                        helper.getLevel().getServer());
        try {
            var source = helper.getLevel().damageSources().arrow(arrow, host);
            ua.meekeyn.requiemunofficial.common.possession.PossessionDetectionManager.onLivingAttack(
                    new net.minecraftforge.event.entity.living.LivingAttackEvent(zombie, source, 2.0F));
            helper.assertTrue(zombie.getTarget() == host,
                    "Projectile victim should target the possessed shooter");
            helper.assertTrue(!PossessionManager.shouldCancelTargetForTest(zombie, host),
                    "Projectile retaliation should survive possession target filtering");
        } finally {
            helper.getLevel().getGameRules().getRule(
                    ua.meekeyn.requiemunofficial.common.init.ModGameRules.POSSESSION_DETECTION)
                    .set(oldDetection, helper.getLevel().getServer());
        }
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void possessedCommandDamageUsesRequestedDamageTypeOnce(GameTestHelper helper) {
        net.minecraft.server.level.ServerPlayer player = createPossessedSkeletonPlayer(helper, "cmddamage");
        net.minecraft.world.entity.LivingEntity host = PossessionManager.findPossessedHost(
                player,
                SoulManager.getPossessedEntityId(player)
        );
        helper.assertTrue(host != null, "Possessed skeleton host should exist");
        float playerHealth = player.getHealth();
        float hostHealth = host.getHealth();

        int result = helper.getLevel().getServer().getCommands().performPrefixedCommand(
                helper.getLevel().getServer().createCommandSourceStack()
                        .withLevel(helper.getLevel())
                        .withEntity(player)
                        .withPermission(4)
                        .withSuppressedOutput(),
                "damage @s 3 minecraft:magic"
        );

        helper.assertTrue(result == 1, "Damage command should report one damaged target");
        helper.assertTrue(player.getHealth() == playerHealth,
                "Damage command must not damage the player body");
        helper.assertTrue(host.getHealth() == hostHealth - 3.0F,
                "Damage command should damage the host exactly once");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void possessedEquipmentSyncKeepsWornHostHelmet(GameTestHelper helper) {
        net.minecraft.server.level.ServerPlayer player = createPossessedSkeletonPlayer(helper, "equipsync");
        net.minecraft.world.entity.LivingEntity host = PossessionManager.findPossessedHost(
                player,
                SoulManager.getPossessedEntityId(player)
        );
        helper.assertTrue(host != null, "Possessed skeleton host should exist");
        ItemStack hostHelmet = host.getItemBySlot(net.minecraft.world.entity.EquipmentSlot.HEAD);
        helper.assertTrue(hostHelmet.is(Items.IRON_HELMET), "Skeleton host should receive a copied player helmet");

        hostHelmet.setDamageValue(7);
        ua.meekeyn.requiemunofficial.common.possession.PossessionInventoryManager
                .syncPlayerEquipmentToHost(player, host);

        helper.assertTrue(host.getItemBySlot(net.minecraft.world.entity.EquipmentSlot.HEAD).getDamageValue() == 7,
                "Equipment sync must not restore host helmet durability from the player copy");
        helper.assertTrue(player.getInventory().armor.get(3).getDamageValue() == 7,
                "Player equipment state should receive the host helmet's durability loss");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void possessedEaterStoresSeparateHungerData(GameTestHelper helper) {
        net.minecraft.server.level.ServerPlayer player = new net.minecraft.server.level.ServerPlayer(
                helper.getLevel().getServer(),
                helper.getLevel(),
                new com.mojang.authlib.GameProfile(UUID.randomUUID(), "possessed-hunger-test")
        );
        net.minecraft.world.entity.monster.piglin.Piglin host =
                new net.minecraft.world.entity.monster.piglin.Piglin(EntityType.PIGLIN, helper.getLevel());
        helper.getLevel().addFreshEntity(host);

        player.getFoodData().setFoodLevel(7);
        SoulManager.setState(player, SoulState.VAGRANT);
        EntitySoulManager.removeSoul(host);

        helper.assertTrue(
                PossessionManager.tryPossess(player, host),
                "A vagrant should possess a soulless piglin"
        );
        helper.assertTrue(player.getFoodData().getFoodLevel() == 20,
                "Possessed eater bodies should start with their own hunger data");

        player.getFoodData().setFoodLevel(3);
        PossessionManager.release(player, host);

        helper.assertTrue(player.getFoodData().getFoodLevel() == 7,
                "Leaving an eater body should restore the possessor's previous hunger");
        helper.assertTrue(
                host.getPersistentData().getCompound("RequiemPossessedFood").getInt("foodLevel") == 3,
                "The possessed body should keep its separate hunger data"
        );
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void vagrantLeafPhaseUsesOneSecondGrace(GameTestHelper helper) {
        net.minecraft.server.level.ServerPlayer player = new net.minecraft.server.level.ServerPlayer(
                helper.getLevel().getServer(),
                helper.getLevel(),
                new com.mojang.authlib.GameProfile(UUID.randomUUID(), "leaf-grace-test")
        );
        player.moveTo(helper.absolutePos(new BlockPos(1, 2, 1)), 0.0F, 0.0F);
        SoulManager.setState(player, SoulState.VAGRANT);
        helper.setBlock(new BlockPos(2, 2, 1), net.minecraft.world.level.block.Blocks.OAK_LEAVES);

        for (int i = 0; i < VagrantRules.LEAF_PHASE_REQUIRED_TICKS - 1; i++) {
            VagrantEvents.setVagrantInput(player, false, false, true);
            tickVagrantPlayer(player);
        }
        helper.assertTrue(!VagrantRules.isLeafPhaseActive(player), "Leaves should require the full 15 tick charge");

        VagrantEvents.setVagrantInput(player, false, false, true);
        tickVagrantPlayer(player);
        helper.assertTrue(VagrantRules.isLeafPhaseActive(player), "Leaf phasing should become active after 15 ticks");

        VagrantEvents.setVagrantInput(player, false, false, false);
        for (int i = 0; i < 10; i++) {
            tickVagrantPlayer(player);
        }
        helper.assertTrue(VagrantRules.isLeafPhaseActive(player), "Returning within 10 ticks should keep leaf permission");

        for (int i = 0; i < 11; i++) {
            tickVagrantPlayer(player);
        }
        helper.assertTrue(!VagrantRules.isLeafPhaseActive(player), "Leaf permission should expire after the grace window");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void leafPhaseDoesNotSuppressStoneOrStairs(GameTestHelper helper) {
        net.minecraft.server.level.ServerPlayer player = new net.minecraft.server.level.ServerPlayer(
                helper.getLevel().getServer(),
                helper.getLevel(),
                new com.mojang.authlib.GameProfile(UUID.randomUUID(), "leaf-only-test")
        );
        player.moveTo(helper.absolutePos(new BlockPos(1, 2, 1)), 0.0F, 0.0F);
        SoulManager.setState(player, SoulState.VAGRANT);
        player.getPersistentData().putInt(VagrantRules.LEAF_PHASE_CHARGE_TICKS, VagrantRules.LEAF_PHASE_REQUIRED_TICKS);
        VagrantRules.setLeafPhaseActive(player, true);

        helper.assertTrue(VagrantRules.shouldSuppressCollisionWith(
                        player,
                        net.minecraft.world.level.block.Blocks.OAK_LEAVES.defaultBlockState()
                ),
                "Leaf permission should suppress leaf collision");
        helper.assertTrue(!VagrantRules.shouldSuppressCollisionWith(
                        player,
                        net.minecraft.world.level.block.Blocks.STONE.defaultBlockState()
                ),
                "Leaf permission must not suppress stone collision");
        helper.assertTrue(!VagrantRules.shouldSuppressCollisionWith(
                        player,
                        net.minecraft.world.level.block.Blocks.OAK_STAIRS.defaultBlockState()
                ),
                "Leaf permission must not suppress stair collision");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void vagrantSenseTargetsNearbyPossessableBodies(GameTestHelper helper) {
        net.minecraft.server.level.ServerPlayer player = new net.minecraft.server.level.ServerPlayer(
                helper.getLevel().getServer(),
                helper.getLevel(),
                new com.mojang.authlib.GameProfile(UUID.randomUUID(), "sense-target-test")
        );
        player.moveTo(helper.absolutePos(new BlockPos(1, 2, 1)), 0.0F, 0.0F);
        SoulManager.setState(player, SoulState.VAGRANT);

        net.minecraft.world.entity.animal.Cow soulless =
                new net.minecraft.world.entity.animal.Cow(EntityType.COW, helper.getLevel());
        soulless.moveTo(helper.absolutePos(new BlockPos(3, 2, 1)), 0.0F, 0.0F);
        helper.getLevel().addFreshEntity(soulless);
        EntitySoulManager.removeSoul(soulless);

        net.minecraft.world.entity.animal.Cow normal =
                new net.minecraft.world.entity.animal.Cow(EntityType.COW, helper.getLevel());
        normal.moveTo(helper.absolutePos(new BlockPos(4, 2, 1)), 0.0F, 0.0F);
        helper.getLevel().addFreshEntity(normal);

        net.minecraft.world.entity.decoration.ArmorStand bystander =
                new net.minecraft.world.entity.decoration.ArmorStand(EntityType.ARMOR_STAND, helper.getLevel());
        bystander.moveTo(helper.absolutePos(new BlockPos(5, 2, 1)), 0.0F, 0.0F);
        helper.getLevel().addFreshEntity(bystander);

        helper.assertTrue(PossessionManager.isVagrantSenseTargetForTest(player, soulless),
                "Soulless empty body should be sensed");
        helper.assertTrue(!PossessionManager.isVagrantSenseTargetForTest(player, normal),
                "Body still holding its own soul can not be possessed yet and must not be sensed");
        helper.assertTrue(!PossessionManager.isVagrantSenseTargetForTest(player, bystander),
                "Body that can never be possessed must not be sensed");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void vagrantSenseTargetsIncludeZombieAndSkeletonWithSoul(GameTestHelper helper) {
        net.minecraft.server.level.ServerPlayer player = new net.minecraft.server.level.ServerPlayer(
                helper.getLevel().getServer(),
                helper.getLevel(),
                new com.mojang.authlib.GameProfile(UUID.randomUUID(), "sense-zombie-skeleton-test")
        );
        player.moveTo(helper.absolutePos(new BlockPos(1, 2, 1)), 0.0F, 0.0F);
        SoulManager.setState(player, SoulState.VAGRANT);

        net.minecraft.world.entity.monster.Zombie zombie =
                new net.minecraft.world.entity.monster.Zombie(EntityType.ZOMBIE, helper.getLevel());
        zombie.moveTo(helper.absolutePos(new BlockPos(3, 2, 1)), 0.0F, 0.0F);
        helper.getLevel().addFreshEntity(zombie);
        zombie.getPersistentData().putBoolean("RequiemSoulInitialized", true);
        zombie.getPersistentData().putBoolean("RequiemHasSoul", true);

        net.minecraft.world.entity.monster.Skeleton skeleton =
                new net.minecraft.world.entity.monster.Skeleton(EntityType.SKELETON, helper.getLevel());
        skeleton.moveTo(helper.absolutePos(new BlockPos(4, 2, 1)), 0.0F, 0.0F);
        helper.getLevel().addFreshEntity(skeleton);
        skeleton.getPersistentData().putBoolean("RequiemSoulInitialized", true);
        skeleton.getPersistentData().putBoolean("RequiemHasSoul", true);

        net.minecraft.world.entity.animal.Cow farAway =
                new net.minecraft.world.entity.animal.Cow(EntityType.COW, helper.getLevel());
        farAway.moveTo(helper.absolutePos(new BlockPos(72, 2, 1)), 0.0F, 0.0F);
        helper.getLevel().addFreshEntity(farAway);

        java.util.List<Integer> ids = PossessionManager.collectVagrantSenseTargetIdsForTest(player);
        helper.assertTrue(ids.contains(zombie.getId()),
                "Zombie carrying a soul flag must still be sensed as a potential host");
        helper.assertTrue(ids.contains(skeleton.getId()),
                "Skeleton carrying a soul flag must still be sensed as a potential host");
        helper.assertTrue(!ids.contains(farAway.getId()),
                "Entities outside the 64 block sense radius must not be sensed");

        ua.meekeyn.requiemunofficial.common.network.VagrantSenseTargetsPacket packet =
                new ua.meekeyn.requiemunofficial.common.network.VagrantSenseTargetsPacket(ids);
        net.minecraft.network.FriendlyByteBuf buffer = new net.minecraft.network.FriendlyByteBuf(io.netty.buffer.Unpooled.buffer());
        ua.meekeyn.requiemunofficial.common.network.VagrantSenseTargetsPacket.encode(packet, buffer);
        ua.meekeyn.requiemunofficial.common.network.VagrantSenseTargetsPacket decoded =
                ua.meekeyn.requiemunofficial.common.network.VagrantSenseTargetsPacket.decode(buffer);
        helper.assertTrue(decoded.entityIds().equals(ids),
                "VagrantSenseTargetsPacket must carry the collected entity ids unchanged");
        helper.assertTrue(decoded.entityIds().contains(zombie.getId())
                        && decoded.entityIds().contains(skeleton.getId()),
                "Zombie and skeleton ids must survive the packet round trip");
        buffer.release();
        helper.succeed();
    }

    @GameTest(template = "empty", timeoutTicks = 120)
    public static void possessedBlazeActionFireballDealsRealDamage(GameTestHelper helper) {
        net.minecraft.server.level.ServerPlayer player = createPossessedBlazePlayer(helper, "blaze-real-hit");
        net.minecraft.world.entity.LivingEntity host = PossessionManager.findPossessedHost(
                player,
                SoulManager.getPossessedEntityId(player)
        );
        helper.assertTrue(host != null, "Possessed Blaze host should exist");
        net.minecraft.world.entity.monster.Zombie target =
                new net.minecraft.world.entity.monster.Zombie(EntityType.ZOMBIE, helper.getLevel());
        target.moveTo(host.getX(), host.getY(), host.getZ() + 2.5D, 0.0F, 0.0F);
        helper.getLevel().addFreshEntity(target);
        host.setYRot(0.0F);
        host.setYHeadRot(0.0F);
        host.yBodyRot = 0.0F;
        float healthBefore = target.getHealth();

        PossessionManager.handleAction(player, ua.meekeyn.requiemunofficial.common.network.PossessionActionPacket.Action.ATTACK);
        for (int i = 0; i < 60; i++) {
            PossessionManager.processHostAbilitiesForTest(host);
            helper.getLevel().tickNonPassenger(target);
            for (SmallFireball fireball : helper.getLevel().getEntitiesOfClass(
                    SmallFireball.class,
                    host.getBoundingBox().inflate(8.0D))) {
                helper.getLevel().tickNonPassenger(fireball);
            }
        }

        helper.assertTrue(target.getHealth() < healthBefore,
                "Possessed Blaze action fireball should damage through the real projectile pipeline");
        helper.succeed();
    }

    private static net.minecraft.server.level.ServerPlayer createVagrantPhasePlayer(GameTestHelper helper,
                                                                                   String name,
                                                                                   BlockPos localPos) {
        net.minecraft.server.level.ServerPlayer player = new net.minecraft.server.level.ServerPlayer(
                helper.getLevel().getServer(),
                helper.getLevel(),
                new com.mojang.authlib.GameProfile(UUID.randomUUID(), name)
        );
        player.moveTo(helper.absolutePos(localPos), 0.0F, 0.0F);
        SoulManager.setState(player, SoulState.VAGRANT);
        VagrantEvents.refreshAfterSoulStateChange(player);
        return player;
    }

    private static void chargeStonePhase(net.minecraft.server.level.ServerPlayer player, int ticks) {
        VagrantEvents.setVagrantInput(player, false, false, true);
        for (int i = 0; i < ticks; i++) {
            player.horizontalCollision = true;
            tickVagrantPlayer(player);
        }
    }

    private static void chargeLeafPhase(net.minecraft.server.level.ServerPlayer player, int ticks) {
        VagrantEvents.setVagrantInput(player, false, false, true);
        for (int i = 0; i < ticks; i++) {
            tickVagrantPlayer(player);
        }
    }

    private static void tickVagrantPlayer(net.minecraft.server.level.ServerPlayer player) {
        VagrantEvents.onPlayerTick(new net.minecraftforge.event.TickEvent.PlayerTickEvent(
                net.minecraftforge.event.TickEvent.Phase.START,
                player
        ));
        VagrantEvents.onPlayerTick(new net.minecraftforge.event.TickEvent.PlayerTickEvent(
                net.minecraftforge.event.TickEvent.Phase.END,
                player
        ));
    }

    private static RunestoneBlockEntity createPoweredRift(GameTestHelper helper, BlockPos absolutePos) {
        BlockState state = ModBlocks.RIFT_RUNE.get().defaultBlockState()
                .setValue(ua.meekeyn.requiemunofficial.common.block.InertRunestoneBlock.HEAD, true)
                .setValue(ua.meekeyn.requiemunofficial.common.block.InertRunestoneBlock.ACTIVATED, true);
        helper.getLevel().setBlock(absolutePos, state, 3);
        RunestoneBlockEntity runestone = (RunestoneBlockEntity) helper.getLevel().getBlockEntity(absolutePos);
        helper.assertTrue(runestone != null, "Rift rune should create a runestone block entity");
        runestone.setStructureData(
                1,
                1,
                true,
                java.util.Map.of("rift", 1),
                java.util.Map.of("rift", absolutePos)
        );
        runestone.setPowerRate(1.0F);
        return runestone;
    }

    @GameTest(template = "empty")
    public static void possessedAnimalUsesSameDisguiseAndRetaliationRules(GameTestHelper helper) {
        var player = new net.minecraft.server.level.ServerPlayer(helper.getLevel().getServer(), helper.getLevel(),
                new com.mojang.authlib.GameProfile(UUID.randomUUID(), "animal-disguise"));
        SoulManager.setState(player, SoulState.VAGRANT);
        var host = new net.minecraft.world.entity.animal.Cow(EntityType.COW, helper.getLevel());
        host.moveTo(helper.absolutePos(new BlockPos(1, 2, 1)), 0.0F, 0.0F);
        helper.getLevel().addFreshEntity(host);
        EntitySoulManager.removeSoul(host);
        helper.assertTrue(PossessionManager.tryPossess(player, host), "Cow possession should start");
        var zombie = new net.minecraft.world.entity.monster.Zombie(EntityType.ZOMBIE, helper.getLevel());
        helper.getLevel().addFreshEntity(zombie);
        zombie.setTarget(host);
        helper.assertTrue(zombie.getTarget() == null, "Possession alone must not provoke a monster against an animal");
        zombie.hurt(helper.getLevel().damageSources().mobAttack(host), 1.0F);
        helper.assertTrue(zombie.getTarget() == host, "Attacked monster must retaliate against an animal host");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void possessedPlayerDamageReachesHost(GameTestHelper helper) {
        var player = createPossessedSkeletonPlayer(helper, "damage-routing");
        var host = PossessionManager.findPossessedHost(player, SoulManager.getPossessedEntityId(player));
        float playerHealth = player.getHealth();
        float hostHealth = host.getHealth();
        helper.assertTrue(!player.canBeHitByProjectile(), "Hidden possessor must not intercept projectiles");
        helper.assertTrue(host.canBeHitByProjectile(), "Possessed body must remain hittable");
        helper.assertTrue(player.hurt(helper.getLevel().damageSources().fellOutOfWorld(), 3.0F),
                "A hit on the possessor must report successful damage to its host");
        helper.assertTrue(host.getHealth() == hostHealth - 3.0F, "Host must receive damage exactly once");
        helper.assertTrue(player.getHealth() == playerHealth, "Soul health must remain unchanged");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void incomingArrowDamagesPossessedSkeleton(GameTestHelper helper) {
        var player = createPossessedSkeletonPlayer(helper, "incoming-arrow");
        var host = PossessionManager.findPossessedHost(player, SoulManager.getPossessedEntityId(player));
        float health = host.getHealth();
        var arrow = new net.minecraft.world.entity.projectile.Arrow(EntityType.ARROW, helper.getLevel());
        arrow.setPos(host.getX() - 2.0D, host.getY() + 0.8D, host.getZ());
        arrow.setDeltaMovement(4.0D, 0.0D, 0.0D);
        helper.getLevel().addFreshEntity(arrow);
        arrow.tick();
        helper.assertTrue(host.getHealth() < health, "Incoming arrow must damage possessed host");
        helper.assertTrue(arrow.isRemoved(), "Successful arrow impact must not bounce off the body");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void playerOwnedArrowHitProvokesHostRetaliation(GameTestHelper helper) {
        var player = createPossessedSkeletonPlayer(helper, "owned-arrow");
        var host = PossessionManager.findPossessedHost(player, SoulManager.getPossessedEntityId(player));
        var victim = new net.minecraft.world.entity.monster.Zombie(EntityType.ZOMBIE, helper.getLevel());
        victim.moveTo(host.getX() + 4.0D, host.getY(), host.getZ(), 0.0F, 0.0F);
        helper.getLevel().addFreshEntity(victim);
        var arrow = new net.minecraft.world.entity.projectile.Arrow(helper.getLevel(), player);
        arrow.setPos(victim.getX() - 2.0D, victim.getY() + 0.8D, victim.getZ());
        arrow.setDeltaMovement(3.0D, 0.0D, 0.0D);
        helper.getLevel().addFreshEntity(arrow);
        helper.assertTrue(arrow.getOwner() == host, "Bow projectile owner must be the host");
        float health = victim.getHealth();
        arrow.tick();
        helper.assertTrue(victim.getHealth() < health, "Real arrow collision must damage victim");
        helper.assertTrue(victim.getTarget() == host, "Arrow victim must retaliate against host");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void blazeFireballCollisionProvokesRetaliation(GameTestHelper helper) {
        var player = createPossessedBlazePlayer(helper, "fireball-retaliation");
        var host = PossessionManager.findPossessedHost(player, SoulManager.getPossessedEntityId(player));
        var victim = new net.minecraft.world.entity.monster.Zombie(EntityType.ZOMBIE, helper.getLevel());
        victim.moveTo(host.getX() + 4.0D, host.getY(), host.getZ(), 0.0F, 0.0F);
        helper.getLevel().addFreshEntity(victim);
        var fireball = new SmallFireball(helper.getLevel(), host, 1.0D, 0.0D, 0.0D);
        fireball.setPos(victim.getX() - 2.0D, victim.getY() + 0.8D, victim.getZ());
        fireball.setDeltaMovement(3.0D, 0.0D, 0.0D);
        helper.getLevel().addFreshEntity(fireball);
        float health = victim.getHealth();
        fireball.tick();
        helper.assertTrue(victim.getHealth() < health, "Real fireball collision must damage victim");
        helper.assertTrue(victim.getRemainingFireTicks() > 0, "Fireball must ignite victim");
        helper.assertTrue(victim.getTarget() == host, "Fireball victim must retaliate against Blaze");
        helper.succeed();
    }

    private static net.minecraft.server.level.ServerPlayer createPossessedSkeletonPlayer(GameTestHelper helper,
                                                                                         String name) {
        net.minecraft.server.level.ServerPlayer player = new net.minecraft.server.level.ServerPlayer(
                helper.getLevel().getServer(),
                helper.getLevel(),
                new com.mojang.authlib.GameProfile(UUID.randomUUID(), name)
        );
        player.moveTo(helper.absolutePos(new BlockPos(1, 2, 1)), 0.0F, 0.0F);
        player.getInventory().armor.set(3, new ItemStack(Items.IRON_HELMET));
        SoulManager.setState(player, SoulState.VAGRANT);

        net.minecraft.world.entity.monster.Skeleton skeleton =
                new net.minecraft.world.entity.monster.Skeleton(EntityType.SKELETON, helper.getLevel());
        skeleton.moveTo(helper.absolutePos(new BlockPos(2, 2, 1)), 0.0F, 0.0F);
        helper.getLevel().addFreshEntity(skeleton);
        EntitySoulManager.removeSoul(skeleton);

        helper.assertTrue(PossessionManager.tryPossess(player, skeleton),
                "Skeleton possession should start");
        return player;
    }

    private static net.minecraft.server.level.ServerPlayer createPossessedBlazePlayer(GameTestHelper helper,
                                                                                      String name) {
        net.minecraft.server.level.ServerPlayer player = new net.minecraft.server.level.ServerPlayer(
                helper.getLevel().getServer(),
                helper.getLevel(),
                new com.mojang.authlib.GameProfile(UUID.randomUUID(), name)
        );
        player.moveTo(helper.absolutePos(new BlockPos(1, 4, 1)), 0.0F, 0.0F);
        SoulManager.setState(player, SoulState.VAGRANT);

        net.minecraft.world.entity.monster.Blaze blaze =
                new net.minecraft.world.entity.monster.Blaze(EntityType.BLAZE, helper.getLevel());
        blaze.moveTo(helper.absolutePos(new BlockPos(1, 4, 1)), 0.0F, 0.0F);
        helper.getLevel().addFreshEntity(blaze);
        EntitySoulManager.removeSoul(blaze);

        helper.assertTrue(PossessionManager.tryPossess(player, blaze),
                "Blaze possession should start");
        return player;
    }

    private static void addEffectForFakePlayer(
            net.minecraft.server.level.ServerPlayer player,
            net.minecraft.world.effect.MobEffectInstance effect
    ) {
        player.getActiveEffectsMap().put(effect.getEffect(), effect);
    }
}
