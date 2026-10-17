package ua.meekeyn.requiemunofficial.mixin;

import net.minecraft.world.entity.WalkAnimationState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/**
 * Стан ходи (WalkAnimationState) не має сетерів для position/speedOld, а possessed-тіло
 * мусить отримати ту саму ходу, що й фізичний гравець. Тому відкриваємо поля напряму.
 */
@Mixin(WalkAnimationState.class)
public interface WalkAnimationStateAccessor {

    @Accessor("speedOld")
    float requiemunofficial$getSpeedOld();

    @Accessor("speedOld")
    void requiemunofficial$setSpeedOld(float value);

    @Accessor("speed")
    float requiemunofficial$getWalkSpeed();

    @Accessor("speed")
    void requiemunofficial$setWalkSpeed(float value);

    @Accessor("position")
    float requiemunofficial$getWalkPosition();

    @Accessor("position")
    void requiemunofficial$setWalkPosition(float value);
}
