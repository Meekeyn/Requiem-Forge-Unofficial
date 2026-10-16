package ua.meekeyn.requiemunofficial.common.block.obelisk;

import net.minecraft.core.BlockPos;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public final class ObeliskMatch {

    private final BlockPos origin;
    private final int coreWidth;
    private final int coreHeight;
    private final Map<String, Integer> runeLevels;
    private final Map<String, BlockPos> runeSamplePositions;

    public ObeliskMatch(BlockPos origin, int coreWidth, int coreHeight,
                        Map<String, Integer> runeLevels,
                        Map<String, BlockPos> runeSamplePositions) {
        this.origin = origin.immutable();
        this.coreWidth = coreWidth;
        this.coreHeight = coreHeight;
        this.runeLevels = Collections.unmodifiableMap(new HashMap<>(runeLevels));

        Map<String, BlockPos> copiedPositions = new HashMap<>();
        for (Map.Entry<String, BlockPos> entry : runeSamplePositions.entrySet()) {
            copiedPositions.put(entry.getKey(), entry.getValue().immutable());
        }
        this.runeSamplePositions = Collections.unmodifiableMap(copiedPositions);
    }

    public BlockPos origin() {
        return origin;
    }

    public int coreWidth() {
        return coreWidth;
    }

    public int coreHeight() {
        return coreHeight;
    }

    public Map<String, Integer> runeLevels() {
        return runeLevels;
    }

    public int getRuneLevel(String runeType) {
        return runeLevels.getOrDefault(runeType, 0);
    }

    public Map<String, BlockPos> runeSamplePositions() {
        return runeSamplePositions;
    }

    public BlockPos getRuneSamplePosition(String runeType) {
        return runeSamplePositions.get(runeType);
    }
}