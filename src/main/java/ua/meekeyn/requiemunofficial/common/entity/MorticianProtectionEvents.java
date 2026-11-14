package ua.meekeyn.requiemunofficial.common.entity;

import net.neoforged.fml.common.EventBusSubscriber;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.event.entity.living.LivingChangeTargetEvent;
import net.neoforged.neoforge.event.entity.living.LivingEntityUseItemEvent;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;
import ua.meekeyn.requiemunofficial.RequiemUnofficial;

@EventBusSubscriber(modid = RequiemUnofficial.MODID)
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
        if (event.getNewAboutToBeSetTarget() instanceof MorticianEntity) {
            event.setCanceled(true);
            event.setNewAboutToBeSetTarget(null);
            if (event.getEntity() instanceof net.minecraft.world.entity.Mob mob) {
                mob.setTarget(null);
            }
        }
    }

}
