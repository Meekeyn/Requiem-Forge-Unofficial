package ua.meekeyn.requiemunofficial.common.recipe;

import com.mojang.serialization.MapCodec;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.conditions.ICondition;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;
import ua.meekeyn.requiemunofficial.RequiemUnofficial;

public final class RecipeConditions {

    private static final DeferredRegister<MapCodec<? extends ICondition>> CONDITIONS =
            DeferredRegister.create(NeoForgeRegistries.CONDITION_SERIALIZERS, RequiemUnofficial.MODID);

    static {
        CONDITIONS.register("config", () -> ConfigRecipeCondition.CODEC);
    }

    private RecipeConditions() {
    }

    public static void register(IEventBus modEventBus) {
        CONDITIONS.register(modEventBus);
    }
}
