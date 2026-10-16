package ua.meekeyn.requiemunofficial.client.screen;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import ua.meekeyn.requiemunofficial.common.config.RequiemConfig;
import ua.meekeyn.requiemunofficial.common.menu.RiftMenu;
import ua.meekeyn.requiemunofficial.common.util.ModIds;

public class RiftScreen extends AbstractContainerScreen<RiftMenu> {

    private static final ResourceLocation BACKGROUND =
            ModIds.id("textures/gui/soul_rift.png");

    private Button resurrectionButton;
    private Button decayZombieButton;
    private Button decaySkeletonButton;
    private Button teleportButton;
    private Button previousDestinationButton;
    private Button nextDestinationButton;
    private int selectedDestination;

    public RiftScreen(RiftMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        this.imageWidth = 176;
        this.imageHeight = 166;
    }

    @Override
    protected void init() {
        super.init();
        this.inventoryLabelY = this.imageHeight - 94;
        this.resurrectionButton = addRenderableWidget(Button.builder(
                Component.translatable("gui.requiemunofficial.rift.start_resurrection"),
                button -> {
                    if (this.minecraft != null && this.minecraft.gameMode != null) {
                        this.minecraft.gameMode.handleInventoryButtonClick(
                                this.menu.containerId,
                                RiftMenu.START_RESURRECTION_BUTTON
                        );
                    }
                }
        ).bounds(this.leftPos + 8, this.topPos + 80, 160, 18).build());
        this.decayZombieButton = addRenderableWidget(Button.builder(
                Component.translatable("gui.requiemunofficial.rift.decay_zombie"),
                button -> clickMenuButton(RiftMenu.START_DECAY_ZOMBIE_BUTTON)
        ).bounds(this.leftPos + 8, this.topPos + 100, 78, 18).build());
        this.decaySkeletonButton = addRenderableWidget(Button.builder(
                Component.translatable("gui.requiemunofficial.rift.decay_skeleton"),
                button -> clickMenuButton(RiftMenu.START_DECAY_SKELETON_BUTTON)
        ).bounds(this.leftPos + 90, this.topPos + 100, 78, 18).build());
        this.teleportButton = addRenderableWidget(Button.builder(
                Component.translatable("gui.requiemunofficial.rift.teleport"),
                button -> clickMenuButton(RiftMenu.TELEPORT_DESTINATION_BUTTON_BASE + selectedDestination)
        ).bounds(this.leftPos + 8, this.topPos + 128, 160, 18).build());
        this.previousDestinationButton = addRenderableWidget(Button.builder(
                Component.literal("<"),
                button -> cycleDestination(-1)
        ).bounds(this.leftPos + 8, this.topPos + 108, 20, 18).build());
        this.nextDestinationButton = addRenderableWidget(Button.builder(
                Component.literal(">"),
                button -> cycleDestination(1)
        ).bounds(this.leftPos + 148, this.topPos + 108, 20, 18).build());
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTicks, int mouseX, int mouseY) {
        RenderSystem.setShaderColor(1F, 1F, 1F, 1F);
        graphics.blit(BACKGROUND, this.leftPos, this.topPos, 0, 0, this.imageWidth, this.imageHeight);
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        graphics.drawString(this.font, this.title, this.titleLabelX, this.titleLabelY, 0x404040, false);
        graphics.drawString(this.font, this.playerInventoryTitle, this.inventoryLabelX, this.inventoryLabelY,
                0x404040, false);

        graphics.drawString(this.font, Component.translatable(
                "gui.requiemunofficial.rift.stability",
                Math.round(this.menu.getPowerRate() * 100.0F)
        ), 8, 22, 0x7A4CC2, false);
        graphics.drawString(this.font, Component.translatable(
                "gui.requiemunofficial.rift.energy",
                this.menu.getStoredSoulEnergy()
        ), 8, 34, 0x505050, false);
        graphics.drawString(this.font, Component.translatable(
                "gui.requiemunofficial.rift.status",
                Component.translatable(this.menu.isRitualActive()
                        ? "gui.requiemunofficial.rift.status.active"
                        : "gui.requiemunofficial.rift.status.idle")
        ), 8, 46, 0x505050, false);
        graphics.drawString(this.font, Component.translatable(
                "gui.requiemunofficial.rift.runes",
                this.menu.getAttritionLevel(),
                this.menu.getEmancipationLevel(),
                this.menu.getPenanceLevel(),
                this.menu.getReclamationLevel(),
                this.menu.getRiftLevel()
                ), 8, 58, 0x505050, false);
        Component destination = selectedDestinationName();
        graphics.drawString(this.font, destination, 32, 113, 0x505050, false);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
        renderBackground(graphics);
        boolean available = this.menu.isStructureValid()
                    && this.menu.isPowered()
                    && !this.menu.isRitualActive()
                    && this.menu.getRiftLevel() > 0;
        if (this.resurrectionButton != null) {
            this.resurrectionButton.active = available && this.menu.getStoredSoulEnergy() >= 5;
            this.decayZombieButton.active = available && this.menu.getStoredSoulEnergy() >= 3;
            this.decaySkeletonButton.active = available && this.menu.getStoredSoulEnergy() >= 3;
            this.teleportButton.active = available
                    && this.menu.getStoredSoulEnergy() >= RequiemConfig.RIFT_TELEPORT_ENERGY_COST.get()
                    && !this.menu.getDestinations().isEmpty();
            this.previousDestinationButton.active = this.menu.getDestinations().size() > 1;
            this.nextDestinationButton.active = this.menu.getDestinations().size() > 1;
        }
        super.render(graphics, mouseX, mouseY, partialTicks);
        renderTooltip(graphics, mouseX, mouseY);
    }

    private Component selectedDestinationName() {
        if (this.menu.getDestinations().isEmpty()) {
            return Component.translatable("gui.requiemunofficial.rift.destination.none");
        }
        if (selectedDestination >= this.menu.getDestinations().size()) {
            selectedDestination = 0;
        }
        return this.menu.getDestinations().get(selectedDestination).resolveName();
    }

    private void cycleDestination(int delta) {
        int size = this.menu.getDestinations().size();
        if (size <= 0) {
            selectedDestination = 0;
            return;
        }
        selectedDestination = Math.floorMod(selectedDestination + delta, size);
    }

    private void clickMenuButton(int buttonId) {
        if (this.minecraft != null && this.minecraft.gameMode != null) {
            this.minecraft.gameMode.handleInventoryButtonClick(this.menu.containerId, buttonId);
        }
    }
}
