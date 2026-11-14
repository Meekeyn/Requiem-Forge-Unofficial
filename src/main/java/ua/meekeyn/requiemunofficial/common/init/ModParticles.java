package ua.meekeyn.requiemunofficial.common.init;

import com.mojang.serialization.Codec;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.core.registries.Registries;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.DeferredHolder;
import ua.meekeyn.requiemunofficial.RequiemUnofficial;
import ua.meekeyn.requiemunofficial.common.particle.RequiemEntityParticleOptions;

public final class ModParticles {

    public static final DeferredRegister<ParticleType<?>> PARTICLES =
            DeferredRegister.create(Registries.PARTICLE_TYPE, RequiemUnofficial.MODID);

    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> SOUL =
            PARTICLES.register("soul", () -> new SimpleParticleType(true));

    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> OBELISK_SOUL =
            PARTICLES.register("obelisk_soul", () -> new SimpleParticleType(true));

    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> SOUL_TRAIL =
            PARTICLES.register("soul_trail", () -> new SimpleParticleType(true));

    // beta.16 RequiemParticleTypes: ATTRITION / ATTUNED / CURE / PENANCE (simple, не alwaysShow).
    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> ATTRITION =
            PARTICLES.register("attrition", () -> new SimpleParticleType(false));

    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> ATTUNED =
            PARTICLES.register("attuned", () -> new SimpleParticleType(false));

    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> CURE =
            PARTICLES.register("cure", () -> new SimpleParticleType(false));

    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> PENANCE =
            PARTICLES.register("penance", () -> new SimpleParticleType(false));

    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> GHOST =
            PARTICLES.register("ghost", () -> new SimpleParticleType(true));

    // Пил із текстури сутності, що летить до того, хто витягує душу (beta.16 RequiemParticleTypes.ENTITY_DUST).
    public static final DeferredHolder<ParticleType<?>, ParticleType<RequiemEntityParticleOptions>> ENTITY_DUST =
            PARTICLES.register("entity_dust", () -> new ParticleType<RequiemEntityParticleOptions>(false) {
                @Override
                public com.mojang.serialization.MapCodec<RequiemEntityParticleOptions> codec() {
                    return RequiemEntityParticleOptions.codec(this);
                }

                @Override
                public net.minecraft.network.codec.StreamCodec<? super net.minecraft.network.RegistryFriendlyByteBuf, RequiemEntityParticleOptions> streamCodec() {
                    return RequiemEntityParticleOptions.streamCodec(this);
                }
            });

    private ModParticles() {
    }

    public static void register(IEventBus eventBus) {
        PARTICLES.register(eventBus);
    }
}