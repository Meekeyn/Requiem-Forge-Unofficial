package ua.meekeyn.requiemunofficial.common.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import ua.meekeyn.requiemunofficial.common.block.entity.RunestoneBlockEntity;
import ua.meekeyn.requiemunofficial.common.block.obelisk.ObeliskMatch;
import ua.meekeyn.requiemunofficial.common.block.obelisk.ObeliskMatcher;
import ua.meekeyn.requiemunofficial.common.entity.ObeliskSoulEntity;
import ua.meekeyn.requiemunofficial.common.entity.MorticianEntity;
import ua.meekeyn.requiemunofficial.common.init.ModEntities;
import ua.meekeyn.requiemunofficial.common.init.ModItems;
import ua.meekeyn.requiemunofficial.common.init.ModParticles;
import ua.meekeyn.requiemunofficial.common.init.ModSounds;
import ua.meekeyn.requiemunofficial.common.init.ModBlocks;
import ua.meekeyn.requiemunofficial.common.ritual.RitualManager;
import ua.meekeyn.requiemunofficial.common.ritual.RiftTeleportManager;
import ua.meekeyn.requiemunofficial.common.item.SoulVesselItem;
import ua.meekeyn.requiemunofficial.common.item.IchorVesselItem;

import java.util.Map;
import java.util.Optional;
import java.util.StringJoiner;

public class InertRunestoneBlock extends BaseEntityBlock implements EntityBlock {

    public static final BooleanProperty ACTIVATED = BooleanProperty.create("activated");
    public static final BooleanProperty HEAD = BooleanProperty.create("head");

    private static final int POWER_ATTEMPTS = 6;
    private static final int RECHECK_DELAY_TICKS = 80;
    private static final int MORTICIAN_SPAWN_ROLL = 150;
    private static final int CHARGE_SOUND_COOLDOWN_TICKS = 40;
    private static final int AMBIENT_SOUND_COOLDOWN_TICKS = 160;

