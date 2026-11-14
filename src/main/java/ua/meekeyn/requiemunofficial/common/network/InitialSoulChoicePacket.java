package ua.meekeyn.requiemunofficial.common.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import ua.meekeyn.requiemunofficial.common.util.ModIds;
import ua.meekeyn.requiemunofficial.common.soul.InitialSoulChoiceManager;
import ua.meekeyn.requiemunofficial.common.soul.SoulState;


public record InitialSoulChoicePacket(SoulState choice) implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<InitialSoulChoicePacket> TYPE = new CustomPacketPayload.Type<>(ModIds.id("initial_soul_choice_packet"));
    public static final StreamCodec<net.minecraft.network.RegistryFriendlyByteBuf, InitialSoulChoicePacket> STREAM_CODEC = StreamCodec.of((buf, p) -> encode(p, buf), InitialSoulChoicePacket::decode);

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }


    public static void encode(InitialSoulChoicePacket packet, FriendlyByteBuf buffer) {
        buffer.writeEnum(packet.choice);
    }

    public static InitialSoulChoicePacket decode(FriendlyByteBuf buffer) {
        return new InitialSoulChoicePacket(buffer.readEnum(SoulState.class));
    }

    public static void handle(InitialSoulChoicePacket packet,
                              IPayloadContext context) {
        ServerPlayer sender = (net.minecraft.server.level.ServerPlayer) context.player();
        if (sender != null && (packet.choice == SoulState.MORTAL || packet.choice == SoulState.REMNANT)) {
            context.enqueueWork(() -> InitialSoulChoiceManager.choose(sender, packet.choice));
        }
    }
}
