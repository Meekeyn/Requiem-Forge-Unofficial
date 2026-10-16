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
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Blocks;
import net.minecraftforge.event.AddReloadListenerEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.EventPriority;
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
import java.util.UUID;

@Mod.EventBusSubscriber(modid = RequiemUnofficial.MODID)
public final class DeathResurrectionManager implements ResourceManagerReloadListener {

    private static final String DIRECTORY = "requiem_resurrections";
    private static final String PENDING_BODY = "RequiemPendingResurrectionBody";
    private static final String HANDLED_DEATH_TICK = "RequiemResurrectionDeathTick";
    private static volatile List<DeathRule> rules = List.of();

    private DeathResurrectionManager() {
    }

    @SubscribeEvent
    public static void registerReloadListener(AddReloadListenerEvent event) {
        event.addListener(new DeathResurrectionManager());
    }

    @Override
    public void onResourceManagerReload(ResourceManager resourceManager) {
        List<DeathRule> loaded = new ArrayList<>();
        resourceManager.listResources(DIRECTORY, id -> id.getPath().endsWith(".json"))
                .forEach((id, resource) -> {
                    try (BufferedReader reader = resource.openAsReader()) {
                        DeathRule rule = parse(JsonParser.parseReader(reader).getAsJsonObject());
                        if (rule != null) {
                            loaded.add(rule);
                        }
                    } catch (Exception exception) {
                        RequiemUnofficial.LOGGER.error("Failed to load death resurrection rule {}", id, exception);
                    }
                });
        loaded.sort(Comparator.comparingInt(DeathRule::priority).reversed());
        rules = List.copyOf(loaded);
        RequiemUnofficial.LOGGER.info("Loaded {} death resurrection rules", rules.size());
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onPlayerDeath(LivingDeathEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)
                || SoulManager.getState(player) != SoulState.REMNANT
                || SoulManager.getResurrectionPos(player) != null) {
            return;
        }

