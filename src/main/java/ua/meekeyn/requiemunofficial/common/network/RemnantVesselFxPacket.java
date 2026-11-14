package ua.meekeyn.requiemunofficial.common.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import ua.meekeyn.requiemunofficial.common.util.ModIds;
import ua.meekeyn.requiemunofficial.client.ClientRemnantVesselFx;
import ua.meekeyn.requiemunofficial.common.soul.SoulState;


public record RemnantVesselFxPacket(SoulState targetState, ItemStack displayStack) implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<RemnantVesselFxPacket> TYPE = new CustomPacketPayload.Type<>(ModIds.id("remnant_vessel_fx_packet"));
    public static final StreamCodec<net.minecraft.network.RegistryFriendlyByteBuf, RemnantVesselFxPacket> STREAM_CODEC = StreamCodec.of((buf, p) -> encode(p, buf), RemnantVesselFxPacket::decode);

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }


    public static void encode(RemnantVesselFxPacket packet, FriendlyByteBuf buffer) {
        buffer.writeEnum(packet.targetState);
        ItemStack.OPTIONAL_STREAM_CODEC.encode((net.minecraft.network.RegistryFriendlyByteBuf) buffer, packet.displayStack);
    }

    public static RemnantVesselFxPacket decode(FriendlyByteBuf buffer) {
        return new RemnantVesselFxPacket(
                buffer.readEnum(SoulState.class),
                ItemStack.OPTIONAL_STREAM_CODEC.decode((net.minecraft.network.RegistryFriendlyByteBuf) buffer)
        );
    }

    public static void handle(RemnantVesselFxPacket packet,
                              IPayloadContext context) {
        context.enqueueWork(() -> ClientRemnantVesselFx.play(packet.targetState, packet.displayStack));
    }
}
