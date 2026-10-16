package ua.meekeyn.requiemunofficial.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import ua.meekeyn.requiemunofficial.RequiemUnofficial;
import ua.meekeyn.requiemunofficial.common.diagnostic.VagrantDiagnostics;
import ua.meekeyn.requiemunofficial.common.network.ModNetwork;
import ua.meekeyn.requiemunofficial.common.network.VagrantInputPacket;
import ua.meekeyn.requiemunofficial.common.possession.PossessionManager;
import ua.meekeyn.requiemunofficial.common.soul.SoulManager;
import ua.meekeyn.requiemunofficial.common.soul.VagrantRules;

import java.util.UUID;

@Mod.EventBusSubscriber(modid = RequiemUnofficial.MODID, value = Dist.CLIENT)
public final class ClientVagrantState {

    private static UUID appliedPlayerId;
    private static boolean originalNoPhysics;
    private static boolean originalNoGravity;
    private static int phaseChargeTicks;
    private static int leafPhaseChargeTicks;
    private static int leafPhaseGraceTicks;

    private ClientVagrantState() {
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;
        if (player == null) {
            appliedPlayerId = null;
            originalNoPhysics = false;
            originalNoGravity = false;
            phaseChargeTicks = 0;
            leafPhaseChargeTicks = 0;
            leafPhaseGraceTicks = 0;
            return;
        }

        if (event.phase == TickEvent.Phase.START) {
            primeVagrantPhasePhysics(minecraft, player);
            return;
        }

        if (event.phase != TickEvent.Phase.END) {
            return;
        }

        UUID possessedId = SoulManager.getPossessedEntityId(player);
        if (player.isSpectator()) {
            VagrantRules.clearPhaseState(player);
            appliedPlayerId = null;
            originalNoPhysics = false;
            originalNoGravity = false;
            phaseChargeTicks = 0;
            leafPhaseChargeTicks = 0;
            leafPhaseGraceTicks = 0;
            return;
        }

        if (possessedId != null) {
            VagrantRules.clearPhaseState(player);
            LivingEntity host = ClientPossessionRenderHelper.getPossessedHost(player);
            if (host != null && PossessionManager.usesBeta16PlayerBody(host)) {
                if (minecraft.getCameraEntity() != player) {
                    minecraft.setCameraEntity(player);
                }
                PossessionManager.syncBeta16HostToPlayer(player, host);
            } else if (host != null && minecraft.getCameraEntity() != host) {
                minecraft.setCameraEntity(host);
            }
            if (player.getUUID().equals(appliedPlayerId)) {
                appliedPlayerId = null;
                originalNoPhysics = false;
                phaseChargeTicks = 0;
                leafPhaseChargeTicks = 0;
                leafPhaseGraceTicks = 0;
            }
            player.fallDistance = 0.0F;
            return;
        }

        if (minecraft.getCameraEntity() != player) {
            minecraft.setCameraEntity(player);
        }

        if (VagrantRules.isRestricted(player)) {
            if (!player.getUUID().equals(appliedPlayerId)) {
                appliedPlayerId = player.getUUID();
                originalNoPhysics = player.noPhysics;
                originalNoGravity = player.isNoGravity();
            }
            applyVagrantPhasePhysics(minecraft, player);
            player.fallDistance = 0.0F;
            return;
        }

        if (player.getUUID().equals(appliedPlayerId)) {
            appliedPlayerId = null;
            if (!player.isSpectator()) {
                player.noPhysics = originalNoPhysics;
                player.setNoGravity(originalNoGravity);
            }
            VagrantRules.clearPhaseState(player);
            originalNoPhysics = false;
            originalNoGravity = false;
            phaseChargeTicks = 0;
            leafPhaseChargeTicks = 0;
            leafPhaseGraceTicks = 0;
        }
    }

    @SubscribeEvent
    public static void onInteractionKey(InputEvent.InteractionKeyMappingTriggered event) {
        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;
        if (player == null || !VagrantRules.isRestricted(player)) {
            return;
        }

        if (event.isAttack()) {
            return;
        }

        boolean entityUse = event.isUseItem()
                && minecraft.hitResult != null
                && minecraft.hitResult.getType() == HitResult.Type.ENTITY;
        if (entityUse) {
            return;
        }

        event.setCanceled(true);
        event.setSwingHand(false);
    }

