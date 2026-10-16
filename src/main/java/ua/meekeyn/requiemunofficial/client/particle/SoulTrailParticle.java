package ua.meekeyn.requiemunofficial.client.particle;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.*;
import net.minecraft.core.particles.SimpleParticleType;
import org.jetbrains.annotations.NotNull;

public class SoulTrailParticle extends TextureSheetParticle {

    protected SoulTrailParticle(ClientLevel level, double x, double y, double z,
                                SpriteSet sprites, double xd, double yd, double zd) {
        super(level, x, y, z, xd, yd, zd);
        this.friction = 0.94F;
        this.gravity = 0.0F;
        this.quadSize *= 0.55F;
        this.lifetime = 14 + this.random.nextInt(8);
        this.setSpriteFromAge(sprites);
    }

    @Override
    public void tick() {
        super.tick();
    }

    @Override
    public @NotNull ParticleRenderType getRenderType() {
        // Використовує стандартний напівпрозорий тип рендеру
        return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT;
    }

    public static class Provider implements ParticleProvider<SimpleParticleType> {
        private final SpriteSet sprites;

        public Provider(SpriteSet sprites) {
            this.sprites = sprites;
        }

        @Override
        public Particle createParticle(@NotNull SimpleParticleType type, @NotNull ClientLevel level,
                                       double x, double y, double z,
                                       double xd, double yd, double zd) {
            return new SoulTrailParticle(level, x, y, z, sprites, xd, yd, zd);
        }
    }
}