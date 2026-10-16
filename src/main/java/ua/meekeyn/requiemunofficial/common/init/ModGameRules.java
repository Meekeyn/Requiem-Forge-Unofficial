package ua.meekeyn.requiemunofficial.common.init;

import net.minecraft.world.level.GameRules;

public final class ModGameRules {

    public static final int STARTING_REMNANT_CHOOSE = 0;
    public static final int STARTING_REMNANT_FORCE_REMNANT = 1;
    public static final int STARTING_REMNANT_FORCE_VANILLA = 2;

    public static final int POSSESSION_KEEP_INVENTORY_NEVER = 0;
    public static final int POSSESSION_KEEP_INVENTORY_LIVING = 1;
    public static final int POSSESSION_KEEP_INVENTORY_ALWAYS = 2;

    public static final int POSSESSION_DETECTION_DISABLED = 0;
    public static final int POSSESSION_DETECTION_NORMAL = 1;
    public static final int POSSESSION_DETECTION_HARD = 2;

    public static final GameRules.Key<GameRules.BooleanValue> SHOW_POSSESSOR_NAME_TAG =
            GameRules.register(
                    "requiemunofficial:showPossessorNameTag",
                    GameRules.Category.PLAYER,
                    GameRules.BooleanValue.create(false)
            );

    public static final GameRules.Key<GameRules.BooleanValue> DISABLE_CURE =
            GameRules.register(
                    "requiemunofficial:disableCure",
                    GameRules.Category.PLAYER,
                    GameRules.BooleanValue.create(false)
            );

    public static final GameRules.Key<GameRules.BooleanValue> DROP_VESSEL_ON_DISCONNECT =
            GameRules.register(
                    "requiemunofficial:dropVesselOnDisconnect",
                    GameRules.Category.PLAYER,
                    GameRules.BooleanValue.create(false)
            );

    public static final GameRules.Key<GameRules.IntegerValue> STARTING_REMNANT_TYPE =
            GameRules.register(
                    "requiemunofficial:startingRemnantType",
                    GameRules.Category.PLAYER,
                    GameRules.IntegerValue.create(STARTING_REMNANT_CHOOSE)
            );

    public static final GameRules.Key<GameRules.IntegerValue> POSSESSION_KEEP_INVENTORY =
            GameRules.register(
                    "requiemunofficial:possessionKeepInventory",
                    GameRules.Category.PLAYER,
                    GameRules.IntegerValue.create(POSSESSION_KEEP_INVENTORY_NEVER)
            );

    public static final GameRules.Key<GameRules.IntegerValue> POSSESSION_DETECTION =
            GameRules.register(
                    "requiemunofficial:possessionDetection",
                    GameRules.Category.MOBS,
                    GameRules.IntegerValue.create(POSSESSION_DETECTION_NORMAL)
            );

    private ModGameRules() {
    }

    public static int getStartingRemnantType(GameRules rules) {
        return clampMode(rules.getInt(STARTING_REMNANT_TYPE));
    }

    public static int getPossessionKeepInventory(GameRules rules) {
        return clampMode(rules.getInt(POSSESSION_KEEP_INVENTORY));
    }

    public static int getPossessionDetection(GameRules rules) {
        return clampMode(rules.getInt(POSSESSION_DETECTION));
    }

    private static int clampMode(int value) {
        return Math.max(0, Math.min(2, value));
    }

    public static boolean shouldTransferPossessionInventory(GameRules rules, boolean hostAlive) {
        return switch (getPossessionKeepInventory(rules)) {
            case POSSESSION_KEEP_INVENTORY_LIVING -> !hostAlive;
            case POSSESSION_KEEP_INVENTORY_ALWAYS -> false;
            default -> true;
        };
    }

    public static void init() {
        // Loads the class early enough for the keys to be registered before worlds are created.
    }
}
