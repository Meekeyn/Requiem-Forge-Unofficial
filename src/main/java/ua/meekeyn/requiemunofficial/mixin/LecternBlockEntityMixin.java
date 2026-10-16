package ua.meekeyn.requiemunofficial.mixin;

import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.entity.LecternBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import ua.meekeyn.requiemunofficial.common.item.OpusLecternRules;

@Mixin(LecternBlockEntity.class)
public abstract class LecternBlockEntityMixin extends BlockEntity {

    @Shadow
    private ItemStack book;

    protected LecternBlockEntityMixin(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    @Inject(method = "hasBook", at = @At("RETURN"), cancellable = true)
    private void requiemunofficial$requiemBooksCountAsLecternBooks(CallbackInfoReturnable<Boolean> cir) {
        if (OpusLecternRules.isLecternReadable(this.book)) {
            cir.setReturnValue(true);
        }
    }
}
