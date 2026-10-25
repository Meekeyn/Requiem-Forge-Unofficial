package ua.meekeyn.requiemunofficial.mixin;

import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import ua.meekeyn.requiemunofficial.client.ClientPossessionFx;
import ua.meekeyn.requiemunofficial.client.ClientPossessionRenderHelper;
import ua.meekeyn.requiemunofficial.client.ClientShadowPlayerFx;
import ua.meekeyn.requiemunofficial.common.config.RequiemClientConfig;

import java.util.Collections;
import java.util.Iterator;

@Mixin(LivingEntityRenderer.class)
public abstract class LivingEntityRendererMixin<T extends LivingEntity> {

    /**
     * Тело бессмертной души рисуется не в главную цель, а в цель
     * {@code players} пост-эффекта теневого игрока (бета-16: метод
     * {@code PlayerRendererLayerMixin#requiem$replaceRenderLayer} возвращал
     * {@code ShadowPlayerFx#getRenderLayer}).
     */
    @Inject(method = "getRenderType", at = @At("RETURN"), cancellable = true)
    private void requiemunofficial$useShadowRenderType(T entity,
                                                       boolean bodyVisible,
                                                       boolean translucent,
                                                       boolean glowing,
                                                       CallbackInfoReturnable<RenderType> cir) {
        if (entity instanceof AbstractClientPlayer player && ClientShadowPlayerFx.isShadowPlayer(player)) {
            // beta.16 graphics.fancyDemonRender: вимкнено -> простий end portal замість шейдерного ореолу.
            if (!RequiemClientConfig.FANCY_DEMON_RENDER.get()) {
                cir.setReturnValue(RenderType.endPortal());
                return;
            }
            cir.setReturnValue(ClientShadowPlayerFx.getShadowRenderType(cir.getReturnValue()));
        }
    }

    /**
     * Пока играет анимация входа, модель цели рисуется не в главную цель, а в
     * {@code zoom_focus} пост-эффекта зума (бета-16: {@code RequiemFx#getZoomFx}).
     */
    @ModifyVariable(
            method = "render(Lnet/minecraft/world/entity/LivingEntity;FFLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V",
            at = @At(
                    value = "INVOKE_ASSIGN",
                    target = "Lnet/minecraft/client/renderer/entity/LivingEntityRenderer;getRenderType(Lnet/minecraft/world/entity/LivingEntity;ZZZ)Lnet/minecraft/client/renderer/RenderType;"
            )
    )
    private RenderType requiemunofficial$zoomPossessedRenderType(RenderType base, T entity) {
        if (base == null || entity == null || entity != ClientPossessionFx.getAnimationEntity()) {
            return base;
        }
        return ClientPossessionFx.getZoomFx(base);
    }

    @ModifyVariable(
            method = "render(Lnet/minecraft/world/entity/LivingEntity;FFLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V",
            at = @At(
                    value = "INVOKE_ASSIGN",
                    target = "Ljava/util/List;iterator()Ljava/util/Iterator;"
            )
    )
    private Iterator<RenderLayer<T, ?>> requiemunofficial$hideVagrantFeatureLayers(Iterator<RenderLayer<T, ?>> iterator,
                                                                                   T entity) {
        if (entity instanceof AbstractClientPlayer player
                && (ClientShadowPlayerFx.isShadowPlayer(player)
                || ClientPossessionRenderHelper.shouldHideLocalSoulPlayer(player))) {
            return Collections.emptyIterator();
        }
        return iterator;
    }
}
