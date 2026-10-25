package ua.meekeyn.requiemunofficial.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.FogRenderer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import ua.meekeyn.requiemunofficial.RequiemUnofficial;
import ua.meekeyn.requiemunofficial.common.soul.SoulManager;
import ua.meekeyn.requiemunofficial.common.soul.VagrantRules;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.UUID;

/**
 * TODO(REMOVE-DEBUG) Temporary diagnostic probe for the black-sky / black-water /
 * missing-keys investigation. Logs one line to the client log every 5 seconds
 * (and immediately when an observed value changes) with the exact state the
 * vanilla sky/fog code uses.
 */
@Mod.EventBusSubscriber(modid = RequiemUnofficial.MODID, value = Dist.CLIENT)
public final class ClientDebugProbe {

    private static int cooldown;
    private static String lastSignature;
    private static boolean reflectionFailed;

    private ClientDebugProbe() {
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;
        if (player == null || minecraft.level == null) {
            cooldown = 0;
            lastSignature = null;
            return;
        }
        boolean due = cooldown >= 100;
        cooldown = Math.min(cooldown + 1, 1_000_000);
        try {
            Entity camera = minecraft.getCameraEntity();
            double cameraY = camera == null ? Double.NaN : camera.getY();
            // Exact vanilla void-disc gate (LevelRenderer.renderSky:1870):
            double playerEye = player.getEyePosition(1.0F).y;
            double horizon = minecraft.level.getLevelData().getHorizonHeight(minecraft.level);
            boolean wouldDrawVoidDisc = playerEye < horizon;

            // Mob-effect fog path (FogRenderer.setupColor:142-157).
            LivingEntity fogEntity = camera instanceof LivingEntity living ? living : null;
            MobEffectInstance darkness = fogEntity == null ? null : fogEntity.getEffect(MobEffects.DARKNESS);
            MobEffectInstance blindness = fogEntity == null ? null : fogEntity.getEffect(MobEffects.BLINDNESS);
            float voidDarknessF5 = 1.0F;
            if (darkness != null) {
                voidDarknessF5 = darkness.getFactorData().isEmpty()
                        ? 0.0F
                        : 1.0F - darkness.getFactorData().get().getFactor(fogEntity, 1.0F);
            }
            // Vagrant night-vision parity (VagrantEvents grants NIGHT_VISION; vanilla
            // lightmap skips the lift when DARKNESS is also present).
            MobEffectInstance nightVision = fogEntity == null ? null : fogEntity.getEffect(MobEffects.NIGHT_VISION);
            int darknessTicks = darkness == null ? 0 : darkness.getDuration();

            // Light levels at the camera: distinguishes "light 0 + no night vision"
            // from a rendering blackout for the murky-water investigation.
            net.minecraft.core.BlockPos lightPos = camera == null
                    ? player.blockPosition()
                    : net.minecraft.core.BlockPos.containing(camera.position());
            int blockLight = minecraft.level.getBrightness(net.minecraft.world.level.LightLayer.BLOCK, lightPos);
            int skyLight = minecraft.level.getBrightness(net.minecraft.world.level.LightLayer.SKY, lightPos);

            float fogR = Float.NaN;
            float fogG = Float.NaN;
            float fogB = Float.NaN;
            if (!reflectionFailed) {
                try {
                    fogR = readFogField("fogRed");
                    fogG = readFogField("fogGreen");
                    fogB = readFogField("fogBlue");
                } catch (Exception ex) {
                    reflectionFailed = true;
                    RequiemUnofficial.LOGGER.warn("[SKYDBG] fog reflection failed: {}", ex.toString());
                }
            }

            String fluid = minecraft.gameRenderer.getMainCamera().getFluidInCamera().name();
            UUID possessed = SoulManager.getPossessedEntityId(player);
            boolean vagrant = VagrantRules.isRestricted(player);
            boolean cameraIsPlayer = camera == player;
            int totalKeys = minecraft.options.keyMappings.length;
            boolean hasDissociation = Arrays.asList(minecraft.options.keyMappings)
                    .contains(ModKeyMappings.DISSOCIATION);
            boolean hasUnconditionalExit = Arrays.asList(minecraft.options.keyMappings)
                    .contains(ModKeyMappings.UNCONDITIONAL_EXIT);

            // Sky dome tint (LevelRenderer.renderSky:1783-1790) and global shader color leaks.
            net.minecraft.world.phys.Vec3 dome = minecraft.level.getSkyColor(
                    minecraft.gameRenderer.getMainCamera().getPosition(), 1.0F);
            float[] shaderColor = com.mojang.blaze3d.systems.RenderSystem.getShaderColor();
            // Actual GL framebuffer clear color (GL_COLOR_CLEAR_VALUE) — the value the
            // main target gets cleared with at LevelRenderer.renderLevel:1215. A black
            // value here while the fog color is light means the background under the
            // sky disc renders black.
            float clearR = Float.NaN;
            float clearG = Float.NaN;
            float clearB = Float.NaN;
            float clearA = Float.NaN;
            try {
                float[] clearColorArr = new float[4];
                org.lwjgl.opengl.GL11.glGetFloatv(org.lwjgl.opengl.GL11.GL_COLOR_CLEAR_VALUE, clearColorArr);
                clearR = clearColorArr[0];
                clearG = clearColorArr[1];
                clearB = clearColorArr[2];
                clearA = clearColorArr[3];
            } catch (Throwable ignored) {
            }
            String postChain = findPostChainName(minecraft);
            boolean suspended = ClientDeathSuspensionFx.isSuspended();
            boolean shadowChain = readStaticBoolean("chain", ClientShadowPlayerFx.class);
            long dayTime = minecraft.level.getDayTime();
            float rain = minecraft.level.getRainLevel(1.0F);

            // Orientation: the void disc is only visible when the view actually crosses
            // below the horizon, so log pitch/yaw of player and camera separately.
            float playerPitch = player.getXRot();
            float playerYaw = player.getYRot();
            float cameraPitch = camera == null ? Float.NaN : camera.getXRot();
            float cameraYaw = camera == null ? Float.NaN : camera.getYRot();
            double cameraEyeY = camera == null ? Double.NaN : camera.position().y + camera.getEyeHeight();
            double eyeAboveHorizon = playerEye - horizon;
            boolean viewLooksBelowHorizon = cameraPitch > 0.0F; // pitch > 0 looks downward

            String signature = String.format(
                    "camIsPlayer=%b camY=%.1f playerY=%.1f eye=%.2f horizon=%.0f eyeD=%.2f disc=%b "
                            + "pitch=%.1f camPitch=%.1f yaw=%.1f camYaw=%.1f lookDown=%b camEyeY=%.2f "
                            + "nv=%b darkDur=%d light=%d/%d "
                            + "fog=(%.3f,%.3f,%.3f) glClear=(%.3f,%.3f,%.3f,%.3f) f5=%.3f darkness=%b blindness=%b "
                            + "fluid=%s possessed=%b vagrant=%b keys=%d/%b/%b "
                            + "dome=(%.3f,%.3f,%.3f) shColor=(%.2f,%.2f,%.2f,%.2f) "
                            + "post=%s susp=%b shadowChain=%b day=%d rain=%.2f",
                    cameraIsPlayer, cameraY, player.getY(), playerEye, horizon, eyeAboveHorizon, wouldDrawVoidDisc,
                    playerPitch, cameraPitch, playerYaw, cameraYaw, viewLooksBelowHorizon, cameraEyeY,
                    nightVision != null, darknessTicks, blockLight, skyLight,
                    fogR, fogG, fogB, clearR, clearG, clearB, clearA, voidDarknessF5, darkness != null, blindness != null,
                    fluid, possessed != null, vagrant, totalKeys, hasDissociation, hasUnconditionalExit,
                    dome.x, dome.y, dome.z,
                    shaderColor[0], shaderColor[1], shaderColor[2], shaderColor[3],
                    postChain, suspended, shadowChain, dayTime, rain);

            if (due || !signature.equals(lastSignature)) {
                lastSignature = signature;
                cooldown = 0;
                RequiemUnofficial.LOGGER.info("[SKYDBG] {}", signature);
            }
        } catch (Throwable t) {
            if (cooldown > 100) {
                cooldown = 0;
                RequiemUnofficial.LOGGER.warn("[SKYDBG] probe error: {}", t.toString());
            }
        }
    }

    private static float readFogField(String name) throws Exception {
        Field field = FogRenderer.class.getDeclaredField(name);
        field.setAccessible(true);
        return field.getFloat(null);
    }

    private static boolean readStaticBoolean(String name, Class<?> owner) {
        try {
            Field field = owner.getDeclaredField(name);
            field.setAccessible(true);
            Object value = field.get(null);
            if (value instanceof Boolean flag) {
                return flag;
            }
            return value != null;
        } catch (Exception ex) {
            return false;
        }
    }

    private static String findPostChainName(Minecraft minecraft) {
        try {
            StringBuilder found = new StringBuilder();
            for (Field field : minecraft.gameRenderer.getClass().getDeclaredFields()) {
                if (field.getType().getSimpleName().contains("PostChain")) {
                    field.setAccessible(true);
                    Object value = field.get(minecraft.gameRenderer);
                    if (value != null) {
                        if (found.length() > 0) {
                            found.append('+');
                        }
                        found.append(field.getName()).append(':')
                                .append(value.getClass().getSimpleName());
                    }
                }
            }
            return found.length() == 0 ? "-" : found.toString();
        } catch (Exception ex) {
            return "err:" + ex.getClass().getSimpleName();
        }
    }
}
