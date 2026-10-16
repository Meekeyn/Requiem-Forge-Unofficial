package ua.meekeyn.requiemunofficial.mixin;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import ua.meekeyn.requiemunofficial.client.ClientPossessionFx;
import ua.meekeyn.requiemunofficial.common.possession.PossessionManager;

import java.util.function.Predicate;

@Mixin(GameRenderer.class)
public abstract class GameRendererMixin {

    /**
     * Проигрывает анимацию входа в сущность. Точка вызова повторяет бету-16:
     * Satin диспетчеризовал {@code ShaderEffectRenderCallback} сразу после
     * {@code LevelRenderer.doEntityOutline()}, до ванильного
     * {@code postEffect.process}.
     */
    @Inject(
            method = "render(FJZ)V",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/renderer/LevelRenderer;doEntityOutline()V",
                    shift = At.Shift.AFTER
            )
    )
    private void requiemunofficial$renderPossessionZoom(float partialTick, long nanoTime, boolean renderLevel, CallbackInfo ci) {
        ClientPossessionFx.renderShaderEffects(partialTick);
    }

    @ModifyArg(
            method = "pick",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/entity/projectile/ProjectileUtil;getEntityHitResult(Lnet/minecraft/world/entity/Entity;Lnet/minecraft/world/phys/Vec3;Lnet/minecraft/world/phys/Vec3;Lnet/minecraft/world/phys/AABB;Ljava/util/function/Predicate;D)Lnet/minecraft/world/phys/EntityHitResult;"
            ),
            index = 4
    )
    private Predicate<Entity> requiemunofficial$ignoreOwnBeta16Host(Predicate<Entity> original) {
        Minecraft minecraft = Minecraft.getInstance();
        return entity -> original.test(entity)
                && (minecraft.player == null || !PossessionManager.isBeta16HostForPlayer(minecraft.player, entity));
    }
}
