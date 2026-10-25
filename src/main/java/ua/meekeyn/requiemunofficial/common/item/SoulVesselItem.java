package ua.meekeyn.requiemunofficial.common.item;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.stats.Stats;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.NeutralMob;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.registries.ForgeRegistries;
import ua.meekeyn.requiemunofficial.common.init.ModItems;
import ua.meekeyn.requiemunofficial.common.init.ModSounds;
import ua.meekeyn.requiemunofficial.common.config.RequiemConfig;
import ua.meekeyn.requiemunofficial.common.record.BodyRecordData;
import ua.meekeyn.requiemunofficial.common.soul.EntitySoulManager;
import ua.meekeyn.requiemunofficial.common.soul.SoulManager;
import ua.meekeyn.requiemunofficial.common.soul.SoulHarvestEvents;
import ua.meekeyn.requiemunofficial.common.soul.SoulKind;
import ua.meekeyn.requiemunofficial.common.possession.PossessionManager;
import ua.meekeyn.requiemunofficial.common.util.ModTags;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public class SoulVesselItem extends Item {

    private static final String TAG_ACTIVE_CAPTURE = "RequiemSoulCapture";
    private static final String TAG_CAPTURE_TARGET = "Target";
    private static final String TAG_CAPTURE_TIME = "UseTime";
    private static final String TAG_CAPTURE_CREATIVE_BOOST = "CreativeBoost";
    private static final String TAG_SOUL_TYPE = "SoulType";
    private static final String TAG_SOUL_KIND = "SoulKind";
    private static final String TAG_SOUL_AMOUNT = "SoulAmount";
    private static final String TAG_SOUL_ENERGY = "SoulEnergy";
    public static final String TAG_BODY_RECORD = "BodyRecord";
    public static final int CAPACITY = 1;
    public static final int SOUL_AGGREGATE_STEALING_DAMAGE = 20;
    public static final int POST_CAPTURE_COOLDOWN_TICKS = 100;
    public static final int CREATIVE_CAPTURE_TIME_TICKS = 10;

    public SoulVesselItem(Properties properties) {
        super(properties);
    }

    public static void setSoulData(ItemStack stack, String soulType, int soulAmount) {
        setSoulData(stack, soulType, SoulKind.BEAST, soulAmount);
    }

    public static void setSoulData(ItemStack stack, String soulType, SoulKind soulKind, int soulAmount) {
        int clampedAmount = Math.max(1, Math.min(CAPACITY, soulAmount));
        CompoundTag tag = stack.getOrCreateTag();
        tag.putString(TAG_SOUL_TYPE, soulType);
        tag.putString(TAG_SOUL_KIND, soulKind.id());
        tag.putInt(TAG_SOUL_AMOUNT, clampedAmount);
        tag.putInt(TAG_SOUL_ENERGY, soulKind.energyFor(clampedAmount));
    }

    public static String getSoulType(ItemStack stack) {
        CompoundTag tag = stack.getTag();
        if (tag == null) {
            return "empty";
        }
        return tag.getString(TAG_SOUL_TYPE);
    }

    public static int getSoulAmount(ItemStack stack) {
        CompoundTag tag = stack.getTag();
        if (tag == null) {
            return 0;
        }
        return Math.max(0, Math.min(CAPACITY, tag.getInt(TAG_SOUL_AMOUNT)));
    }

    public static String getSoulKind(ItemStack stack) {
        CompoundTag tag = stack.getTag();
        if (tag == null || !tag.contains(TAG_SOUL_KIND)) {
            return SoulKind.BEAST.id();
        }
        return tag.getString(TAG_SOUL_KIND);
    }

    public static int getSoulEnergy(ItemStack stack) {
        CompoundTag tag = stack.getTag();
        if (tag == null) {
            return 0;
        }
        if (tag.contains(TAG_SOUL_ENERGY)) {
            return Math.max(0, tag.getInt(TAG_SOUL_ENERGY));
        }
        return getSoulAmount(stack);
    }

    public static void setBodyRecord(ItemStack stack, UUID recordId) {
        stack.getOrCreateTag().putUUID(TAG_BODY_RECORD, recordId);
    }

    public static UUID getBodyRecord(ItemStack stack) {
        CompoundTag tag = stack.getTag();
        return tag != null && tag.hasUUID(TAG_BODY_RECORD) ? tag.getUUID(TAG_BODY_RECORD) : null;
    }

    public static boolean hasSoulData(ItemStack stack) {
        CompoundTag tag = stack.getTag();
        return tag != null && tag.contains(TAG_SOUL_TYPE) && tag.contains(TAG_SOUL_AMOUNT);
    }

    public static void clearSoulData(ItemStack stack) {
        CompoundTag tag = stack.getTag();
        if (tag == null) {
            return;
        }

        tag.remove(TAG_SOUL_TYPE);
        tag.remove(TAG_SOUL_KIND);
        tag.remove(TAG_SOUL_AMOUNT);
        tag.remove(TAG_SOUL_ENERGY);
        tag.remove(TAG_BODY_RECORD);

        if (tag.isEmpty()) {
            stack.setTag(null);
        }
    }

    @Override
    public InteractionResult interactLivingEntity(ItemStack stack, Player player, LivingEntity target,
                                                  InteractionHand hand) {
        return beginCapture(stack, player, target, hand, true, true);
    }

    public static InteractionResult beginCaptureForTest(ItemStack stack, Player player, LivingEntity target,
                                                        InteractionHand hand) {
        return beginCapture(stack, player, target, hand, false, false);
    }

    private static InteractionResult beginCapture(ItemStack stack, Player player, LivingEntity target,
                                                  InteractionHand hand, boolean startUsing, boolean playFeedback) {
        if (!isCaptureVessel(stack)) {
            return InteractionResult.PASS;
        }

        // Клієнт має поглинути взаємодію тією самою рукою, що й сервер.
        if (player.level().isClientSide) {
            return getCaptureType(target).isPresent() && target instanceof Mob
                    ? InteractionResult.SUCCESS : InteractionResult.PASS;
        }

        Optional<CaptureType> captureType = getCaptureType(target);
        if (captureType.isEmpty() || !(target instanceof Mob)) {
            return InteractionResult.PASS;
        }
        if (!EntitySoulManager.hasSoul(target)) {
            return InteractionResult.PASS;
        }

        CompoundTag active = stack.getOrCreateTagElement(TAG_ACTIVE_CAPTURE);
        active.putUUID(TAG_CAPTURE_TARGET, target.getUUID());
        active.putInt(TAG_CAPTURE_TIME, computeCaptureTime(stack, target, player, captureType.get(), hand));
        active.putBoolean(TAG_CAPTURE_CREATIVE_BOOST,
                isCreativeSoulVessel(stack) || hasCreativeOffhandAccelerator(player, hand));
        provokeNeutralTarget(player, target);
        if (startUsing
                && (!(player instanceof net.minecraft.server.level.ServerPlayer serverPlayer)
                || serverPlayer.connection != null && serverPlayer.connection.connection != null)) {
            player.startUsingItem(hand);
        }
        if (playFeedback) {
            player.level().playSound(
                    null,
                    target.blockPosition(),
                    SoundEvents.SOUL_ESCAPE,
                    SoundSource.PLAYERS,
                    0.7F,
                    0.65F
            );
        }
        return InteractionResult.CONSUME;
    }

    private static boolean isCaptureVessel(ItemStack stack) {
        return stack.is(ModItems.EMPTY_SOUL_VESSEL.get()) || stack.is(ModItems.CREATIVE_SOUL_VESSEL.get());
    }

    private static boolean isCreativeSoulVessel(ItemStack stack) {
        return stack.is(ModItems.CREATIVE_SOUL_VESSEL.get());
    }

    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity user) {
        if (!(level instanceof ServerLevel serverLevel) || !(user instanceof Player player)) {
            return stack;
        }

        CompoundTag active = stack.getTagElement(TAG_ACTIVE_CAPTURE);
        if (active == null || !active.hasUUID(TAG_CAPTURE_TARGET)) {
            return stack;
        }
        stack.removeTagKey(TAG_ACTIVE_CAPTURE);

        Entity entity = serverLevel.getEntity(active.getUUID(TAG_CAPTURE_TARGET));
        if (!(entity instanceof LivingEntity target)) {
            return stack;
        }

        Optional<CaptureType> captureType = getCaptureType(target);
        if (captureType.isEmpty() || !EntitySoulManager.hasSoul(target)) {
            return stack;
        }

        player.awardStat(Stats.ITEM_USED.get(this));
        if (!active.getBoolean(TAG_CAPTURE_CREATIVE_BOOST) && !wins(player, target, captureType.get())) {
            player.sendSystemMessage(Component.translatable(
                    "message.requiemunofficial.soul_vessel.capture_failed"
            ));
            return exchangeStack(player, stack, new ItemStack(ModItems.SHATTERED_SOUL_VESSEL.get()));
        }

        SoulKind soulKind = SoulKind.classify(target);
        int soulAmount = SoulHarvestEvents.calculateSoulAmount(target, soulKind);
        var entityTypeId = ForgeRegistries.ENTITY_TYPES.getKey(target.getType());
        String soulType = entityTypeId == null ? "minecraft:unknown" : entityTypeId.toString();
        ItemStack filled = new ItemStack(ModItems.FILLED_SOUL_VESSEL.get());
        setSoulData(filled, soulType, soulKind, soulAmount);
        spawnSoulCaptureEffects(player, target);

        if (captureType.get() == CaptureType.AGGREGATE) {
            target.hurt(level.damageSources().magic(), SOUL_AGGREGATE_STEALING_DAMAGE);
        } else {
            BodyRecordData.BodyRecord record = BodyRecordData.get(serverLevel).getOrCreateRecord(target);
            setBodyRecord(filled, record.recordId());
            EntitySoulManager.removeSoul(target);
        }
        if (!(player instanceof net.minecraft.server.level.ServerPlayer serverPlayer)
                || serverPlayer.connection != null) {
            player.getCooldowns().addCooldown(ModItems.FILLED_SOUL_VESSEL.get(), POST_CAPTURE_COOLDOWN_TICKS);
        }
        return exchangeStack(player, stack, filled);
    }

    @Override
    public void onUseTick(Level level, LivingEntity user, ItemStack stack, int remainingUseDuration) {
        if (!(level instanceof ServerLevel serverLevel)) {
            return;
        }
        CompoundTag active = stack.getTagElement(TAG_ACTIVE_CAPTURE);
        if (active == null || !active.hasUUID(TAG_CAPTURE_TARGET)) {
            return;
        }
        Entity target = serverLevel.getEntity(active.getUUID(TAG_CAPTURE_TARGET));
        if (target instanceof LivingEntity living && living.isAlive()) {
            playSoulCaptureEffects(user, living);
        }
    }

    @Override
    public int getUseDuration(ItemStack stack) {
        CompoundTag active = stack.getTagElement(TAG_ACTIVE_CAPTURE);
        return active == null ? 0 : Math.max(1, active.getInt(TAG_CAPTURE_TIME));
    }

    @Override
    public UseAnim getUseAnimation(ItemStack stack) {
        return UseAnim.BOW;
    }

    private static Optional<CaptureType> getCaptureType(LivingEntity target) {
        if (target.getType().is(ModTags.SOUL_AGGREGATES)) {
            return Optional.of(CaptureType.AGGREGATE);
        }
        if (!target.getType().is(ModTags.SOUL_CAPTURE_BLACKLIST)) {
            return Optional.of(CaptureType.NORMAL);
        }
        return Optional.empty();
    }

    public static boolean canCaptureSoul(LivingEntity target) {
        return getCaptureType(target).isPresent() && EntitySoulManager.hasSoul(target);
    }

    public static boolean tryStealSoul(LivingEntity user, LivingEntity target) {
        return tryStealSoulToRecord(user, target) != null;
    }

    @Nullable
    public static UUID tryStealSoulToRecord(LivingEntity user, LivingEntity target) {
        Optional<CaptureType> captureType = getCaptureType(target);
        if (captureType.isEmpty() || !EntitySoulManager.hasSoul(target)) {
            return null;
        }
        if (!wins(user, target, captureType.get())) {
            return null;
        }

        spawnSoulCaptureEffects(user, target);
        if (captureType.get() == CaptureType.AGGREGATE) {
            target.hurt(user.level().damageSources().magic(), SOUL_AGGREGATE_STEALING_DAMAGE);
            return UUID.randomUUID();
        } else {
            BodyRecordData.BodyRecord record = BodyRecordData.get(user.level()).getOrCreateRecord(target);
            EntitySoulManager.removeSoul(target);
            return record.recordId();
        }
    }

    private static int computeCaptureTime(ItemStack stack, LivingEntity target, Player user,
                                          CaptureType captureType, InteractionHand hand) {
        if (isCreativeSoulVessel(stack) || hasCreativeOffhandAccelerator(user, hand)) {
            return CREATIVE_CAPTURE_TIME_TICKS;
        }
        int targetSoulStrength = computeSoulDefense(target, captureType);
        int playerSoulStrength = Math.max(1, computeSoulOffense(user));
        float max = captureType == CaptureType.NORMAL ? 3.0F : 1.5F;
        float ratio = Math.max(1.0F, Math.min(max, (float) targetSoulStrength / playerSoulStrength));
        return Math.round(96.0F * ratio);
    }

    private static boolean hasCreativeOffhandAccelerator(Player user, InteractionHand hand) {
        return hand == InteractionHand.MAIN_HAND
                && user.getOffhandItem().is(ModItems.CREATIVE_SOUL_VESSEL.get());
    }

    private static void provokeNeutralTarget(Player player, LivingEntity target) {
        if (!(target instanceof Mob mob) || !(target instanceof NeutralMob neutral)
                || mob.getTarget() != null || neutral.getPersistentAngerTarget() != null) {
            return;
        }
        LivingEntity actor = player;
        if (player instanceof net.minecraft.server.level.ServerPlayer serverPlayer) {
            LivingEntity host = PossessionManager.findPossessedHost(serverPlayer);
            if (host != null && host.isAlive()) {
                actor = host;
                PossessionManager.markProvokedByPossessedHost(host, target);
            }
        }
        neutral.setPersistentAngerTarget(actor.getUUID());
        neutral.startPersistentAngerTimer();
        mob.setLastHurtByMob(actor);
        mob.setTarget(actor);
    }

    private static boolean wins(LivingEntity user, LivingEntity target, CaptureType captureType) {
        double configChance = RequiemConfig.SOUL_CAPTURE_CHANCE.get();
        if (configChance <= 0.0D) {
            return false;
        }

        int soulOffense = Math.max(1, computeSoulOffense(user));
        int soulDefense = Math.max(1, computeSoulDefense(target, captureType));
        if (soulOffense > soulDefense) {
            return user.getRandom().nextDouble() < configChance;
        }

        float strengthRatio = (float) soulOffense / soulDefense;
        return user.getRandom().nextDouble() < (strengthRatio * strengthRatio * configChance);
    }

    private static int computeSoulOffense(LivingEntity user) {
        int soulPower = user instanceof Player player ? SoulManager.getSoulPower(player) : 0;
        return (int) Math.round(user.getMaxHealth() + soulPower);
    }

    private static int computeSoulDefense(LivingEntity entity, CaptureType captureType) {
        double maxHealth = captureType == CaptureType.NORMAL ? entity.getMaxHealth() : 1.0D;
        double intrinsicArmor = getAttributeBaseValue(entity, Attributes.ARMOR);
        double intrinsicArmorToughness = getAttributeBaseValue(entity, Attributes.ARMOR_TOUGHNESS);
        double intrinsicStrength = getAttributeBaseValue(entity, Attributes.ATTACK_DAMAGE);
        double invertedHealthRatio = 1.0D - entity.getHealth() / entity.getMaxHealth();
        double woundedModifier = 1.0D - (invertedHealthRatio * invertedHealthRatio);
        double healthModifier = woundedModifier * 0.8D + 0.2D;
        double strengthModifier = computeStrengthModifier(intrinsicStrength);
        double physicalModifier = strengthModifier * healthModifier;
        return (int) Math.round(physicalModifier * (maxHealth + intrinsicArmor * 2.0D + intrinsicArmorToughness * 3.0D));
    }

    private static double computeStrengthModifier(double intrinsicStrength) {
        if (intrinsicStrength == 0.0D) {
            return 0.5D;
        }
        if (intrinsicStrength < 2.0D) {
            return 0.75D;
        }
        return Math.sqrt(intrinsicStrength * 0.5D);
    }

    private static double getAttributeBaseValue(LivingEntity entity, Attribute attribute) {
        var instance = entity.getAttribute(attribute);
        return instance == null ? 0.0D : instance.getBaseValue();
    }

    private static ItemStack exchangeStack(Player player, ItemStack stack, ItemStack result) {
        if (isCreativeSoulVessel(stack)) {
            if (!player.getInventory().add(result)) {
                player.drop(result, false);
            }
            return stack;
        }
        if (stack.getCount() == 1) {
            return result;
        }

        stack.shrink(1);
        if (!player.getInventory().add(result)) {
            player.drop(result, false);
        }
        return stack;
    }

    private static void spawnSoulCaptureEffects(LivingEntity user, LivingEntity target) {
        if (!(target.level() instanceof ServerLevel level)) {
            return;
        }

        Vec3 from = target.getEyePosition();
        Vec3 to = user.getEyePosition().subtract(0.0D, 0.15D, 0.0D);
        Vec3 delta = to.subtract(from);
        int steps = 12;

        level.sendParticles(
                ParticleTypes.SOUL,
                from.x,
                from.y,
                from.z,
                18,
                target.getBbWidth() * 0.35D,
                target.getBbHeight() * 0.25D,
                target.getBbWidth() * 0.35D,
                0.03D
        );
        for (int i = 0; i <= steps; i++) {
            double progress = i / (double) steps;
            Vec3 point = from.add(delta.scale(progress));
            level.sendParticles(
                    ParticleTypes.SOUL,
                    point.x,
                    point.y,
                    point.z,
                    2,
                    0.04D,
                    0.04D,
                    0.04D,
                    0.015D
            );
        }
        level.sendParticles(
                ParticleTypes.SOUL_FIRE_FLAME,
                to.x,
                to.y,
                to.z,
                8,
                0.12D,
                0.12D,
                0.12D,
                0.01D
        );
        level.playSound(
                null,
                target.blockPosition(),
                SoundEvents.SOUL_ESCAPE,
                SoundSource.PLAYERS,
                0.8F,
                0.9F + target.getRandom().nextFloat() * 0.2F
        );
    }

    public static void playSoulCaptureEffects(LivingEntity user, LivingEntity target) {
        if (!(user.level() instanceof ServerLevel level)) {
            return;
        }
        if (level.getRandom().nextFloat() >= 0.75F) {
            return;
        }

        Vec3 from = target.getEyePosition();
        Vec3 to = user.getEyePosition().subtract(0.0D, 0.15D, 0.0D);
        Vec3 delta = to.subtract(from);
        double progress = user.getRandom().nextDouble();
        Vec3 point = from.add(delta.scale(progress));
        level.sendParticles(
                ParticleTypes.SOUL,
                point.x,
                point.y,
                point.z,
                4 + level.random.nextInt(6),
                target.getBbWidth() * 0.12D,
                target.getBbHeight() * 0.12D,
                target.getBbWidth() * 0.12D,
                0.08D
        );
        level.sendParticles(
                ParticleTypes.SOUL_FIRE_FLAME,
                point.x,
                point.y,
                point.z,
                1,
                0.02D,
                0.02D,
                0.02D,
                0.01D
        );
        level.playSound(
                null,
                user.blockPosition(),
                ModSounds.ITEM_EMPTY_VESSEL_USE.get(),
                SoundSource.PLAYERS,
                1.0F,
                1.0F
        );
    }

    private enum CaptureType {
        NORMAL,
        AGGREGATE
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand usedHand) {
        ItemStack stack = player.getItemInHand(usedHand);

        if (!hasSoulData(stack)) {
            return InteractionResultHolder.pass(stack);
        }

        if (!level.isClientSide) {
            int soulAmount = getSoulAmount(stack);

            SoulManager.addSoul(player, soulAmount);

            ItemStack resultStack = new ItemStack(ModItems.SHATTERED_SOUL_VESSEL.get());

            if (stack.getCount() == 1) {
                return InteractionResultHolder.sidedSuccess(resultStack, false);
            }

            stack.shrink(1);

            if (!player.getInventory().add(resultStack)) {
                player.drop(resultStack, false);
            }
        }

        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }

    @Override
    public Component getName(ItemStack stack) {
        if (hasSoulData(stack)) {
            return Component.translatable(
                    "item.requiemunofficial.soul_vessel.named",
                    super.getName(stack),
                    getSoulType(stack),
                    getSoulAmount(stack)
            );
        }
        return super.getName(stack);
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level,
                                List<Component> tooltipComponents, TooltipFlag isAdvanced) {
        super.appendHoverText(stack, level, tooltipComponents, isAdvanced);

        if (hasSoulData(stack)) {
            tooltipComponents.add(Component.translatable(
                    "tooltip.requiemunofficial.soul_vessel.kind",
                    Component.translatable("soul_kind.requiemunofficial." + getSoulKind(stack))
            ));
            tooltipComponents.add(Component.translatable(
                    "tooltip.requiemunofficial.soul_vessel.type",
                    getSoulType(stack)
            ));
            tooltipComponents.add(Component.translatable(
                    "tooltip.requiemunofficial.soul_vessel.energy",
                    getSoulEnergy(stack)
            ));
        } else {
            tooltipComponents.add(Component.translatable(
                    "tooltip.requiemunofficial.soul_vessel.type",
                    Component.translatable("soul_type.requiemunofficial.empty")
            ));
        }
    }
}
