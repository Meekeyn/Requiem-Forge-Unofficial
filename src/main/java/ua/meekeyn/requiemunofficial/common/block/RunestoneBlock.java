package ua.meekeyn.requiemunofficial.common.block;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import org.jetbrains.annotations.Nullable;
import ua.meekeyn.requiemunofficial.common.config.RequiemConfig;

import java.util.function.Supplier;

public class RunestoneBlock extends InertRunestoneBlock {

    private final String runeType;
    private final int maxLevel;
    @Nullable
    private final Supplier<? extends MobEffect> effectSupplier;

    public RunestoneBlock(Properties properties) {
        this(properties, "neutral", 0, (Supplier<? extends MobEffect>) null);
    }

    public RunestoneBlock(Properties properties, String runeType, int maxLevel) {
        this(properties, runeType, maxLevel, (Supplier<? extends MobEffect>) null);
    }

    public RunestoneBlock(Properties properties, String runeType, int maxLevel, @Nullable MobEffect effect) {
        this(properties, runeType, maxLevel, effect == null ? null : () -> effect);
    }

    public RunestoneBlock(Properties properties, String runeType, int maxLevel,
                         @Nullable Supplier<? extends MobEffect> effectSupplier) {
        super(properties);
        this.runeType = runeType;
        this.maxLevel = maxLevel;
        this.effectSupplier = effectSupplier;
    }

    public String getRuneType() {
        return runeType;
    }

    public int getMaxLevel() {
        return maxLevel;
    }

    @Nullable
    public MobEffect getEffect() {
        return effectSupplier == null ? null : effectSupplier.get();
    }

    public boolean hasEffect() {
        return effectSupplier != null;
    }

    protected int getEffectDurationTicks(int runeLevel, int obeliskWidth) {
        int widthBonus = Math.max(0, obeliskWidth - 1) * RequiemConfig.RUNE_EFFECT_WIDTH_DURATION_BONUS_TICKS.get();
        int levelBonus = Math.max(0, runeLevel - 1) * RequiemConfig.RUNE_EFFECT_LEVEL_DURATION_BONUS_TICKS.get();
        return RequiemConfig.RUNE_EFFECT_BASE_DURATION_TICKS.get() + widthBonus + levelBonus;
    }

    protected int getEffectAmplifier(int runeLevel, int obeliskWidth) {
        return Math.max(0, runeLevel - 1);
    }

    protected boolean shouldApplyEffect(ServerPlayer player, int runeLevel, int obeliskWidth) {
        return getEffect() != null;
    }

    public void applyEffect(ServerPlayer player, int runeLevel, int obeliskWidth) {
        MobEffect effect = getEffect();
        if (!shouldApplyEffect(player, runeLevel, obeliskWidth) || effect == null) {
            return;
        }

        int duration = getEffectDurationTicks(runeLevel, obeliskWidth);
        int amplifier = getEffectAmplifier(runeLevel, obeliskWidth);

        MobEffectInstance currentEffect = player.getEffect(effect);

        if (currentEffect != null) {
            boolean weakerAmplifier = currentEffect.getAmplifier() > amplifier;
            boolean enoughDuration = currentEffect.getDuration() > duration / 2;

            if (weakerAmplifier || enoughDuration) {
                return;
            }
        }

        boolean showParticles = effect.getCategory() != MobEffectCategory.HARMFUL;

        // Накладає або оновлює базовий ефект руни
        player.addEffect(new MobEffectInstance(
                effect,
                duration,
                amplifier,
                true,
                showParticles,
                true
        ));
    }
}
