package ua.meekeyn.requiemunofficial.client.screen;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraftforge.common.ForgeConfigSpec;
import ua.meekeyn.requiemunofficial.common.config.RequiemConfig;

public class RequiemConfigScreen extends Screen {
    private final Screen parent;

    public RequiemConfigScreen(Screen parent) {
        super(Component.translatable("screen.requiemunofficial.config.title"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        int centerX = this.width / 2;
        int y = this.height / 6 + 4;

        addNumericButtons(centerX, y, "screen.requiemunofficial.config.soul_capture_chance",
                () -> Math.round(RequiemConfig.SOUL_CAPTURE_CHANCE.get() * 100.0D) + "%",
                () -> setDouble(RequiemConfig.SOUL_CAPTURE_CHANCE, 0.0D, 1.0D,
                        RequiemConfig.SOUL_CAPTURE_CHANCE.get() - 0.05D),
                () -> setDouble(RequiemConfig.SOUL_CAPTURE_CHANCE, 0.0D, 1.0D,
                        RequiemConfig.SOUL_CAPTURE_CHANCE.get() + 0.05D));
        y += 24;

        addNumericButtons(centerX, y, "screen.requiemunofficial.config.rift_cost",
                () -> Integer.toString(RequiemConfig.RIFT_TELEPORT_ENERGY_COST.get()),
                () -> setInt(RequiemConfig.RIFT_TELEPORT_ENERGY_COST, 0, 1000,
                        RequiemConfig.RIFT_TELEPORT_ENERGY_COST.get() - 1),
                () -> setInt(RequiemConfig.RIFT_TELEPORT_ENERGY_COST, 0, 1000,
                        RequiemConfig.RIFT_TELEPORT_ENERGY_COST.get() + 1));
        y += 24;

        addNumericButtons(centerX, y, "screen.requiemunofficial.config.rune_duration",
                () -> Integer.toString(RequiemConfig.RUNE_EFFECT_BASE_DURATION_TICKS.get()),
                () -> setInt(RequiemConfig.RUNE_EFFECT_BASE_DURATION_TICKS, 20, 20 * 60 * 60,
                        RequiemConfig.RUNE_EFFECT_BASE_DURATION_TICKS.get() - 20),
                () -> setInt(RequiemConfig.RUNE_EFFECT_BASE_DURATION_TICKS, 20, 20 * 60 * 60,
                        RequiemConfig.RUNE_EFFECT_BASE_DURATION_TICKS.get() + 20));
        y += 24;

        addNumericButtons(centerX, y, "screen.requiemunofficial.config.rune_width_bonus",
                () -> Integer.toString(RequiemConfig.RUNE_EFFECT_WIDTH_DURATION_BONUS_TICKS.get()),
                () -> setInt(RequiemConfig.RUNE_EFFECT_WIDTH_DURATION_BONUS_TICKS, 0, 20 * 60 * 60,
                        RequiemConfig.RUNE_EFFECT_WIDTH_DURATION_BONUS_TICKS.get() - 20),
                () -> setInt(RequiemConfig.RUNE_EFFECT_WIDTH_DURATION_BONUS_TICKS, 0, 20 * 60 * 60,
                        RequiemConfig.RUNE_EFFECT_WIDTH_DURATION_BONUS_TICKS.get() + 20));
        y += 24;

        addNumericButtons(centerX, y, "screen.requiemunofficial.config.rune_level_bonus",
                () -> Integer.toString(RequiemConfig.RUNE_EFFECT_LEVEL_DURATION_BONUS_TICKS.get()),
                () -> setInt(RequiemConfig.RUNE_EFFECT_LEVEL_DURATION_BONUS_TICKS, 0, 20 * 60 * 60,
                        RequiemConfig.RUNE_EFFECT_LEVEL_DURATION_BONUS_TICKS.get() - 20),
                () -> setInt(RequiemConfig.RUNE_EFFECT_LEVEL_DURATION_BONUS_TICKS, 0, 20 * 60 * 60,
                        RequiemConfig.RUNE_EFFECT_LEVEL_DURATION_BONUS_TICKS.get() + 20));
        y += 24;

        addRenderableWidget(Button.builder(
                Component.translatable("screen.requiemunofficial.config.unconditional_exit",
                        Component.translatable(RequiemConfig.ALLOW_UNCONDITIONAL_POSSESSION_EXIT.get()
                                ? "options.on" : "options.off")),
                button -> {
                    RequiemConfig.ALLOW_UNCONDITIONAL_POSSESSION_EXIT.set(
                            !RequiemConfig.ALLOW_UNCONDITIONAL_POSSESSION_EXIT.get());
                    RequiemConfig.SPEC.save();
                    rebuildWidgets();
                }
        ).bounds(centerX - 100, y, 200, 20).build());

        addRenderableWidget(Button.builder(
                Component.translatable("gui.done"),
                button -> onClose()
        ).bounds(centerX - 100, this.height - 32, 200, 20).build());
    }

    private void addNumericButtons(int centerX, int y, String labelKey, ValueText valueText,
                                   Runnable decrease, Runnable increase) {
        addRenderableWidget(Button.builder(Component.literal("-"), button -> {
            decrease.run();
            rebuildWidgets();
        }).bounds(centerX - 100, y, 22, 20).build());
        addRenderableWidget(Button.builder(
                Component.translatable(labelKey, valueText.get()),
                button -> {
                }
        ).bounds(centerX - 74, y, 148, 20).build());
        addRenderableWidget(Button.builder(Component.literal("+"), button -> {
            increase.run();
            rebuildWidgets();
        }).bounds(centerX + 78, y, 22, 20).build());
    }

    private static void setDouble(ForgeConfigSpec.DoubleValue configValue, double min, double max, double value) {
        configValue.set(Math.max(min, Math.min(max, value)));
        RequiemConfig.SPEC.save();
    }

    private static void setInt(ForgeConfigSpec.IntValue configValue, int min, int max, int value) {
        configValue.set(Math.max(min, Math.min(max, value)));
        RequiemConfig.SPEC.save();
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics);
        graphics.drawCenteredString(this.font, this.title, this.width / 2, 20, 0xFFFFFF);
        graphics.drawCenteredString(
                this.font,
                Component.translatable("screen.requiemunofficial.config.restart_hint"),
                this.width / 2,
                this.height - 52,
                0xA0A0A0
        );
        super.render(graphics, mouseX, mouseY, partialTick);
    }

    @Override
    public void onClose() {
        if (this.minecraft != null) {
            this.minecraft.setScreen(parent);
        }
    }

    @FunctionalInterface
    private interface ValueText {
        String get();
    }
}
