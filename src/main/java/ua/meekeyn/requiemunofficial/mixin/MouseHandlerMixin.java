package ua.meekeyn.requiemunofficial.mixin;

import net.minecraft.client.MouseHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import ua.meekeyn.requiemunofficial.client.ClientPossessionInput;

@Mixin(MouseHandler.class)
public abstract class MouseHandlerMixin {

    @Inject(method = "turnPlayer", at = @At("RETURN"))
    private void requiemunofficial$syncPossessedCameraAfterMouse(CallbackInfo ci) {
        ClientPossessionInput.syncAfterMouseTurn(net.minecraft.client.Minecraft.getInstance());
    }
}
