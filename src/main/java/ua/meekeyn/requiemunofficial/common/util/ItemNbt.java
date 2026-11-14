package ua.meekeyn.requiemunofficial.common.util;

import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import org.jetbrains.annotations.Nullable;

import java.util.function.Consumer;

/** Заміна ItemStack#getTag/getOrCreateTag з 1.20.1: зберігає дані в компоненті CUSTOM_DATA. */
public final class ItemNbt {

    private ItemNbt() {
    }

    /** Доступ до реєстрів для (де)серіалізації ItemStack поза контекстом світу. */
    public static net.minecraft.core.HolderLookup.Provider access() {
        var server = net.neoforged.neoforge.server.ServerLifecycleHooks.getCurrentServer();
        net.minecraft.core.HolderLookup.Provider access = server != null ? server.registryAccess()
                : ua.meekeyn.requiemunofficial.common.init.ModEnchantments.clientAccess.get();
        if (access == null) {
            throw new IllegalStateException("Registry access is not available");
        }
        return access;
    }

    public static ItemStack read(CompoundTag tag) {
        return ItemStack.parseOptional(access(), tag);
    }

    public static CompoundTag write(ItemStack stack) {
        net.minecraft.nbt.Tag saved = stack.saveOptional(access());
        return saved instanceof CompoundTag compound ? compound : new CompoundTag();
    }

    /** Копія тегу або null, якщо даних немає. Зміни копії на предмет не впливають. */
    @Nullable
    public static CompoundTag getTag(ItemStack stack) {
        CustomData data = stack.get(DataComponents.CUSTOM_DATA);
        return data == null ? null : data.copyTag();
    }

    public static boolean hasTag(ItemStack stack) {
        return stack.has(DataComponents.CUSTOM_DATA);
    }

    @Nullable
    public static CompoundTag getTagElement(ItemStack stack, String key) {
        CompoundTag tag = getTag(stack);
        return tag != null && tag.contains(key, 10) ? tag.getCompound(key) : null;
    }

    public static void edit(ItemStack stack, Consumer<CompoundTag> editor) {
        CustomData.update(DataComponents.CUSTOM_DATA, stack, editor);
    }

    public static void setTag(ItemStack stack, @Nullable CompoundTag tag) {
        if (tag == null || tag.isEmpty()) {
            stack.remove(DataComponents.CUSTOM_DATA);
        } else {
            stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
        }
    }

    public static void removeKey(ItemStack stack, String key) {
        if (hasTag(stack)) {
            edit(stack, tag -> tag.remove(key));
        }
    }
}
