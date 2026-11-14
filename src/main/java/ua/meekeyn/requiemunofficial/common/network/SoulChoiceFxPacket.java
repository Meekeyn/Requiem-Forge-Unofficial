package ua.meekeyn.requiemunofficial.common.network;

import net.minecraft.network.FriendlyByteBuf;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import ua.meekeyn.requiemunofficial.common.util.ModIds;
import ua.meekeyn.requiemunofficial.client.ClientDeathSuspensionFx;
import ua.meekeyn.requiemunofficial.common.soul.SoulState;


public record SoulChoiceFxPacket(SoulState choice) implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<SoulChoiceFxPacket> TYPE = new CustomPacketPayload.Type<>(ModIds.id("soul_choice_fx_packet"));
    public static final StreamCodec<net.minecraft.network.RegistryFriendlyByteBuf, SoulChoiceFxPacket> STREAM_CODEC = StreamCodec.of((buf, p) -> encode(p, buf), SoulChoiceFxPacket::decode);

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }


    public static void encode(SoulChoiceFxPacket packet, FriendlyByteBuf buffer) {
        buffer.writeEnum(packet.choice);
    }

    public static SoulChoiceFxPacket decode(FriendlyByteBuf buffer) {
        return new SoulChoiceFxPacket(buffer.readEnum(SoulState.class));
    }

    public static void handle(SoulChoiceFxPacket packet,
                              IPayloadContext context) {
        context.enqueueWork(() -> ClientDeathSuspensionFx.playChoicePulse(packet.choice));
    }
}
