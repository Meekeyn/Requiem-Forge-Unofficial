package ua.meekeyn.requiemunofficial.common.init;

import net.minecraftforge.event.entity.EntityAttributeCreationEvent;
import ua.meekeyn.requiemunofficial.common.entity.MorticianEntity;
import ua.meekeyn.requiemunofficial.common.entity.PlayerShellEntity;
import net.minecraft.world.entity.monster.piglin.Piglin;
import net.minecraft.world.entity.monster.piglin.PiglinBrute;
import net.minecraft.world.entity.npc.Villager;

public final class ModEntityAttributes {

    private ModEntityAttributes() {
    }

    public static void register(EntityAttributeCreationEvent event) {
        // Реєстрація базових атрибутів для сутностей мода
        event.put(ModEntities.MORTICIAN.get(), MorticianEntity.createAttributes().build());
        event.put(ModEntities.PLAYER_SHELL.get(), PlayerShellEntity.createAttributes().build());
        event.put(ModEntities.CURED_VILLAGER.get(), Villager.createAttributes().build());
        event.put(ModEntities.CURED_PIGLIN.get(), Piglin.createAttributes().build());
        event.put(ModEntities.CURED_PIGLIN_BRUTE.get(), PiglinBrute.createAttributes().build());
    }
}
