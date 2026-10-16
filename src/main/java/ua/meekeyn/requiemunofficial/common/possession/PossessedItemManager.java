package ua.meekeyn.requiemunofficial.common.possession;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.ResourceManagerReloadListener;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.TagKey;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.Potion;
import net.minecraft.world.item.alchemy.PotionUtils;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.Level;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.network.chat.Component;
import net.minecraftforge.event.AddReloadListenerEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import ua.meekeyn.requiemunofficial.RequiemUnofficial;
import ua.meekeyn.requiemunofficial.common.soul.SoulManager;
import ua.meekeyn.requiemunofficial.common.soul.SoulState;
import ua.meekeyn.requiemunofficial.common.init.ModEnchantments;
import ua.meekeyn.requiemunofficial.common.init.ModGameRules;
import ua.meekeyn.requiemunofficial.common.advancement.ModCriteria;
import ua.meekeyn.requiemunofficial.common.effect.PenanceMobEffect;
import ua.meekeyn.requiemunofficial.common.util.ModTags;
import ua.meekeyn.requiemunofficial.common.init.ModEffects;

import java.io.BufferedReader;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Mod.EventBusSubscriber(modid = RequiemUnofficial.MODID)
public final class PossessedItemManager implements ResourceManagerReloadListener {

    private static final String DIRECTORY = "requiem/mob_items/healing";
    private static final String DIET_DIRECTORY = "requiem/mob_items/diets";
    private static final String CURE_DIRECTORY = "requiem/mob_items/cure";
    private static final String REPLACED_BONES = "RequiemReplacedBones";
    private static volatile List<HealingRule> healingRules = List.of();
    private static volatile List<DietRule> dietRules = List.of();
    private static volatile List<CureRule> cureRules = List.of();
    private static volatile List<OldTooltipRule> oldTooltipRules = List.of();
    private static final Map<UUID, PendingBodyCure> PENDING_BODY_CURES = new ConcurrentHashMap<>();

    private PossessedItemManager() {
    }

    @SubscribeEvent
    public static void registerReloadListener(AddReloadListenerEvent event) {
        event.addListener(new PossessedItemManager());
    }

    @Override
    public void onResourceManagerReload(ResourceManager resourceManager) {
        List<HealingRule> loaded = new ArrayList<>();
        resourceManager.listResources(DIRECTORY, id -> id.getPath().endsWith(".json"))
                .forEach((id, resource) -> {
                    try (BufferedReader reader = resource.openAsReader()) {
                        HealingRule rule = parse(JsonParser.parseReader(reader).getAsJsonObject());
                        if (rule != null) {
                            loaded.add(rule);
                        }
                    } catch (Exception exception) {
                        RequiemUnofficial.LOGGER.error("Failed to load possessed item rule {}", id, exception);
                    }
                });
        loaded.sort(Comparator.comparingInt(HealingRule::priority).reversed());
        healingRules = List.copyOf(loaded);
        RequiemUnofficial.LOGGER.info("Loaded {} possessed healing rules", healingRules.size());

        List<DietRule> loadedDiets = new ArrayList<>();
        resourceManager.listResources(DIET_DIRECTORY, id -> id.getPath().endsWith(".json"))
                .forEach((id, resource) -> {
                    try (BufferedReader reader = resource.openAsReader()) {
                        DietRule rule = parseDiet(JsonParser.parseReader(reader).getAsJsonObject());
                        if (rule != null) {
                            loadedDiets.add(rule);
                        }
                    } catch (Exception exception) {
                        RequiemUnofficial.LOGGER.error("Failed to load possessed diet rule {}", id, exception);
                    }
                });
        loadedDiets.sort(Comparator.comparingInt(DietRule::priority).reversed());
        dietRules = List.copyOf(loadedDiets);
        RequiemUnofficial.LOGGER.info("Loaded {} possessed diet rules", dietRules.size());

        List<CureRule> loadedCures = new ArrayList<>();
        List<OldTooltipRule> loadedOldTooltips = new ArrayList<>();
        resourceManager.listResources(CURE_DIRECTORY, id -> id.getPath().endsWith(".json"))
                .forEach((id, resource) -> {
                    try (BufferedReader reader = resource.openAsReader()) {
                        JsonObject root = JsonParser.parseReader(reader).getAsJsonObject();
                        CureRule rule = parseCure(root);
                        if (rule != null) {
                            loadedCures.add(rule);
                        }
                        OldTooltipRule oldTooltipRule = parseOldTooltipRule(root);
                        if (oldTooltipRule != null) {
                            loadedOldTooltips.add(oldTooltipRule);
                        }
                    } catch (Exception exception) {
                        RequiemUnofficial.LOGGER.error("Failed to load possessed cure rule {}", id, exception);
                    }
                });
        cureRules = List.copyOf(loadedCures);
        loadedOldTooltips.sort(Comparator.comparingInt(OldTooltipRule::priority).reversed());
        oldTooltipRules = List.copyOf(loadedOldTooltips);
        RequiemUnofficial.LOGGER.info("Loaded {} possessed cure rules and {} legacy tooltip rules",
                cureRules.size(), oldTooltipRules.size());
    }

