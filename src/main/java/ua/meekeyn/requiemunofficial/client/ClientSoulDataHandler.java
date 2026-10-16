package ua.meekeyn.requiemunofficial.client;

import net.minecraft.client.Minecraft;
import ua.meekeyn.requiemunofficial.common.network.SyncSoulDataPacket;
import ua.meekeyn.requiemunofficial.common.soul.ModCapabilities;

public final class ClientSoulDataHandler {

    private ClientSoulDataHandler() {
    }

    public static void handle(SyncSoulDataPacket packet) {
        if (Minecraft.getInstance().player == null) {
            return;
        }

        Minecraft.getInstance().player.getCapability(ModCapabilities.PLAYER_SOUL).ifPresent(soul -> {
            soul.setSoulPower(packet.soulPower());
            soul.setIchorLevel(packet.ichorLevel());
            soul.setState(packet.state());
            if (packet.possessedEntityId() == null) {
                soul.clearPossessedEntityId();
            } else {
                soul.setPossessedEntityId(packet.possessedEntityId());
            }
            if (packet.playerShellId() == null) {
                soul.clearPlayerShellId();
            } else {
                soul.setPlayerShellId(packet.playerShellId());
            }
            if (packet.resurrectionPos() == null || packet.resurrectionDimension().isEmpty()) {
                soul.clearResurrectionPoint();
            } else {
                soul.setResurrectionPoint(packet.resurrectionDimension(), packet.resurrectionPos());
            }
        });
    }
}
