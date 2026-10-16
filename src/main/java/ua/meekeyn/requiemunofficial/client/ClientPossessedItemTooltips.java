package ua.meekeyn.requiemunofficial.client;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.AbstractSkeleton;
import net.minecraft.world.entity.monster.CrossbowAttackMob;
import net.minecraft.world.entity.monster.Drowned;
import net.minecraft.world.entity.monster.Witch;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.CrossbowItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.MilkBucketItem;
import net.minecraft.world.item.TridentItem;
import net.minecraft.world.item.alchemy.PotionUtils;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import ua.meekeyn.requiemunofficial.RequiemUnofficial;
import ua.meekeyn.requiemunofficial.common.possession.PossessedItemManager;
import ua.meekeyn.requiemunofficial.common.soul.SoulManager;
import ua.meekeyn.requiemunofficial.common.util.ModTags;

import java.util.UUID;

@Mod.EventBusSubscriber(modid = RequiemUnofficial.MODID, value = Dist.CLIENT)
public final class ClientPossessedItemTooltips {
    private ClientPossessedItemTooltips() {
    }

    @SubscribeEvent
    public static void onItemTooltip(ItemTooltipEvent event) {
        Player player = event.getEntity();
        if (player == null) {
            player = Minecraft.getInstance().player;
        }
        if (player == null) {
            return;
        }

        LivingEntity host = findPossessedHost(player);
        if (!(host instanceof Mob)) {
            return;
        }

        ItemStack stack = event.getItemStack();
        event.getToolTip().addAll(PossessedItemManager.buildTooltips(player, host, stack));
        contextualTooltip(host, stack, event);
    }

    private static LivingEntity findPossessedHost(Player player) {
        UUID hostId = SoulManager.getPossessedEntityId(player);
        if (hostId == null || player.level() == null) {
            return null;
        }
        for (Entity entity : player.level().getEntities(
                player,
                player.getBoundingBox().inflate(128.0D),
                entity -> hostId.equals(entity.getUUID()))) {
            if (hostId.equals(entity.getUUID()) && entity instanceof LivingEntity living) {
                return living;
            }
        }
        return null;
    }

    private static void contextualTooltip(LivingEntity host, ItemStack stack, ItemTooltipEvent event) {
        String key;
        if (host.getType().is(ModTags.ARROW_GENERATORS) && (stack.getItem() instanceof BowItem || stack.getItem() instanceof CrossbowItem)) {
            key = "requiemunofficial:tooltip.ammo_generator";
        } else if (host instanceof CrossbowAttackMob && stack.getItem() instanceof CrossbowItem) {
            key = "requiemunofficial:tooltip.bolt_hoarder";
        } else if (host instanceof AbstractSkeleton && stack.getItem() instanceof BowItem) {
            key = "requiemunofficial:tooltip.skeletal_efficiency";
        } else if (host instanceof AbstractSkeleton && stack.getItem() instanceof MilkBucketItem) {
            key = "requiemunofficial:tooltip.calcium_bucket";
        } else if (host instanceof Drowned && stack.getItem() instanceof TridentItem) {
            key = "requiemunofficial:tooltip.drowned_grip";
        } else if (host instanceof Witch && stack.is(Items.POTION) && PotionUtils.getPotion(stack) == Potions.WATER) {
            key = "requiemunofficial:tooltip.witch_brew_base";
        } else {
            return;
        }
        event.getToolTip().add(Component.translatable(key).withStyle(ChatFormatting.DARK_GRAY));
    }
}
