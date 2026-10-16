package ua.meekeyn.requiemunofficial.common.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;
import ua.meekeyn.requiemunofficial.client.ClientPossessionNameTags;

import java.util.function.Supplier;

public record PossessionNameTagPacket(int hostEntityId, Component possessorName) {

    public static void encode(PossessionNameTagPacket packet, FriendlyByteBuf buffer) {
        buffer.writeVarInt(packet.hostEntityId);
        buffer.writeNullable(packet.possessorName, FriendlyByteBuf::writeComponent);
    }

    public static PossessionNameTagPacket decode(FriendlyByteBuf buffer) {
        return new PossessionNameTagPacket(
                buffer.readVarInt(),
                buffer.readNullable(FriendlyByteBuf::readComponent)
        );
    }

    public static void handle(PossessionNameTagPacket packet,
                              Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(
                Dist.CLIENT,
                () -> () -> ClientPossessionNameTags.handle(packet)
        ));
        context.setPacketHandled(true);
    }
}
