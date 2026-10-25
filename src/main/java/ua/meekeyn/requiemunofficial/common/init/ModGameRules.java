package ua.meekeyn.requiemunofficial.common.init;

import net.minecraft.commands.synchronization.ArgumentTypeInfo;
import net.minecraft.commands.synchronization.ArgumentTypeInfos;
import net.minecraft.world.level.GameRules;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import ua.meekeyn.requiemunofficial.RequiemUnofficial;
import ua.meekeyn.requiemunofficial.common.gamerule.EnumArgumentInfo;
import ua.meekeyn.requiemunofficial.common.gamerule.EnumGameRule;
import ua.meekeyn.requiemunofficial.common.gamerule.PossessionDetection;
import ua.meekeyn.requiemunofficial.common.gamerule.PossessionKeepInventory;
import ua.meekeyn.requiemunofficial.common.gamerule.StartingRemnantType;

public final class ModGameRules {

    /**
     * Реєстрація типу аргумента {@code EnumArgumentType} для мережі: коли сервер відправляє
     * дерево команд (ClientboundCommandsPacket), Minecraft шукає тип у ArgumentTypeInfos та
     * BuiltInRegistries.COMMAND_ARGUMENT_TYPE. Без цієї реєстрації вхід у світ падає з
     * "Unrecognized argument type". Див. Forge-документацію до ArgumentTypeInfos.registerByClass.
     */
    public static final DeferredRegister<ArgumentTypeInfo<?, ?>> ARGUMENT_TYPES =
            DeferredRegister.create(ForgeRegistries.COMMAND_ARGUMENT_TYPES, RequiemUnofficial.MODID);

    public static final RegistryObject<EnumArgumentInfo> ENUM_ARGUMENT_TYPE_INFO =
            ARGUMENT_TYPES.register("enum", () -> ArgumentTypeInfos.registerByClass(
                    EnumArgumentInfo.enumArgumentClass(),
                    EnumArgumentInfo.INSTANCE
            ));

    // Оригінальні правила beta.16 зберігають простір імен requiem: і текстові перелічувані значення.
    public static final GameRules.Key<GameRules.BooleanValue> SHOW_POSSESSOR_NAME_TAG =
            GameRules.register(
                    "requiem:showPossessorNameTag",
                    GameRules.Category.PLAYER,
                    GameRules.BooleanValue.create(false)
            );

    public static final GameRules.Key<GameRules.BooleanValue> DISABLE_CURE =
            GameRules.register(
                    "requiem:disableCure",
                    GameRules.Category.PLAYER,
                    GameRules.BooleanValue.create(false)
            );

    public static final GameRules.Key<GameRules.BooleanValue> DROP_VESSEL_ON_DISCONNECT =
            GameRules.register(
                    "requiem:dropVesselOnDisconnect",
                    GameRules.Category.PLAYER,
                    GameRules.BooleanValue.create(false)
            );

    public static final GameRules.Key<EnumGameRule<StartingRemnantType>> STARTING_REMNANT_TYPE =
            GameRules.register(
                    "requiem:startingRemnantType",
                    GameRules.Category.PLAYER,
                    EnumGameRule.create(StartingRemnantType.class, StartingRemnantType.CHOOSE, (server, rule) -> {
                    })
            );

    public static final GameRules.Key<EnumGameRule<PossessionKeepInventory>> POSSESSION_KEEP_INVENTORY =
            GameRules.register(
                    "requiem:possessionKeepInventory",
                    GameRules.Category.PLAYER,
                    EnumGameRule.create(PossessionKeepInventory.class, PossessionKeepInventory.NEVER, (server, rule) -> {
                    })
            );

    public static final GameRules.Key<EnumGameRule<PossessionDetection>> POSSESSION_DETECTION =
            GameRules.register(
                    "requiem:possessionDetection",
                    GameRules.Category.MOBS,
                    EnumGameRule.create(PossessionDetection.class, PossessionDetection.NORMAL, (server, rule) -> {
                    })
            );

    private ModGameRules() {
    }

    public static StartingRemnantType getStartingRemnantType(GameRules rules) {
        return rules.getRule(STARTING_REMNANT_TYPE).get();
    }

    public static PossessionKeepInventory getPossessionKeepInventory(GameRules rules) {
        return rules.getRule(POSSESSION_KEEP_INVENTORY).get();
    }

    public static PossessionDetection getPossessionDetection(GameRules rules) {
        return rules.getRule(POSSESSION_DETECTION).get();
    }

    public static boolean shouldTransferPossessionInventory(GameRules rules, boolean hostAlive) {
        return getPossessionKeepInventory(rules).shouldTransfer(hostAlive);
    }

    public static void init() {
        // Loads the class early enough for the keys to be registered before worlds are created.
    }

    public static void register(IEventBus eventBus) {
        ARGUMENT_TYPES.register(eventBus);
    }
}
