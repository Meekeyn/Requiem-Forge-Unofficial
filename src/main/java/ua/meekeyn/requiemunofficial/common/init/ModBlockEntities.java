package ua.meekeyn.requiemunofficial.common.init;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;
import ua.meekeyn.requiemunofficial.RequiemUnofficial;
import ua.meekeyn.requiemunofficial.common.block.entity.DummyBlockEntity;
import ua.meekeyn.requiemunofficial.common.block.entity.RunestoneBlockEntity;

public final class ModBlockEntities {

    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITY_TYPES =
            DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, RequiemUnofficial.MODID);

    public static final RegistryObject<BlockEntityType<DummyBlockEntity>> DUMMY =
            BLOCK_ENTITY_TYPES.register("dummy",
                    () -> BlockEntityType.Builder.of(DummyBlockEntity::new, ModBlocks.TACHYLITE.get()).build(null));

    public static final RegistryObject<BlockEntityType<RunestoneBlockEntity>> RUNESTONE =
            BLOCK_ENTITY_TYPES.register("runestone",
                    () -> BlockEntityType.Builder.of(
                            RunestoneBlockEntity::new,
                            ModBlocks.TACHYLITE_RUNESTONE.get(),
                            ModBlocks.RUNIC_TACHYLITE_ATTRITION.get(),
                            ModBlocks.RUNIC_TACHYLITE_EMANCIPATION.get(),
                            ModBlocks.RUNIC_TACHYLITE_PENANCE.get(),
                            ModBlocks.RUNIC_TACHYLITE_RECLAMATION.get(),
                            ModBlocks.RIFT_RUNE.get()
                    ).build(null));

    private ModBlockEntities() {
    }

    public static void register(IEventBus eventBus) {
        BLOCK_ENTITY_TYPES.register(eventBus);
    }
}