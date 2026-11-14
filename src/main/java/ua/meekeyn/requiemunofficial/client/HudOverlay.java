package ua.meekeyn.requiemunofficial.client;

import net.neoforged.fml.common.EventBusSubscriber;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.shapes.BooleanOp;
import net.minecraft.world.phys.shapes.Shapes;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;
import net.neoforged.neoforge.client.event.RenderGuiLayerEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;
import net.neoforged.api.distmarker.Dist;
import ua.meekeyn.requiemunofficial.RequiemUnofficial;
import ua.meekeyn.requiemunofficial.common.possession.PossessionInventoryManager;
import ua.meekeyn.requiemunofficial.common.soul.SoulManager;
import ua.meekeyn.requiemunofficial.common.soul.SoulState;
import ua.meekeyn.requiemunofficial.common.soul.VagrantRules;
import ua.meekeyn.requiemunofficial.common.util.ModTags;

@EventBusSubscriber(modid = RequiemUnofficial.MODID, value = Dist.CLIENT)
public final class HudOverlay {
    // У 1.21 icons.png і widgets.png прибрані: HUD малюється спрайтами з атласу gui.
    private static final ResourceLocation HOTBAR_SPRITE = ResourceLocation.withDefaultNamespace("hud/hotbar");
    private static final ResourceLocation HOTBAR_SELECTION_SPRITE =
            ResourceLocation.withDefaultNamespace("hud/hotbar_selection");
    private static final ResourceLocation HOTBAR_OFFHAND_LEFT_SPRITE =
            ResourceLocation.withDefaultNamespace("hud/hotbar_offhand_left");
    private static final ResourceLocation HEART_CONTAINER_SPRITE =
            ResourceLocation.withDefaultNamespace("hud/heart/container");
    private static final ResourceLocation HEART_FULL_SPRITE = ResourceLocation.withDefaultNamespace("hud/heart/full");
    private static final ResourceLocation HEART_HALF_SPRITE = ResourceLocation.withDefaultNamespace("hud/heart/half");
    private static final ResourceLocation POSSESSION_ICON =
            ResourceLocation.fromNamespaceAndPath(RequiemUnofficial.MODID, "textures/gui/possession_icon.png");
    private static final int POSSESSION_ICON_SIZE = 16;

    private HudOverlay() {
    }

