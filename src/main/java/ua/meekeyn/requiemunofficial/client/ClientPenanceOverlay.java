package ua.meekeyn.requiemunofficial.client;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderGuiEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import ua.meekeyn.requiemunofficial.RequiemUnofficial;
import ua.meekeyn.requiemunofficial.common.effect.PenanceMobEffect;
import ua.meekeyn.requiemunofficial.common.init.ModEffects;
import ua.meekeyn.requiemunofficial.common.soul.SoulManager;
import ua.meekeyn.requiemunofficial.common.soul.SoulState;

/**
 * beta.16 PenanceComponent#clientTick + RequiemFx#renderPenanceOverlay: кольорова «нудота» на екрані,
 * що наростає за 30 с попередження перед покаранням. Лічильник рахується на клієнті тими ж правилами, що й на сервері.
 */
@Mod.EventBusSubscriber(modid = RequiemUnofficial.MODID, value = Dist.CLIENT)
public final class ClientPenanceOverlay {

    private static final ResourceLocation PENANCE_OVERLAY = new ResourceLocation("textures/misc/nausea.png");

    private static int timeWithPenance = -1;
    private static float lastStrength;
    private static float nextStrength;

    private ClientPenanceOverlay() {
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        lastStrength = nextStrength;
        if (minecraft.player == null || minecraft.isPaused()) {
            if (minecraft.player == null) {
                timeWithPenance = -1;
                lastStrength = nextStrength = 0.0F;
            }
            return;
        }

        MobEffectInstance penance = minecraft.player.getEffect(ModEffects.PENANCE.get());
        if (penance != null && PenanceMobEffect.shouldSplitFromCurrentBody(minecraft.player, penance.getAmplifier())) {
            timeWithPenance = Math.min(PenanceMobEffect.PENANCE_WARNING_TIME, timeWithPenance + 1);
        } else {
            timeWithPenance = -1;
        }

        boolean incorporeal = SoulManager.getState(minecraft.player) == SoulState.VAGRANT
                && SoulManager.getPossessedEntityId(minecraft.player) == null;
        if (incorporeal && penance != null) {
            nextStrength = 0.4F;
        } else {
            nextStrength = Math.min(1.0F, Math.max(0, timeWithPenance) / (float) PenanceMobEffect.PENANCE_WARNING_TIME);
        }
    }

    @SubscribeEvent
    public static void onRenderGui(RenderGuiEvent.Pre event) {
        float intensity = Mth.lerp(event.getPartialTick(), lastStrength, nextStrength);
        if (intensity <= 0.0F) {
            return;
        }
        render(event.getGuiGraphics(), ModEffects.PENANCE.get().getColor(), intensity);
    }

    private static void render(GuiGraphics graphics, int rgb, float intensity) {
        int width = graphics.guiWidth();
        int height = graphics.guiHeight();
        double stretch = Mth.lerp(intensity, 2.0D, 1.0D);
        float r = ((rgb >> 16 & 0xFF) / 255.0F) * intensity;
        float g = ((rgb >> 8 & 0xFF) / 255.0F) * intensity;
        float b = ((rgb & 0xFF) / 255.0F) * intensity;
        int effectWidth = (int) (width * stretch);
        int effectHeight = (int) (height * stretch);
        int left = (width - effectWidth) / 2;
        int top = (height - effectHeight) / 2;

        RenderSystem.disableDepthTest();
        RenderSystem.depthMask(false);
        RenderSystem.enableBlend();
        RenderSystem.blendFuncSeparate(GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ONE,
                GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ONE);
        graphics.setColor(r, g, b, 1.0F);
        graphics.blit(PENANCE_OVERLAY, left, top, -90, 0.0F, 0.0F, effectWidth, effectHeight, effectWidth, effectHeight);
        graphics.setColor(1.0F, 1.0F, 1.0F, 1.0F);
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableBlend();
        RenderSystem.depthMask(true);
        RenderSystem.enableDepthTest();
    }
}
