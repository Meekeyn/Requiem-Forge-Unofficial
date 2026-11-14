package ua.meekeyn.requiemunofficial.client;

import net.minecraft.world.entity.Entity;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import ua.meekeyn.requiemunofficial.RequiemUnofficial;
import ua.meekeyn.requiemunofficial.common.network.SoulVisibilityPacket;

import java.util.HashSet;
import java.util.Set;

/**
 * Entity ids of remote players whose soul is currently out of their own body
 * (server-authoritative, see {@code SoulManager#isSoulForm}). Such players are
 * removed from the render pipeline entirely on this client.
 */
@Mod.EventBusSubscriber(modid = RequiemUnofficial.MODID, value = Dist.CLIENT)
public final class ClientSoulVisibility {

    private static final Set<Integer> HIDDEN_PLAYER_IDS = new HashSet<>();

    private ClientSoulVisibility() {
    }

    public static void handle(SoulVisibilityPacket packet) {
        if (packet.hidden()) {
            HIDDEN_PLAYER_IDS.add(packet.entityId());
        } else {
            HIDDEN_PLAYER_IDS.remove(packet.entityId());
        }
    }

    public static boolean isHidden(Entity entity) {
        return entity != null && HIDDEN_PLAYER_IDS.contains(entity.getId());
    }

    @SubscribeEvent
    public static void onClientLogout(ClientPlayerNetworkEvent.LoggingOut event) {
        HIDDEN_PLAYER_IDS.clear();
    }

    /** Respawn / dimension change: the server re-sends the state for every re-tracked player. */
    @SubscribeEvent
    public static void onClientClone(ClientPlayerNetworkEvent.Clone event) {
        HIDDEN_PLAYER_IDS.clear();
    }
}
