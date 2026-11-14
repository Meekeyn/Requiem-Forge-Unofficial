package ua.meekeyn.requiemunofficial.common.network;

import net.minecraft.network.FriendlyByteBuf;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import ua.meekeyn.requiemunofficial.common.util.ModIds;
import ua.meekeyn.requiemunofficial.common.possession.EntityMobilityManager;

public record MobilityProfilesPacket(String json) implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<MobilityProfilesPacket> TYPE = new CustomPacketPayload.Type<>(ModIds.id("mobility_profiles_packet"));
    public static final StreamCodec<net.minecraft.network.RegistryFriendlyByteBuf, MobilityProfilesPacket> STREAM_CODEC = StreamCodec.of((buf, p) -> encode(p, buf), MobilityProfilesPacket::decode);

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void encode(MobilityProfilesPacket packet, FriendlyByteBuf buffer) {
        buffer.writeUtf(packet.json, 262144);
    }
    public static MobilityProfilesPacket decode(FriendlyByteBuf buffer) {
        return new MobilityProfilesPacket(buffer.readUtf(262144));
    }
    public static void handle(MobilityProfilesPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> EntityMobilityManager.applyClientProfiles(packet.json));
    }
}
