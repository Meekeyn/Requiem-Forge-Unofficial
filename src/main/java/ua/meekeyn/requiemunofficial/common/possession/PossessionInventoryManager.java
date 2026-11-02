package ua.meekeyn.requiemunofficial.common.possession;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.entity.living.LivingDropsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import ua.meekeyn.requiemunofficial.RequiemUnofficial;
import ua.meekeyn.requiemunofficial.common.init.ModGameRules;
import ua.meekeyn.requiemunofficial.common.soul.SoulManager;
import ua.meekeyn.requiemunofficial.common.util.ModTags;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.WeakHashMap;

@Mod.EventBusSubscriber(modid = RequiemUnofficial.MODID)
public final class PossessionInventoryManager {

    private static final String STORED_INVENTORY = "RequiemPossessionInventory";
    private static final String LAST_SYNCED_EQUIPMENT = "RequiemLastSyncedEquipment";
    private static final String VISUAL_MIRROR_SLOTS = "RequiemVisualMirrorSlots";
    /** Слоти 9..35 основного інвентаря гравця (вузол MAIN_INVENTORY beta.16). */
    private static final int MAIN_INVENTORY_SLOTS = 27;
    private static final int DEATH_DROP_PICKUP_DELAY = 40;
    private static final Map<ItemStack, String> VISUAL_MIRRORS = new WeakHashMap<>();

    private PossessionInventoryManager() {
    }

    /**
     * Ванільні перетворення (зомбі -> утопленик, скелет -> зимогор тощо) не копіюють ForgeData,
     * тому збережений інвентар порожнього тіла переносимо на нову сутність. Аналог beta.16
     * PossessedDataBase.onMobConverted.
     */
    @SubscribeEvent
    public static void onMobConverted(net.minecraftforge.event.entity.living.LivingConversionEvent.Post event) {
        if (!event.getEntity().level().isClientSide()) {
            copyStoredInventory(event.getEntity(), event.getOutcome());
        }
    }

    /**
     * Без HOTBAR тіло тримає предмет лише в слоті 0 (MAINHAND_SLOT beta.16), тому вибраний
     * слот хотбара примусово скидається на 0 з обох боків.
     */
    @SubscribeEvent
    public static void onPlayerTick(net.minecraftforge.event.TickEvent.PlayerTickEvent event) {
        if (event.phase != net.minecraftforge.event.TickEvent.Phase.END) {
            return;
        }
        Player player = event.player;
        LivingEntity host = getLimitingHost(player);
        if (host == null) {
            return;
        }
        if (player.getInventory().selected != 0 && !canCarryHotbar(host)) {
            player.getInventory().selected = 0;
            if (player instanceof net.minecraft.server.level.ServerPlayer serverPlayer && serverPlayer.connection != null) {
                serverPlayer.connection.send(new net.minecraft.network.protocol.game.ClientboundSetCarriedItemPacket(0));
            }
        }
        if (player instanceof net.minecraft.server.level.ServerPlayer serverPlayer) {
            relocateInaccessibleItems(serverPlayer, host);
        }
    }

    /**
     * Єдина перевірка для всіх тіл: предмет у руці (вибраний слот) або в другій руці, що лежить у слоті,
     * якого тіло не має, переноситься в доступний слот, щоб рука й інвентар завжди показували одне й те саме.
     * Решта заблокованих слотів лишається за душею, тож нічого не випадає й не губиться.
     */
    public static void relocateInaccessibleItems(net.minecraft.server.level.ServerPlayer player, LivingEntity host) {
        Inventory inventory = player.getInventory();
        boolean changed = false;
        int selected = inventory.selected;
        ItemStack held = inventory.items.get(selected);
        if (!held.isEmpty() && !isSlotAccessible(host, selected)) {
            changed |= moveToAccessibleSlot(inventory, host, held, () -> inventory.items.set(selected, ItemStack.EMPTY));
        }
        ItemStack offhand = inventory.offhand.get(0);
        if (!offhand.isEmpty() && !isSlotAccessible(host, Inventory.SLOT_OFFHAND)) {
            changed |= moveToAccessibleSlot(inventory, host, offhand, () -> inventory.offhand.set(0, ItemStack.EMPTY));
        }
        if (changed) {
            player.containerMenu.broadcastChanges();
            player.inventoryMenu.broadcastChanges();
        }
    }

