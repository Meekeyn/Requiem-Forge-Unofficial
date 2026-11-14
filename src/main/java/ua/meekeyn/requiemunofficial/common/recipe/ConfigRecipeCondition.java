package ua.meekeyn.requiemunofficial.common.recipe;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.neoforged.neoforge.common.conditions.ICondition;
import ua.meekeyn.requiemunofficial.common.config.RequiemConfig;

/**
 * Умова рецепта {@code requiemunofficial:config}: вмикає port-only рецепти лише за відповідного
 * налаштування COMMON-конфігу. Перевіряється при завантаженні датапаків (/reload).
 */
public record ConfigRecipeCondition(String option) implements ICondition {
    public static final MapCodec<ConfigRecipeCondition> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            com.mojang.serialization.Codec.STRING.fieldOf("option").forGetter(ConfigRecipeCondition::option)
    ).apply(instance, ConfigRecipeCondition::new));

    @Override
    public boolean test(IContext context) {
        return switch (this.option) {
            case "runeCraftingRecipes" -> RequiemConfig.RUNE_CRAFTING_RECIPES.get();
            default -> false;
        };
    }

    @Override
    public MapCodec<? extends ICondition> codec() {
        return CODEC;
    }
}
