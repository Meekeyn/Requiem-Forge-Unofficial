package ua.meekeyn.requiemunofficial.common.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import ua.meekeyn.requiemunofficial.common.util.ModIds;
import ua.meekeyn.requiemunofficial.common.soul.VagrantEvents;
import ua.meekeyn.requiemunofficial.common.soul.VagrantRules;


public record VagrantInputPacket(boolean jumping, boolean shifting, boolean phaseAttempt) implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<VagrantInputPacket> TYPE = new CustomPacketPayload.Type<>(ModIds.id("vagrant_input_packet"));
    public static final StreamCodec<net.minecraft.network.RegistryFriendlyByteBuf, VagrantInputPacket> STREAM_CODEC = StreamCodec.of((buf, p) -> encode(p, buf), VagrantInputPacket::decode);

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }


    public static void encode(VagrantInputPacket packet, FriendlyByteBuf buffer) {
        buffer.writeBoolean(packet.jumping);
        buffer.writeBoolean(packet.shifting);
        buffer.writeBoolean(packet.phaseAttempt);
    }

    public static VagrantInputPacket decode(FriendlyByteBuf buffer) {
        return new VagrantInputPacket(buffer.readBoolean(), buffer.readBoolean(), buffer.readBoolean());
    }

    public static void handle(VagrantInputPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            ServerPlayer player = (net.minecraft.server.level.ServerPlayer) context.player();
            if (player != null && VagrantRules.isRestricted(player)) {
                VagrantEvents.setVagrantInput(player, packet.jumping, packet.shifting, packet.phaseAttempt);
            }
        });
    }
}