    private static boolean moveToAccessibleSlot(Inventory inventory, LivingEntity host, ItemStack stack, Runnable clearSource) {
        for (int slot = 0; slot < inventory.items.size(); slot++) {
            if (!isSlotAccessible(host, slot) || slot == inventory.selected && inventory.items.get(slot) == stack) {
                continue;
            }
            ItemStack existing = inventory.items.get(slot);
            if (existing.isEmpty()) {
                clearSource.run();
                inventory.items.set(slot, stack);
                return true;
            }
            if (ItemStack.isSameItemSameTags(existing, stack)
                    && existing.getCount() + stack.getCount() <= existing.getMaxStackSize()) {
                clearSource.run();
                existing.grow(stack.getCount());
                return true;
            }
        }
        return false;
    }

    /** Переносить збережений інвентар тіла з {@code from} на {@code to} і прибирає його з {@code from}. */
    public static void copyStoredInventory(LivingEntity from, LivingEntity to) {
        CompoundTag source = from.getPersistentData();
        if (to == null || to == from || !source.contains(STORED_INVENTORY, Tag.TAG_LIST)) {
            return;
        }
        to.getPersistentData().put(STORED_INVENTORY, source.getList(STORED_INVENTORY, Tag.TAG_COMPOUND).copy());
        source.remove(STORED_INVENTORY);
    }

    /**
     * Drops stored possession inventory when a non-possessed host dies.
     * This handles the case where the player left the body (items stored in host NBT)
     * and then the host was killed — without this, the stored items would vanish.
     * Uses LivingDropsEvent so drops appear in the event list for other mods to observe.
     */
    @SubscribeEvent
    public static void onHostDeathDrop(LivingDropsEvent event) {
        if (event.getEntity().level().isClientSide()) {
            return;
        }
        LivingEntity host = event.getEntity();
        // Skip if host is still possessed (releaseFromHostDeath handles inventory)
        if (host.getPersistentData().getBoolean("RequiemPossessed")) {
            return;
        }
        CompoundTag data = host.getPersistentData();
        if (!data.contains(STORED_INVENTORY, Tag.TAG_LIST)) {
            return;
        }
        // Add stored items to the event's drop list so they drop on death.
        // Items are added directly (not subject to looting/drop chances).
        ListTag stored = data.getList(STORED_INVENTORY, Tag.TAG_COMPOUND);
        for (int i = 0; i < stored.size(); i++) {
            CompoundTag entry = stored.getCompound(i);
            ItemStack stack = ItemStack.of(entry.getCompound("Stack"));
            if (!stack.isEmpty()) {
                net.minecraft.world.entity.item.ItemEntity itemEntity =
                        new net.minecraft.world.entity.item.ItemEntity(
                                host.level(), host.getX(), host.getY() + 0.0625D, host.getZ(), stack);
                itemEntity.setPickUpDelay(40);
                event.getDrops().add(itemEntity);
            }
        }
        // Clear the stored data so nothing drops twice
        data.remove(STORED_INVENTORY);
    }

    public static void onPossessionStarted(ServerPlayer player, LivingEntity host) {
        if (canUseEquipmentSlot(host, EquipmentSlot.MAINHAND)
                || canUseEquipmentSlot(host, EquipmentSlot.OFFHAND)
                || canUseArmor(host)) {
            transferEquipment(host, player, host, true);
        }
        if (canCarryInventory(host)) {
            restoreStoredInventory(player, host);
        }
        syncPlayerEquipmentToHost(player, host);
    }

