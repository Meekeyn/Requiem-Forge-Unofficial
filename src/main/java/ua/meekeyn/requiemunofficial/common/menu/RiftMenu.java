package ua.meekeyn.requiemunofficial.common.menu;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import ua.meekeyn.requiemunofficial.common.block.RiftRunestoneBlock;
import ua.meekeyn.requiemunofficial.common.block.entity.RunestoneBlockEntity;
import ua.meekeyn.requiemunofficial.common.init.ModMenuTypes;
import ua.meekeyn.requiemunofficial.common.record.ObeliskRecordData;
import ua.meekeyn.requiemunofficial.common.remnant.PlayerRiftTracker;
import ua.meekeyn.requiemunofficial.common.ritual.RitualManager;
import ua.meekeyn.requiemunofficial.common.ritual.RitualType;
import ua.meekeyn.requiemunofficial.common.ritual.RiftTeleportManager;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public class RiftMenu extends AbstractContainerMenu {

    public static final int START_RESURRECTION_BUTTON = 0;
    public static final int START_DECAY_ZOMBIE_BUTTON = 1;
    public static final int START_DECAY_SKELETON_BUTTON = 2;
    public static final int TELEPORT_BUTTON = 3;
    public static final int TELEPORT_DESTINATION_BUTTON_BASE = 100;
    private static final int POWER_RATE = 0;
    private static final int STORED_SOUL_ENERGY = 1;
    private static final int STRUCTURE_VALID = 2;
    private static final int POWERED = 3;
    private static final int ATTRITION_LEVEL = 4;
    private static final int EMANCIPATION_LEVEL = 5;
    private static final int PENANCE_LEVEL = 6;
    private static final int RECLAMATION_LEVEL = 7;
    private static final int RIFT_LEVEL = 8;
    private static final int RITUAL_STATUS = 9;
    private static final int DATA_COUNT = 10;

    private final Level level;
    private final BlockPos riftPos;
    private final ContainerData data;
    private final List<ObeliskRecordData.ObeliskDescriptor> destinations;

    public RiftMenu(int id, Inventory playerInventory, FriendlyByteBuf buffer) {
        this(id, playerInventory, buffer.readBlockPos(), new SimpleContainerData(DATA_COUNT), readDestinations(buffer));
    }

    public RiftMenu(int id, Inventory playerInventory, BlockPos riftPos) {
        this(id, playerInventory, riftPos, createServerData(playerInventory.player.level(), riftPos),
                playerInventory.player instanceof net.minecraft.server.level.ServerPlayer serverPlayer
                        ? new ArrayList<>(PlayerRiftTracker.fetchKnownObelisks(serverPlayer))
                        : List.of());
    }

    private RiftMenu(int id, Inventory playerInventory, BlockPos riftPos, ContainerData data,
                     List<ObeliskRecordData.ObeliskDescriptor> destinations) {
        super(ModMenuTypes.RIFT.get(), id);
        this.level = playerInventory.player.level();
        this.riftPos = riftPos.immutable();
        this.data = data;
        this.destinations = destinations.stream()
                .filter(descriptor -> !descriptor.dimension().equals(this.level.dimension().location())
                        || !descriptor.pos().equals(this.riftPos))
                .toList();
        checkContainerDataCount(data, DATA_COUNT);
        addDataSlots(data);
    }

    @Override
    public boolean stillValid(Player player) {
        RunestoneBlockEntity runestone = getRunestone();
        return runestone != null
                && RiftRunestoneBlock.canUseRift(player, runestone)
                && player.distanceToSqr(
                        riftPos.getX() + 0.5D,
                        riftPos.getY() + 0.5D,
                        riftPos.getZ() + 0.5D
                ) <= 64.0D;
    }

    @Override
    public boolean clickMenuButton(Player player, int buttonId) {
        if (!(player instanceof net.minecraft.server.level.ServerPlayer serverPlayer)
                || !stillValid(player)) {
            return false;
        }

        if (buttonId >= TELEPORT_DESTINATION_BUTTON_BASE) {
            int index = buttonId - TELEPORT_DESTINATION_BUTTON_BASE;
            List<ObeliskRecordData.ObeliskDescriptor> known =
                    PlayerRiftTracker.fetchKnownObelisks(serverPlayer).stream()
                            .filter(descriptor -> !descriptor.dimension().equals(this.level.dimension().location())
                                    || !descriptor.pos().equals(this.riftPos))
                            .toList();
            if (index < 0 || index >= known.size()) {
                return false;
            }
            return RiftTeleportManager.teleport(serverPlayer, riftPos, known.get(index).recordId());
        }

        return switch (buttonId) {
            case START_RESURRECTION_BUTTON ->
                    RitualManager.tryStart(serverPlayer, riftPos, RitualType.RESURRECTION);
            case START_DECAY_ZOMBIE_BUTTON ->
                    RitualManager.tryStart(serverPlayer, riftPos, RitualType.DECAY_ZOMBIE);
            case START_DECAY_SKELETON_BUTTON ->
                    RitualManager.tryStart(serverPlayer, riftPos, RitualType.DECAY_SKELETON);
            case TELEPORT_BUTTON -> RiftTeleportManager.teleport(serverPlayer, riftPos);
            default -> false;
        };
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        return ItemStack.EMPTY;
    }

    public int getStoredSoulEnergy() {
        return data.get(STORED_SOUL_ENERGY);
    }

    public float getPowerRate() {
        return data.get(POWER_RATE) / 1000.0F;
    }

    public boolean isStructureValid() {
        return data.get(STRUCTURE_VALID) != 0;
    }

    public boolean isPowered() {
        return data.get(POWERED) != 0;
    }

    public boolean isRitualActive() {
        return data.get(RITUAL_STATUS) != 0;
    }

    public int getAttritionLevel() {
        return data.get(ATTRITION_LEVEL);
    }

    public int getEmancipationLevel() {
        return data.get(EMANCIPATION_LEVEL);
    }

    public int getPenanceLevel() {
        return data.get(PENANCE_LEVEL);
    }

    public int getReclamationLevel() {
        return data.get(RECLAMATION_LEVEL);
    }

    public int getRiftLevel() {
        return data.get(RIFT_LEVEL);
    }

    public List<ObeliskRecordData.ObeliskDescriptor> getDestinations() {
        return destinations;
    }

    public static void writeOpeningData(FriendlyByteBuf buffer, BlockPos sourcePos, ServerPlayer player) {
        buffer.writeBlockPos(sourcePos);
        List<ObeliskRecordData.ObeliskDescriptor> known =
                new ArrayList<>(PlayerRiftTracker.fetchKnownObelisks(player));
        buffer.writeVarInt(known.size());
        for (ObeliskRecordData.ObeliskDescriptor descriptor : known) {
            writeDescriptor(buffer, descriptor);
        }
    }

    private static List<ObeliskRecordData.ObeliskDescriptor> readDestinations(FriendlyByteBuf buffer) {
        int size = buffer.readVarInt();
        List<ObeliskRecordData.ObeliskDescriptor> descriptors = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            descriptors.add(readDescriptor(buffer));
        }
        return descriptors;
    }

    private static void writeDescriptor(FriendlyByteBuf buffer, ObeliskRecordData.ObeliskDescriptor descriptor) {
        buffer.writeUUID(descriptor.recordId());
        buffer.writeResourceLocation(descriptor.dimension());
        buffer.writeBlockPos(descriptor.pos());
        buffer.writeVarInt(descriptor.width());
        buffer.writeVarInt(descriptor.height());
        buffer.writeBoolean(descriptor.name().isPresent());
        descriptor.name().ifPresent(buffer::writeComponent);
    }

    private static ObeliskRecordData.ObeliskDescriptor readDescriptor(FriendlyByteBuf buffer) {
        UUID recordId = buffer.readUUID();
        ResourceLocation dimension = buffer.readResourceLocation();
        BlockPos pos = buffer.readBlockPos();
        int width = buffer.readVarInt();
        int height = buffer.readVarInt();
        Optional<Component> name = buffer.readBoolean()
                ? Optional.of(buffer.readComponent())
                : Optional.empty();
        return new ObeliskRecordData.ObeliskDescriptor(recordId, dimension, pos, width, height, name);
    }

    private RunestoneBlockEntity getRunestone() {
        if (level.getBlockEntity(riftPos) instanceof RunestoneBlockEntity runestone) {
            return runestone;
        }
        return null;
    }

    private static ContainerData createServerData(Level level, BlockPos pos) {
        return new ContainerData() {
            @Override
            public int get(int index) {
                if (!(level.getBlockEntity(pos) instanceof RunestoneBlockEntity runestone)) {
                    return 0;
                }

                return switch (index) {
                    case POWER_RATE -> Math.round(runestone.getPowerRate() * 1000.0F);
                    case STORED_SOUL_ENERGY -> runestone.getStoredSoulEnergy();
                    case STRUCTURE_VALID -> runestone.isStructureValid() ? 1 : 0;
                    case POWERED -> runestone.isPowered() ? 1 : 0;
                    case ATTRITION_LEVEL -> runestone.getRuneLevel("attrition");
                    case EMANCIPATION_LEVEL -> runestone.getRuneLevel("emancipation");
                    case PENANCE_LEVEL -> runestone.getRuneLevel("penance");
                    case RECLAMATION_LEVEL -> runestone.getRuneLevel("reclamation");
                    case RIFT_LEVEL -> runestone.getRuneLevel("rift");
                    case RITUAL_STATUS -> runestone.hasPendingRitual() ? 1 : 0;
                    default -> 0;
                };
            }

            @Override
            public void set(int index, int value) {
            }

            @Override
            public int getCount() {
                return DATA_COUNT;
            }
        };
    }
}
