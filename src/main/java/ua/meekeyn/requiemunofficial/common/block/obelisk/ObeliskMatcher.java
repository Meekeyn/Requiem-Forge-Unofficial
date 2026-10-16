package ua.meekeyn.requiemunofficial.common.block.obelisk;

import net.minecraft.core.BlockPos;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import ua.meekeyn.requiemunofficial.common.block.RunestoneBlock;
import ua.meekeyn.requiemunofficial.common.util.ModIds;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public final class ObeliskMatcher {

    public static final int MAX_OBELISK_CORE_WIDTH = 5;
    public static final int MAX_OBELISK_CORE_HEIGHT = 20;

    private static final TagKey<Block> OBELISK_CORE =
            BlockTags.create(ModIds.id("obelisk/core"));

    private static final TagKey<Block> OBELISK_FRAME =
            BlockTags.create(ModIds.id("obelisk/frame"));

    private ObeliskMatcher() {
    }

    public static Optional<BlockPos> findObeliskOrigin(Level level, BlockPos pos) {
        if (!isCore(level.getBlockState(pos))) {
            return Optional.empty();
        }

        BlockPos current = pos.immutable();
        boolean moved;

        do {
            moved = false;

            while (isCore(level.getBlockState(current.west()))) {
                current = current.west();
                moved = true;
            }

            while (isCore(level.getBlockState(current.north()))) {
                current = current.north();
                moved = true;
            }

            while (isCore(level.getBlockState(current.below()))) {
                current = current.below();
                moved = true;
            }
        } while (moved);

        return Optional.of(current);
    }

    public static Optional<ObeliskMatch> matchObelisk(Level level, BlockPos origin) {
        if (!isCore(level.getBlockState(origin))) {
            return Optional.empty();
        }

        int coreWidth = getCoreWidth(level, origin);
        if (coreWidth <= 0) {
            return Optional.empty();
        }

        if (!matchObeliskBase(level, origin, coreWidth)) {
            return Optional.empty();
        }

        int coreHeight = getCoreHeight(level, origin, coreWidth);
        if (coreHeight <= 0) {
            return Optional.empty();
        }

        if (!matchObeliskCap(level, origin, coreWidth, coreHeight)) {
            return Optional.empty();
        }

        CollectedRunes collectedRunes = collectRuneData(level, origin, coreWidth, coreHeight);

        return Optional.of(new ObeliskMatch(
                origin,
                coreWidth,
                coreHeight,
                collectedRunes.runeLevels,
                collectedRunes.runeSamplePositions
        ));
    }

    private static int getCoreWidth(Level level, BlockPos origin) {
        int width = 1;

        while (width < MAX_OBELISK_CORE_WIDTH && isCore(level.getBlockState(origin.south(width)))) {
            width++;
        }

        if (!isValidCoreSquare(level, origin, width, 0)) {
            return 0;
        }

        return width;
    }

    private static int getCoreHeight(Level level, BlockPos origin, int coreWidth) {
        int height = 0;

        while (height < MAX_OBELISK_CORE_HEIGHT) {
            if (!isValidCoreLayer(level, origin, coreWidth, height)) {
                break;
            }
            height++;
        }

        return height;
    }

    private static boolean isValidCoreSquare(Level level, BlockPos origin, int coreWidth, int yOffset) {
        for (int x = 0; x < coreWidth; x++) {
            for (int z = 0; z < coreWidth; z++) {
                BlockPos checkPos = origin.offset(x, yOffset, z);
                if (!isCore(level.getBlockState(checkPos))) {
                    return false;
                }
            }
        }

        return true;
    }

    private static boolean isValidCoreLayer(Level level, BlockPos origin, int coreWidth, int yOffset) {
        if (!isValidCoreSquare(level, origin, coreWidth, yOffset)) {
            return false;
        }

        BlockPos frameStart = origin.offset(-1, yOffset, -1);
        int frameWidth = coreWidth + 2;

        return checkFrameCorners(level, frameStart, frameWidth);
    }

    private static boolean matchObeliskBase(Level level, BlockPos origin, int coreWidth) {
        BlockPos frameStart = origin.offset(-1, -1, -1);
        int frameWidth = coreWidth + 2;
        return checkFrameRing(level, frameStart, frameWidth);
    }

    private static boolean matchObeliskCap(Level level, BlockPos origin, int coreWidth, int coreHeight) {
        BlockPos frameStart = origin.offset(-1, coreHeight, -1);
        int frameWidth = coreWidth + 2;
        return checkFrameRing(level, frameStart, frameWidth);
    }

    private static boolean checkFrameRing(Level level, BlockPos start, int width) {
        for (int x = 0; x < width; x++) {
            if (!isFrame(level.getBlockState(start.offset(x, 0, 0)))) {
                return false;
            }
            if (!isFrame(level.getBlockState(start.offset(x, 0, width - 1)))) {
                return false;
            }
        }

        for (int z = 0; z < width; z++) {
            if (!isFrame(level.getBlockState(start.offset(0, 0, z)))) {
                return false;
            }
            if (!isFrame(level.getBlockState(start.offset(width - 1, 0, z)))) {
                return false;
            }
        }

        return true;
    }

    private static boolean checkFrameCorners(Level level, BlockPos start, int width) {
        return isFrame(level.getBlockState(start))
                && isFrame(level.getBlockState(start.offset(width - 1, 0, 0)))
                && isFrame(level.getBlockState(start.offset(0, 0, width - 1)))
                && isFrame(level.getBlockState(start.offset(width - 1, 0, width - 1)));
    }

    private static CollectedRunes collectRuneData(Level level, BlockPos origin, int coreWidth, int coreHeight) {
        Map<String, Integer> runeLevels = new HashMap<>();
        Map<String, BlockPos> runeSamplePositions = new HashMap<>();

        for (int y = 0; y < coreHeight; y++) {
            String layerRuneType = null;
            BlockPos layerSamplePosition = null;
            int layerMaxLevel = 0;
            boolean validLayer = true;

            for (int x = 0; x < coreWidth && validLayer; x++) {
                for (int z = 0; z < coreWidth; z++) {
                    BlockPos checkPos = origin.offset(x, y, z);
                    BlockState state = level.getBlockState(checkPos);

                    if (!(state.getBlock() instanceof RunestoneBlock runestoneBlock)) {
                        validLayer = false;
                        break;
                    }

                    String currentRuneType = runestoneBlock.getRuneType();

                    if (layerRuneType == null) {
                        layerRuneType = currentRuneType;
                        layerSamplePosition = checkPos.immutable();
                        layerMaxLevel = runestoneBlock.getMaxLevel();
                    } else if (!layerRuneType.equals(currentRuneType)) {
                        validLayer = false;
                        break;
                    }
                }
            }

            if (validLayer && layerRuneType != null && !"neutral".equals(layerRuneType)) {
                int currentLevel = runeLevels.getOrDefault(layerRuneType, 0);
                if (currentLevel < layerMaxLevel) {
                    runeLevels.put(layerRuneType, currentLevel + 1);
                    runeSamplePositions.putIfAbsent(layerRuneType, layerSamplePosition);
                }
            }
        }

        return new CollectedRunes(runeLevels, runeSamplePositions);
    }

    private static boolean isCore(BlockState state) {
        return state.is(OBELISK_CORE);
    }

    private static boolean isFrame(BlockState state) {
        return state.is(OBELISK_FRAME);
    }

    private static final class CollectedRunes {
        private final Map<String, Integer> runeLevels;
        private final Map<String, BlockPos> runeSamplePositions;

        private CollectedRunes(Map<String, Integer> runeLevels, Map<String, BlockPos> runeSamplePositions) {
            this.runeLevels = runeLevels;
            this.runeSamplePositions = runeSamplePositions;
        }
    }
}
