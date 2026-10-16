package ua.meekeyn.requiemunofficial.common.network;

import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;
import ua.meekeyn.requiemunofficial.RequiemUnofficial;
import ua.meekeyn.requiemunofficial.common.soul.ModCapabilities;
import ua.meekeyn.requiemunofficial.common.util.ModIds;

public final class ModNetwork {

    private static final String PROTOCOL_VERSION = "15";

    public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            ModIds.id("main"),
            () -> PROTOCOL_VERSION,
            PROTOCOL_VERSION::equals,
            PROTOCOL_VERSION::equals
    );

    private static int nextId;

    private ModNetwork() {
    }

    public static void register() {
        RequiemUnofficial.LOGGER.info("Registering network channel");
        nextId = 0;
        CHANNEL.registerMessage(
                nextId(),
                SyncSoulDataPacket.class,
                SyncSoulDataPacket::encode,
                SyncSoulDataPacket::decode,
                SyncSoulDataPacket::handle
        );
        CHANNEL.registerMessage(
                nextId(),
                PossessionActionPacket.class,
                PossessionActionPacket::encode,
                PossessionActionPacket::decode,
                PossessionActionPacket::handle
        );
        CHANNEL.registerMessage(
                nextId(),
                PossessionInputPacket.class,
                PossessionInputPacket::encode,
                PossessionInputPacket::decode,
                PossessionInputPacket::handle
        );
        CHANNEL.registerMessage(
                nextId(),
                OpenSoulChoiceScreenPacket.class,
                OpenSoulChoiceScreenPacket::encode,
                OpenSoulChoiceScreenPacket::decode,
                OpenSoulChoiceScreenPacket::handle
        );
        CHANNEL.registerMessage(
                nextId(),
                InitialSoulChoicePacket.class,
                InitialSoulChoicePacket::encode,
                InitialSoulChoicePacket::decode,
                InitialSoulChoicePacket::handle
        );
        CHANNEL.registerMessage(
                nextId(),
                PossessionFxPacket.class,
                PossessionFxPacket::encode,
                PossessionFxPacket::decode,
                PossessionFxPacket::handle
        );
        CHANNEL.registerMessage(
                nextId(),
                PossessionTargetHintPacket.class,
                PossessionTargetHintPacket::encode,
                PossessionTargetHintPacket::decode,
                PossessionTargetHintPacket::handle
        );
        CHANNEL.registerMessage(
                nextId(),
                VagrantSenseTargetsPacket.class,
                VagrantSenseTargetsPacket::encode,
                VagrantSenseTargetsPacket::decode,
                VagrantSenseTargetsPacket::handle
        );
        CHANNEL.registerMessage(
                nextId(),
                VagrantInputPacket.class,
                VagrantInputPacket::encode,
                VagrantInputPacket::decode,
                VagrantInputPacket::handle
        );
        CHANNEL.registerMessage(
                nextId(),
                DeathSuspensionFxPacket.class,
                DeathSuspensionFxPacket::encode,
                DeathSuspensionFxPacket::decode,
                DeathSuspensionFxPacket::handle
        );
        CHANNEL.registerMessage(
                nextId(),
                SoulChoiceFxPacket.class,
                SoulChoiceFxPacket::encode,
                SoulChoiceFxPacket::decode,
                SoulChoiceFxPacket::handle
        );
        CHANNEL.registerMessage(
                nextId(),
                PossessionNameTagPacket.class,
                PossessionNameTagPacket::encode,
                PossessionNameTagPacket::decode,
                PossessionNameTagPacket::handle
        );
        CHANNEL.registerMessage(
                nextId(),
                RemnantVesselFxPacket.class,
                RemnantVesselFxPacket::encode,
                RemnantVesselFxPacket::decode,
                RemnantVesselFxPacket::handle
        );
    }

    public static int nextId() {
        return nextId++;
    }

    public static void sendSoulData(ServerPlayer player) {
        if (!isConnected(player)) {
            return;
        }
        player.getCapability(ModCapabilities.PLAYER_SOUL).ifPresent(soul ->
                CHANNEL.send(
                        PacketDistributor.PLAYER.with(() -> player),
                        new SyncSoulDataPacket(
                                soul.getSoulPower(),
                                soul.getIchorLevel(),
                                soul.getState(),
                                soul.getPossessedEntityId(),
                                soul.getPlayerShellId(),
                                soul.getResurrectionDimension(),
                                soul.getResurrectionPos()
                        )
                )
        );
    }

    public static void sendToPlayer(ServerPlayer player, Object packet) {
        if (!isConnected(player)) {
            return;
        }
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), packet);
    }

    public static void sendToConnectedPlayer(ServerPlayer player, Object packet) {
        sendToPlayer(player, packet);
    }

    private static boolean isConnected(ServerPlayer player) {
        return player.connection != null && player.connection.connection != null;
    }
}
