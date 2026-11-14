package ua.meekeyn.requiemunofficial.common.effect;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.server.level.ServerPlayer;
import ua.meekeyn.requiemunofficial.common.init.ModEffects;
import ua.meekeyn.requiemunofficial.common.possession.PossessionManager;
import ua.meekeyn.requiemunofficial.common.soul.SoulManager;
import ua.meekeyn.requiemunofficial.common.soul.SoulState;

import java.util.UUID;

public class EmancipationMobEffect extends MobEffect implements StickyMobEffect {

    public EmancipationMobEffect() {
        super(MobEffectCategory.BENEFICIAL, 0xE8E0B8);
    }

    @Override
    public boolean shouldApplyEffectTickThisTick(int duration, int amplifier) {
        return duration % 20 == 0;
    }

    @Override
    public boolean applyEffectTick(LivingEntity entity, int amplifier) {
        // Emancipation allows dissociation via the release key while possessed.
        // Auto-release on effect tick was removed: the effect should not force
        // exit every second, which would make possession impossible to maintain.
        return true;
    }

    @Override
    public boolean shouldStick(LivingEntity entity) {
        // Emancipation should stick to both REMNANT and VAGRANT players, so they can
        // use the dissociation key to exit possession. Without this, the effect would
        // be stripped when a REMNANT becomes VAGRANT by possessing a body.
        return entity instanceof ServerPlayer player
                && (SoulManager.getState(player) == SoulState.REMNANT
                || SoulManager.getState(player) == SoulState.VAGRANT);
    }

    @Override
    public boolean shouldFreezeDuration(LivingEntity entity) {
        return false;
    }
}
