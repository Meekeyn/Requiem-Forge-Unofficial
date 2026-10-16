package ua.meekeyn.requiemunofficial.common.init;

import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import ua.meekeyn.requiemunofficial.RequiemUnofficial;
import ua.meekeyn.requiemunofficial.common.enchantment.HumanityEnchantment;

public final class ModEnchantments {

    public static final DeferredRegister<Enchantment> ENCHANTMENTS =
            DeferredRegister.create(ForgeRegistries.ENCHANTMENTS, RequiemUnofficial.MODID);

    public static final RegistryObject<Enchantment> HUMANITY =
            ENCHANTMENTS.register("humanity", () -> new HumanityEnchantment(
                    Enchantment.Rarity.RARE,
                    EquipmentSlot.MAINHAND
            ));

    private ModEnchantments() {
    }

    public static void register(IEventBus eventBus) {
        ENCHANTMENTS.register(eventBus);
    }
}
