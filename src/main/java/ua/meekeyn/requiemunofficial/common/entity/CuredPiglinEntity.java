package ua.meekeyn.requiemunofficial.common.entity;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.monster.piglin.AbstractPiglin;
import net.minecraft.world.entity.monster.piglin.Piglin;
import net.minecraft.world.level.Level;

public class CuredPiglinEntity extends Piglin {

    public CuredPiglinEntity(EntityType<? extends AbstractPiglin> entityType, Level level) {
        super(entityType, level);
    }

    @Override
    public boolean isConverting() {
        return false;
    }
}
