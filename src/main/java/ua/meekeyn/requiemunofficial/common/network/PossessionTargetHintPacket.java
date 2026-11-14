package ua.meekeyn.requiemunofficial.common.network;

import net.minecraft.network.FriendlyByteBuf;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import ua.meekeyn.requiemunofficial.common.util.ModIds;
import ua.meekeyn.requiemunofficial.client.ClientPossessionTargetHint;


public record PossessionTargetHintPacket(boolean canPossessTarget) implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<PossessionTargetHintPacket> TYPE = new CustomPacketPayload.Type<>(ModIds.id("possession_target_hint_packet"));
    public static final StreamCodec<net.minecraft.network.RegistryFriendlyByteBuf, PossessionTargetHintPacket> STREAM_CODEC = StreamCodec.of((buf, p) -> encode(p, buf), PossessionTargetHintPacket::decode);

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }


    public static void encode(PossessionTargetHintPacket packet, FriendlyByteBuf buffer) {
        buffer.writeBoolean(packet.canPossessTarget);
    }

    public static PossessionTargetHintPacket decode(FriendlyByteBuf buffer) {
        return new PossessionTargetHintPacket(buffer.readBoolean());
    }

    public static void handle(PossessionTargetHintPacket packet,
                              IPayloadContext context) {
        context.enqueueWork(() -> ClientPossessionTargetHint.handle(packet));
    }
}
