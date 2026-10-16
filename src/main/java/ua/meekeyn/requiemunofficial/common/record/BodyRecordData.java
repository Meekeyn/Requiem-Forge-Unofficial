package ua.meekeyn.requiemunofficial.common.record;

import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraftforge.registries.ForgeRegistries;
import ua.meekeyn.requiemunofficial.RequiemUnofficial;
import ua.meekeyn.requiemunofficial.common.soul.EntitySoulManager;

import javax.annotation.Nullable;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public class BodyRecordData extends SavedData {
    public static final String ENTITY_RECORD_TAG = "RequiemBodyRecord";
    private static final String DATA_NAME = RequiemUnofficial.MODID + "_body_records";

    private final Map<UUID, BodyRecord> records = new LinkedHashMap<>();

    public static BodyRecordData get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(BodyRecordData::load, BodyRecordData::new, DATA_NAME);
    }

    public static BodyRecordData get(Level level) {
        if (!(level instanceof ServerLevel serverLevel)) {
            throw new IllegalStateException("Body records are server-side only");
        }
        return get(serverLevel.getServer());
    }

    public static BodyRecordData load(CompoundTag tag) {
        BodyRecordData data = new BodyRecordData();
        ListTag list = tag.getList("Records", Tag.TAG_COMPOUND);
        for (Tag raw : list) {
            BodyRecord record = BodyRecord.load((CompoundTag) raw);
            if (record != null) {
                data.records.put(record.recordId(), record);
            }
        }
        return data;
    }

    @Override
    public CompoundTag save(CompoundTag tag) {
        ListTag list = new ListTag();
        for (BodyRecord record : records.values()) {
            list.add(record.save());
        }
        tag.put("Records", list);
        return tag;
    }

    public Collection<BodyRecord> records() {
        return records.values();
    }

    public Optional<BodyRecord> getRecord(UUID recordId) {
        BodyRecord record = records.get(recordId);
        return record == null || record.invalid() ? Optional.empty() : Optional.of(record);
    }

    public BodyRecord getOrCreateRecord(LivingEntity entity) {
        UUID existing = getEntityRecordId(entity);
        if (existing != null) {
            BodyRecord record = records.get(existing);
            if (record != null && !record.invalid()) {
                updateRecord(entity, record);
                return record;
            }
        }

        BodyRecord record = BodyRecord.create(UUID.randomUUID(), entity);
        records.put(record.recordId(), record);
        entity.getPersistentData().putUUID(ENTITY_RECORD_TAG, record.recordId());
        setDirty();
        return record;
    }

    public void updateRecord(LivingEntity entity) {
        UUID recordId = getEntityRecordId(entity);
        if (recordId == null) {
            return;
        }
        BodyRecord record = records.get(recordId);
        if (record == null || record.invalid()) {
            entity.getPersistentData().remove(ENTITY_RECORD_TAG);
            return;
        }
        updateRecord(entity, record);
        if (record.releasedSoulPending() && EntitySoulManager.isInertSoullessBody(entity)) {
            EntitySoulManager.restoreSoul(entity);
            invalidate(record.recordId());
        }
    }

    private void updateRecord(LivingEntity entity, BodyRecord record) {
        record.updateFrom(entity);
        entity.getPersistentData().putUUID(ENTITY_RECORD_TAG, record.recordId());
        setDirty();
    }

    public void markReleasedSoulPending(UUID recordId) {
        BodyRecord record = records.get(recordId);
        if (record == null || record.invalid()) {
            return;
        }
        record.setReleasedSoulPending(true);
        record.setActiveReleasedSoul(false);
        setDirty();
    }

    public void markActiveReleasedSoul(UUID recordId, boolean active) {
        BodyRecord record = records.get(recordId);
        if (record == null || record.invalid()) {
            return;
        }
        record.setActiveReleasedSoul(active);
        setDirty();
    }

    public void invalidate(UUID recordId) {
        BodyRecord record = records.get(recordId);
        if (record == null) {
            return;
        }
        record.setInvalid(true);
        record.setActiveReleasedSoul(false);
        record.setReleasedSoulPending(false);
        setDirty();
    }

    public Optional<LivingEntity> resolveLoadedBody(MinecraftServer server, UUID recordId) {
        return getRecord(recordId).flatMap(record -> {
            ServerLevel level = server.getLevel(record.dimension());
            if (level == null) {
                return Optional.empty();
            }
            Entity entity = level.getEntity(record.entityId());
            if (entity instanceof LivingEntity living && living.isAlive() && !living.isRemoved()) {
                return Optional.of(living);
            }
            return Optional.empty();
        });
    }

    @Nullable
    public static UUID getEntityRecordId(Entity entity) {
        CompoundTag persistent = entity.getPersistentData();
        return persistent.hasUUID(ENTITY_RECORD_TAG) ? persistent.getUUID(ENTITY_RECORD_TAG) : null;
    }

    public static final class BodyRecord {
        private final UUID recordId;
        private UUID entityId;
        private ResourceKey<Level> dimension;
        private ResourceLocation entityType;
        private double x;
        private double y;
        private double z;
        private boolean releasedSoulPending;
        private boolean activeReleasedSoul;
        private boolean invalid;

        private BodyRecord(UUID recordId, UUID entityId, ResourceKey<Level> dimension, ResourceLocation entityType,
                           double x, double y, double z, boolean releasedSoulPending,
                           boolean activeReleasedSoul, boolean invalid) {
            this.recordId = recordId;
            this.entityId = entityId;
            this.dimension = dimension;
            this.entityType = entityType;
            this.x = x;
            this.y = y;
            this.z = z;
            this.releasedSoulPending = releasedSoulPending;
            this.activeReleasedSoul = activeReleasedSoul;
            this.invalid = invalid;
        }

        private static BodyRecord create(UUID recordId, LivingEntity entity) {
            ResourceLocation typeId = ForgeRegistries.ENTITY_TYPES.getKey(entity.getType());
            return new BodyRecord(
                    recordId,
                    entity.getUUID(),
                    entity.level().dimension(),
                    typeId == null ? new ResourceLocation("minecraft", "unknown") : typeId,
                    entity.getX(),
                    entity.getY(),
                    entity.getZ(),
                    false,
                    false,
                    false
            );
        }

        @Nullable
        private static BodyRecord load(CompoundTag tag) {
            if (!tag.hasUUID("Record") || !tag.hasUUID("Entity")) {
                return null;
            }
            ResourceLocation dimensionId = ResourceLocation.tryParse(tag.getString("Dimension"));
            ResourceLocation typeId = ResourceLocation.tryParse(tag.getString("EntityType"));
            if (dimensionId == null || typeId == null) {
                return null;
            }
            return new BodyRecord(
                    tag.getUUID("Record"),
                    tag.getUUID("Entity"),
                    ResourceKey.create(Registries.DIMENSION, dimensionId),
                    typeId,
                    tag.getDouble("X"),
                    tag.getDouble("Y"),
                    tag.getDouble("Z"),
                    tag.getBoolean("ReleasedSoulPending"),
                    tag.getBoolean("ActiveReleasedSoul"),
                    tag.getBoolean("Invalid")
            );
        }

        private CompoundTag save() {
            CompoundTag tag = new CompoundTag();
            tag.putUUID("Record", recordId);
            tag.putUUID("Entity", entityId);
            tag.putString("Dimension", dimension.location().toString());
            tag.putString("EntityType", entityType.toString());
            tag.putDouble("X", x);
            tag.putDouble("Y", y);
            tag.putDouble("Z", z);
            tag.putBoolean("ReleasedSoulPending", releasedSoulPending);
            tag.putBoolean("ActiveReleasedSoul", activeReleasedSoul);
            tag.putBoolean("Invalid", invalid);
            return tag;
        }

        private void updateFrom(LivingEntity entity) {
            this.entityId = entity.getUUID();
            this.dimension = entity.level().dimension();
            ResourceLocation typeId = ForgeRegistries.ENTITY_TYPES.getKey(entity.getType());
            this.entityType = typeId == null ? this.entityType : typeId;
            this.x = entity.getX();
            this.y = entity.getY();
            this.z = entity.getZ();
        }

        public UUID recordId() {
            return recordId;
        }

        public UUID entityId() {
            return entityId;
        }

        public ResourceKey<Level> dimension() {
            return dimension;
        }

        public ResourceLocation entityType() {
            return entityType;
        }

        public double x() {
            return x;
        }

        public double y() {
            return y;
        }

        public double z() {
            return z;
        }

        public boolean releasedSoulPending() {
            return releasedSoulPending;
        }

        public void setReleasedSoulPending(boolean releasedSoulPending) {
            this.releasedSoulPending = releasedSoulPending;
        }

        public boolean activeReleasedSoul() {
            return activeReleasedSoul;
        }

        public void setActiveReleasedSoul(boolean activeReleasedSoul) {
            this.activeReleasedSoul = activeReleasedSoul;
        }

        public boolean invalid() {
            return invalid;
        }

        public void setInvalid(boolean invalid) {
            this.invalid = invalid;
        }
    }
}
