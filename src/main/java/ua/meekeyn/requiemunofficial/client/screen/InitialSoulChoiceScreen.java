package ua.meekeyn.requiemunofficial.client.screen;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import ua.meekeyn.requiemunofficial.client.ClientDeathSuspensionFx;
import ua.meekeyn.requiemunofficial.common.network.InitialSoulChoicePacket;
import ua.meekeyn.requiemunofficial.common.network.ModNetwork;
import ua.meekeyn.requiemunofficial.common.soul.SoulState;

public final class InitialSoulChoiceScreen extends Screen {
    private DialogueState state = DialogueState.INTRODUCTION;
    private boolean controlsVisible;

    private InitialSoulChoiceScreen() {
        super(Component.translatable("screen.requiemunofficial.soul_choice.title"));
    }

    public static void open() {
        Minecraft.getInstance().setScreen(new InitialSoulChoiceScreen());
    }

    @Override
    protected void init() {
        rebuildControls();
    }

    @Override
    public void tick() {
        if (!controlsVisible && ClientDeathSuspensionFx.hasFinishedAnimation()) {
            rebuildControls();
        }
    }

    private void rebuildControls() {
        clearWidgets();
        controlsVisible = ClientDeathSuspensionFx.hasFinishedAnimation();
        if (!controlsVisible) {
            return;
        }

        int left = 10;
        int wrapWidth = Math.min(300, this.width - 20);
        int textHeight = this.font.wordWrapHeight(Component.translatable(state.textKey), wrapWidth);
        int buttonWidth = Math.min(300, this.width - 20);
        int y = 40 + textHeight + 14;

        for (DialogueChoice choice : state.choices()) {
            addRenderableWidget(new DialogueChoiceButton(
                    left,
                    y,
                    buttonWidth,
                    18,
                    Component.translatable(choice.translationKey),
                    () -> select(choice)
            ));
            y += 24;
        }
    }

    private void select(DialogueChoice choice) {
        if (choice.finalChoice != null) {
            choose(choice.finalChoice);
            return;
        }
        state = choice.next;
        rebuildControls();
    }

    private void choose(SoulState state) {
        ModNetwork.CHANNEL.sendToServer(new InitialSoulChoicePacket(state));
        onClose();
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
        if (!ClientDeathSuspensionFx.hasFinishedAnimation()) {
            return;
        }

        renderBackground(graphics);
        int left = 10;
        int wrapWidth = Math.min(300, this.width - 20);
        graphics.drawWordWrap(
                this.font,
                Component.translatable(state.textKey),
                left,
                40,
                wrapWidth,
                0xD8D8D8
        );
        graphics.drawCenteredString(this.font, Component.translatable("screen.requiemunofficial.soul_choice.instructions"), this.width / 2, this.height - 18, 0xA0A0A0);
        super.render(graphics, mouseX, mouseY, partialTicks);
    }

    @Override
    public boolean shouldCloseOnEsc() {
        return false;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private enum DialogueState {
        INTRODUCTION("requiemunofficial:dialogue.first_contact.introduction.text"),
        EXPLANATION("requiemunofficial:dialogue.first_contact.explanation.text"),
        INTERESTED("requiemunofficial:dialogue.first_contact.interested.text"),
        DISINTERESTED("requiemunofficial:dialogue.first_contact.disinterested.text"),
        VERY_INTERESTED("requiemunofficial:dialogue.first_contact.very_interested.text"),
        VERY_DISINTERESTED("requiemunofficial:dialogue.first_contact.very_disinterested.text");

        private final String textKey;

        DialogueState(String textKey) {
            this.textKey = textKey;
        }

        private DialogueChoice[] choices() {
            return switch (this) {
                case INTRODUCTION -> new DialogueChoice[] {
                        new DialogueChoice("requiemunofficial:dialogue.first_contact.introduction.explain", EXPLANATION),
                        new DialogueChoice("requiemunofficial:dialogue.first_contact.introduction.yes", INTERESTED),
                        new DialogueChoice("requiemunofficial:dialogue.first_contact.introduction.no", DISINTERESTED)
                };
                case EXPLANATION -> new DialogueChoice[] {
                        new DialogueChoice("requiemunofficial:dialogue.first_contact.explanation.ok", INTRODUCTION)
                };
                case INTERESTED -> new DialogueChoice[] {
                        new DialogueChoice("requiemunofficial:dialogue.first_contact.doubt", INTRODUCTION),
                        new DialogueChoice("requiemunofficial:dialogue.first_contact.confirm", VERY_INTERESTED)
                };
                case DISINTERESTED -> new DialogueChoice[] {
                        new DialogueChoice("requiemunofficial:dialogue.first_contact.doubt", INTRODUCTION),
                        new DialogueChoice("requiemunofficial:dialogue.first_contact.confirm", VERY_DISINTERESTED)
                };
                case VERY_INTERESTED -> new DialogueChoice[] {
                        new DialogueChoice("requiemunofficial:dialogue.first_contact.final_doubt", INTERESTED),
                        DialogueChoice.finalChoice("requiemunofficial:dialogue.first_contact.final_confirm", SoulState.REMNANT)
                };
                case VERY_DISINTERESTED -> new DialogueChoice[] {
                        new DialogueChoice("requiemunofficial:dialogue.first_contact.final_doubt", DISINTERESTED),
                        DialogueChoice.finalChoice("requiemunofficial:dialogue.first_contact.final_confirm", SoulState.MORTAL)
                };
            };
        }
    }

    private static final class DialogueChoice {
        private final String translationKey;
        private final DialogueState next;
        private final SoulState finalChoice;

        private DialogueChoice(String translationKey, DialogueState next) {
            this.translationKey = translationKey;
            this.next = next;
            this.finalChoice = null;
        }

        private DialogueChoice(String translationKey, SoulState finalChoice) {
            this.translationKey = translationKey;
            this.next = null;
            this.finalChoice = finalChoice;
        }

        private static DialogueChoice finalChoice(String translationKey, SoulState finalChoice) {
            return new DialogueChoice(translationKey, finalChoice);
        }
    }

    private final class DialogueChoiceButton extends AbstractButton {
        private final Runnable onPress;

        private DialogueChoiceButton(int x, int y, int width, int height, Component message, Runnable onPress) {
            super(x, y, width, height, message);
            this.onPress = onPress;
        }

        @Override
        public void onPress() {
            onPress.run();
        }

        @Override
        protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
            int color = isHoveredOrFocused() ? 0xFFFFA0 : 0xD8D8D8;
            int textX = getX() + (isHoveredOrFocused() ? 10 : 0);
            if (isHoveredOrFocused()) {
                graphics.drawString(InitialSoulChoiceScreen.this.font, ">", getX(), getY() + 5, 0xFFFFA0, false);
            }
            graphics.drawString(InitialSoulChoiceScreen.this.font, getMessage(), textX, getY() + 5, color, false);
        }

        @Override
        protected void updateWidgetNarration(NarrationElementOutput narration) {
            defaultButtonNarrationText(narration);
        }
    }
}
