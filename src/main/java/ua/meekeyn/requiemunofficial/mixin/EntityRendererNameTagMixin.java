package ua.meekeyn.requiemunofficial.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import ua.meekeyn.requiemunofficial.client.ClientPossessionNameTags;

@Mixin(EntityRenderer.class)
public abstract class EntityRendererNameTagMixin<T extends Entity> {

    @Shadow
    protected abstract void renderNameTag(T entity,
                                          Component displayName,
                                          PoseStack poseStack,
                                          MultiBufferSource buffer,
                                          int packedLight);

    @Redirect(
            method = "render",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/renderer/entity/EntityRenderer;renderNameTag(Lnet/minecraft/world/entity/Entity;Lnet/minecraft/network/chat/Component;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V"
            )
    )
    private void requiemunofficial$renderPossessorName(EntityRenderer<T> renderer,
                                                       T entity,
                                                       Component name,
                                                       PoseStack poseStack,
                                                       MultiBufferSource buffer,
                                                       int packedLight) {
        this.renderNameTag(
                entity,
                ClientPossessionNameTags.possessorName(entity).orElse(name),
                poseStack,
                buffer,
                packedLight
        );
    }
}
