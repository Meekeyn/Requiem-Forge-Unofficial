package ua.meekeyn.requiemunofficial.common.item;

import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import ua.meekeyn.requiemunofficial.common.init.ModItems;
import ua.meekeyn.requiemunofficial.common.soul.ModCapabilities;
import ua.meekeyn.requiemunofficial.common.soul.SoulManager;
import ua.meekeyn.requiemunofficial.common.soul.SoulState;

public class DebugItem extends Item {

    public DebugItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand usedHand) {
        ItemStack debugStack = player.getItemInHand(usedHand);

        if (!level.isClientSide) {
            if (player.isShiftKeyDown()) {
                if (usedHand == InteractionHand.OFF_HAND) {
                    SoulManager.clearResurrectionPoint(player);
                    player.sendSystemMessage(Component.literal("Resurrection binding cleared"));
                    return InteractionResultHolder.success(debugStack);
                }

                SoulState nextState = switch (SoulManager.getState(player)) {
                    case MORTAL -> SoulState.REMNANT;
                    case REMNANT -> SoulState.VAGRANT;
                    case VAGRANT -> SoulState.MORTAL;
                };
                SoulManager.setState(player, nextState);
                player.sendSystemMessage(Component.literal("Soul state: " + nextState.name().toLowerCase()));
                return InteractionResultHolder.success(debugStack);
            }

            // Створює тестовий заповнений сосуд душі для швидкої перевірки NBT
            ItemStack vesselStack = new ItemStack(ModItems.FILLED_SOUL_VESSEL.get());
            SoulVesselItem.setSoulData(vesselStack, "human", 1);

            SoulManager.addSoul(player, 1);
            SoulManager.addIchor(player, 1);
            int currentSoul = SoulManager.getSoulPower(player);
            player.sendSystemMessage(Component.literal("Soul increased to " + currentSoul));

            if (!player.getInventory().add(vesselStack)) {
                player.drop(vesselStack, false);
            }
        }

        return InteractionResultHolder.sidedSuccess(debugStack, level.isClientSide);
    }
}
