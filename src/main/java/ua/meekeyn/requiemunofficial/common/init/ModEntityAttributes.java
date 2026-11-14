package ua.meekeyn.requiemunofficial.common.init;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.RangedAttribute;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.neoforge.event.entity.EntityAttributeModificationEvent;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.DeferredHolder;
import ua.meekeyn.requiemunofficial.RequiemUnofficial;
import ua.meekeyn.requiemunofficial.common.entity.MorticianEntity;
import ua.meekeyn.requiemunofficial.common.entity.PlayerShellEntity;
import net.minecraft.world.entity.monster.piglin.Piglin;
import net.minecraft.world.entity.monster.piglin.PiglinBrute;
import net.minecraft.world.entity.npc.Villager;

public final class ModEntityAttributes {

    public static final DeferredRegister<Attribute> ATTRIBUTES =
            DeferredRegister.create(Registries.ATTRIBUTE, RequiemUnofficial.MODID);

    // beta.16 RequiemEntityAttributes: сила душі гравця (база 15) і вроджений захист душі будь-якої істоти (база 0).
    public static final DeferredHolder<Attribute, Attribute> SOUL_OFFENSE = ATTRIBUTES.register("soul_offense",
            () -> new RangedAttribute("attribute.name.remnant.requiemunofficial.soul_offense", 15.0D, 0.0D, 1024.0D)
                    .setSyncable(true));
    public static final DeferredHolder<Attribute, Attribute> SOUL_DEFENSE = ATTRIBUTES.register("soul_defense",
            () -> new RangedAttribute("attribute.name.remnant.requiemunofficial.soul_defense", 0.0D, -1024.0D, 1024.0D)
                    .setSyncable(true));

    private ModEntityAttributes() {
    }

    public static void init(IEventBus eventBus) {
        ATTRIBUTES.register(eventBus);
        eventBus.addListener(ModEntityAttributes::register);
        eventBus.addListener(ModEntityAttributes::modify);
    }

    public static void register(EntityAttributeCreationEvent event) {
        // Реєстрація базових атрибутів для сутностей мода
        event.put(ModEntities.MORTICIAN.get(), MorticianEntity.createAttributes().build());
        event.put(ModEntities.PLAYER_SHELL.get(), PlayerShellEntity.createAttributes().build());
        event.put(ModEntities.CURED_VILLAGER.get(), Villager.createAttributes().build());
        event.put(ModEntities.CURED_PIGLIN.get(), Piglin.createAttributes().build());
        event.put(ModEntities.CURED_PIGLIN_BRUTE.get(), PiglinBrute.createAttributes().build());
    }

    private static void modify(EntityAttributeModificationEvent event) {
        // beta.16 LivingEntityMixin: soul_defense є в усіх живих істот; PlayerEntityMixin: soul_offense у гравця.
        for (EntityType<? extends net.minecraft.world.entity.LivingEntity> type : event.getTypes()) {
            if (!event.has(type, SOUL_DEFENSE)) {
                event.add(type, SOUL_DEFENSE);
            }
        }
        event.add(EntityType.PLAYER, SOUL_OFFENSE);
        // beta.16 FrogEntityMixin: жаб'ячу душу витягти легше.
        event.add(EntityType.FROG, SOUL_DEFENSE, -5.0D);
        // beta.16 MorticianEntity.createMorticianAttributes
        event.add(ModEntities.MORTICIAN.get(), SOUL_OFFENSE, 30.0D);
    }
}
