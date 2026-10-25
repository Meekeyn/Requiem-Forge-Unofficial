package ua.meekeyn.requiemunofficial.common.gamerule;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.network.chat.Component;

import java.util.Arrays;
import java.util.Collection;
import java.util.Locale;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

/**
 * Аргумент команди {@code /gamerule} для перелічуваних правил, що відновлює текстові
 * значення оригіналу beta.16 (напр. {@code CHOOSE}, {@code FORCE_REMNANT}).
 */
public final class EnumArgumentType<T extends Enum<T>> implements ArgumentType<T> {

    private static final DynamicCommandExceptionType INVALID_VALUE =
            new DynamicCommandExceptionType(value -> Component.translatable(
                    "argument.requiemunofficial.enum.invalid", value
            ));

    private final Class<T> enumClass;

    private EnumArgumentType(Class<T> enumClass) {
        this.enumClass = enumClass;
    }

    public static <T extends Enum<T>> EnumArgumentType<T> create(Class<T> enumClass) {
        return new EnumArgumentType<>(enumClass);
    }

    /**
     * Клас перелічування, який приймає цей аргумент; використовується
     * {@link EnumArgumentInfo} для серіалізації типу в дереві команд.
     */
    public Class<T> getEnumClass() {
        return this.enumClass;
    }

    @Override
    public T parse(StringReader reader) throws CommandSyntaxException {
        int start = reader.getCursor();
        String word = reader.readUnquotedString();
        for (T constant : this.enumClass.getEnumConstants()) {
            if (constant.name().equalsIgnoreCase(word)) {
                return constant;
            }
        }
        reader.setCursor(start);
        throw INVALID_VALUE.createWithContext(reader, word);
    }

    public static <T extends Enum<T>> T get(CommandContext<?> context, String name, Class<T> enumClass) {
        return context.getArgument(name, enumClass);
    }

    @Override
    public <S> CompletableFuture<Suggestions> listSuggestions(CommandContext<S> context, SuggestionsBuilder builder) {
        return SharedSuggestionProvider.suggest(
                Arrays.stream(this.enumClass.getEnumConstants())
                        .map(constant -> constant.name().toLowerCase(Locale.ROOT))
                        .collect(Collectors.toList()),
                builder
        );
    }

    @Override
    public Collection<String> getExamples() {
        return Arrays.stream(this.enumClass.getEnumConstants())
                .map(Enum::name)
                .collect(Collectors.toList());
    }
}
