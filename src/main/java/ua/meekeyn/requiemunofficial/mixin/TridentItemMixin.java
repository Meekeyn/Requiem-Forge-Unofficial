package ua.meekeyn.requiemunofficial.mixin;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Abilities;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TridentItem;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import ua.meekeyn.requiemunofficial.common.possession.PossessionManager;

@Mixin(TridentItem.class)
public abstract class TridentItemMixin {
    @Unique
    private static final ThreadLocal<Boolean> requiemunofficial$revertInstabuild =
            ThreadLocal.withInitial(() -> false);

    @Inject(method = "releaseUsing", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/projectile/ThrownTrident;shootFromRotation(Lnet/minecraft/world/entity/Entity;FFFFF)V"))
    private void requiemunofficial$giveDrownedInfinity(ItemStack stack,
                                                       Level level,
                                                       LivingEntity user,
                                                       int remainingTicks,
                                                       CallbackInfo ci) {
        if (!(user instanceof ServerPlayer player)) {
            return;
        }

        LivingEntity host = PossessionManager.findPossessedHost(player);
        if (host != null && host.getType() == EntityType.DROWNED && level.getRandom().nextFloat() < 0.8F) {
            Abilities abilities = player.getAbilities();
            if (!abilities.instabuild) {
                abilities.instabuild = true;
                requiemunofficial$revertInstabuild.set(true);
            }
        }
    }

    @Inject(method = "releaseUsing", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/player/Player;awardStat(Lnet/minecraft/stats/Stat;)V"))
    private void requiemunofficial$revertDrownedInfinity(ItemStack stack,
                                                         Level level,
                                                         LivingEntity user,
                                                         int remainingTicks,
                                                         CallbackInfo ci) {
        if (requiemunofficial$revertInstabuild.get() && user instanceof ServerPlayer player) {
            player.getAbilities().instabuild = false;
            requiemunofficial$revertInstabuild.set(false);
        }
    }
}
