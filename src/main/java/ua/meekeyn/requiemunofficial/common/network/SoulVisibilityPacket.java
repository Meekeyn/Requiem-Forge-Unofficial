package ua.meekeyn.requiemunofficial.common.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;
import ua.meekeyn.requiemunofficial.client.ClientSoulVisibility;

import java.util.function.Supplier;

/**
 * Server -> client: tells players tracking {@code entityId} whether that player is
 * currently in soul form (out of their own body) and must not be rendered.
 * The owning player learns its own state through {@link SyncSoulDataPacket}.
 */
public record SoulVisibilityPacket(int entityId, boolean hidden) {

    public static void encode(SoulVisibilityPacket packet, FriendlyByteBuf buffer) {
        buffer.writeVarInt(packet.entityId);
        buffer.writeBoolean(packet.hidden);
    }

    public static SoulVisibilityPacket decode(FriendlyByteBuf buffer) {
        return new SoulVisibilityPacket(buffer.readVarInt(), buffer.readBoolean());
    }

    public static void handle(SoulVisibilityPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(
                Dist.CLIENT,
                () -> () -> ClientSoulVisibility.handle(packet)
        ));
        context.setPacketHandled(true);
    }
}
