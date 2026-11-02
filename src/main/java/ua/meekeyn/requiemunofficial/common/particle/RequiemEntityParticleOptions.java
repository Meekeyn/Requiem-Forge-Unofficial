package ua.meekeyn.requiemunofficial.common.particle;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.registries.ForgeRegistries;

/**
 * Параметри частинки з двома сутностями (порт beta.16 {@code RequiemEntityParticleEffect}):
 * джерело (з чиєї текстури береться пил) і ціль (до кого летить частинка).
 */
public class RequiemEntityParticleOptions implements ParticleOptions {

    @SuppressWarnings("deprecation")
    public static final ParticleOptions.Deserializer<RequiemEntityParticleOptions> DESERIALIZER =
            new ParticleOptions.Deserializer<>() {
                @Override
                public RequiemEntityParticleOptions fromCommand(ParticleType<RequiemEntityParticleOptions> type,
                                                                StringReader reader) throws CommandSyntaxException {
                    reader.expect(' ');
                    int sourceEntityId = reader.readInt();
                    reader.expect(' ');
                    int targetEntityId = reader.readInt();
                    return new RequiemEntityParticleOptions(type, sourceEntityId, targetEntityId);
                }

                @Override
                public RequiemEntityParticleOptions fromNetwork(ParticleType<RequiemEntityParticleOptions> type,
                                                                FriendlyByteBuf buf) {
                    return new RequiemEntityParticleOptions(type, buf.readVarInt(), buf.readVarInt());
                }
            };

    private final ParticleType<RequiemEntityParticleOptions> type;
    private final int sourceEntityId;
    private final int targetEntityId;

    public RequiemEntityParticleOptions(ParticleType<RequiemEntityParticleOptions> type,
                                        int sourceEntityId, int targetEntityId) {
        this.type = type;
        this.sourceEntityId = sourceEntityId;
        this.targetEntityId = targetEntityId;
    }

    public static Codec<RequiemEntityParticleOptions> codec(ParticleType<RequiemEntityParticleOptions> type) {
        return RecordCodecBuilder.create(instance -> instance.group(
                Codec.INT.fieldOf("srcId").forGetter(RequiemEntityParticleOptions::getSourceEntityId),
                Codec.INT.fieldOf("destId").forGetter(RequiemEntityParticleOptions::getTargetEntityId)
        ).apply(instance, (sourceEntityId, targetEntityId) ->
                new RequiemEntityParticleOptions(type, sourceEntityId, targetEntityId)));
    }

    @Override
    public ParticleType<RequiemEntityParticleOptions> getType() {
        return type;
    }

    @Override
    public void writeToNetwork(FriendlyByteBuf buf) {
        buf.writeVarInt(sourceEntityId);
        buf.writeVarInt(targetEntityId);
    }

    @Override
    public String writeToString() {
        return "%s %d %d".formatted(ForgeRegistries.PARTICLE_TYPES.getKey(type), sourceEntityId, targetEntityId);
    }

    public int getSourceEntityId() {
        return sourceEntityId;
    }

    public int getTargetEntityId() {
        return targetEntityId;
    }
}
