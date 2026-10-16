package ua.meekeyn.requiemunofficial.common.entity;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.Tag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.TimeUtil;
import net.minecraft.util.valueproviders.UniformInt;
import net.minecraft.world.Difficulty;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.MoveTowardsRestrictionGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.npc.WanderingTrader;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.item.trading.MerchantOffers;
import net.minecraft.world.level.Level;
import net.minecraft.server.level.ServerLevel;
import ua.meekeyn.requiemunofficial.common.entity.ai.FreeFromMortalCoilGoal;
import ua.meekeyn.requiemunofficial.common.entity.ai.MorticianLookAtTargetGoal;
import ua.meekeyn.requiemunofficial.common.entity.ai.MoveBackToObeliskGoal;
import ua.meekeyn.requiemunofficial.common.entity.ai.StealSoulGoal;
import ua.meekeyn.requiemunofficial.common.init.ModEntities;
import ua.meekeyn.requiemunofficial.common.init.ModItems;
import ua.meekeyn.requiemunofficial.common.init.ModSounds;
import ua.meekeyn.requiemunofficial.common.item.HumanityEnchantedBookItem;
import ua.meekeyn.requiemunofficial.common.item.SoulVesselItem;
import ua.meekeyn.requiemunofficial.common.block.entity.RunestoneBlockEntity;
import ua.meekeyn.requiemunofficial.common.possession.PossessionManager;
import ua.meekeyn.requiemunofficial.common.record.BodyRecordData;
import ua.meekeyn.requiemunofficial.common.record.ObeliskRecordData;
import ua.meekeyn.requiemunofficial.common.soul.SoulManager;
import ua.meekeyn.requiemunofficial.common.soul.SoulKind;
import ua.meekeyn.requiemunofficial.common.soul.SoulState;
import ua.meekeyn.requiemunofficial.common.soul.VagrantRules;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;

public class MorticianEntity extends WanderingTrader {

    private static final int MAX_TRADE_USES = 12;
    public static final int MAX_LINK_DISTANCE = 20;
    public static final int DESPAWN_DELAY = 20 * 30;
    private static final UniformInt PERSISTENT_ANGER_TIME = TimeUtil.rangeOfSeconds(20, 39);
    private static final EntityDataAccessor<Boolean> OBELISK_PROJECTION = SynchedEntityData.defineId(
            MorticianEntity.class,
            EntityDataSerializers.BOOLEAN
    );
    private static final EntityDataAccessor<Integer> FADING_TICKS = SynchedEntityData.defineId(
            MorticianEntity.class,
            EntityDataSerializers.INT
    );
    private static final EntityDataAccessor<Byte> SPELL = SynchedEntityData.defineId(
            MorticianEntity.class,
            EntityDataSerializers.BYTE
    );
    private ResourceLocation homeDimension;
    private BlockPos homePos;
    private UUID linkedObeliskRecordId;
    private int remainingPersistentAngerTime;
    private UUID persistentAngerTarget;
    private final List<UUID> capturedSouls = new ArrayList<>();
    private boolean releasedCapturedSouls;

