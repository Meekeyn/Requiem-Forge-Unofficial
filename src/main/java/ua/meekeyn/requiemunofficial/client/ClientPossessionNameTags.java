package ua.meekeyn.requiemunofficial.client;

import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import ua.meekeyn.requiemunofficial.RequiemUnofficial;
import ua.meekeyn.requiemunofficial.common.network.PossessionNameTagPacket;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Mod.EventBusSubscriber(modid = RequiemUnofficial.MODID, value = Dist.CLIENT)
public final class ClientPossessionNameTags {

    private static final Map<Integer, Component> POSSESSOR_NAMES = new HashMap<>();

    private ClientPossessionNameTags() {
    }

    public static void handle(PossessionNameTagPacket packet) {
        if (packet.possessorName() == null) {
            POSSESSOR_NAMES.remove(packet.hostEntityId());
        } else {
            POSSESSOR_NAMES.put(packet.hostEntityId(), packet.possessorName());
        }
    }

    public static Optional<Component> possessorName(Entity entity) {
        return Optional.ofNullable(POSSESSOR_NAMES.get(entity.getId()));
    }

    public static boolean shouldForceNameTag(Entity entity) {
        return POSSESSOR_NAMES.containsKey(entity.getId());
    }

    @SubscribeEvent
    public static void onClientLogout(ClientPlayerNetworkEvent.LoggingOut event) {
        POSSESSOR_NAMES.clear();
    }
}
