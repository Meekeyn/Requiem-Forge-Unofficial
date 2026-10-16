package ua.meekeyn.requiemunofficial.common.block.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.state.BlockState;
import ua.meekeyn.requiemunofficial.common.block.InertRunestoneBlock;
import ua.meekeyn.requiemunofficial.common.init.ModBlockEntities;
import ua.meekeyn.requiemunofficial.common.record.ObeliskRecordData;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public class RunestoneBlockEntity extends BaseRunestoneBlockEntity {

    private int obeliskCoreWidth;
    private int obeliskCoreHeight;
    private boolean structureValid;
    private int storedSoulEnergy;
    private String activeRitualId = "";
    private long ritualEndGameTime;
    private UUID ritualPlayerId;
    private UUID ritualTargetId;
    private UUID obeliskRecordId;
    private long nextChargeSoundGameTime;
    private long nextAmbientSoundGameTime;
    private final Map<String, Integer> runeLevels = new HashMap<>();
    private final Map<String, BlockPos> runeSamplePositions = new HashMap<>();

    public RunestoneBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.RUNESTONE.get(), pos, state);
    }

    public int getObeliskCoreWidth() {
        return obeliskCoreWidth;
    }

    public int getObeliskCoreHeight() {
        return obeliskCoreHeight;
    }

    public boolean isStructureValid() {
        return structureValid;
    }

    public int getStoredSoulEnergy() {
        return isHeadBlock() ? storedSoulEnergy : 0;
    }

    public void addStoredSoulEnergy(int amount) {
        if (!isHeadBlock() || amount <= 0) {
            return;
        }

        // Накопичує енергію душ у контролері обеліска
        this.storedSoulEnergy += amount;
        setChanged();
    }

    public int consumeStoredSoulEnergy(int amount) {
        if (!isHeadBlock() || amount <= 0 || this.storedSoulEnergy <= 0) {
            return 0;
        }

        int consumed = Math.min(amount, this.storedSoulEnergy);
        this.storedSoulEnergy -= consumed;
        setChanged();
        return consumed;
    }

    public void startRitual(ResourceLocation ritualId, long endGameTime, UUID playerId, UUID targetId) {
        this.activeRitualId = ritualId.toString();
        this.ritualEndGameTime = endGameTime;
        this.ritualPlayerId = playerId;
        this.ritualTargetId = targetId;
        setChanged();
    }

    public boolean isRitualActive(long gameTime) {
        return hasPendingRitual() && gameTime < this.ritualEndGameTime;
    }

    public boolean hasPendingRitual() {
        return !this.activeRitualId.isEmpty();
    }

    public boolean isRitualReady(long gameTime) {
        return hasPendingRitual() && gameTime >= this.ritualEndGameTime;
    }

    public String getActiveRitualId(long gameTime) {
        return isRitualActive(gameTime) ? this.activeRitualId : "";
    }

    public String getPendingRitualId() {
        return this.activeRitualId;
    }

    public UUID getRitualPlayerId() {
        return this.ritualPlayerId;
    }

    public UUID getRitualTargetId() {
        return this.ritualTargetId;
    }

    public void clearRitual() {
        this.activeRitualId = "";
        this.ritualEndGameTime = 0L;
        this.ritualPlayerId = null;
        this.ritualTargetId = null;
        setChanged();
    }

    public Map<String, Integer> getRuneLevels() {
        return Collections.unmodifiableMap(runeLevels);
    }

    public int getRuneLevel(String runeType) {
        return runeLevels.getOrDefault(runeType, 0);
    }

    public Map<String, BlockPos> getRuneSamplePositions() {
        return Collections.unmodifiableMap(runeSamplePositions);
    }

    public Optional<UUID> getObeliskRecordId() {
        return Optional.ofNullable(obeliskRecordId);
    }

    public Optional<ObeliskRecordData.ObeliskRecord> getObeliskRecord() {
        if (level == null || obeliskRecordId == null) {
            return Optional.empty();
        }
        return ObeliskRecordData.get(level).getRecord(obeliskRecordId);
    }

    public ObeliskRecordData.ObeliskRecord getOrCreateObeliskRecord() {
        if (level instanceof net.minecraft.server.level.ServerLevel serverLevel) {
            ObeliskRecordData.ObeliskRecord record = ObeliskRecordData.get(serverLevel)
                    .getOrCreateRecord(serverLevel, getBlockPos(), this);
            this.obeliskRecordId = record.recordId();
            setChanged();
            return record;
        }
        return null;
    }

    public void clearObeliskRecord(boolean invalidateRecord) {
        if (level instanceof net.minecraft.server.level.ServerLevel serverLevel && obeliskRecordId != null && invalidateRecord) {
            ObeliskRecordData.get(serverLevel).invalidate(obeliskRecordId);
        }
        this.obeliskRecordId = null;
        setChanged();
    }

    public BlockPos getRuneSamplePosition(String runeType) {
        return runeSamplePositions.get(runeType);
    }

    public void setStructureData(int width, int height, boolean valid,
                                 Map<String, Integer> levels,
                                 Map<String, BlockPos> samplePositions) {
        // Зберігає параметри структури, рівні рун і еталонні позиції рун
        this.obeliskCoreWidth = width;
        this.obeliskCoreHeight = height;
        this.structureValid = valid;

        this.runeLevels.clear();
        this.runeLevels.putAll(levels);

        this.runeSamplePositions.clear();
        for (Map.Entry<String, BlockPos> entry : samplePositions.entrySet()) {
            this.runeSamplePositions.put(entry.getKey(), entry.getValue().immutable());
        }

        if (isHeadBlock() && valid && this.runeLevels.getOrDefault("rift", 0) > 0) {
            getOrCreateObeliskRecord();
        }

        setChanged();
    }

    public void clearStructureData() {
        this.obeliskCoreWidth = 0;
        this.obeliskCoreHeight = 0;
        this.structureValid = false;
        this.runeLevels.clear();
        this.runeSamplePositions.clear();
        this.storedSoulEnergy = 0;
        this.nextChargeSoundGameTime = 0L;
        this.nextAmbientSoundGameTime = 0L;
        clearObeliskRecord(true);
        clearRitual();
        this.setPowerRate(0.0F);
        setChanged();
    }

    public boolean shouldPlayChargeSound(long gameTime, int cooldownTicks) {
        if (gameTime < this.nextChargeSoundGameTime) {
            return false;
        }
        this.nextChargeSoundGameTime = gameTime + cooldownTicks;
        setChanged();
        return true;
    }

    public boolean shouldPlayAmbientSound(long gameTime, int cooldownTicks) {
        if (gameTime < this.nextAmbientSoundGameTime) {
            return false;
        }
        this.nextAmbientSoundGameTime = gameTime + cooldownTicks;
        setChanged();
        return true;
    }

    public boolean isHeadBlock() {
        return this.getBlockState().hasProperty(InertRunestoneBlock.HEAD)
                && this.getBlockState().getValue(InertRunestoneBlock.HEAD);
    }

    public boolean isActivatedBlock() {
        return this.getBlockState().hasProperty(InertRunestoneBlock.ACTIVATED)
                && this.getBlockState().getValue(InertRunestoneBlock.ACTIVATED);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);

        this.obeliskCoreWidth = tag.getInt("obelisk_core_width");
        this.obeliskCoreHeight = tag.getInt("obelisk_core_height");
        this.structureValid = tag.getBoolean("structure_valid");
        this.storedSoulEnergy = tag.getInt("stored_soul_energy");
        if (!isHeadBlock()) {
            this.storedSoulEnergy = 0;
        }
        this.activeRitualId = tag.getString("active_ritual_id");
        this.ritualEndGameTime = tag.getLong("ritual_end_game_time");
        this.ritualPlayerId = tag.hasUUID("ritual_player_id") ? tag.getUUID("ritual_player_id") : null;
        this.ritualTargetId = tag.hasUUID("ritual_target_id") ? tag.getUUID("ritual_target_id") : null;
        this.obeliskRecordId = tag.hasUUID("obelisk_record_id") ? tag.getUUID("obelisk_record_id") : null;
        this.nextChargeSoundGameTime = tag.getLong("next_charge_sound_game_time");
        this.nextAmbientSoundGameTime = tag.getLong("next_ambient_sound_game_time");

        this.runeLevels.clear();
        CompoundTag runeLevelsTag = tag.getCompound("rune_levels");
        for (String key : runeLevelsTag.getAllKeys()) {
            this.runeLevels.put(key, runeLevelsTag.getInt(key));
        }

        this.runeSamplePositions.clear();
        CompoundTag samplePositionsTag = tag.getCompound("rune_sample_positions");
        for (String key : samplePositionsTag.getAllKeys()) {
            CompoundTag posTag = samplePositionsTag.getCompound(key);
            this.runeSamplePositions.put(key, new BlockPos(
                    posTag.getInt("x"),
                    posTag.getInt("y"),
                    posTag.getInt("z")
            ));
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);

        tag.putInt("obelisk_core_width", this.obeliskCoreWidth);
        tag.putInt("obelisk_core_height", this.obeliskCoreHeight);
        tag.putBoolean("structure_valid", this.structureValid);
        if (isHeadBlock()) {
            tag.putInt("stored_soul_energy", this.storedSoulEnergy);
        }
        tag.putString("active_ritual_id", this.activeRitualId);
        tag.putLong("ritual_end_game_time", this.ritualEndGameTime);
        if (this.ritualPlayerId != null) {
            tag.putUUID("ritual_player_id", this.ritualPlayerId);
        }
        if (this.ritualTargetId != null) {
            tag.putUUID("ritual_target_id", this.ritualTargetId);
        }
        if (this.obeliskRecordId != null) {
            tag.putUUID("obelisk_record_id", this.obeliskRecordId);
        }
        tag.putLong("next_charge_sound_game_time", this.nextChargeSoundGameTime);
        tag.putLong("next_ambient_sound_game_time", this.nextAmbientSoundGameTime);

        CompoundTag runeLevelsTag = new CompoundTag();
        for (Map.Entry<String, Integer> entry : this.runeLevels.entrySet()) {
            runeLevelsTag.putInt(entry.getKey(), entry.getValue());
        }
        tag.put("rune_levels", runeLevelsTag);

        CompoundTag samplePositionsTag = new CompoundTag();
        for (Map.Entry<String, BlockPos> entry : this.runeSamplePositions.entrySet()) {
            CompoundTag posTag = new CompoundTag();
            posTag.putInt("x", entry.getValue().getX());
            posTag.putInt("y", entry.getValue().getY());
            posTag.putInt("z", entry.getValue().getZ());
            samplePositionsTag.put(entry.getKey(), posTag);
        }
        tag.put("rune_sample_positions", samplePositionsTag);
    }
}
