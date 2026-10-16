package ua.meekeyn.requiemunofficial.common.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;
import ua.meekeyn.requiemunofficial.client.ClientSoulDataHandler;
import ua.meekeyn.requiemunofficial.common.soul.SoulState;

import java.util.UUID;
import java.util.function.Supplier;
import net.minecraft.core.BlockPos;

public record SyncSoulDataPacket(
        int soulPower,
        int ichorLevel,
        SoulState state,
        UUID possessedEntityId,
        UUID playerShellId,
        String resurrectionDimension,
        BlockPos resurrectionPos
) {

    public static void encode(SyncSoulDataPacket packet, FriendlyByteBuf buffer) {
        buffer.writeVarInt(packet.soulPower);
        buffer.writeVarInt(packet.ichorLevel);
        buffer.writeEnum(packet.state);
        buffer.writeNullable(packet.possessedEntityId, FriendlyByteBuf::writeUUID);
        buffer.writeNullable(packet.playerShellId, FriendlyByteBuf::writeUUID);
        buffer.writeUtf(packet.resurrectionDimension);
        buffer.writeNullable(packet.resurrectionPos, FriendlyByteBuf::writeBlockPos);
    }

    public static SyncSoulDataPacket decode(FriendlyByteBuf buffer) {
        return new SyncSoulDataPacket(
                buffer.readVarInt(),
                buffer.readVarInt(),
                buffer.readEnum(SoulState.class),
                buffer.readNullable(FriendlyByteBuf::readUUID),
                buffer.readNullable(FriendlyByteBuf::readUUID),
                buffer.readUtf(),
                buffer.readNullable(FriendlyByteBuf::readBlockPos)
        );
    }

    public static void handle(SyncSoulDataPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(
                Dist.CLIENT,
                () -> () -> ClientSoulDataHandler.handle(packet)
        ));
        context.setPacketHandled(true);
    }
}
