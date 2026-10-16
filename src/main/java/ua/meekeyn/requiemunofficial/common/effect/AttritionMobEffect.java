package ua.meekeyn.requiemunofficial.common.effect;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeMap;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import ua.meekeyn.requiemunofficial.common.init.ModDamageSources;
import ua.meekeyn.requiemunofficial.common.init.ModEffects;
import ua.meekeyn.requiemunofficial.common.possession.PossessionManager;
import ua.meekeyn.requiemunofficial.common.soul.SoulManager;
import ua.meekeyn.requiemunofficial.common.soul.SoulState;

public class AttritionMobEffect extends MobEffect implements StickyMobEffect {

    public static final int MAX_LEVEL = 3;
    public static final int DEFAULT_DURATION = 300;

    public AttritionMobEffect() {
        super(MobEffectCategory.HARMFUL, 0xAA3322);
        addAttributeModifier(
                Attributes.MAX_HEALTH,
                "069ae0b1-4014-41dd-932f-a5da4417d711",
                -0.2D,
                AttributeModifier.Operation.MULTIPLY_TOTAL
        );
    }

    public static void apply(LivingEntity target, int amount, int minDuration) {
        if (amount <= 0) {
            return;
        }

        MobEffectInstance attrition = target.getEffect(ModEffects.ATTRITION.get());
        int expectedAmplifier = attrition == null ? amount - 1 : attrition.getAmplifier() + amount;
        int amplifier = Math.min(MAX_LEVEL, expectedAmplifier);
        int duration = Math.max(attrition == null ? 0 : attrition.getDuration(), minDuration);
        addAttrition(target, amplifier, duration);

        if (expectedAmplifier > MAX_LEVEL
                && (!(target instanceof Player) || target.level().getLevelData().isHardcore())) {
            target.hurt(ModDamageSources.attritionHardcoreDeath(target), Float.MAX_VALUE);
        }
    }

    public static void addAttrition(LivingEntity target, int amplifier, int duration) {
        MobEffectInstance effect = new MobEffectInstance(
                ModEffects.ATTRITION.get(),
                duration,
                Math.min(amplifier, MAX_LEVEL),
                false,
                false,
                true
        );
        if (target instanceof ServerPlayer player && !hasLiveConnection(player)) {
            player.getActiveEffectsMap().put(effect.getEffect(), effect);
        } else {
            target.addEffect(effect);
        }
    }

    public static void reduce(LivingEntity target, int amount) {
        if (amount <= 0) {
            return;
        }

        MobEffectInstance attrition = target.getEffect(ModEffects.ATTRITION.get());
        if (attrition == null) {
            return;
        }

        int amplifier = attrition.getAmplifier() - amount;
        if (target instanceof ServerPlayer player && !hasLiveConnection(player)) {
            player.getActiveEffectsMap().remove(ModEffects.ATTRITION.get());
        } else {
            StickyEffectReapplicationManager.definitivelyClear(
                    target,
                    ModEffects.ATTRITION.get(),
                    () -> target.removeEffect(ModEffects.ATTRITION.get())
            );
        }
        if (amplifier >= 0) {
            addAttrition(target, amplifier, DEFAULT_DURATION);
        }
    }

    private static boolean hasLiveConnection(ServerPlayer player) {
        return player.connection != null && player.connection.connection != null;
    }

    @Override
    public double getAttributeModifierValue(int amplifier, AttributeModifier modifier) {
        return super.getAttributeModifierValue(Math.min(amplifier, MAX_LEVEL), modifier);
    }

    @Override
    public void addAttributeModifiers(LivingEntity entity, AttributeMap attributes, int amplifier) {
        super.addAttributeModifiers(entity, attributes, amplifier);
        if (entity.getHealth() > entity.getMaxHealth()) {
            entity.setHealth(entity.getMaxHealth());
        }
    }

    @Override
    public boolean shouldStick(LivingEntity entity) {
        return entity instanceof Player player && SoulManager.getState(player) == SoulState.VAGRANT;
    }

    @Override
    public boolean shouldFreezeDuration(LivingEntity entity) {
        return shouldStick(entity) || PossessionManager.getPossessorId(entity) != null;
    }

    @Override
    public boolean isDurationEffectTick(int duration, int amplifier) {
        int interval = Math.max(10, 40 >> amplifier);
        return duration % interval == 0;
    }
}
