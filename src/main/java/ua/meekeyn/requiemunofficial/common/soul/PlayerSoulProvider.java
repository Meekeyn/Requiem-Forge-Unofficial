package ua.meekeyn.requiemunofficial.common.soul;

import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.Tag;
import net.minecraftforge.common.capabilities.ICapabilitySerializable;
import net.minecraftforge.common.util.INBTSerializable;
import net.minecraftforge.common.util.LazyOptional;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import ua.meekeyn.requiemunofficial.RequiemUnofficial;

public class PlayerSoulProvider implements ICapabilitySerializable<CompoundTag> {

    public static final ResourceLocation ID =
            new ResourceLocation(RequiemUnofficial.MODID, "player_soul");

    private final PlayerSoul backend = new PlayerSoul();

    private final LazyOptional<IPlayerSoul> optional = LazyOptional.of(() -> backend);

    @Override
    public <T> @NotNull LazyOptional<T> getCapability(@NotNull net.minecraftforge.common.capabilities.Capability<T> cap,
                                                      @Nullable Direction side) {
        if (cap == ModCapabilities.PLAYER_SOUL) {
            return optional.cast();
        }
        return LazyOptional.empty();
    }

    @Override
    public CompoundTag serializeNBT() {
        CompoundTag tag = new CompoundTag();
        tag.putInt("SoulPower", backend.getSoulPower());
        tag.putInt("IchorLevel", backend.getIchorLevel());
        tag.putString("SoulState", backend.getState().name());
        if (backend.getPossessedEntityId() != null) {
            tag.putUUID("PossessedEntity", backend.getPossessedEntityId());
        }
        if (backend.getPlayerShellId() != null) {
            tag.putUUID("PlayerShell", backend.getPlayerShellId());
        }
        if (backend.getResurrectionPos() != null && !backend.getResurrectionDimension().isEmpty()) {
            tag.putString("ResurrectionDimension", backend.getResurrectionDimension());
            tag.putLong("ResurrectionPos", backend.getResurrectionPos().asLong());
        }
        ListTag knownRifts = new ListTag();
        for (java.util.UUID recordId : backend.getKnownRifts()) {
            knownRifts.add(NbtUtils.createUUID(recordId));
        }
        tag.put("KnownRifts", knownRifts);
        return tag;
    }

    @Override
    public void deserializeNBT(CompoundTag nbt) {
        backend.setSoulPower(nbt.getInt("SoulPower"));
        backend.setIchorLevel(nbt.getInt("IchorLevel"));
        backend.setState(SoulState.byName(nbt.getString("SoulState")));
        if (nbt.hasUUID("PossessedEntity")) {
            backend.setPossessedEntityId(nbt.getUUID("PossessedEntity"));
        } else {
            backend.clearPossessedEntityId();
        }
        if (nbt.hasUUID("PlayerShell")) {
            backend.setPlayerShellId(nbt.getUUID("PlayerShell"));
        } else {
            backend.clearPlayerShellId();
        }
        if (nbt.contains("ResurrectionDimension") && nbt.contains("ResurrectionPos")) {
            backend.setResurrectionPoint(
                    nbt.getString("ResurrectionDimension"),
                    net.minecraft.core.BlockPos.of(nbt.getLong("ResurrectionPos"))
            );
        } else {
            backend.clearResurrectionPoint();
        }
        if (nbt.contains("KnownRifts", Tag.TAG_LIST)) {
            for (Tag raw : nbt.getList("KnownRifts", Tag.TAG_INT_ARRAY)) {
                backend.addKnownRift(NbtUtils.loadUUID(raw));
            }
        }
    }
}