    public InertRunestoneBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any()
                .setValue(ACTIVATED, false)
                .setValue(HEAD, false));
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new RunestoneBlockEntity(pos, state);
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<net.minecraft.world.level.block.Block, BlockState> builder) {
        builder.add(ACTIVATED, HEAD);
    }

    @Override
    public @Nullable BlockState getStateForPlacement(BlockPlaceContext context) {
        // Під час встановлення блок починає як неактивний вузол
        return this.defaultBlockState()
                .setValue(ACTIVATED, false)
                .setValue(HEAD, false);
    }

    @Override
    public void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) {
        super.onPlace(state, level, pos, oldState, movedByPiston);

        if (!level.isClientSide) {
            // Планує початкову серверну перевірку структури
            level.scheduleTick(pos, this, 1);
        }
    }

    @Override
    public void neighborChanged(BlockState state, Level level, BlockPos pos,
                                net.minecraft.world.level.block.Block block, BlockPos fromPos, boolean isMoving) {
        super.neighborChanged(state, level, pos, block, fromPos, isMoving);

        if (!level.isClientSide) {
            // Планує повторну перевірку після зміни сусідніх блоків
            level.scheduleTick(pos, this, 1);
        }
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos,
                                 net.minecraft.world.entity.player.Player player,
                                 InteractionHand hand, BlockHitResult hit) {
        if (level.isClientSide) {
            return InteractionResult.SUCCESS;
        }

        if (!(player instanceof ServerPlayer serverPlayer)) {
            return InteractionResult.CONSUME;
        }

        ItemStack handStack = player.getItemInHand(hand);

        if (handStack.is(ModItems.DEBUG_ITEM.get())) {
            if (level.getBlockEntity(pos) instanceof RunestoneBlockEntity runestone) {
                sendDebugInfo(serverPlayer, state, pos, runestone);
                return InteractionResult.CONSUME;
            }

            serverPlayer.sendSystemMessage(Component.literal("No runestone block entity at " + pos.toShortString()));
            return InteractionResult.CONSUME;
        }

        if (tryChangeRune(level, pos, state, serverPlayer, hand, handStack)) {
            return InteractionResult.CONSUME;
        }

        if (handStack.is(ModItems.FILLED_SOUL_VESSEL.get()) && SoulVesselItem.hasSoulData(handStack)) {
            if (tryAbsorbSoulVessel(level, pos, serverPlayer, hand, handStack)) {
                return InteractionResult.CONSUME;
            }
        }

        return InteractionResult.PASS;
    }

    protected boolean tryChangeRune(Level level, BlockPos pos, BlockState state,
                                    ServerPlayer player, InteractionHand hand, ItemStack stack) {
        net.minecraft.world.level.block.Block replacement = null;
        net.minecraft.world.item.Item outputVessel = null;
        boolean clearingRune = false;
        if (state.is(ModBlocks.TACHYLITE_RUNESTONE.get())
                && stack.getItem() instanceof IchorVesselItem ichorVessel) {
            replacement = runestoneForIchor(ichorVessel.getIchorType());
            outputVessel = ModItems.EMPTY_SOUL_VESSEL.get();
        } else if (state.getBlock() instanceof RunestoneBlock
                && stack.is(ModItems.EMPTY_SOUL_VESSEL.get())) {
            replacement = ModBlocks.TACHYLITE_RUNESTONE.get();
            outputVessel = ichorVesselForRunestone(state.getBlock());
            clearingRune = true;
        }
        if (!(replacement instanceof InertRunestoneBlock replacementRunestone)
                || outputVessel == null) {
            return false;
        }

        int storedEnergy = 0;
        if (level.getBlockEntity(pos) instanceof RunestoneBlockEntity existing) {
            if (existing.isHeadBlock() && existing.hasPendingRitual()) {
                player.sendSystemMessage(Component.translatable(
                        "message.requiemunofficial.runestone.ritual_active"
                ));
                return true;
            }
            storedEnergy = existing.getStoredSoulEnergy();
        }

        boolean activated = state.hasProperty(ACTIVATED) && state.getValue(ACTIVATED);
        boolean head = state.hasProperty(HEAD) && state.getValue(HEAD);
        BlockState replacementState = replacementRunestone.defaultBlockState()
                .setValue(ACTIVATED, activated)
                .setValue(HEAD, head);
        level.setBlock(pos, replacementState, UPDATE_ALL);

        if (storedEnergy > 0
                && level.getBlockEntity(pos) instanceof RunestoneBlockEntity replacementEntity) {
            replacementEntity.addStoredSoulEnergy(storedEnergy);
        }

        exchangeVessel(player, hand, stack, outputVessel);
        level.playSound(
                null,
                pos,
                clearingRune ? ModSounds.BLOCK_RUNESTONE_CLEAR.get() : ModSounds.BLOCK_RUNESTONE_CARVE.get(),
                SoundSource.BLOCKS,
                1.0F,
                1.0F
        );
        level.gameEvent(player, clearingRune ? GameEvent.FLUID_PICKUP : GameEvent.BLOCK_CHANGE, pos);
        level.scheduleTick(pos, replacementRunestone, 1);
        return true;
    }

    private static net.minecraft.world.level.block.Block runestoneForIchor(String ichorType) {
        return switch (ichorType) {
            case "attrition" -> ModBlocks.RUNIC_TACHYLITE_ATTRITION.get();
            case "emancipation" -> ModBlocks.RUNIC_TACHYLITE_EMANCIPATION.get();
            case "penance" -> ModBlocks.RUNIC_TACHYLITE_PENANCE.get();
            case "reclamation" -> ModBlocks.RUNIC_TACHYLITE_RECLAMATION.get();
            default -> null;
        };
    }

    private static net.minecraft.world.item.Item ichorVesselForRunestone(
            net.minecraft.world.level.block.Block block
    ) {
        if (block == ModBlocks.RUNIC_TACHYLITE_ATTRITION.get()) {
            return ModItems.ICHOR_VESSEL_ATTRITION.get();
        }
        if (block == ModBlocks.RUNIC_TACHYLITE_EMANCIPATION.get()) {
            return ModItems.ICHOR_VESSEL_EMANCIPATION.get();
        }
        if (block == ModBlocks.RUNIC_TACHYLITE_PENANCE.get()) {
            return ModItems.ICHOR_VESSEL_PENANCE.get();
        }
        if (block == ModBlocks.RUNIC_TACHYLITE_RECLAMATION.get()) {
            return ModItems.ICHOR_VESSEL_RECLAMATION.get();
        }
        return null;
    }

    private static void exchangeVessel(ServerPlayer player, InteractionHand hand, ItemStack stack,
                                       net.minecraft.world.item.Item outputItem) {
        if (player.getAbilities().instabuild) {
            return;
        }
        ItemStack result = new ItemStack(outputItem);
        if (stack.getCount() == 1) {
            player.setItemInHand(hand, result);
            return;
        }
        stack.shrink(1);
        if (!player.getInventory().add(result)) {
            player.drop(result, false);
        }
    }

    protected boolean tryAbsorbSoulVessel(Level level, BlockPos clickedPos,
                                          ServerPlayer player, InteractionHand hand, ItemStack stack) {
        Optional<BlockPos> origin = ObeliskMatcher.findObeliskOrigin(level, clickedPos);
        if (origin.isEmpty()) {
            return false;
        }

        if (!(level.getBlockEntity(origin.get()) instanceof RunestoneBlockEntity headRunestone)) {
            return false;
        }

        if (!headRunestone.isStructureValid() || !headRunestone.isHeadBlock()) {
            return false;
        }

        int soulAmount = Math.max(1, SoulVesselItem.getSoulAmount(stack));
        int soulEnergy = Math.max(1, SoulVesselItem.getSoulEnergy(stack));
        String soulType = SoulVesselItem.getSoulType(stack);

        headRunestone.addStoredSoulEnergy(soulEnergy);

        Vec3 center = getObeliskCenter(origin.get(), headRunestone.getObeliskCoreWidth(), headRunestone.getObeliskCoreHeight());
        spawnAggregatedSoulEntities(
                (ServerLevel) level,
                clickedPos,
                center,
                Math.min(8, soulAmount),
                soulType,
                soulAmount
        );

        if (!player.getAbilities().instabuild) {
            ItemStack resultStack = new ItemStack(ModItems.EMPTY_SOUL_VESSEL.get());

            if (stack.getCount() == 1) {
                player.setItemInHand(hand, resultStack);
            } else {
                stack.shrink(1);
                if (!player.getInventory().add(resultStack)) {
                    player.drop(resultStack, false);
                }
            }
        }

        return true;
    }

    protected void spawnAggregatedSoulEntities(ServerLevel level, BlockPos sourcePos, Vec3 center, int count,
                                               String soulType, int soulAmount) {
        for (int i = 0; i < count; i++) {
            double offsetX = (level.random.nextDouble() - 0.5D) * 0.4D;
            double offsetY = level.random.nextDouble() * 0.4D;
            double offsetZ = (level.random.nextDouble() - 0.5D) * 0.4D;

            ObeliskSoulEntity soul = new ObeliskSoulEntity(ModEntities.OBELISK_SOUL.get(), level);
            soul.setPos(
                    sourcePos.getX() + 0.5D + offsetX,
                    sourcePos.getY() + 0.8D + offsetY,
                    sourcePos.getZ() + 0.5D + offsetZ
            );
            soul.setTarget(center.x, center.y, center.z);
            soul.setLifetime(30 + level.random.nextInt(20));
            soul.setSoulData(soulType, soulAmount);

            level.addFreshEntity(soul);
        }
    }

    protected void sendDebugInfo(ServerPlayer player, BlockState state, BlockPos pos, RunestoneBlockEntity runestone) {
        player.sendSystemMessage(Component.literal("=== Runestone Debug ==="));
        player.sendSystemMessage(Component.literal("Pos: " + pos.toShortString()));
        player.sendSystemMessage(Component.literal("Block: " + state.getBlock().getClass().getSimpleName()));
        player.sendSystemMessage(Component.literal("Activated: " + state.getValue(ACTIVATED)));
        player.sendSystemMessage(Component.literal("Head: " + state.getValue(HEAD)));
        player.sendSystemMessage(Component.literal("Structure valid: " + runestone.isStructureValid()));
        player.sendSystemMessage(Component.literal("Core width: " + runestone.getObeliskCoreWidth()));
        player.sendSystemMessage(Component.literal("Core height: " + runestone.getObeliskCoreHeight()));
        player.sendSystemMessage(Component.literal("Powered: " + runestone.isPowered()));
        player.sendSystemMessage(Component.literal("Power rate: " + runestone.getPowerRate()));
        player.sendSystemMessage(Component.literal("Previous power rate: " + runestone.getPreviousPowerRate()));
        player.sendSystemMessage(Component.literal("Last power update time: " + runestone.getLastPowerUpdateTime()));
        player.sendSystemMessage(Component.literal("Stored soul energy: " + runestone.getStoredSoulEnergy()));
        player.sendSystemMessage(Component.literal("Rune levels: " + formatRuneLevels(runestone)));
        player.sendSystemMessage(Component.literal("Rune sample positions: " + formatRunePositions(runestone)));
    }

    protected String formatRuneLevels(RunestoneBlockEntity runestone) {
        if (runestone.getRuneLevels().isEmpty()) {
            return "none";
        }

        StringJoiner joiner = new StringJoiner(", ");
        for (Map.Entry<String, Integer> entry : runestone.getRuneLevels().entrySet()) {
            joiner.add(entry.getKey() + "=" + entry.getValue());
        }
        return joiner.toString();
    }

    protected String formatRunePositions(RunestoneBlockEntity runestone) {
        if (runestone.getRuneSamplePositions().isEmpty()) {
            return "none";
        }

        StringJoiner joiner = new StringJoiner(", ");
        for (Map.Entry<String, BlockPos> entry : runestone.getRuneSamplePositions().entrySet()) {
            joiner.add(entry.getKey() + "=" + entry.getValue().toShortString());
        }
        return joiner.toString();
    }

    @Override
    public void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        super.tick(state, level, pos, random);

        Optional<BlockPos> origin = ObeliskMatcher.findObeliskOrigin(level, pos);
        if (origin.isEmpty()) {
            deactivateNearbyRunestones(level, pos);
            return;
        }

        Optional<ObeliskMatch> match = ObeliskMatcher.matchObelisk(level, origin.get());
        if (match.isPresent()) {
            activateStructure(level, match.get());
            if (level.getBlockEntity(origin.get()) instanceof RunestoneBlockEntity runestone) {
                RitualManager.completePending(level, origin.get(), runestone);
            }
        } else {
            deactivateNearbyRunestones(level, origin.get());
        }
    }

    protected void activateStructure(ServerLevel level, ObeliskMatch match) {
        BlockPos origin = match.origin();
        BlockState previousOriginState = level.getBlockState(origin);
        boolean wasActive = previousOriginState.hasProperty(ACTIVATED)
                && previousOriginState.getValue(ACTIVATED);

        for (int y = 0; y < match.coreHeight(); y++) {
            for (int x = 0; x < match.coreWidth(); x++) {
                for (int z = 0; z < match.coreWidth(); z++) {
                    BlockPos currentPos = origin.offset(x, y, z);
                    BlockState currentState = level.getBlockState(currentPos);

                    if (!(currentState.getBlock() instanceof InertRunestoneBlock)) {
                        continue;
                    }

                    boolean isHead = currentPos.equals(origin);
                    BlockState targetState = currentState
                            .setValue(ACTIVATED, true)
                            .setValue(HEAD, isHead);

                    if (currentState != targetState) {
                        level.setBlock(currentPos, targetState, UPDATE_ALL);
                    }

                    if (level.getBlockEntity(currentPos) instanceof RunestoneBlockEntity runestone) {
                        // Записує валідну структуру, рівні рун і точні позиції зразків рун
                        runestone.setStructureData(
                                match.coreWidth(),
                                match.coreHeight(),
                                true,
                                match.runeLevels(),
                                match.runeSamplePositions()
                        );
                    }
                }
            }
        }

        if (level.getBlockEntity(origin) instanceof RunestoneBlockEntity headRunestone) {
            updatePowerAroundHead(level, origin, headRunestone);
            if (headRunestone.getRuneLevel("rift") > 0) {
                RiftTeleportManager.register(level, origin);
            }
            level.scheduleTick(origin, this, RECHECK_DELAY_TICKS);
        }

        if (!wasActive) {
            level.playSound(null, origin, ModSounds.OBELISK_ACTIVATE.get(), SoundSource.BLOCKS, 1.0F, 1.0F);
        }
    }

    protected void updatePowerAroundHead(ServerLevel level, BlockPos headPos, RunestoneBlockEntity headRunestone) {
        if (!headRunestone.isHeadBlock() || !headRunestone.isStructureValid()) {
            return;
        }

        int coreWidth = headRunestone.getObeliskCoreWidth();
        int coreHeight = headRunestone.getObeliskCoreHeight();

        if (coreWidth <= 0 || coreHeight <= 0) {
            propagatePowerToStructure(level, headPos, coreWidth, coreHeight, 0.0F);
            return;
        }

        Vec3 center = getObeliskCenter(headPos, coreWidth, coreHeight);
        double range = getPowerRange(coreWidth);

        BlockPos.MutableBlockPos checkedPos = new BlockPos.MutableBlockPos();
        int successes = 0;

        for (int attempt = 0; attempt < POWER_ATTEMPTS; attempt++) {
            double sampleX = center.x + level.random.nextDouble() * range * 2.0D - range;
            double sampleZ = center.z + level.random.nextDouble() * range * 2.0D - range;

            checkedPos.set((int) Math.round(sampleX), (int) Math.floor(center.y), (int) Math.round(sampleZ));

            BlockState checkedState = level.getBlockState(checkedPos);

            while (checkedPos.getY() > level.getMinBuildHeight() && !checkedState.isSolidRender(level, checkedPos)) {
                checkedPos.move(Direction.DOWN);
                checkedState = level.getBlockState(checkedPos);
            }

            if (checkedState.is(BlockTags.SOUL_SPEED_BLOCKS)) {
                successes++;
                spawnSoulParticle(level, center, Vec3.atCenterOf(checkedPos).add(0.0D, 0.4D, 0.0D));
            }
        }

        float ambientPower = successes / (float) POWER_ATTEMPTS;
        float storedEnergyBonus = Math.min(0.5F, headRunestone.getStoredSoulEnergy() * 0.1F);
        float targetPowerRate = Math.min(1.0F, ambientPower + storedEnergyBonus);
        float powerRate = headRunestone.getPowerRate()
                + (targetPowerRate - headRunestone.getPowerRate()) * 0.35F;

        if (headRunestone.getStoredSoulEnergy() > 0) {
            // Повільно витрачає накопичену енергію душ під час роботи обеліска
            headRunestone.consumeStoredSoulEnergy(1);
            if (headRunestone.shouldPlayChargeSound(level.getGameTime(), CHARGE_SOUND_COOLDOWN_TICKS)) {
                level.playSound(null, headPos, ModSounds.OBELISK_CHARGE.get(), SoundSource.BLOCKS, 0.45F, 1.0F);
            }
        }

        propagatePowerToStructure(level, headPos, coreWidth, coreHeight, powerRate);

        if (powerRate > 0.5F) {
            if (headRunestone.shouldPlayAmbientSound(level.getGameTime(), AMBIENT_SOUND_COOLDOWN_TICKS)) {
                level.playSound(null, headPos, ModSounds.OBELISK_AMBIENT.get(), SoundSource.BLOCKS, 0.25F, 1.0F);
            }
            applyEffectsAroundHead(level, headPos, headRunestone);
            trySpawnMortician(level, headPos, headRunestone);
        }
    }

    protected void trySpawnMortician(ServerLevel level, BlockPos headPos,
                                     RunestoneBlockEntity headRunestone) {
        if (headRunestone.getRuneLevel("rift") <= 0
                || level.random.nextInt(MORTICIAN_SPAWN_ROLL) != 0) {
            return;
        }

        AABB nearby = new AABB(headPos).inflate(48.0D);
        if (!level.getEntitiesOfClass(MorticianEntity.class, nearby).isEmpty()) {
            return;
        }

        int x = headPos.getX() + level.random.nextIntBetweenInclusive(-10, 10);
        int z = headPos.getZ() + level.random.nextIntBetweenInclusive(-10, 10);
        int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
        BlockPos spawnPos = new BlockPos(x, y, z);
        if (!level.getWorldBorder().isWithinBounds(spawnPos)
                || !level.noCollision(ModEntities.MORTICIAN.get().getAABB(
                        x + 0.5D, y, z + 0.5D))) {
            return;
        }

        MorticianEntity mortician = ModEntities.MORTICIAN.get().create(level);
        if (mortician == null) {
            return;
        }

        mortician.moveTo(x + 0.5D, y, z + 0.5D, level.random.nextFloat() * 360.0F, 0.0F);
        mortician.linkToObelisk(level, headPos);
        mortician.restrictTo(headPos, 32);
        level.addFreshEntity(mortician);
    }

    protected void propagatePowerToStructure(ServerLevel level, BlockPos origin, int coreWidth, int coreHeight, float powerRate) {
        if (coreWidth <= 0 || coreHeight <= 0) {
            return;
        }

        for (int y = 0; y < coreHeight; y++) {
            for (int x = 0; x < coreWidth; x++) {
                for (int z = 0; z < coreWidth; z++) {
                    BlockPos currentPos = origin.offset(x, y, z);
                    if (level.getBlockEntity(currentPos) instanceof RunestoneBlockEntity runestone) {
                        // Оновлює силу обеліска для всіх вузлів core-структури
                        runestone.setPowerRate(powerRate);
                    }
                }
            }
        }
    }

    protected Vec3 getObeliskCenter(BlockPos origin, int coreWidth, int coreHeight) {
        double centerX = origin.getX() + (coreWidth / 2.0D);
        double centerY = origin.getY() + Math.max(0.5D, coreHeight * 0.5D);
        double centerZ = origin.getZ() + (coreWidth / 2.0D);
        return new Vec3(centerX, centerY, centerZ);
    }

    protected double getPowerRange(int coreWidth) {
        return coreWidth * 5.0D;
    }

    protected void spawnSoulParticle(ServerLevel level, Vec3 center, Vec3 source) {
        Vec3 direction = center.subtract(source);
        if (direction.lengthSqr() <= 0.0001D) {
            return;
        }

        Vec3 normalized = direction.normalize();

        // Створює частинку душі, що летить у бік центру обеліска
        level.sendParticles(
                ModParticles.OBELISK_SOUL.get(),
                source.x,
                source.y,
                source.z,
                0,
                normalized.x,
                normalized.y,
                normalized.z,
                0.08D
        );
    }

    protected void applyEffectsAroundHead(ServerLevel level, BlockPos headPos, RunestoneBlockEntity runestone) {
        if (!runestone.isHeadBlock() || !runestone.isStructureValid() || !runestone.isPowered()) {
            return;
        }

        int coreWidth = runestone.getObeliskCoreWidth();
        if (coreWidth <= 0) {
            return;
        }

        double range = coreWidth * 10.0D + 10.0D;
        AABB box = new AABB(headPos).inflate(range);

        for (ServerPlayer player : level.getEntitiesOfClass(ServerPlayer.class, box, this::canReceiveObeliskEffects)) {
            for (Map.Entry<String, Integer> entry : runestone.getRuneLevels().entrySet()) {
                applyRuneEffectToPlayer(
                        level,
                        player,
                        entry.getKey(),
                        entry.getValue(),
                        coreWidth,
                        runestone.getRuneSamplePosition(entry.getKey())
                );
            }
        }
    }

    protected boolean canReceiveObeliskEffects(ServerPlayer player) {
        if (player.isSpectator()) {
            return false;
        }

        if (player.gameMode.getGameModeForPlayer() == GameType.SPECTATOR) {
            return false;
        }

        return true;
    }

    protected void applyRuneEffectToPlayer(ServerLevel level, ServerPlayer player,
                                           String runeType, int runeLevel, int obeliskWidth,
                                           @Nullable BlockPos runeSamplePos) {
        if (runeSamplePos == null) {
            return;
        }

        BlockState runeState = level.getBlockState(runeSamplePos);
        if (!(runeState.getBlock() instanceof RunestoneBlock runestoneBlock)) {
            return;
        }

        if (!runeType.equals(runestoneBlock.getRuneType())) {
            return;
        }

        // Застосовує ефект через реальний блок-носій руни
        runestoneBlock.applyEffect(player, runeLevel, obeliskWidth);
    }

    protected void deactivateNearbyRunestones(ServerLevel level, BlockPos center) {
        int horizontalRange = ObeliskMatcher.MAX_OBELISK_CORE_WIDTH + 2;
        int verticalRange = ObeliskMatcher.MAX_OBELISK_CORE_HEIGHT + 1;

        BlockPos min = center.offset(-horizontalRange, -verticalRange, -horizontalRange);
        BlockPos max = center.offset(horizontalRange, verticalRange, horizontalRange);
        boolean hadActiveRunestone = false;

        for (BlockPos currentPos : BlockPos.betweenClosed(min, max)) {
            BlockState currentState = level.getBlockState(currentPos);

            if (!(currentState.getBlock() instanceof InertRunestoneBlock)) {
                continue;
            }

            boolean changed = false;
            BlockState newState = currentState;

            if (currentState.getValue(ACTIVATED)) {
                hadActiveRunestone = true;
                newState = newState.setValue(ACTIVATED, false);
                changed = true;
            }

            if (currentState.getValue(HEAD)) {
                newState = newState.setValue(HEAD, false);
                changed = true;
            }

            if (changed) {
                level.setBlock(currentPos, newState, UPDATE_ALL);
            }

            if (level.getBlockEntity(currentPos) instanceof RunestoneBlockEntity runestone) {
                // Очищає тимчасові дані невалідної або зруйнованої структури
                if (runestone.isHeadBlock() && runestone.getRuneLevel("rift") > 0) {
                    RiftTeleportManager.unregister(level, currentPos);
                }
                runestone.clearStructureData();
            }
        }

        if (hadActiveRunestone) {
            level.playSound(null, center, ModSounds.OBELISK_DEACTIVATE.get(), SoundSource.BLOCKS, 1.0F, 1.0F);
        }
    }

    @Override
    public boolean triggerEvent(BlockState state, Level level, BlockPos pos, int id, int param) {
        super.triggerEvent(state, level, pos, id, param);
        BlockEntity blockentity = level.getBlockEntity(pos);
        return blockentity != null && blockentity.triggerEvent(id, param);
    }

    @Override
    public boolean useShapeForLightOcclusion(BlockState state) {
        return false;
    }

    @Override
    public float getShadeBrightness(BlockState state, BlockGetter level, BlockPos pos) {
        return 1.0F;
    }
}
