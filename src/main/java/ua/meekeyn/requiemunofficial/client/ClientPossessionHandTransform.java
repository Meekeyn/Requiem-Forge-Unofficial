package ua.meekeyn.requiemunofficial.client;

import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderHandEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import ua.meekeyn.requiemunofficial.RequiemUnofficial;
import ua.meekeyn.requiemunofficial.common.soul.SoulManager;
import ua.meekeyn.requiemunofficial.common.soul.SoulState;

@Mod.EventBusSubscriber(modid = RequiemUnofficial.MODID, value = Dist.CLIENT)
public final class ClientPossessionHandTransform {
    private ClientPossessionHandTransform() {
    }

    @SubscribeEvent
    public static void onRenderHand(RenderHandEvent event) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null) {
            return;
        }
        if (SoulManager.getState(minecraft.player) == SoulState.VAGRANT
                && SoulManager.getPossessedEntityId(minecraft.player) == null) {
            return;
        }
        if (SoulManager.getPossessedEntityId(minecraft.player) == null) {
            return;
        }
    }
}
