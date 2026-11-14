package ua.meekeyn.requiemunofficial.common.network;

import net.minecraft.network.FriendlyByteBuf;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import ua.meekeyn.requiemunofficial.common.util.ModIds;
import ua.meekeyn.requiemunofficial.client.screen.InitialSoulChoiceScreen;


public record OpenSoulChoiceScreenPacket() implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<OpenSoulChoiceScreenPacket> TYPE = new CustomPacketPayload.Type<>(ModIds.id("open_soul_choice_screen_packet"));
    public static final StreamCodec<net.minecraft.network.RegistryFriendlyByteBuf, OpenSoulChoiceScreenPacket> STREAM_CODEC = StreamCodec.of((buf, p) -> encode(p, buf), OpenSoulChoiceScreenPacket::decode);

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }


    public static void encode(OpenSoulChoiceScreenPacket packet, FriendlyByteBuf buffer) {
    }

    public static OpenSoulChoiceScreenPacket decode(FriendlyByteBuf buffer) {
        return new OpenSoulChoiceScreenPacket();
    }

    public static void handle(OpenSoulChoiceScreenPacket packet,
                              IPayloadContext context) {
        context.enqueueWork(InitialSoulChoiceScreen::open);
    }
}