    @SubscribeEvent
    public static void onRenderGuiOverlayPre(RenderGuiLayerEvent.Pre event) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null) {
            return;
        }

        if (isFreeVagrant(minecraft) && isBodyStatusOverlay(event)
                || isPossessing(minecraft) && isHiddenPossessedOverlay(minecraft, event)) {
            event.setCanceled(true);
            return;
        }
        if (event.getName().equals(VanillaGuiLayers.HOTBAR) && hasLimitedHotbar(minecraft)) {
            // Ванільний хотбар показав би заблоковані слоти; малюємо власний, а Post-логіку
            // хотбара (серця тіла тощо) викликаємо вручну, бо скасований оверлей Post не отримує.
            event.setCanceled(true);
            onRenderGuiOverlay(new RenderGuiLayerEvent.Post(
                    event.getGuiGraphics(), event.getPartialTick(), event.getName(), event.getLayer()));
        }
    }

    private static boolean hasLimitedHotbar(Minecraft minecraft) {
        if (!isPossessing(minecraft) || minecraft.player.isCreative() || minecraft.player.isSpectator()) {
            return false;
        }
        LivingEntity host = ClientPossessionRenderHelper.getPossessedHost(minecraft.player);
        return host != null && !PossessionInventoryManager.canCarryHotbar(host);
    }

    private static boolean isHiddenPossessedOverlay(Minecraft minecraft, RenderGuiLayerEvent.Pre event) {
        // Здоров'я тіла малюється окремо в renderBodyHealth, тож ванільний рядок сердець зайвий.
        if (event.getName().equals(VanillaGuiLayers.PLAYER_HEALTH)) {
            return true;
        }
        // Смуга голоду лишається тільки для regular eater: лише їхній голод справжній.
        if (!event.getName().equals(VanillaGuiLayers.FOOD_LEVEL)) {
            return false;
        }
        LivingEntity host = ClientPossessionRenderHelper.getPossessedHost(minecraft.player);
        return host == null || !host.getType().is(ModTags.REGULAR_EATERS);
    }

    @SubscribeEvent
    public static void onRenderGuiOverlay(RenderGuiLayerEvent.Post event) {
        Minecraft minecraft = Minecraft.getInstance();

        if (minecraft.player == null) {
            return;
        }

        GuiGraphics guiGraphics = event.getGuiGraphics();
        if (event.getName().equals(VanillaGuiLayers.HOTBAR)) {
            renderVagrantBlockOcclusion(minecraft, guiGraphics);
        }

        if (minecraft.options.hideGui) {
            return;
        }

        if (event.getName().equals(VanillaGuiLayers.CROSSHAIR)
                && isFreeVagrant(minecraft)
                && ClientPossessionTargetHint.canPossessTarget()) {
            renderPossessionTargetIcon(minecraft, guiGraphics);
        }
        if (event.getName().equals(VanillaGuiLayers.HOTBAR)
                && isPossessing(minecraft)
                && (minecraft.getCameraEntity() != minecraft.player || hasLimitedHotbar(minecraft))) {
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

        if (hasLimitedHotbar(minecraft)) {
            renderLimitedPossessionHotbar(minecraft, guiGraphics, top);
            return;
        }

        guiGraphics.blitSprite(HOTBAR_SPRITE, left, top, 182, 22);
        guiGraphics.blitSprite(HOTBAR_SELECTION_SPRITE, left - 1 + selected * 20, top - 1, 24, 23);

        for (int slot = 0; slot < 9; slot++) {
            ItemStack stack = minecraft.player.getInventory().items.get(slot);
            int x = left + 3 + slot * 20;
            int y = top + 3;
            guiGraphics.renderItem(stack, x, y);
            guiGraphics.renderItemDecorations(minecraft.font, stack, x, y);
        }
    }

    /** Тіло без хотбара: лише слот 0 (якщо є руки) і друга рука, як у beta.16. */
    private static void renderLimitedPossessionHotbar(Minecraft minecraft, GuiGraphics guiGraphics, int top) {
        LivingEntity host = ClientPossessionRenderHelper.getPossessedHost(minecraft.player);
        if (host == null || !PossessionInventoryManager.canUseItems(host)) {
            return;
        }
        int left = minecraft.getWindow().getGuiScaledWidth() / 2 - 11;
        guiGraphics.blitSprite(HOTBAR_SPRITE, 182, 22, 0, 0, left, top, 21, 22);
        guiGraphics.blitSprite(HOTBAR_SPRITE, 182, 22, 181, 0, left + 21, top, 1, 22);
        guiGraphics.blitSprite(HOTBAR_SELECTION_SPRITE, left - 1, top - 1, 24, 23);
        ItemStack mainHand = minecraft.player.getInventory().items.get(0);
        guiGraphics.renderItem(mainHand, left + 3, top + 3);
        guiGraphics.renderItemDecorations(minecraft.font, mainHand, left + 3, top + 3);

        ItemStack offHand = minecraft.player.getOffhandItem();
        if (!offHand.isEmpty() && PossessionInventoryManager.canUseEquipmentSlot(host, EquipmentSlot.OFFHAND)) {
            guiGraphics.blitSprite(HOTBAR_OFFHAND_LEFT_SPRITE, left - 29, top - 1, 29, 24);
            guiGraphics.renderItem(offHand, left - 26, top + 3);
            guiGraphics.renderItemDecorations(minecraft.font, offHand, left - 26, top + 3);
        }
    }

    private static void renderBodyHealth(RenderGuiLayerEvent.Post event,
                                         Minecraft minecraft,
                                         LivingEntity possessedHost) {
        if (!event.getName().equals(VanillaGuiLayers.HOTBAR)) {
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
            guiGraphics.blitSprite(HEART_CONTAINER_SPRITE, slotX, slotY, 9, 9);
            if (bodyHealth >= 2) {
                guiGraphics.blitSprite(HEART_FULL_SPRITE, slotX, slotY, 9, 9);
            } else if (bodyHealth == 1) {
                guiGraphics.blitSprite(HEART_HALF_SPRITE, slotX, slotY, 9, 9);
            }
        }

    }

    private static boolean isBodyStatusOverlay(RenderGuiLayerEvent.Pre event) {
        return event.getName().equals(VanillaGuiLayers.PLAYER_HEALTH)
                || event.getName().equals(VanillaGuiLayers.ARMOR_LEVEL)
                || event.getName().equals(VanillaGuiLayers.FOOD_LEVEL)
                || event.getName().equals(VanillaGuiLayers.AIR_LEVEL)
                || event.getName().equals(VanillaGuiLayers.VEHICLE_HEALTH);
    }

    private static boolean isFreeVagrant(Minecraft minecraft) {
        return SoulManager.getState(minecraft.player) == SoulState.VAGRANT
                && SoulManager.getPossessedEntityId(minecraft.player) == null;
    }

    private static boolean isPossessing(Minecraft minecraft) {
        return SoulManager.getPossessedEntityId(minecraft.player) != null;
    }
}
