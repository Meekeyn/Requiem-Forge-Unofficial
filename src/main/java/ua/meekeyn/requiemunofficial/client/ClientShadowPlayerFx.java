package ua.meekeyn.requiemunofficial.client;

import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.PostChain;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL30;
import ua.meekeyn.requiemunofficial.RequiemUnofficial;
import ua.meekeyn.requiemunofficial.common.soul.SoulManager;
import ua.meekeyn.requiemunofficial.common.soul.SoulState;
import ua.meekeyn.requiemunofficial.common.util.ModIds;
import ua.meekeyn.requiemunofficial.mixin.RenderTypeAccessor;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * Эффект «призрачного» игрока, перенесённый из бета-16 (там —
 * {@code ShadowPlayerFx} + {@code RequiemRenderPhases}).
 * <p>
 * Тело (и теперь ещё и рука от первого лица) бессмертной души рисуются не в
 * главную цель, а в отдельный фреймбфер {@code players} пост-эффекта
 * {@code shaders/post/shadow_player.json}. Фреймбфер {@code players} разделяет
 * буфер глубины с главной целью, а сам не пишет глубину ({@code depthMask(false)}),
 * поэтому силуэт получает правильную перспективу и корректно прячется за
 * блоками. Цвет кожи при этом никогда не попадает в главную цель: из
 * {@code players} берётся только альфа-канал, он размывается (ванильный
 * {@code blur}, радиус 20) и накладывается на сцену пост-проходом
 * {@code shadow_players} — серый размытый ореол вокруг силуэта.
 * <p>
 * В бете-16 цепочка обрабатывалась по событию Satin
 * {@code ShaderEffectRenderCallback} — оно диспетчеризовалось сразу после
 * {@code LevelRenderer.doEntityOutline()}. Здесь то же самое делает
 * {@code GameRendererMixin}. Первая отрисовка душевного игрока за кадр
 * (проход сущностей либо сброс буфера руки) очищает цель {@code players},
 * как это делал {@code ShadowPlayerFx#beginPlayersFbWrite}.
 */
public final class ClientShadowPlayerFx {

    private static final ResourceLocation SHADOW_PLAYER_SHADER =
            ModIds.id("shaders/post/shadow_player.json");
    /** Цвет очистки цели {@code players}: чистый ноль, чтобы альфа-маска была пустой. */
    private static final float[] PLAYERS_CLEAR_COLOR = {0.0F, 0.0F, 0.0F, 0.0F};

    // TODO(REMOVE-DEBUG): temporary isolation switch for sky-artifact triage.
    // While run/no_shadow_chain.txt exists, the whole shadow-player chain is
    // bypassed (players render normally, no post chain runs). Diagnostic only.
    private static boolean debugChainDisabled() {
        return new java.io.File("no_shadow_chain.txt").exists();
    }

    /** Обёртки над базовыми типами: по одному экземпляру на базовый тип. */
    private static final Map<RenderType, RenderType> SHADOW_RENDER_TYPES = new HashMap<>();

    private static PostChain chain;
    private static boolean chainFailed;
    private static int chainWidth = -1;
    private static int chainHeight = -1;
    /** Установлен, когда в цель {@code players} что-то было отрисовано в этом кадре. */
    private static boolean renderedSoulPlayers;

    private ClientShadowPlayerFx() {
    }

    /**
     * Начало кадра (бета-16: {@code ShadowPlayerFx#beforeEntitiesRender}):
     * разрешаем однократную очистку цели при первом же привиде кадра.
     */
    public static void beginFrame() {
        renderedSoulPlayers = false;
    }

    /**
     * Игрок должен выглядеть как призрак: бета-16 проверяла
     * {@code isIncorporeal() || isLifeTransient()} — бессмертная душа либо
     * приостановленная смерть (экран выбора души).
     */
    public static boolean isShadowPlayer(AbstractClientPlayer player) {
        if (player == null || player.isSpectator()) {
            return false;
        }
        if (debugChainDisabled()) { // TODO(REMOVE-DEBUG)
            return false;
        }
        if (player == Minecraft.getInstance().player && ClientDeathSuspensionFx.isSuspended()) {
            return true;
        }
        return SoulManager.getState(player) == SoulState.VAGRANT
                && SoulManager.getPossessedEntityId(player) == null;
    }

    /**
     * Возвращает обёртку над {@code base}, рисующую в цель {@code players}.
     * Обёртка стабильная (по одному экземпляру на базовый тип): буферы
     * сортируются по идентичности RenderType.
     */
    public static RenderType getShadowRenderType(RenderType base) {
        if (base == null) {
            return null;
        }
        if (debugChainDisabled()) { // TODO(REMOVE-DEBUG)
            return base;
        }
        if (getOrCreateChain(Minecraft.getInstance()) == null) {
            return base;
        }
        return SHADOW_RENDER_TYPES.computeIfAbsent(base, ClientShadowPlayerFx::createShadowRenderType);
    }

    /**
     * Проигрывает цепочку теневых игроков текущего кадра (бета-16:
     * {@code ShadowPlayerFx#renderShaderEffects}). Вызывается после
     * {@code LevelRenderer.doEntityOutline()}, когда и тело, и рука уже
     * сброшены в цель {@code players}.
     */
    public static void renderShaderEffects(float partialTick) {
        if (debugChainDisabled()) { // TODO(REMOVE-DEBUG)
            return;
        }
        if (!renderedSoulPlayers) {
            return;
        }

        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null) {
            return;
        }

        PostChain current = getOrCreateChain(minecraft);
        if (current == null) {
            return;
        }

        RenderTarget main = minecraft.getMainRenderTarget();
        if (chainWidth != main.width || chainHeight != main.height) {
            chainWidth = main.width;
            chainHeight = main.height;
            current.resize(chainWidth, chainHeight);
            // resize создаёт новые цели — буфер глубины нужно прикрепить заново
            assignMainDepth(minecraft);
        }

        current.process(partialTick);
        main.bindWrite(true);
    }

    /**
     * Копия базового типа, у которой начало/конец отрисовки дополнительно
     * переключают целевой фреймбфер (бета-16:
     * {@code RequiemRenderPhases#getShadowRenderLayer} через
     * {@code RenderLayerHelper.copy}).
     */
    private static RenderType createShadowRenderType(RenderType base) {
        return new RenderType(
                base.toString() + "_requiem_shadow_players",
                base.format(),
                base.mode(),
                base.bufferSize(),
                base.affectsCrumbling(),
                ((RenderTypeAccessor) base).requiemunofficial$getSortOnUpload(),
                () -> {
                    base.setupRenderState();
                    beginPlayersFbWrite();
                },
                () -> {
                    base.clearRenderState();
                    Minecraft.getInstance().getMainRenderTarget().bindWrite(false);
                    RenderSystem.depthMask(true);
                }
        ) {
        };
    }

    /**
     * Бета-16: {@code ShadowPlayerFx#beginPlayersFbWrite} — привязывает цель
     * {@code players}, запрещает запись глубины и очищает цвет один раз за кадр.
     * <p>
     * Как и в бета-16 (там — {@code FramebufferAccessor#getClearColor}), текущий
     * clearColor сохраняется и восстанавливается после очистки, чтобы мод не
     * оставлял «чужой» GL-цвет очистки для последующих ванильных проходов. На
     * композит теней это не влияет: {@code PostPass} каждый раз задаёт clearColor
     * из stored-каналей цели ({@code RenderTarget#clear}); цвет фона после
     * композитинга определяет финальный проход {@code shadow_blit} без блендинга.
     */
    private static void beginPlayersFbWrite() {
        Minecraft minecraft = Minecraft.getInstance();
        if (chain == null || minecraft.level == null) {
            return;
        }
        RenderTarget players = chain.getTempTarget("players");
        if (players == null) {
            return;
        }
        players.bindWrite(false);
        RenderSystem.depthMask(false);
        if (!renderedSoulPlayers) {
            float[] previousClearColor = new float[4];
            GL11.glGetFloatv(GL11.GL_COLOR_CLEAR_VALUE, previousClearColor);
            RenderSystem.clearColor(
                    PLAYERS_CLEAR_COLOR[0], PLAYERS_CLEAR_COLOR[1],
                    PLAYERS_CLEAR_COLOR[2], PLAYERS_CLEAR_COLOR[3]
            );
            RenderSystem.clear(GL11.GL_COLOR_BUFFER_BIT, Minecraft.ON_OSX);
            RenderSystem.clearColor(
                    previousClearColor[0], previousClearColor[1],
                    previousClearColor[2], previousClearColor[3]
            );
            renderedSoulPlayers = true;
        }
    }

    /**
     * Бета-16: {@code ShadowPlayerFx#assignDepthTexture} — цель {@code players}
     * использует буфер глубины главной цели, чтобы силуэт прятался за блоками.
     */
    private static void assignMainDepth(Minecraft minecraft) {
        if (chain == null) {
            return;
        }
        RenderTarget players = chain.getTempTarget("players");
        RenderTarget main = minecraft.getMainRenderTarget();
        if (players == null || main.getDepthTextureId() <= -1) {
            return;
        }
        players.bindWrite(false);
        GlStateManager._glFramebufferTexture2D(
                GL30.GL_FRAMEBUFFER, GL30.GL_DEPTH_ATTACHMENT,
                GL11.GL_TEXTURE_2D, main.getDepthTextureId(), 0
        );
        main.bindWrite(false);
    }

    private static PostChain getOrCreateChain(Minecraft minecraft) {
        if (chain != null) {
            return chain;
        }
        if (chainFailed) {
            return null;
        }

        try {
            chain = new PostChain(
                    minecraft.getTextureManager(),
                    minecraft.getResourceManager(),
                    minecraft.getMainRenderTarget(),
                    SHADOW_PLAYER_SHADER
            );
            RenderTarget main = minecraft.getMainRenderTarget();
            chainWidth = main.width;
            chainHeight = main.height;
            chain.resize(chainWidth, chainHeight);
            assignMainDepth(minecraft);
            RequiemUnofficial.LOGGER.info("Loaded shadow player post chain {}", SHADOW_PLAYER_SHADER);
            return chain;
        } catch (IOException | RuntimeException exception) {
            chainFailed = true;
            RequiemUnofficial.LOGGER.error("Failed to load shadow player post chain {}", SHADOW_PLAYER_SHADER, exception);
            return null;
        }
    }
}
