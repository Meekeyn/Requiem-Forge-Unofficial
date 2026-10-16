package ua.meekeyn.requiemunofficial.common.item;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.EnchantedBookItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.enchantment.EnchantmentInstance;
import net.minecraft.world.level.Level;
import ua.meekeyn.requiemunofficial.common.init.ModEnchantments;

import javax.annotation.Nullable;
import java.util.List;

public class HumanityEnchantedBookItem extends EnchantedBookItem {

    public HumanityEnchantedBookItem(Properties properties) {
        super(properties);
    }

    @Override
    public ItemStack getDefaultInstance() {
        return create(1);
    }

    public static ItemStack create(int level) {
        ItemStack stack = new ItemStack(ua.meekeyn.requiemunofficial.common.init.ModItems.HUMANITY_ENCHANTED_BOOK.get());
        EnchantedBookItem.addEnchantment(
                stack,
                new EnchantmentInstance(ModEnchantments.HUMANITY.get(), Math.max(1, Math.min(2, level)))
        );
        return stack;
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level,
                                List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, level, tooltip, flag);
        tooltip.add(Component.translatable("tooltip.requiemunofficial.humanity_enchanted_book")
                .withStyle(ChatFormatting.AQUA));
    }
}
