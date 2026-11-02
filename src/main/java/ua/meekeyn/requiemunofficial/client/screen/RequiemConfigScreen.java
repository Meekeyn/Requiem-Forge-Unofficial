package ua.meekeyn.requiemunofficial.client.screen;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.minecraftforge.common.ForgeConfigSpec;
import ua.meekeyn.requiemunofficial.common.config.RequiemConfig;

import java.util.List;

public class RequiemConfigScreen extends Screen {
    private static final int ROW_HEIGHT = 32;
    private static final int BUTTON_HEIGHT = 28;
    private static final int SIDE_BUTTON_WIDTH = 28;

    private final Screen parent;
    private int scroll;
    private int contentHeight;
    private int listTop;
    private int listBottom;

    public RequiemConfigScreen(Screen parent) {
        super(Component.translatable("screen.requiemunofficial.config.title"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        int rowWidth = Math.min(this.width - 24, 380);
        int left = (this.width - rowWidth) / 2;
        this.listTop = 36;
        this.listBottom = this.height - 62;
        int y = listTop - scroll;

        addNumericRow(left, rowWidth, y, "screen.requiemunofficial.config.soul_capture_chance",
                () -> Math.round(RequiemConfig.SOUL_CAPTURE_CHANCE.get() * 100.0D) + "%",
                () -> setDouble(RequiemConfig.SOUL_CAPTURE_CHANCE, 0.0D, 1.0D,
                        RequiemConfig.SOUL_CAPTURE_CHANCE.get() - 0.05D),
                () -> setDouble(RequiemConfig.SOUL_CAPTURE_CHANCE, 0.0D, 1.0D,
                        RequiemConfig.SOUL_CAPTURE_CHANCE.get() + 0.05D));
        y += ROW_HEIGHT;

        addNumericRow(left, rowWidth, y, "screen.requiemunofficial.config.rift_cost",
                () -> Integer.toString(RequiemConfig.RIFT_TELEPORT_ENERGY_COST.get()),
                () -> setInt(RequiemConfig.RIFT_TELEPORT_ENERGY_COST, 0, 1000,
                        RequiemConfig.RIFT_TELEPORT_ENERGY_COST.get() - 1),
                () -> setInt(RequiemConfig.RIFT_TELEPORT_ENERGY_COST, 0, 1000,
                        RequiemConfig.RIFT_TELEPORT_ENERGY_COST.get() + 1));
        y += ROW_HEIGHT;

        addNumericRow(left, rowWidth, y, "screen.requiemunofficial.config.rune_duration",
                () -> Integer.toString(RequiemConfig.RUNE_EFFECT_BASE_DURATION_TICKS.get()),
                () -> setInt(RequiemConfig.RUNE_EFFECT_BASE_DURATION_TICKS, 20, 20 * 60 * 60,
                        RequiemConfig.RUNE_EFFECT_BASE_DURATION_TICKS.get() - 20),
                () -> setInt(RequiemConfig.RUNE_EFFECT_BASE_DURATION_TICKS, 20, 20 * 60 * 60,
                        RequiemConfig.RUNE_EFFECT_BASE_DURATION_TICKS.get() + 20));
        y += ROW_HEIGHT;

        addNumericRow(left, rowWidth, y, "screen.requiemunofficial.config.rune_width_bonus",
                () -> Integer.toString(RequiemConfig.RUNE_EFFECT_WIDTH_DURATION_BONUS_TICKS.get()),
                () -> setInt(RequiemConfig.RUNE_EFFECT_WIDTH_DURATION_BONUS_TICKS, 0, 20 * 60 * 60,
                        RequiemConfig.RUNE_EFFECT_WIDTH_DURATION_BONUS_TICKS.get() - 20),
                () -> setInt(RequiemConfig.RUNE_EFFECT_WIDTH_DURATION_BONUS_TICKS, 0, 20 * 60 * 60,
                        RequiemConfig.RUNE_EFFECT_WIDTH_DURATION_BONUS_TICKS.get() + 20));
        y += ROW_HEIGHT;

        addNumericRow(left, rowWidth, y, "screen.requiemunofficial.config.rune_level_bonus",
                () -> Integer.toString(RequiemConfig.RUNE_EFFECT_LEVEL_DURATION_BONUS_TICKS.get()),
                () -> setInt(RequiemConfig.RUNE_EFFECT_LEVEL_DURATION_BONUS_TICKS, 0, 20 * 60 * 60,
                        RequiemConfig.RUNE_EFFECT_LEVEL_DURATION_BONUS_TICKS.get() - 20),
                () -> setInt(RequiemConfig.RUNE_EFFECT_LEVEL_DURATION_BONUS_TICKS, 0, 20 * 60 * 60,
                        RequiemConfig.RUNE_EFFECT_LEVEL_DURATION_BONUS_TICKS.get() + 20));
        y += ROW_HEIGHT;

        addToggleRow(left, rowWidth, y, "screen.requiemunofficial.config.unconditional_exit",
                RequiemConfig.ALLOW_UNCONDITIONAL_POSSESSION_EXIT);
        y += ROW_HEIGHT;

        addToggleRow(left, rowWidth, y, "screen.requiemunofficial.config.own_body_exit",
                RequiemConfig.ALLOW_OWN_BODY_EXIT_WITHOUT_EMANCIPATION);
        y += ROW_HEIGHT;

        this.contentHeight = y + scroll - listTop;
        int maxScroll = Math.max(0, contentHeight - (listBottom - listTop));
        if (scroll > maxScroll) {
            scroll = maxScroll;
        }

        addRenderableWidget(Button.builder(
                Component.translatable("gui.done"),
                button -> onClose()
        ).bounds(this.width / 2 - 100, this.height - 32, 200, 20).build());
    }

    private boolean isRowVisible(int y) {
        return y >= listTop - 2 && y + BUTTON_HEIGHT <= listBottom + 2;
    }

    private void addNumericRow(int left, int rowWidth, int y, String labelKey, ValueText valueText,
                               Runnable decrease, Runnable increase) {
        boolean visible = isRowVisible(y);
        WrappedButton minus = new WrappedButton(left, y, SIDE_BUTTON_WIDTH, BUTTON_HEIGHT,
                Component.literal("-"), button -> {
            decrease.run();
            rebuildWidgets();
        });
        WrappedButton label = new WrappedButton(left + SIDE_BUTTON_WIDTH + 2, y,
                rowWidth - 2 * SIDE_BUTTON_WIDTH - 4, BUTTON_HEIGHT,
                Component.translatable(labelKey, valueText.get()), button -> {
        });
        WrappedButton plus = new WrappedButton(left + rowWidth - SIDE_BUTTON_WIDTH, y, SIDE_BUTTON_WIDTH,
                BUTTON_HEIGHT, Component.literal("+"), button -> {
            increase.run();
            rebuildWidgets();
        });
        for (WrappedButton widget : new WrappedButton[]{minus, label, plus}) {
            widget.visible = visible;
            widget.active = visible;
            addRenderableWidget(widget);
        }
    }

    private void addToggleRow(int left, int rowWidth, int y, String labelKey, ForgeConfigSpec.BooleanValue value) {
        boolean visible = isRowVisible(y);
        WrappedButton toggle = new WrappedButton(left, y, rowWidth, BUTTON_HEIGHT,
                Component.translatable(labelKey,
                        Component.translatable(value.get() ? "options.on" : "options.off")),
                button -> {
                    value.set(!value.get());
                    RequiemConfig.SPEC.save();
                    rebuildWidgets();
                });
        toggle.visible = visible;
        toggle.active = visible;
        addRenderableWidget(toggle);
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
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        int maxScroll = Math.max(0, contentHeight - (listBottom - listTop));
        if (maxScroll > 0) {
            int next = Math.max(0, Math.min(maxScroll, scroll - (int) Math.signum(delta) * ROW_HEIGHT));
            if (next != scroll) {
                scroll = next;
                rebuildWidgets();
            }
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, delta);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics);
        super.render(graphics, mouseX, mouseY, partialTick);
        graphics.drawCenteredString(this.font, this.title, this.width / 2, 14, 0xFFFFFF);
        graphics.drawCenteredString(
                this.font,
                Component.translatable("screen.requiemunofficial.config.restart_hint"),
                this.width / 2,
                this.height - 50,
                0xA0A0A0
        );
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

    /**
     * Кнопка, що переносить текст на другий рядок і за потреби зменшує його, щоб підпис будь-якою мовою
     * завжди читався повністю.
     */
    private static final class WrappedButton extends Button {
        WrappedButton(int x, int y, int width, int height, Component message, OnPress onPress) {
            super(x, y, width, height, message, onPress, DEFAULT_NARRATION);
        }

        @Override
        public void renderString(GuiGraphics graphics, Font font, int color) {
            int maxWidth = this.getWidth() - 8;
            int maxLines = Math.max(1, (this.getHeight() - 4) / font.lineHeight);
            float scale = 1.0F;
            List<FormattedCharSequence> lines = font.split(this.getMessage(), maxWidth);
            while (lines.size() > maxLines && scale > 0.5F) {
                scale -= 0.1F;
                lines = font.split(this.getMessage(), Math.round(maxWidth / scale));
            }
            float textHeight = lines.size() * font.lineHeight * scale;
            float startY = this.getY() + (this.getHeight() - textHeight) / 2.0F;
            graphics.pose().pushPose();
            graphics.pose().scale(scale, scale, 1.0F);
            for (int i = 0; i < lines.size(); i++) {
                FormattedCharSequence line = lines.get(i);
                float x = (this.getX() + this.getWidth() / 2.0F) / scale - font.width(line) / 2.0F;
                float y = (startY + i * font.lineHeight * scale) / scale;
                graphics.drawString(font, line, Math.round(x), Math.round(y), color, true);
            }
            graphics.pose().popPose();
        }
    }
}
