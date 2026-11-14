package ua.meekeyn.requiemunofficial.client;

import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.entity.Entity;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;
import ua.meekeyn.requiemunofficial.RequiemUnofficial;
import ua.meekeyn.requiemunofficial.common.network.VagrantSenseTargetsPacket;

import java.util.HashSet;
import java.util.Set;

/**
 * Client side list of soulless bodies sensed by a vagrant passing through walls.
 *
 * <p>The server only tells us <i>which</i> entities can currently be possessed; the outline itself
 * is decided locally at render time (see {@code MinecraftMixin#requiemunofficial$senseSoullessOutline}),
 * so no entity state (glowing tag) is ever mutated. Entity shared flags are rewritten by the
 * server every tick, which is why setting {@code glowing} client side never worked, and it would
 * also clobber legitimate glowing effects such as the spectral arrow.
 */
@EventBusSubscriber(modid = RequiemUnofficial.MODID, value = Dist.CLIENT)
public final class ClientVagrantSenseTargets {
    private static final Set<Integer> senseTargets = new HashSet<>();

    private ClientVagrantSenseTargets() {
    }

    public static void handle(VagrantSenseTargetsPacket packet) {
        ClientLevel level = Minecraft.getInstance().level;
        if (level == null) {
            senseTargets.clear();
            return;
        }

        senseTargets.clear();
        senseTargets.addAll(packet.entityIds());
    }

    /**
     * Whether the given entity should receive the white spectral-arrow style outline this frame.
     * The outline is only shown while the local vagrant is actually passing through a wall.
     */
    public static boolean shouldOutline(Entity entity) {
        if (entity == null || !ClientVagrantPhaseFx.isPhasing()) {
            return false;
        }
        if (senseTargets.isEmpty() || Minecraft.getInstance().player == null) {
            return false;
        }
        return senseTargets.contains(entity.getId());
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        if (senseTargets.isEmpty()) {
            return;
        }

        if (Minecraft.getInstance().level == null) {
            senseTargets.clear();
        }
    }
}
