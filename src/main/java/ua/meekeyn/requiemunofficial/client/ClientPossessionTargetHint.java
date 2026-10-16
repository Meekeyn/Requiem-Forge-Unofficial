package ua.meekeyn.requiemunofficial.client;

import ua.meekeyn.requiemunofficial.common.network.PossessionTargetHintPacket;

public final class ClientPossessionTargetHint {
    private static boolean canPossessTarget;

    private ClientPossessionTargetHint() {
    }

    public static void handle(PossessionTargetHintPacket packet) {
        canPossessTarget = packet.canPossessTarget();
    }

    public static boolean canPossessTarget() {
        return canPossessTarget;
    }
}
