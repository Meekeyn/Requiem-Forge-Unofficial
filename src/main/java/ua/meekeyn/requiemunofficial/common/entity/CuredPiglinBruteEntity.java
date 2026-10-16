package ua.meekeyn.requiemunofficial.common.entity;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.monster.piglin.PiglinBrute;
import net.minecraft.world.level.Level;

public class CuredPiglinBruteEntity extends PiglinBrute {

    public CuredPiglinBruteEntity(EntityType<? extends PiglinBrute> entityType, Level level) {
        super(entityType, level);
    }

    @Override
    public boolean isConverting() {
        return false;
    }
}
