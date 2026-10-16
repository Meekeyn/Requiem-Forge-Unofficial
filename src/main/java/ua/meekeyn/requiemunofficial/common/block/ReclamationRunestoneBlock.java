package ua.meekeyn.requiemunofficial.common.block;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;

import java.util.function.Supplier;

public class ReclamationRunestoneBlock extends RunestoneBlock {

    public ReclamationRunestoneBlock(Properties properties, String runeType, int maxLevel,
                                     Supplier<? extends MobEffect> effectSupplier) {
        super(properties, runeType, maxLevel, effectSupplier);
    }

    @Override
    public void applyEffect(ServerPlayer player, int runeLevel, int obeliskWidth) {
        MobEffect effect = getEffect();
        if (effect == null) {
            return;
        }

        int duration = Math.max(20 * 30, (10 - Math.min(obeliskWidth, 9)) * 20 * 60);
        int amplifier = Math.max(0, runeLevel - 1);

        MobEffectInstance currentEffect = player.getEffect(effect);
        if (currentEffect != null) {
            boolean strongerAlready = currentEffect.getAmplifier() > amplifier;
            boolean enoughDuration = currentEffect.getDuration() > duration / 2;

            if (strongerAlready || enoughDuration) {
                return;
            }
        }

        // Накладає або продовжує відновлювальний ефект reclamation
        player.addEffect(new MobEffectInstance(
                effect,
                duration,
                amplifier,
                true,
                false,
                true
        ));
    }
}
