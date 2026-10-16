package ua.meekeyn.requiemunofficial.common.soul;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;
import ua.meekeyn.requiemunofficial.RequiemUnofficial;
import ua.meekeyn.requiemunofficial.common.config.RequiemConfig;
import ua.meekeyn.requiemunofficial.common.init.ModItems;
import ua.meekeyn.requiemunofficial.common.item.SoulVesselItem;
import ua.meekeyn.requiemunofficial.common.util.ModTags;

@Mod.EventBusSubscriber(modid = RequiemUnofficial.MODID)
public final class SoulHarvestEvents {

    private SoulHarvestEvents() {
    }

    @SubscribeEvent
    public static void onMobDeath(LivingDeathEvent event) {
        LivingEntity entity = event.getEntity();
        if (!(event.getSource().getEntity() instanceof Player player)
                || !(entity instanceof Mob)
                || (entity.getType().is(ModTags.SOUL_CAPTURE_BLACKLIST)
                && !entity.getType().is(ModTags.SOUL_AGGREGATES))) {
            return;
        }

        if (player.getRandom().nextDouble() > RequiemConfig.SOUL_CAPTURE_CHANCE.get()) {
            return;
        }

        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack stack = player.getInventory().getItem(i);
            if (!stack.is(ModItems.EMPTY_SOUL_VESSEL.get())) {
                continue;
            }

            SoulKind soulKind = SoulKind.classify(entity);
            int soulAmount = calculateSoulAmount(entity, soulKind);
            var entityTypeId = ForgeRegistries.ENTITY_TYPES.getKey(entity.getType());
            String soulType = entityTypeId == null ? "minecraft:unknown" : entityTypeId.toString();

            ItemStack filled = new ItemStack(ModItems.FILLED_SOUL_VESSEL.get());
            SoulVesselItem.setSoulData(filled, soulType, soulKind, soulAmount);
            stack.shrink(1);

            if (!player.getInventory().add(filled)) {
                player.drop(filled, false);
            }
            return;
        }
    }

    public static int calculateSoulAmount(LivingEntity entity, SoulKind soulKind) {
        int healthUnits = Math.max(1, (int) Math.ceil(entity.getMaxHealth() / 20.0F));
        int minimum = soulKind == SoulKind.AGGREGATE ? 4 : 1;
        return Math.min(SoulVesselItem.CAPACITY, Math.max(minimum, healthUnits));
    }
}
