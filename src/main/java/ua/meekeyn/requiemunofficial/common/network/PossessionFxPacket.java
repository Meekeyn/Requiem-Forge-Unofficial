package ua.meekeyn.requiemunofficial.common.network;

import net.minecraft.network.FriendlyByteBuf;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import ua.meekeyn.requiemunofficial.common.util.ModIds;
import ua.meekeyn.requiemunofficial.client.ClientPossessionFx;


public record PossessionFxPacket(Phase phase, int targetEntityId) implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<PossessionFxPacket> TYPE = new CustomPacketPayload.Type<>(ModIds.id("possession_fx_packet"));
    public static final StreamCodec<net.minecraft.network.RegistryFriendlyByteBuf, PossessionFxPacket> STREAM_CODEC = StreamCodec.of((buf, p) -> encode(p, buf), PossessionFxPacket::decode);

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }


    public enum Phase {
        START,
        ACK
    }

    public static void encode(PossessionFxPacket packet, FriendlyByteBuf buffer) {
        buffer.writeEnum(packet.phase);
        buffer.writeVarInt(packet.targetEntityId);
    }

    public static PossessionFxPacket decode(FriendlyByteBuf buffer) {
        return new PossessionFxPacket(
                buffer.readEnum(Phase.class),
                buffer.readVarInt()
        );
    }

    public static void handle(PossessionFxPacket packet,
                              IPayloadContext context) {
        context.enqueueWork(() -> ClientPossessionFx.handle(packet));
    }
}