    private static void primeVagrantPhasePhysics(Minecraft minecraft, LocalPlayer player) {
        if (!VagrantRules.isRestricted(player)) {
            phaseChargeTicks = 0;
            leafPhaseChargeTicks = 0;
            leafPhaseGraceTicks = 0;
            return;
        }
        boolean phaseAttempt = isPhaseAttemptInput(minecraft, player);
        updateClientPhaseCharge(player, phaseAttempt);
        sendVagrantInput(minecraft, phaseAttempt);
        applyVagrantPhasePhysics(minecraft, player);
        player.fallDistance = 0.0F;
    }

    private static void applyVagrantPhasePhysics(Minecraft minecraft, LocalPlayer player) {
        boolean charged = phaseChargeTicks >= VagrantRules.PHASE_REQUIRED_TICKS;
        boolean leafActive = VagrantRules.isLeafPhaseActive(player);
        boolean insideBlock = VagrantRules.isInsidePhaseableBlock(player);
        boolean phaseMovement = VagrantRules.shouldUsePhaseMovement(player);
        player.noPhysics = false;
        if (phaseMovement && (charged || leafActive || insideBlock)) {
            VagrantRules.updatePhaseLowering(player, false);
            applyEntryPhaseMovement(minecraft, player, charged || leafActive, insideBlock);
            applyManualVerticalPhaseMovement(minecraft, player);
            limitPhaseSpeed(player);
            preventUncommandedPhaseDescent(minecraft, player);
        } else {
            VagrantRules.updatePhaseLowering(player, false);
        }
        player.setNoGravity(phaseMovement);
        VagrantDiagnostics.logPhaseSnapshot("CLIENT", player, "applyVagrantPhasePhysics");
    }

    private static boolean isPhaseAttemptInput(Minecraft minecraft, LocalPlayer player) {
        boolean movingIntoWall = minecraft.options.keyUp.isDown()
                || minecraft.options.keyLeft.isDown()
                || minecraft.options.keyRight.isDown();
        boolean verticalPhaseEntry = minecraft.options.keyJump.isDown() && player.getLookAngle().y > 0.45D
                || minecraft.options.keyShift.isDown() && player.getLookAngle().y < -0.45D;
        return (movingIntoWall || verticalPhaseEntry)
                && VagrantRules.shouldChargePhaseEntry(player)
                || phaseChargeTicks >= VagrantRules.PHASE_REQUIRED_TICKS
                && VagrantRules.shouldChargePhaseEntry(player);
    }

    private static void updateClientPhaseCharge(LocalPlayer player, boolean phaseAttempt) {
        boolean insideBlock = VagrantRules.isInsidePhaseableBlock(player);
        boolean insideLeaves = VagrantRules.isInsideLeaves(player);
        player.getPersistentData().putBoolean(VagrantRules.PHASE_ATTEMPTING, phaseAttempt);
        if (insideBlock) {
            if (VagrantRules.isInsideNonLeafPhaseableBlock(player)) {
                if (phaseChargeTicks >= VagrantRules.PHASE_REQUIRED_TICKS) {
                    phaseChargeTicks = VagrantRules.PHASE_REQUIRED_TICKS;
                } else {
                    phaseChargeTicks = 0;
                    leafPhaseChargeTicks = 0;
                    leafPhaseGraceTicks = 0;
                    VagrantRules.setLeafPhaseActive(player, false);
                }
            }
            updateClientLeafPhaseCharge(player, phaseAttempt, insideLeaves);
        } else if (phaseAttempt) {
            phaseChargeTicks = Math.min(VagrantRules.PHASE_REQUIRED_TICKS, phaseChargeTicks + 1);
            updateClientLeafPhaseCharge(player, true, insideLeaves);
        } else {
            phaseChargeTicks = 0;
            updateClientLeafPhaseCharge(player, false, insideLeaves);
        }
        player.getPersistentData().putInt(VagrantRules.PHASE_CHARGE_TICKS, phaseChargeTicks);
        player.getPersistentData().putInt(VagrantRules.LEAF_PHASE_CHARGE_TICKS, leafPhaseChargeTicks);
        player.getPersistentData().putInt(VagrantRules.LEAF_PHASE_GRACE_TICK_DATA, leafPhaseGraceTicks);
    }

