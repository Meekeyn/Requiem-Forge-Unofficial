package ua.meekeyn.requiemunofficial.gametest;

import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;

/**
 * Дрібні допоміжні засоби для тестів.
 */
public final class GameTestSupport {
    private GameTestSupport() {
    }

    /**
     * Приєднує фіктивне з'єднання, щоб ванільні виклики, які надсилають пакети
     * (наприклад, старт кулдауну предмета), не падали з NPE у безголовому тесті.
     */
    public static void attachConnection(GameTestHelper helper, ServerPlayer player) {
        if (player.connection != null) {
            return;
        }
        new ServerGamePacketListenerImpl(
                helper.getLevel().getServer(),
                new Connection(PacketFlow.SERVERBOUND),
                player
        );
    }
}
