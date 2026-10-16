package ua.meekeyn.requiemunofficial.mixin;

import net.minecraft.world.entity.monster.Blaze;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(Blaze.class)
public interface BlazeAccessor {
    @Invoker("isCharged")
    boolean requiemunofficial$isCharged();

    @Invoker("setCharged")
    void requiemunofficial$setCharged(boolean charged);
}
