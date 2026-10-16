package ua.meekeyn.requiemunofficial.mixin;

import net.minecraft.client.Minecraft;
import net.minecraft.client.model.ShulkerModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.world.entity.monster.Shulker;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import ua.meekeyn.requiemunofficial.client.ClientPossessionRenderHelper;

@Mixin(ShulkerModel.class)
public abstract class ShulkerModelMixin {
    @Shadow
    @Final
    private ModelPart head;

    @Unique
    private boolean requiemunofficial$disabledHead;

    @Inject(method = "setupAnim(Lnet/minecraft/world/entity/monster/Shulker;FFFFF)V", at = @At("RETURN"))
    private void requiemunofficial$hidePossessedFirstPersonHead(Shulker shulker,
                                                                float limbSwing,
                                                                float limbSwingAmount,
                                                                float ageInTicks,
                                                                float netHeadYaw,
                                                                float headPitch,
                                                                CallbackInfo ci) {
        Minecraft minecraft = Minecraft.getInstance();
        boolean shouldHideHead = minecraft.options.getCameraType().isFirstPerson()
                && ClientPossessionRenderHelper.isLocalPlayersPossessedHost(shulker);
        if (shouldHideHead) {
            this.head.visible = false;
            this.requiemunofficial$disabledHead = true;
        } else if (this.requiemunofficial$disabledHead) {
            this.head.visible = true;
            this.requiemunofficial$disabledHead = false;
        }
    }
}