    @SubscribeEvent
    public static void onUseItem(PlayerInteractEvent.RightClickItem event) {
        if (event.getLevel().isClientSide()
                || !(event.getEntity() instanceof ServerPlayer player)
                || SoulManager.getPossessedEntityId(player) == null) {
            return;
        }

        LivingEntity host = findPossessedHost(player);
        if (host == null) {
            return;
        }

        if (tryUsePossessedItem(player, host, event.getItemStack(), event.getHand())) {
            event.setCanceled(true);
            event.setCancellationResult(InteractionResult.CONSUME);
        }
    }

    @SubscribeEvent
    public static void onUseItemOnPossessedEntity(PlayerInteractEvent.EntityInteract event) {
        if (event.getLevel().isClientSide()
                || !(event.getTarget() instanceof LivingEntity host)
                || PossessionManager.getPossessorId(host) == null) {
            return;
        }

        if (tryUsePossessedCureOnEntity(event.getEntity(), host, event.getItemStack(), event.getHand())) {
            event.setCanceled(true);
            event.setCancellationResult(InteractionResult.CONSUME);
        }
    }

    public static boolean tryUsePossessedItemForTest(ServerPlayer player, LivingEntity host, ItemStack stack) {
        return tryUsePossessedItem(player, host, stack, InteractionHand.MAIN_HAND);
    }

    public static boolean tryUsePossessedItemForTest(ServerPlayer player,
                                                     LivingEntity host,
                                                     ItemStack stack,
                                                     InteractionHand hand) {
        return tryUsePossessedItem(player, host, stack, hand);
    }

    public static boolean tryUsePossessedCureOnEntityForTest(Player user,
                                                             LivingEntity host,
                                                             ItemStack stack,
                                                             InteractionHand hand) {
        return tryUsePossessedCureOnEntity(user, host, stack, hand);
    }