    public static void onPossessionEnded(ServerPlayer player, LivingEntity host) {
        if (!ModGameRules.shouldTransferPossessionInventory(
                player.level().getGameRules(),
                host.isAlive()
        )) {
            return;
        }

        if (canUseEquipmentSlot(host, EquipmentSlot.MAINHAND)
                || canUseEquipmentSlot(host, EquipmentSlot.OFFHAND)
                || canUseArmor(host)) {
            transferEquipment(player, host, host, true);
        }
        if (canCarryInventory(host)) {
            transferMainInventory(player, host);
        }
    }

    public static void syncPlayerEquipmentToHost(ServerPlayer player, LivingEntity host) {
        for (EquipmentSlot slot : EquipmentSlot.values()) {
            if (!canUseEquipmentSlot(host, slot)) {
                continue;
            }
            ItemStack playerStack = player.getItemBySlot(slot);
            ItemStack hostStack = host.getItemBySlot(slot);
            ItemStack lastSynced = getLastSyncedStack(host, slot);
            if (ItemStack.matches(playerStack, lastSynced)) {
                if (isCurrentMirror(host, slot, hostStack)) {
                    if (isArmorDamageOnlyChange(slot, hostStack, lastSynced)) {
                        setEquipment(player, slot, hostStack.copy());
                        rememberSyncedStack(host, slot, hostStack);
                    } else if (!ItemStack.matches(hostStack, lastSynced)) {
                        ItemStack mirror = createVisualMirror(playerStack, host, slot);
                        host.setItemSlot(slot, mirror);
                        rememberMirror(host, slot, mirror);
                    }
                } else {
                    setEquipment(player, slot, hostStack.copy());
                    rememberSyncedStack(host, slot, hostStack);
                }
                continue;
            }
            ItemStack mirror = createVisualMirror(playerStack, host, slot);
            host.setItemSlot(slot, mirror);
            rememberMirror(host, slot, mirror);
            rememberSyncedStack(host, slot, playerStack);
            if (host instanceof Mob mob && !playerStack.isEmpty()) {
                mob.setDropChance(slot, 2.0F);
            }
        }
    }

    public static boolean canAccessPlayerInventorySlot(Player player, int slot) {
        UUID possessedId = SoulManager.getPossessedEntityId(player);
        if (possessedId == null || player.isCreative() || player.isSpectator()) {
            return true;
        }

        LivingEntity host = findPossessedHost(player, possessedId);
        if (host == null) {
            return true;
        }

        return isSlotAccessible(host, slot);
    }

    /**
     * Розкладка слотів як у InventoryLimiter beta.16: слот 0 — рука тіла (HANDS),
     * 1..8 — HOTBAR, 9..35 — MAIN_INVENTORY, 36..39 — ARMOR, 40 — друга рука (HANDS).
     */
    public static boolean isSlotAccessible(LivingEntity host, int slot) {
        if (slot == 0) {
            return canUseItems(host) || canCarryHotbar(host);
        }
        if (slot > 0 && slot < 9) {
            return canCarryHotbar(host);
        }
        if (slot >= 9 && slot < 36) {
            return canCarryInventory(host);
        }
        if (slot >= 36 && slot < 40) {
            return canUseArmor(host);
        }
        if (slot == 40) {
            return canUseEquipmentSlot(host, EquipmentSlot.OFFHAND);
        }
        return true;
    }

    /** Повертає тіло, якщо гравець зараз вселений і на нього діють обмеження інвентаря. */
    @org.jetbrains.annotations.Nullable
    public static LivingEntity getLimitingHost(Player player) {
        UUID possessedId = SoulManager.getPossessedEntityId(player);
        if (possessedId == null || player.isCreative() || player.isSpectator()) {
            return null;
        }
        return findPossessedHost(player, possessedId);
    }

    public static InventoryShape getInventoryShape(Player player) {
        UUID possessedId = SoulManager.getPossessedEntityId(player);
        if (possessedId == null || player.isCreative() || player.isSpectator()) {
            return InventoryShape.NORMAL;
        }

        LivingEntity host = findPossessedHost(player, possessedId);
        return host == null ? InventoryShape.NORMAL : getInventoryShape(host);
    }