    public MorticianEntity(EntityType<? extends WanderingTrader> entityType, Level level) {
        super(entityType, level);
        setPersistenceRequired();
        setDespawnDelay(Integer.MAX_VALUE);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return AgeableMob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 24.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.24D)
                .add(Attributes.FOLLOW_RANGE, 32.0D);
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(1, new MorticianLookAtTargetGoal(this));
        goalSelector.addGoal(3, new StealSoulGoal(this));
        goalSelector.addGoal(4, new FreeFromMortalCoilGoal(this));
        goalSelector.addGoal(6, new MoveBackToObeliskGoal(this, 0.35D));
        goalSelector.addGoal(8, new RandomStrollGoal(this, 0.35D));
        goalSelector.addGoal(9, new LookAtPlayerGoal(this, Player.class, 8.0F));
        goalSelector.addGoal(10, new LookAtPlayerGoal(this, LivingEntity.class, 8.0F));
        targetSelector.addGoal(1, new HurtByTargetGoal(this));
        targetSelector.addGoal(3, new NearestAttackableTargetGoal<>(
                this,
                Player.class,
                10,
                true,
                false,
                living -> living instanceof Player player && shouldAngerAt(player)
        ));
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        entityData.define(OBELISK_PROJECTION, false);
        entityData.define(FADING_TICKS, 0);
        entityData.define(SPELL, (byte) 0);
    }

    public void setSpellcasting(boolean spellcasting) {
        entityData.set(SPELL, (byte) (spellcasting ? 1 : 0));
    }

    public boolean isSpellcasting() {
        return entityData.get(SPELL) != 0;
    }

    public void setFadingTicks(int fadingTicks) {
        entityData.set(FADING_TICKS, Math.max(0, Math.min(DESPAWN_DELAY, fadingTicks)));
    }

    public int getFadingTicks() {
        return entityData.get(FADING_TICKS);
    }

    public float getFadingAmount() {
        return getFadingTicks() / (float) DESPAWN_DELAY;
    }

    public void setObeliskProjection(boolean projection) {
        entityData.set(OBELISK_PROJECTION, projection);
    }

    public boolean isObeliskProjection() {
        return entityData.get(OBELISK_PROJECTION);
    }

    public void linkToObelisk(ServerLevel level, BlockPos pos) {
        this.homeDimension = level.dimension().location();
        this.homePos = pos.immutable();
        if (level.getBlockEntity(pos) instanceof RunestoneBlockEntity runestone) {
            ObeliskRecordData.ObeliskRecord record = runestone.getOrCreateObeliskRecord();
            this.linkedObeliskRecordId = record == null ? null : record.recordId();
        }
        setFadingTicks(0);
        setObeliskProjection(true);
    }

    @Nullable
    public BlockPos getHomePos() {
        return homePos;
    }

    public boolean isHomeDimension() {
        return homeDimension != null && homeDimension.equals(level().dimension().location());
    }

    public void addCapturedSoul(UUID recordId) {
        if (recordId != null && !capturedSouls.contains(recordId)) {
            capturedSouls.add(recordId);
        }
    }

    public int getCapturedSoulCount() {
        return capturedSouls.size();
    }

    public boolean hasCapturedSoul(UUID recordId) {
        return capturedSouls.contains(recordId);
    }

    public boolean releaseCapturedSoul(UUID recordId) {
        if (!(level() instanceof ServerLevel serverLevel) || recordId == null) {
            return false;
        }
        BodyRecordData data = BodyRecordData.get(serverLevel);
        var record = data.getRecord(recordId);
        if (record.isEmpty()) {
            capturedSouls.remove(recordId);
            return false;
        }
        if (record.get().activeReleasedSoul()) {
            capturedSouls.remove(recordId);
            return false;
        }

        data.markActiveReleasedSoul(recordId, true);
        ReleasedSoulEntity releasedSoul = new ReleasedSoulEntity(ModEntities.RELEASED_SOUL.get(), level());
        releasedSoul.setOwnerRecord(recordId);
        releasedSoul.setSoulData(record.get().entityType().toString(), 1);
        releasedSoul.moveTo(getX(), getY(0.8D), getZ(), getYRot(), getXRot());
        releasedSoul.setDeltaMovement(getLookAngle().normalize().scale(0.15D));
        level().addFreshEntity(releasedSoul);
        capturedSouls.remove(recordId);
        return true;
    }

    @Override
    public MerchantOffers getOffers() {
        MerchantOffers offers = super.getOffers();
        if (!isCanonicalMorticianOfferList(offers)) {
            populateMorticianOffers(offers);
        } else {
            removeDuplicateOffers(offers);
        }
        return offers;
    }

    @Override
    protected void updateTrades() {
        populateMorticianOffers(super.getOffers());
    }

    @Override
    protected void addOffersFromItemListings(MerchantOffers offers,
                                             net.minecraft.world.entity.npc.VillagerTrades.ItemListing[] trades,
                                             int maxNumbers) {
        if (!isCanonicalMorticianOfferList(offers)) {
            populateMorticianOffers(offers);
        }
    }

    private static void populateMorticianOffers(MerchantOffers offers) {
        offers.clear();

        offers.add(new MerchantOffer(
                new ItemStack(Items.GOLD_INGOT, 32),
                new ItemStack(Items.NETHERITE_INGOT),
                new ItemStack(ModItems.SEALED_REMNANT_VESSEL.get()),
                MAX_TRADE_USES,
                10,
                0.05F
        ));
        offers.add(new MerchantOffer(
                new ItemStack(ModItems.SHATTERED_SOUL_VESSEL.get()),
                new ItemStack(Items.GOLD_INGOT),
                new ItemStack(ModItems.EMPTY_SOUL_VESSEL.get()),
                MAX_TRADE_USES,
                2,
                0.05F
        ));
        offers.add(new MerchantOffer(
                HumanityEnchantedBookItem.create(1),
                new ItemStack(Items.GOLD_INGOT, 20),
                HumanityEnchantedBookItem.create(2),
                5,
                1,
                0.05F
        ));
        offers.add(soulTrade("minecraft:villager", SoulKind.MORTAL,
                ModItems.ICHOR_VESSEL_EMANCIPATION.get()));
        offers.add(soulTrade("minecraft:axolotl", SoulKind.BEAST,
                ModItems.ICHOR_VESSEL_RECLAMATION.get()));
        offers.add(soulTrade("minecraft:ghast", SoulKind.BEAST,
                ModItems.ICHOR_VESSEL_ATTRITION.get()));
        offers.add(soulTrade("minecraft:pillager", SoulKind.MORTAL,
                ModItems.ICHOR_VESSEL_PENANCE.get()));
        removeDuplicateOffers(offers);
    }

    private static boolean isCanonicalMorticianOfferList(MerchantOffers offers) {
        if (offers.size() != 7) {
            return false;
        }
        return offers.stream().anyMatch(offer -> offer.getResult().is(ModItems.SEALED_REMNANT_VESSEL.get()))
                && offers.stream().anyMatch(offer -> offer.getResult().is(ModItems.EMPTY_SOUL_VESSEL.get()))
                && offers.stream().filter(offer -> offer.getResult().is(ModItems.ICHOR_VESSEL_EMANCIPATION.get())).count() == 1
                && offers.stream().anyMatch(offer -> offer.getResult().is(ModItems.ICHOR_VESSEL_RECLAMATION.get()))
                && offers.stream().anyMatch(offer -> offer.getResult().is(ModItems.ICHOR_VESSEL_ATTRITION.get()))
                && offers.stream().anyMatch(offer -> offer.getResult().is(ModItems.ICHOR_VESSEL_PENANCE.get()));
    }

    private static MerchantOffer soulTrade(String soulType, SoulKind soulKind,
                                           net.minecraft.world.item.Item result) {
        ItemStack filledVessel = new ItemStack(ModItems.FILLED_SOUL_VESSEL.get());
        SoulVesselItem.setSoulData(filledVessel, soulType, soulKind, 1);
        return new MerchantOffer(
                filledVessel,
                new ItemStack(Items.GOLD_INGOT, 5),
                new ItemStack(result),
                MAX_TRADE_USES,
                5,
                0.05F
        );
    }

    @Override
    public void aiStep() {
        setDespawnDelay(Integer.MAX_VALUE);
        super.aiStep();
        if (!level().isClientSide) {
            tickAnger();
        }
        if (!level().isClientSide && isObeliskProjection()) {
            tickProjectionLink();
        }
    }

    private void tickProjectionLink() {
        if (homeDimension == null || homePos == null || !isValidLinkedObelisk()) {
            if (tryRelinkNearbyObelisk()) {
                return;
            }
            setFadingTicks(getFadingTicks() + 1);
            if (getFadingTicks() >= DESPAWN_DELAY) {
                discard();
            }
            return;
        }

        if (getFadingTicks() != 0) {
            setFadingTicks(0);
        }
    }

    private boolean tryRelinkNearbyObelisk() {
        if (!(level() instanceof ServerLevel serverLevel)) {
            return false;
        }
        BlockPos origin = blockPosition();
        BlockPos best = null;
        double bestDistance = Double.MAX_VALUE;
        var descriptorMatch = ObeliskRecordData.get(serverLevel)
                .nearestValidLoaded(serverLevel, origin, MAX_LINK_DISTANCE);
        if (descriptorMatch.isPresent()) {
            best = descriptorMatch.get().pos();
        } else {
            for (BlockPos candidate : BlockPos.betweenClosed(
                    origin.offset(-MAX_LINK_DISTANCE, -MAX_LINK_DISTANCE, -MAX_LINK_DISTANCE),
                    origin.offset(MAX_LINK_DISTANCE, MAX_LINK_DISTANCE, MAX_LINK_DISTANCE))) {
                if (!isValidObeliskAt(serverLevel, candidate)) {
                    continue;
                }
                double distance = candidate.distSqr(origin);
                if (distance < bestDistance) {
                    bestDistance = distance;
                    best = candidate.immutable();
                }
            }
        }
        if (best == null) {
            return false;
        }
        linkToObelisk(serverLevel, best);
        return true;
    }

    private boolean isValidLinkedObelisk() {
        MinecraftServer server = getServer();
        if (server == null || homeDimension == null || homePos == null) {
            return false;
        }
        if (linkedObeliskRecordId != null) {
            ObeliskRecordData data = ObeliskRecordData.get(server);
            if (!data.validateLoadedDestination(server, linkedObeliskRecordId)) {
                return false;
            }
            var record = data.getRecord(linkedObeliskRecordId);
            if (record.isEmpty()) {
                return false;
            }
            homeDimension = record.get().dimension();
            homePos = record.get().pos();
        }
        ServerLevel homeLevel = server.getLevel(net.minecraft.resources.ResourceKey.create(
                Registries.DIMENSION,
                homeDimension
        ));
        if (homeLevel == null || !homeLevel.isLoaded(homePos)) {
            return true;
        }
        if (!(homeLevel.getBlockEntity(homePos) instanceof RunestoneBlockEntity runestone)) {
            return false;
        }
        return runestone.isHeadBlock()
                && runestone.isActivatedBlock()
                && runestone.isStructureValid()
                && runestone.getRuneLevel("rift") > 0
                && runestone.isPowered();
    }

    private boolean isValidObeliskAt(ServerLevel level, BlockPos pos) {
        if (!(level.getBlockEntity(pos) instanceof RunestoneBlockEntity runestone)) {
            return false;
        }
        return runestone.isHeadBlock()
                && runestone.isActivatedBlock()
                && runestone.isStructureValid()
                && runestone.getRuneLevel("rift") > 0
                && runestone.isPowered();
    }

    private void tickAnger() {
        if (remainingPersistentAngerTime > 0) {
            --remainingPersistentAngerTime;
            if (persistentAngerTarget != null && getTarget() == null && level() instanceof ServerLevel serverLevel) {
                Player player = serverLevel.getServer().getPlayerList().getPlayer(persistentAngerTarget);
                if (player != null && shouldAngerAt(player)) {
                    setTarget(player);
                }
            }
        } else {
            persistentAngerTarget = null;
        }
    }

    private boolean shouldAngerAt(Player player) {
        return persistentAngerTarget != null
                && persistentAngerTarget.equals(player.getUUID())
                && level().getDifficulty() != Difficulty.PEACEFUL
                && !player.isCreative()
                && !player.isSpectator();
    }

    private void startPersistentAnger(Player player) {
        persistentAngerTarget = player.getUUID();
        remainingPersistentAngerTime = PERSISTENT_ANGER_TIME.sample(random);
    }

    @Override
    public void setTarget(@Nullable LivingEntity target) {
        if (target != null && level() instanceof ServerLevel serverLevel) {
            UUID possessorId = PossessionManager.getPossessorId(target);
            if (possessorId != null) {
                ServerPlayer possessor = serverLevel.getServer().getPlayerList().getPlayer(possessorId);
                if (possessor != null) {
                    super.setTarget(possessor);
                    return;
                }
            }
        }
        super.setTarget(target);
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        boolean result = super.hurt(source, amount);
        Entity attacker = source.getEntity();
        if (result && attacker instanceof Player player && !level().isClientSide) {
            startPersistentAnger(player);
        }
        return result;
    }

    @Override
    public void die(DamageSource damageSource) {
        super.die(damageSource);
        releaseCapturedSoulsOnDeath();
    }

    private void releaseCapturedSoulsOnDeath() {
        if (level().isClientSide || releasedCapturedSouls) {
            return;
        }
        releasedCapturedSouls = true;
        for (UUID capturedSoul : List.copyOf(capturedSouls)) {
            releaseCapturedSoul(capturedSoul);
        }
        capturedSouls.clear();
    }

    @Override
    public InteractionResult mobInteract(Player player, InteractionHand hand) {
        if (hand == InteractionHand.MAIN_HAND && isAlive() && !isBaby()) {
            prepareOffersFor(player);
            if (player instanceof ServerPlayer serverPlayer && serverPlayer.connection == null) {
                return InteractionResult.SUCCESS;
            }
        }
        return super.mobInteract(player, hand);
    }

    private void prepareOffersFor(Player player) {
        MerchantOffers offers = getOffers();
        if (offers.isEmpty()) {
            return;
        }
        MerchantOffer first = offers.get(0);
        if (SoulManager.getState(player) == SoulState.REMNANT) {
            first = new MerchantOffer(
                    new ItemStack(ModItems.EMPTY_SOUL_VESSEL.get()),
                    new ItemStack(Items.NETHERITE_INGOT),
                    new ItemStack(ModItems.SEALED_REMNANT_VESSEL.get()),
                    VagrantRules.isRestricted(player) ? 0 : 1,
                    1,
                    0.05F
            );
        } else if (VagrantRules.isRestricted(player)) {
            first = new MerchantOffer(
                    first.getBaseCostA(),
                    first.getCostB(),
                    first.getResult(),
                    0,
                    first.getXp(),
                    first.getPriceMultiplier()
            );
        }
        offers.set(0, first);
        removeDuplicateOffers(offers);
    }

    private static void removeDuplicateOffers(MerchantOffers offers) {
        java.util.Set<String> seen = new java.util.HashSet<>();
        for (int i = offers.size() - 1; i >= 0; i--) {
            MerchantOffer offer = offers.get(i);
            String key = offerKey(offer);
            if (!seen.add(key)) {
                offers.remove(i);
            }
        }
    }

    private static String offerKey(MerchantOffer offer) {
        return stackKey(offer.getBaseCostA())
                + "|" + stackKey(offer.getCostB())
                + "|" + stackKey(offer.getResult())
                + "|" + offer.getMaxUses()
                + "|" + offer.getXp()
                + "|" + offer.getPriceMultiplier();
    }

    private static String stackKey(ItemStack stack) {
        if (stack.isEmpty()) {
            return "empty";
        }
        return net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(stack.getItem())
                + "x" + stack.getCount()
                + (stack.hasTag() ? stack.getTag().toString() : "");
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putBoolean("obelisk_projection", isObeliskProjection());
        tag.putInt("fading_ticks", getFadingTicks());
        if (homeDimension != null) {
            tag.putString("linked_obelisk_dimension", homeDimension.toString());
        }
        if (linkedObeliskRecordId != null) {
            tag.putUUID("linked_obelisk_record", linkedObeliskRecordId);
        }
        if (homePos != null) {
            tag.putInt("linked_obelisk_x", homePos.getX());
            tag.putInt("linked_obelisk_y", homePos.getY());
            tag.putInt("linked_obelisk_z", homePos.getZ());
        }
        tag.putInt("AngerTime", remainingPersistentAngerTime);
        if (persistentAngerTarget != null) {
            tag.putUUID("AngryAt", persistentAngerTarget);
        }
        ListTag captured = new ListTag();
        for (UUID capturedSoul : capturedSouls) {
            captured.add(NbtUtils.createUUID(capturedSoul));
        }
        tag.put("captured_souls", captured);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        setObeliskProjection(tag.getBoolean("obelisk_projection") || tag.getBoolean("ObeliskProjection"));
        setFadingTicks(tag.contains("fading_ticks") ? tag.getInt("fading_ticks") : tag.getInt("FadingTicks"));
        if (tag.contains("linked_obelisk_dimension")) {
            homeDimension = ResourceLocation.tryParse(tag.getString("linked_obelisk_dimension"));
        }
        linkedObeliskRecordId = tag.hasUUID("linked_obelisk_record")
                ? tag.getUUID("linked_obelisk_record")
                : null;
        if (tag.contains("linked_obelisk_x")
                && tag.contains("linked_obelisk_y")
                && tag.contains("linked_obelisk_z")) {
            homePos = new BlockPos(
                    tag.getInt("linked_obelisk_x"),
                    tag.getInt("linked_obelisk_y"),
                    tag.getInt("linked_obelisk_z")
            );
        }
        remainingPersistentAngerTime = tag.getInt("AngerTime");
        persistentAngerTarget = tag.hasUUID("AngryAt") ? tag.getUUID("AngryAt") : null;
        capturedSouls.clear();
        if (tag.contains("captured_souls", Tag.TAG_LIST)) {
            ListTag captured = tag.getList("captured_souls", Tag.TAG_INT_ARRAY);
            for (Tag capturedSoul : captured) {
                capturedSouls.add(NbtUtils.loadUUID(capturedSoul));
            }
        }
    }

    @Override
    public boolean removeWhenFarAway(double distanceToClosestPlayer) {
        return false;
    }

    public SoundEvent getCastSpellSound() {
        return ModSounds.ENTITY_MORTICIAN_CAST_SPELL.get();
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return ModSounds.ENTITY_MORTICIAN_AMBIENT.get();
    }

    @Override
    protected SoundEvent getHurtSound(net.minecraft.world.damagesource.DamageSource damageSource) {
        return ModSounds.ENTITY_MORTICIAN_HURT.get();
    }

    @Override
    protected SoundEvent getDeathSound() {
        return ModSounds.ENTITY_MORTICIAN_DEATH.get();
    }

    @Override
    protected SoundEvent getTradeUpdatedSound(boolean isYesSound) {
        return isYesSound ? ModSounds.ENTITY_MORTICIAN_YES.get() : ModSounds.ENTITY_MORTICIAN_NO.get();
    }

    @Override
    public SoundEvent getNotifyTradeSound() {
        return ModSounds.ENTITY_MORTICIAN_TRADE.get();
    }

    @Override
    public MorticianEntity getBreedOffspring(ServerLevel level, AgeableMob otherParent) {
        return null;
    }
}
