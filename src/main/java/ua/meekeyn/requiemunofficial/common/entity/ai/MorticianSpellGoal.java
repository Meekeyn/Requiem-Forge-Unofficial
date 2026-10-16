package ua.meekeyn.requiemunofficial.common.entity.ai;

import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import ua.meekeyn.requiemunofficial.common.entity.MorticianEntity;

public abstract class MorticianSpellGoal extends Goal {
    protected final MorticianEntity mortician;
    protected int spellCooldown;
    protected int nextCastTime;

    protected MorticianSpellGoal(MorticianEntity mortician) {
        this.mortician = mortician;
    }

    @Override
    public void start() {
        spellCooldown = getWarmupTime();
        nextCastTime = mortician.tickCount + getCooldown();
        mortician.playSound(getSoundPrepare(), 1.0F, 1.0F);
        mortician.setSpellcasting(true);
    }

    @Override
    public boolean canUse() {
        LivingEntity target = mortician.getTarget();
        return target != null
                && target.isAlive()
                && !mortician.isSpellcasting()
                && mortician.tickCount >= nextCastTime;
    }

    @Override
    public boolean canContinueToUse() {
        LivingEntity target = mortician.getTarget();
        return target != null && target.isAlive() && spellCooldown > 0;
    }

    @Override
    public void stop() {
        mortician.setSpellcasting(false);
    }

    @Override
    public void tick() {
        --spellCooldown;
        if (spellCooldown == 0) {
            castSpell();
            mortician.playSound(mortician.getCastSpellSound(), 1.0F, 1.0F);
            mortician.setTarget(null);
        }
    }

    protected abstract SoundEvent getSoundPrepare();

    protected abstract int getCooldown();

    protected abstract int getWarmupTime();

    protected abstract void castSpell();
}
