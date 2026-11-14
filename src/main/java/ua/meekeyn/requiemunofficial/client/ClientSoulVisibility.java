package ua.meekeyn.requiemunofficial.client;

import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.ints.IntSet;
import net.minecraft.world.entity.Entity;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.event.entity.EntityLeaveLevelEvent;
import ua.meekeyn.requiemunofficial.RequiemUnofficial;
import ua.meekeyn.requiemunofficial.common.network.SoulVisibilityPacket;

/**
 * Клієнтський список id сторонніх гравців, які зараз у формі душі
 * (дані надходять через {@link SoulVisibilityPacket}).
 */
@EventBusSubscriber(modid = RequiemUnofficial.MODID, value = Dist.CLIENT)
public final class ClientSoulVisibility {

    private static final IntSet HIDDEN_PLAYERS = new IntOpenHashSet();

    private ClientSoulVisibility() {
    }

    public static void handle(SoulVisibilityPacket packet) {
        if (packet.hidden()) {
            HIDDEN_PLAYERS.add(packet.entityId());
        } else {
            HIDDEN_PLAYERS.remove(packet.entityId());
        }
    }

    public static boolean isHidden(Entity entity) {
        return entity != null && HIDDEN_PLAYERS.contains(entity.getId());
    }

    @SubscribeEvent
    public static void onEntityLeave(EntityLeaveLevelEvent event) {
        if (event.getLevel().isClientSide()) {
            HIDDEN_PLAYERS.remove(event.getEntity().getId());
        }
    }

    @SubscribeEvent
    public static void onClientLogout(ClientPlayerNetworkEvent.LoggingOut event) {
        HIDDEN_PLAYERS.clear();
    }

    @SubscribeEvent
    public static void onClientRespawn(ClientPlayerNetworkEvent.Clone event) {
        // Респавн/зміна виміру: клієнтський рівень перестворюється, старі id недійсні.
        HIDDEN_PLAYERS.clear();
    }
}
