package ua.meekeyn.requiemunofficial.common.loot;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.storage.loot.predicates.LootItemConditionType;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

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

    public static final RegistryObject<LootItemConditionType> RIFT_MORTICIAN = LOOT_CONDITION_TYPES.register(
            "rift_mortician", () -> new LootItemConditionType(new RiftMorticianLootCondition.Serializer()));
    public static final RegistryObject<LootItemConditionType> BOUND_SHELL = LOOT_CONDITION_TYPES.register(
            "bound_shell", () -> new LootItemConditionType(new BoundShellLootCondition.Serializer()));
    public static final RegistryObject<LootItemConditionType> HOST = LOOT_CONDITION_TYPES.register(
            "host", () -> new LootItemConditionType(
                    new HostLootCondition.Serializer(HostLootCondition.CheckedEntity.HOST)));
    public static final RegistryObject<LootItemConditionType> POSSESSOR = LOOT_CONDITION_TYPES.register(
            "possessor", () -> new LootItemConditionType(
                    new HostLootCondition.Serializer(HostLootCondition.CheckedEntity.POSSESSOR)));

    private RequiemLootConditions() {
    }

    public static void register(IEventBus modEventBus) {
        LOOT_CONDITION_TYPES.register(modEventBus);
    }
}
