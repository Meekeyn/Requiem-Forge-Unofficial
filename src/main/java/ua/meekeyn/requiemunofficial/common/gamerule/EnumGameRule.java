package ua.meekeyn.requiemunofficial.common.gamerule;

import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.GameRules;
import org.jetbrains.annotations.Nullable;

import java.util.Locale;
import java.util.function.BiConsumer;

/**
 * Перелічуване ігрове правило (аналог {@code EnumRule} з Fabric, який використовував оригінал
 * beta.16). Значення читаються/пишуться як текст (напр. {@code CHOOSE}) і працюють через
 * {@code /gamerule}.
 */
public final class EnumGameRule<T extends Enum<T>> extends GameRules.Value<EnumGameRule<T>> {

    private final Class<T> enumClass;
    private T value;

    private EnumGameRule(GameRules.Type<EnumGameRule<T>> type, Class<T> enumClass, T initial) {
        super(type);
        this.enumClass = enumClass;
        this.value = initial;
    }

    public static <T extends Enum<T>> GameRules.Type<EnumGameRule<T>> create(
            Class<T> enumClass,
            T defaultValue,
            BiConsumer<MinecraftServer, EnumGameRule<T>> callback
    ) {
        return new GameRules.Type<>(
                () -> EnumArgumentType.create(enumClass),
                type -> new EnumGameRule<>(type, enumClass, defaultValue),
                callback,
                (visitor, key, type) -> {
                    // Загальний visit уже викликано у GameRules.callVisitorCap; тут нічого не треба.
                }
        );
    }

    public T get() {
        return this.value;
    }

    public void set(T newValue, @Nullable MinecraftServer server) {
        this.value = newValue;
        this.onChanged(server);
    }

    @Override
    protected void updateFromArgument(CommandContext<CommandSourceStack> context, String name) {
        this.value = EnumArgumentType.get(context, name, this.enumClass);
    }

    @Override
    protected void deserialize(String raw) {
        for (T constant : this.enumClass.getEnumConstants()) {
            if (constant.name().equalsIgnoreCase(raw)) {
                this.value = constant;
                return;
            }
        }
    }

    @Override
    public String serialize() {
        return this.value.name();
    }

    @Override
    public int getCommandResult() {
        return this.value.ordinal();
    }

    @Override
    protected EnumGameRule<T> getSelf() {
        return this;
    }

    @Override
    protected EnumGameRule<T> copy() {
        return new EnumGameRule<>(this.type, this.enumClass, this.value);
    }

    @Override
    public void setFrom(EnumGameRule<T> other, @Nullable MinecraftServer server) {
        this.value = other.value;
        this.onChanged(server);
    }

    @Override
    public String toString() {
        return this.value.name().toLowerCase(Locale.ROOT);
    }
}
