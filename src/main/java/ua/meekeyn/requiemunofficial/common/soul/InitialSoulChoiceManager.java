package ua.meekeyn.requiemunofficial.common.soul;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.fml.ModList;
import ua.meekeyn.requiemunofficial.common.gamerule.StartingRemnantType;
import ua.meekeyn.requiemunofficial.common.init.ModGameRules;
import ua.meekeyn.requiemunofficial.common.advancement.ModCriteria;

import java.lang.reflect.Method;
import java.util.Map;

public final class InitialSoulChoiceManager {

    private static final String CHOICE_MADE = "RequiemInitialSoulChoiceMade";
    private static final ResourceLocation ORIGINS_SOUL_LAYER =
            new ResourceLocation("requiemunofficial", "soul_type");
    private static final ResourceLocation ORIGINS_MORTAL =
            new ResourceLocation("requiemunofficial", "mortal");
    private static final ResourceLocation ORIGINS_REMNANT =
            new ResourceLocation("requiemunofficial", "remnant");

    private InitialSoulChoiceManager() {
    }

    public static void initialize(ServerPlayer player) {
        if (SoulManager.getState(player) != SoulState.MORTAL) {
            markChosen(player);
        }
    }

    public static void choose(ServerPlayer player, SoulState choice) {
        if (choice != SoulState.MORTAL && choice != SoulState.REMNANT) {
            return;
        }
        DeathSuspensionManager.choose(player, choice);
    }

    public static boolean syncFromOrigins(ServerPlayer player) {
        if (!ModList.get().isLoaded("origins")
                || hasChosen(player)
                || SoulManager.getState(player) != SoulState.MORTAL) {
            return false;
        }

        ResourceLocation origin = readOriginsSoulType(player);
        if (ORIGINS_REMNANT.equals(origin)) {
            applyChoice(player, SoulState.REMNANT);
            return true;
        }
        if (ORIGINS_MORTAL.equals(origin)) {
            applyChoice(player, SoulState.MORTAL);
            return true;
        }
        return false;
    }

    static void applyChoice(ServerPlayer player, SoulState choice) {
        SoulManager.setState(player, choice == SoulState.REMNANT ? SoulState.REMNANT : SoulState.MORTAL);
        markChosen(player);
        SoulManager.sync(player);
        ModCriteria.MADE_REMNANT_CHOICE.trigger(player, choice);
    }

    private static ResourceLocation readOriginsSoulType(ServerPlayer player) {
        try {
            Class<?> containerApi = Class.forName("io.github.edwinmindcraft.origins.api.capabilities.IOriginContainer");
            Method get = containerApi.getMethod("get", net.minecraft.world.entity.Entity.class);
            Object optional = get.invoke(null, player);
            if (!(optional instanceof LazyOptional<?> lazyOptional)) {
                return null;
            }

            final ResourceLocation[] selected = new ResourceLocation[1];
            lazyOptional.ifPresent(container -> {
                try {
                    Method getOrigins = container.getClass().getMethod("getOrigins");
                    Object rawOrigins = getOrigins.invoke(container);
                    if (!(rawOrigins instanceof Map<?, ?> origins)) {
                        return;
                    }
                    for (Map.Entry<?, ?> entry : origins.entrySet()) {
                        if (!(entry.getKey() instanceof ResourceKey<?> layer)
                                || !(entry.getValue() instanceof ResourceKey<?> origin)) {
                            continue;
                        }
                        if (ORIGINS_SOUL_LAYER.equals(layer.location())) {
                            selected[0] = origin.location();
                            return;
                        }
                    }
                } catch (ReflectiveOperationException ignored) {
                }
            });
            return selected[0];
        } catch (ReflectiveOperationException exception) {
            return null;
        }
    }

    public static boolean hasChosen(Player player) {
        if (persisted(player).getBoolean(CHOICE_MADE)) {
            return true;
        }
        return player instanceof ServerPlayer serverPlayer
                && DeathSuspensionManager.advancementChoiceDone(serverPlayer);
    }

    static SoulState forcedChoice(ServerPlayer player) {
        return ModGameRules.getStartingRemnantType(player.level().getGameRules())
                == StartingRemnantType.FORCE_REMNANT
                ? SoulState.REMNANT
                : SoulState.MORTAL;
    }

    static void markChosen(Player player) {
        persisted(player).putBoolean(CHOICE_MADE, true);
    }

    public static void markChosenForTest(Player player) {
        markChosen(player);
    }

    private static CompoundTag persisted(Player player) {
        CompoundTag root = player.getPersistentData();
        if (!root.contains(Player.PERSISTED_NBT_TAG, Tag.TAG_COMPOUND)) {
            root.put(Player.PERSISTED_NBT_TAG, new CompoundTag());
        }
        return root.getCompound(Player.PERSISTED_NBT_TAG);
    }
}
