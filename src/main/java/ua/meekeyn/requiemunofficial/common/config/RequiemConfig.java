package ua.meekeyn.requiemunofficial.common.config;

import net.neoforged.neoforge.common.ModConfigSpec;

public final class RequiemConfig {
    public static final ModConfigSpec SPEC;

    public static final ModConfigSpec.DoubleValue SOUL_CAPTURE_CHANCE;
    public static final ModConfigSpec.IntValue RIFT_TELEPORT_ENERGY_COST;
    public static final ModConfigSpec.IntValue RUNE_EFFECT_BASE_DURATION_TICKS;
    public static final ModConfigSpec.IntValue RUNE_EFFECT_WIDTH_DURATION_BONUS_TICKS;
    public static final ModConfigSpec.IntValue RUNE_EFFECT_LEVEL_DURATION_BONUS_TICKS;
    public static final ModConfigSpec.BooleanValue ALLOW_UNCONDITIONAL_POSSESSION_EXIT;
    public static final ModConfigSpec.BooleanValue ALLOW_OWN_BODY_EXIT_WITHOUT_EMANCIPATION;
    public static final ModConfigSpec.BooleanValue OBELISK_REQUIRE_SOUL_BLOCKS;
    public static final ModConfigSpec.BooleanValue OBELISK_STORED_ENERGY_BONUS;
    public static final ModConfigSpec.BooleanValue RUNE_CRAFTING_RECIPES;
    public static final ModConfigSpec.BooleanValue ICHOR_VESSEL_USE_CONDITIONS;

    static {
        ModConfigSpec.Builder builder = new ModConfigSpec.Builder();

        // Усі налаштування тут — port-only (в оригиналі beta.16 їх немає), тому тримаємо їх
        // під єдиною секцією requiemunofficial.
        builder.push("requiemunofficial");

        builder.push("soul");
        SOUL_CAPTURE_CHANCE = builder
                .comment("Chance from 0.0 to 1.0 to capture a killed mob soul into an empty soul vessel.")
                .defineInRange("soulCaptureChance", 1.0D, 0.0D, 1.0D);
        builder.pop();

        builder.push("rift");
        RIFT_TELEPORT_ENERGY_COST = builder
                .comment("Stored soul energy consumed by one rift-to-rift teleport.",
                        "Set to 0 to match original beta.16 behavior (attrition only, no energy cost).")
                .defineInRange("riftTeleportEnergyCost", 0, 0, 1000);
        builder.pop();

        builder.push("runestones");
        RUNE_EFFECT_BASE_DURATION_TICKS = builder
                .comment("Base duration for obelisk runestone effects, in ticks.",
                        "Duration = base + widthBonus * (width - 1) + levelBonus * (level - 1).",
                        "Defaults 220 / 40 / 0 reproduce the original beta.16 formula (9 + width * 2) * 20.")
                .defineInRange("runeEffectBaseDurationTicks", 220, 20, 20 * 60 * 60);
        RUNE_EFFECT_WIDTH_DURATION_BONUS_TICKS = builder
                .comment("Extra duration per obelisk width above 1, in ticks.",
                        "Original beta.16: +40 ticks per width above 1.")
                .defineInRange("runeEffectWidthDurationBonusTicks", 40, 0, 20 * 60 * 60);
        RUNE_EFFECT_LEVEL_DURATION_BONUS_TICKS = builder
                .comment("Extra duration per rune level above 1, in ticks.",
                        "Original beta.16 does not have level bonus; set to 0 to match.")
                .defineInRange("runeEffectLevelDurationBonusTicks", 0, 0, 20 * 60 * 60);
        OBELISK_REQUIRE_SOUL_BLOCKS = builder
                .comment("Obelisks draw power from soul sand / soul soil (#minecraft:soul_speed_blocks) around them.",
                        "Set to false to let obelisks work at full power anywhere. Original beta.16: true.")
                .define("obeliskRequireSoulBlocks", true);
        OBELISK_STORED_ENERGY_BONUS = builder
                .comment("Port-only: stored soul energy adds up to +0.5 obelisk power. Original beta.16: false.")
                .define("obeliskStoredEnergyBonus", false);
        builder.pop();

        builder.push("extras");
        RUNE_CRAFTING_RECIPES = builder
                .comment("Port-only: shapeless crafting recipes for runic tachylite (runestone + ichor vessel)",
                        "and the rift rune. Original beta.16 only carves runes by using an ichor vessel on a runestone.",
                        "Requires /reload or world restart.")
                .define("runeCraftingRecipes", false);
        ICHOR_VESSEL_USE_CONDITIONS = builder
                .comment("Port-only: Reclamation ichor needs missing health, Attrition/Penance ichor cannot be drunk",
                        "while that effect is already active. Original beta.16: false (always drinkable).")
                .define("ichorVesselUseConditions", false);
        builder.pop();

        builder.push("possession");
        ALLOW_UNCONDITIONAL_POSSESSION_EXIT = builder
                .comment("Allow port-only unconditional possession exit (bound to its own key).",
                        "When false, dissociation follows the original beta.16 conditions.")
                .define("allowUnconditionalPossessionExit", false);
        ALLOW_OWN_BODY_EXIT_WITHOUT_EMANCIPATION = builder
                .comment("Allow a living remnant to leave their own body with the dissociation key (',')",
                        "without the Emancipation effect. When false, the original beta.16 rule applies",
                        "(Emancipation required), unless allowUnconditionalPossessionExit is enabled.")
                .define("allowOwnBodyExitWithoutEmancipation", false);
        builder.pop();

        builder.pop();

        SPEC = builder.build();
    }

    private RequiemConfig() {
    }
}
