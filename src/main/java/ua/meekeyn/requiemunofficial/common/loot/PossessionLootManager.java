package ua.meekeyn.requiemunofficial.common.loot;

import net.neoforged.fml.common.EventBusSubscriber;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSet;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.neoforged.neoforge.event.entity.living.LivingConversionEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;
import ua.meekeyn.requiemunofficial.RequiemUnofficial;

/**
 * Порт beta.16 {@code LootingPossessedData}: при ПЕРШОМУ вселенні в моба гравець отримує лут
 * з таблиці {@code <namespace сутності>:requiem/possession/<path сутності>}
 * (наприклад {@code minecraft:requiem/possession/skeleton} -> 1-5 стріл).
 * Прапорець "вже був одержимий" зберігається в ForgeData моба і переноситься при перетвореннях
 * (у beta.16 — {@code previously_possessed} у компоненті PossessedData, copyFrom при onMobConverted).
 */
@EventBusSubscriber(modid = RequiemUnofficial.MODID)
public final class PossessionLootManager {
    /** Аналог {@code requiem:possession} (RequiemLootTables.POSSESSION): THIS_ENTITY + ORIGIN. */
    public static final LootContextParamSet POSSESSION = new LootContextParamSet.Builder()
            .required(LootContextParams.THIS_ENTITY)
            .required(LootContextParams.ORIGIN)
            .build();

    public static final String PREVIOUSLY_POSSESSED = "RequiemPreviouslyPossessed";

    private PossessionLootManager() {
    }

    /** Викликається після успішного початку вселення (beta.16 PossessionStateChangeCallback). */
    public static void giftFirstPossessionLoot(ServerPlayer player, LivingEntity host) {
        // У beta.16 PossessedData існує лише для MobEntity.
        if (!(host instanceof Mob) || !(host.level() instanceof ServerLevel level)) {
            return;
        }
        if (host.getPersistentData().getBoolean(PREVIOUSLY_POSSESSED)) {
            return;
        }
        host.getPersistentData().putBoolean(PREVIOUSLY_POSSESSED, true);

        LootTable lootTable = level.getServer().reloadableRegistries().getLootTable(net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.LOOT_TABLE, getLootTableId(host)));
        LootParams params = new LootParams.Builder(level)
                .withParameter(LootContextParams.THIS_ENTITY, host)
                .withParameter(LootContextParams.ORIGIN, host.position())
                .withLuck(player.getLuck())
                .create(POSSESSION);
        // beta.16: player.getInventory()::offerOrDrop. Тут add + drop, щоб не надсилати пакет
        // слоту напряму (у тестових гравців без з'єднання це падало б).
        lootTable.getRandomItems(params, stack -> giveOrDrop(player, stack));
    }

    /** Позначає тіло як уже одержиме (beta.16 onPossessed / copyFrom при заміні тіла). */
    public static void markPreviouslyPossessed(LivingEntity entity) {
        if (entity != null) {
            entity.getPersistentData().putBoolean(PREVIOUSLY_POSSESSED, true);
        }
    }

    public static ResourceLocation getLootTableId(LivingEntity host) {
        ResourceLocation typeId = BuiltInRegistries.ENTITY_TYPE.getKey(host.getType());
        return ResourceLocation.fromNamespaceAndPath(typeId.getNamespace(), "requiem/possession/" + typeId.getPath());
    }

    private static void giveOrDrop(ServerPlayer player, ItemStack stack) {
        if (stack.isEmpty()) {
            return;
        }
        if (!player.getInventory().add(stack) && !stack.isEmpty()) {
            player.drop(stack, false);
        }
    }

    /** Ванільні перетворення не копіюють ForgeData — переносимо прапорець вручну. */
    @SubscribeEvent
    public static void onMobConverted(LivingConversionEvent.Post event) {
        if (!event.getEntity().level().isClientSide()
                && event.getEntity().getPersistentData().getBoolean(PREVIOUSLY_POSSESSED)) {
            markPreviouslyPossessed(event.getOutcome());
        }
    }
}
