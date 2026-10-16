package ua.meekeyn.requiemunofficial.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.shapes.BooleanOp;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraftforge.client.gui.overlay.VanillaGuiOverlay;
import net.minecraftforge.client.event.RenderGuiOverlayEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.api.distmarker.Dist;
import ua.meekeyn.requiemunofficial.RequiemUnofficial;
import ua.meekeyn.requiemunofficial.common.soul.SoulManager;
import ua.meekeyn.requiemunofficial.common.soul.SoulState;
import ua.meekeyn.requiemunofficial.common.soul.VagrantRules;

@Mod.EventBusSubscriber(modid = RequiemUnofficial.MODID, value = Dist.CLIENT)
public final class HudOverlay {
    private static final ResourceLocation GUI_ICONS =
            new ResourceLocation("minecraft", "textures/gui/icons.png");
    private static final ResourceLocation WIDGETS =
            new ResourceLocation("minecraft", "textures/gui/widgets.png");
    private static final ResourceLocation POSSESSION_ICON =
            new ResourceLocation(RequiemUnofficial.MODID, "textures/gui/possession_icon.png");
    private static final int HEART_EMPTY_U = 16;
    private static final int HEART_FULL_U = 52;
    private static final int HEART_HALF_U = 61;
    private static final int HEART_V = 0;
    private static final int POSSESSION_ICON_SIZE = 16;

    private HudOverlay() {
    }

