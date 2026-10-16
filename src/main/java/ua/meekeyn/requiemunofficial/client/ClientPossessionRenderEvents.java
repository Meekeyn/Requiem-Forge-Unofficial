package ua.meekeyn.requiemunofficial.client;

import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderPlayerEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import ua.meekeyn.requiemunofficial.RequiemUnofficial;

@Mod.EventBusSubscriber(modid = RequiemUnofficial.MODID, value = Dist.CLIENT)
public final class ClientPossessionRenderEvents {

    private ClientPossessionRenderEvents() {
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onRenderPossessedSoulPlayer(RenderPlayerEvent.Pre event) {
        if (event.getEntity() instanceof AbstractClientPlayer player
                && ClientPossessionRenderHelper.shouldHideLocalSoulPlayer(player)) {
            event.setCanceled(true);
        }
    }
}
