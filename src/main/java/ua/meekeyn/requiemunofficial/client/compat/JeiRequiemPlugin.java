package ua.meekeyn.requiemunofficial.client.compat;

import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.registration.IRecipeRegistration;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import ua.meekeyn.requiemunofficial.RequiemUnofficial;
import ua.meekeyn.requiemunofficial.common.init.ModBlocks;
import ua.meekeyn.requiemunofficial.common.init.ModItems;
import ua.meekeyn.requiemunofficial.common.util.ModIds;

import java.util.List;

@JeiPlugin
public final class JeiRequiemPlugin implements IModPlugin {

    private static final ResourceLocation UID = ModIds.id("jei");

    @Override
    public ResourceLocation getPluginUid() {
        return UID;
    }

    @Override
    public void registerRecipes(IRecipeRegistration registration) {
        addInfo(registration, new ItemStack(ModItems.OPUS_DAEMONIUM.get()), "opus_daemonium");
        addInfo(registration, new ItemStack(ModItems.HUMANITY_ENCHANTED_BOOK.get()), "humanity");
        addInfo(registration, new ItemStack(ModItems.EMPTY_SOUL_VESSEL.get()), "empty_soul_vessel");
        addInfo(registration, new ItemStack(ModItems.FILLED_SOUL_VESSEL.get()), "filled_soul_vessel");
        addInfo(registration, new ItemStack(ModItems.SEALED_REMNANT_VESSEL.get()), "sealed_remnant_vessel");
        addInfo(registration, new ItemStack(ModItems.PURE_SOUL_VESSEL.get()), "pure_soul_vessel");
        addInfo(registration, new ItemStack(ModItems.TOTEM_OF_SKELETONIZATION.get()), "totem_of_skeletonization");

        registration.addItemStackInfo(
                List.of(
                        new ItemStack(ModBlocks.TACHYLITE_RUNESTONE.get()),
                        new ItemStack(ModBlocks.RUNIC_TACHYLITE_ATTRITION.get()),
                        new ItemStack(ModBlocks.RUNIC_TACHYLITE_EMANCIPATION.get()),
                        new ItemStack(ModBlocks.RUNIC_TACHYLITE_PENANCE.get()),
                        new ItemStack(ModBlocks.RUNIC_TACHYLITE_RECLAMATION.get()),
                        new ItemStack(ModBlocks.RIFT_RUNE.get())
                ),
                Component.translatable("jei." + RequiemUnofficial.MODID + ".info.runestones")
        );
    }

    private static void addInfo(IRecipeRegistration registration, ItemStack stack, String key) {
        registration.addItemStackInfo(
                stack,
                Component.translatable("jei." + RequiemUnofficial.MODID + ".info." + key)
        );
    }
}