    public static InventoryShape getInventoryShape(LivingEntity host) {
        boolean itemUser = canUseItems(host);
        boolean hotbarCarrier = canCarryHotbar(host);
        boolean inventoryCarrier = canCarryInventory(host);
        boolean armorUser = canUseArmor(host);

        if (!itemUser && !hotbarCarrier && !inventoryCarrier && !armorUser) {
            return InventoryShape.ALT_LARGE;
        }
        if (!inventoryCarrier) {
            return InventoryShape.ALT;
        }
        return InventoryShape.ALT_SMALL;
    }

    private static void transferEquipment(LivingEntity source,
                                          LivingEntity destination,
                                          LivingEntity capabilityHost,
                                          boolean includeArmor) {
        for (EquipmentSlot slot : EquipmentSlot.values()) {
            if (!includeArmor && slot.getType() == EquipmentSlot.Type.ARMOR) {
                continue;
            }
            ItemStack sourceStack = source.getItemBySlot(slot);
            if (sourceStack.isEmpty()) {
                continue;
            }
            if (!canUseEquipmentSlot(capabilityHost, slot)) {
                continue;
            }

            ItemStack destinationStack = destination.getItemBySlot(slot);
            if (isCurrentMirror(source, slot, sourceStack)) {
                setEquipment(source, slot, ItemStack.EMPTY);
                continue;
            }
            if (isCurrentMirror(destination, slot, destinationStack)) {
                setEquipment(destination, slot, ItemStack.EMPTY);
                destinationStack = ItemStack.EMPTY;
            }
            if (destinationStack == sourceStack) {
                setEquipment(source, slot, ItemStack.EMPTY);
                continue;
            }

            if (destinationStack.isEmpty()) {
                setEquipment(destination, slot, sourceStack.copy());
                if (destination instanceof Mob mob) {
                    mob.setDropChance(slot, 2.0F);
                }
            } else {
                destination.spawnAtLocation(sourceStack.copy());
            }
            setEquipment(source, slot, ItemStack.EMPTY);
        }
    }

    public static boolean canUseEquipmentSlot(LivingEntity entity, EquipmentSlot slot) {
        Loadout loadout = getLoadout(entity);
        return switch (slot) {
            case MAINHAND -> loadout.mainHand();
            case OFFHAND -> loadout.offHand();
            default -> loadout.armor();
        };
    }

    /** Чи має тіло хоча б одну руку (основну чи другу). */
    public static boolean canUseItems(LivingEntity entity) {
        Loadout loadout = getLoadout(entity);
        return loadout.mainHand() || loadout.offHand();
    }

    public static boolean canCarryInventory(LivingEntity entity) {
        return getInventoryCapacity(entity) > 0;
    }

    /**
     * Кількість слотів основного інвентаря гравця (індекси {@code 0..capacity-1}), які тіло
     * фізично носить. Слоти за межами місткості лишаються за душею і заблоковані.
     */
    public static int getInventoryCapacity(LivingEntity entity) {
        return getLoadout(entity).inventorySlots();
    }

    public static boolean canCarryHotbar(LivingEntity entity) {
        return entity.getType().is(ModTags.HOTBAR_CARRIERS);
    }

    public static boolean canCraft(LivingEntity entity) {
        return entity.getType().is(ModTags.SUPERCRAFTERS);
    }

    public static boolean canUseArmor(LivingEntity entity) {
        return getLoadout(entity).armor();
    }

