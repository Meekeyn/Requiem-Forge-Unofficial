package ua.meekeyn.requiemunofficial.common.block.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import ua.meekeyn.requiemunofficial.common.init.ModBlockEntities;

public final class DummyBlockEntity extends BlockEntity {

    public DummyBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.DUMMY.get(), pos, state);
    }
}