package ua.meekeyn.requiemunofficial.common.init;

import com.mojang.serialization.Codec;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.core.registries.Registries;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;
import ua.meekeyn.requiemunofficial.RequiemUnofficial;
import ua.meekeyn.requiemunofficial.common.particle.RequiemEntityParticleOptions;

public final class ModParticles {

    public static final DeferredRegister<ParticleType<?>> PARTICLES =
            DeferredRegister.create(Registries.PARTICLE_TYPE, RequiemUnofficial.MODID);

    public static final RegistryObject<SimpleParticleType> SOUL =
            PARTICLES.register("soul", () -> new SimpleParticleType(true));

    public static final RegistryObject<SimpleParticleType> OBELISK_SOUL =
            PARTICLES.register("obelisk_soul", () -> new SimpleParticleType(true));

    public static final RegistryObject<SimpleParticleType> SOUL_TRAIL =
            PARTICLES.register("soul_trail", () -> new SimpleParticleType(true));

    // beta.16 RequiemParticleTypes: ATTRITION / ATTUNED / CURE / PENANCE (simple, не alwaysShow).
    public static final RegistryObject<SimpleParticleType> ATTRITION =
            PARTICLES.register("attrition", () -> new SimpleParticleType(false));

    public static final RegistryObject<SimpleParticleType> ATTUNED =
            PARTICLES.register("attuned", () -> new SimpleParticleType(false));

    public static final RegistryObject<SimpleParticleType> CURE =
            PARTICLES.register("cure", () -> new SimpleParticleType(false));

    public static final RegistryObject<SimpleParticleType> PENANCE =
            PARTICLES.register("penance", () -> new SimpleParticleType(false));

    public static final RegistryObject<SimpleParticleType> GHOST =
            PARTICLES.register("ghost", () -> new SimpleParticleType(true));

    // Пил із текстури сутності, що летить до того, хто витягує душу (beta.16 RequiemParticleTypes.ENTITY_DUST).
    public static final RegistryObject<ParticleType<RequiemEntityParticleOptions>> ENTITY_DUST =
            PARTICLES.register("entity_dust", () -> new ParticleType<RequiemEntityParticleOptions>(
                    false, RequiemEntityParticleOptions.DESERIALIZER) {
                @Override
                public Codec<RequiemEntityParticleOptions> codec() {
                    return RequiemEntityParticleOptions.codec(this);
                }
            });

    private ModParticles() {
    }

    public static void register(IEventBus eventBus) {
        PARTICLES.register(eventBus);
    }
}