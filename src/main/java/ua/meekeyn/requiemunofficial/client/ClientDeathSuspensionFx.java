package ua.meekeyn.requiemunofficial.client;

import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.EffectInstance;
import net.minecraft.client.renderer.PostChain;
import net.minecraft.client.renderer.PostPass;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.client.event.RenderGuiOverlayEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.joml.Matrix4f;
import ua.meekeyn.requiemunofficial.RequiemUnofficial;
import ua.meekeyn.requiemunofficial.common.init.ModSounds;
import ua.meekeyn.requiemunofficial.common.soul.SoulState;
import ua.meekeyn.requiemunofficial.common.util.ModIds;
import ua.meekeyn.requiemunofficial.mixin.PostChainAccessor;

import java.io.IOException;
import java.util.List;

@Mod.EventBusSubscriber(modid = RequiemUnofficial.MODID, value = Dist.CLIENT)
public final class ClientDeathSuspensionFx {
    private static final int FREEZE_ANIMATION_TICKS = 60;
    private static final int CHOICE_PULSE_TICKS = 40;
    private static final int VESSEL_PULSE_MAX_ALPHA = 6;
    private static final int HEALING_VESSEL_PULSE_MAX_ALPHA = 10;
    private static final ResourceLocation ZA_WARUDO_SHADER = ModIds.id("shaders/post/za_warudo.json");
    private static final float EXPANSION_RATE = 4.0F;
    private static final int INVERSION_TICK = 25;

    private static boolean suspended;
    private static int freezeTicks;
    private static int pulseTicks;
    private static int pulseColor = 0xFFE04048;
    private static int pulseMaxAlpha = VESSEL_PULSE_MAX_ALPHA;
    private static float radius;
    private static float previousRadius;
    private static float outerSat = 1.0F;
    private static PostChain postChain;
    private static int postWidth = -1;
    private static int postHeight = -1;

    private ClientDeathSuspensionFx() {
    }

    public static void setSuspended(boolean value) {
        if (suspended == value) {
            return;
        }
        suspended = value;
        if (value) {
            freezeTicks = 0;
            pulseTicks = 0;
            radius = 0.0F;
            previousRadius = 0.0F;
            outerSat = 1.0F;
            Minecraft minecraft = Minecraft.getInstance();
            if (minecraft.player != null && minecraft.level != null) {
                minecraft.level.playLocalSound(
                        minecraft.player.getX(),
                        minecraft.player.getY(),
                        minecraft.player.getZ(),
                        ModSounds.EFFECT_TIME_STOP.get(),
                        SoundSource.PLAYERS,
                        1.0F,
                        1.0F,
                        false
                );
            }
        }
    }

    public static boolean hasFinishedAnimation() {
        return !suspended || freezeTicks > FREEZE_ANIMATION_TICKS;
    }

    /**
     * Приостановлена ли смерть локального игрока (экран выбора души).
     * Используется {@link ClientShadowPlayerFx}, чтобы игрок в это время
     * выглядел как призрак — в бете-16 это был {@code isLifeTransient()}.
     */
    public static boolean isSuspended() {
        return suspended;
    }

    public static void playChoicePulse(SoulState choice) {
        playPulse(choice, VESSEL_PULSE_MAX_ALPHA);
    }

    public static void playRemnantVesselPulse(SoulState choice) {
        playPulse(choice, choice == SoulState.MORTAL
                ? HEALING_VESSEL_PULSE_MAX_ALPHA
                : VESSEL_PULSE_MAX_ALPHA);
    }

    private static void playPulse(SoulState choice, int maxAlpha) {
        if (choice == SoulState.REMNANT) {
            pulseColor = 0xFFFF4050;
            pulseTicks = CHOICE_PULSE_TICKS;
        } else if (choice == SoulState.MORTAL) {
            pulseColor = 0xFF20CC99;
            pulseTicks = CHOICE_PULSE_TICKS / 2;
        }
        pulseMaxAlpha = maxAlpha;
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        if (suspended) {
            freezeTicks++;
            previousRadius = radius;
            if (freezeTicks == INVERSION_TICK) {
                outerSat = 0.3F;
            } else if (freezeTicks < INVERSION_TICK) {
                radius += EXPANSION_RATE;
            } else if (freezeTicks < 2 * INVERSION_TICK) {
                radius -= EXPANSION_RATE;
            }
        }
        if (pulseTicks > 0) {
            pulseTicks--;
        }
    }

