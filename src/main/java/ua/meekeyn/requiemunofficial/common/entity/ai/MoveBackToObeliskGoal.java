package ua.meekeyn.requiemunofficial.common.entity.ai;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.ai.goal.RandomStrollGoal;
import net.minecraft.world.entity.ai.util.DefaultRandomPos;
import net.minecraft.world.phys.Vec3;
import ua.meekeyn.requiemunofficial.common.entity.MorticianEntity;

import javax.annotation.Nullable;

public class MoveBackToObeliskGoal extends RandomStrollGoal {
    private static final int HORIZONTAL_RANGE = 10;
    private static final int VERTICAL_RANGE = 7;
    private final MorticianEntity mortician;

    public MoveBackToObeliskGoal(MorticianEntity mortician, double speedModifier) {
        super(mortician, speedModifier, 10);
        this.mortician = mortician;
    }

    @Override
    public boolean canUse() {
        BlockPos home = mortician.getHomePos();
        return home != null
                && mortician.isHomeDimension()
                && !mortician.blockPosition().closerThan(home, 16.0D)
                && super.canUse();
    }

    @Override
    @Nullable
    protected Vec3 getPosition() {
        BlockPos home = mortician.getHomePos();
        if (home == null) {
            return null;
        }
        return DefaultRandomPos.getPosTowards(
                mortician,
                HORIZONTAL_RANGE,
                VERTICAL_RANGE,
                Vec3.atCenterOf(home),
                Math.PI / 2.0D
        );
    }
}
