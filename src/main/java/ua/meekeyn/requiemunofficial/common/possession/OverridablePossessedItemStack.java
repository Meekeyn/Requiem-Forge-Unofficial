package ua.meekeyn.requiemunofficial.common.possession;

import net.minecraft.world.item.ItemStack;

public interface OverridablePossessedItemStack {
    static OverridablePossessedItemStack get(ItemStack stack) {
        return (OverridablePossessedItemStack) (Object) stack;
    }

    static void clearUseDuration(ItemStack stack) {
        get(stack).requiemunofficial$clearPossessedUseDuration();
    }

    static void overrideUseDuration(ItemStack stack, int ticks) {
        get(stack).requiemunofficial$overridePossessedUseDuration(ticks);
    }

    void requiemunofficial$clearPossessedUseDuration();

    void requiemunofficial$overridePossessedUseDuration(int ticks);
}
