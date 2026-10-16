package ua.meekeyn.requiemunofficial.common.effect;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import ua.meekeyn.requiemunofficial.common.soul.SoulManager;
import ua.meekeyn.requiemunofficial.common.soul.SoulState;

public class ReclamationMobEffect extends MobEffect implements StickyMobEffect {

    public ReclamationMobEffect() {
        super(MobEffectCategory.BENEFICIAL, 0x3F8A63);
    }

    @Override
    public boolean isDurationEffectTick(int duration, int amplifier) {
        return duration == 1;
    }

    @Override
    public void applyEffectTick(LivingEntity entity, int amplifier) {
        if (!entity.level().isClientSide()) {
            AttritionMobEffect.reduce(entity, amplifier + 1);
        }
    }

    @Override
    public boolean shouldStick(LivingEntity entity) {
        return false;
    }

    @Override
    public boolean shouldFreezeDuration(LivingEntity entity) {
        return entity instanceof Player player && SoulManager.getState(player) == SoulState.VAGRANT;
    }
}
