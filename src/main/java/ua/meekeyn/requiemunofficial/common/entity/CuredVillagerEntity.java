package ua.meekeyn.requiemunofficial.common.entity;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.level.Level;

public class CuredVillagerEntity extends Villager {

    public CuredVillagerEntity(EntityType<? extends Villager> entityType, Level level) {
        super(entityType, level);
    }
}
