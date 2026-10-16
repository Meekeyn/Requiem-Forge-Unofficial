package ua.meekeyn.requiemunofficial.common.init;

import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.core.registries.Registries;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;
import ua.meekeyn.requiemunofficial.RequiemUnofficial;

public final class ModParticles {

    public static final DeferredRegister<ParticleType<?>> PARTICLES =
            DeferredRegister.create(Registries.PARTICLE_TYPE, RequiemUnofficial.MODID);

    public static final RegistryObject<SimpleParticleType> SOUL =
            PARTICLES.register("soul", () -> new SimpleParticleType(true));

    public static final RegistryObject<SimpleParticleType> OBELISK_SOUL =
            PARTICLES.register("obelisk_soul", () -> new SimpleParticleType(true));

    public static final RegistryObject<SimpleParticleType> SOUL_TRAIL =
            PARTICLES.register("soul_trail", () -> new SimpleParticleType(true));

    public static final RegistryObject<SimpleParticleType> GHOST =
            PARTICLES.register("ghost", () -> new SimpleParticleType(true));

    private ModParticles() {
    }

    public static void register(IEventBus eventBus) {
        PARTICLES.register(eventBus);
    }
}