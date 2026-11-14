package ua.meekeyn.requiemunofficial.client.screen;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.client.gui.components.StringWidget;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.GameRules;
import net.neoforged.neoforge.common.ModConfigSpec;
import ua.meekeyn.requiemunofficial.common.config.RequiemClientConfig;
import ua.meekeyn.requiemunofficial.common.init.ModGameRules;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;
import ua.meekeyn.requiemunofficial.common.config.RequiemConfig;

import java.util.List;

public class RequiemConfigScreen extends Screen {
    private static final int ROW_HEIGHT = 32;
    private static final int BUTTON_HEIGHT = 28;
    private static final int SIDE_BUTTON_WIDTH = 28;

    private static final int HEADER_HEIGHT = 20;
    private record RuleRow(String labelKey, String id, String[] options, Function<GameRules, String> reader) {
    }

    private static final String[] BOOLEAN_OPTIONS = {"false", "true"};
    private static final List<RuleRow> RULE_ROWS = List.of(
            new RuleRow("screen.requiemunofficial.config.rule_show_possessor_name_tag", "requiem:showPossessorNameTag",
                    BOOLEAN_OPTIONS, rules -> rules.getRule(ModGameRules.SHOW_POSSESSOR_NAME_TAG).serialize()),
            new RuleRow("screen.requiemunofficial.config.rule_disable_cure", "requiem:disableCure",
                    BOOLEAN_OPTIONS, rules -> rules.getRule(ModGameRules.DISABLE_CURE).serialize()),
            new RuleRow("screen.requiemunofficial.config.rule_drop_vessel", "requiem:dropVesselOnDisconnect",
                    BOOLEAN_OPTIONS, rules -> rules.getRule(ModGameRules.DROP_VESSEL_ON_DISCONNECT).serialize()),
            new RuleRow("screen.requiemunofficial.config.rule_starting_remnant", "requiem:startingRemnantType",
                    new String[]{"CHOOSE", "FORCE_REMNANT", "FORCE_VANILLA"},
                    rules -> rules.getRule(ModGameRules.STARTING_REMNANT_TYPE).serialize()),
            new RuleRow("screen.requiemunofficial.config.rule_keep_inventory", "requiem:possessionKeepInventory",
                    new String[]{"NEVER", "LIVING", "ALWAYS"},
                    rules -> rules.getRule(ModGameRules.POSSESSION_KEEP_INVENTORY).serialize()),
            new RuleRow("screen.requiemunofficial.config.rule_detection", "requiem:possessionDetection",
                    new String[]{"DISABLED", "NORMAL", "HARD"},
                    rules -> rules.getRule(ModGameRules.POSSESSION_DETECTION).serialize()));

    private final Map<String, String> sentRuleValues = new HashMap<>();
    private final Screen parent;
    private int scroll;
    private int contentHeight;
    private int listTop;
    private int listBottom;
    private int scrollbarX;
    private boolean draggingScrollbar;

