package ua.meekeyn.requiemunofficial.common.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;
import ua.meekeyn.requiemunofficial.common.possession.EntityMobilityManager;
import java.util.function.Supplier;

public record MobilityProfilesPacket(String json) {
    public static void encode(MobilityProfilesPacket packet, FriendlyByteBuf buffer) {
        buffer.writeUtf(packet.json, 262144);
    }
    public static MobilityProfilesPacket decode(FriendlyByteBuf buffer) {
        return new MobilityProfilesPacket(buffer.readUtf(262144));
    }
    public static void handle(MobilityProfilesPacket packet, Supplier<NetworkEvent.Context> supplier) {
        var context = supplier.get();
        context.enqueueWork(() -> EntityMobilityManager.applyClientProfiles(packet.json));
        context.setPacketHandled(true);
    }
}
