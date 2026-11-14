package ua.meekeyn.requiemunofficial.mixin;

import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Unit;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.CrossbowItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ProjectileWeaponItem;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import ua.meekeyn.requiemunofficial.common.possession.PossessionManager;
import ua.meekeyn.requiemunofficial.common.possession.PossessionProjectileHooks;
import ua.meekeyn.requiemunofficial.common.util.ItemNbt;
import ua.meekeyn.requiemunofficial.common.util.ModTags;

/**
 * 1.21: боєприпас списується в ProjectileWeaponItem#useAmmo. Тут скелет-носій отримує «нескінченні» стріли
 * (лук — 80%, арбалет — 50%), а згенерована для тіла стріла не списується і не підбирається.
 */
@Mixin(ProjectileWeaponItem.class)
public abstract class ProjectileWeaponItemMixin {

    @Inject(method = "useAmmo", at = @At("HEAD"), cancellable = true)
    private static void requiemunofficial$infiniteAmmo(ItemStack weapon, ItemStack ammo, LivingEntity shooter,
                                                       boolean intangible, CallbackInfoReturnable<ItemStack> cir) {
        if (intangible || ammo.isEmpty()) {
            return;
        }

        boolean infinite = false;
        CompoundTag tag = ItemNbt.getTag(weapon);
        if (tag != null && tag.getBoolean(PossessionProjectileHooks.INFINITY_SHOT_TAG)) {
            ItemNbt.removeKey(weapon, PossessionProjectileHooks.INFINITY_SHOT_TAG);
            infinite = true;
        } else if (shooter instanceof ServerPlayer player) {
            LivingEntity host = PossessionManager.findPossessedHost(player);
            if (host != null && host.getType().is(ModTags.HUMANOID_SKELETONS)) {
                if (weapon.getItem() instanceof BowItem) {
                    infinite = shooter.level().getRandom().nextFloat() < 0.8F;
                } else if (weapon.getItem() instanceof CrossbowItem) {
                    infinite = shooter.getRandom().nextFloat() < 0.5F;
                }
            }
        }

        if (infinite) {
            ItemStack copy = ammo.copyWithCount(1);
            copy.set(DataComponents.INTANGIBLE_PROJECTILE, Unit.INSTANCE);
            cir.setReturnValue(copy);
        }
    }
}
