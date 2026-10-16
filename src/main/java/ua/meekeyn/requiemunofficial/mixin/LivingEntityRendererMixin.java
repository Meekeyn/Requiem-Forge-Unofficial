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
import ua.meekeyn.requiemunofficial.common.soul.SoulManager;
import ua.meekeyn.requiemunofficial.common.soul.SoulState;

import java.util.Collections;
import java.util.Iterator;

@Mixin(LivingEntityRenderer.class)
public abstract class LivingEntityRendererMixin<T extends LivingEntity> {

    @Inject(method = "getRenderType", at = @At("HEAD"), cancellable = true)
    private void requiemunofficial$useTranslucentRenderType(T entity,
                                                            boolean bodyVisible,
                                                            boolean translucent,
                                                            boolean glowing,
                                                            CallbackInfoReturnable<RenderType> cir) {
        if (requiemunofficial$isVisibleVagrantPlayer(entity)) {
            cir.setReturnValue(RenderType.entityTranslucent(
                    ua.meekeyn.requiemunofficial.client.ClientVagrantTexture.location()
            ));
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
        if (requiemunofficial$isVisibleVagrantPlayer(entity)
                || requiemunofficial$isHiddenPossessedSoulPlayer(entity)) {
            return Collections.emptyIterator();
        }
        return iterator;
    }

    private static boolean requiemunofficial$isVisibleVagrantPlayer(LivingEntity entity) {
        return entity instanceof AbstractClientPlayer player
                && SoulManager.getState(player) == SoulState.VAGRANT
                && SoulManager.getPossessedEntityId(player) == null
                && !player.isSpectator();
    }

    private static boolean requiemunofficial$isHiddenPossessedSoulPlayer(LivingEntity entity) {
        return entity instanceof AbstractClientPlayer player
                && ClientPossessionRenderHelper.shouldHideLocalSoulPlayer(player);
    }
}
