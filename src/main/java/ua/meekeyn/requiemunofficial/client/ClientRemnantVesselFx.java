package ua.meekeyn.requiemunofficial.client;

import net.minecraft.client.Minecraft;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import ua.meekeyn.requiemunofficial.common.soul.SoulState;

public final class ClientRemnantVesselFx {
    private ClientRemnantVesselFx() {
    }

    public static void play(SoulState targetState, ItemStack displayStack) {
        Minecraft minecraft = Minecraft.getInstance();
        Player player = minecraft.player;
        if (player == null) {
            return;
        }

        minecraft.particleEngine.createTrackingEmitter(player, ParticleTypes.PORTAL, 120);
        minecraft.gameRenderer.displayItemActivation(displayStack);
        ClientDeathSuspensionFx.playRemnantVesselPulse(targetState);
    }
}
