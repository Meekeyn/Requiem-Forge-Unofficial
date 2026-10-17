package ua.meekeyn.requiemunofficial.common.possession;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.ResourceManagerReloadListener;
import net.minecraftforge.event.AddReloadListenerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import ua.meekeyn.requiemunofficial.RequiemUnofficial;
import ua.meekeyn.requiemunofficial.common.util.ModIds;

import java.io.BufferedReader;
import java.util.HashMap;
import java.util.Map;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Function;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.FlyingMob;
import net.minecraft.world.entity.animal.FlyingAnimal;
import net.minecraft.world.entity.animal.WaterAnimal;

@Mod.EventBusSubscriber(modid = RequiemUnofficial.MODID)
public final class EntityMobilityManager implements ResourceManagerReloadListener {

    private static final ResourceLocation RESOURCE = ModIds.id("entity_mobility.json");
    private static volatile Map<ResourceLocation, MobilityProfile> profiles = Map.of();
    private static final List<Function<LivingEntity, MobilityProfile>> resolvers = new CopyOnWriteArrayList<>();

    private EntityMobilityManager() {
    }

    @SubscribeEvent
    public static void registerReloadListener(AddReloadListenerEvent event) {
        event.addListener(new EntityMobilityManager());
    }

    @Override
    public void onResourceManagerReload(ResourceManager resourceManager) {
        Map<ResourceLocation, MobilityProfile> loaded = new HashMap<>();
        resourceManager.getResource(RESOURCE).ifPresent(resource -> {
            try (BufferedReader reader = resource.openAsReader()) {
                loaded.putAll(parse(JsonParser.parseReader(reader).getAsJsonObject()));
            } catch (Exception exception) {
                RequiemUnofficial.LOGGER.error("Failed to load {}", RESOURCE, exception);
            }
        });
        profiles = Map.copyOf(loaded);
        RequiemUnofficial.LOGGER.info("Loaded {} possession mobility profiles", profiles.size());
    }

    public static MobilityProfile get(net.minecraft.world.entity.EntityType<?> type) {
        return profiles.getOrDefault(BuiltInRegistries.ENTITY_TYPE.getKey(type), MobilityProfile.DEFAULT);
    }

    /** Адаптер повертає null, якщо не підтримує цей вид; datapack має найвищий пріоритет. */
    public static void registerResolver(Function<LivingEntity, MobilityProfile> resolver) {
        resolvers.add(java.util.Objects.requireNonNull(resolver));
    }

    public static MobilityProfile get(LivingEntity entity) {
        MobilityProfile configured = profiles.get(BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType()));
        if (configured != null) {
            return configured;
        }
        for (Function<LivingEntity, MobilityProfile> resolver : resolvers) {
            MobilityProfile resolved = resolver.apply(entity);
            if (resolved != null) {
                return resolved;
            }
        }
        boolean flyingNavigation = entity instanceof net.minecraft.world.entity.Mob mob
                && mob.getNavigation() instanceof net.minecraft.world.entity.ai.navigation.FlyingPathNavigation;
        if (entity instanceof FlyingMob || entity instanceof FlyingAnimal || flyingNavigation) {
            return new MobilityProfile(entity instanceof FlyingMob ? MobilityMode.FORCED : MobilityMode.ENABLED,
                    MobilityMode.DISABLED, WalkMode.NORMAL, 0.08D, 0.35D,
                    0.0D, 0.0D, 0.0D, false, false, false);
        }
        if (entity instanceof WaterAnimal || entity instanceof net.minecraft.world.entity.Mob mob
                && mob.getNavigation() instanceof net.minecraft.world.entity.ai.navigation.WaterBoundPathNavigation) {
            return new MobilityProfile(MobilityMode.DISABLED, MobilityMode.FORCED, WalkMode.NORMAL,
                    0.08D, 0.35D, 0.0D, 0.0D, 0.0D, false, false, false);
        }
        // Невідомі реалізації зберігають власний travel та стандартні атрибути фізики.
        return MobilityProfile.DEFAULT;
    }

    public static Map<ResourceLocation, MobilityProfile> parse(JsonObject root) {
        Map<ResourceLocation, MobilityProfile> parsed = new HashMap<>();
        for (Map.Entry<String, JsonElement> entry : root.entrySet()) {
            ResourceLocation id = ResourceLocation.tryParse(entry.getKey());
            if (id == null || !entry.getValue().isJsonObject()) {
                continue;
            }
            JsonObject value = entry.getValue().getAsJsonObject();
            parsed.put(id, new MobilityProfile(
                    mode(value, "flightMode"),
                    mode(value, "swimMode"),
                    walkMode(value),
                    value.has("gravity") ? value.get("gravity").getAsDouble() : 0.08D,
                    value.has("inertia") ? value.get("inertia").getAsDouble() : 0.35D,
                    value.has("landedSpeedModifier") ? value.get("landedSpeedModifier").getAsDouble() : 0.0D,
                    value.has("waterSpeedModifier") ? value.get("waterSpeedModifier").getAsDouble() : 0.0D,
                    value.has("fallSpeedModifier") ? value.get("fallSpeedModifier").getAsDouble() : 0.0D,
                    value.has("climbsWalls") && value.get("climbsWalls").getAsBoolean(),
                    value.has("phasesThroughWalls") && value.get("phasesThroughWalls").getAsBoolean(),
                    value.has("flopsOnLand") && value.get("flopsOnLand").getAsBoolean()
            ));
        }
        return parsed;
    }

    private static MobilityMode mode(JsonObject object, String key) {
        if (!object.has(key)) {
            return MobilityMode.DISABLED;
        }
        try {
            return MobilityMode.valueOf(object.get(key).getAsString());
        } catch (IllegalArgumentException exception) {
            return MobilityMode.DISABLED;
        }
    }

    private static WalkMode walkMode(JsonObject object) {
        if (!object.has("walkMode")) {
            return WalkMode.NORMAL;
        }
        try {
            return WalkMode.valueOf(object.get("walkMode").getAsString());
        } catch (IllegalArgumentException exception) {
            return WalkMode.NORMAL;
        }
    }

    public enum MobilityMode {
        DISABLED,
        ENABLED,
        FORCED,
        FLOATING,
        SINKING
    }

    public enum WalkMode {
        NORMAL,
        JUMPY
    }

    public record MobilityProfile(
            MobilityMode flightMode,
            MobilityMode swimMode,
            WalkMode walkMode,
            double gravity,
            double inertia,
            double landedSpeedModifier,
            double waterSpeedModifier,
            double fallSpeedModifier,
            boolean climbsWalls,
            boolean phasesThroughWalls,
            boolean flopsOnLand
    ) {
        public static final MobilityProfile DEFAULT =
                new MobilityProfile(
                        MobilityMode.DISABLED,
                        MobilityMode.DISABLED,
                        WalkMode.NORMAL,
                        0.08D,
                        0.35D,
                        0.0D,
                        0.0D,
                        0.0D,
                        false,
                        false,
                        false
                );

        public boolean canFly() {
            return flightMode == MobilityMode.ENABLED || flightMode == MobilityMode.FORCED;
        }

        public boolean forcesFlight() {
            return flightMode == MobilityMode.FORCED;
        }

        public boolean canSwim() {
            return swimMode == MobilityMode.ENABLED
                    || swimMode == MobilityMode.FORCED
                    || swimMode == MobilityMode.FLOATING;
        }
    }
}
