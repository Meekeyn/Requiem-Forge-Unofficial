package ua.meekeyn.requiemunofficial.common.init;

import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;
import ua.meekeyn.requiemunofficial.RequiemUnofficial;

public final class ModCreativeTabs {

    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, RequiemUnofficial.MODID);

    public static final RegistryObject<CreativeModeTab> MAIN_TAB =
            CREATIVE_MODE_TABS.register("main", () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup.requiemunofficial"))
                    .icon(() -> new ItemStack(ModItems.EMPTY_SOUL_VESSEL.get()))
                    .displayItems((parameters, output) -> {
                        output.accept(ModItems.DEBUG_ITEM.get());
                        output.accept(ModItems.OPUS_DAEMONIUM.get());
                        output.accept(ModItems.HUMANITY_ENCHANTED_BOOK.get());
                        output.accept(ModItems.EMPTY_SOUL_VESSEL.get());
                        output.accept(ModItems.FILLED_SOUL_VESSEL.get());
                        output.accept(ModItems.PURE_SOUL_VESSEL.get());
                        output.accept(ModItems.SEALED_REMNANT_VESSEL.get());
                        output.accept(ModItems.SHATTERED_SOUL_VESSEL.get());
                        output.accept(ModItems.CREATIVE_SOUL_VESSEL.get());
                        output.accept(ModItems.TOTEM_OF_SKELETONIZATION.get());
                        output.accept(ModItems.ICHOR_VESSEL_ATTRITION.get());
                        output.accept(ModItems.ICHOR_VESSEL_EMANCIPATION.get());
                        output.accept(ModItems.ICHOR_VESSEL_PENANCE.get());
                        output.accept(ModItems.ICHOR_VESSEL_RECLAMATION.get());

                        output.accept(ModBlocks.TACHYLITE.get());
                        output.accept(ModBlocks.CHISELED_TACHYLITE.get());
                        output.accept(ModBlocks.CHISELED_TACHYLITE_PILLAR.get());
                        output.accept(ModBlocks.CHISELED_TACHYLITE_SLAB.get());
                        output.accept(ModBlocks.CHISELED_TACHYLITE_STAIRS.get());
                        output.accept(ModBlocks.SCRAPED_TACHYLITE.get());
                        output.accept(ModBlocks.DERELICT_TACHYLITE.get());
                        output.accept(ModBlocks.TACHYLITE_RUNESTONE.get());
                        output.accept(ModBlocks.RUNIC_TACHYLITE_ATTRITION.get());
                        output.accept(ModBlocks.RUNIC_TACHYLITE_EMANCIPATION.get());
                        output.accept(ModBlocks.RUNIC_TACHYLITE_PENANCE.get());
                        output.accept(ModBlocks.RUNIC_TACHYLITE_RECLAMATION.get());
                        output.accept(ModBlocks.RIFT_RUNE.get());

                        output.accept(ModItems.MORTICIAN_SPAWN_EGG.get());
                    })
                    .build());

    private ModCreativeTabs() {
    }

    public static void register(IEventBus eventBus) {
        CREATIVE_MODE_TABS.register(eventBus);
    }
}