        for (DeathRule rule : rules) {
            if (!rule.matches(player, event.getSource())) {
                continue;
            }
            Mob body = rule.result().create(player.serverLevel());
            if (body == null) {
                return;
            }
            body.moveTo(player.getX(), player.getY(), player.getZ(), player.getYRot(), player.getXRot());
            body.setPersistenceRequired();
            if (player.serverLevel().addFreshEntity(body)) {
                persisted(player).putUUID(PENDING_BODY, body.getUUID());
            }
            return;
        }
    }

    public static boolean tryHandleRemnantDeath(ServerPlayer player, DamageSource source) {
        if (player.level().isClientSide()
                || SoulManager.getState(player) != SoulState.REMNANT
                || SoulManager.getResurrectionPos(player) != null
                || alreadyHandledThisTick(player)) {
            return false;
        }

        DeathRule rule = matchingRule(player, source);
        if (rule == null) {
            return false;
        }

        Mob body = createBody(player, player, rule.result());
        if (body == null) {
            return false;
        }

        markHandledThisTick(player);
        SoulManager.setState(player, SoulState.VAGRANT);
        SoulManager.clearPlayerShell(player);
        player.stopUsingItem();
        player.fallDistance = 0.0F;
        player.setDeltaMovement(0.0D, 0.0D, 0.0D);
        player.setHealth(Math.max(1.0F, player.getMaxHealth()));
        if (hasLiveConnection(player)) {
            player.teleportTo(body.getX(), body.getY(), body.getZ());
        } else {
            player.moveTo(body.getX(), body.getY(), body.getZ(), player.getYRot(), player.getXRot());
        }
        if (PossessionManager.tryPossess(player, body)) {
            ModCriteria.PLAYER_RESURRECTED_AS_ENTITY.trigger(player, body);
            return true;
        }

        body.discard();
        return false;
    }

    public static boolean tryHandlePossessedHostDeath(ServerPlayer player, LivingEntity host, DamageSource source) {
        if (player.level().isClientSide()
                || SoulManager.getState(player) != SoulState.VAGRANT
                || alreadyHandledThisTick(player)) {
            return false;
        }
        return TotemResurrectionManager.tryConsumeAndApply(player, host, source);
    }

    private static DeathRule matchingRule(ServerPlayer player, DamageSource source) {
        for (DeathRule rule : rules) {
            if (rule.matches(player, source)) {
                return rule;
            }
        }
        return null;
    }

    static Mob createBody(ServerPlayer player, LivingEntity source, EntityType<? extends Mob> resultType) {
        Mob body = resultType.create(player.serverLevel());
        if (body == null) {
            return null;
        }
        body.moveTo(source.getX(), source.getY(), source.getZ(), source.getYRot(), source.getXRot());
        body.setPersistenceRequired();
        if (!player.serverLevel().addFreshEntity(body)) {
            return null;
        }
        return body;
    }

    private static boolean alreadyHandledThisTick(ServerPlayer player) {
        return persisted(player).getLong(HANDLED_DEATH_TICK) == player.serverLevel().getGameTime();
    }

    private static void markHandledThisTick(ServerPlayer player) {
        persisted(player).putLong(HANDLED_DEATH_TICK, player.serverLevel().getGameTime());
    }

    private static boolean hasLiveConnection(ServerPlayer player) {
        return player.connection != null && player.connection.connection != null;
    }

    @SubscribeEvent(priority = EventPriority.LOW)
    public static void onPlayerClone(PlayerEvent.Clone event) {
        if (!event.isWasDeath()) {
            return;
        }
        var oldData = persisted(event.getOriginal());
        if (oldData.hasUUID(PENDING_BODY)) {
            persisted(event.getEntity()).putUUID(PENDING_BODY, oldData.getUUID(PENDING_BODY));
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onPlayerRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        var data = persisted(player);
        if (!data.hasUUID(PENDING_BODY)) {
            return;
        }
        UUID bodyId = data.getUUID(PENDING_BODY);
        data.remove(PENDING_BODY);

        Entity entity = player.serverLevel().getEntity(bodyId);
        if (!(entity instanceof LivingEntity body) || !body.isAlive()) {
            return;
        }
        SoulManager.setState(player, SoulState.VAGRANT);
        player.teleportTo(body.getX(), body.getY(), body.getZ());
        if (PossessionManager.tryPossess(player, body)) {
            ModCriteria.PLAYER_RESURRECTED_AS_ENTITY.trigger(player, body);
        }
    }

    public static DeathRule parse(JsonObject root) {
        if (root.has("consumable") || root.has("possessed")) {
            return null;
        }
        JsonObject entity = root.getAsJsonObject("entity");
        ResourceLocation resultId = entity == null ? null : ResourceLocation.tryParse(string(entity, "type"));
        EntityType<?> rawType = resultId == null ? null : BuiltInRegistries.ENTITY_TYPE.get(resultId);
        if (!(rawType instanceof EntityType<?>)) {
            return null;
        }

        JsonObject killingBlow = root.getAsJsonObject("killing_blow");
        List<TagKey<DamageType>> damageTags = parseDamageTags(killingBlow);
        EntityMatcher directEntity = parseDirectEntity(killingBlow);
        JsonObject player = root.getAsJsonObject("player");
        ResourceLocation dimension = parseDimension(player);
        TagKey<Biome> biomeTag = parseBiomeTag(player);
        Integer maxLight = parseMaxLight(player);
        boolean headInSand = hasSpecialCondition(root, "head_in_sand");
        int priority = root.has("priority") ? root.get("priority").getAsInt() : 0;

        @SuppressWarnings("unchecked")
        EntityType<? extends Mob> result = (EntityType<? extends Mob>) rawType;
        return new DeathRule(priority, result, damageTags, directEntity, dimension, biomeTag, maxLight, headInSand);
    }

    private static List<TagKey<DamageType>> parseDamageTags(JsonObject killingBlow) {
        if (killingBlow == null || !killingBlow.has("tags")) {
            return List.of();
        }
        List<TagKey<DamageType>> tags = new ArrayList<>();
        for (JsonElement element : killingBlow.getAsJsonArray("tags")) {
            JsonObject tag = element.getAsJsonObject();
            if (tag.has("expected") && !tag.get("expected").getAsBoolean()) {
                continue;
            }
            ResourceLocation id = ResourceLocation.tryParse(string(tag, "id"));
            if (id != null) {
                tags.add(TagKey.create(Registries.DAMAGE_TYPE, id));
            }
        }
        return List.copyOf(tags);
    }

    private static EntityMatcher parseDirectEntity(JsonObject killingBlow) {
        if (killingBlow == null || !killingBlow.has("direct_entity")) {
            return null;
        }
        JsonObject direct = killingBlow.getAsJsonObject("direct_entity");
        if (direct.has("expected") && !direct.get("expected").getAsBoolean()) {
            return null;
        }
        return EntityMatcher.parse(string(direct, "type"));
    }

    private static ResourceLocation parseDimension(JsonObject player) {
        JsonObject location = player == null ? null : player.getAsJsonObject("location");
        return location != null && location.has("dimension")
                ? ResourceLocation.tryParse(location.get("dimension").getAsString())
                : null;
    }

    private static Integer parseMaxLight(JsonObject player) {
        JsonObject location = player == null ? null : player.getAsJsonObject("location");
        JsonObject light = location == null ? null : location.getAsJsonObject("light");
        JsonObject nested = light == null ? null : light.getAsJsonObject("light");
        return nested != null && nested.has("max") ? nested.get("max").getAsInt() : null;
    }

    private static TagKey<Biome> parseBiomeTag(JsonObject player) {
        JsonObject location = player == null ? null : player.getAsJsonObject("location");
        ResourceLocation id = location != null && location.has("requiemunofficial:biome_tag")
                ? ResourceLocation.tryParse(location.get("requiemunofficial:biome_tag").getAsString())
                : null;
        return id == null ? null : TagKey.create(Registries.BIOME, id);
    }

    private static boolean hasSpecialCondition(JsonObject root, String condition) {
        JsonArray array = root.getAsJsonArray("special_conditions");
        if (array == null) {
            return false;
        }
        for (JsonElement element : array) {
            if (condition.equals(element.getAsString())) {
                return true;
            }
        }
        return false;
    }

    private static String string(JsonObject object, String key) {
        return object.has(key) ? object.get(key).getAsString() : "";
    }

    private static net.minecraft.nbt.CompoundTag persisted(Player player) {
        net.minecraft.nbt.CompoundTag root = player.getPersistentData();
        if (!root.contains(Player.PERSISTED_NBT_TAG, net.minecraft.nbt.Tag.TAG_COMPOUND)) {
            root.put(Player.PERSISTED_NBT_TAG, new net.minecraft.nbt.CompoundTag());
        }
        return root.getCompound(Player.PERSISTED_NBT_TAG);
    }

    public record DeathRule(
            int priority,
            EntityType<? extends Mob> result,
            List<TagKey<DamageType>> damageTags,
            EntityMatcher directEntity,
            ResourceLocation dimension,
            TagKey<Biome> biomeTag,
            Integer maxLight,
            boolean headInSand
    ) {
        public boolean matches(ServerPlayer player, DamageSource source) {
            if (!damageTags.isEmpty() && damageTags.stream().noneMatch(source::is)) {
                return false;
            }
            if (directEntity != null && !directEntity.matches(source.getDirectEntity())) {
                return false;
            }
            if (dimension != null && !player.level().dimension().location().equals(dimension)) {
                return false;
            }
            if (biomeTag != null && !player.level().getBiome(player.blockPosition()).is(biomeTag)) {
                return false;
            }
            if (maxLight != null && player.level().getMaxLocalRawBrightness(player.blockPosition()) > maxLight) {
                return false;
            }
            return !headInSand || player.getEyeInFluidType() == net.minecraftforge.common.ForgeMod.EMPTY_TYPE.get()
                    && player.level().getBlockState(player.blockPosition().above()).is(Blocks.SAND);
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

        boolean matches(Entity entity) {
            if (entity == null) {
                return false;
            }
            return tag != null
                    ? entity.getType().is(tag)
                    : BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType()).equals(id);
        }
    }
}