    /**
     * Слоти конкретного тіла: руки окремо, броня та місткість інвентаря. Для ванільних мобів
     * береться вбудована таблиця (повторює те, що моб реально носить у ванілі), для решти
     * (Alex's Mobs тощо) — стара логіка тегів {@code inventory/*}. Тег {@code armor_banned}
     * завжди має пріоритет.
     */
    public static Loadout getLoadout(LivingEntity entity) {
        EntityType<?> type = entity.getType();
        // beta.16 VanillaRequiemPlugin#canUseItems / canWearArmor / refreshInventoryLocks.
        boolean hands = type.is(ModTags.ITEM_USERS) || entity instanceof Mob mob && mob.canPickUpLoot();
        boolean armor = !type.is(ModTags.ARMOR_BANNED)
                && (type.is(ModTags.ARMOR_USERS)
                || !entity.getItemBySlot(EquipmentSlot.HEAD).isEmpty()
                || entity instanceof Mob mob && mob.canTakeItem(new ItemStack(net.minecraft.world.item.Items.LEATHER_HELMET)));
        return new Loadout(hands, hands, armor, type.is(ModTags.INVENTORY_CARRIERS) ? MAIN_INVENTORY_SLOTS : 0);
    }

    /**
     * Clears all visual equipment mirrors from a host entity.
     * Used when the host dies with a player inside, so mirrored equipment
     * (which has dropChance=2.0) doesn't drop as duplicate items.
     */
    public static void clearVisualMirrors(LivingEntity host) {
        for (EquipmentSlot slot : EquipmentSlot.values()) {
            // Спершу прибираємо сам предмет-дзеркало (dropChance 2.0), інакше він випаде поряд з оригіналом у гравця.
            if (isCurrentMirror(host, slot, host.getItemBySlot(slot))) {
                host.setItemSlot(slot, ItemStack.EMPTY);
            }
            clearMirror(host, slot);
        }
    }

    /**
     * Тіло загинуло з душею всередині: те, що тіло несло (слоти в межах його місткості,
     * предмет у головній руці, броня й друга рука, якщо тіло їх має), випадає на місці смерті. Кожен слот спершу очищається, і лише потім
     * спавниться предмет, тож дублювання неможливе. Затримка підбору не дає душі, яку
     * телепортують на те саме місце, миттєво підібрати все назад. Ванільний keepInventory
     * тут не діє — керує лише requiem:possessionKeepInventory (перевіряє викликач).
     * Залишки {@code RequiemPossessionInventory} на тілі (якщо є) також випадають тут.
     */
    public static void dropSoulInventoryAtDeath(ServerPlayer player, LivingEntity host, Vec3 deathPos) {
        if (!(player.level() instanceof ServerLevel level)) {
            return;
        }
        ServerLevel dropLevel = host != null && host.level() instanceof ServerLevel hostLevel ? hostLevel : level;
        Inventory inventory = player.getInventory();
        // Випадає лише те, що тіло справді несло (за його loadout); решта лишається в душі.
        for (int slot = 0; slot < inventory.items.size(); slot++) {
            if (host != null && isSlotAccessible(host, slot)) {
                dropDeathSlot(inventory.items, slot, dropLevel, deathPos);
            }
        }
        if (host != null && canUseArmor(host)) {
            for (int slot = 0; slot < inventory.armor.size(); slot++) {
                dropDeathSlot(inventory.armor, slot, dropLevel, deathPos);
            }
        }
        if (host != null && canUseEquipmentSlot(host, EquipmentSlot.OFFHAND)) {
            for (int slot = 0; slot < inventory.offhand.size(); slot++) {
                dropDeathSlot(inventory.offhand, slot, dropLevel, deathPos);
            }
        }
        if (host != null && host.getPersistentData().contains(STORED_INVENTORY, Tag.TAG_LIST)) {
            ListTag stored = host.getPersistentData().getList(STORED_INVENTORY, Tag.TAG_COMPOUND);
            host.getPersistentData().remove(STORED_INVENTORY);
            for (int i = 0; i < stored.size(); i++) {
                ItemStack stack = ItemStack.of(stored.getCompound(i).getCompound("Stack"));
                if (!stack.isEmpty()) {
                    spawnDeathDrop(dropLevel, deathPos, stack);
                }
            }
        }
        inventory.setChanged();
    }

