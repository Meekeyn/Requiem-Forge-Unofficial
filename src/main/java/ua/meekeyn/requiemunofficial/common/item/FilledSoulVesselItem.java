package ua.meekeyn.requiemunofficial.common.item;

import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.registries.ForgeRegistries;
import ua.meekeyn.requiemunofficial.common.entity.ReleasedSoulEntity;
import ua.meekeyn.requiemunofficial.common.init.ModEntities;
import ua.meekeyn.requiemunofficial.common.init.ModItems;
import ua.meekeyn.requiemunofficial.common.record.BodyRecordData;
import ua.meekeyn.requiemunofficial.common.soul.EntitySoulManager;

import java.util.UUID;

public class FilledSoulVesselItem extends SoulVesselItem {

    public FilledSoulVesselItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult interactLivingEntity(ItemStack stack, Player player, LivingEntity target,
                                                  InteractionHand hand) {
        if (!hasSoulData(stack)) {
            return InteractionResult.PASS;
        }
        if (player.level().isClientSide) {
            return InteractionResult.SUCCESS;
        }
        if (!EntitySoulManager.isInertSoullessBody(target)) {
            sendSystemMessage(player, Component.translatable(
                    "message.requiemunofficial.soul_vessel.restore_has_soul"
            ));
            return InteractionResult.FAIL;
        }

        var targetType = ForgeRegistries.ENTITY_TYPES.getKey(target.getType());
        String targetSoulType = targetType == null ? "minecraft:unknown" : targetType.toString();
        if (!targetSoulType.equals(getSoulType(stack))) {
            sendSystemMessage(player, Component.translatable(
                    "message.requiemunofficial.soul_vessel.restore_wrong_body",
                    getSoulType(stack),
                    targetSoulType
            ));
            return InteractionResult.FAIL;
        }

        UUID recordId = getBodyRecord(stack);
        if (recordId != null) {
            UUID targetRecord = BodyRecordData.getEntityRecordId(target);
            if (!recordId.equals(targetRecord)) {
                sendSystemMessage(player, Component.translatable(
                        "message.requiemunofficial.soul_vessel.restore_wrong_body",
                        getSoulType(stack),
                        targetSoulType
                ));
                return InteractionResult.FAIL;
            }
        }

        EntitySoulManager.restoreSoul(target);
        if (recordId != null) {
            BodyRecordData.get(player.level()).invalidate(recordId);
        }
        sendSystemMessage(player, Component.translatable(
                "message.requiemunofficial.soul_vessel.restored",
                targetSoulType
        ));

        ItemStack empty = new ItemStack(ModItems.EMPTY_SOUL_VESSEL.get());
        if (stack.getCount() == 1) {
            player.setItemInHand(hand, empty);
        } else {
            stack.shrink(1);
            if (!player.getInventory().add(empty)) {
                player.drop(empty, false);
            }
        }
        return InteractionResult.CONSUME;
    }

    private static void sendSystemMessage(Player player, Component message) {
        if (!(player instanceof net.minecraft.server.level.ServerPlayer serverPlayer)
                || serverPlayer.connection != null && serverPlayer.connection.connection != null) {
            player.sendSystemMessage(message);
        }
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!hasSoulData(stack)) {
            return InteractionResultHolder.pass(stack);
        }

        if (!level.isClientSide) {
            UUID recordId = getBodyRecord(stack);
            if (recordId != null) {
                BodyRecordData data = BodyRecordData.get(level);
                var record = data.getRecord(recordId);
                if (record.isEmpty() || record.get().activeReleasedSoul()) {
                    return InteractionResultHolder.fail(stack);
                }
            }
            ReleasedSoulEntity releasedSoul =
                    new ReleasedSoulEntity(ModEntities.RELEASED_SOUL.get(), level);
            releasedSoul.setOwnerRecord(recordId);
            releasedSoul.moveTo(
                    player.getX(),
                    player.getY(0.8D),
                    player.getZ(),
                    player.getYRot(),
                    player.getXRot()
            );
            Vec3 velocity = player.getLookAngle().normalize().scale(0.15D);
            releasedSoul.setDeltaMovement(velocity);
            releasedSoul.setSoulData(getSoulType(stack), getSoulAmount(stack));
            if (!level.addFreshEntity(releasedSoul)) {
                return InteractionResultHolder.fail(stack);
            }
            if (recordId != null) {
                BodyRecordData.get(level).markActiveReleasedSoul(recordId, true);
            }
        }

        if (player.getAbilities().instabuild) {
            return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
        }
        if (stack.getCount() == 1) {
            return InteractionResultHolder.sidedSuccess(
                    new ItemStack(ModItems.EMPTY_SOUL_VESSEL.get()),
                    level.isClientSide
            );
        }

        stack.shrink(1);
        ItemStack empty = new ItemStack(ModItems.EMPTY_SOUL_VESSEL.get());
        if (!player.getInventory().add(empty)) {
            player.drop(empty, false);
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }
}
