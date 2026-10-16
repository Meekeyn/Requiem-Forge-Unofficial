package ua.meekeyn.requiemunofficial.mixin;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.EffectRenderingInventoryScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.level.entity.EntityTypeTest;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import ua.meekeyn.requiemunofficial.common.possession.PossessionInventoryManager;
import ua.meekeyn.requiemunofficial.common.util.ModIds;

import java.util.UUID;

@Mixin(InventoryScreen.class)
public abstract class InventoryScreenMixin extends EffectRenderingInventoryScreen<InventoryMenu> {

    private static final ResourceLocation ALT_INVENTORY =
            ModIds.id("textures/gui/alt_inventory.png");
    private static final ResourceLocation ALT_INVENTORY_SMALL =
            ModIds.id("textures/gui/alt_inventory_small.png");
    private static final ResourceLocation ALT_INVENTORY_LARGE =
            ModIds.id("textures/gui/alt_inventory_large.png");
    private static final ResourceLocation INVENTORY_SLOTS =
            ModIds.id("textures/gui/inventory_slots.png");

    private InventoryScreenMixin(InventoryMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
    }

    @Inject(method = "renderBg", at = @At("HEAD"), cancellable = true)
    private void requiemunofficial$drawPossessionInventoryBackground(GuiGraphics graphics,
                                                                     float partialTick,
                                                                     int mouseX,
                                                                     int mouseY,
                                                                     CallbackInfo ci) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.level == null) {
            return;
        }

        PossessionInventoryManager.InventoryShape shape =
                PossessionInventoryManager.getInventoryShape(minecraft.player);
        ResourceLocation background = switch (shape) {
            case ALT_SMALL -> ALT_INVENTORY_SMALL;
            case ALT -> ALT_INVENTORY;
            case ALT_LARGE -> ALT_INVENTORY_LARGE;
            case NORMAL -> null;
        };
        if (background == null) {
            return;
        }

        graphics.blit(background, this.leftPos, this.topPos, 0, 0, this.imageWidth, this.imageHeight);
        LivingEntity renderedEntity = requiemunofficial$getPossessedHost(minecraft);
        requiemunofficial$drawPossessionSlots(graphics, shape, renderedEntity);
        if (renderedEntity == null) {
            renderedEntity = minecraft.player;
        }
        int entityX = this.leftPos + (shape == PossessionInventoryManager.InventoryShape.ALT_SMALL ? 51 : 91);
        int entityY = this.topPos + (shape == PossessionInventoryManager.InventoryShape.ALT_LARGE ? 150 : 75);
        int size = shape == PossessionInventoryManager.InventoryShape.ALT_LARGE ? 26 : 30;
        InventoryScreen.renderEntityInInventoryFollowsMouse(
                graphics,
                entityX,
                entityY,
                size,
                entityX - mouseX,
                entityY - 50 - mouseY,
                renderedEntity
        );
        ci.cancel();
    }

    private void requiemunofficial$drawPossessionSlots(GuiGraphics graphics,
                                                       PossessionInventoryManager.InventoryShape shape,
                                                       LivingEntity host) {
        if (host == null) {
            return;
        }

        int x = this.leftPos;
        int y = this.topPos;
        if (PossessionInventoryManager.canUseArmor(host)) {
            graphics.blit(INVENTORY_SLOTS, x + 7, y + 7, 7, 7, 18, 72);
        }
        if (PossessionInventoryManager.canUseItems(host)) {
            if (shape == PossessionInventoryManager.InventoryShape.ALT_SMALL) {
                graphics.blit(INVENTORY_SLOTS, x + 76, y + 61, 76, 61, 18, 18);
            } else {
                graphics.blit(INVENTORY_SLOTS, x + 46, y + 61, 46, 61, 48, 18);
            }
        }
        if (PossessionInventoryManager.canCraft(host)) {
            graphics.blit(INVENTORY_SLOTS, x + 97, y + 17, 97, 17, 75, 36);
        }
        if (PossessionInventoryManager.canCarryInventory(host)) {
            graphics.blit(INVENTORY_SLOTS, x + 7, y + 83, 7, 83, 162, 76);
        } else if (PossessionInventoryManager.canCarryHotbar(host)) {
            graphics.blit(INVENTORY_SLOTS, x + 7, y + 141, 7, 141, 162, 18);
        }
    }

    private LivingEntity requiemunofficial$getPossessedHost(Minecraft minecraft) {
        UUID hostId = minecraft.player.getCapability(
                ua.meekeyn.requiemunofficial.common.soul.ModCapabilities.PLAYER_SOUL
        ).map(soul -> soul.getPossessedEntityId()).orElse(null);
        if (hostId == null) {
            return null;
        }
        Entity camera = minecraft.getCameraEntity();
        if (camera instanceof LivingEntity living && hostId.equals(living.getUUID())) {
            return living;
        }
        Entity entity = minecraft.level.getEntities(
                EntityTypeTest.forClass(LivingEntity.class),
                minecraft.player.getBoundingBox().inflate(128.0D),
                candidate -> hostId.equals(candidate.getUUID())
        ).stream().findFirst().orElse(null);
        return entity instanceof LivingEntity living ? living : null;
    }
}
