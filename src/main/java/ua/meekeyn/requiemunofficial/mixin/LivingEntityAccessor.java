package ua.meekeyn.requiemunofficial.mixin;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(LivingEntity.class)
public interface LivingEntityAccessor {
    @Accessor("jumping")
    boolean requiemunofficial$isJumping();

    @Invoker("jumpFromGround")
    void requiemunofficial$jumpFromGround();

    @Accessor("useItem")
    void requiemunofficial$setUseItem(ItemStack stack);

    @Accessor("useItemRemaining")
    void requiemunofficial$setUseItemRemaining(int ticks);

    @Invoker("setLivingEntityFlag")
    void requiemunofficial$setLivingEntityFlag(int flag, boolean value);
}
