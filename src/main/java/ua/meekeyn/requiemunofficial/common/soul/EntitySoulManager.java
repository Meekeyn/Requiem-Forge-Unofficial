package ua.meekeyn.requiemunofficial.common.soul;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraftforge.event.entity.EntityLeaveLevelEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import ua.meekeyn.requiemunofficial.RequiemUnofficial;
import ua.meekeyn.requiemunofficial.common.entity.PlayerShellEntity;
import ua.meekeyn.requiemunofficial.common.record.BodyRecordData;
import ua.meekeyn.requiemunofficial.common.util.ModTags;

import java.util.UUID;

@Mod.EventBusSubscriber(modid = RequiemUnofficial.MODID)
public final class EntitySoulManager {
    private static final String HAS_SOUL = "RequiemHasSoul";
    private static final String SOUL_INITIALIZED = "RequiemSoulInitialized";
    private static final String ORIGINAL_NO_AI = "RequiemSoulOriginalNoAi";
    private static final String INERT_APPLIED = "RequiemSoulInertApplied";
    private static final String POSSESSED = "RequiemPossessed";
    private static final String INERT_Y_ROT = "RequiemSoulInertYRot";
    private static final String INERT_X_ROT = "RequiemSoulInertXRot";

    private EntitySoulManager() {
    }

    public static boolean hasSoul(LivingEntity entity) {
        if (isNaturallySoulless(entity)) {
            return false;
        }
        if (!entity.getPersistentData().getBoolean(SOUL_INITIALIZED)) {
            entity.getPersistentData().putBoolean(SOUL_INITIALIZED, true);
            entity.getPersistentData().putBoolean(HAS_SOUL, true);
        }
        return entity.getPersistentData().getBoolean(HAS_SOUL);
    }

    public static void removeSoul(LivingEntity entity) {
        entity.getPersistentData().putBoolean(SOUL_INITIALIZED, true);
        entity.getPersistentData().putBoolean(HAS_SOUL, false);
        if (!entity.level().isClientSide()) {
            BodyRecordData.get(entity.level()).getOrCreateRecord(entity);
        }
        immobilizeSoullessBody(entity);
    }

    public static void restoreSoul(LivingEntity entity) {
        if (isNaturallySoulless(entity)) {
            return;
        }
        entity.getPersistentData().putBoolean(SOUL_INITIALIZED, true);
        entity.getPersistentData().putBoolean(HAS_SOUL, true);
        if (entity instanceof Mob mob && entity.getPersistentData().contains(ORIGINAL_NO_AI)) {
            mob.setNoAi(entity.getPersistentData().getBoolean(ORIGINAL_NO_AI));
            entity.getPersistentData().remove(ORIGINAL_NO_AI);
        }
        entity.getPersistentData().remove(INERT_APPLIED);
        entity.getPersistentData().remove(INERT_Y_ROT);
        entity.getPersistentData().remove(INERT_X_ROT);
    }

    public static boolean canBePossessionHost(LivingEntity entity) {
        return isNaturallySoulless(entity) || !hasSoul(entity);
    }

    public static boolean isInertSoullessBody(LivingEntity entity) {
        return !isNaturallySoulless(entity)
                && entity.getPersistentData().getBoolean(SOUL_INITIALIZED)
                && !entity.getPersistentData().getBoolean(HAS_SOUL);
    }

    @SubscribeEvent
    public static void onLivingTick(LivingEvent.LivingTickEvent event) {
        if (event.getEntity().level().isClientSide()) {
            return;
        }
        if (BodyRecordData.getEntityRecordId(event.getEntity()) != null) {
            BodyRecordData.get(event.getEntity().level()).updateRecord(event.getEntity());
        }
        freezeIfInert(event.getEntity());
    }

    @SubscribeEvent
    public static void onLivingDeath(LivingDeathEvent event) {
        if (event.getEntity().level().isClientSide()) {
            return;
        }
        UUID recordId = BodyRecordData.getEntityRecordId(event.getEntity());
        if (recordId != null) {
            BodyRecordData.get(event.getEntity().level()).invalidate(recordId);
        }
    }

