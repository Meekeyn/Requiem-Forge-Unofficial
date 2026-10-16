package ua.meekeyn.requiemunofficial.common.entity.ai;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import ua.meekeyn.requiemunofficial.common.entity.MorticianEntity;

import java.util.EnumSet;

public class MorticianLookAtTargetGoal extends Goal {
    private final MorticianEntity mortician;

    public MorticianLookAtTargetGoal(MorticianEntity mortician) {
        this.mortician = mortician;
        setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        return mortician.isSpellcasting();
    }

    @Override
    public void start() {
        mortician.getNavigation().stop();
    }

    @Override
    public void tick() {
        LivingEntity target = mortician.getTarget();
        if (target != null) {
            mortician.getLookControl().setLookAt(target, 30.0F, 30.0F);
        }
    }
}