    private static void dropDeathSlot(List<ItemStack> compartment, int slot, ServerLevel level, Vec3 pos) {
        ItemStack stack = compartment.get(slot);
        if (stack.isEmpty()) {
            return;
        }
        compartment.set(slot, ItemStack.EMPTY);
        spawnDeathDrop(level, pos, stack);
    }

    private static void spawnDeathDrop(ServerLevel level, Vec3 pos, ItemStack stack) {
        ItemEntity itemEntity = new ItemEntity(level, pos.x, pos.y + 0.0625D, pos.z, stack);
        itemEntity.setPickUpDelay(DEATH_DROP_PICKUP_DELAY);
        level.addFreshEntity(itemEntity);
    }

    private static LivingEntity findPossessedHost(Player player, UUID hostId) {
        if (player.level() == null) {
            return null;
        }
        Entity entity = player.level().getServer() == null
                ? findClientSideEntity(player, hostId)
                : player.level().getServer().getLevel(player.level().dimension()).getEntity(hostId);
        return entity instanceof LivingEntity living && living.isAlive() ? living : null;
    }

    private static Entity findClientSideEntity(Player player, UUID hostId) {
        for (Entity entity : player.level().getEntities(
                player,
                player.getBoundingBox().inflate(128.0D),
                entity -> hostId.equals(entity.getUUID())
        )) {
            return entity;
        }
        return null;
    }

    private static void setEquipment(LivingEntity entity, EquipmentSlot slot, ItemStack stack) {
        if (!(entity instanceof Player player)) {
            entity.setItemSlot(slot, stack);
            if (stack.isEmpty()) {
                clearMirror(entity, slot);
            }
            rememberSyncedStack(entity, slot, stack);
            return;
        }

        switch (slot.getType()) {
            case HAND -> {
                if (slot == EquipmentSlot.MAINHAND) {
                    player.getInventory().items.set(player.getInventory().selected, stack);
                } else {
                    player.getInventory().offhand.set(0, stack);
                }
            }
            case ARMOR -> player.getInventory().armor.set(slot.getIndex(), stack);
        }
    }

    private static ItemStack getLastSyncedStack(LivingEntity host, EquipmentSlot slot) {
        CompoundTag root = host.getPersistentData().getCompound(LAST_SYNCED_EQUIPMENT);
        if (!root.contains(slot.getName(), Tag.TAG_COMPOUND)) {
            return ItemStack.EMPTY;
        }
        return ItemStack.of(root.getCompound(slot.getName()));
    }

    private static void rememberSyncedStack(LivingEntity host, EquipmentSlot slot, ItemStack stack) {
        CompoundTag root = host.getPersistentData().getCompound(LAST_SYNCED_EQUIPMENT);
        if (stack.isEmpty()) {
            root.remove(slot.getName());
        } else {
            root.put(slot.getName(), stack.save(new CompoundTag()));
        }
        if (root.isEmpty()) {
            host.getPersistentData().remove(LAST_SYNCED_EQUIPMENT);
        } else {
            host.getPersistentData().put(LAST_SYNCED_EQUIPMENT, root);
        }
    }

    private static ItemStack createVisualMirror(ItemStack source, LivingEntity host, EquipmentSlot slot) {
        if (source.isEmpty()) {
            return ItemStack.EMPTY;
        }
        ItemStack mirror = source.copy();
        return mirror;
    }

    private static boolean isCurrentMirror(LivingEntity host, EquipmentSlot slot, ItemStack stack) {
        return mirrorKey(host, slot).equals(VISUAL_MIRRORS.get(stack)) || hasVisualMirrorTag(host, slot, stack);
    }

    private static void rememberMirror(LivingEntity host, EquipmentSlot slot, ItemStack stack) {
        if (!stack.isEmpty()) {
            VISUAL_MIRRORS.put(stack, mirrorKey(host, slot));
            CompoundTag slots = host.getPersistentData().getCompound(VISUAL_MIRROR_SLOTS);
            slots.putBoolean(slot.getName(), true);
            host.getPersistentData().put(VISUAL_MIRROR_SLOTS, slots);
        }
    }

