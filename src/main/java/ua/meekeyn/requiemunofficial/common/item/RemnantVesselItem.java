package ua.meekeyn.requiemunofficial.common.item;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import ua.meekeyn.requiemunofficial.common.init.ModItems;
import ua.meekeyn.requiemunofficial.common.init.ModSounds;
import ua.meekeyn.requiemunofficial.common.network.ModNetwork;
import ua.meekeyn.requiemunofficial.common.network.RemnantVesselFxPacket;
import ua.meekeyn.requiemunofficial.common.possession.PossessedItemManager;
import ua.meekeyn.requiemunofficial.common.possession.PossessionManager;
import ua.meekeyn.requiemunofficial.common.soul.SoulManager;
import ua.meekeyn.requiemunofficial.common.soul.SoulState;
import ua.meekeyn.requiemunofficial.common.advancement.ModCriteria;

import javax.annotation.Nullable;
import java.util.List;
import java.util.UUID;

public class RemnantVesselItem extends Item {

    private final SoulState targetState;
    private final ChatFormatting tooltipColor;
    private final String tooltipKey;

    public RemnantVesselItem(Properties properties, SoulState targetState,
                             ChatFormatting tooltipColor, String tooltipKey) {
        super(properties);
        this.targetState = targetState;
        this.tooltipColor = tooltipColor;
        this.tooltipKey = tooltipKey;
    }

    public boolean canApply(SoulState currentState) {
        return targetState == SoulState.MORTAL
                ? currentState != SoulState.MORTAL
                : currentState == SoulState.MORTAL;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!(player instanceof ServerPlayer serverPlayer) || level.isClientSide) {
            return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
        }

        SoulState currentState = SoulManager.getState(serverPlayer);
        if (!canApply(currentState)) {
            serverPlayer.sendSystemMessage(Component.translatable(
                    "message.requiemunofficial.remnant_vessel.no_change"
            ));
            return InteractionResultHolder.success(stack);
        }

        if (targetState == SoulState.MORTAL) {
            return becomeMortal(serverPlayer, stack);
        }

        SoulManager.setState(serverPlayer, SoulState.REMNANT);
        ModCriteria.MADE_REMNANT_CHOICE.trigger(serverPlayer, SoulState.REMNANT);
        serverPlayer.setInvisible(false);
        playConversionSound(serverPlayer, true);
        playConversionFx(serverPlayer);
        sendMessage(serverPlayer, Component.translatable(
                "message.requiemunofficial.remnant_vessel.became_remnant"
        ));
        return InteractionResultHolder.success(exchangeForEmpty(serverPlayer, stack));
    }

    private InteractionResultHolder<ItemStack> becomeMortal(ServerPlayer player, ItemStack stack) {
        UUID possessedId = SoulManager.getPossessedEntityId(player);
        LivingEntity host = possessedId == null ? null : PossessionManager.findPossessedHost(player, possessedId);

        if (host != null) {
            if (!PossessedItemManager.hasPendingBodyCure(player, host)) {
                return InteractionResultHolder.success(stack);
            }
            PossessionManager.restorePlayerBodyFromHost(player, host);
        }

        SoulManager.setState(player, SoulState.MORTAL);
        ModCriteria.MADE_REMNANT_CHOICE.trigger(player, SoulState.MORTAL);
        player.setInvisible(false);
        playConversionSound(player, false);
        playConversionFx(player);
        sendMessage(player, Component.translatable(
                "message.requiemunofficial.remnant_vessel.became_mortal"
        ));
        return InteractionResultHolder.success(exchangeForEmpty(player, stack));
    }

    private static void sendMessage(ServerPlayer player, Component message) {
        if (player.connection != null && player.connection.connection != null) {
            player.sendSystemMessage(message);
        }
    }

    private static void playConversionSound(ServerPlayer player, boolean remnant) {
        player.level().playSound(
                null,
                player.blockPosition(),
                ModSounds.ITEM_OPUS_USE.get(),
                SoundSource.PLAYERS,
                1.0F,
                0.1F
        );
        player.level().playSound(
                null,
                player.blockPosition(),
                remnant ? ModSounds.EFFECT_BECOME_REMNANT.get() : ModSounds.EFFECT_BECOME_MORTAL.get(),
                SoundSource.PLAYERS,
                1.4F,
                0.1F
        );
    }

    private void playConversionFx(ServerPlayer player) {
        ModNetwork.sendToConnectedPlayer(
                player,
                new RemnantVesselFxPacket(targetState, new ItemStack(this))
        );
    }

    private static ItemStack exchangeForEmpty(Player player, ItemStack stack) {
        if (player.getAbilities().instabuild) {
            return stack;
        }
        if (stack.getCount() == 1) {
            return new ItemStack(ModItems.EMPTY_SOUL_VESSEL.get());
        }

        stack.shrink(1);
        ItemStack empty = new ItemStack(ModItems.EMPTY_SOUL_VESSEL.get());
        if (!player.getInventory().add(empty)) {
            player.drop(empty, false);
        }
        return stack;
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return true;
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level,
                                List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable(tooltipKey).withStyle(tooltipColor));
        super.appendHoverText(stack, level, tooltip, flag);
    }
}
