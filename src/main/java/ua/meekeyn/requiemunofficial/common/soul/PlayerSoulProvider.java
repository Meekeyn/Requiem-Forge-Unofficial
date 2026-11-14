package ua.meekeyn.requiemunofficial.common.soul;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.Tag;
import net.neoforged.neoforge.attachment.IAttachmentHolder;
import net.neoforged.neoforge.attachment.IAttachmentSerializer;

public class PlayerSoulProvider implements IAttachmentSerializer<CompoundTag, PlayerSoul> {

    @Override
    public CompoundTag write(PlayerSoul backend, HolderLookup.Provider provider) {
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
    public PlayerSoul read(IAttachmentHolder holder, CompoundTag nbt, HolderLookup.Provider provider) {
        PlayerSoul backend = new PlayerSoul();
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
        return backend;
    }
}
