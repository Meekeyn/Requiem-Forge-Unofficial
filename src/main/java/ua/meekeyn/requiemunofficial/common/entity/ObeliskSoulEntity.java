package ua.meekeyn.requiemunofficial.common.entity;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;

public class ObeliskSoulEntity extends Entity {

    private static final EntityDataAccessor<String> SOUL_TYPE =
            SynchedEntityData.defineId(ObeliskSoulEntity.class, EntityDataSerializers.STRING);
    private static final EntityDataAccessor<Integer> SOUL_AMOUNT =
            SynchedEntityData.defineId(ObeliskSoulEntity.class, EntityDataSerializers.INT);

    private double targetX;
    private double targetY;
    private double targetZ;
    private int lifetime = 40;

    public ObeliskSoulEntity(EntityType<? extends ObeliskSoulEntity> entityType, Level level) {
        super(entityType, level);
        this.noPhysics = true;
    }

    public void setTarget(double x, double y, double z) {
        this.targetX = x;
        this.targetY = y;
        this.targetZ = z;
    }

    public void setLifetime(int lifetime) {
        this.lifetime = Math.max(1, lifetime);
    }

    public void setSoulData(String soulType, int soulAmount) {
        this.entityData.set(SOUL_TYPE, soulType);
        this.entityData.set(SOUL_AMOUNT, Math.max(1, soulAmount));
    }

    public String getSoulType() {
        return this.entityData.get(SOUL_TYPE);
    }

    public int getSoulAmount() {
        return this.entityData.get(SOUL_AMOUNT);
    }

    @Override
    protected void defineSynchedData(net.minecraft.network.syncher.SynchedEntityData.Builder builder) {
        builder.define(SOUL_TYPE, "empty");
        builder.define(SOUL_AMOUNT, 1);
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        this.targetX = tag.getDouble("target_x");
        this.targetY = tag.getDouble("target_y");
        this.targetZ = tag.getDouble("target_z");
        this.lifetime = Math.max(1, tag.getInt("lifetime"));
        setSoulData(tag.getString("soul_type"), tag.getInt("soul_amount"));
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        tag.putDouble("target_x", this.targetX);
        tag.putDouble("target_y", this.targetY);
        tag.putDouble("target_z", this.targetZ);
        tag.putInt("lifetime", this.lifetime);
        tag.putString("soul_type", getSoulType());
        tag.putInt("soul_amount", getSoulAmount());
    }

    @Override
    public void tick() {
        super.tick();

        if (this.level().isClientSide) {
            return;
        }

        if (this.lifetime-- <= 0) {
            this.disintegrate();
            return;
        }

        double dx = this.targetX - this.getX();
        double dy = this.targetY - this.getY();
        double dz = this.targetZ - this.getZ();
        double distanceSqr = dx * dx + dy * dy + dz * dz;
        if (distanceSqr <= 0.04D) {
            this.disintegrate();
            return;
        }

        double distance = Math.sqrt(distanceSqr);
        double speed = 0.18D;
        double mx = dx / distance * speed;
        double my = dy / distance * speed;
        double mz = dz / distance * speed;

        this.setDeltaMovement(mx, my, mz);
        this.setPos(this.getX() + mx, this.getY() + my, this.getZ() + mz);

        if (this.level() instanceof ServerLevel serverLevel && this.tickCount % 2 == 0) {
            serverLevel.sendParticles(
                    net.minecraft.core.particles.ParticleTypes.SOUL_FIRE_FLAME,
                    this.getX(),
                    this.getY(),
                    this.getZ(),
                    Math.min(3, getSoulAmount()),
                    0.04D,
                    0.04D,
                    0.04D,
                    0.01D
            );
        }
    }

    private void disintegrate() {
        // beta.16 ObeliskSoulEntity#getDisintegrationSound
        this.level().playSound(null, this.getX(), this.getY(), this.getZ(),
                net.minecraft.sounds.SoundEvents.SOUL_ESCAPE, net.minecraft.sounds.SoundSource.AMBIENT, 1.0F, 1.5F);
        this.level().playSound(null, this.getX(), this.getY(), this.getZ(),
                ua.meekeyn.requiemunofficial.common.init.ModSounds.ENTITY_OBELISK_SOUL_DISINTEGRATE.get(),
                net.minecraft.sounds.SoundSource.AMBIENT, 1.0F, 1.0F);
        this.discard();
    }
}
