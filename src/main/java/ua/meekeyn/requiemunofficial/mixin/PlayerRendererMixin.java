package ua.meekeyn.requiemunofficial.mixin;

import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import ua.meekeyn.requiemunofficial.client.ClientVagrantTexture;
import ua.meekeyn.requiemunofficial.client.ClientPossessionRenderHelper;
import ua.meekeyn.requiemunofficial.common.soul.SoulManager;
import ua.meekeyn.requiemunofficial.common.soul.SoulState;

@Mixin(PlayerRenderer.class)
public abstract class PlayerRendererMixin extends LivingEntityRenderer<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> {

    public PlayerRendererMixin(EntityRendererProvider.Context context,
                               PlayerModel<AbstractClientPlayer> model,
                               float shadowRadius) {
        super(context, model, shadowRadius);
    }

    @Inject(method = "setModelProperties", at = @At("TAIL"))
    private void requiemunofficial$singleLayerVagrant(AbstractClientPlayer player, CallbackInfo ci) {
        if (requiemunofficial$isVisibleVagrant(player)) {
            PlayerModel<AbstractClientPlayer> model = getModel();
            model.hat.visible = false;
            model.jacket.visible = false;
            model.leftSleeve.visible = false;
            model.rightSleeve.visible = false;
            model.leftPants.visible = false;
            model.rightPants.visible = false;
        }
    }

    @Inject(method = "render", at = @At("HEAD"), cancellable = true)
    private void requiemunofficial$hideLocalSoulPlayer(AbstractClientPlayer player,
                                                       float entityYaw,
                                                       float partialTicks,
                                                       PoseStack poseStack,
                                                       MultiBufferSource buffer,
                                                       int packedLight,
                                                       CallbackInfo ci) {
        if (!ClientPossessionRenderHelper.shouldHideLocalSoulPlayer(player)) {
            return;
        }
        ci.cancel();
    }

    @Inject(method = "renderRightHand", at = @At("HEAD"), cancellable = true)
    private void requiemunofficial$hidePossessedSoulRightHand(PoseStack poseStack,
                                                              MultiBufferSource buffer,
                                                              int packedLight,
                                                              AbstractClientPlayer player,
                                                              CallbackInfo ci) {
        if (requiemunofficial$renderPossessedBodyArm(poseStack, buffer, packedLight, player, HumanoidArm.RIGHT)) {
            ci.cancel();
            return;
        }
        if (requiemunofficial$renderVagrantArm(poseStack, buffer, packedLight, player, HumanoidArm.RIGHT)) {
            ci.cancel();
            return;
        }
        if (requiemunofficial$shouldHideSoulHands(player)) {
            ci.cancel();
        }
    }

    @Inject(method = "renderLeftHand", at = @At("HEAD"), cancellable = true)
    private void requiemunofficial$hidePossessedSoulLeftHand(PoseStack poseStack,
                                                             MultiBufferSource buffer,
                                                             int packedLight,
                                                             AbstractClientPlayer player,
                                                             CallbackInfo ci) {
        if (requiemunofficial$renderPossessedBodyArm(poseStack, buffer, packedLight, player, HumanoidArm.LEFT)) {
            ci.cancel();
            return;
        }
        if (requiemunofficial$renderVagrantArm(poseStack, buffer, packedLight, player, HumanoidArm.LEFT)) {
            ci.cancel();
            return;
        }
        if (requiemunofficial$shouldHideSoulHands(player)) {
            ci.cancel();
        }
    }

    private static boolean requiemunofficial$renderPossessedBodyArm(PoseStack poseStack,
                                                                    MultiBufferSource buffer,
                                                                    int packedLight,
                                                                    AbstractClientPlayer player,
                                                                    HumanoidArm armSide) {
        if (!ClientPossessionRenderHelper.shouldHideLocalSoulPlayer(player)) {
            return false;
        }

        LivingEntity host = ClientPossessionRenderHelper.getPossessedHost(player);
        if (host == null) {
            return true;
        }

        EntityRenderer<? super LivingEntity> renderer = Minecraft.getInstance()
                .getEntityRenderDispatcher()
                .getRenderer(host);
        if (!(renderer instanceof LivingEntityRenderer<?, ?> livingRenderer)
                || !(livingRenderer.getModel() instanceof HumanoidModel<?> humanoidModel)) {
            return true;
        }

        @SuppressWarnings("unchecked")
        HumanoidModel<LivingEntity> hostModel = (HumanoidModel<LivingEntity>) humanoidModel;
        hostModel.attackTime = 0.0F;
        hostModel.crouching = false;
        hostModel.swimAmount = 0.0F;
        hostModel.setupAnim(host, 0.0F, 0.0F, player.tickCount, 0.0F, 0.0F);

        ModelPart arm = armSide == HumanoidArm.RIGHT ? hostModel.rightArm : hostModel.leftArm;
        arm.xRot = 0.0F;
        VertexConsumer consumer = buffer.getBuffer(hostModel.renderType(renderer.getTextureLocation(host)));
        arm.render(poseStack, consumer, Math.max(packedLight, LightTexture.FULL_BRIGHT), OverlayTexture.NO_OVERLAY);
        return true;
    }

    private boolean requiemunofficial$renderVagrantArm(PoseStack poseStack,
                                                       MultiBufferSource buffer,
                                                       int packedLight,
                                                       AbstractClientPlayer player,
                                                       HumanoidArm armSide) {
        if (!requiemunofficial$isVisibleVagrant(player)) {
            return false;
        }

        PlayerModel<AbstractClientPlayer> playerModel = this.getModel();
        playerModel.attackTime = 0.0F;
        playerModel.crouching = false;
        playerModel.swimAmount = 0.0F;
        playerModel.setupAnim(player, 0.0F, 0.0F, player.tickCount, 0.0F, 0.0F);

        ModelPart arm = armSide == HumanoidArm.RIGHT ? playerModel.rightArm : playerModel.leftArm;
        arm.xRot = 0.0F;
        VertexConsumer consumer = buffer.getBuffer(RenderType.entityTranslucent(ClientVagrantTexture.location()));
        arm.render(
                poseStack,
                consumer,
                packedLight,
                OverlayTexture.NO_OVERLAY,
                ClientVagrantTexture.RED,
                ClientVagrantTexture.GREEN,
                ClientVagrantTexture.BLUE,
                ClientVagrantTexture.ALPHA
        );
        return true;
    }

    private static boolean requiemunofficial$shouldHideSoulHands(AbstractClientPlayer player) {
        return ClientPossessionRenderHelper.shouldHideLocalSoulPlayer(player)
                && ClientPossessionRenderHelper.getPossessedHost(player) != null;
    }

    private static boolean requiemunofficial$isVisibleVagrant(AbstractClientPlayer player) {
        return SoulManager.getState(player) == SoulState.VAGRANT
                && SoulManager.getPossessedEntityId(player) == null
                && !player.isSpectator();
    }
}
