package ua.meekeyn.requiemunofficial.mixin;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.util.Mth;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.shapes.BooleanOp;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import ua.meekeyn.requiemunofficial.common.soul.VagrantRules;

@Mixin(ServerGamePacketListenerImpl.class)
public abstract class ServerGamePacketListenerVagrantCollisionMixin {
    @Unique private static final double REQUIEMUNOFFICIAL_COLLISION_EPSILON = 9.999999747378752E-6D;

    @Shadow public ServerPlayer player;

    @Inject(method = "isPlayerCollidingWithAnythingNew", at = @At("RETURN"), cancellable = true)
    private void requiemunofficial$allowPermittedVagrantNewBlockCollisions(LevelReader level,
                                                                           AABB oldAabb,
                                                                           double x,
                                                                           double y,
                                                                           double z,
                                                                           CallbackInfoReturnable<Boolean> cir) {
        if (!cir.getReturnValue() || !VagrantRules.isRestricted(this.player)) {
            return;
        }

        AABB newAabb = this.player.getBoundingBox().move(
                x - this.player.getX(),
                y - this.player.getY(),
                z - this.player.getZ()
        );
        if (requiemunofficial$hasOnlyPermittedNewBlockCollisions(level, oldAabb, newAabb)) {
            cir.setReturnValue(false);
        }
    }

    @Unique
    private boolean requiemunofficial$hasOnlyPermittedNewBlockCollisions(LevelReader level, AABB oldAabb, AABB newAabb) {
        VoxelShape oldShape = Shapes.create(oldAabb.deflate(REQUIEMUNOFFICIAL_COLLISION_EPSILON));
        VoxelShape newShape = Shapes.create(newAabb.deflate(REQUIEMUNOFFICIAL_COLLISION_EPSILON));
        int vanillaNewCollisionCount = requiemunofficial$countVanillaNewCollisions(level, oldShape, newAabb);
        if (vanillaNewCollisionCount <= 0) {
            return false;
        }

        int permittedBlockCollisionCount = 0;

        int minX = Mth.floor(newAabb.minX);
        int minY = Mth.floor(newAabb.minY);
        int minZ = Mth.floor(newAabb.minZ);
        int maxX = Mth.floor(newAabb.maxX);
        int maxY = Mth.floor(newAabb.maxY);
        int maxZ = Mth.floor(newAabb.maxZ);

        for (BlockPos pos : BlockPos.betweenClosed(minX, minY, minZ, maxX, maxY, maxZ)) {
            BlockState state = level.getBlockState(pos);
            if (state.isAir()) {
                continue;
            }

            VoxelShape blockShape = state.getCollisionShape(level, pos).move(pos.getX(), pos.getY(), pos.getZ());
            if (blockShape.isEmpty()
                    || !Shapes.joinIsNotEmpty(blockShape, newShape, BooleanOp.AND)
                    || Shapes.joinIsNotEmpty(blockShape, oldShape, BooleanOp.AND)) {
                continue;
            }

            if (!VagrantRules.shouldSuppressCollisionWith(this.player, state)) {
                return false;
            }
            permittedBlockCollisionCount++;
        }

        return permittedBlockCollisionCount == vanillaNewCollisionCount;
    }

    @Unique
    private int requiemunofficial$countVanillaNewCollisions(LevelReader level, VoxelShape oldShape, AABB newAabb) {
        int collisions = 0;
        for (VoxelShape shape : level.getCollisions(
                this.player,
                newAabb.deflate(REQUIEMUNOFFICIAL_COLLISION_EPSILON)
        )) {
            if (!Shapes.joinIsNotEmpty(shape, oldShape, BooleanOp.AND)) {
                collisions++;
            }
        }
        return collisions;
    }
}
