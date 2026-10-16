package ua.meekeyn.requiemunofficial.common.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;
import ua.meekeyn.requiemunofficial.client.ClientDeathSuspensionFx;

import java.util.function.Supplier;

public record DeathSuspensionFxPacket(boolean suspended) {

    public static void encode(DeathSuspensionFxPacket packet, FriendlyByteBuf buffer) {
        buffer.writeBoolean(packet.suspended);
    }

    public static DeathSuspensionFxPacket decode(FriendlyByteBuf buffer) {
        return new DeathSuspensionFxPacket(buffer.readBoolean());
    }

    public static void handle(DeathSuspensionFxPacket packet,
                              Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(
                Dist.CLIENT,
                () -> () -> ClientDeathSuspensionFx.setSuspended(packet.suspended)
        ));
        context.setPacketHandled(true);
    }
}
