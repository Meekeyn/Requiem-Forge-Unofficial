package ua.meekeyn.requiemunofficial.common.soul;

import java.util.UUID;
import java.util.LinkedHashSet;
import java.util.Set;
import net.minecraft.core.BlockPos;

public class PlayerSoul implements IPlayerSoul {

    private int soulPower;
    private int ichorLevel;
    private SoulState state = SoulState.MORTAL;
    private UUID possessedEntityId;
    private UUID playerShellId;
    private String resurrectionDimension = "";
    private BlockPos resurrectionPos;
    private final Set<UUID> knownRifts = new LinkedHashSet<>();

    @Override
    public int getSoulPower() {
        return soulPower;
    }

    @Override
    public void setSoulPower(int value) {
        this.soulPower = value;
    }

    @Override
    public void addSoulPower(int value) {
        this.soulPower += value;
    }

    @Override
    public int getIchorLevel() {
        return ichorLevel;
    }

    @Override
    public void setIchorLevel(int value) {
        this.ichorLevel = value;
    }

    @Override
    public void addIchorLevel(int value) {
        this.ichorLevel += value;
    }

    @Override
    public SoulState getState() {
        return state;
    }

    @Override
    public void setState(SoulState state) {
        this.state = state == null ? SoulState.MORTAL : state;
    }

    @Override
    public UUID getPossessedEntityId() {
        return possessedEntityId;
    }

    @Override
    public void setPossessedEntityId(UUID entityId) {
        this.possessedEntityId = entityId;
    }

    @Override
    public void clearPossessedEntityId() {
        this.possessedEntityId = null;
    }

    @Override
    public UUID getPlayerShellId() {
        return playerShellId;
    }

    @Override
    public void setPlayerShellId(UUID shellId) {
        this.playerShellId = shellId;
    }

    @Override
    public void clearPlayerShellId() {
        this.playerShellId = null;
    }

    @Override
    public String getResurrectionDimension() {
        return resurrectionDimension;
    }

    @Override
    public BlockPos getResurrectionPos() {
        return resurrectionPos;
    }

    @Override
    public void setResurrectionPoint(String dimension, BlockPos pos) {
        this.resurrectionDimension = dimension == null ? "" : dimension;
        this.resurrectionPos = pos == null ? null : pos.immutable();
    }

    @Override
    public void clearResurrectionPoint() {
        this.resurrectionDimension = "";
        this.resurrectionPos = null;
    }

    @Override
    public boolean addKnownRift(UUID recordId) {
        return recordId != null && knownRifts.add(recordId);
    }

    @Override
    public Set<UUID> getKnownRifts() {
        return Set.copyOf(knownRifts);
    }

    @Override
    public void removeKnownRift(UUID recordId) {
        knownRifts.remove(recordId);
    }

    @Override
    public void copyFrom(IPlayerSoul other) {
        this.soulPower = other.getSoulPower();
        this.ichorLevel = other.getIchorLevel();
        this.state = other.getState();
        this.possessedEntityId = other.getPossessedEntityId();
        this.playerShellId = other.getPlayerShellId();
        setResurrectionPoint(other.getResurrectionDimension(), other.getResurrectionPos());
        this.knownRifts.clear();
        this.knownRifts.addAll(other.getKnownRifts());
    }
}
