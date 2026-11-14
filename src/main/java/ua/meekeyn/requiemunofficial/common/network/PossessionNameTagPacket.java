package ua.meekeyn.requiemunofficial.common.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import ua.meekeyn.requiemunofficial.common.util.ModIds;
import ua.meekeyn.requiemunofficial.client.ClientPossessionNameTags;


public record PossessionNameTagPacket(int hostEntityId, Component possessorName) implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<PossessionNameTagPacket> TYPE = new CustomPacketPayload.Type<>(ModIds.id("possession_name_tag_packet"));
    public static final StreamCodec<net.minecraft.network.RegistryFriendlyByteBuf, PossessionNameTagPacket> STREAM_CODEC = StreamCodec.of((buf, p) -> encode(p, buf), PossessionNameTagPacket::decode);

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }


    public static void encode(PossessionNameTagPacket packet, FriendlyByteBuf buffer) {
        buffer.writeVarInt(packet.hostEntityId);
        buffer.writeBoolean(packet.possessorName != null); if (packet.possessorName != null) { ua.meekeyn.requiemunofficial.common.network.ModNetwork.writeComponent(buffer, packet.possessorName); };
    }

    public static PossessionNameTagPacket decode(FriendlyByteBuf buffer) {
        return new PossessionNameTagPacket(
                buffer.readVarInt(),
                (buffer.readBoolean() ? ua.meekeyn.requiemunofficial.common.network.ModNetwork.readComponent(buffer) : null)
        );
    }

    public static void handle(PossessionNameTagPacket packet,
                              IPayloadContext context) {
        context.enqueueWork(() -> ClientPossessionNameTags.handle(packet));
    }
}
