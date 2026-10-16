package ua.meekeyn.requiemunofficial.common.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;
import ua.meekeyn.requiemunofficial.common.soul.VagrantEvents;
import ua.meekeyn.requiemunofficial.common.soul.VagrantRules;

import java.util.function.Supplier;

public record VagrantInputPacket(boolean jumping, boolean shifting, boolean phaseAttempt) {

    public static void encode(VagrantInputPacket packet, FriendlyByteBuf buffer) {
        buffer.writeBoolean(packet.jumping);
        buffer.writeBoolean(packet.shifting);
        buffer.writeBoolean(packet.phaseAttempt);
    }

    public static VagrantInputPacket decode(FriendlyByteBuf buffer) {
        return new VagrantInputPacket(buffer.readBoolean(), buffer.readBoolean(), buffer.readBoolean());
    }

    public static void handle(VagrantInputPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player != null && VagrantRules.isRestricted(player)) {
                VagrantEvents.setVagrantInput(player, packet.jumping, packet.shifting, packet.phaseAttempt);
            }
        });
        context.setPacketHandled(true);
    }
}
