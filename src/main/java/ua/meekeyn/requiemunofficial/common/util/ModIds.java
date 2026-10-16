package ua.meekeyn.requiemunofficial.common.util;

import net.minecraft.resources.ResourceLocation;
import ua.meekeyn.requiemunofficial.RequiemUnofficial;

public final class ModIds {

    private ModIds() {
    }

    public static ResourceLocation id(String path) {
        // Створює ідентифікатор ресурсу мода
        return ResourceLocation.fromNamespaceAndPath(RequiemUnofficial.MODID, path);
    }
}