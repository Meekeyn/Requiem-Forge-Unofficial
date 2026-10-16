package ua.meekeyn.requiemunofficial.common.entity;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import ua.meekeyn.requiemunofficial.common.soul.SoulLifecycleManager;

import java.util.Optional;
import java.util.UUID;

public class PlayerShellEntity extends PathfinderMob {

    private static final EntityDataAccessor<Optional<UUID>> OWNER_ID =
            SynchedEntityData.defineId(PlayerShellEntity.class, EntityDataSerializers.OPTIONAL_UUID);
    private static final String BODY_INVENTORY = "RequiemBodyInventory";
    private static final String BODY_INVENTORY_CAPTURED = "RequiemBodyInventoryCaptured";
    private static final String BODY_SELECTED_SLOT = "RequiemBodySelectedSlot";

    private UUID ownerId;
    private ListTag bodyInventory = new ListTag();
    private boolean bodyInventoryCaptured;
    private int bodySelectedSlot;

    public PlayerShellEntity(EntityType<? extends PathfinderMob> entityType, Level level) {
        super(entityType, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return PathfinderMob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 20.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.0D)
                .add(Attributes.FOLLOW_RANGE, 16.0D);
    }

    public UUID getOwnerId() {
        return this.entityData.get(OWNER_ID).orElse(ownerId);
    }

    public void setOwner(UUID ownerId, Component ownerName) {
        this.ownerId = ownerId;
        this.entityData.set(OWNER_ID, Optional.of(ownerId));
        this.setCustomName(ownerName.copy());
        this.setCustomNameVisible(true);
        this.setNoAi(true);
        this.setPersistenceRequired();
    }

    public void captureBody(ServerPlayer player) {
        setOwner(player.getUUID(), player.getDisplayName());
        setHealth(Math.max(1.0F, getMaxHealth() * player.getHealth() / player.getMaxHealth()));
        bodyInventory = player.getInventory().save(new ListTag());
        bodyInventoryCaptured = true;
        bodySelectedSlot = player.getInventory().selected;
        for (EquipmentSlot slot : EquipmentSlot.values()) {
            ItemStack stack = player.getItemBySlot(slot);
            if (!stack.isEmpty()) {
                setItemSlot(slot, stack.copy());
            }
            setDropChance(slot, 0.0F);
        }
        for (MobEffectInstance effect : player.getActiveEffects()) {
            if (!SoulLifecycleManager.isSoulboundEffect(effect.getEffect())) {
                addEffect(new MobEffectInstance(effect));
            }
        }
    }

    public void restoreBody(ServerPlayer player) {
        float healthRatio = getHealth() / getMaxHealth();
        player.setHealth(Math.max(1.0F, player.getMaxHealth() * healthRatio));
        if (hasLiveConnection(player)) {
            player.teleportTo(getX(), getY(), getZ());
        } else {
            player.moveTo(getX(), getY(), getZ(), getYRot(), getXRot());
        }
        player.setYRot(getYRot());
        player.setXRot(getXRot());
        player.setDeltaMovement(getDeltaMovement());
        player.stopUsingItem();
        restoreCapturedInventory(player);
        bodyInventory.clear();
        bodyInventoryCaptured = false;
        player.getInventory().selected = bodySelectedSlot;
        restoreBodyEffects(player);
        if (hasLiveConnection(player)) {
            player.inventoryMenu.broadcastChanges();
        }
    }

    public void transferCapturedStacksToSoul(ServerPlayer player, boolean transferAll) {
        ListTag remaining = new ListTag();
        for (Tag rawEntry : bodyInventory) {
            CompoundTag entry = (CompoundTag) rawEntry;
            int slot = Byte.toUnsignedInt(entry.getByte("Slot"));
            ItemStack stack = ItemStack.of(entry);
            if (!stack.isEmpty() && (transferAll || SoulLifecycleManager.isSoulboundStack(stack, slot))) {
                placeSavedStack(player, slot, stack);
                clearEquipmentMirror(slot);
            } else {
                remaining.add(entry.copy());
            }
        }
        bodyInventory = remaining;
    }

    public boolean hasCapturedStackMatching(net.minecraft.world.item.Item item) {
        for (Tag rawEntry : bodyInventory) {
            ItemStack stack = ItemStack.of((CompoundTag) rawEntry);
            if (stack.is(item)) {
                return true;
            }
        }
        return false;
    }

    public boolean hasCapturedStackAt(int slot, net.minecraft.world.item.Item item) {
        for (Tag rawEntry : bodyInventory) {
            CompoundTag entry = (CompoundTag) rawEntry;
            if (Byte.toUnsignedInt(entry.getByte("Slot")) == slot
                    && ItemStack.of(entry).is(item)) {
                return true;
            }
        }
        return false;
    }

    public boolean belongsTo(UUID playerId) {
        return playerId != null && playerId.equals(getOwnerId());
    }

    public boolean hasCapturedBodyInventory() {
        return bodyInventoryCaptured;
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(OWNER_ID, Optional.empty());
    }

    @Override
    public boolean removeWhenFarAway(double distanceToClosestPlayer) {
        return false;
    }

