package ua.meekeyn.requiemunofficial.common.ritual;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import ua.meekeyn.requiemunofficial.RequiemUnofficial;
import ua.meekeyn.requiemunofficial.common.block.entity.RunestoneBlockEntity;
import ua.meekeyn.requiemunofficial.common.block.obelisk.ObeliskMatcher;
import ua.meekeyn.requiemunofficial.common.soul.SoulManager;
import ua.meekeyn.requiemunofficial.common.soul.SoulLifecycleManager;
import ua.meekeyn.requiemunofficial.common.soul.SoulState;

@Mod.EventBusSubscriber(modid = RequiemUnofficial.MODID)
public final class ResurrectionManager {

    private ResurrectionManager() {
    }

    @SubscribeEvent
    public static void onPlayerRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }

        BlockPos headPos = SoulManager.getResurrectionPos(player);
        String dimensionId = SoulManager.getResurrectionDimension(player);
        if (headPos == null || dimensionId.isEmpty()) {
            return;
        }

        ResourceLocation dimensionLocation = ResourceLocation.tryParse(dimensionId);
        if (dimensionLocation == null) {
            cancel(player);
            return;
        }

        ResourceKey<Level> dimensionKey = ResourceKey.create(Registries.DIMENSION, dimensionLocation);
        ServerLevel level = player.getServer().getLevel(dimensionKey);
        if (level == null
                || !(level.getBlockEntity(headPos) instanceof RunestoneBlockEntity runestone)
                || !runestone.isHeadBlock()
                || !runestone.isStructureValid()
                || !runestone.isActivatedBlock()
                || !runestone.isPowered()
                || ObeliskMatcher.matchObelisk(level, headPos).isEmpty()) {
            cancel(player);
            return;
        }

        int safeY = headPos.getY() + Math.max(1, runestone.getObeliskCoreHeight()) + 1;
        BlockPos safePos = findSafePosition(level, new BlockPos(headPos.getX(), safeY, headPos.getZ()));

        player.teleportTo(
                level,
                safePos.getX() + 0.5D,
                safePos.getY(),
                safePos.getZ() + 0.5D,
                player.getYRot(),
                player.getXRot()
        );
        SoulLifecycleManager.removeShell(player, false);
        SoulManager.clearResurrectionPoint(player);
        SoulManager.setState(player, SoulState.MORTAL);
        player.sendSystemMessage(Component.translatable(
                "message.requiemunofficial.ritual.resurrection_complete"
        ));
    }

    private static BlockPos findSafePosition(ServerLevel level, BlockPos start) {
        BlockPos.MutableBlockPos checked = start.mutable();
        for (int offset = 0; offset < 8; offset++) {
            if (level.getBlockState(checked).getCollisionShape(level, checked).isEmpty()
                    && level.getBlockState(checked.above()).getCollisionShape(level, checked.above()).isEmpty()) {
                return checked.immutable();
            }
            checked.move(0, 1, 0);
        }
        return start;
    }

    private static void cancel(ServerPlayer player) {
        SoulManager.clearResurrectionPoint(player);
        player.sendSystemMessage(Component.translatable(
                "message.requiemunofficial.ritual.resurrection_lost"
        ));
    }
}
