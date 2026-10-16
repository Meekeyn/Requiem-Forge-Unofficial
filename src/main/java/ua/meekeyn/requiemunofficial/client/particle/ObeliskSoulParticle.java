package ua.meekeyn.requiemunofficial.client.particle;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.*;
import net.minecraft.core.particles.SimpleParticleType;
import org.jetbrains.annotations.NotNull;

public class ObeliskSoulParticle extends TextureSheetParticle {

    protected ObeliskSoulParticle(ClientLevel level, double x, double y, double z,
                                  SpriteSet sprites, double xd, double yd, double zd) {
        super(level, x, y, z, xd, yd, zd);
        this.friction = 0.92F;
        this.gravity = 0.0F;
        this.quadSize *= 0.9F;
        this.lifetime = 20 + this.random.nextInt(10);
        this.setSpriteFromAge(sprites);
    }

    @Override
    public void tick() {
        super.tick();
    }

    @Override
    public @NotNull ParticleRenderType getRenderType() {
        // Рендерить частинку душі обеліска через напівпрозорий атлас
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
            return new ObeliskSoulParticle(level, x, y, z, sprites, xd, yd, zd);
        }
    }
}