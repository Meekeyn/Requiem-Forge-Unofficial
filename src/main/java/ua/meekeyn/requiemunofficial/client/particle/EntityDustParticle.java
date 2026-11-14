package ua.meekeyn.requiemunofficial.client.particle;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SingleQuadParticle;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import ua.meekeyn.requiemunofficial.common.particle.RequiemEntityParticleOptions;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

/**
 * Порт beta.16 {@code EntityDustParticle}: шматочок текстури сутності-джерела,
 * що прокладає шлях повз повні блоки до очей цілі (того, хто витягує душу).
 */
public class EntityDustParticle extends SingleQuadParticle {
    private final ParticleRenderType sheet;
    private final Entity target;
    private final Set<BlockPos> exploredBlocks = new LinkedHashSet<>();
    private final float sampleU;
    private final float sampleV;
    private @Nullable Vec3 nextStep;

    public EntityDustParticle(ClientLevel level, double x, double y, double z,
                              double xd, double yd, double zd, ResourceLocation texture, Entity target) {
        super(level, x, y, z, xd, yd, zd);
        this.sheet = TextureSheet.get(texture);
        this.target = target;
        this.gravity = 1.0F;
        this.rCol = 0.6F;
        this.gCol = 0.6F;
        this.bCol = 0.6F;
        this.alpha = this.random.nextFloat() * 0.3F + 0.3F;

        this.quadSize /= 2.0F;
        this.sampleU = this.random.nextFloat() * 31.0F;
        this.sampleV = this.random.nextFloat() * 31.0F;
    }

    // Екзотичні рендерери (моди) можуть кидати виняток або повертати null - тоді частинку не створюємо.
    private static <E extends Entity> @Nullable ResourceLocation getTexture(E src) {
        try {
            EntityRenderer<? super E> renderer = Minecraft.getInstance().getEntityRenderDispatcher().getRenderer(src);
            return renderer == null ? null : renderer.getTextureLocation(src);
        } catch (RuntimeException | LinkageError e) {
            return null;
        }
    }

    @Override
    public void tick() {
        if (this.nextStep == null || this.reachedNextStep(this.nextStep)) {
            Vec3 targetPos = this.getTargetPos();
            double distanceToTarget = targetPos.distanceToSqr(this.x, this.y, this.z);
            if (distanceToTarget < 0.1D) {
                this.remove();
                return;
            }
            this.nextStep = this.findNextStep(targetPos, distanceToTarget);
        }
        this.moveTowardsGoal(this.nextStep);
        super.tick();
    }

    private boolean reachedNextStep(Vec3 nextStep) {
        if (nextStep.distanceToSqr(this.x, this.y, this.z) < 0.1D) {
            this.exploredBlocks.add(BlockPos.containing(nextStep));
            return true;
        }
        return false;
    }

    private void moveTowardsGoal(Vec3 nextStep) {
        Vec3 desiredDir = nextStep.subtract(this.x, this.y, this.z).normalize();
        this.setParticleSpeed(
                Mth.lerp(0.4D, this.xd, desiredDir.x * 0.7D),
                Mth.lerp(0.4D, this.yd, desiredDir.y * 0.7D),
                Mth.lerp(0.4D, this.zd, desiredDir.z * 0.7D)
        );
    }

    private Vec3 getTargetPos() {
        return this.target.getEyePosition().subtract(0.0D, 0.2D, 0.0D);
    }

    private Vec3 findNextStep(Vec3 targetPos, double distanceToTarget) {
        if (distanceToTarget < 1.0D) {
            return targetPos;
        }
        BlockPos nextPos = findNextPos();
        // Немає вільного непройденого сусіднього блоку: летимо прямо до цілі, а не до (0, 0, 0).
        return nextPos == null ? targetPos : swizzle(nextPos);
    }

    @NotNull
    private Vec3 swizzle(BlockPos nextPos) {
        return new Vec3(
                nextPos.getX() + this.level.random.nextFloat() - 0.5D,
                nextPos.getY() + this.level.random.nextFloat() - 0.5D,
                nextPos.getZ() + this.level.random.nextFloat() - 0.5D
        );
    }

    private @Nullable BlockPos findNextPos() {
        Vec3 eyePos = this.target.getEyePosition();
        BlockPos best = null;
        double bestDistance = Integer.MAX_VALUE;
        for (BlockPos candidate : BlockPos.betweenClosed(
                Mth.floor(this.x) - 1,
                Mth.floor(this.y) - 1,
                Mth.floor(this.z) - 1,
                Mth.floor(this.x) + 1,
                Mth.floor(this.y) + 1,
                Mth.floor(this.z) + 1
        )) {
            if (!this.exploredBlocks.contains(candidate)
                    && !this.level.getBlockState(candidate).isCollisionShapeFullBlock(this.level, candidate)) {
                double candidateDistance = candidate.distToLowCornerSqr(eyePos.x, eyePos.y, eyePos.z);
                if (candidateDistance < bestDistance) {
                    bestDistance = candidateDistance;
                    best = candidate.immutable();
                }
            }
        }
        return best;
    }

    @Override
    public @NotNull ParticleRenderType getRenderType() {
        return this.sheet;
    }

    @Override
    protected float getU0() {
        return (this.sampleU + 1.0F) * 4.0F / 128.0F;
    }

    @Override
    protected float getU1() {
        return this.sampleU * 4.0F / 128.0F;
    }

    @Override
    protected float getV0() {
        return (this.sampleV + 1.0F) * 4.0F / 128.0F;
    }

    @Override
    protected float getV1() {
        return this.sampleV * 4.0F / 128.0F;
    }

    public static class Provider implements ParticleProvider<RequiemEntityParticleOptions> {
        @Override
        public Particle createParticle(@NotNull RequiemEntityParticleOptions options, @NotNull ClientLevel level,
                                       double x, double y, double z, double xd, double yd, double zd) {
            Entity src = level.getEntity(options.getSourceEntityId());
            Entity target = level.getEntity(options.getTargetEntityId());
            if (src == null || target == null) {
                return null;
            }
            ResourceLocation texture = getTexture(src);
            return texture == null
                    ? null
                    : new EntityDustParticle(level, x, y, z, xd, yd, zd, texture, target);
        }
    }

    /**
     * Окремий тип рендеру на кожну текстуру сутності. Forge 1.20.1 сортує невідомі
     * типи за identityHashCode, тож кеш гарантує один екземпляр на текстуру
     * (у beta.16 для цього був ParticleManagerMixin).
     */
    public record TextureSheet(ResourceLocation texture) implements ParticleRenderType {
        private static final Map<ResourceLocation, ParticleRenderType> CACHE = new HashMap<>();

        static ParticleRenderType get(ResourceLocation texture) {
            return CACHE.computeIfAbsent(texture, TextureSheet::new);
        }

        @Override
        public BufferBuilder begin(Tesselator tesselator, TextureManager textureManager) {
            RenderSystem.depthMask(true);
            RenderSystem.setShader(GameRenderer::getParticleShader);
            RenderSystem.setShaderTexture(0, texture);
            RenderSystem.enableBlend();
            RenderSystem.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA);
            return tesselator.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.PARTICLE);
        }

        @Override
        public String toString() {
            return "EntityDustParticleTextureSheet[%s]".formatted(texture);
        }
    }
}
