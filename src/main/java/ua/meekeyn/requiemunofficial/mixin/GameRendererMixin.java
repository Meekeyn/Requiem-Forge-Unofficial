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
import ua.meekeyn.requiemunofficial.client.ClientShadowPlayerFx;
import ua.meekeyn.requiemunofficial.common.possession.PossessionManager;

import java.util.function.Predicate;

@Mixin(GameRenderer.class)
public abstract class GameRendererMixin {

    /**
     * Начало кадра: сбрасывает флаг отрисовки в цель {@code players}, чтобы
     * первая же душа кадра очистила её (бета-16: {@code ShadowPlayerFx}
     * делал это в {@code EntitiesPreRenderCallback}).
     */
    @Inject(method = "render(Lnet/minecraft/client/DeltaTracker;Z)V", at = @At("HEAD"))
    private void requiemunofficial$beginShadowPlayerFrame(net.minecraft.client.DeltaTracker deltaTracker, boolean renderLevel, CallbackInfo ci) {
        ClientShadowPlayerFx.beginFrame();
    }

    /**
     * Проигрывает пост-эффекты. Точка вызова повторяет бету-16: Satin
     * диспетчеризовал {@code ShaderEffectRenderCallback} сразу после
     * {@code LevelRenderer.doEntityOutline()}, до ванильного
     * {@code postEffect.process}. К этому моменту и тело, и рука уже сброшены
     * в цель {@code players} теневого игрока.
     */
    @Inject(
            method = "render(Lnet/minecraft/client/DeltaTracker;Z)V",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/renderer/LevelRenderer;doEntityOutline()V",
                    shift = At.Shift.AFTER
            )
    )
    private void requiemunofficial$renderPostEffects(net.minecraft.client.DeltaTracker deltaTracker, boolean renderLevel, CallbackInfo ci) {
        float partialTick = deltaTracker.getGameTimeDeltaPartialTick(false);
        ClientShadowPlayerFx.renderShaderEffects(partialTick);
        ClientPossessionFx.renderShaderEffects(partialTick);
    }

    @ModifyArg(
            method = "pick(Lnet/minecraft/world/entity/Entity;DDF)Lnet/minecraft/world/phys/HitResult;",
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
