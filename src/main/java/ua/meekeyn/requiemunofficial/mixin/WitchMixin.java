package ua.meekeyn.requiemunofficial.mixin;

import net.minecraft.util.RandomSource;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Witch;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.Potion;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.alchemy.Potions;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import ua.meekeyn.requiemunofficial.common.possession.PossessionManager;

import java.util.UUID;

@Mixin(Witch.class)
public abstract class WitchMixin {

    @Redirect(
            method = "performRangedAttack",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/item/alchemy/PotionContents;createItemStack(Lnet/minecraft/world/item/Item;Lnet/minecraft/core/Holder;)Lnet/minecraft/world/item/ItemStack;"
            )
    )
    private ItemStack requiemunofficial$preferWeaknessAgainstUndead(net.minecraft.world.item.Item item,
                                                                    net.minecraft.core.Holder<Potion> potion,
                                                                    LivingEntity target,
                                                                    float distanceFactor) {
        if (target.isInvertedHealAndHarm() && !target.hasEffect(MobEffects.WEAKNESS)) {
            return PotionContents.createItemStack(item, Potions.WEAKNESS);
        }
        return PotionContents.createItemStack(item, potion);
    }

    // Witch "Drinking speed penalty" (vanilla Witch.SPEED_MODIFIER_DRINKING_UUID, private).
    private static final net.minecraft.resources.ResourceLocation REQUIEMUNOFFICIAL$DRINKING_SPEED_MODIFIER =
            net.minecraft.resources.ResourceLocation.withDefaultNamespace("drinking");

    // A possessed witch is driven by the player: it must not start drinking potions on its own
    // (heal/water breathing/fire resistance/swiftness). Vanilla's drink start also applies the
    // -0.25 movement speed penalty, which the Player Body copies and which stops the player dead.
    @Inject(method = "aiStep", at = @At("HEAD"))
    private void requiemunofficial$dropDrinkingPenaltyWhilePossessed(CallbackInfo ci) {
        Witch self = (Witch) (Object) this;
        if (!self.level().isClientSide() && PossessionManager.getPossessorId(self) != null) {
            AttributeInstance speed = self.getAttribute(Attributes.MOVEMENT_SPEED);
            if (speed != null && speed.getModifier(REQUIEMUNOFFICIAL$DRINKING_SPEED_MODIFIER) != null) {
                speed.removeModifier(REQUIEMUNOFFICIAL$DRINKING_SPEED_MODIFIER);
            }
        }
    }

    // Every potion-start roll in Witch.aiStep begins with random.nextFloat() < chance; returning
    // 1.0F for a possessed witch makes all of them fail, so the auto-drink branch never starts.
    // (The only other roll in the method is the cosmetic ambient-particle event.)
    @Redirect(
            method = "aiStep",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/util/RandomSource;nextFloat()F"
            )
    )
    private float requiemunofficial$noAutoDrinkWhilePossessed(RandomSource random) {
        Witch self = (Witch) (Object) this;
        if (!self.level().isClientSide() && PossessionManager.getPossessorId(self) != null) {
            return 1.0F;
        }
        return random.nextFloat();
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
