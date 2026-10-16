package ua.meekeyn.requiemunofficial.common.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;
import ua.meekeyn.requiemunofficial.common.possession.PossessionManager;

import java.util.function.Supplier;

public record PossessionInputPacket(float forward,
                                    float strafe,
                                    boolean jumping,
                                    boolean shifting,
                                    float yRot,
                                    float xRot) {

    public static void encode(PossessionInputPacket packet, FriendlyByteBuf buffer) {
        buffer.writeFloat(packet.forward);
        buffer.writeFloat(packet.strafe);
        buffer.writeBoolean(packet.jumping);
        buffer.writeBoolean(packet.shifting);
        buffer.writeFloat(packet.yRot);
        buffer.writeFloat(packet.xRot);
    }

    public static PossessionInputPacket decode(FriendlyByteBuf buffer) {
        return new PossessionInputPacket(
                buffer.readFloat(),
                buffer.readFloat(),
                buffer.readBoolean(),
                buffer.readBoolean(),
                buffer.readFloat(),
                buffer.readFloat()
        );
    }

    public static void handle(PossessionInputPacket packet,
                              Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        ServerPlayer sender = context.getSender();
        if (sender != null) {
            context.enqueueWork(() -> PossessionManager.updatePossessionInput(sender, packet));
        }
        context.setPacketHandled(true);
    }
}
