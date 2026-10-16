package ua.meekeyn.requiemunofficial.mixin;

import net.minecraft.client.renderer.RenderType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/**
 * {@code sortOnUpload} нужен, чтобы обёртка {@code RenderType} для анимации
 * вселения повторяла поведение базового типа (например, у
 * {@code entityTranslucent} сортировка квадов по загрузке включена).
 */
@Mixin(RenderType.class)
public interface RenderTypeAccessor {

    @Accessor("sortOnUpload")
    boolean requiemunofficial$getSortOnUpload();
}
