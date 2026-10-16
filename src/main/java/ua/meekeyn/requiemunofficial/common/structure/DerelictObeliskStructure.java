package ua.meekeyn.requiemunofficial.common.structure;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.Vec3i;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.level.NoiseColumn;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.PoolElementStructurePiece;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureType;
import net.minecraft.world.level.levelgen.structure.pools.EmptyPoolElement;
import net.minecraft.world.level.levelgen.structure.pools.StructurePoolElement;
import net.minecraft.world.level.levelgen.structure.pools.StructureTemplatePool;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplateManager;
import ua.meekeyn.requiemunofficial.common.init.ModStructures;

import java.util.List;
import java.util.Optional;
import java.util.OptionalInt;

public class DerelictObeliskStructure extends Structure {
    public static final Codec<DerelictObeliskStructure> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            settingsCodec(instance),
            StructureTemplatePool.CODEC.fieldOf("start_pool").forGetter(structure -> structure.startPool)
    ).apply(instance, DerelictObeliskStructure::new));

    private final Holder<StructureTemplatePool> startPool;

    public DerelictObeliskStructure(StructureSettings settings, Holder<StructureTemplatePool> startPool) {
        super(settings);
        this.startPool = startPool;
    }

    @Override
    protected Optional<GenerationStub> findGenerationPoint(GenerationContext context) {
        ChunkPos chunkPos = context.chunkPos();
        BlockPos centerPos = new BlockPos(chunkPos.getMinBlockX(), 0, chunkPos.getMinBlockZ());

        return Optional.of(new GenerationStub(centerPos, piecesBuilder -> {
            RandomSource random = context.random();
            StructurePoolElement spawnedStructure = this.startPool.value().getRandomTemplate(random);

            if (spawnedStructure == EmptyPoolElement.INSTANCE) {
                return;
            }

            Rotation rotation = Rotation.getRandom(random);
            BlockPos startPos = chunkPos.getWorldPosition();
            StructureTemplateManager structureManager = context.structureTemplateManager();
            PoolElementStructurePiece piece = new PoolElementStructurePiece(
                    structureManager,
                    spawnedStructure,
                    startPos,
                    spawnedStructure.getGroundLevelDelta(),
                    rotation,
                    spawnedStructure.getBoundingBox(structureManager, startPos, rotation)
            );
            BoundingBox boundingBox = piece.getBoundingBox();
            OptionalInt floorY = getFloorHeight(
                    random,
                    context.randomState(),
                    context.chunkGenerator(),
                    boundingBox,
                    context.heightAccessor()
            );

            if (floorY.isEmpty()) {
                return;
            }

            int lowering = boundingBox.minY() + piece.getGroundLevelDelta();
            piece.move(0, floorY.getAsInt() - lowering, 0);

            Vec3i structureCenter = piece.getBoundingBox().getCenter();
            int xOffset = centerPos.getX() - structureCenter.getX();
            int zOffset = centerPos.getZ() - structureCenter.getZ();
            piece.move(xOffset, 0, zOffset);

            piecesBuilder.addPiece(piece);
        }));
    }

    @Override
    public StructureType<?> type() {
        return ModStructures.DERELICT_OBELISK.get();
    }

    static OptionalInt getFloorHeight(RandomSource random, RandomState randomState, ChunkGenerator chunkGenerator, BoundingBox box, LevelHeightAccessor world) {
        int maxY = Mth.nextInt(random, 60, 100);
        List<BlockPos> corners = List.of(
                new BlockPos(box.minX(), 0, box.minZ()),
                new BlockPos(box.maxX(), 0, box.minZ()),
                new BlockPos(box.minX(), 0, box.maxZ()),
                new BlockPos(box.maxX(), 0, box.maxZ())
        );
        List<NoiseColumn> cornerColumns = corners.stream()
                .map(blockPos -> chunkGenerator.getBaseColumn(blockPos.getX(), blockPos.getZ(), world, randomState))
                .toList();
        Heightmap.Types heightmapType = Heightmap.Types.OCEAN_FLOOR_WG;

        for (int y = maxY; y > 15; --y) {
            int validCorners = 0;

            for (NoiseColumn cornerColumn : cornerColumns) {
                BlockState blockState = cornerColumn.getBlock(y);
                if (heightmapType.isOpaque().test(blockState)) {
                    ++validCorners;
                }
            }

            if (validCorners >= 3) {
                validCorners = 0;

                for (NoiseColumn cornerColumn : cornerColumns) {
                    BlockState blockState = cornerColumn.getBlock(y + box.getYSpan() - 1);
                    if (blockState.isAir()) {
                        ++validCorners;
                        if (validCorners == 2) {
                            return OptionalInt.of(y + 1);
                        }
                    }
                }
            }
        }

        return OptionalInt.empty();
    }
}
