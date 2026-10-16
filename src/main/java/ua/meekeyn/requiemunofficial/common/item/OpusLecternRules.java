package ua.meekeyn.requiemunofficial.common.item;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

public final class OpusLecternRules {

    private OpusLecternRules() {
    }

    public static boolean isLecternReadable(ItemStack stack) {
        Item item = stack.getItem();
        return item instanceof OpusDaemoniumItem || item instanceof RemnantVesselItem;
    }
}