    @Override
    protected void dropCustomDeathLoot(DamageSource source, int looting, boolean recentlyHit) {
        super.dropCustomDeathLoot(source, looting, recentlyHit);
        dropCapturedBodyInventory();
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        UUID savedOwnerId = getOwnerId();
        if (savedOwnerId != null) {
            tag.putUUID("Owner", savedOwnerId);
        }
        tag.put(BODY_INVENTORY, bodyInventory.copy());
        tag.putBoolean(BODY_INVENTORY_CAPTURED, bodyInventoryCaptured);
        tag.putInt(BODY_SELECTED_SLOT, bodySelectedSlot);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        ownerId = tag.hasUUID("Owner") ? tag.getUUID("Owner") : null;
        this.entityData.set(OWNER_ID, Optional.ofNullable(ownerId));
        bodyInventory = tag.getList(BODY_INVENTORY, net.minecraft.nbt.Tag.TAG_COMPOUND);
        bodyInventoryCaptured = tag.contains(BODY_INVENTORY_CAPTURED)
                ? tag.getBoolean(BODY_INVENTORY_CAPTURED)
                : !bodyInventory.isEmpty();
        bodySelectedSlot = tag.getInt(BODY_SELECTED_SLOT);
    }

    private void restoreBodyEffects(ServerPlayer player) {
        for (MobEffect effect : player.getActiveEffectsMap().keySet().toArray(MobEffect[]::new)) {
            if (!SoulLifecycleManager.isSoulboundEffect(effect)) {
                removeEffect(player, effect);
            }
        }
        for (MobEffectInstance effect : getActiveEffects()) {
            addEffect(player, effect);
        }
    }

    private static void removeEffect(ServerPlayer player, MobEffect effect) {
        if (hasLiveConnection(player)) {
            player.removeEffect(effect);
        } else {
            player.getActiveEffectsMap().remove(effect);
        }
    }

    private static void addEffect(ServerPlayer player, MobEffectInstance effect) {
        MobEffectInstance copy = new MobEffectInstance(effect);
        if (hasLiveConnection(player)) {
            player.addEffect(copy);
        } else {
            player.getActiveEffectsMap().put(copy.getEffect(), copy);
        }
    }

    private static void placeSavedStack(ServerPlayer player, int slot, ItemStack stack) {
        if (slot >= 0 && slot < player.getInventory().items.size()) {
            player.getInventory().items.set(slot, stack);
            return;
        }
        if (slot >= 100 && slot < 100 + player.getInventory().armor.size()) {
            player.getInventory().armor.set(slot - 100, stack);
            return;
        }
        if (slot >= 150 && slot < 150 + player.getInventory().offhand.size()) {
            player.getInventory().offhand.set(slot - 150, stack);
            return;
        }
        if (!player.getInventory().add(stack)) {
            player.drop(stack, false);
        }
    }

    private void restoreCapturedInventory(ServerPlayer player) {
        for (Tag rawEntry : bodyInventory) {
            CompoundTag entry = (CompoundTag) rawEntry;
            int slot = Byte.toUnsignedInt(entry.getByte("Slot"));
            ItemStack stack = ItemStack.of(entry);
            if (!stack.isEmpty()) {
                restoreSavedStack(player, slot, stack);
            }
        }
    }

    private static void restoreSavedStack(ServerPlayer player, int slot, ItemStack stack) {
        ItemStack existing = getStackAtSavedSlot(player, slot);
        if (existing != null && existing.isEmpty()) {
            placeSavedStack(player, slot, stack);
            return;
        }
        if (!player.getInventory().add(stack.copy())) {
            player.drop(stack.copy(), false);
        }
    }

    private static ItemStack getStackAtSavedSlot(ServerPlayer player, int slot) {
        if (slot >= 0 && slot < player.getInventory().items.size()) {
            return player.getInventory().items.get(slot);
        }
        if (slot >= 100 && slot < 100 + player.getInventory().armor.size()) {
            return player.getInventory().armor.get(slot - 100);
        }
        if (slot >= 150 && slot < 150 + player.getInventory().offhand.size()) {
            return player.getInventory().offhand.get(slot - 150);
        }
        return null;
    }

    private void clearEquipmentMirror(int inventorySlot) {
        EquipmentSlot slot = switch (inventorySlot) {
            case 36, 100 -> EquipmentSlot.FEET;
            case 37, 101 -> EquipmentSlot.LEGS;
            case 38, 102 -> EquipmentSlot.CHEST;
            case 39, 103 -> EquipmentSlot.HEAD;
            case 40, 150 -> EquipmentSlot.OFFHAND;
            default -> null;
        };
        if (inventorySlot == bodySelectedSlot) {
            slot = EquipmentSlot.MAINHAND;
        }
        if (slot != null) {
            setItemSlot(slot, ItemStack.EMPTY);
        }
    }

    private void dropCapturedBodyInventory() {
        if (!bodyInventoryCaptured || bodyInventory.isEmpty()) {
            return;
        }
        for (Tag rawEntry : bodyInventory) {
            ItemStack stack = ItemStack.of((CompoundTag) rawEntry);
            if (!stack.isEmpty()) {
                spawnAtLocation(stack.copy());
            }
        }
        bodyInventory.clear();
        bodyInventoryCaptured = false;
        for (EquipmentSlot slot : EquipmentSlot.values()) {
            setItemSlot(slot, ItemStack.EMPTY);
        }
    }

    private static boolean hasLiveConnection(ServerPlayer player) {
        return player.connection != null && player.connection.connection != null;
    }
}
