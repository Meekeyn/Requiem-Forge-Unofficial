package ua.meekeyn.requiemunofficial.mixin;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.CrossbowItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import ua.meekeyn.requiemunofficial.common.possession.PossessionManager;
import ua.meekeyn.requiemunofficial.common.possession.PossessionProjectileHooks;
import ua.meekeyn.requiemunofficial.common.util.ModTags;

@Mixin(CrossbowItem.class)
public abstract class CrossbowItemMixin {
    @ModifyVariable(method = "tryLoadProjectiles", at = @At(value = "STORE", ordinal = 0), ordinal = 0)
    private static boolean requiemunofficial$giveCrossbowInfinity(boolean creative,
                                                                  LivingEntity shooter,
                                                                  ItemStack crossbow) {
        if (creative || !(shooter instanceof ServerPlayer player)) {
            return creative;
        }

        LivingEntity host = PossessionManager.findPossessedHost(player);
        if (host != null
                && host.getType().is(ModTags.HUMANOID_SKELETONS)
                && (shooter.getRandom().nextFloat() < 0.5F || shooter.level().isClientSide())) {
            crossbow.getOrCreateTag().putBoolean(PossessionProjectileHooks.INFINITY_SHOT_TAG, true);
            return true;
        }
        return false;
    }

    @ModifyVariable(method = "shootProjectile", at = @At("HEAD"), ordinal = 0, argsOnly = true)
    private static boolean requiemunofficial$preventGeneratedBoltPickup(boolean creative,
                                                                        Level level,
                                                                        LivingEntity shooter,
                                                                        InteractionHand hand,
                                                                        ItemStack crossbow,
                                                                        ItemStack projectile,
                                                                        float soundPitch,
                                                                        boolean creative2,
                                                                        float velocity,
                                                                        float inaccuracy,
                                                                        float projectileAngle) {
        if (!level.isClientSide() && projectileAngle == 0.0F) {
            CompoundTag tag = crossbow.getTag();
            if (tag != null && tag.getBoolean(PossessionProjectileHooks.INFINITY_SHOT_TAG)) {
                tag.remove(PossessionProjectileHooks.INFINITY_SHOT_TAG);
                return true;
            }
        }
        return creative;
    }
}
