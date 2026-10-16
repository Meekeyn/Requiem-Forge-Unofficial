package ua.meekeyn.requiemunofficial.mixin;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.DeathScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.protocol.game.ServerboundClientCommandPacket;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import ua.meekeyn.requiemunofficial.client.ClientVagrantSenseTargets;
import ua.meekeyn.requiemunofficial.common.possession.PossessionManager;
import ua.meekeyn.requiemunofficial.common.soul.SoulManager;
import ua.meekeyn.requiemunofficial.common.soul.SoulState;

@Mixin(Minecraft.class)
public abstract class MinecraftMixin {

    @Inject(method = "tick", at = @At("RETURN"))
    private void requiemunofficial$copyBeta16HostHurtCameraState(CallbackInfo ci) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null) {
            return;
        }

        LivingEntity host = PossessionManager.getBeta16PlayerBodyHost(minecraft.player);
        if (host == null || host.hurtTime <= 0) {
            return;
        }

        minecraft.player.hurtTime = host.hurtTime;
        minecraft.player.hurtDuration = host.hurtDuration;
    }

    @Inject(method = "setScreen", at = @At("HEAD"), cancellable = true)
    private void requiemunofficial$skipDemonDeathScreen(Screen screen, CallbackInfo ci) {
        Minecraft minecraft = Minecraft.getInstance();
        if (!(screen instanceof DeathScreen)
                || minecraft.player == null
                || minecraft.player.connection == null
                || !shouldSkipDeathScreen(SoulManager.getState(minecraft.player))) {
            return;
        }

        minecraft.player.connection.send(new ServerboundClientCommandPacket(
                ServerboundClientCommandPacket.Action.PERFORM_RESPAWN
        ));
        ci.cancel();
    }

    /**
     * White outline for soulless bodies while the vagrant is passing through walls.
     *
     * <p>Local client side decision: vanilla renders {@code shouldEntityAppearGlowing} entities
     * into the entity outline framebuffer with the entity's team colour (white by default), which
     * is exactly the spectral arrow glow. Nothing is written to the entity, so real glowing
     * effects are left untouched and the server cannot overwrite this state.
     */
    @Inject(method = "shouldEntityAppearGlowing", at = @At("RETURN"), cancellable = true)
    private void requiemunofficial$senseSoullessOutline(Entity entity, CallbackInfoReturnable<Boolean> cir) {
        if (Boolean.TRUE.equals(cir.getReturnValue())) {
            return;
        }
        if (ClientVagrantSenseTargets.shouldOutline(entity)) {
            cir.setReturnValue(true);
        }
    }

    private static boolean shouldSkipDeathScreen(SoulState state) {
        return state == SoulState.REMNANT || state == SoulState.VAGRANT;
    }
}
