package ua.meekeyn.requiemunofficial.mixin;

import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Witch;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.Potion;
import net.minecraft.world.item.alchemy.PotionUtils;
import net.minecraft.world.item.alchemy.Potions;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import ua.meekeyn.requiemunofficial.common.possession.PossessionManager;

@Mixin(Witch.class)
public abstract class WitchMixin {

    @Redirect(
            method = "performRangedAttack",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/item/alchemy/PotionUtils;setPotion(Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/item/alchemy/Potion;)Lnet/minecraft/world/item/ItemStack;"
            )
    )
    private ItemStack requiemunofficial$preferWeaknessAgainstUndead(ItemStack stack,
                                                                    Potion potion,
                                                                    LivingEntity target,
                                                                    float distanceFactor) {
        if (target.isInvertedHealAndHarm() && !target.hasEffect(MobEffects.WEAKNESS)) {
            return PotionUtils.setPotion(stack, Potions.WEAKNESS);
        }
        return PotionUtils.setPotion(stack, potion);
    }

    @Inject(
            method = "aiStep",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/entity/monster/Witch;setItemSlot(Lnet/minecraft/world/entity/EquipmentSlot;Lnet/minecraft/world/item/ItemStack;)V",
                    ordinal = 0,
                    shift = At.Shift.AFTER
            )
    )
    private void requiemunofficial$keepBottleForPossessedWitch(CallbackInfo ci) {
        Witch self = (Witch) (Object) this;
        if (PossessionManager.getPossessorId(self) != null) {
            self.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.GLASS_BOTTLE));
        }
    }
}
