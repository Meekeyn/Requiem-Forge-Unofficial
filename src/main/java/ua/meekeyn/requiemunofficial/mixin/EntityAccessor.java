package ua.meekeyn.requiemunofficial.mixin;

import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(Entity.class)
public interface EntityAccessor {

    @Accessor("maxUpStep")
    float requiemunofficial$getMaxUpStep();

    @Accessor("maxUpStep")
    void requiemunofficial$setMaxUpStep(float maxUpStep);
}
