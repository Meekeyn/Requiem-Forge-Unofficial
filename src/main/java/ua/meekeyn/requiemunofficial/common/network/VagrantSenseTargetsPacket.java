package ua.meekeyn.requiemunofficial.common.network;

import net.minecraft.network.FriendlyByteBuf;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import ua.meekeyn.requiemunofficial.common.util.ModIds;
import ua.meekeyn.requiemunofficial.client.ClientVagrantSenseTargets;

import java.util.ArrayList;
import java.util.List;

public record VagrantSenseTargetsPacket(List<Integer> entityIds) implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<VagrantSenseTargetsPacket> TYPE = new CustomPacketPayload.Type<>(ModIds.id("vagrant_sense_targets_packet"));
    public static final StreamCodec<net.minecraft.network.RegistryFriendlyByteBuf, VagrantSenseTargetsPacket> STREAM_CODEC = StreamCodec.of((buf, p) -> encode(p, buf), VagrantSenseTargetsPacket::decode);

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }


    public static void encode(VagrantSenseTargetsPacket packet, FriendlyByteBuf buffer) {
        buffer.writeVarInt(packet.entityIds.size());
        for (int entityId : packet.entityIds) {
            buffer.writeVarInt(entityId);
        }
    }

    public static VagrantSenseTargetsPacket decode(FriendlyByteBuf buffer) {
        int size = buffer.readVarInt();
        List<Integer> entityIds = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            entityIds.add(buffer.readVarInt());
        }
        return new VagrantSenseTargetsPacket(List.copyOf(entityIds));
    }

    public static void handle(VagrantSenseTargetsPacket packet,
                              IPayloadContext context) {
        context.enqueueWork(() -> ClientVagrantSenseTargets.handle(packet));
    }
}
