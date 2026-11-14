package ua.meekeyn.requiemunofficial.common.config;

import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * Клієнтські налаштування порту. В оригіналі beta.16 {@code graphics.fancyDemonRender}
 * жив у клієнтському конфігу {@code requiem.json5} (клас {@code RequiemConfig.Graphics}
 * читався лише клієнтським міксином {@code PlayerRendererLayerMixin}), тому тут окремий
 * CLIENT-spec, а не COMMON.
 */
public final class RequiemClientConfig {
    public static final ModConfigSpec SPEC;
    public static final ModConfigSpec.BooleanValue FANCY_DEMON_RENDER;

    static {
        ModConfigSpec.Builder builder = new ModConfigSpec.Builder();

        builder.push("requiemunofficial");
        builder.push("graphics");
        FANCY_DEMON_RENDER = builder
                .comment("Fancy shader render for incorporeal players. May impact performance.",
                        "Mirrors beta.16 graphics.fancyDemonRender.",
                        "When false, incorporeal players fall back to the plain end portal render layer.")
                .define("fancyDemonRender", true);
        builder.pop();
        builder.pop();

        SPEC = builder.build();
    }

    private RequiemClientConfig() {
    }
}
