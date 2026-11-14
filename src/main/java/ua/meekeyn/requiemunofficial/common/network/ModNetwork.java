package ua.meekeyn.requiemunofficial.common.network;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import ua.meekeyn.requiemunofficial.RequiemUnofficial;
import ua.meekeyn.requiemunofficial.common.soul.ModCapabilities;
import ua.meekeyn.requiemunofficial.common.util.ModIds;

public final class ModNetwork {

    private static final String PROTOCOL_VERSION = "18";

    private ModNetwork() {
    }

    public static void register(RegisterPayloadHandlersEvent event) {
        RequiemUnofficial.LOGGER.info("Registering network payloads");
        PayloadRegistrar registrar = event.registrar(PROTOCOL_VERSION);
        registrar.playBidirectional(DeathSuspensionFxPacket.TYPE, DeathSuspensionFxPacket.STREAM_CODEC, DeathSuspensionFxPacket::handle);
        registrar.playBidirectional(InitialSoulChoicePacket.TYPE, InitialSoulChoicePacket.STREAM_CODEC, InitialSoulChoicePacket::handle);
        registrar.playBidirectional(OpenSoulChoiceScreenPacket.TYPE, OpenSoulChoiceScreenPacket.STREAM_CODEC, OpenSoulChoiceScreenPacket::handle);
        registrar.playBidirectional(PossessionActionPacket.TYPE, PossessionActionPacket.STREAM_CODEC, PossessionActionPacket::handle);
        registrar.playBidirectional(PossessionFxPacket.TYPE, PossessionFxPacket.STREAM_CODEC, PossessionFxPacket::handle);
        registrar.playBidirectional(PossessionInputPacket.TYPE, PossessionInputPacket.STREAM_CODEC, PossessionInputPacket::handle);
        registrar.playBidirectional(PossessionNameTagPacket.TYPE, PossessionNameTagPacket.STREAM_CODEC, PossessionNameTagPacket::handle);
        registrar.playBidirectional(PossessionTargetHintPacket.TYPE, PossessionTargetHintPacket.STREAM_CODEC, PossessionTargetHintPacket::handle);
        registrar.playBidirectional(RemnantVesselFxPacket.TYPE, RemnantVesselFxPacket.STREAM_CODEC, RemnantVesselFxPacket::handle);
        registrar.playBidirectional(SoulChoiceFxPacket.TYPE, SoulChoiceFxPacket.STREAM_CODEC, SoulChoiceFxPacket::handle);
        registrar.playToClient(SoulVisibilityPacket.TYPE, SoulVisibilityPacket.STREAM_CODEC, SoulVisibilityPacket::handle);
        registrar.playBidirectional(SyncSoulDataPacket.TYPE, SyncSoulDataPacket.STREAM_CODEC, SyncSoulDataPacket::handle);
        registrar.playBidirectional(VagrantInputPacket.TYPE, VagrantInputPacket.STREAM_CODEC, VagrantInputPacket::handle);
        registrar.playBidirectional(VagrantSenseTargetsPacket.TYPE, VagrantSenseTargetsPacket.STREAM_CODEC, VagrantSenseTargetsPacket::handle);
        registrar.playToClient(MobilityProfilesPacket.TYPE, MobilityProfilesPacket.STREAM_CODEC, MobilityProfilesPacket::handle);
    }

    public static void writeComponent(net.minecraft.network.FriendlyByteBuf buffer, net.minecraft.network.chat.Component component) {
        net.minecraft.network.chat.ComponentSerialization.TRUSTED_STREAM_CODEC.encode(
                (net.minecraft.network.RegistryFriendlyByteBuf) buffer, component);
    }

    public static net.minecraft.network.chat.Component readComponent(net.minecraft.network.FriendlyByteBuf buffer) {
        return net.minecraft.network.chat.ComponentSerialization.TRUSTED_STREAM_CODEC.decode(
                (net.minecraft.network.RegistryFriendlyByteBuf) buffer);
    }

    public static void sendToServer(CustomPacketPayload packet) {
        PacketDistributor.sendToServer(packet);
    }

    /**
     * Розсилає клієнтам, що відстежують {@code player}, чи треба ховати його модель
     * (форма душі: VAGRANT або вселення). Самому гравцю не надсилається: він знає свій стан
     * із {@link SyncSoulDataPacket}.
     */
    public static void broadcastSoulVisibility(ServerPlayer player) {
        PacketDistributor.sendToPlayersTrackingEntity(player,
                new SoulVisibilityPacket(player.getId(), ua.meekeyn.requiemunofficial.common.soul.SoulManager.isInSoulForm(player)));
    }

    /** Надсилає {@code tracker} поточний стан видимості гравця {@code target}. */
    public static void sendSoulVisibilityTo(ServerPlayer tracker, ServerPlayer target) {
        sendToPlayer(tracker, new SoulVisibilityPacket(target.getId(),
                ua.meekeyn.requiemunofficial.common.soul.SoulManager.isInSoulForm(target)));
    }

    public static void sendSoulData(ServerPlayer player) {
        broadcastSoulVisibility(player);
        if (!isConnected(player)) {
            return;
        }
        ua.meekeyn.requiemunofficial.common.soul.PlayerSoul soul = player.getData(ModCapabilities.PLAYER_SOUL);
        PacketDistributor.sendToPlayer(player, new SyncSoulDataPacket(
                                    soul.getSoulPower(),
                                    soul.getIchorLevel(),
                                    soul.getState(),
                                    soul.getPossessedEntityId(),
                                    soul.getPlayerShellId(),
                                    soul.getResurrectionDimension(),
                                    soul.getResurrectionPos()
                            ));
    }

    public static void sendToPlayer(ServerPlayer player, Object packet) {
        if (!isConnected(player)) {
            return;
        }
        PacketDistributor.sendToPlayer(player, (CustomPacketPayload) packet);
    }

    public static void sendToConnectedPlayer(ServerPlayer player, Object packet) {
        sendToPlayer(player, packet);
    }

    private static boolean isConnected(ServerPlayer player) {
        return player.connection != null && player.connection.getConnection() != null
                && player.connection.getConnection().isConnected();
    }
}