    @SubscribeEvent
    public static void onRenderGuiOverlayPre(RenderGuiOverlayEvent.Pre event) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null) {
            return;
        }

        if (isFreeVagrant(minecraft) && isBodyStatusOverlay(event)
                || isPossessing(minecraft) && event.getOverlay() == VanillaGuiOverlay.PLAYER_HEALTH.type()) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onRenderGuiOverlay(RenderGuiOverlayEvent.Post event) {
        Minecraft minecraft = Minecraft.getInstance();

        if (minecraft.player == null) {
            return;
        }

        GuiGraphics guiGraphics = event.getGuiGraphics();
        if (event.getOverlay() == VanillaGuiOverlay.HOTBAR.type()) {
            renderVagrantBlockOcclusion(minecraft, guiGraphics);
        }

        if (minecraft.options.hideGui) {
            return;
        }

        if (event.getOverlay() == VanillaGuiOverlay.CROSSHAIR.type()
                && isFreeVagrant(minecraft)
                && ClientPossessionTargetHint.canPossessTarget()) {
            renderPossessionTargetIcon(minecraft, guiGraphics);
        }
        if (event.getOverlay() == VanillaGuiOverlay.HOTBAR.type()
                && isPossessing(minecraft)
                && minecraft.getCameraEntity() != minecraft.player) {
            renderPossessionHotbar(minecraft, guiGraphics);
        }

        if (!Boolean.getBoolean("requiemunofficial.debugHud")) {
            LivingEntity possessedHost = ClientPossessionRenderHelper.getPossessedHost(minecraft.player);
            if (possessedHost != null && !minecraft.player.isCreative()) {
                renderBodyHealth(event, minecraft, displayedBodyHealthEntity(possessedHost));
            }
            return;
        }

        int soulPower = SoulManager.getSoulPower(minecraft.player);
        int ichorLevel = SoulManager.getIchorLevel(minecraft.player);
        String soulState = SoulManager.getState(minecraft.player).name().toLowerCase();
        boolean resurrectionBound = SoulManager.getResurrectionPos(minecraft.player) != null;

        int x = 10;
        int y = 10;

        // Відображає поточну силу душі
        guiGraphics.drawString(
                minecraft.font,
                "Soul: " + soulPower,
                x,
                y,
                0xFFFFFF,
                false
        );

        // Відображає поточний рівень іхору
        guiGraphics.drawString(
                minecraft.font,
                "Ichor: " + ichorLevel,
                x,
                y + 10,
                0xFFAA55,
                false
        );

        guiGraphics.drawString(
                minecraft.font,
                "State: " + soulState,
                x,
                y + 20,
                0xB48CFF,
                false
        );

        guiGraphics.drawString(
                minecraft.font,
                "Rift bound: " + (resurrectionBound ? "yes" : "no"),
                x,
                y + 30,
                resurrectionBound ? 0x7A4CC2 : 0x777777,
                false
        );

        LivingEntity possessedHost = ClientPossessionRenderHelper.getPossessedHost(minecraft.player);
        if (possessedHost != null && !minecraft.player.isCreative()) {
            renderBodyHealth(event, minecraft, displayedBodyHealthEntity(possessedHost));
        }
    }

    private static void renderVagrantBlockOcclusion(Minecraft minecraft, GuiGraphics guiGraphics) {
        if (minecraft.level == null
                || !VagrantRules.isRestricted(minecraft.player)
                || !isVagrantEyeInsideSolidBlock(minecraft)) {
            return;
        }

        BlockPos pos = BlockPos.containing(minecraft.player.getEyePosition());
        BlockState state = minecraft.level.getBlockState(pos);
        if (VagrantRules.isLeaves(state)) {
            return;
        }
        int color = state.isAir()
                ? 0x202020
                : state.getMapColor(minecraft.level, pos).calculateRGBColor(MapColor.Brightness.NORMAL);
        guiGraphics.fill(
                0,
                0,
                minecraft.getWindow().getGuiScaledWidth(),
                minecraft.getWindow().getGuiScaledHeight(),
                0xCC000000 | (color & 0x00FFFFFF)
        );
    }

    private static boolean isVagrantEyeInsideSolidBlock(Minecraft minecraft) {
        BlockPos pos = BlockPos.containing(minecraft.player.getEyePosition());
        BlockState state = minecraft.level.getBlockState(pos);
        if (state.isAir() || VagrantRules.isLeaves(state) || !VagrantRules.canPassThrough(state)) {
            return false;
        }

        AABB eyeBox = new AABB(minecraft.player.getEyePosition(), minecraft.player.getEyePosition())
                .inflate(0.08D);
        return Shapes.joinIsNotEmpty(
                state.getCollisionShape(minecraft.level, pos).move(pos.getX(), pos.getY(), pos.getZ()),
                Shapes.create(eyeBox),
                BooleanOp.AND
        );
    }

    private static LivingEntity displayedBodyHealthEntity(LivingEntity possessedHost) {
        return possessedHost.getVehicle() instanceof LivingEntity vehicle ? vehicle : possessedHost;
    }

    private static void renderPossessionTargetIcon(Minecraft minecraft, GuiGraphics guiGraphics) {
        int centerX = minecraft.getWindow().getGuiScaledWidth() / 2;
        int centerY = minecraft.getWindow().getGuiScaledHeight() / 2;
        guiGraphics.blit(
                POSSESSION_ICON,
                centerX + 10,
                centerY - POSSESSION_ICON_SIZE / 2,
                0,
                0,
                POSSESSION_ICON_SIZE,
                POSSESSION_ICON_SIZE,
                POSSESSION_ICON_SIZE,
                POSSESSION_ICON_SIZE
        );
    }

    private static void renderPossessionHotbar(Minecraft minecraft, GuiGraphics guiGraphics) {
        int left = minecraft.getWindow().getGuiScaledWidth() / 2 - 91;
        int top = minecraft.getWindow().getGuiScaledHeight() - 22;
        int selected = minecraft.player.getInventory().selected;

        guiGraphics.blit(WIDGETS, left, top, 0, 0, 182, 22);
        guiGraphics.blit(WIDGETS, left - 1 + selected * 20, top - 1, 0, 22, 24, 22);

        for (int slot = 0; slot < 9; slot++) {
            ItemStack stack = minecraft.player.getInventory().items.get(slot);
            int x = left + 3 + slot * 20;
            int y = top + 3;
            guiGraphics.renderItem(stack, x, y);
            guiGraphics.renderItemDecorations(minecraft.font, stack, x, y);
        }
    }

    private static void renderBodyHealth(RenderGuiOverlayEvent.Post event,
                                         Minecraft minecraft,
                                         LivingEntity possessedHost) {
        if (event.getOverlay() != VanillaGuiOverlay.HOTBAR.type()) {
            return;
        }

        GuiGraphics guiGraphics = event.getGuiGraphics();
        int health = Mth.ceil(possessedHost.getHealth());
        int maxHealth = Math.max(1, Mth.ceil(possessedHost.getMaxHealth()));
        int left = minecraft.getWindow().getGuiScaledWidth() / 2 - 91;
        int top = minecraft.getWindow().getGuiScaledHeight() - 39;
        int hearts = Mth.ceil(maxHealth / 2.0F);

        for (int i = 0; i < hearts; i++) {
            int row = i / 10;
            int col = i % 10;
            int slotX = left + col * 8;
            int slotY = top - row * 10;
            int bodyHealth = health - i * 2;
            guiGraphics.blit(GUI_ICONS, slotX, slotY, HEART_EMPTY_U, HEART_V, 9, 9);
            if (bodyHealth >= 2) {
                guiGraphics.blit(GUI_ICONS, slotX, slotY, HEART_FULL_U, HEART_V, 9, 9);
            } else if (bodyHealth == 1) {
                guiGraphics.blit(GUI_ICONS, slotX, slotY, HEART_HALF_U, HEART_V, 9, 9);
            }
        }

    }

    private static boolean isBodyStatusOverlay(RenderGuiOverlayEvent.Pre event) {
        return event.getOverlay() == VanillaGuiOverlay.PLAYER_HEALTH.type()
                || event.getOverlay() == VanillaGuiOverlay.ARMOR_LEVEL.type()
                || event.getOverlay() == VanillaGuiOverlay.FOOD_LEVEL.type()
                || event.getOverlay() == VanillaGuiOverlay.AIR_LEVEL.type()
                || event.getOverlay() == VanillaGuiOverlay.MOUNT_HEALTH.type();
    }

    private static boolean isFreeVagrant(Minecraft minecraft) {
        return SoulManager.getState(minecraft.player) == SoulState.VAGRANT
                && SoulManager.getPossessedEntityId(minecraft.player) == null;
    }

    private static boolean isPossessing(Minecraft minecraft) {
        return SoulManager.getPossessedEntityId(minecraft.player) != null;
    }
}
