package ua.meekeyn.requiemunofficial.common.particle;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.mojang.serialization.Codec;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

/**
 * Параметри частинки з двома сутностями (порт beta.16 {@code RequiemEntityParticleEffect}):
 * джерело (з чиєї текстури береться пил) і ціль (до кого летить частинка).
 */
public class RequiemEntityParticleOptions implements ParticleOptions {

    private final ParticleType<RequiemEntityParticleOptions> type;
    private final int sourceEntityId;
    private final int targetEntityId;

    public RequiemEntityParticleOptions(ParticleType<RequiemEntityParticleOptions> type,
                                        int sourceEntityId, int targetEntityId) {
        this.type = type;
        this.sourceEntityId = sourceEntityId;
        this.targetEntityId = targetEntityId;
    }

    public static MapCodec<RequiemEntityParticleOptions> codec(ParticleType<RequiemEntityParticleOptions> type) {
        return RecordCodecBuilder.mapCodec(instance -> instance.group(
                Codec.INT.fieldOf("srcId").forGetter(RequiemEntityParticleOptions::getSourceEntityId),
                Codec.INT.fieldOf("destId").forGetter(RequiemEntityParticleOptions::getTargetEntityId)
        ).apply(instance, (sourceEntityId, targetEntityId) ->
                new RequiemEntityParticleOptions(type, sourceEntityId, targetEntityId)));
    }

    public static StreamCodec<RegistryFriendlyByteBuf, RequiemEntityParticleOptions> streamCodec(
            ParticleType<RequiemEntityParticleOptions> type) {
        return StreamCodec.composite(
                ByteBufCodecs.VAR_INT, RequiemEntityParticleOptions::getSourceEntityId,
                ByteBufCodecs.VAR_INT, RequiemEntityParticleOptions::getTargetEntityId,
                (sourceEntityId, targetEntityId) -> new RequiemEntityParticleOptions(type, sourceEntityId, targetEntityId)
        );
    }

    @Override
    public ParticleType<RequiemEntityParticleOptions> getType() {
        return type;
    }

    public int getSourceEntityId() {
        return sourceEntityId;
    }

    public int getTargetEntityId() {
        return targetEntityId;
    }
}
