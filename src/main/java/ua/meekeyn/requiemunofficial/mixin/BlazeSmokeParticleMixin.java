package ua.meekeyn.requiemunofficial.mixin;

import net.minecraft.client.Minecraft;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Blaze;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import ua.meekeyn.requiemunofficial.common.possession.PossessionManager;

@Mixin(Blaze.class)
public abstract class BlazeSmokeParticleMixin {

    @Redirect(
            method = "aiStep",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/level/Level;addParticle(Lnet/minecraft/core/particles/ParticleOptions;DDDDDD)V"
            )
    )
    private void requiemunofficial$thinLocalPossessedBlazeSmoke(Level level,
                                                                ParticleOptions particle,
                                                                double x,
                                                                double y,
                                                                double z,
                                                                double xSpeed,
                                                                double ySpeed,
                                                                double zSpeed) {
        Blaze self = (Blaze) (Object) this;
        if (level.isClientSide()
                && (particle == ParticleTypes.LARGE_SMOKE || particle == ParticleTypes.SMOKE)
                && shouldThinLocalSmoke(self)) {
            return;
        }
        level.addParticle(particle, x, y, z, xSpeed, ySpeed, zSpeed);
    }

    private boolean shouldThinLocalSmoke(Blaze blaze) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null) {
            return false;
        }
        LivingEntity host = PossessionManager.getBeta16PlayerBodyHost(minecraft.player);
        return host == blaze && blaze.tickCount % 3 != 0;
    }
}
