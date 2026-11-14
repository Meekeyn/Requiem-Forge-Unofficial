package ua.meekeyn.requiemunofficial.common.entity;

import net.minecraft.core.Holder;
import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.Property;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
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
    // Профіль для скіна на клієнті: ім'я та значення властивості "textures" (base64) з GameProfile.
    private static final EntityDataAccessor<String> PROFILE_NAME =
            SynchedEntityData.defineId(PlayerShellEntity.class, EntityDataSerializers.STRING);
    private static final EntityDataAccessor<String> PROFILE_TEXTURES =
            SynchedEntityData.defineId(PlayerShellEntity.class, EntityDataSerializers.STRING);
    private static final String PROFILE_TEXTURES_PROPERTY = "textures";
    private static final String POSSESSED_DATA = "RequiemPossessed";
    private static final String BODY_INVENTORY = "RequiemBodyInventory";
    private static final String BODY_INVENTORY_CAPTURED = "RequiemBodyInventoryCaptured";
    private static final String BODY_SELECTED_SLOT = "RequiemBodySelectedSlot";
    private static final String DISPLAY_ID = "DisplayId";
    private static final String DISPLAY_NAME = "DisplayName";
    private static final String PROFILE_NAME_TAG = "ProfileName";
    private static final String PROFILE_TEXTURES_TAG = "ProfileTextures";

    private UUID ownerId;
    // Профіль для відображення (скін/ім'я); власник лишається в ownerId, як у beta.16 setDisplayProfile.
    private UUID displayId;
    private String displayName;
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
        // На клієнті OWNER_ID несе UUID профілю для скіна; на сервері власник — лише поле ownerId.
        if (this.level().isClientSide) {
            return this.entityData.get(OWNER_ID).orElse(null);
        }
        return ownerId;
    }

    public void setOwner(UUID ownerId, Component ownerName) {
        this.ownerId = ownerId;
        this.displayId = null;
        this.displayName = null;
        this.entityData.set(OWNER_ID, Optional.ofNullable(ownerId));
        this.setCustomName(ownerName.copy());
        this.setCustomNameVisible(true);
        this.setNoAi(true);
        this.setPersistenceRequired();
    }

    public void setDisplayProfile(GameProfile profile) {
        setDisplayProfile(profile.getId(), profile.getName());
        String textures = getTexturesProperty(profile);
        if (textures != null) {
            this.entityData.set(PROFILE_TEXTURES, textures);
        }
    }

    public void setDisplayProfile(UUID id, String name) {
        // Змінюємо лише відображуваний профіль; власник (belongsTo/Owner NBT) не чіпаємо.
        this.displayId = id;
        this.displayName = name;
        syncDisplayId();
        // Текстури попереднього профілю більше не відповідають новому id.
        this.entityData.set(PROFILE_NAME, name == null ? "" : name);
        this.entityData.set(PROFILE_TEXTURES, "");
        if (name != null) {
            this.setCustomName(Component.literal(name));
            this.setCustomNameVisible(true);
        }
    }

    private void syncDisplayId() {
        this.entityData.set(OWNER_ID, Optional.ofNullable(displayId != null ? displayId : ownerId));
    }

    /** Ім'я профілю для скіна (клієнт); порожній рядок, якщо невідоме. */
    public String getProfileName() {
        return this.entityData.get(PROFILE_NAME);
    }

    /** Значення властивості "textures" профілю (base64); порожній рядок, якщо невідоме. */
    public String getProfileTextures() {
        return this.entityData.get(PROFILE_TEXTURES);
    }

    /** Чи керує зараз оболонкою чиясь душа (вселення); лише сервер. */
    public boolean isControlledBySoul() {
        return getPersistentData().getBoolean(POSSESSED_DATA);
    }

    private static String getTexturesProperty(GameProfile profile) {
        for (Property property : profile.getProperties().get(PROFILE_TEXTURES_PROPERTY)) {
            if (property.value() != null && !property.value().isEmpty()) {
                return property.value();
            }
        }
        return null;
    }

    public void captureBody(ServerPlayer player) {
        setOwner(player.getUUID(), player.getDisplayName());
        GameProfile profile = player.getGameProfile();
        this.entityData.set(PROFILE_NAME, profile.getName() == null ? "" : profile.getName());
        String textures = getTexturesProperty(profile);
        this.entityData.set(PROFILE_TEXTURES, textures == null ? "" : textures);
        // Оболонка без AI ніколи не рухається, тож BodyRotationControl не повертає тіло: задаємо поворот явно.
        float yaw = player.getYRot();
        setYRot(yaw);
        setXRot(player.getXRot());
        this.yRotO = yaw;
        this.yBodyRot = yaw;
        this.yBodyRotO = yaw;
        this.yHeadRot = yaw;
        this.yHeadRotO = yaw;
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
            ItemStack stack = ua.meekeyn.requiemunofficial.common.util.ItemNbt.read(entry);
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
            ItemStack stack = ua.meekeyn.requiemunofficial.common.util.ItemNbt.read((CompoundTag) rawEntry);
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
                    && ua.meekeyn.requiemunofficial.common.util.ItemNbt.read(entry).is(item)) {
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
    protected void defineSynchedData(net.minecraft.network.syncher.SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(OWNER_ID, Optional.empty());
        builder.define(PROFILE_NAME, "");
        builder.define(PROFILE_TEXTURES, "");
    }

    /**
     * У 1.21 NoAi-моб не симулює фізику на сервері (travel викликається лише при isEffectiveAi),
     * тож оболонка зависала в повітрі. Рух керується сервером незалежно від NoAi — тіло падає як у 1.20.
     */
    @Override
    public boolean isControlledByLocalInstance() {
        return super.isControlledByLocalInstance() || !level().isClientSide;
    }

    @Override
    protected float tickHeadTurn(float yRot, float animStep) {
        // NoAi-оболонка дивиться туди ж, куди дивився гравець; працює і на клієнті
        // (yRot приходить у пакеті спавну, прапорець NoAi синхронізується).
        if (isNoAi()) {
            float yaw = getYRot();
            this.yBodyRot = yaw;
            this.yBodyRotO = yaw;
            this.yHeadRot = yaw;
            this.yHeadRotO = yaw;
            return animStep;
        }
        return super.tickHeadTurn(yRot, animStep);
    }

    @Override
    public boolean isInvulnerableTo(DamageSource source) {
        // Покинуте тіло не задихається у блоках і не тоне.
        return source.is(DamageTypes.IN_WALL)
                || source.is(DamageTypeTags.IS_DROWNING)
                || super.isInvulnerableTo(source);
    }

    @Override
    public boolean removeWhenFarAway(double distanceToClosestPlayer) {
        return false;
    }

    @Override
    protected void dropCustomDeathLoot(net.minecraft.server.level.ServerLevel level, DamageSource source, boolean recentlyHit) {
        super.dropCustomDeathLoot(level, source, recentlyHit);
        dropCapturedBodyInventory();
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        if (ownerId != null) {
            tag.putUUID("Owner", ownerId);
        }
        if (displayId != null) {
            tag.putUUID(DISPLAY_ID, displayId);
        }
        if (displayName != null) {
            tag.putString(DISPLAY_NAME, displayName);
        }
        if (!getProfileName().isEmpty()) {
            tag.putString(PROFILE_NAME_TAG, getProfileName());
        }
        if (!getProfileTextures().isEmpty()) {
            tag.putString(PROFILE_TEXTURES_TAG, getProfileTextures());
        }
        tag.put(BODY_INVENTORY, bodyInventory.copy());
        tag.putBoolean(BODY_INVENTORY_CAPTURED, bodyInventoryCaptured);
        tag.putInt(BODY_SELECTED_SLOT, bodySelectedSlot);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        ownerId = tag.hasUUID("Owner") ? tag.getUUID("Owner") : null;
        displayId = tag.hasUUID(DISPLAY_ID) ? tag.getUUID(DISPLAY_ID) : null;
        displayName = tag.contains(DISPLAY_NAME, Tag.TAG_STRING) ? tag.getString(DISPLAY_NAME) : null;
        syncDisplayId();
        this.entityData.set(PROFILE_NAME, tag.getString(PROFILE_NAME_TAG));
        this.entityData.set(PROFILE_TEXTURES, tag.getString(PROFILE_TEXTURES_TAG));
        bodyInventory = tag.getList(BODY_INVENTORY, net.minecraft.nbt.Tag.TAG_COMPOUND);
        bodyInventoryCaptured = tag.contains(BODY_INVENTORY_CAPTURED)
                ? tag.getBoolean(BODY_INVENTORY_CAPTURED)
                : !bodyInventory.isEmpty();
        bodySelectedSlot = tag.getInt(BODY_SELECTED_SLOT);
    }

    private void restoreBodyEffects(ServerPlayer player) {
        for (Holder<MobEffect> effect : player.getActiveEffectsMap().keySet().toArray(Holder[]::new)) {
            if (!SoulLifecycleManager.isSoulboundEffect(effect)) {
                removeEffect(player, effect);
            }
        }
        for (MobEffectInstance effect : getActiveEffects()) {
            addEffect(player, effect);
        }
    }

    private static void removeEffect(ServerPlayer player, Holder<MobEffect> effect) {
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
            dropRemainder(player, stack);
        }
    }

    /**
     * Викидає залишок біля гравця напряму, повз ItemTossEvent: поки гравець ще VAGRANT,
     * VagrantEvents скасовує викидання, і player.drop просто знищив би предмет.
     */
    private static void dropRemainder(ServerPlayer player, ItemStack remainder) {
        if (remainder.isEmpty()) {
            return;
        }
        net.minecraft.world.entity.item.ItemEntity item = new net.minecraft.world.entity.item.ItemEntity(
                player.level(), player.getX(), player.getEyeY() - 0.3D, player.getZ(), remainder);
        item.setPickUpDelay(40);
        player.level().addFreshEntity(item);
    }

    private void restoreCapturedInventory(ServerPlayer player) {
        for (Tag rawEntry : bodyInventory) {
            CompoundTag entry = (CompoundTag) rawEntry;
            int slot = Byte.toUnsignedInt(entry.getByte("Slot"));
            ItemStack stack = ua.meekeyn.requiemunofficial.common.util.ItemNbt.read(entry);
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
        // Одна копія: add() зменшує її на покладену кількість, тож викидаємо лише справжній залишок.
        ItemStack remainder = stack.copy();
        if (!player.getInventory().add(remainder)) {
            dropRemainder(player, remainder);
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
            ItemStack stack = ua.meekeyn.requiemunofficial.common.util.ItemNbt.read((CompoundTag) rawEntry);
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
        return player.connection != null && player.connection.getConnection() != null;
    }
}
