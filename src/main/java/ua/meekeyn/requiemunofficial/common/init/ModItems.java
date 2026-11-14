package ua.meekeyn.requiemunofficial.common.init;

import net.minecraft.world.item.Item;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.minecraft.core.registries.BuiltInRegistries;
import net.neoforged.neoforge.registries.DeferredHolder;
import ua.meekeyn.requiemunofficial.RequiemUnofficial;
import ua.meekeyn.requiemunofficial.common.item.IchorVesselItem;
import ua.meekeyn.requiemunofficial.common.item.SoulVesselItem;
import ua.meekeyn.requiemunofficial.common.item.TotemOfSkeletonizationItem;
import ua.meekeyn.requiemunofficial.common.item.RemnantVesselItem;
import ua.meekeyn.requiemunofficial.common.item.FilledSoulVesselItem;
import ua.meekeyn.requiemunofficial.common.item.HumanityEnchantedBookItem;
import ua.meekeyn.requiemunofficial.common.soul.SoulState;
import net.minecraft.ChatFormatting;

public final class ModItems {

    public static final DeferredRegister<Item> ITEMS =
            DeferredRegister.create(BuiltInRegistries.ITEM, RequiemUnofficial.MODID);

    public static final DeferredHolder<Item, Item> HUMANITY_ENCHANTED_BOOK =
            ITEMS.register("humanity_enchanted_book",
                    () -> new HumanityEnchantedBookItem(new Item.Properties().stacksTo(1)));

    public static final DeferredHolder<Item, Item> MORTICIAN_SPAWN_EGG =
            ITEMS.register("mortician_spawn_egg", () ->
                    new net.neoforged.neoforge.common.DeferredSpawnEggItem(ModEntities.MORTICIAN, 0x3A312E, 0xC7B299, new Item.Properties()));

    public static final DeferredHolder<Item, Item> EMPTY_SOUL_VESSEL =
            ITEMS.register("empty_soul_vessel", () -> new SoulVesselItem(new Item.Properties().stacksTo(1)));

    public static final DeferredHolder<Item, Item> FILLED_SOUL_VESSEL =
            ITEMS.register("filled_soul_vessel",
                    () -> new FilledSoulVesselItem(new Item.Properties().stacksTo(1)));

    public static final DeferredHolder<Item, Item> PURE_SOUL_VESSEL =
            ITEMS.register("pure_soul_vessel", () -> new RemnantVesselItem(
                    new Item.Properties().stacksTo(1),
                    SoulState.MORTAL,
                    ChatFormatting.AQUA,
                    "requiemunofficial:remnant_vessel.cure"
            ));

    public static final DeferredHolder<Item, Item> SEALED_REMNANT_VESSEL =
            ITEMS.register("sealed_remnant_vessel", () -> new RemnantVesselItem(
                    new Item.Properties().stacksTo(1),
                    SoulState.REMNANT,
                    ChatFormatting.RED,
                    "requiemunofficial:remnant_vessel.curse"
            ));

    public static final DeferredHolder<Item, Item> SHATTERED_SOUL_VESSEL =
            ITEMS.register("shattered_soul_vessel", () -> new SoulVesselItem(new Item.Properties().stacksTo(1)));

    public static final DeferredHolder<Item, Item> CREATIVE_SOUL_VESSEL =
            ITEMS.register("creative_soul_vessel", () -> new SoulVesselItem(new Item.Properties().stacksTo(1)));

    public static final DeferredHolder<Item, Item> TOTEM_OF_SKELETONIZATION =
            ITEMS.register("totem_of_skeletonization",
                    () -> new TotemOfSkeletonizationItem(new Item.Properties().stacksTo(1)));

    public static final DeferredHolder<Item, Item> ICHOR_VESSEL_ATTRITION =
            ITEMS.register("ichor_vessel_attrition",
                    () -> new IchorVesselItem(new Item.Properties().stacksTo(1), "attrition"));

    public static final DeferredHolder<Item, Item> ICHOR_VESSEL_EMANCIPATION =
            ITEMS.register("ichor_vessel_emancipation",
                    () -> new IchorVesselItem(new Item.Properties().stacksTo(1), "emancipation"));

    public static final DeferredHolder<Item, Item> ICHOR_VESSEL_PENANCE =
            ITEMS.register("ichor_vessel_penance",
                    () -> new IchorVesselItem(new Item.Properties().stacksTo(1), "penance"));

    public static final DeferredHolder<Item, Item> ICHOR_VESSEL_RECLAMATION =
            ITEMS.register("ichor_vessel_reclamation",
                    () -> new IchorVesselItem(new Item.Properties().stacksTo(1), "reclamation"));

    private ModItems() {
    }

    public static void register(IEventBus eventBus) {
        ITEMS.register(eventBus);
    }
}
