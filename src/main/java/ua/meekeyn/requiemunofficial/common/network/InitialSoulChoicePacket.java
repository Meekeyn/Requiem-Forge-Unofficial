package ua.meekeyn.requiemunofficial.common.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;
import ua.meekeyn.requiemunofficial.common.soul.InitialSoulChoiceManager;
import ua.meekeyn.requiemunofficial.common.soul.SoulState;

import java.util.function.Supplier;

public record InitialSoulChoicePacket(SoulState choice) {

    public static void encode(InitialSoulChoicePacket packet, FriendlyByteBuf buffer) {
        buffer.writeEnum(packet.choice);
    }

    public static InitialSoulChoicePacket decode(FriendlyByteBuf buffer) {
        return new InitialSoulChoicePacket(buffer.readEnum(SoulState.class));
    }

    public static void handle(InitialSoulChoicePacket packet,
                              Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        ServerPlayer sender = context.getSender();
        if (sender != null && (packet.choice == SoulState.MORTAL || packet.choice == SoulState.REMNANT)) {
            context.enqueueWork(() -> InitialSoulChoiceManager.choose(sender, packet.choice));
        }
        context.setPacketHandled(true);
    }
}
