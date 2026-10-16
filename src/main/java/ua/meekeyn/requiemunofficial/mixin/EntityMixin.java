package ua.meekeyn.requiemunofficial.mixin;

import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.animal.Panda;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.shapes.BooleanOp;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import ua.meekeyn.requiemunofficial.common.possession.PossessionManager;
import ua.meekeyn.requiemunofficial.common.soul.VagrantRules;

import java.util.List;

@Mixin(Entity.class)
public abstract class EntityMixin {

    @Inject(method = "canBeHitByProjectile", at = @At("HEAD"), cancellable = true)
    private void requiemunofficial$projectilesHitHostInsteadOfPossessor(CallbackInfoReturnable<Boolean> cir) {
        if ((Object) this instanceof Player player && PossessionManager.isPossessing(player)) {
            cir.setReturnValue(false);
        }
    }

    @Inject(method = "isPushable", at = @At("HEAD"), cancellable = true)
    private void requiemunofficial$vagrantsAreNotPushable(CallbackInfoReturnable<Boolean> cir) {
        Entity self = (Entity) (Object) this;
        if (requiemunofficial$isRestrictedVagrant(self)) {
            cir.setReturnValue(false);
        }
    }

    @Inject(method = "canCollideWith", at = @At("HEAD"), cancellable = true)
    private void requiemunofficial$vagrantsDoNotCollide(Entity other, CallbackInfoReturnable<Boolean> cir) {
        Entity self = (Entity) (Object) this;
        if (requiemunofficial$isRestrictedVagrant(self)
                || requiemunofficial$isRestrictedVagrant(other)
                || PossessionManager.isPossessorHostCollisionPair(self, other)) {
            cir.setReturnValue(false);
        }
    }

    @Inject(method = "push(Lnet/minecraft/world/entity/Entity;)V", at = @At("HEAD"), cancellable = true)
    private void requiemunofficial$cancelVagrantEntityPush(Entity other, CallbackInfo ci) {
        Entity self = (Entity) (Object) this;
        if (requiemunofficial$isRestrictedVagrant(self)
                || requiemunofficial$isRestrictedVagrant(other)
                || PossessionManager.isPossessorHostCollisionPair(self, other)) {
            ci.cancel();
        }
    }

    @Inject(method = "gameEvent(Lnet/minecraft/world/level/gameevent/GameEvent;)V", at = @At("HEAD"), cancellable = true)
    private void requiemunofficial$suppressSoulGameEvent(GameEvent event, CallbackInfo ci) {
        if (requiemunofficial$shouldSuppressGameEvent((Entity) (Object) this, event)) {
            ci.cancel();
        }
    }

    @Inject(method = "gameEvent(Lnet/minecraft/world/level/gameevent/GameEvent;Lnet/minecraft/world/entity/Entity;)V",
            at = @At("HEAD"), cancellable = true)
    private void requiemunofficial$suppressSoulGameEvent(GameEvent event, Entity source, CallbackInfo ci) {
        if (requiemunofficial$shouldSuppressGameEvent((Entity) (Object) this, event)) {
            ci.cancel();
        }
    }

    @Inject(method = "getDimensions", at = @At("RETURN"), cancellable = true)
    private void requiemunofficial$scaleScaredPossessedPanda(Pose pose,
                                                             CallbackInfoReturnable<EntityDimensions> cir) {
        Entity self = (Entity) (Object) this;
        if (self instanceof Panda panda
                && PossessionManager.getPossessorId(panda) != null
                && panda.isShiftKeyDown()) {
            cir.setReturnValue(cir.getReturnValue().scale(1.0F, 1.85F));
        }
    }

    @Inject(method = "isVisuallyCrawling", at = @At("HEAD"), cancellable = true)
    private void requiemunofficial$vagrantsDoNotVisuallyCrawl(CallbackInfoReturnable<Boolean> cir) {
        if (requiemunofficial$isRestrictedVagrant((Entity) (Object) this)) {
            cir.setReturnValue(false);
        }
    }

    @Redirect(
            method = "collideBoundingBox",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/level/Level;getBlockCollisions(Lnet/minecraft/world/entity/Entity;Lnet/minecraft/world/phys/AABB;)Ljava/lang/Iterable;"
            )
    )
    private static Iterable<VoxelShape> requiemunofficial$vagrantBlockCollisions(Level level,
                                                                                 Entity entity,
                                                                                 AABB box) {
        if (!(entity instanceof Player player) || !VagrantRules.shouldSuppressBlockCollision(player)) {
            return level.getBlockCollisions(entity, box);
        }

        return requiemunofficial$phaseBlockCollisions(level, player, box, VagrantRules.shouldSuppressOnlyLeafCollision(player));
    }

    private static Iterable<VoxelShape> requiemunofficial$phaseBlockCollisions(Level level,
                                                                               Player player,
                                                                               AABB box,
                                                                               boolean leavesOnly) {
        List<VoxelShape> shapes = new java.util.ArrayList<>();
        VoxelShape entityShape = Shapes.create(box);
        int minX = Mth.floor(box.minX);
        int minY = Mth.floor(box.minY);
        int minZ = Mth.floor(box.minZ);
        int maxX = Mth.floor(box.maxX);
        int maxY = Mth.floor(box.maxY);
        int maxZ = Mth.floor(box.maxZ);
        for (BlockPos pos : BlockPos.betweenClosed(minX, minY, minZ, maxX, maxY, maxZ)) {
            BlockState state = level.getBlockState(pos);
            if (state.isAir() || VagrantRules.shouldSuppressCollisionWith(player, state)) {
                continue;
            }
            VoxelShape shape = state.getCollisionShape(level, pos).move(pos.getX(), pos.getY(), pos.getZ());
            if (!shape.isEmpty() && Shapes.joinIsNotEmpty(shape, entityShape, BooleanOp.AND)) {
                shapes.add(shape);
            }
        }
        return shapes;
    }

    private static boolean requiemunofficial$shouldSuppressGameEvent(Entity entity, GameEvent event) {
        if (!(entity instanceof Player player)) {
            return false;
        }
        if (PossessionManager.isPossessing(player)) {
            return true;
        }
        return event == GameEvent.STEP && VagrantRules.isRestricted(player);
    }

    private static boolean requiemunofficial$isRestrictedVagrant(Entity entity) {
        return entity instanceof Player player && VagrantRules.isRestricted(player);
    }

}
