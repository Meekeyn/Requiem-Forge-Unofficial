package ua.meekeyn.requiemunofficial.mixin;

import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import ua.meekeyn.requiemunofficial.client.ClientPossessionNameTags;

@Mixin(LivingEntityRenderer.class)
public abstract class LivingEntityRendererNameTagMixin<T extends LivingEntity> {

    @Inject(method = "shouldShowName(Lnet/minecraft/world/entity/LivingEntity;)Z", at = @At("HEAD"), cancellable = true)
    private void requiemunofficial$forcePossessorNameTag(T entity,
                                                         CallbackInfoReturnable<Boolean> cir) {
        if (ClientPossessionNameTags.shouldForceNameTag(entity)) {
            cir.setReturnValue(true);
        }
    }
}
