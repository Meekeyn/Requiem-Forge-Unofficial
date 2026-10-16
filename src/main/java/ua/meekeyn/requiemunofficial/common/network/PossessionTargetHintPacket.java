package ua.meekeyn.requiemunofficial.common.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;
import ua.meekeyn.requiemunofficial.client.ClientPossessionTargetHint;

import java.util.function.Supplier;

public record PossessionTargetHintPacket(boolean canPossessTarget) {

    public static void encode(PossessionTargetHintPacket packet, FriendlyByteBuf buffer) {
        buffer.writeBoolean(packet.canPossessTarget);
    }

    public static PossessionTargetHintPacket decode(FriendlyByteBuf buffer) {
        return new PossessionTargetHintPacket(buffer.readBoolean());
    }

    public static void handle(PossessionTargetHintPacket packet,
                              Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(
                Dist.CLIENT,
                () -> () -> ClientPossessionTargetHint.handle(packet)
        ));
        context.setPacketHandled(true);
    }
}
