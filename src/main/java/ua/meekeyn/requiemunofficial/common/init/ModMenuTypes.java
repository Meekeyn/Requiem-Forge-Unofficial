package ua.meekeyn.requiemunofficial.common.init;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.inventory.MenuType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.DeferredHolder;
import ua.meekeyn.requiemunofficial.RequiemUnofficial;
import ua.meekeyn.requiemunofficial.common.menu.RiftMenu;

public final class ModMenuTypes {

    public static final DeferredRegister<MenuType<?>> MENU_TYPES =
            DeferredRegister.create(Registries.MENU, RequiemUnofficial.MODID);

    public static final DeferredHolder<MenuType<?>, MenuType<RiftMenu>> RIFT =
            MENU_TYPES.register("rift", () -> net.neoforged.neoforge.common.extensions.IMenuTypeExtension.create(RiftMenu::new));

    // Kept for compatibility with existing technical menu references.
    public static final DeferredHolder<MenuType<?>, MenuType<DummyMenu>> DUMMY =
            MENU_TYPES.register("dummy",
                    () -> new MenuType<>(DummyMenu::new, FeatureFlags.DEFAULT_FLAGS));

    private ModMenuTypes() {
    }

    public static void register(IEventBus eventBus) {
        MENU_TYPES.register(eventBus);
    }

    public static final class DummyMenu extends net.minecraft.world.inventory.AbstractContainerMenu {

        public DummyMenu(int containerId, net.minecraft.world.entity.player.Inventory inventory) {
            super(DUMMY.get(), containerId);
        }

        @Override
        public boolean stillValid(net.minecraft.world.entity.player.Player player) {
            return true;
        }

        @Override
        public net.minecraft.world.item.ItemStack quickMoveStack(
                net.minecraft.world.entity.player.Player player,
                int index
        ) {
            return net.minecraft.world.item.ItemStack.EMPTY;
        }
    }
}
