package ua.meekeyn.requiemunofficial.common.init;

import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.minecraft.core.registries.BuiltInRegistries;
import net.neoforged.neoforge.registries.DeferredHolder;
import ua.meekeyn.requiemunofficial.RequiemUnofficial;
import ua.meekeyn.requiemunofficial.common.block.ReclamationRunestoneBlock;
import ua.meekeyn.requiemunofficial.common.block.RiftRunestoneBlock;
import ua.meekeyn.requiemunofficial.common.block.RunestoneBlock;
import ua.meekeyn.requiemunofficial.common.block.TachyliteBlock;

public final class ModBlocks {
    public static final DeferredRegister<Block> BLOCKS =
            DeferredRegister.create(BuiltInRegistries.BLOCK, RequiemUnofficial.MODID);

    public static final DeferredHolder<Block, Block> TACHYLITE =
            BLOCKS.register("tachylite", () -> new TachyliteBlock(
                    BlockBehaviour.Properties.of()
                            .mapColor(MapColor.COLOR_BLACK)
                            .strength(3.0F, 6.0F)
                            .sound(SoundType.STONE)
                            .requiresCorrectToolForDrops()
            ));

    public static final DeferredHolder<Block, Block> TACHYLITE_SLAB =
            BLOCKS.register("tachylite/tachylite_slab", () -> new SlabBlock(tachyliteProperties()));

    public static final DeferredHolder<Block, Block> TACHYLITE_STAIRS =
            BLOCKS.register("tachylite/tachylite_stairs", () -> new StairBlock(
                    TACHYLITE.get().defaultBlockState(),
                    tachyliteProperties()
            ));

    public static final DeferredHolder<Block, Block> CHISELED_TACHYLITE =
            BLOCKS.register("tachylite/chiseled", () -> new Block(tachyliteProperties()));

    public static final DeferredHolder<Block, Block> CHISELED_TACHYLITE_PILLAR =
            BLOCKS.register("tachylite/chiseled_pillar", () -> new RotatedPillarBlock(tachyliteProperties()));

    public static final DeferredHolder<Block, Block> CHISELED_TACHYLITE_SLAB =
            BLOCKS.register("tachylite/chiseled_slab", () -> new SlabBlock(tachyliteProperties()));

    public static final DeferredHolder<Block, Block> CHISELED_TACHYLITE_STAIRS =
            BLOCKS.register("tachylite/chiseled_stairs", () -> new StairBlock(
                    CHISELED_TACHYLITE.get().defaultBlockState(),
                    tachyliteProperties()
            ));

    public static final DeferredHolder<Block, Block> SCRAPED_TACHYLITE =
            BLOCKS.register("tachylite/scraped", () -> new Block(tachyliteProperties()));

    public static final DeferredHolder<Block, Block> DERELICT_TACHYLITE =
            BLOCKS.register("tachylite/derelict", () -> new Block(tachyliteProperties()));

    public static final DeferredHolder<Block, Block> TACHYLITE_RUNESTONE =
            BLOCKS.register("tachylite_runestone", () -> new RunestoneBlock(
                    BlockBehaviour.Properties.of()
                            .mapColor(MapColor.COLOR_BLACK)
                            .strength(3.0F, 6.0F)
                            .sound(SoundType.STONE)
                            .requiresCorrectToolForDrops(),
                    "neutral",
                    0
            ));

    public static final DeferredHolder<Block, Block> RUNIC_TACHYLITE_ATTRITION =
            BLOCKS.register("runic_tachylite_attrition", () -> new RunestoneBlock(
                    BlockBehaviour.Properties.of()
                            .mapColor(MapColor.COLOR_BLACK)
                            .strength(3.0F, 6.0F)
                            .sound(SoundType.STONE)
                            .requiresCorrectToolForDrops(),
                    "attrition",
                    3,
                    ModEffects.ATTRITION
            ));

    public static final DeferredHolder<Block, Block> RUNIC_TACHYLITE_EMANCIPATION =
            BLOCKS.register("runic_tachylite_emancipation", () -> new RunestoneBlock(
                    BlockBehaviour.Properties.of()
                            .mapColor(MapColor.COLOR_BLACK)
                            .strength(3.0F, 6.0F)
                            .sound(SoundType.STONE)
                            .requiresCorrectToolForDrops(),
                    "emancipation",
                    1,
                    ModEffects.EMANCIPATION
            ));

