package ua.meekeyn.requiemunofficial.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Spider;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import ua.meekeyn.requiemunofficial.RequiemUnofficial;
import ua.meekeyn.requiemunofficial.common.network.ModNetwork;
import ua.meekeyn.requiemunofficial.common.network.PossessionActionPacket;
import ua.meekeyn.requiemunofficial.common.network.PossessionInputPacket;
import ua.meekeyn.requiemunofficial.common.possession.EntityMobilityManager;
import ua.meekeyn.requiemunofficial.common.possession.PossessionManager;
import ua.meekeyn.requiemunofficial.common.soul.SoulManager;
import ua.meekeyn.requiemunofficial.common.util.ModTags;

@Mod.EventBusSubscriber(modid = RequiemUnofficial.MODID, value = Dist.CLIENT)
public final class ClientPossessionInput {

    private static int jumpCooldown;
    private static int attackRepeatCooldown;
    private static int currentCameraHostId = -1;

    private ClientPossessionInput() {
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }

        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null) {
            jumpCooldown = 0;
            attackRepeatCooldown = 0;
            currentCameraHostId = -1;
            return;
        }
        if (minecraft.screen != null) {
            jumpCooldown = 0;
            attackRepeatCooldown = 0;
            return;
        }
        if (minecraft.player.isSpectator()) {
            jumpCooldown = 0;
            attackRepeatCooldown = 0;
            currentCameraHostId = -1;
            return;
        }

        while (ModKeyMappings.DISSOCIATION.consumeClick()) {
            ModNetwork.CHANNEL.sendToServer(
                    new PossessionActionPacket(PossessionActionPacket.Action.RELEASE)
            );
        }

        boolean controllingHost = minecraft.getCameraEntity() != minecraft.player
                || SoulManager.getPossessedEntityId(minecraft.player) != null;
        if (!controllingHost) {
            jumpCooldown = 0;
            attackRepeatCooldown = 0;
            currentCameraHostId = -1;
            return;
        }
        LivingEntity beta16Host = PossessionManager.getBeta16PlayerBodyHost(minecraft.player);
        if (beta16Host != null) {
            currentCameraHostId = -1;
            sendBeta16PossessionActions(minecraft);
            applyLocalBeta16VerticalPrediction(minecraft, beta16Host);
            sendPossessionInput(minecraft);
            return;
        }
        Entity camera = minecraft.getCameraEntity();
        if (!(camera instanceof LivingEntity) || camera == minecraft.player) {
            return;
        }

        initializeLocalRotationFromNewCameraHost(minecraft);
        syncCameraHostRotationFromPlayer(minecraft, true);
        sendPossessionInput(minecraft);

        sendPossessionActions(minecraft);

        if (jumpCooldown > 0) {
            jumpCooldown--;
        }
    }

    private static void sendPossessionActions(Minecraft minecraft) {
        while (minecraft.options.keyAttack.consumeClick()) {
            if (!isMiningBlock(minecraft)) {
                ModNetwork.CHANNEL.sendToServer(
                        new PossessionActionPacket(PossessionActionPacket.Action.ATTACK)
                );
                attackRepeatCooldown = 4;
            }
        }
        if (attackRepeatCooldown > 0) {
            attackRepeatCooldown--;
        }
        if (minecraft.options.keyAttack.isDown()
                && attackRepeatCooldown == 0
                && !isMiningBlock(minecraft)) {
            ModNetwork.CHANNEL.sendToServer(
                    new PossessionActionPacket(PossessionActionPacket.Action.ATTACK)
            );
            attackRepeatCooldown = 4;
        }

        while (ModKeyMappings.POSSESSION_INTERACT.consumeClick()) {
            ModNetwork.CHANNEL.sendToServer(
                    new PossessionActionPacket(PossessionActionPacket.Action.INTERACT)
            );
        }
    }

    private static void sendPossessionInput(Minecraft minecraft) {
        float yRot = minecraft.player.getYRot();
        float xRot = minecraft.player.getXRot();
        boolean shifting = minecraft.options.keyShift.isDown();
        boolean jumping = minecraft.options.keyJump.isDown();
        float forward = movement(minecraft.options.keyUp.isDown(), minecraft.options.keyDown.isDown());
        float strafe = movement(minecraft.options.keyLeft.isDown(), minecraft.options.keyRight.isDown());
        ModNetwork.CHANNEL.sendToServer(new PossessionInputPacket(
                forward,
                strafe,
                jumping,
                shifting,
                yRot,
                xRot
        ));
    }

    private static void sendBeta16PossessionActions(Minecraft minecraft) {
        sendPossessionActions(minecraft);
    }

    private static void applyLocalBeta16VerticalPrediction(Minecraft minecraft, LivingEntity host) {
        LocalPlayer player = minecraft.player;
        if (player == null) {
            return;
        }

        EntityMobilityManager.MobilityProfile mobility = EntityMobilityManager.get(host.getType());
        player.setNoGravity(mobility.canFly());
        if (!mobility.canFly()) {
            return;
        }

        boolean jumping = minecraft.options.keyJump.isDown();
        boolean shifting = minecraft.options.keyShift.isDown();
        Vec3 current = player.getDeltaMovement();
        double y;
        if (jumping != shifting) {
            double speed = Math.max(0.02D, player.getAttributeValue(Attributes.MOVEMENT_SPEED));
            y = jumping ? speed : -speed;
        } else {
            y = current.y * Math.max(0.0D, Math.min(1.0D, mobility.inertia()));
            if (!player.isNoGravity()) {
                y -= mobility.gravity();
            }
        }

        if (Math.abs(y - current.y) > 1.0E-5D) {
            player.setDeltaMovement(current.x, y, current.z);
        }
        player.fallDistance = 0.0F;
    }

    private static float movement(boolean positive, boolean negative) {
        if (positive == negative) {
            return 0.0F;
        }
        return positive ? 1.0F : -1.0F;
    }

    public static void syncAfterMouseTurn(Minecraft minecraft) {
        if (minecraft.player == null
                || minecraft.player.isSpectator()
                || PossessionManager.isUsingBeta16PlayerBody(minecraft.player)
                || !isControllingPossessedHost(minecraft)) {
            return;
        }
        syncCameraHostRotationFromPlayer(minecraft, true);
    }

    private static void initializeLocalRotationFromNewCameraHost(Minecraft minecraft) {
        if (!(minecraft.getCameraEntity() instanceof LivingEntity host)
                || minecraft.getCameraEntity() == minecraft.player) {
            currentCameraHostId = -1;
            return;
        }
        if (currentCameraHostId == host.getId()) {
            return;
        }
        currentCameraHostId = host.getId();
        float yRot = host.getYRot();
        float xRot = host.getXRot();
        setLivingRotation(minecraft.player, yRot, xRot, host.yRotO, host.xRotO, host.yHeadRotO, host.yBodyRotO);
        initializeHandBobFromView(minecraft.player);
    }

    private static void syncCameraHostRotationFromPlayer(Minecraft minecraft, boolean updateHandBob) {
        if (!(minecraft.getCameraEntity() instanceof LivingEntity host)
                || minecraft.getCameraEntity() == minecraft.player) {
            return;
        }
        float yRot = minecraft.player.getYRot();
        float xRot = minecraft.player.getXRot();
        if (updateHandBob) {
            updatePossessedHandBob(minecraft.player);
        }
        setLivingRotation(host, yRot, xRot, minecraft.player.yRotO, minecraft.player.xRotO,
                minecraft.player.yHeadRotO, minecraft.player.yBodyRotO);
        stabilizeLocalHostRender(host);
    }

    private static void initializeHandBobFromView(LocalPlayer player) {
        player.xBob = player.getXRot();
        player.xBobO = player.xBob;
        player.yBob = player.getYRot();
        player.yBobO = player.yBob;
    }

    private static void updatePossessedHandBob(LocalPlayer player) {
        float yBob = nearestEquivalentAngle(player.yBob, player.getYRot());
        player.xBobO = player.xBob;
        player.yBobO = yBob;
        player.xBob += (player.getXRot() - player.xBob) * 0.5F;
        player.yBob = yBob + Mth.wrapDegrees(player.getYRot() - yBob) * 0.5F;
    }

    private static float nearestEquivalentAngle(float angle, float reference) {
        return reference - Mth.wrapDegrees(reference - angle);
    }

    private static void setLivingRotation(LivingEntity entity,
                                          float yRot,
                                          float xRot,
                                          float yRotO,
                                          float xRotO,
                                          float yHeadRotO,
                                          float yBodyRotO) {
        entity.setYRot(yRot);
        entity.setYHeadRot(yRot);
        entity.yBodyRot = yRot;
        entity.yRotO = yRotO;
        entity.yHeadRotO = yHeadRotO;
        entity.yBodyRotO = yBodyRotO;
        entity.setXRot(xRot);
        entity.xRotO = xRotO;
    }

    private static void stabilizeLocalHostRender(LivingEntity host) {
        host.xo = host.getX();
        host.yo = host.getY();
        host.zo = host.getZ();
    }

    @SubscribeEvent
    public static void onInteractionKey(InputEvent.InteractionKeyMappingTriggered event) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null
                || minecraft.player.isSpectator()
                || !isControllingPossessedHost(minecraft)) {
            return;
        }
        if (PossessionManager.isUsingBeta16PlayerBody(minecraft.player)) {
            return;
        }
        if (shouldAllowSkeletonSpiderMount(minecraft)) {
            return;
        }
        if (!event.isAttack()
                || minecraft.hitResult != null
                && minecraft.hitResult.getType() == HitResult.Type.BLOCK) {
            return;
        }

        event.setCanceled(true);
        event.setSwingHand(false);
    }

    private static boolean shouldAllowSkeletonSpiderMount(Minecraft minecraft) {
        if (!(minecraft.getCameraEntity() instanceof LivingEntity host)
                || !host.getType().is(ModTags.HUMANOID_SKELETONS)
                || host.isPassenger()
                || minecraft.hitResult == null
                || minecraft.hitResult.getType() != HitResult.Type.ENTITY
                || !(((EntityHitResult) minecraft.hitResult).getEntity() instanceof Spider spider)) {
            return false;
        }
        return spider.getPassengers().isEmpty();
    }

    private static boolean isMiningBlock(Minecraft minecraft) {
        return minecraft.hitResult != null
                && minecraft.hitResult.getType() == HitResult.Type.BLOCK;
    }

    private static boolean isControllingPossessedHost(Minecraft minecraft) {
        return minecraft.getCameraEntity() != minecraft.player
                || SoulManager.getPossessedEntityId(minecraft.player) != null;
    }

}
