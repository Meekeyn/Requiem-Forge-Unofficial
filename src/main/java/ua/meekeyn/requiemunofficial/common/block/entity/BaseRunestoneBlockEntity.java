package ua.meekeyn.requiemunofficial.common.block.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

public abstract class BaseRunestoneBlockEntity extends BlockEntity {

    @Nullable
    protected Component customName;

    protected float powerRate;
    protected float previousPowerRate;
    protected long lastPowerUpdateTime;

    protected BaseRunestoneBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    public void setCustomName(@Nullable Component customName) {
        this.customName = customName;
        setChanged();
    }

    @Nullable
    public Component getCustomName() {
        return customName;
    }

    public float getPowerRate() {
        return powerRate;
    }

    public float getPreviousPowerRate() {
        return previousPowerRate;
    }

    public long getLastPowerUpdateTime() {
        return lastPowerUpdateTime;
    }

    public boolean isPowered() {
        return this.powerRate > 0.5F;
    }

    public void setPowerRate(float powerRate) {
        // Оновлює поточну та попередню силу живлення для плавного переходу
        this.previousPowerRate = this.powerRate;
        this.powerRate = powerRate;
        this.lastPowerUpdateTime = this.level == null ? 0L : this.level.getGameTime();
        setChanged();
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);

        if (tag.contains("power_rate")) {
            this.powerRate = tag.getFloat("power_rate");
        }

        if (tag.contains("previous_power_rate")) {
            this.previousPowerRate = tag.getFloat("previous_power_rate");
        }

        if (tag.contains("last_power_update_time")) {
            this.lastPowerUpdateTime = tag.getLong("last_power_update_time");
        }

        if (tag.contains("custom_name")) {
            this.customName = Component.Serializer.fromJson(tag.getString("custom_name"));
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);

        tag.putFloat("power_rate", this.powerRate);
        tag.putFloat("previous_power_rate", this.previousPowerRate);
        tag.putLong("last_power_update_time", this.lastPowerUpdateTime);

        if (this.customName != null) {
            tag.putString("custom_name", Component.Serializer.toJson(this.customName));
        }
    }
}