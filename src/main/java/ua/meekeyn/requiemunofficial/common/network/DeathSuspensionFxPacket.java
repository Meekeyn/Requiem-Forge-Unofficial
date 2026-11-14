package ua.meekeyn.requiemunofficial.common.network;

import net.minecraft.network.FriendlyByteBuf;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import ua.meekeyn.requiemunofficial.common.util.ModIds;
import ua.meekeyn.requiemunofficial.client.ClientDeathSuspensionFx;


public record DeathSuspensionFxPacket(boolean suspended) implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<DeathSuspensionFxPacket> TYPE = new CustomPacketPayload.Type<>(ModIds.id("death_suspension_fx_packet"));
    public static final StreamCodec<net.minecraft.network.RegistryFriendlyByteBuf, DeathSuspensionFxPacket> STREAM_CODEC = StreamCodec.of((buf, p) -> encode(p, buf), DeathSuspensionFxPacket::decode);

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }


    public static void encode(DeathSuspensionFxPacket packet, FriendlyByteBuf buffer) {
        buffer.writeBoolean(packet.suspended);
    }

    public static DeathSuspensionFxPacket decode(FriendlyByteBuf buffer) {
        return new DeathSuspensionFxPacket(buffer.readBoolean());
    }

    public static void handle(DeathSuspensionFxPacket packet,
                              IPayloadContext context) {
        context.enqueueWork(() -> ClientDeathSuspensionFx.setSuspended(packet.suspended));
    }
}