    public static final DeferredHolder<Block, Block> RUNIC_TACHYLITE_PENANCE =
            BLOCKS.register("runic_tachylite_penance", () -> new RunestoneBlock(
                    BlockBehaviour.Properties.of()
                            .mapColor(MapColor.COLOR_BLACK)
                            .strength(3.0F, 6.0F)
                            .sound(SoundType.STONE)
                            .requiresCorrectToolForDrops(),
                    "penance",
                    3,
                    ModEffects.PENANCE
            ));

    public static final DeferredHolder<Block, Block> RUNIC_TACHYLITE_RECLAMATION =
            BLOCKS.register("runic_tachylite_reclamation", () -> new ReclamationRunestoneBlock(
                    BlockBehaviour.Properties.of()
                            .mapColor(MapColor.COLOR_BLACK)
                            .strength(3.0F, 6.0F)
                            .sound(SoundType.STONE)
                            .requiresCorrectToolForDrops(),
                    "reclamation",
                    1,
                    () -> ModEffects.RECLAMATION
            ));

    public static final DeferredHolder<Block, Block> RIFT_RUNE =
            BLOCKS.register("rift_rune", () -> new RiftRunestoneBlock(
                    BlockBehaviour.Properties.of()
                            .mapColor(MapColor.COLOR_BLACK)
                            .strength(3.0F, 6.0F)
                            .sound(SoundType.STONE)
                            .requiresCorrectToolForDrops(),
                    "rift",
                    1
            ));

    private ModBlocks() {
    }

    private static BlockBehaviour.Properties tachyliteProperties() {
        return BlockBehaviour.Properties.of()
                .mapColor(MapColor.COLOR_BLACK)
                .strength(3.0F, 6.0F)
                .sound(SoundType.STONE)
                .requiresCorrectToolForDrops();
    }

    public static void register(IEventBus eventBus) {
        BLOCKS.register(eventBus);
    }

    public static void registerBlockItems() {
        ModItems.ITEMS.register("tachylite", () -> new BlockItem(TACHYLITE.get(), new Item.Properties()));
        ModItems.ITEMS.register("tachylite/tachylite_slab", () -> new BlockItem(TACHYLITE_SLAB.get(), new Item.Properties()));
        ModItems.ITEMS.register("tachylite/tachylite_stairs", () -> new BlockItem(TACHYLITE_STAIRS.get(), new Item.Properties()));
        ModItems.ITEMS.register("tachylite/chiseled", () -> new BlockItem(CHISELED_TACHYLITE.get(), new Item.Properties()));
        ModItems.ITEMS.register("tachylite/chiseled_pillar", () -> new BlockItem(CHISELED_TACHYLITE_PILLAR.get(), new Item.Properties()));
        ModItems.ITEMS.register("tachylite/chiseled_slab", () -> new BlockItem(CHISELED_TACHYLITE_SLAB.get(), new Item.Properties()));
        ModItems.ITEMS.register("tachylite/chiseled_stairs", () -> new BlockItem(CHISELED_TACHYLITE_STAIRS.get(), new Item.Properties()));
        ModItems.ITEMS.register("tachylite/scraped", () -> new BlockItem(SCRAPED_TACHYLITE.get(), new Item.Properties()));
        ModItems.ITEMS.register("tachylite/derelict", () -> new BlockItem(DERELICT_TACHYLITE.get(), new Item.Properties()));
        ModItems.ITEMS.register("tachylite_runestone", () -> new BlockItem(TACHYLITE_RUNESTONE.get(), new Item.Properties()));
        ModItems.ITEMS.register("runic_tachylite_attrition", () -> new BlockItem(RUNIC_TACHYLITE_ATTRITION.get(), new Item.Properties()));
        ModItems.ITEMS.register("runic_tachylite_emancipation", () -> new BlockItem(RUNIC_TACHYLITE_EMANCIPATION.get(), new Item.Properties()));
        ModItems.ITEMS.register("runic_tachylite_penance", () -> new BlockItem(RUNIC_TACHYLITE_PENANCE.get(), new Item.Properties()));
        ModItems.ITEMS.register("runic_tachylite_reclamation", () -> new BlockItem(RUNIC_TACHYLITE_RECLAMATION.get(), new Item.Properties()));
        ModItems.ITEMS.register("rift_rune", () -> new BlockItem(RIFT_RUNE.get(), new Item.Properties()));
    }
}
