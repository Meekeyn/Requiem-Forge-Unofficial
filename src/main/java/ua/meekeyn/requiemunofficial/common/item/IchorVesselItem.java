package ua.meekeyn.requiemunofficial.common.item;

import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.Level;
import ua.meekeyn.requiemunofficial.common.init.ModEffects;
import ua.meekeyn.requiemunofficial.common.init.ModItems;
import ua.meekeyn.requiemunofficial.common.soul.SoulManager;

import javax.annotation.Nullable;
import java.util.List;

public class IchorVesselItem extends Item {

    private static final int USE_DURATION = 20;
    private static final int EFFECT_DURATION = 20 * 60 * 10;
    private final String ichorType;

    public IchorVesselItem(Properties properties, String ichorType) {
        super(properties);
        this.ichorType = ichorType;
    }

    public String getIchorType() {
        return ichorType;
    }

    public MobEffect getEffect() {
        return switch (ichorType) {
            case "attrition" -> ModEffects.ATTRITION.get();
            case "emancipation" -> ModEffects.EMANCIPATION.get();
            case "penance" -> ModEffects.PENANCE.get();
            case "reclamation" -> ModEffects.RECLAMATION.get();
            default -> null;
        };
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!canActivate(player)) {
            return InteractionResultHolder.fail(stack);
        }
        player.startUsingItem(hand);
        return InteractionResultHolder.consume(stack);
    }

    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity user) {
        if (!level.isClientSide) {
            if (!canActivate(user)) {
                return stack;
            }
            MobEffect effect = getEffect();
            if (effect != null) {
                user.addEffect(new MobEffectInstance(effect, EFFECT_DURATION, 0, false, false, true));
            }
            if (user instanceof Player player) {
                SoulManager.addIchor(player, 1);
                player.sendSystemMessage(Component.translatable(
                        "message.requiemunofficial.ichor_vessel.absorbed",
                        SoulManager.getIchorLevel(player),
                        Component.translatable("ichor_type.requiemunofficial." + ichorType)
                ));
            }
        }

        if (!(user instanceof Player player) || !player.getAbilities().instabuild) {
            stack.shrink(1);
            if (stack.isEmpty()) {
                return new ItemStack(ModItems.EMPTY_SOUL_VESSEL.get());
            }
            if (user instanceof Player consumingPlayer) {
                ItemStack empty = new ItemStack(ModItems.EMPTY_SOUL_VESSEL.get());
                if (!consumingPlayer.getInventory().add(empty)) {
                    consumingPlayer.drop(empty, false);
                }
            }
        }
        return stack;
    }

    private boolean canActivate(LivingEntity user) {
        MobEffect effect = getEffect();
        if (effect == null) {
            return false;
        }
        return switch (ichorType) {
            case "reclamation" -> user.getHealth() < user.getMaxHealth();
            case "attrition", "penance" -> !user.hasEffect(effect);
            default -> true;
        };
    }

    @Override
    public int getUseDuration(ItemStack stack) {
        return USE_DURATION;
    }

    @Override
    public UseAnim getUseAnimation(ItemStack stack) {
        return UseAnim.BOW;
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level,
                                List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, level, tooltip, flag);
        tooltip.add(Component.translatable(
                "tooltip.requiemunofficial.ichor_vessel.type",
                Component.translatable("ichor_type.requiemunofficial." + ichorType)
        ));
        tooltip.add(Component.translatable("tooltip.requiemunofficial.ichor_vessel.use"));
    }
}