    public RequiemConfigScreen(Screen parent) {
        super(Component.translatable("screen.requiemunofficial.config.title"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        int rowWidth = Math.min(this.width - 36, 380);
        int left = (this.width - rowWidth) / 2 - 4;
        this.scrollbarX = left + rowWidth + 6;
        this.listTop = 36;
        this.listBottom = this.height - 62;
        int y = listTop - scroll;

        // Налаштування звичайного Requiem — вгорі; нижче лише те, що додав порт.
        addSectionHeader(left, rowWidth, y, "screen.requiemunofficial.config.section_requiem");
        y += HEADER_HEIGHT;
        addToggleRow(left, rowWidth, y, "screen.requiemunofficial.config.fancy_demon_render",
                RequiemClientConfig.FANCY_DEMON_RENDER, RequiemClientConfig.SPEC);
        y += ROW_HEIGHT;

        // Ігрові правила оригінального Requiem (/gamerule requiem:*) — діють на світ, тому лише там, де їх можна змінити.
        if (canEditGameRules()) {
            addSectionHeader(left, rowWidth, y, "screen.requiemunofficial.config.section_world_rules");
            y += HEADER_HEIGHT;
            for (RuleRow rule : RULE_ROWS) {
                addRuleRow(left, rowWidth, y, rule);
                y += ROW_HEIGHT;
            }
        }

        addSectionHeader(left, rowWidth, y, "screen.requiemunofficial.config.section_port");
        y += HEADER_HEIGHT;

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

        addToggleRow(left, rowWidth, y, "screen.requiemunofficial.config.obelisk_soul_blocks",
                RequiemConfig.OBELISK_REQUIRE_SOUL_BLOCKS);
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

    private boolean canEditGameRules() {
        if (this.minecraft == null || this.minecraft.player == null) {
            return false;
        }
        return this.minecraft.getSingleplayerServer() != null || this.minecraft.player.hasPermissions(2);
    }

    /** Поточне значення правила: у одиночній грі читаємо з сервера, на чужому сервері — лише те, що змінили тут. */
    private String ruleValue(RuleRow rule) {
        String sent = sentRuleValues.get(rule.id());
        if (sent != null) {
            return sent;
        }
        MinecraftServer server = this.minecraft.getSingleplayerServer();
        return server != null ? rule.reader().apply(server.getGameRules()) : "?";
    }

    private void applyRule(RuleRow rule, String value) {
        String command = "gamerule " + rule.id() + " " + value;
        MinecraftServer server = this.minecraft.getSingleplayerServer();
        if (server != null) {
            server.execute(() -> server.getCommands()
                    .performPrefixedCommand(server.createCommandSourceStack(), command));
        } else if (this.minecraft.player != null) {
            this.minecraft.player.connection.sendCommand(command);
        }
        sentRuleValues.put(rule.id(), value);
    }

    private void addRuleRow(int left, int rowWidth, int y, RuleRow rule) {
        String current = ruleValue(rule);
        Component shown = rule.options().length == 2 && rule.options()[0].equals("false")
                ? Component.translatable(current.equals("true") ? "options.on" : "options.off")
                : Component.literal(current);
        WrappedButton button = new WrappedButton(left, y, rowWidth, BUTTON_HEIGHT,
                Component.translatable(rule.labelKey(), shown), pressed -> {
            int index = 0;
            for (int i = 0; i < rule.options().length; i++) {
                if (rule.options()[i].equalsIgnoreCase(current)) {
                    index = i + 1;
                    break;
                }
            }
            applyRule(rule, rule.options()[index % rule.options().length]);
            rebuildWidgets();
        });
        boolean visible = isRowVisible(y);
        button.visible = visible;
        button.active = visible;
        addRenderableWidget(button);
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

    private void addSectionHeader(int left, int rowWidth, int y, String labelKey) {
        if (!isRowVisible(y)) {
            return;
        }
        addRenderableWidget(new StringWidget(left, y, rowWidth, HEADER_HEIGHT - 4,
                Component.translatable(labelKey).withStyle(net.minecraft.ChatFormatting.YELLOW), this.font));
    }

    private void addToggleRow(int left, int rowWidth, int y, String labelKey, ModConfigSpec.BooleanValue value) {
        addToggleRow(left, rowWidth, y, labelKey, value, RequiemConfig.SPEC);
    }

    private void addToggleRow(int left, int rowWidth, int y, String labelKey, ModConfigSpec.BooleanValue value,
                              ModConfigSpec spec) {
        boolean visible = isRowVisible(y);
        WrappedButton toggle = new WrappedButton(left, y, rowWidth, BUTTON_HEIGHT,
                Component.translatable(labelKey,
                        Component.translatable(value.get() ? "options.on" : "options.off")),
                button -> {
                    value.set(!value.get());
                    spec.save();
                    rebuildWidgets();
                });
        toggle.visible = visible;
        toggle.active = visible;
        addRenderableWidget(toggle);
    }

    private static void setDouble(ModConfigSpec.DoubleValue configValue, double min, double max, double value) {
        configValue.set(Math.max(min, Math.min(max, value)));
        RequiemConfig.SPEC.save();
    }

    private static void setInt(ModConfigSpec.IntValue configValue, int min, int max, int value) {
        configValue.set(Math.max(min, Math.min(max, value)));
        RequiemConfig.SPEC.save();
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double delta) {
        int maxScroll = Math.max(0, contentHeight - (listBottom - listTop));
        if (maxScroll > 0) {
            int next = Math.max(0, Math.min(maxScroll, scroll - (int) Math.signum(delta) * ROW_HEIGHT));
            if (next != scroll) {
                scroll = next;
                rebuildWidgets();
            }
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, delta);
    }

    private int maxScroll() {
        return Math.max(0, contentHeight - (listBottom - listTop));
    }

    private int thumbHeight() {
        int track = listBottom - listTop;
        return Math.max(16, track * track / Math.max(track, contentHeight));
    }

    private void scrollToMouse(double mouseY) {
        int max = maxScroll();
        int range = (listBottom - listTop) - thumbHeight();
        if (max <= 0 || range <= 0) return;
        double t = (mouseY - listTop - thumbHeight() / 2.0D) / range;
        int next = (int) Math.round(Math.max(0.0D, Math.min(1.0D, t)) * max);
        if (next != scroll) {
            scroll = next;
            rebuildWidgets();
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0 && maxScroll() > 0 && mouseX >= scrollbarX && mouseX < scrollbarX + 6
                && mouseY >= listTop && mouseY < listBottom) {
            draggingScrollbar = true;
            scrollToMouse(mouseY);
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (draggingScrollbar) {
            scrollToMouse(mouseY);
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        draggingScrollbar = false;
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        graphics.drawCenteredString(this.font, this.title, this.width / 2, 14, 0xFFFFFF);
        int max = maxScroll();
        if (max > 0) {
            int thumb = thumbHeight();
            int thumbY = listTop + (int) ((long) scroll * ((listBottom - listTop) - thumb) / max);
            graphics.fill(scrollbarX, listTop, scrollbarX + 6, listBottom, 0xFF000000);
            graphics.fill(scrollbarX, thumbY, scrollbarX + 6, thumbY + thumb, 0xFF808080);
            graphics.fill(scrollbarX, thumbY, scrollbarX + 5, thumbY + thumb - 1, 0xFFC0C0C0);
            if (scroll < max) {
                graphics.drawCenteredString(this.font, "▼", this.width / 2, listBottom + 2, 0xA0A0A0);
            }
        }
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
