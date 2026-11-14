package ua.meekeyn.requiemunofficial.common.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import ua.meekeyn.requiemunofficial.client.ClientSoulVisibility;
import ua.meekeyn.requiemunofficial.common.util.ModIds;

/**
 * Сервер -> клієнти, що відстежують гравця: чи перебуває гравець у формі душі
 * (поза тілом або у вселеному тілі). Такого гравця інші клієнти не малюють.
 */
public record SoulVisibilityPacket(int entityId, boolean hidden) implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<SoulVisibilityPacket> TYPE = new CustomPacketPayload.Type<>(ModIds.id("soul_visibility_packet"));
    public static final StreamCodec<net.minecraft.network.RegistryFriendlyByteBuf, SoulVisibilityPacket> STREAM_CODEC = StreamCodec.of((buf, p) -> encode(p, buf), SoulVisibilityPacket::decode);

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void encode(SoulVisibilityPacket packet, FriendlyByteBuf buffer) {
        buffer.writeVarInt(packet.entityId);
        buffer.writeBoolean(packet.hidden);
    }

    public static SoulVisibilityPacket decode(FriendlyByteBuf buffer) {
        return new SoulVisibilityPacket(buffer.readVarInt(), buffer.readBoolean());
    }

    public static void handle(SoulVisibilityPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> ClientSoulVisibility.handle(packet));
    }
}
