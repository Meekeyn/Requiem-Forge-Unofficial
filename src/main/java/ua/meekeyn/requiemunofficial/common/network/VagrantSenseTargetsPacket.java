package ua.meekeyn.requiemunofficial.common.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;
import ua.meekeyn.requiemunofficial.client.ClientVagrantSenseTargets;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

public record VagrantSenseTargetsPacket(List<Integer> entityIds) {

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
                              Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(
                Dist.CLIENT,
                () -> () -> ClientVagrantSenseTargets.handle(packet)
        ));
        context.setPacketHandled(true);
    }
}
