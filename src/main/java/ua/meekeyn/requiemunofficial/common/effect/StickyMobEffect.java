package ua.meekeyn.requiemunofficial.common.effect;

import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;

public interface StickyMobEffect {
    boolean shouldStick(LivingEntity entity);

    default boolean shouldFreezeDuration(LivingEntity entity) {
        return shouldStick(entity);
    }

    static boolean shouldStick(MobEffectInstance effect, LivingEntity affected) {
        return effect.getEffect() instanceof StickyMobEffect sticky
                && sticky.shouldStick(affected)
                && (effect.getDuration() > 0 || sticky.shouldFreezeDuration(affected));
    }
}
