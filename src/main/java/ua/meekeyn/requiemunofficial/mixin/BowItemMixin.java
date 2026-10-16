package ua.meekeyn.requiemunofficial.mixin;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import ua.meekeyn.requiemunofficial.common.possession.PossessionManager;
import ua.meekeyn.requiemunofficial.common.possession.PossessionProjectileHooks;
import ua.meekeyn.requiemunofficial.common.util.ModTags;

@Mixin(BowItem.class)
public abstract class BowItemMixin {
    @ModifyVariable(method = "releaseUsing", ordinal = 0, at = @At("STORE"))
    private boolean requiemunofficial$giveSkeletonInfinity(boolean infinite,
                                                           ItemStack stack,
                                                           Level level,
                                                           LivingEntity user,
                                                           int remainingTicks) {
        CompoundTag tag = stack.getTag();
        if (tag != null && tag.getBoolean(PossessionProjectileHooks.INFINITY_SHOT_TAG)) {
            tag.remove(PossessionProjectileHooks.INFINITY_SHOT_TAG);
            return true;
        }

        if (user instanceof net.minecraft.server.level.ServerPlayer player) {
            LivingEntity host = PossessionManager.findPossessedHost(player);
            if (host != null && host.getType().is(ModTags.HUMANOID_SKELETONS)) {
                return infinite || level.getRandom().nextFloat() < 0.8F;
            }
        }
        return infinite;
    }
}
