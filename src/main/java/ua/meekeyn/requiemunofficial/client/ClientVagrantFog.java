package ua.meekeyn.requiemunofficial.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ViewportEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import ua.meekeyn.requiemunofficial.RequiemUnofficial;
import ua.meekeyn.requiemunofficial.common.soul.VagrantRules;

@Mod.EventBusSubscriber(modid = RequiemUnofficial.MODID, value = Dist.CLIENT)
public final class ClientVagrantFog {

    private static final float INSIDE_BLOCK_FOG_START = 0.0F;
    private static final float INSIDE_BLOCK_FOG_END = 5.0F;

    private ClientVagrantFog() {
    }

    @SubscribeEvent
    public static void onRenderFog(ViewportEvent.RenderFog event) {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null
                || !VagrantRules.isRestricted(player)
                || !VagrantRules.isInsideNonLeafPhaseableBlock(player)) {
            return;
        }

        event.setNearPlaneDistance(INSIDE_BLOCK_FOG_START);
        event.setFarPlaneDistance(INSIDE_BLOCK_FOG_END);
        event.setCanceled(true);
    }

    @SubscribeEvent
    public static void onComputeFogColor(ViewportEvent.ComputeFogColor event) {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null || !VagrantRules.isRestricted(player)) {
            return;
        }

        // Official Requiem parity (BackgroundRendererMixin#neutralizeBackgroundTint):
        // an incorporeal soul must never see a background tint darker than its own
        // luminance, otherwise dark fluid fog (e.g. water fog ~#0A0A65) renders the
        // whole underwater screen black. This only lifts the tint for the vagrant;
        // the fog distance and every other fog behavior stay untouched.
        float luminance = event.getRed() * 0.3F + event.getGreen() * 0.59F + event.getBlue() * 0.11F;
        event.setRed(Math.max(event.getRed(), luminance));
        event.setGreen(Math.max(event.getGreen(), luminance));
        event.setBlue(Math.max(event.getBlue(), luminance));
    }
}
