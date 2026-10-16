package ua.meekeyn.requiemunofficial.common.entity.ai;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.entity.LivingEntity;
import ua.meekeyn.requiemunofficial.common.entity.MorticianEntity;
import ua.meekeyn.requiemunofficial.common.init.ModSounds;
import ua.meekeyn.requiemunofficial.common.item.SoulVesselItem;

public class StealSoulGoal extends MorticianSpellGoal {
    public StealSoulGoal(MorticianEntity mortician) {
        super(mortician);
    }

    @Override
    public boolean canUse() {
        return super.canUse()
                && mortician.getTarget() != null
                && SoulVesselItem.canCaptureSoul(mortician.getTarget());
    }

    @Override
    protected SoundEvent getSoundPrepare() {
        return ModSounds.ENTITY_MORTICIAN_PREPARE_ATTACK.get();
    }

    @Override
    protected int getCooldown() {
        return 200;
    }

    @Override
    protected int getWarmupTime() {
        return 100;
    }

    @Override
    public void tick() {
        LivingEntity target = mortician.getTarget();
        if (target != null) {
            SoulVesselItem.playSoulCaptureEffects(mortician, target);
        }
        super.tick();
    }

    @Override
    protected void castSpell() {
        LivingEntity target = mortician.getTarget();
        if (target == null) {
            return;
        }

        java.util.UUID bodyRecord = SoulVesselItem.tryStealSoulToRecord(mortician, target);
        if (bodyRecord != null) {
            mortician.addCapturedSoul(bodyRecord);
        } else {
            spawnFailureParticles();
        }
    }

    private void spawnFailureParticles() {
        if (!(mortician.level() instanceof ServerLevel level)) {
            return;
        }
        for (int i = 0; i < 7; i++) {
            level.sendParticles(
                    ParticleTypes.SMOKE,
                    mortician.getRandomX(1.0D),
                    mortician.getRandomY() + 0.5D,
                    mortician.getRandomZ(1.0D),
                    1,
                    mortician.getRandom().nextGaussian() * 0.02D,
                    mortician.getRandom().nextGaussian() * 0.02D,
                    mortician.getRandom().nextGaussian() * 0.02D,
                    0.0D
            );
        }
    }
}
