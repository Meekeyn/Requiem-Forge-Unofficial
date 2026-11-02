package ua.meekeyn.requiemunofficial.common.recipe;

import com.google.gson.JsonObject;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.GsonHelper;
import net.minecraftforge.common.crafting.conditions.ICondition;
import net.minecraftforge.common.crafting.conditions.IConditionSerializer;
import ua.meekeyn.requiemunofficial.common.config.RequiemConfig;
import ua.meekeyn.requiemunofficial.common.util.ModIds;

/**
 * Умова рецепта {@code requiemunofficial:config}: вмикає port-only рецепти лише за відповідного
 * налаштування COMMON-конфігу. Перевіряється при завантаженні датапаків (/reload).
 */
public record ConfigRecipeCondition(String option) implements ICondition {
    public static final ResourceLocation ID = ModIds.id("config");

    @Override
    public ResourceLocation getID() {
        return ID;
    }

    @Override
    public boolean test(IContext context) {
        return switch (this.option) {
            case "runeCraftingRecipes" -> RequiemConfig.RUNE_CRAFTING_RECIPES.get();
            default -> false;
        };
    }

    public static final class Serializer implements IConditionSerializer<ConfigRecipeCondition> {
        public static final Serializer INSTANCE = new Serializer();

        @Override
        public void write(JsonObject json, ConfigRecipeCondition value) {
            json.addProperty("option", value.option());
        }

        @Override
        public ConfigRecipeCondition read(JsonObject json) {
            return new ConfigRecipeCondition(GsonHelper.getAsString(json, "option"));
        }

        @Override
        public ResourceLocation getID() {
            return ID;
        }
    }
}
