package ua.meekeyn.requiemunofficial.common.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;
import ua.meekeyn.requiemunofficial.client.screen.InitialSoulChoiceScreen;

import java.util.function.Supplier;

public record OpenSoulChoiceScreenPacket() {

    public static void encode(OpenSoulChoiceScreenPacket packet, FriendlyByteBuf buffer) {
    }

    public static OpenSoulChoiceScreenPacket decode(FriendlyByteBuf buffer) {
        return new OpenSoulChoiceScreenPacket();
    }

    public static void handle(OpenSoulChoiceScreenPacket packet,
                              Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(
                Dist.CLIENT,
                () -> InitialSoulChoiceScreen::open
        ));
        context.setPacketHandled(true);
    }
}
