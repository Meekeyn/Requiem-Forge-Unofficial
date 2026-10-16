package ua.meekeyn.requiemunofficial.common.init;

import net.minecraft.world.effect.MobEffect;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import ua.meekeyn.requiemunofficial.RequiemUnofficial;
import ua.meekeyn.requiemunofficial.common.effect.AttritionMobEffect;
import ua.meekeyn.requiemunofficial.common.effect.EmancipationMobEffect;
import ua.meekeyn.requiemunofficial.common.effect.PenanceMobEffect;
import ua.meekeyn.requiemunofficial.common.effect.ReclamationMobEffect;

public final class ModEffects {

    public static final DeferredRegister<MobEffect> EFFECTS =
            DeferredRegister.create(ForgeRegistries.MOB_EFFECTS, RequiemUnofficial.MODID);

    public static final RegistryObject<MobEffect> ATTRITION =
            EFFECTS.register("attrition", AttritionMobEffect::new);

    public static final RegistryObject<MobEffect> EMANCIPATION =
            EFFECTS.register("emancipation", EmancipationMobEffect::new);

    public static final RegistryObject<MobEffect> PENANCE =
            EFFECTS.register("penance", PenanceMobEffect::new);

    public static final RegistryObject<MobEffect> RECLAMATION =
            EFFECTS.register("reclamation", ReclamationMobEffect::new);

    private ModEffects() {
    }

    public static void register(IEventBus eventBus) {
        EFFECTS.register(eventBus);
    }
}