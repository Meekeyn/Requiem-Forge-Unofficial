package ua.meekeyn.requiemunofficial.common.init;

import net.minecraft.world.effect.MobEffect;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.minecraft.core.registries.BuiltInRegistries;
import net.neoforged.neoforge.registries.DeferredHolder;
import ua.meekeyn.requiemunofficial.RequiemUnofficial;
import ua.meekeyn.requiemunofficial.common.effect.AttritionMobEffect;
import ua.meekeyn.requiemunofficial.common.effect.EmancipationMobEffect;
import ua.meekeyn.requiemunofficial.common.effect.PenanceMobEffect;
import ua.meekeyn.requiemunofficial.common.effect.ReclamationMobEffect;

public final class ModEffects {

    public static final DeferredRegister<MobEffect> EFFECTS =
            DeferredRegister.create(BuiltInRegistries.MOB_EFFECT, RequiemUnofficial.MODID);

    public static final DeferredHolder<MobEffect, MobEffect> ATTRITION =
            EFFECTS.register("attrition", AttritionMobEffect::new);

    public static final DeferredHolder<MobEffect, MobEffect> EMANCIPATION =
            EFFECTS.register("emancipation", EmancipationMobEffect::new);

    public static final DeferredHolder<MobEffect, MobEffect> PENANCE =
            EFFECTS.register("penance", PenanceMobEffect::new);

    public static final DeferredHolder<MobEffect, MobEffect> RECLAMATION =
            EFFECTS.register("reclamation", ReclamationMobEffect::new);

    private ModEffects() {
    }

    public static void register(IEventBus eventBus) {
        EFFECTS.register(eventBus);
    }
}