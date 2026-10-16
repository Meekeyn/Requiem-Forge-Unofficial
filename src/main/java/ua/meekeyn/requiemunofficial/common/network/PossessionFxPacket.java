package ua.meekeyn.requiemunofficial.common.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;
import ua.meekeyn.requiemunofficial.client.ClientPossessionFx;

import java.util.function.Supplier;

public record PossessionFxPacket(Phase phase, int targetEntityId) {

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
                              Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(
                Dist.CLIENT,
                () -> () -> ClientPossessionFx.handle(packet)
        ));
        context.setPacketHandled(true);
    }
}
