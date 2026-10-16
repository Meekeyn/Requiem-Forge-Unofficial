package ua.meekeyn.requiemunofficial.client;

import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.shaders.Uniform;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.EffectInstance;
import net.minecraft.client.renderer.PostChain;
import net.minecraft.client.renderer.PostPass;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import ua.meekeyn.requiemunofficial.RequiemUnofficial;
import ua.meekeyn.requiemunofficial.common.network.PossessionFxPacket;
import ua.meekeyn.requiemunofficial.common.util.ModIds;
import ua.meekeyn.requiemunofficial.mixin.PostChainAccessor;
import ua.meekeyn.requiemunofficial.mixin.RenderTypeAccessor;

import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Анимация входа в сущность, перенесённая из бета-16 (там — {@code RequiemFx}).
 * <p>
 * Пока идёт попытка вселения, картинка прогоняется через пост-эффект
 * {@code shaders/post/zoom.json}: первый проход ({@code fish_eye}) искажает
 * мир, второй ({@code combine}) накладывает поверх него модель цели,
 * отрисованную в отдельную цель {@code zoom_focus}. Сила искажения задаётся
 * uniform {@code Slider} = {@code (fishEye - partialTick) / 40 + 0.25}.
 * <p>
 * Цепочка обрабатывается в {@code GameRendererMixin} сразу после
 * {@code LevelRenderer.doEntityOutline()} — там же, где Satin вызывал
 * {@code ShaderEffectRenderCallback} в бете-16.
 */
@Mod.EventBusSubscriber(modid = RequiemUnofficial.MODID, value = Dist.CLIENT)
public final class ClientPossessionFx {

    private static final ResourceLocation ZOOM_SHADER = ModIds.id("shaders/post/zoom.json");
    /** Стартовое значение счётчика fish-eye в бете-16 ({@code RequiemFx#beginFishEyeAnimation}). */
    private static final int FISH_EYE_ANIMATION_TICKS = 10;
    /** Сколько тиков длилась анимация в бете-16 — столько же длился кулдаун вселения. */
    private static final int POSSESSION_ATTEMPT_TICKS = 8;

    private static final Map<RenderType, RenderType> ZOOM_RENDER_TYPES = new HashMap<>();

    private static WeakReference<Entity> possessionTarget = new WeakReference<>(null);
    private static int fishEyeAnimation = -1;
    private static int attemptTicks;
    private static int targetEntityId = -1;

    private static PostChain zoomChain;
    private static boolean zoomChainFailed;
    private static int zoomWidth = -1;
    private static int zoomHeight = -1;

    private ClientPossessionFx() {
    }

    public static void handle(PossessionFxPacket packet) {
        if (packet.phase() == PossessionFxPacket.Phase.START) {
            Minecraft minecraft = Minecraft.getInstance();
            targetEntityId = packet.targetEntityId();
            Entity target = minecraft.level == null ? null : minecraft.level.getEntity(targetEntityId);
            if (target != null) {
                beginFishEyeAnimation(target);
            }
            return;
        }
        onPossessionAck();
    }

    public static boolean isAnimatingForTest() {
        return possessionTarget.get() != null;
    }

    public static int targetEntityIdForTest() {
        return targetEntityId;
    }

    /**
     * Начинает анимацию входа в сущность (бета-16: {@code RequiemFx#beginFishEyeAnimation}).
     * Цель {@code zoom_focus} очищается сразу, чтобы в неё не попали кадры
     * предыдущего вселения.
     */
    public static void beginFishEyeAnimation(Entity possessed) {
        possessionTarget = new WeakReference<>(possessed);
        targetEntityId = possessed.getId();
        fishEyeAnimation = FISH_EYE_ANIMATION_TICKS;
        attemptTicks = 0;

        Minecraft minecraft = Minecraft.getInstance();
        if (getOrCreateZoomChain(minecraft) != null) {
            RenderTarget focus = zoomFocusTarget();
            if (focus != null) {
                focus.clear(false);
                minecraft.getMainRenderTarget().bindWrite(false);
            }
        }
    }

    /**
     * Заканчивает анимацию. В бете-16 это делалось по окончании кулдауна
     * вселения ({@code RequiemFx#onPossessionAck}); здесь срабатывает и по
     * приходу подтверждения, и по истечении времени попытки.
     */
    public static void onPossessionAck() {
        possessionTarget.clear();
        targetEntityId = -1;
    }

