package ua.meekeyn.requiemunofficial.client;

import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;
import ua.meekeyn.requiemunofficial.RequiemUnofficial;
import ua.meekeyn.requiemunofficial.common.network.ModNetwork;
import ua.meekeyn.requiemunofficial.common.network.PossessionActionPacket;
import ua.meekeyn.requiemunofficial.common.network.PossessionInputPacket;
import ua.meekeyn.requiemunofficial.common.possession.EntityMobilityManager;
import ua.meekeyn.requiemunofficial.common.possession.PossessedMountRules;
import ua.meekeyn.requiemunofficial.common.possession.PossessionManager;
import ua.meekeyn.requiemunofficial.common.soul.SoulManager;
import ua.meekeyn.requiemunofficial.common.util.ModTags;

@EventBusSubscriber(modid = RequiemUnofficial.MODID, value = Dist.CLIENT)
public final class ClientPossessionInput {

    private static int jumpCooldown;
    private static int attackRepeatCooldown;
    private static int currentCameraHostId = -1;
    private static boolean emergencyShiftPressed;
    private static boolean reqExitPending; // ТИМЧАСОВО [REQ-EXIT]

    private ClientPossessionInput() {
    }

    // ТИМЧАСОВО [REQ-EXIT]: фіксуємо сире натискання V, до будь-яких ранніх виходів обробника тіку.
    @SubscribeEvent // ТИМЧАСОВО [REQ-EXIT]
    public static void reqExitOnKey(InputEvent.Key event) { // ТИМЧАСОВО [REQ-EXIT]
        if (event.getAction() != org.lwjgl.glfw.GLFW.GLFW_PRESS || !ModKeyMappings.UNCONDITIONAL_EXIT.matches(event.getKey(), event.getScanCode())) return; // ТИМЧАСОВО [REQ-EXIT]
        Minecraft mc = Minecraft.getInstance(); // ТИМЧАСОВО [REQ-EXIT]
        StringBuilder conflicts = new StringBuilder(); // ТИМЧАСОВО [REQ-EXIT]
        for (net.minecraft.client.KeyMapping km : mc.options.keyMappings) { if (km != ModKeyMappings.UNCONDITIONAL_EXIT && km.same(ModKeyMappings.UNCONDITIONAL_EXIT)) conflicts.append(km.getName()).append(' '); } // ТИМЧАСОВО [REQ-EXIT]
        RequiemUnofficial.LOGGER.info("[REQ-EXIT] client: V pressed key={} conflicts=[{}] player={} screen={} camera={} clientPossessedId={}", ModKeyMappings.UNCONDITIONAL_EXIT.getKey().getName(), conflicts.toString().trim(), mc.player != null, mc.screen == null ? "none" : mc.screen.getClass().getSimpleName(), mc.getCameraEntity() == null ? "null" : mc.getCameraEntity().getClass().getSimpleName() + "#" + mc.getCameraEntity().getId(), mc.player == null ? "n/a" : SoulManager.getPossessedEntityId(mc.player)); // ТИМЧАСОВО [REQ-EXIT]
        reqExitPending = true; // ТИМЧАСОВО [REQ-EXIT]
    } // ТИМЧАСОВО [REQ-EXIT]

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {

        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null) {
            jumpCooldown = 0;
            attackRepeatCooldown = 0;
            currentCameraHostId = -1;
            emergencyShiftPressed = false;
            reqExitPending = false; // ТИМЧАСОВО [REQ-EXIT]
            return;
        }
        if (minecraft.screen != null) {
            if (reqExitPending) { RequiemUnofficial.LOGGER.info("[REQ-EXIT] client: chain stopped, screen open: {}", minecraft.screen.getClass().getSimpleName()); reqExitPending = false; } // ТИМЧАСОВО [REQ-EXIT]
            jumpCooldown = 0;
            attackRepeatCooldown = 0;
            emergencyShiftPressed = false;
            LivingEntity menuHost = PossessionManager.getBeta16PlayerBodyHost(minecraft.player);
            if (menuHost != null) {
                applyLocalBeta16VerticalPrediction(minecraft, menuHost);
            }
            if (SoulManager.getPossessedEntityId(minecraft.player) != null) {
                ModNetwork.sendToServer(new PossessionInputPacket(
                        0.0F, 0.0F, false, false,
                        minecraft.player.getYRot(), minecraft.player.getXRot()));
            }
            return;
        }
        if (minecraft.player.isSpectator()) {
            if (reqExitPending) { RequiemUnofficial.LOGGER.info("[REQ-EXIT] client: chain stopped, player is spectator"); reqExitPending = false; } // ТИМЧАСОВО [REQ-EXIT]
            jumpCooldown = 0;
            attackRepeatCooldown = 0;
            currentCameraHostId = -1;
            emergencyShiftPressed = false;
            return;
        }

