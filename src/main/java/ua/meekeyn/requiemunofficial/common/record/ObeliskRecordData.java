package ua.meekeyn.requiemunofficial.common.record;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.SavedData;
import ua.meekeyn.requiemunofficial.common.block.entity.RunestoneBlockEntity;

import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public class ObeliskRecordData extends SavedData {
    private static final String DATA_NAME = "requiemunofficial_obelisk_records";

    private final Map<UUID, ObeliskRecord> records = new LinkedHashMap<>();

    public static ObeliskRecordData get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(
                ObeliskRecordData::load,
                ObeliskRecordData::new,
                DATA_NAME
        );
    }

    public static ObeliskRecordData get(Level level) {
        return get(level.getServer());
    }

    public static ObeliskRecordData load(CompoundTag tag) {
        ObeliskRecordData data = new ObeliskRecordData();
        ListTag list = tag.getList("Records", Tag.TAG_COMPOUND);
        for (Tag raw : list) {
            ObeliskRecord.load((CompoundTag) raw).ifPresent(record ->
                    data.records.put(record.recordId(), record));
        }
        return data;
    }

    @Override
    public CompoundTag save(CompoundTag tag) {
        ListTag list = new ListTag();
        for (ObeliskRecord record : records.values()) {
            list.add(record.save());
        }
        tag.put("Records", list);
        return tag;
    }

    public Collection<ObeliskRecord> records() {
        return records.values();
    }

    public Optional<ObeliskRecord> getRecord(UUID recordId) {
        return Optional.ofNullable(records.get(recordId)).filter(record -> !record.invalid());
    }

    public Optional<ObeliskRecord> getRecordAt(ResourceKey<Level> dimension, BlockPos pos) {
        return records.values().stream()
                .filter(record -> !record.invalid())
                .filter(record -> record.dimension().equals(dimension.location()))
                .filter(record -> record.pos().equals(pos))
                .findFirst();
    }

    public ObeliskRecord getOrCreateRecord(ServerLevel level, BlockPos pos, RunestoneBlockEntity runestone) {
        ResourceLocation dimension = level.dimension().location();
        Optional<ObeliskRecord> existing = getRecordAt(level.dimension(), pos);
        if (existing.isPresent()) {
            ObeliskRecord record = existing.get();
            record.updateFrom(runestone, dimension, pos);
            setDirty();
            return record;
        }

        records.values().stream()
                .filter(record -> !record.invalid())
                .filter(record -> record.dimension().equals(dimension))
                .filter(record -> record.pos().equals(pos))
                .forEach(ObeliskRecord::invalidate);

        ObeliskRecord record = ObeliskRecord.create(UUID.randomUUID(), dimension, pos, runestone);
        records.put(record.recordId(), record);
        setDirty();
        return record;
    }

    public void invalidate(UUID recordId) {
        ObeliskRecord record = records.get(recordId);
        if (record != null && !record.invalid()) {
            record.invalidate();
            setDirty();
        }
    }

    public void invalidateAt(ResourceKey<Level> dimension, BlockPos pos) {
        boolean changed = false;
        for (ObeliskRecord record : records.values()) {
            if (!record.invalid()
                    && record.dimension().equals(dimension.location())
                    && record.pos().equals(pos)) {
                record.invalidate();
                changed = true;
            }
        }
        if (changed) {
            setDirty();
        }
    }

    public boolean validateLoadedDestination(MinecraftServer server, UUID recordId) {
        ObeliskRecord record = records.get(recordId);
        if (record == null || record.invalid()) {
            return false;
        }
        ServerLevel level = server.getLevel(ResourceKey.create(net.minecraft.core.registries.Registries.DIMENSION, record.dimension()));
        if (level == null || !level.isLoaded(record.pos())) {
            return true;
        }
        if (!(level.getBlockEntity(record.pos()) instanceof RunestoneBlockEntity runestone)
                || !isUsableRift(runestone)) {
            record.invalidate();
            setDirty();
            return false;
        }
        record.updateFrom(runestone, level.dimension().location(), record.pos());
        setDirty();
        return true;
    }

    public Optional<ObeliskRecord> nearestValidLoaded(ServerLevel level, BlockPos origin, int maxDistance) {
        double maxDistanceSqr = maxDistance * maxDistance;
        return records.values().stream()
                .filter(record -> !record.invalid())
                .filter(record -> record.dimension().equals(level.dimension().location()))
                .filter(record -> record.pos().distSqr(origin) <= maxDistanceSqr)
                .filter(record -> level.isLoaded(record.pos()))
                .filter(record -> level.getBlockEntity(record.pos()) instanceof RunestoneBlockEntity runestone
                        && isUsableRift(runestone))
                .min(Comparator.comparingDouble(record -> record.pos().distSqr(origin)));
    }

    public static boolean isUsableRift(RunestoneBlockEntity runestone) {
        return runestone.isHeadBlock()
                && runestone.isStructureValid()
                && runestone.isActivatedBlock()
                && runestone.isPowered()
                && runestone.getRuneLevel("rift") > 0;
    }

    public record ObeliskDescriptor(UUID recordId, ResourceLocation dimension, BlockPos pos, int width, int height,
                                    Optional<Component> name) {
        public Component resolveName() {
            return name.orElseGet(() -> Component.literal(pos.toShortString()));
        }
    }

    public static final class ObeliskRecord {
        private final UUID recordId;
        private ResourceLocation dimension;
        private BlockPos pos;
        private int width;
        private int height;
        private Component name;
        private boolean invalid;

        private ObeliskRecord(UUID recordId, ResourceLocation dimension, BlockPos pos, int width, int height,
                              Component name, boolean invalid) {
            this.recordId = recordId;
            this.dimension = dimension;
            this.pos = pos.immutable();
            this.width = width;
            this.height = height;
            this.name = name;
            this.invalid = invalid;
        }

        private static ObeliskRecord create(UUID recordId, ResourceLocation dimension, BlockPos pos,
                                            RunestoneBlockEntity runestone) {
            return new ObeliskRecord(
                    recordId,
                    dimension,
                    pos,
                    Math.max(1, runestone.getObeliskCoreWidth()),
                    Math.max(1, runestone.getObeliskCoreHeight()),
                    runestone.getCustomName(),
                    false
            );
        }

        private static Optional<ObeliskRecord> load(CompoundTag tag) {
            if (!tag.hasUUID("Record") || !tag.contains("Dimension") || !tag.contains("Pos")) {
                return Optional.empty();
            }
            ResourceLocation dimension = ResourceLocation.tryParse(tag.getString("Dimension"));
            if (dimension == null) {
                return Optional.empty();
            }
            Component name = null;
            if (tag.contains("Name")) {
                name = Component.Serializer.fromJson(tag.getString("Name"));
            }
            return Optional.of(new ObeliskRecord(
                    tag.getUUID("Record"),
                    dimension,
                    BlockPos.of(tag.getLong("Pos")),
                    Math.max(1, tag.getInt("Width")),
                    Math.max(1, tag.getInt("Height")),
                    name,
                    tag.getBoolean("Invalid")
            ));
        }

        private CompoundTag save() {
            CompoundTag tag = new CompoundTag();
            tag.putUUID("Record", recordId);
            tag.putString("Dimension", dimension.toString());
            tag.putLong("Pos", pos.asLong());
            tag.putInt("Width", width);
            tag.putInt("Height", height);
            if (name != null) {
                tag.putString("Name", Component.Serializer.toJson(name));
            }
            tag.putBoolean("Invalid", invalid);
            return tag;
        }

        private void updateFrom(RunestoneBlockEntity runestone, ResourceLocation dimension, BlockPos pos) {
            this.dimension = dimension;
            this.pos = pos.immutable();
            this.width = Math.max(1, runestone.getObeliskCoreWidth());
            this.height = Math.max(1, runestone.getObeliskCoreHeight());
            this.name = runestone.getCustomName();
            this.invalid = false;
        }

        public ObeliskDescriptor descriptor() {
            return new ObeliskDescriptor(recordId, dimension, pos, width, height, Optional.ofNullable(name));
        }

        public UUID recordId() {
            return recordId;
        }

        public ResourceLocation dimension() {
            return dimension;
        }

        public BlockPos pos() {
            return pos;
        }

        public int width() {
            return width;
        }

        public int height() {
            return height;
        }

        public boolean invalid() {
            return invalid;
        }

        private void invalidate() {
            this.invalid = true;
        }
    }
}