    @SubscribeEvent
    public static void onRenderLevelStage(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_LEVEL || !suspended) {
            return;
        }

        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.level == null) {
            return;
        }

        PostChain chain = getOrCreatePostChain(minecraft);
        if (chain == null) {
            return;
        }

        RenderTarget target = minecraft.getMainRenderTarget();
        if (postWidth != target.width || postHeight != target.height) {
            postWidth = target.width;
            postHeight = target.height;
            chain.resize(postWidth, postHeight);
        }

        updateUniforms(chain, event);
        chain.process(event.getPartialTick());
        minecraft.getMainRenderTarget().bindWrite(false);
    }

    @SubscribeEvent
    public static void onRenderGuiOverlay(RenderGuiOverlayEvent.Post event) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null) {
            return;
        }

        GuiGraphics graphics = event.getGuiGraphics();
        if (pulseTicks > 0) {
            renderChoicePulse(graphics, minecraft, event.getPartialTick());
        }
    }

    private static PostChain getOrCreatePostChain(Minecraft minecraft) {
        if (postChain != null) {
            return postChain;
        }
        try {
            postChain = new PostChain(
                    minecraft.getTextureManager(),
                    minecraft.getResourceManager(),
                    minecraft.getMainRenderTarget(),
                    ZA_WARUDO_SHADER
            );
            RenderTarget target = minecraft.getMainRenderTarget();
            postWidth = target.width;
            postHeight = target.height;
            postChain.resize(postWidth, postHeight);
            RequiemUnofficial.LOGGER.info("Loaded ZaWorld post chain {}", ZA_WARUDO_SHADER);
            return postChain;
        } catch (IOException | RuntimeException exception) {
            RequiemUnofficial.LOGGER.error("Failed to load ZaWorld post chain {}", ZA_WARUDO_SHADER, exception);
            return null;
        }
    }

    private static void updateUniforms(PostChain chain, RenderLevelStageEvent event) {
        List<PostPass> passes = ((PostChainAccessor) chain).requiemunofficial$getPasses();
        if (passes.isEmpty()) {
            return;
        }

        Minecraft minecraft = Minecraft.getInstance();
        Camera camera = event.getCamera();
        Vec3 cameraPosition = camera.getPosition();
        Entity centerEntity = camera.getEntity();
        float tickDelta = event.getPartialTick();
        double centerX = centerEntity == null ? cameraPosition.x : Mth.lerp(tickDelta, centerEntity.xOld, centerEntity.getX());
        double centerY = centerEntity == null ? cameraPosition.y : Mth.lerp(tickDelta, centerEntity.yOld, centerEntity.getY());
        double centerZ = centerEntity == null ? cameraPosition.z : Mth.lerp(tickDelta, centerEntity.zOld, centerEntity.getZ());

        EffectInstance effect = passes.get(0).getEffect();
        effect.safeGetUniform("ViewPort").set(0, 0, minecraft.getWindow().getWidth(), minecraft.getWindow().getHeight());
        effect.safeGetUniform("InverseTransformMatrix").set(inverseTransformMatrix(event));
        effect.safeGetUniform("CameraPosition").set((float) cameraPosition.x, (float) cameraPosition.y, (float) cameraPosition.z);
        effect.safeGetUniform("Center").set((float) centerX, (float) centerY, (float) centerZ);
        effect.safeGetUniform("Radius").set(Mth.lerp(tickDelta, previousRadius, radius));
        effect.safeGetUniform("OuterSat").set(outerSat);
        effect.safeGetUniform("STime").set((freezeTicks + tickDelta) / 20.0F);
    }

    private static Matrix4f inverseTransformMatrix(RenderLevelStageEvent event) {
        PoseStack view = new PoseStack();
        Camera camera = event.getCamera();
        view.mulPose(Axis.XP.rotationDegrees(camera.getXRot()));
        view.mulPose(Axis.YP.rotationDegrees(camera.getYRot() + 180.0F));
        return new Matrix4f(event.getProjectionMatrix()).mul(view.last().pose()).invert();
    }

    private static void renderChoicePulse(GuiGraphics graphics, Minecraft minecraft, float partialTick) {
        int width = minecraft.getWindow().getGuiScaledWidth();
        int height = minecraft.getWindow().getGuiScaledHeight();
        float progress = Mth.clamp((pulseTicks - partialTick) / CHOICE_PULSE_TICKS, 0.0F, 1.0F);
        int alpha = (int) (pulseMaxAlpha * progress * progress);
        int color = (alpha << 24) | (pulseColor & 0x00FFFFFF);
        graphics.fill(0, 0, width, height, color);
    }
}