    public static List<Component> buildTooltips(Player player, LivingEntity host, ItemStack stack) {
        List<TooltipMatch> matches = new ArrayList<>();
        for (HealingRule rule : healingRules) {
            TooltipMatch match = rule.tooltipMatch(host, stack);
            if (match != null) {
                matches.add(match);
            }
        }
        for (DietRule rule : dietRules) {
            TooltipMatch match = rule.tooltipMatch(host, stack);
            if (match != null) {
                matches.add(match);
            }
        }
        for (CureRule rule : cureRules) {
            TooltipMatch match = rule.tooltipMatch(host, stack, player);
            if (match != null) {
                matches.add(match);
            }
        }
        for (OldTooltipRule rule : oldTooltipRules) {
            TooltipMatch match = rule.tooltipMatch(host, stack);
            if (match != null) {
                matches.add(match);
            }
        }

        matches.sort(Comparator.comparingInt(TooltipMatch::priority).reversed());
        List<Component> lines = new ArrayList<>();
        for (TooltipMatch match : matches) {
            if (match.shortCircuits()) {
                return List.of(match.line());
            }
            lines.add(match.line());
        }
        return List.copyOf(lines);
    }

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END
                || !(event.player instanceof ServerPlayer player)
                || player.level().isClientSide()) {
            return;
        }

        PendingBodyCure pending = PENDING_BODY_CURES.get(player.getUUID());
        if (pending == null) {
            return;
        }

        LivingEntity host = findPossessedHost(player);
        if (host == null
                || !pending.hostId().equals(host.getUUID())
                || SoulManager.getPossessedEntityId(player) == null) {
            PENDING_BODY_CURES.remove(player.getUUID());
            return;
        }

        if (host.hasEffect(MobEffects.WEAKNESS)
                && player.level().getGameTime() < pending.completeGameTime()) {
            playCureProgress(host);
            return;
        }

        PENDING_BODY_CURES.remove(player.getUUID());
        PossessionManager.restorePlayerBodyFromHost(player, host);
        SoulManager.setState(player, SoulState.REMNANT);
        SoulManager.clearPossession(player);
        player.setHealth(player.getMaxHealth());
    }

    public static boolean hasPendingBodyCure(ServerPlayer possessor, LivingEntity host) {
        PendingBodyCure pending = PENDING_BODY_CURES.get(possessor.getUUID());
        return pending != null && pending.hostId().equals(host.getUUID());
    }

    private static boolean tryUsePossessedItem(ServerPlayer player,
                                               LivingEntity host,
                                               ItemStack stack,
                                               InteractionHand hand) {
        OverridablePossessedItemStack.clearUseDuration(stack);

        UseDecision cureDecision = getCureUseDecision(player, host, stack, player);
        if (cureDecision == UseDecision.START_LONG_USE) {
            OverridablePossessedItemStack.overrideUseDuration(stack, 48);
            player.startUsingItem(hand);
            return true;
        } else if (cureDecision == UseDecision.BLOCK) {
            return true;
        }

        for (HealingRule rule : healingRules) {
            if (!rule.matches(host, stack)) {
                continue;
            }

            if (player.getCooldowns().isOnCooldown(stack.getItem()) || host.getHealth() >= host.getMaxHealth()) {
                return true;
            }

            if (rule.useTime() <= 0) {
                return finishHealingUse(player, host, stack, hand, rule).isPresent();
            }

            OverridablePossessedItemStack.overrideUseDuration(stack, rule.useTime());
            player.startUsingItem(hand);
            return true;
        }

        DietDecision dietDecision = getDietDecision(host, stack);
        return dietDecision == DietDecision.BLOCK;
    }

    public static Optional<ItemStack> finishPossessedItemUse(Level level,
                                                            LivingEntity user,
                                                            ItemStack stack,
                                                            InteractionHand hand) {
        if (level.isClientSide()
                || !(user instanceof ServerPlayer player)
                || SoulManager.getPossessedEntityId(player) == null) {
            return Optional.empty();
        }

        LivingEntity host = findPossessedHost(player);
        if (host == null) {
            return Optional.empty();
        }

        if (isReadyToCure(player, host, stack, player)) {
            return finishSelfCure(player, host, stack, hand);
        }

        for (HealingRule rule : healingRules) {
            if (rule.matches(host, stack)) {
                return finishHealingUse(player, host, stack, hand, rule);
            }
        }

        DietRule dietRule = findMatchingDietRule(host, stack);
        if (dietRule != null && dietRule.removeHarmfulEffects()) {
            return Optional.of(finishWitchDietUse(player, host, stack, level));
        }

        return Optional.empty();
    }

    private static boolean tryUsePossessedCureOnEntity(Player user,
                                                       LivingEntity host,
                                                       ItemStack stack,
                                                       InteractionHand hand) {
        UUID possessorId = PossessionManager.getPossessorId(host);
        if (possessorId == null || user.getServer() == null) {
            return false;
        }

        ServerPlayer possessor = user.getServer().getPlayerList().getPlayer(possessorId);
        if (possessor == null || !host.getUUID().equals(SoulManager.getPossessedEntityId(possessor))) {
            return false;
        }

        return tryStartBodyCure(possessor, host, stack, hand, user);
    }

    private static UseDecision getCureUseDecision(ServerPlayer possessor,
                                                  LivingEntity host,
                                                  ItemStack stack,
                                                  Player itemUser) {
        boolean matchedReagent = false;
        for (CureRule rule : cureRules) {
            if (!rule.matches(host, stack)) {
                continue;
            }
            matchedReagent = true;
            if (possessor.level().getGameRules().getBoolean(ModGameRules.DISABLE_CURE)
                    || PenanceMobEffect.blocksCure(host.getEffect(ModEffects.PENANCE.get()))
                    || PenanceMobEffect.blocksCure(possessor.getEffect(ModEffects.PENANCE.get()))
                    || itemUser.getCooldowns().isOnCooldown(stack.getItem())) {
                return UseDecision.BLOCK;
            }
            return host.hasEffect(MobEffects.WEAKNESS) ? UseDecision.START_LONG_USE : UseDecision.BLOCK;
        }
        return matchedReagent ? UseDecision.BLOCK : UseDecision.PASS;
    }

    private static boolean isReadyToCure(ServerPlayer possessor,
                                         LivingEntity host,
                                         ItemStack stack,
                                         Player itemUser) {
        return getCureUseDecision(possessor, host, stack, itemUser) == UseDecision.START_LONG_USE;
    }

    private static Optional<ItemStack> finishSelfCure(ServerPlayer possessor,
                                                      LivingEntity host,
                                                      ItemStack stack,
                                                      InteractionHand hand) {
        if (tryStartBodyCure(possessor, host, stack, hand, possessor)) {
            return Optional.of(stack);
        }
        return Optional.of(stack);
    }

    private static Optional<ItemStack> finishHealingUse(ServerPlayer player,
                                                        LivingEntity host,
                                                        ItemStack stack,
                                                        InteractionHand hand,
                                                        HealingRule rule) {
        if (player.getCooldowns().isOnCooldown(stack.getItem()) || host.getHealth() >= host.getMaxHealth()) {
            return Optional.of(stack);
        }

        ItemStack result;
        if (rule.usage() == HealingUsage.REPLACE_BONE) {
            if (!replaceBone(player, host, stack)) {
                return Optional.of(stack);
            }
            result = stack;
        } else {
            FoodProperties food = stack.getFoodProperties(player);
            if (food == null) {
                return Optional.of(stack);
            }
            host.heal(food.getNutrition());
            result = stack.getItem().finishUsingItem(stack, player.level(), player);
        }

        addCooldown(player, stack.getItem(), rule.cooldown());
        host.swing(hand, true);
        return Optional.of(result);
    }

    private static boolean replaceBone(ServerPlayer player, LivingEntity host, ItemStack stack) {
        if (!host.isAlive() || host.getHealth() >= host.getMaxHealth()) {
            return false;
        }

        if (!player.getAbilities().instabuild) {
            stack.shrink(1);
        }
        CompoundTag data = host.getPersistentData();
        data.putInt(REPLACED_BONES, data.getInt(REPLACED_BONES) + 1);
        host.heal(4.0F);
        if (host instanceof Mob mob) {
            mob.playAmbientSound();
        }

        if (host.getType().is(ModTags.REPLACEABLE_SKELETONS)
                && shouldReplaceSkeleton(host)
                && host.level() instanceof ServerLevel level
                && host instanceof Mob mob) {
            replaceSkeletonBody(player, level, mob);
        }
        return true;
    }

    private static boolean shouldReplaceSkeleton(LivingEntity host) {
        int replacedBones = host.getPersistentData().getInt(REPLACED_BONES);
        Difficulty difficulty = host.level().getDifficulty();
        return switch (difficulty) {
            case PEACEFUL -> false;
            case EASY -> replacedBones > 8;
            case NORMAL -> replacedBones > 4;
            case HARD -> replacedBones > 2;
        };
    }

    private static void replaceSkeletonBody(ServerPlayer player, ServerLevel level, Mob source) {
        Mob replacement = source.convertTo(EntityType.SKELETON, true);
        if (replacement == null) {
            return;
        }

        replacement.setHealth(Math.min(source.getHealth(), replacement.getMaxHealth()));
        if (source.getRandom().nextInt(5) == 0
                && replacement.getItemBySlot(EquipmentSlot.HEAD).isEmpty()
                && !replacement.getType().is(ModTags.ARMOR_BANNED)) {
            replacement.setItemSlot(EquipmentSlot.HEAD, new ItemStack(Items.WITHER_SKELETON_SKULL));
        }

        if (PossessionManager.getPossessorId(source) != null
                && PossessionManager.transferPossession(player, source, replacement)) {
            ModCriteria.TRANSFORMED_POSSESSED_ENTITY.trigger(player, source, replacement, false);
        }
    }

    private static ItemStack finishWitchDietUse(ServerPlayer player,
                                                LivingEntity host,
                                                ItemStack stack,
                                                Level level) {
        Map<MobEffect, MobEffectInstance> before = Map.copyOf(player.getActiveEffectsMap());
        ItemStack result = stack.getItem().finishUsingItem(stack, level, player);
        Map<MobEffect, MobEffectInstance> after = Map.copyOf(player.getActiveEffectsMap());
        revertNewHarmfulEffects(player, before, after);
        host.swing(player.getUsedItemHand(), true);
        return result;
    }

    private static void revertNewHarmfulEffects(Player player,
                                                Map<MobEffect, MobEffectInstance> before,
                                                Map<MobEffect, MobEffectInstance> after) {
        for (Map.Entry<MobEffect, MobEffectInstance> entry : after.entrySet()) {
            MobEffect effect = entry.getKey();
            if (effect.isBeneficial()) {
                continue;
            }

            MobEffectInstance previous = before.get(effect);
            MobEffectInstance current = entry.getValue();
            if (previous == current || previous != null && previous.equals(current)) {
                continue;
            }

            player.removeEffect(effect);
            if (previous != null) {
                player.addEffect(new MobEffectInstance(
                        effect,
                        Math.min(previous.getDuration(), current.getDuration()),
                        Math.min(previous.getAmplifier(), current.getAmplifier()),
                        previous.isAmbient(),
                        previous.isVisible(),
                        previous.showIcon()
                ));
            }
        }
    }

    private static boolean tryStartBodyCure(ServerPlayer possessor,
                                            LivingEntity host,
                                            ItemStack stack,
                                            InteractionHand hand) {
        return tryStartBodyCure(possessor, host, stack, hand, possessor);
    }

    private static boolean tryStartBodyCure(ServerPlayer possessor,
                                            LivingEntity host,
                                            ItemStack stack,
                                            InteractionHand hand,
                                            Player itemUser) {
        for (CureRule rule : cureRules) {
            if (!rule.matches(host, stack)
                    || possessor.level().getGameRules().getBoolean(ModGameRules.DISABLE_CURE)
                    || PenanceMobEffect.blocksCure(host.getEffect(ModEffects.PENANCE.get()))
                    || PenanceMobEffect.blocksCure(possessor.getEffect(ModEffects.PENANCE.get()))
                    || !host.hasEffect(MobEffects.WEAKNESS)
                    || itemUser.getCooldowns().isOnCooldown(stack.getItem())) {
                continue;
            }

            MobEffectInstance weakness = host.getEffect(MobEffects.WEAKNESS);
            addCooldown(itemUser, stack.getItem(), 40);
            if (!itemUser.getAbilities().instabuild) {
                stack.shrink(1);
            }
            long completeGameTime = possessor.level().getGameTime()
                    + Math.max(1, weakness == null ? 1 : weakness.getDuration());
            PENDING_BODY_CURES.put(possessor.getUUID(), new PendingBodyCure(host.getUUID(), completeGameTime));
            playCureStart(host);
            host.swing(hand, true);
            return true;
        }
        return false;
    }

    private static void addCooldown(Player player, Item item, int ticks) {
        if (ticks > 0) {
            player.getCooldowns().addCooldown(item, ticks);
        }
    }

    private static DietDecision getDietDecision(LivingEntity host, ItemStack stack) {
        boolean hasDietForHost = false;
        boolean isFood = stack.getFoodProperties(host) != null;
        for (DietRule rule : dietRules) {
            if (!rule.entity().matches(host.getType())) {
                continue;
            }
            hasDietForHost = true;
            if (rule.item().matches(stack)) {
                return DietDecision.ALLOW_VANILLA;
            }
        }
        return hasDietForHost && isFood ? DietDecision.BLOCK : DietDecision.PASS;
    }

    private static DietRule findMatchingDietRule(LivingEntity host, ItemStack stack) {
        for (DietRule rule : dietRules) {
            if (rule.matches(host, stack)) {
                return rule;
            }
        }
        return null;
    }

    private static void playCureStart(LivingEntity host) {
        host.level().broadcastEntityEvent(host, (byte) 16);
        host.level().playSound(
                null,
                host.blockPosition(),
                SoundEvents.ZOMBIE_VILLAGER_CURE,
                SoundSource.HOSTILE,
                1.0F,
                1.0F + (host.getRandom().nextFloat() - host.getRandom().nextFloat()) * 0.2F
        );
        playCureProgress(host);
    }

    private static void playCureProgress(LivingEntity host) {
        host.level().broadcastEntityEvent(host, (byte) 16);
        if (!(host.level() instanceof ServerLevel level) || host.tickCount % 5 != 0) {
            return;
        }
        level.sendParticles(
                ParticleTypes.HAPPY_VILLAGER,
                host.getRandomX(0.7D),
                host.getRandomY() + 0.25D,
                host.getRandomZ(0.7D),
                3,
                0.15D,
                0.15D,
                0.15D,
                0.0D
        );
    }

    private static LivingEntity findPossessedHost(ServerPlayer player) {
        UUID hostId = SoulManager.getPossessedEntityId(player);
        if (hostId == null || player.getServer() == null) {
            return null;
        }
        for (net.minecraft.server.level.ServerLevel level : player.getServer().getAllLevels()) {
            net.minecraft.world.entity.Entity entity = level.getEntity(hostId);
            if (entity instanceof LivingEntity living
                    && living.isAlive()
                    && hostId.equals(living.getUUID())) {
                return living;
            }
        }
        return null;
    }

    public static int getHumanityLevel(LivingEntity host) {
        return EnchantmentHelper.getEnchantmentLevel(ModEnchantments.HUMANITY.get(), host);
    }

    public static HealingRule parse(JsonObject root) {
        if (root.has("enabled") && !root.get("enabled").getAsBoolean()) {
            return null;
        }
        JsonObject mob = root.getAsJsonObject("mob");
        JsonObject override = root.getAsJsonObject("override");
        if (mob == null || override == null || !"requiemunofficial:healing".equals(string(override, "type"))) {
            return null;
        }

        JsonObject item = override.getAsJsonObject("item");
        if (item == null) {
            return null;
        }
        EntityMatcher entityMatcher = EntityMatcher.parse(string(mob, "type"));
        ItemMatcher itemMatcher = ItemMatcher.parse(item);
        if (entityMatcher == null || itemMatcher == null) {
            return null;
        }
        int priority = root.has("priority") ? root.get("priority").getAsInt() : 100;
        int useTime = override.has("use_time") ? Math.max(0, override.get("use_time").getAsInt()) : 0;
        int cooldown = override.has("cooldown") ? Math.max(0, override.get("cooldown").getAsInt()) : 0;
        HealingUsage usage = HealingUsage.parse(string(override, "usage"));
        if (usage == null) {
            return null;
        }
        return new HealingRule(priority, tooltip(root), entityMatcher, itemMatcher, useTime, cooldown, usage);
    }

    public static DietRule parseDiet(JsonObject root) {
        if (root.has("enabled") && !root.get("enabled").getAsBoolean()) {
            return null;
        }
        JsonObject mob = root.getAsJsonObject("mob");
        JsonObject override = root.getAsJsonObject("override");
        if (mob == null || override == null || !"requiemunofficial:diet".equals(string(override, "type"))) {
            return null;
        }
        JsonObject food = override.getAsJsonObject("food");
        EntityMatcher entityMatcher = EntityMatcher.parse(string(mob, "type"));
        ItemMatcher itemMatcher = food == null ? null : ItemMatcher.parse(food);
        if (entityMatcher == null || itemMatcher == null) {
            return null;
        }
        int priority = root.has("priority") ? root.get("priority").getAsInt() : 100;
        return new DietRule(
                priority,
                tooltip(root),
                entityMatcher,
                itemMatcher,
                "remove_harmful_effects".equals(string(override, "filter"))
        );
    }

    public static CureRule parseCure(JsonObject root) {
        if (root.has("enabled") && !root.get("enabled").getAsBoolean()) {
            return null;
        }
        JsonObject override = root.getAsJsonObject("override");
        if (override == null || !"requiemunofficial:cure".equals(string(override, "type"))) {
            return null;
        }
        int priority = root.has("priority") ? root.get("priority").getAsInt() : 100;
        JsonObject reagent = override.getAsJsonObject("reagent");
        ItemMatcher itemMatcher = reagent == null ? null : ItemMatcher.parse(reagent);
        return itemMatcher == null ? null : new CureRule(priority, tooltip(root), itemMatcher);
    }

    private static OldTooltipRule parseOldTooltipRule(JsonObject root) {
        if (root.has("enabled") && !root.get("enabled").getAsBoolean()) {
            return null;
        }
        if (!root.has("schema_version") || root.get("schema_version").getAsInt() != 0) {
            return null;
        }
        Component tooltip = tooltip(root);
        JsonObject requirements = root.getAsJsonObject("requirements");
        JsonObject result = root.getAsJsonObject("result");
        if (tooltip == null || requirements == null || result == null
                || !"requiemunofficial:pass".equals(string(result, "action"))) {
            return null;
        }
        JsonObject usedItem = requirements.getAsJsonObject("used_item");
        Potion potion = usedItem == null ? null : potion(usedItem);
        if (potion == null) {
            return null;
        }
        int priority = root.has("priority") ? root.get("priority").getAsInt() : 100;
        return new OldTooltipRule(priority, tooltip, potion);
    }

    private static Component tooltip(JsonObject root) {
        JsonObject tooltip = root.getAsJsonObject("tooltip");
        if (tooltip == null || !tooltip.has("translate")) {
            return null;
        }
        Component component = Component.translatable(tooltip.get("translate").getAsString());
        if (tooltip.has("color") && "dark_gray".equals(tooltip.get("color").getAsString())) {
            component = component.copy().withStyle(ChatFormatting.DARK_GRAY);
        }
        return component;
    }

    private static Potion potion(JsonObject object) {
        if (!object.has("potion")) {
            return null;
        }
        ResourceLocation id = ResourceLocation.tryParse(object.get("potion").getAsString());
        return id == null ? null : BuiltInRegistries.POTION.getOptional(id).orElse(null);
    }

    private static String string(JsonObject object, String key) {
        return object.has(key) ? object.get(key).getAsString() : "";
    }

    public record HealingRule(int priority,
                              Component tooltip,
                              EntityMatcher entity,
                              ItemMatcher item,
                              int useTime,
                              int cooldown,
                              HealingUsage usage) {
        public boolean matches(LivingEntity host, ItemStack stack) {
            return entity.matches(host.getType()) && item.matches(stack);
        }

        TooltipMatch tooltipMatch(LivingEntity host, ItemStack stack) {
            if (tooltip == null || !matches(host, stack)) {
                return null;
            }
            return new TooltipMatch(priority, tooltip, host.getHealth() < host.getMaxHealth());
        }
    }

    public record DietRule(int priority, Component tooltip, EntityMatcher entity, ItemMatcher item, boolean removeHarmfulEffects) {
        public boolean matches(LivingEntity host, ItemStack stack) {
            return entity.matches(host.getType()) && item.matches(stack);
        }

        TooltipMatch tooltipMatch(LivingEntity host, ItemStack stack) {
            if (tooltip == null || !entity.matches(host.getType())) {
                return null;
            }
            if (item.matches(stack)) {
                return new TooltipMatch(priority, tooltip, true);
            }
            if (stack.getFoodProperties(host) != null) {
                return new TooltipMatch(priority, tooltip.copy().withStyle(ChatFormatting.STRIKETHROUGH), false);
            }
            return null;
        }
    }

    public record CureRule(int priority, Component tooltip, ItemMatcher reagent) {
        public boolean matches(LivingEntity host, ItemStack stack) {
            return (host.getType() == EntityType.ZOMBIE
                    || host.getType() == EntityType.ZOMBIE_VILLAGER
                    || host.getType() == EntityType.ZOMBIFIED_PIGLIN)
                    && reagent.matches(stack);
        }

        TooltipMatch tooltipMatch(LivingEntity host, ItemStack stack, Player player) {
            if (tooltip == null || !matches(host, stack)) {
                return null;
            }
            boolean ready = !player.level().getGameRules().getBoolean(ModGameRules.DISABLE_CURE)
                    && host.hasEffect(MobEffects.WEAKNESS)
                    && !player.getCooldowns().isOnCooldown(stack.getItem());
            return new TooltipMatch(priority, tooltip, ready);
        }
    }

    private record OldTooltipRule(int priority, Component tooltip, Potion potion) {
        TooltipMatch tooltipMatch(LivingEntity host, ItemStack stack) {
            if ((host.getType() == EntityType.ZOMBIE
                    || host.getType() == EntityType.ZOMBIE_VILLAGER
                    || host.getType() == EntityType.ZOMBIFIED_PIGLIN)
                    && PotionUtils.getPotion(stack) == potion) {
                return new TooltipMatch(priority, tooltip, true);
            }
            return null;
        }
    }

    private record TooltipMatch(int priority, Component line, boolean shortCircuits) {
    }

    private enum HealingUsage {
        EAT_TO_HEAL,
        REPLACE_BONE;

        static HealingUsage parse(String value) {
            return switch (value) {
                case "eat_to_heal" -> EAT_TO_HEAL;
                case "replace_bone" -> REPLACE_BONE;
                default -> null;
            };
        }
    }

    private enum UseDecision {
        PASS,
        BLOCK,
        START_LONG_USE
    }

    private enum DietDecision {
        PASS,
        BLOCK,
        ALLOW_VANILLA
    }

    private record PendingBodyCure(UUID hostId, long completeGameTime) {
    }

    public record EntityMatcher(ResourceLocation id, TagKey<EntityType<?>> tag) {
        static EntityMatcher parse(String value) {
            boolean isTag = value.startsWith("#");
            ResourceLocation id = ResourceLocation.tryParse(isTag ? value.substring(1) : value);
            if (id == null) {
                return null;
            }
            return new EntityMatcher(isTag ? null : id,
                    isTag ? TagKey.create(Registries.ENTITY_TYPE, id) : null);
        }

        boolean matches(EntityType<?> type) {
            return tag != null ? type.is(tag) : BuiltInRegistries.ENTITY_TYPE.getKey(type).equals(id);
        }
    }

    public record ItemMatcher(List<ResourceLocation> items, TagKey<Item> tag, boolean alwaysEdibleFood) {
        static ItemMatcher parse(JsonObject object) {
            if (object.has("tag")) {
                ResourceLocation id = ResourceLocation.tryParse(object.get("tag").getAsString());
                return id == null ? null : new ItemMatcher(List.of(), TagKey.create(Registries.ITEM, id), false);
            }
            for (Map.Entry<String, JsonElement> entry : object.entrySet()) {
                if (entry.getKey().endsWith(":food")
                        && entry.getValue().isJsonObject()
                        && entry.getValue().getAsJsonObject().has("always_edible")
                        && entry.getValue().getAsJsonObject().get("always_edible").getAsBoolean()) {
                    return new ItemMatcher(List.of(), null, true);
                }
            }
            JsonArray array = object.getAsJsonArray("items");
            if (array == null) {
                return null;
            }
            List<ResourceLocation> ids = new ArrayList<>();
            for (JsonElement element : array) {
                ResourceLocation id = ResourceLocation.tryParse(element.getAsString());
                if (id != null) {
                    ids.add(id);
                }
            }
            return ids.isEmpty() ? null : new ItemMatcher(List.copyOf(ids), null, false);
        }

        boolean matches(ItemStack stack) {
            if (alwaysEdibleFood) {
                FoodProperties food = stack.getFoodProperties(null);
                return food != null && food.canAlwaysEat();
            }
            return tag != null
                    ? stack.is(tag)
                    : items.contains(BuiltInRegistries.ITEM.getKey(stack.getItem()));
        }
    }
}
