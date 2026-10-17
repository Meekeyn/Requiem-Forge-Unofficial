package ua.meekeyn.requiemunofficial.common.soul;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.item.ItemTossEvent;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.event.entity.living.LivingChangeTargetEvent;
import net.minecraftforge.event.entity.living.LivingFallEvent;
import net.minecraftforge.event.entity.player.AttackEntityEvent;
import net.minecraftforge.event.entity.player.EntityItemPickupEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import ua.meekeyn.requiemunofficial.RequiemUnofficial;
import ua.meekeyn.requiemunofficial.common.diagnostic.VagrantDiagnostics;
import ua.meekeyn.requiemunofficial.common.possession.PossessionManager;

@Mod.EventBusSubscriber(modid = RequiemUnofficial.MODID)
public final class VagrantEvents {

    private static final String FLIGHT_GRANTED = "RequiemVagrantFlight";
    private static final String FLIGHT_ENTRY_NORMALIZED = "RequiemVagrantFlightEntryNormalized";
    private static final String ORIGINAL_MAYFLY = "RequiemOriginalMayfly";
    private static final String ORIGINAL_FLYING = "RequiemOriginalFlying";
    private static final String ORIGINAL_NO_PHYSICS = "RequiemOriginalNoPhysics";
    private static final String ORIGINAL_NO_GRAVITY = "RequiemOriginalNoGravity";
    private static final String VAGRANT_JUMPING = "RequiemVagrantJumping";
    private static final String VAGRANT_SHIFTING = "RequiemVagrantShifting";
    private static final String VAGRANT_PHASE_ATTEMPT = "RequiemVagrantPhaseAttempt";
    private VagrantEvents() {
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onLeftClickBlock(PlayerInteractEvent.LeftClickBlock event) {
        if (isRestrictedFreeVagrant(event.getEntity())) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        if (isRestrictedFreeVagrant(event.getEntity())) {
            event.setCanceled(true);
            event.setCancellationResult(InteractionResult.FAIL);
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onRightClickItem(PlayerInteractEvent.RightClickItem event) {
        if (isRestrictedFreeVagrant(event.getEntity())) {
            event.setCanceled(true);
            event.setCancellationResult(InteractionResult.FAIL);
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onEntityInteract(PlayerInteractEvent.EntityInteract event) {
        if (isRestrictedFreeVagrant(event.getEntity())) {
            if (event.getTarget() instanceof net.minecraft.world.item.trading.Merchant) {
                return;
            }
            event.setCanceled(true);
            event.setCancellationResult(InteractionResult.FAIL);
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onEntityInteractSpecific(PlayerInteractEvent.EntityInteractSpecific event) {
        if (isRestrictedFreeVagrant(event.getEntity())) {
            if (event.getTarget() instanceof net.minecraft.world.item.trading.Merchant) {
                return;
            }
            event.setCanceled(true);
            event.setCancellationResult(InteractionResult.FAIL);
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onBreakBlock(BlockEvent.BreakEvent event) {
        if (isRestrictedFreeVagrant(event.getPlayer())) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onAttackEntity(AttackEntityEvent event) {
        if (isRestrictedFreeVagrant(event.getEntity())) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onItemToss(ItemTossEvent event) {
        if (VagrantRules.isRestricted(event.getPlayer())) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onItemPickup(EntityItemPickupEvent event) {
        if (VagrantRules.isRestricted(event.getEntity())) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onLivingFall(LivingFallEvent event) {
        if (event.getEntity() instanceof ServerPlayer player && suppressFallForVagrant(player)) {
            event.setCanceled(true);
            event.setDistance(0.0F);
            event.setDamageMultiplier(0.0F);
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onLivingAttack(LivingAttackEvent event) {
        if (event.getEntity() instanceof ServerPlayer player && shouldSuppressDamageForVagrant(player)) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onLivingChangeTarget(LivingChangeTargetEvent event) {
        LivingEntity target = event.getNewTarget();
        if (target instanceof ServerPlayer player && VagrantRules.isRestricted(player)) {
            event.setCanceled(true);
            event.setNewTarget(null);
            if (event.getEntity() instanceof Mob mob) {
                mob.setTarget(null);
            }
        }
    }

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.player.level().isClientSide()
                || !(event.player instanceof ServerPlayer player)) {
            return;
        }

        if (event.phase == TickEvent.Phase.START) {
            primeVagrantPhasePhysics(player);
            return;
        }

        if (event.phase != TickEvent.Phase.END) {
            return;
        }

        updateVagrantState(player);
    }

    public static void updateVagrantStateForTest(ServerPlayer player) {
        updateVagrantState(player);
    }

    public static void refreshAfterSoulStateChange(ServerPlayer player) {
        updateVagrantState(player);
    }

    public static boolean suppressFallForVagrant(ServerPlayer player) {
        if (!VagrantRules.isRestricted(player)) {
            return false;
        }
        player.fallDistance = 0.0F;
        return true;
    }

    public static boolean shouldSuppressDamageForVagrant(ServerPlayer player) {
        return VagrantRules.isRestricted(player);
    }

    public static void setVagrantInput(ServerPlayer player, boolean jumping, boolean shifting, boolean phaseAttempt) {
        player.getPersistentData().putBoolean(VAGRANT_JUMPING, jumping);
        player.getPersistentData().putBoolean(VAGRANT_SHIFTING, shifting);
        player.getPersistentData().putBoolean(VAGRANT_PHASE_ATTEMPT, phaseAttempt);
    }

    private static void updateVagrantState(ServerPlayer player) {
        if (player.isSpectator()) {
            clearStoredVagrantState(player);
            return;
        }

        if (VagrantRules.isRestricted(player)) {
            boolean alreadyGranted = player.getPersistentData().getBoolean(FLIGHT_GRANTED);
            if (!alreadyGranted) {
                player.getPersistentData().putBoolean(ORIGINAL_MAYFLY, player.getAbilities().mayfly);
                player.getPersistentData().putBoolean(ORIGINAL_FLYING, player.getAbilities().flying);
                player.getPersistentData().putBoolean(ORIGINAL_NO_PHYSICS, player.noPhysics);
                player.getPersistentData().putBoolean(ORIGINAL_NO_GRAVITY, player.isNoGravity());
            }
            boolean abilitiesChanged = !player.getAbilities().mayfly
                    || !alreadyGranted;
            player.getPersistentData().putBoolean(FLIGHT_GRANTED, true);
            if (!player.getPersistentData().getBoolean(FLIGHT_ENTRY_NORMALIZED)) {
                abilitiesChanged |= player.getAbilities().flying;
                player.getAbilities().flying = false;
                player.getPersistentData().putBoolean(FLIGHT_ENTRY_NORMALIZED, true);
            }
            if (!player.isCreative() && !player.isSpectator()) {
                player.getAbilities().mayfly = true;
            }
            player.fallDistance = 0.0F;
            refreshSoulHunger(player);
            if (hasLiveConnection(player)) {
                player.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, 260, 0, true, false, false));
            }
            applyVagrantPhasePhysics(player);
            if (VagrantRules.isInsideNonLeafPhaseableBlock(player) && hasLiveConnection(player)) {
                player.addEffect(new MobEffectInstance(MobEffects.DARKNESS, 40, 0, true, false, false));
            }
            player.setInvisible(false);
            clearVagrantTargets(player);
            if (abilitiesChanged) {
                updateAbilities(player);
            }
            return;
        }

        if (player.getPersistentData().getBoolean(FLIGHT_GRANTED)) {
            player.getPersistentData().remove(FLIGHT_GRANTED);
            player.getPersistentData().remove(FLIGHT_ENTRY_NORMALIZED);
            if (!player.isCreative() && !player.isSpectator()) {
                player.getAbilities().mayfly = player.getPersistentData().getBoolean(ORIGINAL_MAYFLY);
                player.getAbilities().flying = player.getAbilities().mayfly
                        && player.getPersistentData().getBoolean(ORIGINAL_FLYING);
            }
            player.getPersistentData().remove(ORIGINAL_MAYFLY);
            player.getPersistentData().remove(ORIGINAL_FLYING);
            if (!player.isSpectator()) {
                player.noPhysics = player.getPersistentData().getBoolean(ORIGINAL_NO_PHYSICS);
            }
            player.getPersistentData().remove(ORIGINAL_NO_PHYSICS);
            player.setNoGravity(player.getPersistentData().getBoolean(ORIGINAL_NO_GRAVITY));
            player.getPersistentData().remove(ORIGINAL_NO_GRAVITY);
            player.getPersistentData().remove(VAGRANT_JUMPING);
            player.getPersistentData().remove(VAGRANT_SHIFTING);
            player.getPersistentData().remove(VAGRANT_PHASE_ATTEMPT);
            VagrantRules.clearPhaseState(player);
            if (SoulManager.getPossessedEntityId(player) == null) {
                player.setInvisible(false);
            }
            updateAbilities(player);
            return;
        }

        if (SoulManager.getState(player) != SoulState.VAGRANT
                && SoulManager.getPossessedEntityId(player) == null) {
            boolean abilitiesChanged = false;
            if (!player.isCreative() && !player.isSpectator()
                    && (player.getAbilities().mayfly || player.getAbilities().flying)) {
                player.getAbilities().mayfly = false;
                player.getAbilities().flying = false;
                abilitiesChanged = true;
            }
            if (player.isInvisible()) {
                player.setInvisible(false);
            }
            if (!player.isSpectator() && player.noPhysics) {
                player.noPhysics = false;
            }
            if (player.isNoGravity()) {
                player.setNoGravity(false);
            }
            player.getPersistentData().remove(VAGRANT_JUMPING);
            player.getPersistentData().remove(VAGRANT_SHIFTING);
            player.getPersistentData().remove(VAGRANT_PHASE_ATTEMPT);
            VagrantRules.clearPhaseState(player);
            if (abilitiesChanged) {
                updateAbilities(player);
            }
        }
    }

    private static void primeVagrantPhasePhysics(ServerPlayer player) {
        if (!VagrantRules.isRestricted(player)) {
            return;
        }
        updatePhaseCharge(player);
        applyVagrantPhasePhysics(player);
        player.fallDistance = 0.0F;
    }

    private static void applyVagrantPhasePhysics(ServerPlayer player) {
        boolean charged = VagrantRules.isPhaseCharged(player);
        boolean leafActive = VagrantRules.isLeafPhaseActive(player);
        boolean insideBlock = VagrantRules.isInsidePhaseableBlock(player);
        boolean phaseMovement = VagrantRules.shouldUsePhaseMovement(player);
        player.noPhysics = false;
        if (phaseMovement && (charged || leafActive || insideBlock)) {
            VagrantRules.updatePhaseLowering(player, false);
            applyEntryPhaseMovement(player, charged || VagrantRules.isLeafPhaseActive(player), insideBlock);
            applyManualVerticalPhaseMovement(player);
            limitPhaseSpeed(player);
            preventUncommandedPhaseDescent(player);
        } else {
            VagrantRules.updatePhaseLowering(player, false);
        }
        player.setNoGravity(phaseMovement);
        VagrantDiagnostics.logPhaseSnapshot("SERVER", player, "applyVagrantPhasePhysics");
    }

    private static void updatePhaseCharge(ServerPlayer player) {
        boolean insideBlock = VagrantRules.isInsidePhaseableBlock(player);
        boolean insideLeaves = VagrantRules.isInsideLeaves(player);
        boolean attempting = player.getPersistentData().getBoolean(VAGRANT_PHASE_ATTEMPT)
                && (VagrantRules.shouldChargePhaseEntry(player) || player.horizontalCollision);
        player.getPersistentData().putBoolean(VagrantRules.PHASE_ATTEMPTING, attempting);
        if (insideBlock) {
            if (VagrantRules.isInsideNonLeafPhaseableBlock(player) && !insideLeaves) {
                if (VagrantRules.isPhaseCharged(player)) {
                    player.getPersistentData().putInt(VagrantRules.PHASE_CHARGE_TICKS, VagrantRules.PHASE_REQUIRED_TICKS);
                } else {
                    player.getPersistentData().putInt(VagrantRules.PHASE_CHARGE_TICKS, 0);
                }
                if (!VagrantRules.isLeafPhaseActive(player)) {
                    player.getPersistentData().putInt(VagrantRules.LEAF_PHASE_CHARGE_TICKS, 0);
                    VagrantRules.setLeafPhaseActive(player, false);
                }
            } else if (insideLeaves) {
                int ticks = player.getPersistentData().getInt(VagrantRules.PHASE_CHARGE_TICKS);
                player.getPersistentData().putInt(
                        VagrantRules.PHASE_CHARGE_TICKS,
                        Math.min(VagrantRules.PHASE_REQUIRED_TICKS, ticks + 1)
                );
            }
            updateLeafPhaseCharge(player, attempting, insideLeaves);
        } else if (attempting || VagrantRules.isPhaseCharged(player) && VagrantRules.shouldChargePhaseEntry(player)) {
            int ticks = player.getPersistentData().getInt(VagrantRules.PHASE_CHARGE_TICKS);
            player.getPersistentData().putInt(
                    VagrantRules.PHASE_CHARGE_TICKS,
                    Math.min(VagrantRules.PHASE_REQUIRED_TICKS, ticks + 1)
            );
            updateLeafPhaseCharge(player, attempting, insideLeaves);
        } else {
            player.getPersistentData().putInt(VagrantRules.PHASE_CHARGE_TICKS, 0);
            updateLeafPhaseCharge(player, false, insideLeaves);
        }
    }

    private static void updateLeafPhaseCharge(ServerPlayer player, boolean attempting, boolean insideLeaves) {
        if (insideLeaves && VagrantRules.isLeafPhaseActive(player)) {
            player.getPersistentData().putInt(
                    VagrantRules.LEAF_PHASE_CHARGE_TICKS,
                    VagrantRules.LEAF_PHASE_REQUIRED_TICKS
            );
            player.getPersistentData().putInt(
                    VagrantRules.LEAF_PHASE_GRACE_TICK_DATA,
                    VagrantRules.LEAF_PHASE_GRACE_TICKS
            );
            VagrantRules.setLeafPhaseActive(player, true);
            return;
        }

        if (attempting && (insideLeaves
                || VagrantRules.isTouchingLeaves(player)
                || VagrantRules.isLookingIntoLeaves(player))) {

            int ticks = player.getPersistentData().getInt(VagrantRules.LEAF_PHASE_CHARGE_TICKS);
            int updated = Math.min(VagrantRules.LEAF_PHASE_REQUIRED_TICKS, ticks + 1);

            player.getPersistentData().putInt(VagrantRules.LEAF_PHASE_CHARGE_TICKS, updated);

            if (updated >= VagrantRules.LEAF_PHASE_REQUIRED_TICKS) {
                player.getPersistentData().putInt(
                        VagrantRules.LEAF_PHASE_GRACE_TICK_DATA,
                        VagrantRules.LEAF_PHASE_GRACE_TICKS
                );
                VagrantRules.setLeafPhaseActive(player, true);
            }
            return;
        }

        int grace = player.getPersistentData().getInt(VagrantRules.LEAF_PHASE_GRACE_TICK_DATA);

        if (grace > 0) {
            grace--;
            player.getPersistentData().putInt(VagrantRules.LEAF_PHASE_GRACE_TICK_DATA, grace);
            player.getPersistentData().putInt(
                    VagrantRules.LEAF_PHASE_CHARGE_TICKS,
                    VagrantRules.LEAF_PHASE_REQUIRED_TICKS
            );
            VagrantRules.setLeafPhaseActive(player, true);
            return;
        }

        player.getPersistentData().putInt(VagrantRules.LEAF_PHASE_CHARGE_TICKS, 0);
        player.getPersistentData().putInt(VagrantRules.LEAF_PHASE_GRACE_TICK_DATA, 0);
        VagrantRules.setLeafPhaseActive(player, false);
    }

    private static void applyManualVerticalPhaseMovement(ServerPlayer player) {
        if (!VagrantRules.isInsidePhaseableBlock(player)) {
            return;
        }

        boolean jumping = player.getPersistentData().getBoolean(VAGRANT_JUMPING);
        boolean shifting = player.isShiftKeyDown() || player.getPersistentData().getBoolean(VAGRANT_SHIFTING);
        Vec3 movement = player.getDeltaMovement();
        if (jumping == shifting) {
            player.setDeltaMovement(movement.x, 0.0D, movement.z);
        } else if (jumping) {
            player.setDeltaMovement(movement.x, VagrantRules.PHASE_SPEED, movement.z);
        } else {
            player.setDeltaMovement(movement.x, -VagrantRules.PHASE_SPEED, movement.z);
        }
    }

    private static void applyEntryPhaseMovement(ServerPlayer player, boolean charged, boolean insideBlock) {
        if (!charged || insideBlock) {
            return;
        }

        Vec3 look = player.getLookAngle();
        boolean jumping = player.getPersistentData().getBoolean(VAGRANT_JUMPING);
        boolean shifting = player.isShiftKeyDown() || player.getPersistentData().getBoolean(VAGRANT_SHIFTING);
        if ((jumping && look.y > 0.45D) || (shifting && look.y < -0.45D)) {
            Vec3 push = look.normalize().scale(VagrantRules.PHASE_SPEED);
            player.setDeltaMovement(push.x, push.y, push.z);
            return;
        }

        Vec3 horizontal = new Vec3(look.x, 0.0D, look.z);
        if (horizontal.lengthSqr() < 1.0E-5D) {
            return;
        }

        Vec3 push = horizontal.normalize().scale(VagrantRules.PHASE_SPEED);
        player.setDeltaMovement(push.x, 0.0D, push.z);
    }

    private static void clearStoredVagrantState(ServerPlayer player) {
        if (!player.getPersistentData().getBoolean(FLIGHT_GRANTED)) {
            return;
        }
        player.getPersistentData().remove(FLIGHT_GRANTED);
        player.getPersistentData().remove(FLIGHT_ENTRY_NORMALIZED);
        player.getPersistentData().remove(ORIGINAL_MAYFLY);
        player.getPersistentData().remove(ORIGINAL_FLYING);
        player.getPersistentData().remove(ORIGINAL_NO_PHYSICS);
        player.getPersistentData().remove(ORIGINAL_NO_GRAVITY);
        player.getPersistentData().remove(VAGRANT_JUMPING);
        player.getPersistentData().remove(VAGRANT_SHIFTING);
        player.getPersistentData().remove(VAGRANT_PHASE_ATTEMPT);
        VagrantRules.clearPhaseState(player);
    }

    private static void updateAbilities(ServerPlayer player) {
        if (hasLiveConnection(player)) {
            player.onUpdateAbilities();
        }
    }

    private static boolean hasLiveConnection(ServerPlayer player) {
        return player.connection != null && player.connection.connection != null;
    }

    private static void refreshSoulHunger(ServerPlayer player) {
        // Безтілесна душа не витрачає голод (див. FoodDataMixin), але збережене значення
        // не перезаписується: після виходу з тіла-їдця відновлюється попередній голод гравця.
        player.getFoodData().setExhaustion(0.0F);
    }

    private static void limitPhaseSpeed(ServerPlayer player) {
        Vec3 movement = player.getDeltaMovement();
        double maxSpeedSqr = VagrantRules.PHASE_SPEED * VagrantRules.PHASE_SPEED;
        if (movement.lengthSqr() > maxSpeedSqr) {
            player.setDeltaMovement(movement.normalize().scale(VagrantRules.PHASE_SPEED));
        }
    }

    private static void preventUncommandedPhaseDescent(ServerPlayer player) {
        if (!VagrantRules.isInsidePhaseableBlock(player)
                || player.isShiftKeyDown()
                || player.getPersistentData().getBoolean(VAGRANT_SHIFTING)) {
            return;
        }

        Vec3 movement = player.getDeltaMovement();
        if (movement.y < 0.0D) {
            player.setDeltaMovement(movement.x, 0.0D, movement.z);
        }
    }

    private static void clearVagrantTargets(ServerPlayer player) {
        AABB searchBox = player.getBoundingBox().inflate(64.0D);
        for (Mob mob : player.level().getEntitiesOfClass(Mob.class, searchBox, mob -> mob.getTarget() == player)) {
            mob.setTarget(null);
            mob.getNavigation().stop();
        }
    }

    private static boolean isRestrictedFreeVagrant(net.minecraft.world.entity.player.Player player) {
        return VagrantRules.isRestricted(player) && !PossessionManager.isUsingBeta16PlayerBody(player);
    }
}
