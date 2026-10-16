package ua.meekeyn.requiemunofficial.common.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;
import ua.meekeyn.requiemunofficial.client.ClientRemnantVesselFx;
import ua.meekeyn.requiemunofficial.common.soul.SoulState;

import java.util.function.Supplier;

public record RemnantVesselFxPacket(SoulState targetState, ItemStack displayStack) {

    public static void encode(RemnantVesselFxPacket packet, FriendlyByteBuf buffer) {
        buffer.writeEnum(packet.targetState);
        buffer.writeItem(packet.displayStack);
    }

    public static RemnantVesselFxPacket decode(FriendlyByteBuf buffer) {
        return new RemnantVesselFxPacket(
                buffer.readEnum(SoulState.class),
                buffer.readItem()
        );
    }

    public static void handle(RemnantVesselFxPacket packet,
                              Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(
                Dist.CLIENT,
                () -> () -> ClientRemnantVesselFx.play(packet.targetState, packet.displayStack)
        ));
        context.setPacketHandled(true);
    }
}
