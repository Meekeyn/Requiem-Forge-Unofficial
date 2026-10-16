package ua.meekeyn.requiemunofficial.common.possession;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import ua.meekeyn.requiemunofficial.common.init.ModGameRules;
import ua.meekeyn.requiemunofficial.common.soul.SoulManager;
import ua.meekeyn.requiemunofficial.common.util.ModTags;

import java.util.Map;
import java.util.UUID;
import java.util.WeakHashMap;

public final class PossessionInventoryManager {

    private static final String STORED_INVENTORY = "RequiemPossessionInventory";
    private static final String LAST_SYNCED_EQUIPMENT = "RequiemLastSyncedEquipment";
    private static final String VISUAL_MIRROR_SLOTS = "RequiemVisualMirrorSlots";
    private static final int MAIN_INVENTORY_SIZE = 36;
    private static final Map<ItemStack, String> VISUAL_MIRRORS = new WeakHashMap<>();

    private PossessionInventoryManager() {
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

        if (slot >= 0 && slot < 9) {
            return canCarryHotbar(host) || canCarryInventory(host) || canUseItems(host);
        }
        if (slot >= 9 && slot < 36) {
            return canCarryInventory(host);
        }
        if (slot >= 36 && slot < 40) {
            return canUseArmor(host);
        }
        if (slot == 40) {
            return canUseItems(host);
        }
        return true;
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
        return switch (slot.getType()) {
            case HAND -> canUseItems(entity);
            case ARMOR -> canUseArmor(entity);
        };
    }

    public static boolean canUseItems(LivingEntity entity) {
        return entity.getType().is(ModTags.ITEM_USERS)
                || entity instanceof Mob mob && mob.canPickUpLoot();
    }

    public static boolean canCarryInventory(LivingEntity entity) {
        return entity.getType().is(ModTags.INVENTORY_CARRIERS);
    }

    public static boolean canCarryHotbar(LivingEntity entity) {
        return entity.getType().is(ModTags.HOTBAR_CARRIERS);
    }

    public static boolean canCraft(LivingEntity entity) {
        return entity.getType().is(ModTags.SUPERCRAFTERS);
    }

    public static boolean canUseArmor(LivingEntity entity) {
        return !entity.getType().is(ModTags.ARMOR_BANNED)
                && entity.getType().is(ModTags.ARMOR_USERS);
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
        int size = Math.min(MAIN_INVENTORY_SIZE, player.getInventory().items.size());
        for (int slot = 0; slot < size; slot++) {
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

    public enum InventoryShape {
        NORMAL,
        ALT_SMALL,
        ALT,
        ALT_LARGE
    }
}
