package ua.meekeyn.requiemunofficial.common.ritual;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.ResourceManagerReloadListener;
import net.minecraft.tags.TagKey;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.AddReloadListenerEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import ua.meekeyn.requiemunofficial.RequiemUnofficial;
import ua.meekeyn.requiemunofficial.common.possession.PossessionManager;
import ua.meekeyn.requiemunofficial.common.soul.SoulManager;
import ua.meekeyn.requiemunofficial.common.soul.SoulState;
import ua.meekeyn.requiemunofficial.common.advancement.ModCriteria;

import java.io.BufferedReader;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Mod.EventBusSubscriber(modid = RequiemUnofficial.MODID)
public final class TotemResurrectionManager implements ResourceManagerReloadListener {

    private static final String DIRECTORY = "requiem_resurrections";
    private static volatile List<TotemRule> rules = List.of();

    private TotemResurrectionManager() {
    }

    @SubscribeEvent
    public static void registerReloadListener(AddReloadListenerEvent event) {
        event.addListener(new TotemResurrectionManager());
    }

    @Override
    public void onResourceManagerReload(ResourceManager resourceManager) {
        List<TotemRule> loaded = new ArrayList<>();
        resourceManager.listResources(DIRECTORY, id -> id.getPath().endsWith(".json"))
                .forEach((id, resource) -> {
                    try (BufferedReader reader = resource.openAsReader()) {
                        TotemRule rule = parse(JsonParser.parseReader(reader).getAsJsonObject());
                        if (rule != null) {
                            loaded.add(rule);
                        }
                    } catch (Exception exception) {
                        RequiemUnofficial.LOGGER.error("Failed to load totem resurrection rule {}", id, exception);
                    }
                });
        loaded.sort(Comparator.comparingInt(TotemRule::priority).reversed());
        rules = List.copyOf(loaded);
        RequiemUnofficial.LOGGER.info("Loaded {} totem resurrection rules", rules.size());
    }