    private static String mirrorKey(LivingEntity host, EquipmentSlot slot) {
        return host.getUUID() + ":" + slot.getName();
    }

    private static void clearMirror(LivingEntity host, EquipmentSlot slot) {
        CompoundTag slots = host.getPersistentData().getCompound(VISUAL_MIRROR_SLOTS);
        slots.remove(slot.getName());
        if (slots.isEmpty()) {
            host.getPersistentData().remove(VISUAL_MIRROR_SLOTS);
        } else {
            host.getPersistentData().put(VISUAL_MIRROR_SLOTS, slots);
        }
    }

    private static boolean hasVisualMirrorTag(LivingEntity host, EquipmentSlot slot, ItemStack stack) {
        return !stack.isEmpty()
                && host.getPersistentData().getCompound(VISUAL_MIRROR_SLOTS).getBoolean(slot.getName());
    }

    private static boolean isArmorDamageOnlyChange(EquipmentSlot slot, ItemStack current, ItemStack previous) {
        if (slot.getType() != EquipmentSlot.Type.ARMOR
                || current.isEmpty()
                || previous.isEmpty()
                || current.getCount() != previous.getCount()
                || !current.is(previous.getItem())) {
            return false;
        }
        ItemStack currentComparable = current.copy();
        ItemStack previousComparable = previous.copy();
        currentComparable.setDamageValue(0);
        previousComparable.setDamageValue(0);
        return ItemStack.matches(currentComparable, previousComparable)
                && current.getDamageValue() != previous.getDamageValue();
    }

    private static void transferMainInventory(ServerPlayer player, LivingEntity host) {
        ListTag stored = new ListTag();
        // Лише основний інвентар (9..35), який тіло носить, переходить до нього; решта лишається за душею.
        int size = player.getInventory().items.size();
        for (int slot = 9; slot < size; slot++) {
            if (!isSlotAccessible(host, slot)) {
                continue;
            }
            ItemStack stack = player.getInventory().items.get(slot);
            if (stack.isEmpty()) {
                continue;
            }

            ItemStack moved = stack.copy();
            player.getInventory().items.set(slot, ItemStack.EMPTY);
            if (host.isAlive()) {
                CompoundTag entry = new CompoundTag();
                entry.putByte("Slot", (byte) slot);
                entry.put("Stack", moved.save(new CompoundTag()));
                stored.add(entry);
            } else {
                host.spawnAtLocation(moved);
            }
        }

        if (!stored.isEmpty()) {
            host.getPersistentData().put(STORED_INVENTORY, stored);
        } else {
            host.getPersistentData().remove(STORED_INVENTORY);
        }
    }

    private static void restoreStoredInventory(ServerPlayer player, LivingEntity host) {
        if (!host.getPersistentData().contains(STORED_INVENTORY, Tag.TAG_LIST)) {
            return;
        }

        ListTag stored = host.getPersistentData().getList(STORED_INVENTORY, Tag.TAG_COMPOUND);
        for (Tag rawEntry : stored) {
            CompoundTag entry = (CompoundTag) rawEntry;
            ItemStack stack = ItemStack.of(entry.getCompound("Stack"));
            int slot = Byte.toUnsignedInt(entry.getByte("Slot"));
            if (stack.isEmpty()) {
                continue;
            }
            if (slot < player.getInventory().items.size()
                    && player.getInventory().items.get(slot).isEmpty()) {
                player.getInventory().items.set(slot, stack);
            } else if (!player.getInventory().add(stack)) {
                player.drop(stack, false);
            }
        }
        host.getPersistentData().remove(STORED_INVENTORY);
    }

    /**
     * Слоти тіла: основна/друга рука, броня та кількість слотів основного інвентаря гравця.
     */
    public record Loadout(boolean mainHand, boolean offHand, boolean armor, int inventorySlots) {
        public static final Loadout NONE = new Loadout(false, false, false, 0);
    }

    public enum InventoryShape {
        NORMAL,
        ALT_SMALL,
        ALT,
        ALT_LARGE
    }
}