    /**
     * Сущность, для которой сейчас играет анимация, либо {@code null}.
     */
    public static Entity getAnimationEntity() {
        return possessionTarget.get();
    }

    /**
     * Возвращает обёртку над {@code base}, рисующую в цель {@code zoom_focus}.
     * Обёртка стабильная (по одному экземпляру на базовый тип): буферы
     * сортируются по идентичности RenderType.
     */
    public static RenderType getZoomFx(RenderType base) {
        if (base == null) {
            return null;
        }
        if (getOrCreateZoomChain(Minecraft.getInstance()) == null) {
            return base;
        }
        return ZOOM_RENDER_TYPES.computeIfAbsent(base, ClientPossessionFx::createZoomRenderType);
    }

    /**
     * Проигрывает цепочку зума текущего кадра и очищает {@code zoom_focus},
     * чтобы следующий кадр начинался с пустой цели.
     */
    public static void renderShaderEffects(float partialTick) {
        if (possessionTarget.get() == null) {
            return;
        }

        Minecraft minecraft = Minecraft.getInstance();
        PostChain chain = getOrCreateZoomChain(minecraft);
        if (chain == null) {
            return;
        }

        RenderTarget main = minecraft.getMainRenderTarget();
        if (zoomWidth != main.width || zoomHeight != main.height) {
            zoomWidth = main.width;
            zoomHeight = main.height;
            chain.resize(zoomWidth, zoomHeight);
        }

        RenderTarget focus = chain.getTempTarget("zoom_focus");
        if (focus == null) {
            return;
        }

        setSlider(chain, ((float) fishEyeAnimation - partialTick) / 40.0F + 0.25F);
        chain.process(partialTick);
        focus.clear(false);
        main.bindWrite(false);
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END || possessionTarget.get() == null) {
            return;
        }

        fishEyeAnimation--;
        if (++attemptTicks >= POSSESSION_ATTEMPT_TICKS) {
            onPossessionAck();
        }
    }

    private static PostChain getOrCreateZoomChain(Minecraft minecraft) {
        if (zoomChain != null) {
            return zoomChain;
        }
        if (zoomChainFailed) {
            return null;
        }

        try {
            zoomChain = new PostChain(
                    minecraft.getTextureManager(),
                    minecraft.getResourceManager(),
                    minecraft.getMainRenderTarget(),
                    ZOOM_SHADER
            );
            RenderTarget main = minecraft.getMainRenderTarget();
            zoomWidth = main.width;
            zoomHeight = main.height;
            zoomChain.resize(zoomWidth, zoomHeight);
            RequiemUnofficial.LOGGER.info("Loaded possession zoom post chain {}", ZOOM_SHADER);
            return zoomChain;
        } catch (IOException | RuntimeException exception) {
            zoomChainFailed = true;
            RequiemUnofficial.LOGGER.error("Failed to load possession zoom post chain {}", ZOOM_SHADER, exception);
            return null;
        }
    }

    private static RenderTarget zoomFocusTarget() {
        return zoomChain == null ? null : zoomChain.getTempTarget("zoom_focus");
    }

    private static void setSlider(PostChain chain, float value) {
        List<PostPass> passes = ((PostChainAccessor) chain).requiemunofficial$getPasses();
        for (PostPass pass : passes) {
            EffectInstance effect = pass.getEffect();
            Uniform uniform = effect.getUniform("Slider");
            if (uniform != null) {
                uniform.set(value);
                return;
            }
        }
    }

    /**
     * Копия базового типа, у которой начало/конец отрисовки переключают
     * целевой фреймбфер: модель сущности попадает в {@code zoom_focus},
     * а после отрисовки управление возвращается главной цели.
     */
    private static RenderType createZoomRenderType(RenderType base) {
        return new RenderType(
                base.toString() + "_requiem_zoom",
                base.format(),
                base.mode(),
                base.bufferSize(),
                base.affectsCrumbling(),
                ((RenderTypeAccessor) base).requiemunofficial$getSortOnUpload(),
                () -> {
                    base.setupRenderState();
                    RenderTarget focus = zoomFocusTarget();
                    if (focus != null) {
                        focus.bindWrite(false);
                    }
                },
                () -> {
                    base.clearRenderState();
                    Minecraft.getInstance().getMainRenderTarget().bindWrite(false);
                }
        ) {
        };
    }
}
