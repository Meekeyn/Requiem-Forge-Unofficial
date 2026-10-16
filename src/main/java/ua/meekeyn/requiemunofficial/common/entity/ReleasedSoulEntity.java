package ua.meekeyn.requiemunofficial.common.entity;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.level.Level;
import net.minecraftforge.network.NetworkHooks;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.phys.Vec3;
import ua.meekeyn.requiemunofficial.common.record.BodyRecordData;

import java.util.UUID;

public class ReleasedSoulEntity extends Entity {
    private static final byte SOUL_EXPIRED_STATUS = 1;
    private static final byte TELEPORT_AWAY_STATUS = 2;
    private static final byte MERGE_WITH_BODY_STATUS = 3;

    public static final byte BODY_MISSING = -1;
    public static final byte BODY_FOUND = 0;
    public static final byte BODY_ISEKAI = 1;

    private static final EntityDataAccessor<String> SOUL_TYPE =
            SynchedEntityData.defineId(ReleasedSoulEntity.class, EntityDataSerializers.STRING);
    private static final EntityDataAccessor<Integer> SOUL_AMOUNT =
            SynchedEntityData.defineId(ReleasedSoulEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Byte> BODY_STATUS =
            SynchedEntityData.defineId(ReleasedSoulEntity.class, EntityDataSerializers.BYTE);
    private UUID ownerId;
    private UUID ownerRecord;
    private int maxAge = 60;
    private int targetChangeCooldown = 0;
    private int targetChanges = 1;
    private Vec3 targetPos = Vec3.ZERO;

    public ReleasedSoulEntity(EntityType<? extends ReleasedSoulEntity> entityType, Level level) {
        super(entityType, level);
        this.noPhysics = true;
    }

    public void setOwner(UUID ownerId) {
        this.ownerId = ownerId;
    }

    public void setOwnerRecord(UUID ownerRecord) {
        this.ownerRecord = ownerRecord;
    }

    public void setSoulData(String soulType, int soulAmount) {
        this.entityData.set(SOUL_TYPE, soulType == null ? "empty" : soulType);
        this.entityData.set(SOUL_AMOUNT, Math.max(1, soulAmount));
    }

    public String getSoulType() {
        return this.entityData.get(SOUL_TYPE);
    }

    public int getSoulAmount() {
        return this.entityData.get(SOUL_AMOUNT);
    }

    @Override
    protected void defineSynchedData() {
        this.entityData.define(SOUL_TYPE, "empty");
        this.entityData.define(SOUL_AMOUNT, 1);
        this.entityData.define(BODY_STATUS, BODY_FOUND);
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        ownerId = tag.hasUUID("Owner") ? tag.getUUID("Owner") : null;
        ownerRecord = tag.hasUUID("OwnerRecord") ? tag.getUUID("OwnerRecord") : null;
        maxAge = tag.contains("MaxAge") ? tag.getInt("MaxAge") : Math.max(1, tag.getInt("Lifetime"));
        setSoulData(tag.getString("SoulType"), tag.getInt("SoulAmount"));
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        if (ownerId != null) {
            tag.putUUID("Owner", ownerId);
        }
        if (ownerRecord != null) {
            tag.putUUID("OwnerRecord", ownerRecord);
        }
        tag.putInt("MaxAge", maxAge);
        tag.putString("SoulType", getSoulType());
        tag.putInt("SoulAmount", getSoulAmount());
    }

    @Override
    public void tick() {
        super.tick();
        if (!level().isClientSide) {
            serverTickLifecycle();
        }
        steerTowardsTarget();
        setPos(
                getX() + getDeltaMovement().x,
                getY() + getDeltaMovement().y,
                getZ() + getDeltaMovement().z
        );
        if (level() instanceof ServerLevel serverLevel && tickCount % 2 == 0) {
            spawnTrailParticle(serverLevel);
        }
    }

    private void serverTickLifecycle() {
        byte status = computeBodyStatus();
        setBodyStatus(status);
        if (status == BODY_FOUND) {
            maxAge = -1;
            LivingEntityRef owner = resolveOwnerBody();
            if (owner != null && owner.entity().getBoundingBox().intersects(getBoundingBox())) {
                releaseSoul(MERGE_WITH_BODY_STATUS);
                BodyRecordData.get(level()).updateRecord(owner.entity());
            }
            return;
        }

        if (maxAge < 0 || maxAge > 60) {
            maxAge = 60;
        }
        if (maxAge-- <= 0) {
            expire();
        }
    }

    private byte computeBodyStatus() {
        if (ownerRecord == null || !(level() instanceof ServerLevel serverLevel)) {
            return BODY_MISSING;
        }
        BodyRecordData data = BodyRecordData.get(serverLevel);
        var record = data.getRecord(ownerRecord);
        if (record.isEmpty()) {
            return BODY_MISSING;
        }
        var resolved = data.resolveLoadedBody(serverLevel.getServer(), ownerRecord);
        if (resolved.isPresent()
                && resolved.get().level() == level()
                && resolved.get().distanceTo(this) < 100.0F) {
            return BODY_FOUND;
        }
        return BODY_ISEKAI;
    }

    private LivingEntityRef resolveOwnerBody() {
        if (ownerRecord == null || !(level() instanceof ServerLevel serverLevel)) {
            return null;
        }
        return BodyRecordData.get(serverLevel)
                .resolveLoadedBody(serverLevel.getServer(), ownerRecord)
                .map(LivingEntityRef::new)
                .orElse(null);
    }

    private void releaseSoul(byte status) {
        if (ownerRecord != null) {
            BodyRecordData.get(level()).markReleasedSoulPending(ownerRecord);
        }
        level().broadcastEntityEvent(this, status);
        discard();
    }

    private void expire() {
        if (getBodyStatus() == BODY_ISEKAI) {
            releaseSoul(TELEPORT_AWAY_STATUS);
            return;
        }
        if (ownerRecord != null) {
            BodyRecordData.get(level()).invalidate(ownerRecord);
        }
        level().broadcastEntityEvent(this, SOUL_EXPIRED_STATUS);
        discard();
    }

    private void steerTowardsTarget() {
        if (targetChangeCooldown-- <= 0 || targetPos == Vec3.ZERO) {
            targetChanges++;
            targetChangeCooldown = 20 + random.nextInt(25);
            targetPos = selectNextTarget();
        }

        Vec3 desired = targetPos.subtract(position());
        if (desired.lengthSqr() > 1.0E-4D) {
            double speed = getBodyStatus() == BODY_FOUND ? 0.11D : 0.04D;
            setDeltaMovement(getDeltaMovement().scale(0.82D).add(desired.normalize().scale(speed)));
        } else {
            setDeltaMovement(getDeltaMovement().scale(0.75D).add(0.0D, 0.01D, 0.0D));
        }
        double maxSpeed = getBodyStatus() == BODY_FOUND ? 0.55D : 0.4D;
        if (getDeltaMovement().lengthSqr() > maxSpeed * maxSpeed) {
            setDeltaMovement(getDeltaMovement().normalize().scale(maxSpeed));
        }
    }

    private Vec3 selectNextTarget() {
        byte status = getBodyStatus();
        if (status == BODY_FOUND) {
            LivingEntityRef owner = resolveOwnerBody();
            if (owner != null) {
                return selectPosTowards(owner.entity().position().add(0.0D, owner.entity().getBbHeight() * 0.5D, 0.0D));
            }
        }
        if (status == BODY_ISEKAI) {
            return position().add(0.0D, random.nextGaussian() * 0.1D, 0.0D);
        }
        return position().add(random.nextGaussian(), random.nextGaussian(), random.nextGaussian());
    }

    private Vec3 selectPosTowards(Vec3 target) {
        double distance = position().distanceTo(target);
        double step = Math.min(15.0D, distance);
        if (step <= 1.0E-4D) {
            return target;
        }
        double convergence = Math.max(1.0D, targetChanges);
        double fuzzyFactor = (step * 1.5D) / convergence;
        Vec3 towards = target.subtract(position()).normalize().scale(step);
        Vec3 fuzzy = new Vec3(
                towards.x * Math.abs(1.0D + random.nextGaussian() * fuzzyFactor),
                towards.y * Math.abs(1.0D + random.nextGaussian() * fuzzyFactor),
                towards.z * Math.abs(1.0D + random.nextGaussian() * fuzzyFactor)
        );
        return position().add(fuzzy);
    }

    private void setBodyStatus(byte status) {
        this.entityData.set(BODY_STATUS, status);
    }

    public byte getBodyStatus() {
        return this.entityData.get(BODY_STATUS);
    }

    private void spawnTrailParticle(ServerLevel serverLevel) {
        if (getBodyStatus() == BODY_ISEKAI) {
            serverLevel.sendParticles(
                    ParticleTypes.PORTAL,
                    getX(),
                    getY(),
                    getZ(),
                    2,
                    0.12D,
                    0.12D,
                    0.12D,
                    0.1D
            );
            return;
        }
        if (getBodyStatus() == BODY_MISSING) {
            serverLevel.sendParticles(
                    ParticleTypes.SMOKE,
                    getX(),
                    getY(),
                    getZ(),
                    2,
                    0.08D,
                    0.08D,
                    0.08D,
                    0.02D
            );
            return;
        }
        serverLevel.sendParticles(
                ParticleTypes.SOUL,
                getX(),
                getY(),
                getZ(),
                2,
                0.08D,
                0.08D,
                0.08D,
                0.01D
        );
    }

    @Override
    public void handleEntityEvent(byte id) {
        if (id == TELEPORT_AWAY_STATUS) {
            level().playLocalSound(getX(), getY(), getZ(), SoundEvents.ENDERMAN_TELEPORT, SoundSource.NEUTRAL, 1.0F, 1.0F, false);
            for (double angle = 0.0D; angle < Math.PI * 2.0D; angle += Math.PI / 20.0D) {
                level().addParticle(ParticleTypes.PORTAL, getX() + Math.cos(angle) * 5.0D, getY() - 0.4D, getZ() + Math.sin(angle) * 5.0D, Math.cos(angle) * -5.0D, 0.0D, Math.sin(angle) * -5.0D);
                level().addParticle(ParticleTypes.PORTAL, getX() + Math.cos(angle) * 5.0D, getY() - 0.4D, getZ() + Math.sin(angle) * 5.0D, Math.cos(angle) * -7.0D, 0.0D, Math.sin(angle) * -7.0D);
            }
            return;
        }
        if (id == MERGE_WITH_BODY_STATUS) {
            level().playLocalSound(getX(), getY(), getZ(), SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.NEUTRAL, 1.0F, 1.0F, false);
            for (int i = 0; i < 25; i++) {
                level().addParticle(ParticleTypes.SOUL, getX() + random.nextGaussian() / 15.0D, getY() + random.nextGaussian() / 15.0D, getZ() + random.nextGaussian() / 15.0D, 0.0D, 0.2D, 0.0D);
            }
            return;
        }
        super.handleEntityEvent(id);
    }

    @Override
    public Packet<ClientGamePacketListener> getAddEntityPacket() {
        return NetworkHooks.getEntitySpawningPacket(this);
    }

    private record LivingEntityRef(net.minecraft.world.entity.LivingEntity entity) {
    }
}
