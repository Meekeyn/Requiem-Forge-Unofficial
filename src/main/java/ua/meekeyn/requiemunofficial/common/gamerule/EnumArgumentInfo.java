package ua.meekeyn.requiemunofficial.common.gamerule;

import com.google.gson.JsonObject;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.synchronization.ArgumentTypeInfo;
import net.minecraft.network.FriendlyByteBuf;

/**
 * Серіалізація {@link EnumArgumentType} для синхронізації дерева команд із клієнтом.
 *
 * <p>Minecraft 1.20.1 при відправці {@code ClientboundCommandsPacket} шукає тип аргумента у
 * {@code ArgumentTypeInfos.byClass} та {@code BuiltInRegistries.COMMAND_ARGUMENT_TYPE}; без
 * реєстрації вхід у світ падає з {@code IllegalArgumentException: Unrecognized argument type}.
 * Реєстрація виконується через Forge API: {@code ForgeRegistries.COMMAND_ARGUMENT_TYPES}
 * (DeferredRegister, див. {@code ModGameRules}) і {@code ArgumentTypeInfos.registerByClass}.
 *
 * <p>Мережевий формат — повне ім'я класу перелічування, тож клієнт відновлює той самий
 * {@link EnumArgumentType} (парсер, підказки та перевірка значень працюють і на клієнті).
 */
public final class EnumArgumentInfo implements ArgumentTypeInfo<EnumArgumentType<?>, EnumArgumentInfo.Template> {

    public static final EnumArgumentInfo INSTANCE = new EnumArgumentInfo();

    /** Лише класи перелічувань цього пакету дозволено завантажувати з мережі. */
    private static final String ALLOWED_PACKAGE = "ua.meekeyn.requiemunofficial.common.gamerule.";

    private EnumArgumentInfo() {
    }

    /**
     * {@code EnumArgumentType.class} у потрібній параметризації для
     * {@code ArgumentTypeInfos.registerByClass}.
     */
    @SuppressWarnings("unchecked")
    public static Class<EnumArgumentType<?>> enumArgumentClass() {
        return (Class<EnumArgumentType<?>>) (Class<?>) EnumArgumentType.class;
    }

    @Override
    public void serializeToNetwork(Template template, FriendlyByteBuf buffer) {
        buffer.writeUtf(template.enumClass().getName());
    }

    @Override
    public Template deserializeFromNetwork(FriendlyByteBuf buffer) {
        return new Template(loadEnumClass(buffer.readUtf()));
    }

    @Override
    public void serializeToJson(Template template, JsonObject json) {
        json.addProperty("enum", template.enumClass().getName());
    }

    @Override
    public Template unpack(EnumArgumentType<?> argument) {
        return new Template(argument.getEnumClass());
    }

    @SuppressWarnings("unchecked")
    private static Class<? extends Enum<?>> loadEnumClass(String className) {
        if (!className.startsWith(ALLOWED_PACKAGE)) {
            throw new IllegalStateException("Refusing to load enum class outside " + ALLOWED_PACKAGE + ": " + className);
        }

        try {
            Class<?> clazz = Class.forName(className, false, EnumArgumentInfo.class.getClassLoader());
            if (!clazz.isEnum() || !Enum.class.isAssignableFrom(clazz)) {
                throw new IllegalStateException("Not an enum class: " + className);
            }

            return (Class<? extends Enum<?>>) clazz;
        } catch (ClassNotFoundException exception) {
            throw new IllegalStateException("Unknown enum class for the requiem /gamerule argument: " + className, exception);
        }
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static EnumArgumentType<?> create(Class<? extends Enum<?>> enumClass) {
        return EnumArgumentType.create((Class) enumClass);
    }

    /**
     * Шаблон, що зберігає клас перелічування для відтворення {@link EnumArgumentType} з мережі.
     */
    public static final class Template implements ArgumentTypeInfo.Template<EnumArgumentType<?>> {

        private final Class<? extends Enum<?>> enumClass;

        Template(Class<? extends Enum<?>> enumClass) {
            this.enumClass = enumClass;
        }

        public Class<? extends Enum<?>> enumClass() {
            return this.enumClass;
        }

        @Override
        public EnumArgumentType<?> instantiate(CommandBuildContext context) {
            return create(this.enumClass);
        }

        @Override
        public ArgumentTypeInfo<EnumArgumentType<?>, ?> type() {
            return INSTANCE;
        }
    }
}