        while (ModKeyMappings.DISSOCIATION.consumeClick()) {
            RequiemUnofficial.LOGGER.info("[REQ-EXIT] client: ',' consumed, sending RELEASE"); // ТИМЧАСОВО [REQ-EXIT]
            ModNetwork.sendToServer(
                    new PossessionActionPacket(PossessionActionPacket.Action.RELEASE)
            );
        }

        boolean controllingHost = minecraft.getCameraEntity() != minecraft.player
                || SoulManager.getPossessedEntityId(minecraft.player) != null;
        if (!controllingHost) {
            // V поза тілом-носієм: сервер вирішує (конфіг + стан remnant), чи вийти з власного тіла.
            while (ModKeyMappings.UNCONDITIONAL_EXIT.consumeClick()) {
                reqExitPending = false; // ТИМЧАСОВО [REQ-EXIT]
                RequiemUnofficial.LOGGER.info("[REQ-EXIT] client: V consumed outside host, sending UNCONDITIONAL_RELEASE"); // ТИМЧАСОВО [REQ-EXIT]
                ModNetwork.sendToServer(
                        new PossessionActionPacket(PossessionActionPacket.Action.UNCONDITIONAL_RELEASE)
                );
            }
            if (reqExitPending) { RequiemUnofficial.LOGGER.info("[REQ-EXIT] client: chain stopped, controllingHost=false (camera==player, clientPossessedId=null)"); reqExitPending = false; } // ТИМЧАСОВО [REQ-EXIT]
            jumpCooldown = 0;
            attackRepeatCooldown = 0;
            currentCameraHostId = -1;
            emergencyShiftPressed = false;
            return;
        }

        // Port-only безумовний вихід. Сервер вирішує, чи він дозволений конфігом.
        while (ModKeyMappings.UNCONDITIONAL_EXIT.consumeClick()) {
            RequiemUnofficial.LOGGER.info("[REQ-EXIT] client: V consumed, sending UNCONDITIONAL_RELEASE"); // ТИМЧАСОВО [REQ-EXIT]
            reqExitPending = false; // ТИМЧАСОВО [REQ-EXIT]
            ModNetwork.sendToServer(
                    new PossessionActionPacket(PossessionActionPacket.Action.UNCONDITIONAL_RELEASE)
            );
        }
        if (reqExitPending) { RequiemUnofficial.LOGGER.info("[REQ-EXIT] client: V pressed but consumeClick() returned nothing (click taken by another mapping?)"); reqExitPending = false; } // ТИМЧАСОВО [REQ-EXIT]

        LivingEntity beta16Host = PossessionManager.getBeta16PlayerBodyHost(minecraft.player);
        if (beta16Host != null) {
            currentCameraHostId = -1;
            // Недвижні тіла керуються через beta16-шлях, тому аварійний Shift-вихід перевіряємо тут.
            sendEmergencyExitIfRequested(minecraft, beta16Host);
            sendBeta16PossessionActions(minecraft);
            sendAbilityKeyActions(minecraft);
            applyLocalBeta16VerticalPrediction(minecraft, beta16Host);
            sendPossessionInput(minecraft);
            return;
        }
        Entity camera = minecraft.getCameraEntity();
        if (!(camera instanceof LivingEntity) || camera == minecraft.player) {
            emergencyShiftPressed = false;
            return;
        }

