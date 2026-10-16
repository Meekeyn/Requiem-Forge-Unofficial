package ua.meekeyn.requiemunofficial.common.soul;

import java.util.UUID;
import java.util.Set;
import net.minecraft.core.BlockPos;

public interface IPlayerSoul {

    int getSoulPower();

    void setSoulPower(int value);

    void addSoulPower(int value);

    int getIchorLevel();

    void setIchorLevel(int value);

    void addIchorLevel(int value);

    SoulState getState();

    void setState(SoulState state);

    UUID getPossessedEntityId();

    void setPossessedEntityId(UUID entityId);

    void clearPossessedEntityId();

    UUID getPlayerShellId();

    void setPlayerShellId(UUID shellId);

    void clearPlayerShellId();

    String getResurrectionDimension();

    BlockPos getResurrectionPos();

    void setResurrectionPoint(String dimension, BlockPos pos);

    void clearResurrectionPoint();

    boolean addKnownRift(UUID recordId);

    Set<UUID> getKnownRifts();

    void removeKnownRift(UUID recordId);

    void copyFrom(IPlayerSoul other);
}