    @SubscribeEvent
    public static void onEntityLeaveLevel(EntityLeaveLevelEvent event) {
        if (event.getLevel().isClientSide() || !(event.getEntity() instanceof LivingEntity living)) {
            return;
        }
        UUID recordId = BodyRecordData.getEntityRecordId(living);
        if (recordId != null && living.isRemoved() && isFinalBodyRemoval(living)) {
            BodyRecordData.get(event.getLevel()).invalidate(recordId);
        }
    }

    private static boolean isFinalBodyRemoval(LivingEntity living) {
        Entity.RemovalReason reason = living.getRemovalReason();
        return reason != Entity.RemovalReason.UNLOADED_TO_CHUNK
                && reason != Entity.RemovalReason.CHANGED_DIMENSION;
    }

    @SubscribeEvent
    public static void onLivingAttack(LivingAttackEvent event) {
        Entity attacker = event.getSource().getEntity();
        if (attacker instanceof LivingEntity living
                && isInertSoullessBody(living)
                && !living.getPersistentData().getBoolean(POSSESSED)) {
            event.setCanceled(true);
        }
    }

    private static void freezeIfInert(LivingEntity entity) {
        if (!isInertSoullessBody(entity) || entity.getPersistentData().getBoolean(POSSESSED)) {
            return;
        }
        stopSoullessBodyActivity(entity);
        applyInertBodyPhysics(entity);
        if (entity.getPersistentData().getBoolean(INERT_APPLIED)) {
            return;
        }
        immobilizeSoullessBody(entity);
    }

    private static boolean isNaturallySoulless(LivingEntity entity) {
        return entity instanceof PlayerShellEntity || entity.getType().is(ModTags.SOULLESS);
    }

    private static void immobilizeSoullessBody(LivingEntity entity) {
        if (entity.getPersistentData().getBoolean(POSSESSED)) {
            return;
        }
        entity.getPersistentData().putBoolean(INERT_APPLIED, true);
        entity.getPersistentData().putFloat(INERT_Y_ROT, entity.getYRot());
        entity.getPersistentData().putFloat(INERT_X_ROT, entity.getXRot());
        entity.noPhysics = false;
        entity.setNoGravity(false);
        stopSoullessBodyActivity(entity);
    }

    private static void stopSoullessBodyActivity(LivingEntity entity) {
        entity.setSprinting(false);
        entity.setJumping(false);
        entity.xxa = 0.0F;
        entity.yya = 0.0F;
        entity.zza = 0.0F;
        entity.noPhysics = false;
        entity.setNoGravity(false);
        if (entity.getPersistentData().contains(INERT_Y_ROT)) {
            float yRot = entity.getPersistentData().getFloat(INERT_Y_ROT);
            float xRot = entity.getPersistentData().getFloat(INERT_X_ROT);
            entity.setYRot(yRot);
            entity.setYHeadRot(yRot);
            entity.yBodyRot = yRot;
            entity.setXRot(xRot);
        }
        if (entity instanceof Mob mob) {
            mob.setTarget(null);
            mob.getNavigation().stop();
            if (!entity.getPersistentData().contains(ORIGINAL_NO_AI)) {
                entity.getPersistentData().putBoolean(ORIGINAL_NO_AI, mob.isNoAi());
            }
            mob.setNoAi(true);
        }
    }

    private static void applyInertBodyPhysics(LivingEntity entity) {
        if (entity.isPassenger()) {
            return;
        }
        if (entity instanceof Mob mob && mob.isNoAi()) {
            mob.setNoAi(false);
            entity.travel(net.minecraft.world.phys.Vec3.ZERO);
            mob.setNoAi(true);
            return;
        }
        entity.travel(net.minecraft.world.phys.Vec3.ZERO);
    }

}
