package ua.meekeyn.requiemunofficial.client.particle;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.*;
import net.minecraft.core.particles.SimpleParticleType;
import org.jetbrains.annotations.NotNull;

public class SimpleGhostParticle extends TextureSheetParticle {

    private final SpriteSet spriteProvider;

    protected SimpleGhostParticle(ClientLevel level, double x, double y, double z,
                                  SpriteSet sprites, double xd, double yd, double zd) {
        super(level, x, y, z, xd, yd, zd);
        this.friction = 0.96F;
        this.gravity = 0.0F;
        this.quadSize *= 1.2F;
        this.lifetime = 24 + this.random.nextInt(12);
        this.spriteProvider = sprites;
        // Як і в beta16: частинка, народжена всередині блоку, не має фізичної взаємодії (проходить крізь стіни).
        this.hasPhysics = !this.isColliding();
        this.setSpriteFromAge(sprites);
    }

    /**
     * Чи зараз частинка перебуває у суцільному блоці.
     * У beta16: {@code !world.isSpaceEmpty(null, bb)}.
     */
    public boolean isColliding() {
        return !this.level.noCollision(null, this.getBoundingBox());
    }

    /**
     * Вибір спрайту залежно від віку.
     * Як і в beta16: частинка, що народилася в стіні ({@code !hasPhysics}) і вже звідти вийшла,
     * показує жовту фазу (gold_ghost_*), інакше — білу (ghost_*).
     */
    @Override
    public void setSpriteFromAge(@NotNull SpriteSet spriteProvider) {
        if (!this.hasPhysics && !this.isColliding()) {
            this.setSprite(spriteProvider.get(this.age / 2 + this.lifetime / 2, this.lifetime));
        } else {
            this.setSprite(spriteProvider.get(this.age / 2, this.lifetime));
        }
    }

    @Override
    public void tick() {
        super.tick();
        if (!this.removed) {
            this.setSpriteFromAge(this.spriteProvider);
        }
    }

    @Override
    public @NotNull ParticleRenderType getRenderType() {
        // Використовує напівпрозорий рендер для примарної частинки
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
            return new SimpleGhostParticle(level, x, y, z, sprites, xd, yd, zd);
        }
    }
}
