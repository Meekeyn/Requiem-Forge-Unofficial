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
    public boolean isDurationEffectTick(int duration, int amplifier) {
        return duration % 20 == 0;
    }

    @Override
    public void applyEffectTick(LivingEntity entity, int amplifier) {
        if (!(entity instanceof ServerPlayer player)) {
            return;
        }

        UUID possessedId = SoulManager.getPossessedEntityId(player);
        if (possessedId != null) {
            PossessionManager.release(player, PossessionManager.findPossessedHost(player, possessedId));
            player.removeEffect(ModEffects.EMANCIPATION.get());
        }
    }

    @Override
    public boolean shouldStick(LivingEntity entity) {
        return entity instanceof ServerPlayer player && SoulManager.getState(player) == SoulState.REMNANT;
    }

    @Override
    public boolean shouldFreezeDuration(LivingEntity entity) {
        return false;
    }
}
