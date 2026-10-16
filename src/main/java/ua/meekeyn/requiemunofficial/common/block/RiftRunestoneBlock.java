package ua.meekeyn.requiemunofficial.common.block;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraftforge.network.NetworkHooks;
import ua.meekeyn.requiemunofficial.common.block.entity.RunestoneBlockEntity;
import ua.meekeyn.requiemunofficial.common.block.obelisk.ObeliskMatcher;
import ua.meekeyn.requiemunofficial.common.effect.AttritionMobEffect;
import ua.meekeyn.requiemunofficial.common.init.ModEffects;
import ua.meekeyn.requiemunofficial.common.menu.RiftMenu;
import ua.meekeyn.requiemunofficial.common.record.ObeliskRecordData;
import ua.meekeyn.requiemunofficial.common.remnant.PlayerRiftTracker;
import ua.meekeyn.requiemunofficial.common.soul.SoulManager;
import ua.meekeyn.requiemunofficial.common.soul.SoulState;

import java.util.Optional;

public class RiftRunestoneBlock extends RunestoneBlock {

    public RiftRunestoneBlock(Properties properties, String runeType, int maxLevel, MobEffect effect) {
        super(properties, runeType, maxLevel, effect);
    }

    public RiftRunestoneBlock(Properties properties, String runeType, int maxLevel) {
        super(properties, runeType, maxLevel);
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player,
                                 InteractionHand hand, BlockHitResult hit) {
        InteractionResult superResult = super.use(state, level, pos, player, hand, hit);
        if (superResult.consumesAction()) {
            return superResult;
        }

        if (!state.getValue(ACTIVATED)) {
            return InteractionResult.PASS;
        }

        if (!(player instanceof ServerPlayer serverPlayer)) {
            return InteractionResult.SUCCESS;
        }

        Optional<BlockPos> origin = ObeliskMatcher.findObeliskOrigin(level, pos);
        if (origin.isEmpty()
                || !(level.getBlockEntity(origin.get()) instanceof RunestoneBlockEntity runestone)) {
            return InteractionResult.PASS;
        }

        BlockPos headPos = origin.get();
        ObeliskRecordData.ObeliskRecord record = runestone.getOrCreateObeliskRecord();
        if (record != null && PlayerRiftTracker.addKnownRift(serverPlayer, record)) {
            serverPlayer.sendSystemMessage(Component.translatable(
                    "message.requiemunofficial.rift.witnessed",
                    record.descriptor().resolveName()
            ));
        }

        if (!canUseRift(serverPlayer, runestone)) {
            return InteractionResult.PASS;
        }

        MenuProvider provider = new SimpleMenuProvider(
                (containerId, inventory, menuPlayer) -> new RiftMenu(containerId, inventory, headPos),
                getMenuTitle(runestone)
        );

        NetworkHooks.openScreen(serverPlayer, provider, buffer -> RiftMenu.writeOpeningData(buffer, headPos, serverPlayer));
        return InteractionResult.CONSUME;
    }

    protected Component getMenuTitle(RunestoneBlockEntity runestone) {
        if (runestone.getCustomName() != null) {
            return runestone.getCustomName();
        }

        return Component.translatable("container.requiemunofficial.rift");
    }

    public static boolean canUseRift(Player player, RunestoneBlockEntity runestone) {
        return runestone.isHeadBlock()
                && runestone.isStructureValid()
                && runestone.isActivatedBlock()
                && runestone.isPowered()
                && runestone.getRuneLevel("rift") > 0
                && isIncorporealVagrant(player)
                && !hasMaximumAttrition(player);
    }

    private static boolean isIncorporealVagrant(Player player) {
        return SoulManager.getState(player) == SoulState.VAGRANT
                && SoulManager.getPossessedEntityId(player) == null;
    }

    private static boolean hasMaximumAttrition(Player player) {
        MobEffectInstance attrition = player.getEffect(ModEffects.ATTRITION.get());
        return attrition != null && attrition.getAmplifier() >= AttritionMobEffect.MAX_LEVEL;
    }

    @Override
    public void applyEffect(ServerPlayer player, int runeLevel, int obeliskWidth) {
        // Rift runes provide a menu instead of a status effect.
    }
}
