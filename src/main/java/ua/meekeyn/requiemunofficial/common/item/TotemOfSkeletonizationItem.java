package ua.meekeyn.requiemunofficial.common.item;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import ua.meekeyn.requiemunofficial.common.transformation.TransformationManager;

import javax.annotation.Nullable;
import java.util.List;

public class TotemOfSkeletonizationItem extends Item {

    public TotemOfSkeletonizationItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult interactLivingEntity(
            ItemStack stack,
            Player player,
            LivingEntity target,
            InteractionHand hand
    ) {
        if (player.level().isClientSide()) {
            return InteractionResult.SUCCESS;
        }

        if (TransformationManager.skeletonize(target) == null) {
            player.sendSystemMessage(Component.translatable(
                    "message.requiemunofficial.transformation.not_skeletonizable"
            ));
            return InteractionResult.FAIL;
        }

        if (!player.getAbilities().instabuild) {
            stack.shrink(1);
        }
        return InteractionResult.CONSUME;
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable(
                "tooltip.requiemunofficial.totem_of_skeletonization"
        ).withStyle(ChatFormatting.GRAY));
        super.appendHoverText(stack, level, tooltip, flag);
    }
}
