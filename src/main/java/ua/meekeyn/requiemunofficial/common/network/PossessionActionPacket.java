package ua.meekeyn.requiemunofficial.common.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;
import ua.meekeyn.requiemunofficial.common.possession.PossessionManager;

import java.util.function.Supplier;

public record PossessionActionPacket(Action action) {

    public enum Action {
        JUMP,
        ATTACK,
        INTERACT,
        RELEASE
    }

    public static void encode(PossessionActionPacket packet, FriendlyByteBuf buffer) {
        buffer.writeEnum(packet.action);
    }

    public static PossessionActionPacket decode(FriendlyByteBuf buffer) {
        return new PossessionActionPacket(buffer.readEnum(Action.class));
    }

    public static void handle(PossessionActionPacket packet,
                              Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        ServerPlayer sender = context.getSender();
        if (sender != null) {
            context.enqueueWork(() -> PossessionManager.handleAction(sender, packet.action));
        }
        context.setPacketHandled(true);
    }
}