    private static void updateClientLeafPhaseCharge(LocalPlayer player, boolean phaseAttempt, boolean insideLeaves) {
        if (insideLeaves && VagrantRules.isLeafPhaseActive(player)) {
            leafPhaseChargeTicks = VagrantRules.LEAF_PHASE_REQUIRED_TICKS;
            leafPhaseGraceTicks = VagrantRules.LEAF_PHASE_GRACE_TICKS;
            VagrantRules.setLeafPhaseActive(player, true);
        } else if (phaseAttempt && (insideLeaves || VagrantRules.isTouchingLeaves(player) || VagrantRules.isLookingIntoLeaves(player))) {
            leafPhaseChargeTicks = Math.min(VagrantRules.LEAF_PHASE_REQUIRED_TICKS, leafPhaseChargeTicks + 1);
            VagrantRules.setLeafPhaseActive(player, leafPhaseChargeTicks >= VagrantRules.LEAF_PHASE_REQUIRED_TICKS);
            if (leafPhaseChargeTicks >= VagrantRules.LEAF_PHASE_REQUIRED_TICKS) {
                leafPhaseGraceTicks = VagrantRules.LEAF_PHASE_GRACE_TICKS;
            }
        } else if (VagrantRules.isLeafPhaseActive(player)) {
            if (leafPhaseGraceTicks > 0) {
                leafPhaseGraceTicks--;
                leafPhaseChargeTicks = VagrantRules.LEAF_PHASE_REQUIRED_TICKS;
                VagrantRules.setLeafPhaseActive(player, true);
            } else {
                leafPhaseChargeTicks = 0;
                VagrantRules.setLeafPhaseActive(player, false);
            }
        } else {
            leafPhaseChargeTicks = 0;
            leafPhaseGraceTicks = 0;
            VagrantRules.setLeafPhaseActive(player, false);
        }
    }

    private static void sendVagrantInput(Minecraft minecraft, boolean phaseAttempt) {
        ModNetwork.CHANNEL.sendToServer(new VagrantInputPacket(
                minecraft.options.keyJump.isDown(),
                minecraft.options.keyShift.isDown(),
                phaseAttempt
        ));
    }

    private static void applyManualVerticalPhaseMovement(Minecraft minecraft, LocalPlayer player) {
        if (!VagrantRules.isInsidePhaseableBlock(player)) {
            return;
        }

        boolean jumping = minecraft.options.keyJump.isDown();
        boolean shifting = minecraft.options.keyShift.isDown();
        Vec3 movement = player.getDeltaMovement();
        if (jumping == shifting) {
            player.setDeltaMovement(movement.x, 0.0D, movement.z);
        } else if (jumping) {
            player.setDeltaMovement(movement.x, VagrantRules.PHASE_SPEED, movement.z);
        } else {
            player.setDeltaMovement(movement.x, -VagrantRules.PHASE_SPEED, movement.z);
        }
    }

    private static void applyEntryPhaseMovement(Minecraft minecraft, LocalPlayer player, boolean charged, boolean insideBlock) {
        if (!charged || insideBlock) {
            return;
        }

        Vec3 look = player.getLookAngle();
        boolean jumping = minecraft.options.keyJump.isDown();
        boolean shifting = minecraft.options.keyShift.isDown();
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

    private static void limitPhaseSpeed(LocalPlayer player) {
        Vec3 movement = player.getDeltaMovement();
        double maxSpeedSqr = VagrantRules.PHASE_SPEED * VagrantRules.PHASE_SPEED;
        if (movement.lengthSqr() > maxSpeedSqr) {
            player.setDeltaMovement(movement.normalize().scale(VagrantRules.PHASE_SPEED));
        }
    }

    private static void preventUncommandedPhaseDescent(Minecraft minecraft, LocalPlayer player) {
        if (!VagrantRules.isInsidePhaseableBlock(player) || minecraft.options.keyShift.isDown()) {
            return;
        }

        Vec3 movement = player.getDeltaMovement();
        if (movement.y < 0.0D) {
            player.setDeltaMovement(movement.x, 0.0D, movement.z);
        }
    }
}

