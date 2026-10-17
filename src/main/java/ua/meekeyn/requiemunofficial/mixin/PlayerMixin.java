package ua.meekeyn.requiemunofficial.mixin;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.ProjectileWeaponItem;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import ua.meekeyn.requiemunofficial.common.possession.PossessionProjectileHooks;
import ua.meekeyn.requiemunofficial.common.possession.PossessionManager;
import ua.meekeyn.requiemunofficial.common.soul.SoulManager;
import ua.meekeyn.requiemunofficial.common.soul.VagrantRules;
import ua.meekeyn.requiemunofficial.common.util.ModTags;

import java.util.UUID;

@Mixin(Player.class)
public abstract class PlayerMixin {
    private static final float REQUIEMUNOFFICIAL_VAGRANT_SPRINT_MULTIPLIER = 1.5F;

    @Inject(method = "hurt", at = @At("HEAD"), cancellable = true)
    private void requiemunofficial$damagePossessedBody(net.minecraft.world.damagesource.DamageSource source,
                                                      float amount, CallbackInfoReturnable<Boolean> cir) {
        Player player = (Player) (Object) this;
        if (player instanceof net.minecraft.server.level.ServerPlayer serverPlayer) {
            UUID hostId = SoulManager.getPossessedEntityId(player);
            LivingEntity host = hostId == null ? null : PossessionManager.findPossessedHost(serverPlayer, hostId);
            if (host != null && host.isAlive()) {
                cir.setReturnValue(host.hurt(source, amount));
            }
        }
    }

    @Inject(method = "getProjectile", at = @At("RETURN"), cancellable = true)
    private void requiemunofficial$generatePossessedBodyArrow(ItemStack weapon,
                                                              CallbackInfoReturnable<ItemStack> cir) {
        if (!cir.getReturnValue().isEmpty()) {
            return;
        }

        Player player = (Player) (Object) this;
        LivingEntity host = requiemunofficial$getPossessedHost(player);
        if (host != null && host.getType().is(ModTags.ARROW_GENERATORS)) {
            if (weapon.getItem() instanceof ProjectileWeaponItem) {
                weapon.getOrCreateTag().putBoolean(PossessionProjectileHooks.INFINITY_SHOT_TAG, true);
            }
            cir.setReturnValue(new ItemStack(Items.ARROW));
        }
    }

    @Inject(method = "getDimensions", at = @At("HEAD"), cancellable = true)
    private void requiemunofficial$usePossessedHostDimensions(Pose pose,
                                                               CallbackInfoReturnable<EntityDimensions> cir) {
        Player player = (Player) (Object) this;
        LivingEntity host = PossessionManager.getBeta16PlayerBodyHost(player);
        if (host != null) {
            cir.setReturnValue(host.getDimensions(pose));
        }
    }

    @Inject(method = "getStandingEyeHeight", at = @At("HEAD"), cancellable = true)
    private void requiemunofficial$usePossessedHostEyeHeight(Pose pose,
                                                             EntityDimensions dimensions,
                                                             CallbackInfoReturnable<Float> cir) {
        Player player = (Player) (Object) this;
        LivingEntity host = PossessionManager.getBeta16PlayerBodyHost(player);
        if (host != null) {
            cir.setReturnValue(host.getEyeHeight(pose));
        }
    }

    private static LivingEntity requiemunofficial$getPossessedHost(Player player) {
        UUID hostId = SoulManager.getPossessedEntityId(player);
        if (hostId == null) {
            return null;
        }

        for (Entity entity : player.level().getEntities(
                player,
                player.getBoundingBox().inflate(128.0D),
                entity -> hostId.equals(entity.getUUID())
        )) {
            if (entity instanceof LivingEntity living && living.isAlive()) {
                return living;
            }
        }
        return null;
    }

    @Inject(method = "getFlyingSpeed", at = @At("RETURN"), cancellable = true)
    private void requiemunofficial$vagrantSprintFlightSpeed(CallbackInfoReturnable<Float> cir) {
        Player player = (Player) (Object) this;
        if (VagrantRules.isRestricted(player)
                && player.getAbilities().flying
                && player.isSprinting()
                && !VagrantRules.shouldSuppressBlockCollision(player)) {
            cir.setReturnValue(cir.getReturnValue() * REQUIEMUNOFFICIAL_VAGRANT_SPRINT_MULTIPLIER);
        }
    }
    @org.spongepowered.asm.mixin.injection.ModifyVariable(method = "travel", at = @At("HEAD"), argsOnly = true)
    private net.minecraft.world.phys.Vec3 requiemunofficial$bodyTravel(net.minecraft.world.phys.Vec3 input) {
        return ua.meekeyn.requiemunofficial.common.possession.PlayerBodyMovement.prepareTravel((Player) (Object) this, input);
    }

}
