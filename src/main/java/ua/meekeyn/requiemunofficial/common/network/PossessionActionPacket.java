package ua.meekeyn.requiemunofficial.common.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import ua.meekeyn.requiemunofficial.common.util.ModIds;
import ua.meekeyn.requiemunofficial.common.possession.PossessionManager;


public record PossessionActionPacket(Action action) implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<PossessionActionPacket> TYPE = new CustomPacketPayload.Type<>(ModIds.id("possession_action_packet"));
    public static final StreamCodec<net.minecraft.network.RegistryFriendlyByteBuf, PossessionActionPacket> STREAM_CODEC = StreamCodec.of((buf, p) -> encode(p, buf), PossessionActionPacket::decode);

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }


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
                              IPayloadContext context) {
        ServerPlayer sender = (net.minecraft.server.level.ServerPlayer) context.player();
        if (packet.action == Action.RELEASE || packet.action == Action.EMERGENCY_RELEASE || packet.action == Action.UNCONDITIONAL_RELEASE) ua.meekeyn.requiemunofficial.RequiemUnofficial.LOGGER.info("[REQ-EXIT] server: packet received action={} sender={}", packet.action, sender == null ? "null" : sender.getGameProfile().getName()); // ТИМЧАСОВО [REQ-EXIT]
        if (sender != null) {
            context.enqueueWork(() -> PossessionManager.handleAction(sender, packet.action));
        }
    }
}
