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
}
