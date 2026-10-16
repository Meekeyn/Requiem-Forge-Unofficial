package ua.meekeyn.requiemunofficial.mixin;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.MilkBucketItem;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import ua.meekeyn.requiemunofficial.common.possession.PossessionManager;
import ua.meekeyn.requiemunofficial.common.util.ModTags;

@Mixin(MilkBucketItem.class)
public abstract class MilkBucketItemMixin {
    @Inject(method = "finishUsingItem", at = @At("RETURN"))
    private void requiemunofficial$regenPossessedSkeletons(ItemStack stack,
                                                           Level level,
                                                           LivingEntity user,
                                                           CallbackInfoReturnable<ItemStack> cir) {
        if (!(user instanceof ServerPlayer player)) {
            return;
        }

        LivingEntity host = PossessionManager.findPossessedHost(player);
        if (host != null && host.getType().is(ModTags.SKELETONS)) {
            host.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 30 * 20));
        }
    }
}