        initializeLocalRotationFromNewCameraHost(minecraft);
        syncCameraHostRotationFromPlayer(minecraft, true);
        sendPossessionInput(minecraft);

        sendPossessionActions(minecraft);
        sendAbilityKeyActions(minecraft);
        sendEmergencyExitIfRequested(minecraft, (LivingEntity) camera);

        if (jumpCooldown > 0) {
            jumpCooldown--;
        }
    }

    // Оригінальний beta.16 аварійний вихід: Shift, утримуваний на недвижному (immovable) тілі.
    // Розширення порту: те саме для frictionless_hosts (големи), але не коли Shift означає злізання.
    // Надсилаємо рівно один пакет на натискання, а не щотик.
    private static void sendEmergencyExitIfRequested(Minecraft minecraft, LivingEntity host) {
        if (!minecraft.options.keyShift.isDown()) {
            emergencyShiftPressed = false;
            return;
        }
        if (emergencyShiftPressed) {
            return;
        }
        boolean immovable = host.getType().is(ModTags.IMMOVABLE);
        if (!immovable && !host.getType().is(ModTags.FRICTIONLESS_HOSTS)) {
            return;
        }
        emergencyShiftPressed = true;
        // Для frictionless-тіл на транспорті це натискання лишається злізанням; вихід лише наступним натисканням.
        // Літаючим frictionless-тілам Shift потрібен для спуску, тому для них аварійного виходу немає.
        if (!immovable && (host.isPassenger() || minecraft.player.isPassenger()
                || EntityMobilityManager.get(host).canFly())) {
            return;
        }
        ModNetwork.sendToServer(
                new PossessionActionPacket(PossessionActionPacket.Action.EMERGENCY_RELEASE)
        );
    }

    private static void sendPossessionActions(Minecraft minecraft) {
        while (minecraft.options.keyAttack.consumeClick()) {
            if (!isMiningBlock(minecraft)) {
                ModNetwork.sendToServer(
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
            ModNetwork.sendToServer(
                    new PossessionActionPacket(PossessionActionPacket.Action.ATTACK)
            );
            attackRepeatCooldown = 4;
        }
    }

    /** Клавіша здібності збігається з «використати» (ПКМ за замовчуванням) — тоді діє стара схема в onInteractionKey. */
    private static boolean abilitySharesUseKey(Minecraft minecraft) {
        return ModKeyMappings.ABILITY.same(minecraft.options.keyUse);
    }

    private static void sendAbilityKeyActions(Minecraft minecraft) {
        if (abilitySharesUseKeyOrDrain(minecraft)) {
            return;
        }
        while (ModKeyMappings.ABILITY.consumeClick()) {
            ModNetwork.sendToServer(
                    new PossessionActionPacket(PossessionActionPacket.Action.INTERACT)
            );
        }
    }

    private static boolean abilitySharesUseKeyOrDrain(Minecraft minecraft) {
        if (!abilitySharesUseKey(minecraft)) {
            return false;
        }
        while (ModKeyMappings.ABILITY.consumeClick()) {
            // обробляється в onInteractionKey
        }
        return true;
    }

    private static void sendPossessionInput(Minecraft minecraft) {
        float yRot = minecraft.player.getYRot();
        float xRot = minecraft.player.getXRot();
        boolean shifting = minecraft.options.keyShift.isDown();
        boolean jumping = minecraft.options.keyJump.isDown();
        float forward = movement(minecraft.options.keyUp.isDown(), minecraft.options.keyDown.isDown());
        float strafe = movement(minecraft.options.keyLeft.isDown(), minecraft.options.keyRight.isDown());
        // A possessed rider on a spider only walks: the sprint key does nothing (no sprint flag,
        // no sprint particles/FOV on the local player), matching the server-side drive.
        boolean ridingSpider = minecraft.getCameraEntity() instanceof LivingEntity cameraHost
                && cameraHost != minecraft.player
                && cameraHost.getVehicle() instanceof net.minecraft.world.entity.monster.Spider;
        if (ridingSpider && minecraft.player.isSprinting()) {
            minecraft.player.setSprinting(false);
        }
        ModNetwork.sendToServer(new PossessionInputPacket(
                forward,
                strafe,
                jumping,
                shifting,
                yRot,
                xRot,
                !ridingSpider && (minecraft.options.keySprint.isDown() || minecraft.player.isSprinting())
        ));
    }

    private static void sendBeta16PossessionActions(Minecraft minecraft) {
        sendPossessionActions(minecraft);
    }

    private static void applyLocalBeta16VerticalPrediction(Minecraft minecraft, LivingEntity host) {
        LocalPlayer player = minecraft.player;
        if (player == null || PossessionManager.usesExtendedPlayerBody(host)) {
            return;
        }

        EntityMobilityManager.MobilityProfile mobility = EntityMobilityManager.get(host);
        player.setNoGravity(mobility.canFly());
        if (!mobility.canFly()) {
            return;
        }

        boolean jumping = minecraft.screen == null && minecraft.options.keyJump.isDown();
        boolean shifting = minecraft.screen == null && minecraft.options.keyShift.isDown();
        Vec3 current = player.getDeltaMovement();
        double y = ua.meekeyn.requiemunofficial.common.possession.PossessionMovementController.verticalSpeed(
                current.y, Math.max(0.02D, player.getAttributeValue(Attributes.MOVEMENT_SPEED)),
                mobility.inertia(), jumping, shifting);

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
        // Вершник-носій рухається разом із транспортом: xo = x вимикало б інтерполяцію камери
        // між тіками (камера стрибала раз на тік, рух ставав «дерганим»). Залишаємо ванільні xo,
        // які rideTick уже виставив перед positionRider.
        if (host.isPassenger()) {
            return;
        }
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
        // Обхід для посадки - лише на ПКМ; ЛКМ по транспорту лишається звичайною атакою носія.
        if (event.isUseItem() && shouldAllowPossessedMount(minecraft)) {
            return;
        }
        if (event.isAttack()) {
            if (minecraft.hitResult != null
                    && minecraft.hitResult.getType() == HitResult.Type.BLOCK) {
                return;
            }
            event.setCanceled(true);
            event.setSwingHand(false);
            return;
        }
        if (event.isUseItem()) {
            if (!abilitySharesUseKey(minecraft)) {
                return;
            }
            // Оригінальна схема: непряма взаємодія (здібність INTERACT) — на ПКМ із порожньою
            // рукою. Якщо в руці є предмет або ціль — блок, лишаємо ванильне використання,
            // щоб не ламати застосування предметів та взаємодію з блоками.
            if (!minecraft.player.getMainHandItem().isEmpty()
                    || minecraft.hitResult != null
                    && minecraft.hitResult.getType() == HitResult.Type.BLOCK) {
                return;
            }
            ModNetwork.sendToServer(
                    new PossessionActionPacket(PossessionActionPacket.Action.INTERACT)
            );
            event.setCanceled(true);
            event.setSwingHand(false);
        }
    }

    // Правий клік під час одержимості зазвичай поглинається, щоб власне тіло гравця не
    // взаємодіяло. Пропускаємо його (лише use) для цілей-транспорту (PossessedMountRules: павук, коні-нежить,
    // спустошувач, курка, страйдер), щоб серверні події посадили тіло-носій верхи.
    private static boolean shouldAllowPossessedMount(Minecraft minecraft) {
        if (!(minecraft.getCameraEntity() instanceof LivingEntity host)
                || host.isPassenger()
                || minecraft.hitResult == null
                || minecraft.hitResult.getType() != HitResult.Type.ENTITY) {
            return false;
        }
        Entity target = ((EntityHitResult) minecraft.hitResult).getEntity();
        if (!target.getPassengers().isEmpty()) {
            return false;
        }
        return PossessedMountRules.canMount(target, host);
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
