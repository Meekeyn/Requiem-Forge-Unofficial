package ua.meekeyn.requiemunofficial.common.loot;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.storage.loot.predicates.LootItemConditionType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.DeferredHolder;

/**
 * Типи умов лут-таблиць з оригінального Requiem beta.16 (RequiemLootTables#init).
 * Реєструються саме в просторі імен {@code requiem}, щоб старі датапаки Requiem
 * ({@code "condition": "requiem:host"} тощо) працювали без змін.
 */
public final class RequiemLootConditions {
    /** Простір імен оригінального мода — навмисно не MODID порту. */
    public static final String LEGACY_NAMESPACE = "requiem";

    public static final DeferredRegister<LootItemConditionType> LOOT_CONDITION_TYPES =
            DeferredRegister.create(Registries.LOOT_CONDITION_TYPE, LEGACY_NAMESPACE);

    public static final DeferredHolder<LootItemConditionType, LootItemConditionType> RIFT_MORTICIAN = LOOT_CONDITION_TYPES.register(
            "rift_mortician", () -> new LootItemConditionType(RiftMorticianLootCondition.CODEC));
    public static final DeferredHolder<LootItemConditionType, LootItemConditionType> BOUND_SHELL = LOOT_CONDITION_TYPES.register(
            "bound_shell", () -> new LootItemConditionType(BoundShellLootCondition.CODEC));
    public static final DeferredHolder<LootItemConditionType, LootItemConditionType> HOST = LOOT_CONDITION_TYPES.register(
            "host", () -> new LootItemConditionType(
                    HostLootCondition.codec(HostLootCondition.CheckedEntity.HOST)));
    public static final DeferredHolder<LootItemConditionType, LootItemConditionType> POSSESSOR = LOOT_CONDITION_TYPES.register(
            "possessor", () -> new LootItemConditionType(
                    HostLootCondition.codec(HostLootCondition.CheckedEntity.POSSESSOR)));

    private RequiemLootConditions() {
    }

    public static void register(IEventBus modEventBus) {
        LOOT_CONDITION_TYPES.register(modEventBus);
    }
}
