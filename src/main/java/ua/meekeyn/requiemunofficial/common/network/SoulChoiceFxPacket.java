package ua.meekeyn.requiemunofficial.common.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;
import ua.meekeyn.requiemunofficial.client.ClientDeathSuspensionFx;
import ua.meekeyn.requiemunofficial.common.soul.SoulState;

import java.util.function.Supplier;

public record SoulChoiceFxPacket(SoulState choice) {

    public static void encode(SoulChoiceFxPacket packet, FriendlyByteBuf buffer) {
        buffer.writeEnum(packet.choice);
    }

    public static SoulChoiceFxPacket decode(FriendlyByteBuf buffer) {
        return new SoulChoiceFxPacket(buffer.readEnum(SoulState.class));
    }

    public static void handle(SoulChoiceFxPacket packet,
                              Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(
                Dist.CLIENT,
                () -> () -> ClientDeathSuspensionFx.playChoicePulse(packet.choice)
        ));
        context.setPacketHandled(true);
    }
}
