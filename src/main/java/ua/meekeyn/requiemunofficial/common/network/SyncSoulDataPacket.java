package ua.meekeyn.requiemunofficial.common.network;

import net.minecraft.network.FriendlyByteBuf;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import ua.meekeyn.requiemunofficial.common.util.ModIds;
import ua.meekeyn.requiemunofficial.client.ClientSoulDataHandler;
import ua.meekeyn.requiemunofficial.common.soul.SoulState;

import java.util.UUID;
import net.minecraft.core.BlockPos;

public record SyncSoulDataPacket(
        int soulPower,
        int ichorLevel,
        SoulState state,
        UUID possessedEntityId,
        UUID playerShellId,
        String resurrectionDimension,
        BlockPos resurrectionPos
) implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<SyncSoulDataPacket> TYPE = new CustomPacketPayload.Type<>(ModIds.id("sync_soul_data_packet"));
    public static final StreamCodec<net.minecraft.network.RegistryFriendlyByteBuf, SyncSoulDataPacket> STREAM_CODEC = StreamCodec.of((buf, p) -> encode(p, buf), SyncSoulDataPacket::decode);

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }


    public static void encode(SyncSoulDataPacket packet, FriendlyByteBuf buffer) {
        buffer.writeVarInt(packet.soulPower);
        buffer.writeVarInt(packet.ichorLevel);
        buffer.writeEnum(packet.state);
        buffer.writeNullable(packet.possessedEntityId, (b, v) -> b.writeUUID(v));
        buffer.writeNullable(packet.playerShellId, (b, v) -> b.writeUUID(v));
        buffer.writeUtf(packet.resurrectionDimension);
        buffer.writeNullable(packet.resurrectionPos, (b, v) -> b.writeBlockPos(v));
    }

    public static SyncSoulDataPacket decode(FriendlyByteBuf buffer) {
        return new SyncSoulDataPacket(
                buffer.readVarInt(),
                buffer.readVarInt(),
                buffer.readEnum(SoulState.class),
                buffer.readNullable(b -> b.readUUID()),
                buffer.readNullable(b -> b.readUUID()),
                buffer.readUtf(),
                buffer.readNullable(b -> b.readBlockPos())
        );
    }

    public static void handle(SyncSoulDataPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> ClientSoulDataHandler.handle(packet));
    }
}