    @SubscribeEvent
    public static void onUseItem(PlayerInteractEvent.RightClickItem event) {
        if (event.getLevel().isClientSide()
                || event.getHand() != InteractionHand.MAIN_HAND
                || !(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }

        LivingEntity host = player.getVehicle() instanceof LivingEntity living
                && living.getUUID().equals(SoulManager.getPossessedEntityId(player))
                ? living
                : null;
        if (host == null
                && SoulManager.getState(player) != SoulState.VAGRANT
                && SoulManager.getState(player) != SoulState.REMNANT) {
            return;
        }

        ItemStack stack = event.getItemStack();
        for (TotemRule rule : rules) {
            if (!rule.matches(player, host, stack)) {
                continue;
            }
            if (apply(player, host, rule.result())) {
                if (!player.getAbilities().instabuild) {
                    stack.shrink(1);
                }
                event.setCanceled(true);
                event.setCancellationResult(InteractionResult.CONSUME);
            }
            return;
        }
    }

    private static boolean apply(ServerPlayer player, LivingEntity host, EntityType<? extends Mob> resultType) {
        Mob result = resultType.create(player.serverLevel());
        if (result == null) {
            return false;
        }

        LivingEntity source = host == null ? player : host;
        result.moveTo(source.getX(), source.getY(), source.getZ(), source.getYRot(), source.getXRot());
        result.setCustomName(source.getCustomName());
        result.setCustomNameVisible(source.isCustomNameVisible());
        result.setPersistenceRequired();
        if (!player.serverLevel().addFreshEntity(result)) {
            return false;
        }

        if (host != null) {
            PossessionManager.releaseForHostReplacement(player, host);
            host.discard();
        } else {
            SoulManager.setState(player, SoulState.VAGRANT);
            player.teleportTo(result.getX(), result.getY(), result.getZ());
        }

        if (!PossessionManager.tryPossess(player, result)) {
            result.discard();
            return false;
        }
        ModCriteria.PLAYER_RESURRECTED_AS_ENTITY.trigger(player, result);
        return true;
    }

    static boolean tryConsumeAndApply(ServerPlayer player, LivingEntity host, DamageSource source) {
        if (host == null || !player.isAlive()) {
            return false;
        }
        for (TotemRule rule : rules) {
            ItemStack stack = findConsumable(player, host, rule);
            if (stack.isEmpty() || !rule.matches(player, host, stack)) {
                continue;
            }
            if (!player.getAbilities().instabuild) {
                stack.shrink(1);
            }
            if (apply(player, host, rule.result())) {
                return true;
            }
            return false;
        }
        return false;
    }

    private static ItemStack findConsumable(ServerPlayer player, LivingEntity host, TotemRule rule) {
        for (InteractionHand hand : InteractionHand.values()) {
            ItemStack stack = host.getItemInHand(hand);
            if (rule.item().matches(stack)) {
                return stack;
            }
        }
        for (InteractionHand hand : InteractionHand.values()) {
            ItemStack stack = player.getItemInHand(hand);
            if (rule.item().matches(stack)) {
                return stack;
            }
        }
        return ItemStack.EMPTY;
    }

    public static TotemRule parse(JsonObject root) {
        JsonObject consumable = root.getAsJsonObject("consumable");
        JsonObject entity = root.getAsJsonObject("entity");
        if (consumable == null || entity == null) {
            return null;
        }

        ItemMatcher itemMatcher = ItemMatcher.parse(consumable);
        ResourceLocation resultId = ResourceLocation.tryParse(string(entity, "type"));
        EntityType<?> rawType = resultId == null ? null : BuiltInRegistries.ENTITY_TYPE.get(resultId);
        if (itemMatcher == null || !(rawType instanceof EntityType<?>)) {
            return null;
        }

        JsonObject possessed = root.getAsJsonObject("possessed");
        EntityMatcher possessedMatcher = possessed == null
                ? null
                : EntityMatcher.parse(string(possessed, "type"));
        MobEffect possessedEffect = parseEffect(possessed);
        MobEffect playerEffect = parseEffect(root.getAsJsonObject("player"));
        int priority = root.has("priority") ? root.get("priority").getAsInt() : 0;

        @SuppressWarnings("unchecked")
        EntityType<? extends Mob> result = (EntityType<? extends Mob>) rawType;
        return new TotemRule(
                priority,
                itemMatcher,
                result,
                possessedMatcher,
                possessedEffect,
                playerEffect
        );
    }

    private static MobEffect parseEffect(JsonObject owner) {
        JsonObject effects = owner == null ? null : owner.getAsJsonObject("effects");
        if (effects == null || effects.entrySet().isEmpty()) {
            return null;
        }
        ResourceLocation id = ResourceLocation.tryParse(effects.entrySet().iterator().next().getKey());
        return id == null ? null : BuiltInRegistries.MOB_EFFECT.get(id);
    }

    private static String string(JsonObject object, String key) {
        return object.has(key) ? object.get(key).getAsString() : "";
    }

    public record TotemRule(
            int priority,
            ItemMatcher item,
            EntityType<? extends Mob> result,
            EntityMatcher possessed,
            MobEffect possessedEffect,
            MobEffect playerEffect
    ) {
        public boolean matches(ServerPlayer player, LivingEntity host, ItemStack stack) {
            if (!item.matches(stack)) {
                return false;
            }
            if (possessed != null) {
                return host != null
                        && possessed.matches(host.getType())
                        && (possessedEffect == null || host.hasEffect(possessedEffect));
            }
            return host == null && (playerEffect == null || player.hasEffect(playerEffect));
        }
    }

    public record EntityMatcher(ResourceLocation id, TagKey<EntityType<?>> tag) {
        static EntityMatcher parse(String value) {
            boolean isTag = value.startsWith("#");
            ResourceLocation id = ResourceLocation.tryParse(isTag ? value.substring(1) : value);
            return id == null ? null : new EntityMatcher(
                    isTag ? null : id,
                    isTag ? TagKey.create(Registries.ENTITY_TYPE, id) : null
            );
        }

        boolean matches(EntityType<?> type) {
            return tag != null
                    ? type.is(tag)
                    : BuiltInRegistries.ENTITY_TYPE.getKey(type).equals(id);
        }
    }

    public record ItemMatcher(List<ResourceLocation> ids) {
        static ItemMatcher parse(JsonObject object) {
            JsonArray items = object.getAsJsonArray("items");
            if (items == null) {
                return null;
            }
            List<ResourceLocation> ids = new ArrayList<>();
            for (JsonElement element : items) {
                ResourceLocation id = ResourceLocation.tryParse(element.getAsString());
                if (id != null) {
                    ids.add(id);
                }
            }
            return ids.isEmpty() ? null : new ItemMatcher(List.copyOf(ids));
        }

        boolean matches(ItemStack stack) {
            return ids.contains(BuiltInRegistries.ITEM.getKey(stack.getItem()));
        }
    }
}
