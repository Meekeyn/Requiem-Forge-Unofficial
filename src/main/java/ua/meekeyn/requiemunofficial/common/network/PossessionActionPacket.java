package ua.meekeyn.requiemunofficial.common.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;
import ua.meekeyn.requiemunofficial.common.possession.PossessionManager;

import java.util.function.Supplier;

public record PossessionActionPacket(Action action) {

    public enum Action {
        JUMP,
        ATTACK,
        INTERACT,
        RELEASE,
        // Аварійний Shift-вихід: immovable (оригінальна поведінка beta.16) і frictionless_hosts (розширення порту).
        EMERGENCY_RELEASE,
        // Port-only безумовний вихід; сервер застосовує його лише за увімкненого конфігу.
        UNCONDITIONAL_RELEASE
    }

    public static void encode(PossessionActionPacket packet, FriendlyByteBuf buffer) {
        buffer.writeEnum(packet.action);
    }

    public static PossessionActionPacket decode(FriendlyByteBuf buffer) {
        return new PossessionActionPacket(buffer.readEnum(Action.class));
    }

    public static void handle(PossessionActionPacket packet,
                              Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        ServerPlayer sender = context.getSender();
        if (packet.action == Action.RELEASE || packet.action == Action.EMERGENCY_RELEASE || packet.action == Action.UNCONDITIONAL_RELEASE) ua.meekeyn.requiemunofficial.RequiemUnofficial.LOGGER.info("[REQ-EXIT] server: packet received action={} sender={}", packet.action, sender == null ? "null" : sender.getGameProfile().getName()); // ТИМЧАСОВО [REQ-EXIT]
        if (sender != null) {
            context.enqueueWork(() -> PossessionManager.handleAction(sender, packet.action));
        }
        context.setPacketHandled(true);
    }
}
