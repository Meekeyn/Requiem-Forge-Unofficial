package ua.meekeyn.requiemunofficial.common.entity;

import net.minecraft.world.item.Items;
import net.minecraftforge.event.entity.living.LivingChangeTargetEvent;
import net.minecraftforge.event.entity.living.LivingEntityUseItemEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import ua.meekeyn.requiemunofficial.RequiemUnofficial;

@Mod.EventBusSubscriber(modid = RequiemUnofficial.MODID)
public final class MorticianProtectionEvents {
    private MorticianProtectionEvents() {
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onUseItemStart(LivingEntityUseItemEvent.Start event) {
        if (event.getEntity() instanceof MorticianEntity && event.getItem().is(Items.POTION)) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onTargetChange(LivingChangeTargetEvent event) {
        if (event.getNewTarget() instanceof MorticianEntity) {
            event.setCanceled(true);
            event.setNewTarget(null);
            if (event.getEntity() instanceof net.minecraft.world.entity.Mob mob) {
                mob.setTarget(null);
            }
        }
    }

}
