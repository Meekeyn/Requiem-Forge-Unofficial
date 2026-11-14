package ua.meekeyn.requiemunofficial.common.init;

import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.enchantment.Enchantment;
import net.neoforged.neoforge.server.ServerLifecycleHooks;
import ua.meekeyn.requiemunofficial.common.util.ModIds;

import java.util.function.Supplier;

/**
 * Зачарування «Humanity» тепер описується датою ({@code data/requiemunofficial/enchantment/humanity.json}),
 * тому в коді є лише ключ і допоміжні методи отримання холдера.
 */
public final class ModEnchantments {

    public static final ResourceKey<Enchantment> HUMANITY = ResourceKey.create(Registries.ENCHANTMENT, ModIds.id("humanity"));

    /** Встановлюється клієнтом, щоб getDefaultInstance() працював і без сервера. */
    public static volatile Supplier<HolderLookup.Provider> clientAccess = () -> null;

    private ModEnchantments() {
    }

    public static Holder<Enchantment> humanity(HolderLookup.Provider access) {
        return access.lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(HUMANITY);
    }

    public static Holder<Enchantment> humanity() {
        var server = ServerLifecycleHooks.getCurrentServer();
        HolderLookup.Provider access = server != null ? server.registryAccess() : clientAccess.get();
        if (access == null) {
            throw new IllegalStateException("Registry access is not available yet");
        }
        return humanity(access);
    }
}
