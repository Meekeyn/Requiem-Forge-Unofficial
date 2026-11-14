package ua.meekeyn.requiemunofficial.common.init;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.levelgen.structure.StructureType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.DeferredHolder;
import ua.meekeyn.requiemunofficial.RequiemUnofficial;
import ua.meekeyn.requiemunofficial.common.structure.DerelictObeliskStructure;

public final class ModStructures {
    public static final DeferredRegister<StructureType<?>> STRUCTURE_TYPES =
            DeferredRegister.create(Registries.STRUCTURE_TYPE, RequiemUnofficial.MODID);

    public static final DeferredHolder<StructureType<?>, StructureType<DerelictObeliskStructure>> DERELICT_OBELISK =
            STRUCTURE_TYPES.register("derelict_obelisk", () -> (StructureType<DerelictObeliskStructure>) () -> DerelictObeliskStructure.CODEC);

    private ModStructures() {
    }

    public static void register(IEventBus eventBus) {
        STRUCTURE_TYPES.register(eventBus);
    }
}
