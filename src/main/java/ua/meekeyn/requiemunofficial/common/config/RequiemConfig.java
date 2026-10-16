package ua.meekeyn.requiemunofficial.common.config;

import net.minecraftforge.common.ForgeConfigSpec;

public final class RequiemConfig {
    public static final ForgeConfigSpec SPEC;

    public static final ForgeConfigSpec.DoubleValue SOUL_CAPTURE_CHANCE;
    public static final ForgeConfigSpec.IntValue OPUS_DAEMONIUM_XP_LEVEL_COST;
    public static final ForgeConfigSpec.IntValue RIFT_TELEPORT_ENERGY_COST;
    public static final ForgeConfigSpec.IntValue RUNE_EFFECT_BASE_DURATION_TICKS;
    public static final ForgeConfigSpec.IntValue RUNE_EFFECT_WIDTH_DURATION_BONUS_TICKS;
    public static final ForgeConfigSpec.IntValue RUNE_EFFECT_LEVEL_DURATION_BONUS_TICKS;

    static {
        ForgeConfigSpec.Builder builder = new ForgeConfigSpec.Builder();

        builder.push("soul");
        SOUL_CAPTURE_CHANCE = builder
                .comment("Chance from 0.0 to 1.0 to capture a killed mob soul into an empty soul vessel.")
                .defineInRange("soulCaptureChance", 1.0D, 0.0D, 1.0D);
        OPUS_DAEMONIUM_XP_LEVEL_COST = builder
                .comment("Legacy setting retained for old configs. Stage 20 beta.16-style Opus and remnant vessels do not spend XP.")
                .defineInRange("opusDaemoniumXpLevelCost", 5, 0, 100);
        builder.pop();

        builder.push("rift");
        RIFT_TELEPORT_ENERGY_COST = builder
                .comment("Stored soul energy consumed by one rift-to-rift teleport.")
                .defineInRange("riftTeleportEnergyCost", 1, 0, 1000);
        builder.pop();

        builder.push("runestones");
        RUNE_EFFECT_BASE_DURATION_TICKS = builder
                .comment("Base duration for obelisk runestone effects, in ticks.")
                .defineInRange("runeEffectBaseDurationTicks", 200, 20, 20 * 60 * 60);
        RUNE_EFFECT_WIDTH_DURATION_BONUS_TICKS = builder
                .comment("Extra duration per obelisk width above 1, in ticks.")
                .defineInRange("runeEffectWidthDurationBonusTicks", 40, 0, 20 * 60 * 60);
        RUNE_EFFECT_LEVEL_DURATION_BONUS_TICKS = builder
                .comment("Extra duration per rune level above 1, in ticks.")
                .defineInRange("runeEffectLevelDurationBonusTicks", 60, 0, 20 * 60 * 60);
        builder.pop();

        SPEC = builder.build();
    }

    private RequiemConfig() {
    }
}
