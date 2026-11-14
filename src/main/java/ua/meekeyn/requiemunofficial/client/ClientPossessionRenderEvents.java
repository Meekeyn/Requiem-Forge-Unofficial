package ua.meekeyn.requiemunofficial.client;

import net.neoforged.fml.common.EventBusSubscriber;
import net.minecraft.client.player.AbstractClientPlayer;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.neoforge.client.event.RenderPlayerEvent;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;
import ua.meekeyn.requiemunofficial.RequiemUnofficial;

@EventBusSubscriber(modid = RequiemUnofficial.MODID, value = Dist.CLIENT)
public final class ClientPossessionRenderEvents {

    private ClientPossessionRenderEvents() {
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onRenderPossessedSoulPlayer(RenderPlayerEvent.Pre event) {
        if (event.getEntity() instanceof AbstractClientPlayer player
                && ClientPossessionRenderHelper.shouldHideSoulPlayer(player)) {
            event.setCanceled(true);
        }
    }
}
