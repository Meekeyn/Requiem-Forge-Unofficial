package ua.meekeyn.requiemunofficial.common.entity.ai;

import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import ua.meekeyn.requiemunofficial.common.entity.MorticianEntity;
import ua.meekeyn.requiemunofficial.common.init.ModEffects;
import ua.meekeyn.requiemunofficial.common.init.ModSounds;
import ua.meekeyn.requiemunofficial.common.possession.PossessionManager;

public class FreeFromMortalCoilGoal extends MorticianSpellGoal {
    public static final int PENANCE_TIME = 10 * 20 * 60;

    public FreeFromMortalCoilGoal(MorticianEntity mortician) {
        super(mortician);
    }

    @Override
    public boolean canUse() {
        return super.canUse() && mortician.getTarget() instanceof Player;
    }

    @Override
    protected SoundEvent getSoundPrepare() {
        return ModSounds.ENTITY_MORTICIAN_PREPARE_ATTACK.get();
    }

    @Override
    protected int getCooldown() {
        return 100;
    }

    @Override
    protected int getWarmupTime() {
        return 20;
    }

    @Override
    protected void castSpell() {
        LivingEntity target = mortician.getTarget();
        if (!(target instanceof Player player)) {
            return;
        }

        int amplifier = PossessionManager.isPossessing(player) ? 2 : 1;
        player.addEffect(new MobEffectInstance(ModEffects.PENANCE.get(), PENANCE_TIME, amplifier, false, false, true));
        player.knockback(1.0D, Math.sin(mortician.getYRot() * Math.PI / 180.0D),
                -Math.cos(mortician.getYRot() * Math.PI / 180.0D));
    }
}
