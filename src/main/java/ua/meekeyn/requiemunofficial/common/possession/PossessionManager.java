package ua.meekeyn.requiemunofficial.common.possession;

import net.minecraft.network.chat.Component;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.ResourceArgument;
import net.minecraft.commands.arguments.coordinates.Vec3Argument;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.animal.Pufferfish;
import net.minecraft.world.entity.animal.axolotl.Axolotl;
import net.minecraft.world.entity.animal.frog.Frog;
import net.minecraft.world.entity.animal.horse.AbstractHorse;
import net.minecraft.world.entity.animal.horse.Llama;
import net.minecraft.world.entity.animal.SnowGolem;
import net.minecraft.world.entity.monster.Evoker;
import net.minecraft.world.entity.monster.Guardian;
import net.minecraft.world.entity.monster.Blaze;
import net.minecraft.world.entity.monster.AbstractSkeleton;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.monster.RangedAttackMob;
import net.minecraft.world.entity.monster.Shulker;
import net.minecraft.world.entity.monster.Spider;
import net.minecraft.world.entity.monster.Vex;
import net.minecraft.world.entity.projectile.EvokerFangs;
import net.minecraft.world.entity.projectile.LargeFireball;
import net.minecraft.world.entity.projectile.LlamaSpit;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.entity.projectile.Arrow;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.entity.projectile.Snowball;
import net.minecraft.world.entity.projectile.ShulkerBullet;
import net.minecraft.world.entity.projectile.SmallFireball;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.event.entity.living.LivingChangeTargetEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingFallEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.living.LivingKnockBackEvent;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.entity.EntityTeleportEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.CommandEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import ua.meekeyn.requiemunofficial.RequiemUnofficial;
import ua.meekeyn.requiemunofficial.common.soul.SoulManager;
import ua.meekeyn.requiemunofficial.common.soul.SoulLifecycleManager;
import ua.meekeyn.requiemunofficial.common.soul.SoulState;
import ua.meekeyn.requiemunofficial.common.soul.VagrantRules;
import ua.meekeyn.requiemunofficial.common.soul.VagrantEvents;
import ua.meekeyn.requiemunofficial.common.soul.EntitySoulManager;
import ua.meekeyn.requiemunofficial.common.soul.PlayerSoulEvents;
import ua.meekeyn.requiemunofficial.common.util.ModTags;
import ua.meekeyn.requiemunofficial.common.entity.PlayerShellEntity;
import ua.meekeyn.requiemunofficial.common.effect.AttritionMobEffect;
import ua.meekeyn.requiemunofficial.common.effect.PenanceMobEffect;
import ua.meekeyn.requiemunofficial.common.init.ModDamageSources;
import ua.meekeyn.requiemunofficial.common.init.ModEffects;
import ua.meekeyn.requiemunofficial.common.init.ModGameRules;
import ua.meekeyn.requiemunofficial.common.network.PossessionActionPacket;
import ua.meekeyn.requiemunofficial.common.network.PossessionInputPacket;
import ua.meekeyn.requiemunofficial.common.network.PossessionFxPacket;
import ua.meekeyn.requiemunofficial.common.network.PossessionNameTagPacket;
import ua.meekeyn.requiemunofficial.common.network.PossessionTargetHintPacket;
import ua.meekeyn.requiemunofficial.common.network.VagrantSenseTargetsPacket;
import ua.meekeyn.requiemunofficial.common.network.ModNetwork;
import ua.meekeyn.requiemunofficial.common.remnant.DroppedVesselTracker;
import ua.meekeyn.requiemunofficial.common.advancement.ModCriteria;
import ua.meekeyn.requiemunofficial.common.ritual.DeathResurrectionManager;
import ua.meekeyn.requiemunofficial.mixin.GuardianAccessor;
import ua.meekeyn.requiemunofficial.mixin.EntityAccessor;
import ua.meekeyn.requiemunofficial.mixin.LivingEntityAccessor;
import ua.meekeyn.requiemunofficial.mixin.ShulkerAccessor;
import ua.meekeyn.requiemunofficial.mixin.BlazeAccessor;

import java.util.Comparator;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.StreamSupport;

@Mod.EventBusSubscriber(modid = RequiemUnofficial.MODID)
public final class PossessionManager {

    private static final String ORIGINAL_NO_AI = "RequiemOriginalNoAi";
    private static final String ORIGINAL_NO_GRAVITY = "RequiemOriginalNoGravity";
    private static final String ORIGINAL_NO_PHYSICS = "RequiemOriginalNoPhysics";
    private static final String ORIGINAL_MAX_UP_STEP = "RequiemOriginalMaxUpStep";
    private static final String POSSESSED = "RequiemPossessed";
    private static final String POSSESSOR = "RequiemPossessor";
    private static final double SPECIAL_MOBILITY_CONTROL_SCALE = 0.4D;
    private static final double HUMANOID_PLAYER_BODY_SPEED_SCALE = 0.4D;
    private static final float MIN_POSSESSED_STEP_HEIGHT = 0.6F;
    private static final String NEXT_ATTACK_TIME = "RequiemNextHostAttack";
    private static final String NEXT_INTERACT_TIME = "RequiemNextHostInteract";
    private static final String HAD_CUSTOM_NAME = "RequiemHadCustomName";
    private static final String ORIGINAL_CUSTOM_NAME = "RequiemOriginalCustomName";
    private static final String ORIGINAL_CUSTOM_NAME_VISIBLE = "RequiemOriginalCustomNameVisible";
    private static final String ORIGINAL_PLAYER_FOOD = "RequiemOriginalPlayerFood";
    private static final String HOST_FOOD = "RequiemPossessedFood";
    private static final String ORIGINAL_PLAYER_NO_PHYSICS = "RequiemOriginalPlayerNoPhysics";
    private static final String ORIGINAL_PLAYER_NO_GRAVITY = "RequiemOriginalPlayerNoGravity";
    private static final String ORIGINAL_PLAYER_MAYFLY = "RequiemOriginalPlayerMayfly";
    private static final String ORIGINAL_PLAYER_FLYING = "RequiemOriginalPlayerFlying";
    private static final String BETA16_PLAYER_BODY = "RequiemBeta16PlayerBody";
    private static final String ORIGINAL_PLAYER_MAX_UP_STEP = "RequiemOriginalPlayerMaxUpStep";
    private static final String STORED_DISCONNECT_HOST = "RequiemStoredDisconnectHost";
    private static final String STORED_DISCONNECT_HOST_TYPE = "Type";
    private static final String STORED_DISCONNECT_HOST_DIMENSION = "Dimension";
    private static final String STORED_DISCONNECT_HOST_ENTITY = "Entity";
    private static final String LAST_POSSESSOR = "RequiemLastPossessor";
    private static final String PROVOKED_BY_POSSESSED_HOST = "RequiemProvokedByPossessedHost";
    private static final String PROVOKED_BY_POSSESSED_HOST_UNTIL = "RequiemProvokedByPossessedHostUntil";
    private static final String GUARDIAN_BEAM_TARGET = "RequiemGuardianBeamTarget";
    private static final String GUARDIAN_BEAM_END_TIME = "RequiemGuardianBeamEndTime";
    private static final String BLAZE_ATTACK_STEP = "RequiemBlazeAttackStep";
    private static final String BLAZE_ATTACK_TIME = "RequiemBlazeAttackTime";
    private static final String BLAZE_ATTACK_TARGET = "RequiemBlazeAttackTarget";
    private static final String BLAZE_ATTACK_FORWARD = "RequiemBlazeAttackForward";
    private static final String AXOLOTL_PLAYING_DEAD_END_TIME = "RequiemAxolotlPlayingDeadEndTime";
    private static final int POSSESSION_RANGE = 5;
    private static final double VAGRANT_SENSE_RANGE = 64.0D;
    private static final int POSSESSION_DELAY_TICKS = 8;
    private static final int GUARDIAN_BEAM_WARMUP_TICKS = 40;
    private static final int AXOLOTL_PLAYING_DEAD_TICKS = 200;
    private static final int POSSESSED_HOST_PROVOKE_TICKS = 20 * 30;
    private static final int RELEASE_INPUT_GRACE_TICKS = 10;
    private static final UUID AXOLOTL_PLAYING_DEAD_SLOWDOWN =
            UUID.fromString("5a13c664-7932-4a73-b341-0745cb737754");
    private static final UUID BETA16_PLAYER_SPEED_MODIFIER =
            UUID.fromString("f25420ed-4041-4cc6-87fa-6e5272dddb2b");
    private static final double POSSESSOR_RESYNC_DISTANCE_SQR = 64.0D;
    private static final Map<UUID, PendingPossession> PENDING_POSSESSIONS = new HashMap<>();
    private static final Map<UUID, ServerPlayer> ACTIVE_POSSESSORS = new HashMap<>();
    private static final Map<UUID, PossessionInput> POSSESSION_INPUTS = new HashMap<>();
    private static final Map<UUID, Boolean> POSSESSION_TARGET_HINTS = new HashMap<>();
    private static final Map<UUID, List<Integer>> VAGRANT_SENSE_TARGETS = new HashMap<>();
    private static final Map<UUID, Long> POSSESSION_STARTED_AT = new HashMap<>();
    private static final Set<String> ALEX_NIGHT_EYED = Set.of(
            "cave_centipede",
            "cockroach",
            "dropbear",
            "endergrade",
            "enderiophage",
            "farseer",
            "froststalker",
            "murmur",
            "raccoon",
            "rattlesnake",
            "skreecher",
            "soul_vulture",
            "spectre",
            "tarantula_hawk",
            "underminer",
            "void_worm",
            "warped_mosco",
            "warped_toad"
    );
    private static final Set<String> ALEX_AQUATIC = Set.of(
            "alligator_snapping_turtle",
            "anaconda",
            "blobfish",
            "bone_serpent",
            "cachalot_whale",
            "caiman",
            "catfish",
            "comb_jelly",
            "cosmic_cod",
            "crocodile",
            "devils_hole_pupfish",
            "flying_fish",
            "frilled_shark",
            "giant_squid",
            "hammerhead_shark",
            "laviathan",
            "lobster",
            "mantis_shrimp",
            "mimic_octopus",
            "mudskipper",
            "orca",
            "platypus",
            "sea_bear",
            "seal",
            "skelewag",
            "stradpole",
            "terrapin",
            "triops",
            "warped_toad"
    );
    private static final Set<String> ALEX_WATER_BREATHERS = Set.of(
            "blobfish",
            "catfish",
            "comb_jelly",
            "cosmic_cod",
            "devils_hole_pupfish",
            "flying_fish",
            "frilled_shark",
            "giant_squid",
            "hammerhead_shark",
            "lobster",
            "mantis_shrimp",
            "mimic_octopus",
            "skelewag",
            "triops"
    );
    private static final Set<String> ALEX_AIR_BREATHING_AQUATIC = Set.of(
            "alligator_snapping_turtle",
            "anaconda",
            "cachalot_whale",
            "caiman",
            "crocodile",
            "orca",
            "platypus",
            "sea_bear",
            "seal",
            "terrapin"
    );
    private static final Set<String> ALEX_FIREPROOF = Set.of(
            "bone_serpent",
            "laviathan",
            "straddler",
            "stradpole",
            "sunbird"
    );
    private static final Set<String> ALEX_WATER_HURT = Set.of(
            "bone_serpent",
            "laviathan",
            "straddler",
            "stradpole",
            "sunbird"
    );
    private static final Set<String> ALEX_LONG_REACH = Set.of(
            "bone_serpent",
            "cachalot_whale",
            "crocodile",
            "elephant",
            "giant_squid",
            "laviathan",
            "orca",
            "rhinoceros",
            "straddler",
            "void_worm"
    );
    private static final Set<String> ALEX_POISON_ATTACKERS = Set.of(
            "cave_centipede",
            "komodo_dragon",
            "rattlesnake",
            "tarantula_hawk",
            "warped_toad"
    );
    private static final Set<String> ALEX_HEAVY_ATTACKERS = Set.of(
            "bison",
            "bunfungus",
            "elephant",
            "gorilla",
            "grizzly_bear",
            "kangaroo",
            "laviathan",
            "mantis_shrimp",
            "moose",
            "rhinoceros",
            "rocky_roller",
            "sea_bear",
            "tiger",
            "tusklin",
            "void_worm",
            "warped_mosco"
    );
    private static final Set<String> ALEX_SLOWING_ATTACKERS = Set.of(
            "alligator_snapping_turtle",
            "anaconda",
            "crocodile",
            "froststalker",
            "giant_squid",
            "mimic_octopus",
            "skreecher"
    );
    private static final Set<String> ALEX_CHARGING_ATTACKERS = Set.of(
            "bison",
            "elephant",
            "gazelle",
            "moose",
            "rhinoceros",
            "tusklin"
    );
    private static final Set<String> ALEX_TELEPORTERS = Set.of(
            "cosmaw",
            "endergrade",
            "enderiophage",
            "spectre"
    );

    private PossessionManager() {
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onEntityInteract(PlayerInteractEvent.EntityInteract event) {
        if (event.getLevel().isClientSide()
                || !(event.getEntity() instanceof ServerPlayer player)
                || player.isSpectator()
                || !(event.getTarget() instanceof LivingEntity target)) {
            return;
        }

        if (SoulManager.getState(player) != SoulState.VAGRANT) {
            return;
        }
        if (SoulManager.getPossessedEntityId(player) != null) {
            return;
        }
        if (target instanceof net.minecraft.world.item.trading.Merchant
                && !canPossess(player, target)) {
            return;
        }
        if (player.getAbilities().instabuild) {
            InteractionResult itemResult = tryCreativeItemEntityInteraction(player, target, event.getHand());
            if (itemResult != InteractionResult.PASS) {
                event.setCanceled(true);
                event.setCancellationResult(itemResult);
                return;
            }
        }
        if (event.getHand() != InteractionHand.MAIN_HAND) {
            return;
        }

        event.setCanceled(true);
        event.setCancellationResult(net.minecraft.world.InteractionResult.CONSUME);
        beginPossessionAttempt(player, target, true);
    }

    private static InteractionResult tryCreativeItemEntityInteraction(ServerPlayer player,
                                                                      LivingEntity target,
                                                                      InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (stack.isEmpty()) {
            return InteractionResult.PASS;
        }
        // У creative-привида предмет має першу спробу, інакше possession перехопить чужі item interactions.
        InteractionResult result = stack.interactLivingEntity(player, target, hand);
        return result;
    }

    public static boolean beginPossessionAttemptForTest(ServerPlayer player, LivingEntity target) {
        return beginPossessionAttempt(player, target, false);
    }

    private static boolean beginPossessionAttempt(ServerPlayer player, LivingEntity target, boolean feedback) {
        if (target instanceof PlayerShellEntity shell
                && SoulLifecycleManager.tryReclaimShell(player, shell)) {
            return true;
        }

        Component failure = possessionFailureMessage(player, target);
        if (failure != null) {
            sendSystemMessage(player, failure);
            return false;
        }

        long completeTime = player.level().getGameTime() + POSSESSION_DELAY_TICKS;
        PENDING_POSSESSIONS.put(player.getUUID(), new PendingPossession(target.getUUID(), completeTime));
        if (feedback) {
            player.level().playSound(
                    null,
                    target.blockPosition(),
                    SoundEvents.SOUL_ESCAPE,
                    SoundSource.PLAYERS,
                    1.4F,
                    0.6F
            );
            ModNetwork.sendToConnectedPlayer(
                    player,
                    new PossessionFxPacket(PossessionFxPacket.Phase.START, target.getId())
            );
        }
        return true;
    }

    public static boolean tryPossess(ServerPlayer player, LivingEntity target) {
        PENDING_POSSESSIONS.remove(player.getUUID());
        if (target instanceof PlayerShellEntity shell
                && SoulLifecycleManager.tryReclaimShell(player, shell)) {
            return true;
        }

        Component failure = possessionFailureMessage(player, target);
        if (failure != null) {
            sendSystemMessage(player, failure);
            return false;
        }

        Component targetName = target.getDisplayName();
        if (target instanceof Mob mob) {
            target.getPersistentData().putBoolean(ORIGINAL_NO_AI, mob.isNoAi());
            mob.setNoAi(false);
            mob.setPersistenceRequired();
        }
        target.getPersistentData().putBoolean(ORIGINAL_NO_GRAVITY, target.isNoGravity());
        if (EntityMobilityManager.get(target.getType()).forcesFlight()) {
            target.setNoGravity(true);
        } else {
            target.setNoGravity(false);
        }
        target.getPersistentData().putBoolean(ORIGINAL_NO_PHYSICS, target.noPhysics);
        target.noPhysics = EntityMobilityManager.get(target.getType()).phasesThroughWalls();
        applyPossessedStepHeight(target);

        target.getPersistentData().putBoolean(POSSESSED, true);
        target.getPersistentData().putUUID(POSSESSOR, player.getUUID());
        player.removeEffect(ModEffects.EMANCIPATION.get());
        PossessionInventoryManager.onPossessionStarted(player, target);
        applyHostFoodData(player, target);
        applyPossessorName(player, target);
        if (usesBeta16PlayerBody(target)) {
            startBeta16PlayerBodyPossession(player, target);
        } else {
            suppressPossessorBody(player);
            syncPossessorToHost(player, target, true);
        }
        syncPossessorNameTag(player, target);
        SoulManager.setPossessedEntity(player, target.getUUID());
        rememberExistingHostility(target);
        ACTIVE_POSSESSORS.put(player.getUUID(), player);
        POSSESSION_STARTED_AT.put(player.getUUID(), player.level().getGameTime());
        ModNetwork.sendToConnectedPlayer(
                player,
                new PossessionFxPacket(PossessionFxPacket.Phase.ACK, target.getId())
        );
        ModCriteria.PLAYER_POSSESSED_ENTITY.trigger(player, target);
        sendSystemMessage(player, Component.translatable(
                "message.requiemunofficial.possession.started",
                targetName
        ));
        return true;
    }

    public static boolean transferPossession(ServerPlayer player,
                                             LivingEntity source,
                                             LivingEntity replacement) {
        if (!player.getUUID().equals(getPossessor(source))
                || !source.getUUID().equals(SoulManager.getPossessedEntityId(player))
                || !replacement.isAlive()) {
            return false;
        }

        restoreHost(source);
        replacement.setCustomName(source.getCustomName());
        replacement.setCustomNameVisible(source.isCustomNameVisible());
        replacement.setNoGravity(source.isNoGravity());
        if (replacement instanceof Mob replacementMob && source instanceof Mob sourceMob) {
            replacementMob.setNoAi(sourceMob.isNoAi());
        }
        replacement.getPersistentData().putBoolean(
                ORIGINAL_NO_GRAVITY,
                replacement.isNoGravity()
        );
        if (replacement instanceof Mob mob) {
            replacement.getPersistentData().putBoolean(ORIGINAL_NO_AI, mob.isNoAi());
            mob.setNoAi(false);
            mob.setPersistenceRequired();
        }
        if (EntityMobilityManager.get(replacement.getType()).forcesFlight()) {
            replacement.setNoGravity(true);
        } else {
            replacement.setNoGravity(false);
        }
        replacement.getPersistentData().putBoolean(ORIGINAL_NO_PHYSICS, replacement.noPhysics);
        replacement.noPhysics = EntityMobilityManager.get(replacement.getType()).phasesThroughWalls();
        applyPossessedStepHeight(replacement);
        replacement.getPersistentData().putBoolean(POSSESSED, true);
        replacement.getPersistentData().putUUID(POSSESSOR, player.getUUID());
        applyHostFoodData(player, replacement);
        applyPossessorName(player, replacement);

        SoulManager.setPossessedEntity(player, replacement.getUUID());
        ACTIVE_POSSESSORS.put(player.getUUID(), player);
        POSSESSION_STARTED_AT.put(player.getUUID(), player.level().getGameTime());
        if (usesBeta16PlayerBody(replacement)) {
            startBeta16PlayerBodyPossession(player, replacement);
        } else {
            suppressPossessorBody(player);
            syncPossessorToHost(player, replacement, true);
        }
        syncPossessorNameTag(player, replacement);
        return true;
    }

    public static UUID getPossessorId(LivingEntity host) {
        return getPossessor(host);
    }

    public static boolean isPossessing(net.minecraft.world.entity.player.Player player) {
        return SoulManager.getPossessedEntityId(player) != null;
    }

    public static boolean isBeta16HostForPlayer(Player player, Entity entity) {
        if (!(entity instanceof LivingEntity host) || player == null || !usesBeta16PlayerBody(host)) {
            return false;
        }
        UUID possessedId = SoulManager.getPossessedEntityId(player);
        return possessedId != null
                && possessedId.equals(host.getUUID())
                && player.level().dimension().equals(host.level().dimension());
    }

    public static boolean isPossessorHostCollisionPair(Entity first, Entity second) {
        if (first instanceof Player player && isBeta16HostForPlayer(player, second)) {
            return true;
        }
        return second instanceof Player player && isBeta16HostForPlayer(player, first);
    }

    public static LivingEntity findPossessedHost(ServerPlayer player) {
        UUID hostId = SoulManager.getPossessedEntityId(player);
        return hostId == null ? null : findHost(player, hostId);
    }

    public static ServerPlayer getActivePossessor(ServerLevel level, UUID possessorId) {
        ServerPlayer listed = level.getServer().getPlayerList().getPlayer(possessorId);
        return listed == null ? ACTIVE_POSSESSORS.get(possessorId) : listed;
    }

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END
                || event.player.level().isClientSide()
                || !(event.player instanceof ServerPlayer player)) {
            return;
        }

        processPendingPossession(player);
        updatePossessionTargetHint(player);
        updateVagrantSenseTargets(player);

        UUID possessedId = SoulManager.getPossessedEntityId(player);
        if (player.isSpectator()) {
            if (possessedId != null) {
                release(player, findHost(player, possessedId), false);
            }
            clearVagrantSenseTargets(player);
            return;
        }
        if (possessedId == null) {
            return;
        }
        if (SoulManager.getState(player) != SoulState.VAGRANT) {
            LivingEntity staleHost = findHost(player, possessedId);
            release(player, staleHost, false);
            return;
        }

        LivingEntity storedHost = findHost(player, possessedId);
        if (storedHost == null || !storedHost.isAlive()) {
            release(player, storedHost);
            return;
        }
        UUID hostPossessor = getPossessor(storedHost);
        if (hostPossessor == null) {
            storedHost.getPersistentData().putBoolean(POSSESSED, true);
            storedHost.getPersistentData().putUUID(POSSESSOR, player.getUUID());
        } else if (!player.getUUID().equals(hostPossessor)) {
            release(player, storedHost);
            return;
        }

        if (usesBeta16PlayerBody(storedHost)) {
            updateBeta16PlayerBodyParameters(player, storedHost);
            applyBeta16MobilityToPlayer(player, storedHost);
            syncBeta16HostToPlayer(player, storedHost);
        } else {
            syncPossessorToHost(player, storedHost, false);
        }
        PossessionInventoryManager.syncPlayerEquipmentToHost(player, storedHost);
        if (!usesBeta16PlayerBody(storedHost)) {
            syncPossessorToHost(player, storedHost, false);
        }
        processBlazeAttack(storedHost);
        if (player.tickCount % 20 == 0) {
            syncPossessorNameTag(player, storedHost);
        }
        processGuardianBeam(player, storedHost);
        processBodyInteractions(storedHost);
        clearPossessedDisguiseTargets(player, storedHost);
        if (player.tickCount % 40 == 0) {
            applyHostPassiveTraits(player, storedHost);
        }
        if (player.tickCount % 10 == 0) {
            PossessionDetectionManager.detectBumps(storedHost);
        }
    }

    public static boolean hasPendingPossessionForTest(ServerPlayer player) {
        return PENDING_POSSESSIONS.containsKey(player.getUUID());
    }

    private static void processPendingPossession(ServerPlayer player) {
        PendingPossession pending = PENDING_POSSESSIONS.get(player.getUUID());
        if (pending == null) {
            return;
        }

        LivingEntity target = findHost(player, pending.targetId());
        if (target == null
                || SoulManager.getState(player) != SoulState.VAGRANT
                || SoulManager.getPossessedEntityId(player) != null
                || player.distanceToSqr(target) > POSSESSION_RANGE * POSSESSION_RANGE
                || !canPossess(player, target)) {
            PENDING_POSSESSIONS.remove(player.getUUID());
            return;
        }

        if (player.level() instanceof ServerLevel level && player.tickCount % 2 == 0) {
            level.sendParticles(
                    ParticleTypes.SOUL,
                    target.getX(),
                    target.getY() + target.getBbHeight() * 0.5D,
                    target.getZ(),
                    4,
                    target.getBbWidth() * 0.25D,
                    target.getBbHeight() * 0.2D,
                    target.getBbWidth() * 0.25D,
                    0.03D
            );
        }

        if (player.level().getGameTime() >= pending.completeGameTime()) {
            tryPossess(player, target);
        }
    }

    private static void updatePossessionTargetHint(ServerPlayer player) {
        if (player.tickCount % 2 != 0) {
            return;
        }

        boolean canPossessTarget = findLookedAtPossessionTarget(player) != null;
        Boolean previous = POSSESSION_TARGET_HINTS.put(player.getUUID(), canPossessTarget);
        if (previous == null || previous != canPossessTarget) {
            ModNetwork.sendToConnectedPlayer(player, new PossessionTargetHintPacket(canPossessTarget));
        }
    }

    private static void updateVagrantSenseTargets(ServerPlayer player) {
        if (player.tickCount % 4 != 0) {
            return;
        }

        if (SoulManager.getState(player) != SoulState.VAGRANT
                || SoulManager.getPossessedEntityId(player) != null
                || hasPossessionBlockingPenance(player)
                || !shouldShowVagrantSenseTargets(player)) {
            clearVagrantSenseTargets(player);
            return;
        }

        List<Integer> targets = collectVagrantSenseTargetIds(player);
        List<Integer> previous = VAGRANT_SENSE_TARGETS.get(player.getUUID());
        if (!targets.equals(previous)) {
            VAGRANT_SENSE_TARGETS.put(player.getUUID(), targets);
        }
        ModNetwork.sendToConnectedPlayer(player, new VagrantSenseTargetsPacket(targets));
    }

    public static List<Integer> collectVagrantSenseTargetIdsForTest(ServerPlayer player) {
        return collectVagrantSenseTargetIds(player);
    }

    public static boolean isVagrantSenseTargetForTest(ServerPlayer player, LivingEntity entity) {
        return canPossess(player, entity);
    }

    private static List<Integer> collectVagrantSenseTargetIds(ServerPlayer player) {
        AABB searchBox = player.getBoundingBox().inflate(VAGRANT_SENSE_RANGE);
        return player.serverLevel().getEntitiesOfClass(LivingEntity.class, searchBox).stream()
                .filter(entity -> entity != player && canPossess(player, entity))
                .map(Entity::getId)
                .sorted()
                .toList();
    }

    private static boolean shouldShowVagrantSenseTargets(ServerPlayer player) {
        return VagrantRules.isRestricted(player);
    }

    private static void clearVagrantSenseTargets(ServerPlayer player) {
        List<Integer> previous = VAGRANT_SENSE_TARGETS.remove(player.getUUID());
        if (previous != null && !previous.isEmpty()) {
            ModNetwork.sendToConnectedPlayer(player, new VagrantSenseTargetsPacket(List.of()));
        }
    }

    private static LivingEntity findLookedAtPossessionTarget(ServerPlayer player) {
        if (SoulManager.getState(player) != SoulState.VAGRANT
                || SoulManager.getPossessedEntityId(player) != null
                || hasPossessionBlockingPenance(player)) {
            return null;
        }

        Vec3 start = player.getEyePosition();
        Vec3 look = player.getLookAngle();
        Vec3 end = start.add(look.scale(POSSESSION_RANGE));
        BlockHitResult blockHit = player.level().clip(new ClipContext(
                start,
                end,
                ClipContext.Block.COLLIDER,
                ClipContext.Fluid.NONE,
                player
        ));
        double maxDistanceSqr = POSSESSION_RANGE * POSSESSION_RANGE;
        if (blockHit.getType() != HitResult.Type.MISS) {
            maxDistanceSqr = blockHit.getLocation().distanceToSqr(start);
        }

        AABB searchBox = player.getBoundingBox().expandTowards(look.scale(POSSESSION_RANGE)).inflate(1.0D);
        EntityHitResult entityHit = ProjectileUtil.getEntityHitResult(
                player.level(),
                player,
                start,
                end,
                searchBox,
                entity -> entity instanceof LivingEntity && entity.isPickable()
        );
        if (entityHit == null || entityHit.getLocation().distanceToSqr(start) > maxDistanceSqr) {
            return null;
        }

        if (!(entityHit.getEntity() instanceof LivingEntity target)) {
            return null;
        }
        return canPossess(player, target) ? target : null;
    }

    @SubscribeEvent
    public static void onEntityJoinLevel(EntityJoinLevelEvent event) {
        if (event.getLevel().isClientSide()
                || !(event.getEntity() instanceof AbstractArrow arrow)
                || !(arrow.getOwner() instanceof ServerPlayer player)) {
            return;
        }

        UUID hostId = SoulManager.getPossessedEntityId(player);
        LivingEntity host = hostId == null ? null : findHost(player, hostId);
        if (host != null) {
            arrow.setOwner(host);
            if (host.getType().is(ModTags.ARROW_GENERATORS)) {
                arrow.pickup = AbstractArrow.Pickup.CREATIVE_ONLY;
            }
        }
    }

    @SubscribeEvent
    public static void onPossessedPlayerTeleport(EntityTeleportEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }

        UUID possessedId = SoulManager.getPossessedEntityId(player);
        if (possessedId == null) {
            return;
        }

        LivingEntity host = findHost(player, possessedId);
        if (host == null
                || !host.isAlive()
                || !player.getUUID().equals(getPossessor(host))
                || host.level() != player.level()) {
            return;
        }

        host.teleportTo(event.getTargetX(), event.getTargetY(), event.getTargetZ());
        host.setDeltaMovement(Vec3.ZERO);
        host.fallDistance = 0.0F;
        host.hasImpulse = true;
    }

    @SubscribeEvent
    public static void onLivingChangeTarget(LivingChangeTargetEvent event) {
        LivingEntity attacker = event.getEntity();
        LivingEntity target = event.getNewTarget();
        if (attacker instanceof Monster monster
                && monster.getPersistentData().getLong(PROVOKED_BY_POSSESSED_HOST_UNTIL) == Long.MAX_VALUE
                && (target == null || !monster.getPersistentData().hasUUID(PROVOKED_BY_POSSESSED_HOST)
                || !monster.getPersistentData().getUUID(PROVOKED_BY_POSSESSED_HOST).equals(target.getUUID()))) {
            monster.getPersistentData().remove(PROVOKED_BY_POSSESSED_HOST);
            monster.getPersistentData().remove(PROVOKED_BY_POSSESSED_HOST_UNTIL);
        }
        if (target != null && shouldCancelPossessedTarget(attacker, target)) {
            event.setCanceled(true);
            event.setNewTarget(null);
            if (attacker instanceof Mob mob) {
                mob.setTarget(null);
            }
        }
    }

    @SubscribeEvent
    public static void onPossessedPlayerAttacked(LivingAttackEvent event) {
        if (event.getEntity() instanceof ServerPlayer player
                && SoulManager.getPossessedEntityId(player) != null) {
            event.setCanceled(true);
        }

        if (isCreativePossessedHostProtected(event.getEntity())
                && !event.getSource().is(DamageTypes.GENERIC_KILL)) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onBeta16PlayerBodyFall(LivingFallEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }

        LivingEntity host = getBeta16PlayerBodyHost(player);
        if (host == null) {
            return;
        }

        event.setCanceled(true);
        float distance = event.getDistance();
        float damageMultiplier = event.getDamageMultiplier();
        player.fallDistance = 0.0F;
        host.fallDistance = distance;
        if (distance > 0.0F && damageMultiplier > 0.0F) {
            host.causeFallDamage(distance, damageMultiplier, host.damageSources().fall());
        }
        host.fallDistance = 0.0F;
    }

    @SubscribeEvent
    public static void onPossessedBodyFall(LivingFallEvent event) {
        LivingEntity host = event.getEntity();
        if (getPossessor(host) == null) {
            return;
        }

        EntityMobilityManager.MobilityProfile mobility = EntityMobilityManager.get(host.getType());
        if (!mobility.canFly() && mobility.fallSpeedModifier() <= 0.0D) {
            return;
        }

        host.fallDistance = 0.0F;
        event.setDistance(0.0F);
        event.setDamageMultiplier(0.0F);
        event.setCanceled(true);
    }

    @SubscribeEvent
    public static void onPossessedHostHurt(LivingHurtEvent event) {
        // Possessed bodies are allowed to die. The possessor is protected by onPossessedPlayerAttacked.
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onBeta16PlayerBodyKnockback(LivingKnockBackEvent event) {
        LivingEntity host = event.getEntity();
        if (!usesBeta16PlayerBody(host)) {
            return;
        }

        ServerPlayer player = beta16Possessor(host);
        if (player == null) {
            return;
        }

        event.setCanceled(true);
        applyKnockbackImpulseToPlayer(player, event.getStrength(), event.getRatioX(), event.getRatioZ());
        syncBeta16HostToPlayer(player, host);
    }

    @SubscribeEvent
    public static void onHostDeath(LivingDeathEvent event) {
        if (event.getEntity().level().isClientSide() || event.getEntity().getServer() == null) {
            return;
        }

        if (event.getEntity().getPersistentData().getBoolean(POSSESSED)) {
            UUID possessorId = getPossessor(event.getEntity());
            ServerPlayer possessor = possessorId == null
                    ? null
                    : event.getEntity().getServer().getPlayerList().getPlayer(possessorId);
            if (possessor != null) {
                ModCriteria.ENTITY_DIED_AFTER_POSSESSION.trigger(
                        possessor,
                        event.getEntity(),
                        event.getSource().getEntity() == event.getEntity()
                );
                applyHostDeathAttrition(possessor);
            }
            UUID hostId = event.getEntity().getUUID();
            for (ServerPlayer player : event.getEntity().getServer().getPlayerList().getPlayers()) {
                if (hostId.equals(SoulManager.getPossessedEntityId(player))) {
                    PlayerSoulEvents.rememberPossessedBodyDeath(
                            player,
                            event.getEntity().level().dimension(),
                            event.getEntity().position(),
                            event.getEntity().getYRot(),
                            event.getEntity().getXRot()
                    );
                    player.stopRiding();
                    if (DeathResurrectionManager.tryHandlePossessedHostDeath(
                            player,
                            event.getEntity(),
                            event.getSource()
                    )) {
                        event.setCanceled(true);
                        return;
                    }
                    releaseFromHostDeath(player, event.getEntity());
                }
            }
            return;
        }

        UUID possessorId = getPossessor(event.getEntity());
        if (possessorId == null && event.getEntity().getPersistentData().hasUUID(LAST_POSSESSOR)) {
            possessorId = event.getEntity().getPersistentData().getUUID(LAST_POSSESSOR);
        }
        ServerPlayer previousPossessor = possessorId == null
                ? null
                : event.getEntity().getServer().getPlayerList().getPlayer(possessorId);
        if (previousPossessor != null) {
            ModCriteria.ENTITY_DIED_AFTER_POSSESSION.trigger(
                    previousPossessor,
                    event.getEntity(),
                    event.getSource().getEntity() == event.getEntity()
            );
        }

    }

    public static void applyHostDeathAttrition(ServerPlayer possessor) {
        if (possessor.serverLevel().getLevelData().isHardcore()) {
            possessor.hurt(ModDamageSources.attritionHardcoreDeath(possessor), Float.MAX_VALUE);
        } else {
            AttritionMobEffect.apply(possessor, 1, AttritionMobEffect.DEFAULT_DURATION);
        }
    }

    private static boolean isCreativePossessedHostProtected(LivingEntity host) {
        UUID possessorId = getPossessor(host);
        if (possessorId == null || host.level().getServer() == null) {
            return false;
        }
        ServerPlayer possessor = host.level().getServer().getPlayerList().getPlayer(possessorId);
        return possessor != null && possessor.isCreative();
    }

    @SubscribeEvent
    public static void onPlayerLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            clearVagrantSenseTargets(player);
        }

        if (event.getEntity() instanceof ServerPlayer player
                && SoulManager.getPossessedEntityId(player) != null) {
            POSSESSION_TARGET_HINTS.remove(player.getUUID());
            POSSESSION_INPUTS.remove(player.getUUID());
            ACTIVE_POSSESSORS.remove(player.getUUID());
            POSSESSION_STARTED_AT.remove(player.getUUID());
            if (player.level().getGameRules().getBoolean(ModGameRules.DROP_VESSEL_ON_DISCONNECT)
                    && DroppedVesselTracker.dropVessel(player)) {
                return;
            }
            LivingEntity host = findHost(player, SoulManager.getPossessedEntityId(player));
            if (host != null) {
                suspendForDisconnect(player, host);
            }
        } else if (event.getEntity() instanceof ServerPlayer player
                && player.level().getGameRules().getBoolean(ModGameRules.DROP_VESSEL_ON_DISCONNECT)) {
            POSSESSION_TARGET_HINTS.remove(player.getUUID());
            DroppedVesselTracker.dropVessel(player);
        }
    }

    public static LivingEntity findPossessedHost(ServerPlayer player, UUID hostId) {
        return findHost(player, hostId);
    }

    public static boolean suspendForDisconnect(ServerPlayer player, LivingEntity host) {
        if (!host.getUUID().equals(SoulManager.getPossessedEntityId(player))
                || !player.getUUID().equals(getPossessor(host))) {
            return false;
        }
        if (host instanceof Mob mob) {
            mob.setPersistenceRequired();
        }
        ACTIVE_POSSESSORS.remove(player.getUUID());
        return storeDisconnectedHost(player, host);
    }

    public static LivingEntity restoreDisconnectedHost(ServerPlayer player) {
        CompoundTag data = player.getPersistentData();
        if (!data.contains(STORED_DISCONNECT_HOST, Tag.TAG_COMPOUND)) {
            return null;
        }

        CompoundTag stored = data.getCompound(STORED_DISCONNECT_HOST);
        if (!stored.contains(STORED_DISCONNECT_HOST_TYPE)
                || !stored.contains(STORED_DISCONNECT_HOST_ENTITY, Tag.TAG_COMPOUND)) {
            data.remove(STORED_DISCONNECT_HOST);
            return null;
        }

        ServerLevel level = player.serverLevel();
        if (stored.contains(STORED_DISCONNECT_HOST_DIMENSION)) {
            ServerLevel storedLevel = player.getServer().getLevel(ResourceKey.create(
                    Registries.DIMENSION,
                    new ResourceLocation(stored.getString(STORED_DISCONNECT_HOST_DIMENSION))
            ));
            if (storedLevel != null) {
                level = storedLevel;
            }
        }

        Optional<EntityType<?>> type = BuiltInRegistries.ENTITY_TYPE.getOptional(
                new ResourceLocation(stored.getString(STORED_DISCONNECT_HOST_TYPE))
        );
        if (type.isEmpty()) {
            data.remove(STORED_DISCONNECT_HOST);
            return null;
        }

        Entity entity = type.get().create(level);
        if (!(entity instanceof LivingEntity host)) {
            data.remove(STORED_DISCONNECT_HOST);
            return null;
        }

        host.load(stored.getCompound(STORED_DISCONNECT_HOST_ENTITY));
        UUID hostId = SoulManager.getPossessedEntityId(player);
        if (hostId == null
                || !hostId.equals(host.getUUID())
                || !player.getUUID().equals(getPossessor(host))
                || !level.addFreshEntity(host)) {
            data.remove(STORED_DISCONNECT_HOST);
            return null;
        }

        data.remove(STORED_DISCONNECT_HOST);
        return host;
    }

    private static boolean storeDisconnectedHost(ServerPlayer player, LivingEntity host) {
        ResourceLocation typeId = BuiltInRegistries.ENTITY_TYPE.getKey(host.getType());
        if (typeId == null) {
            return false;
        }

        CompoundTag stored = new CompoundTag();
        stored.putString(STORED_DISCONNECT_HOST_TYPE, typeId.toString());
        stored.putString(STORED_DISCONNECT_HOST_DIMENSION, host.level().dimension().location().toString());
        stored.put(STORED_DISCONNECT_HOST_ENTITY, host.saveWithoutId(new CompoundTag()));
        player.getPersistentData().put(STORED_DISCONNECT_HOST, stored);
        host.remove(Entity.RemovalReason.UNLOADED_WITH_PLAYER);
        return true;
    }

    public static boolean resumeAfterDisconnect(ServerPlayer player, LivingEntity host) {
        UUID hostId = SoulManager.getPossessedEntityId(player);
        if (hostId == null
                || !hostId.equals(host.getUUID())
                || !player.getUUID().equals(getPossessor(host))
                || !host.isAlive()) {
            return false;
        }

        if (usesBeta16PlayerBody(host)) {
            startBeta16PlayerBodyPossession(player, host);
        } else {
            syncPossessorToHost(player, host, false);
            suppressPossessorBody(player);
        }
        if (host instanceof Mob mob) {
            mob.setPersistenceRequired();
        }
        ACTIVE_POSSESSORS.put(player.getUUID(), player);
        POSSESSION_STARTED_AT.put(player.getUUID(), player.level().getGameTime());
        SoulManager.sync(player);
        return true;
    }

    public static void release(ServerPlayer player, LivingEntity host) {
        release(player, host, true);
    }

    public static void releaseForHostReplacement(ServerPlayer player, LivingEntity host) {
        release(player, host, false, false);
    }

    public static void releaseFromHostDeathForTest(ServerPlayer player, LivingEntity host) {
        releaseFromHostDeath(player, host);
    }

    public static void restorePlayerBodyFromHost(ServerPlayer player, LivingEntity host) {
        if (host == null) {
            SoulManager.clearPossession(player);
            ACTIVE_POSSESSORS.remove(player.getUUID());
            POSSESSION_INPUTS.remove(player.getUUID());
            POSSESSION_TARGET_HINTS.remove(player.getUUID());
            clearVagrantSenseTargets(player);
            restoreBeta16PlayerBodyParameters(player);
            restorePossessorBody(player);
            player.setInvisible(false);
            return;
        }

        ServerLevel level = (ServerLevel) host.level();
        Vec3 position = host.position();
        float yRot = host.getYRot();
        float xRot = host.getXRot();
        boolean consumeHost = !(host instanceof ServerPlayer);

        release(player, host, false);
        if (consumeHost && host.isAlive()) {
            host.discard();
        }
        if (player.connection != null) {
            player.teleportTo(level, position.x, position.y, position.z, yRot, xRot);
        } else {
            player.absMoveTo(position.x, position.y, position.z, yRot, xRot);
        }
        player.setInvisible(false);
    }

    public static void handleAction(ServerPlayer player, PossessionActionPacket.Action action) {
        if (player.isSpectator()) {
            return;
        }
        if (action == PossessionActionPacket.Action.RELEASE) {
            if (isWithinReleaseInputGrace(player)) {
                return;
            }
            LivingEntity host = getControlledHost(player);
            if (host != null) {
                release(player, host);
                return;
            }
            SoulLifecycleManager.splitPlayer(player, true);
            return;
        }

        LivingEntity host = getControlledHost(player);
        if (host == null) {
            return;
        }

        switch (action) {
            case JUMP -> jump(host);
            case ATTACK -> attack(player, host);
            case INTERACT -> interact(player, host);
            case RELEASE -> {
            }
        }
    }

    public static void updatePossessionInput(ServerPlayer player, PossessionInputPacket packet) {
        if (player.isSpectator()) {
            POSSESSION_INPUTS.remove(player.getUUID());
            return;
        }
        LivingEntity host = getControlledHost(player);
        if (host == null || !isFinite(packet.yRot()) || !isFinite(packet.xRot())) {
            POSSESSION_INPUTS.remove(player.getUUID());
            return;
        }
        float yRot = Mth.wrapDegrees(packet.yRot());
        float xRot = Math.max(-90.0F, Math.min(90.0F, packet.xRot()));
        player.setYRot(yRot);
        player.setYHeadRot(yRot);
        player.setXRot(xRot);
        if (usesBeta16PlayerBody(host)) {
            player.setShiftKeyDown(packet.shifting());
        }
        POSSESSION_INPUTS.put(player.getUUID(), new PossessionInput(
                clampInput(packet.forward()),
                clampInput(packet.strafe()),
                packet.jumping(),
                packet.shifting(),
                yRot,
                xRot
        ));
    }

    public static LivingEntity getBeta16PlayerBodyHost(Player player) {
        UUID hostId = SoulManager.getPossessedEntityId(player);
        if (hostId == null) {
            return null;
        }
        for (Entity entity : player.level().getEntities(
                player,
                player.getBoundingBox().inflate(128.0D),
                entity -> hostId.equals(entity.getUUID())
        )) {
            if (entity instanceof LivingEntity living && usesBeta16PlayerBody(living)) {
                return living;
            }
        }
        return null;
    }

    public static boolean isUsingBeta16PlayerBody(Player player) {
        return getBeta16PlayerBodyHost(player) != null;
    }

    public static boolean usesBeta16PlayerBody(LivingEntity host) {
        return host != null && (host.getType().is(ModTags.HUMANOID_SKELETONS) || host instanceof Blaze);
    }

    public static void syncBeta16HostToPlayer(Player player, LivingEntity host) {
        if (player == null || host == null || !usesBeta16PlayerBody(host)) {
            return;
        }
        double previousX = host.getX();
        double previousY = host.getY();
        double previousZ = host.getZ();
        if (player instanceof ServerPlayer serverPlayer) {
            updateBeta16PlayerBodyParameters(serverPlayer, host);
        }
        host.setYRot(player.getYRot());
        host.setYHeadRot(player.getYRot());
        host.yBodyRot = player.getYRot();
        host.setXRot(player.getXRot());
        host.setShiftKeyDown(player.isShiftKeyDown());
        host.setSprinting(player.isSprinting());
        host.setDeltaMovement(player.getDeltaMovement());
        host.absMoveTo(player.getX(), player.getY(), player.getZ(), player.getYRot(), player.getXRot());
        syncBeta16HostItemUse(player, host);
        if (host.distanceToSqr(previousX, previousY, previousZ) < 0.25D) {
            host.xo = previousX;
            host.yo = previousY;
            host.zo = previousZ;
        } else {
            host.xo = host.getX();
            host.yo = host.getY();
            host.zo = host.getZ();
        }
        host.fallDistance = player.fallDistance;
        host.horizontalCollision = player.horizontalCollision;
        host.verticalCollision = player.verticalCollision;
        host.verticalCollisionBelow = player.verticalCollisionBelow;
        host.setOnGround(player.onGround());
        host.hasImpulse = true;
    }

    private static void syncBeta16HostItemUse(Player player, LivingEntity host) {
        if (player.isUsingItem()) {
            InteractionHand hand = player.getUsedItemHand();
            ItemStack hostStack = host.getItemInHand(hand);
            if (!host.isUsingItem()
                    || host.getUsedItemHand() != hand
                    || !ItemStack.isSameItemSameTags(host.getUseItem(), hostStack)) {
                LivingEntityAccessor accessor = (LivingEntityAccessor) host;
                accessor.requiemunofficial$setUseItem(hostStack);
                accessor.requiemunofficial$setLivingEntityFlag(1, true);
                accessor.requiemunofficial$setLivingEntityFlag(2, hand == InteractionHand.OFF_HAND);
            }
            ((LivingEntityAccessor) host).requiemunofficial$setUseItemRemaining(
                    Math.max(2, hostStack.getUseDuration() + 1)
            );
            if (host instanceof AbstractSkeleton skeleton && hostStack.getItem() instanceof BowItem) {
                skeleton.setAggressive(true);
            }
            return;
        }

        if (host.isUsingItem()) {
            LivingEntityAccessor accessor = (LivingEntityAccessor) host;
            accessor.requiemunofficial$setUseItem(ItemStack.EMPTY);
            accessor.requiemunofficial$setUseItemRemaining(0);
            accessor.requiemunofficial$setLivingEntityFlag(1, false);
        }
        if (host instanceof AbstractSkeleton skeleton && skeleton.getMainHandItem().getItem() instanceof BowItem) {
            skeleton.setAggressive(false);
        }
    }

    private static boolean isBeta16FlyingHost(LivingEntity host) {
        return EntityMobilityManager.get(host.getType()).canFly();
    }

    private static ServerPlayer beta16Possessor(LivingEntity host) {
        UUID possessorId = getPossessor(host);
        if (possessorId == null) {
            return null;
        }
        ServerPlayer player = ACTIVE_POSSESSORS.get(possessorId);
        if (player == null && host.getServer() != null) {
            player = host.getServer().getPlayerList().getPlayer(possessorId);
        }
        if (player == null
                || !host.getUUID().equals(SoulManager.getPossessedEntityId(player))
                || !usesBeta16PlayerBody(host)) {
            return null;
        }
        return player;
    }

    private static void applyKnockbackImpulseToPlayer(ServerPlayer player,
                                                      float strength,
                                                      double ratioX,
                                                      double ratioZ) {
        if (strength <= 0.0F) {
            return;
        }
        Vec3 direction = new Vec3(ratioX, 0.0D, ratioZ);
        if (direction.lengthSqr() < 1.0E-8D) {
            return;
        }

        Vec3 current = player.getDeltaMovement();
        Vec3 impulse = direction.normalize().scale(strength);
        player.setDeltaMovement(
                current.x / 2.0D - impulse.x,
                player.onGround() ? Math.min(0.4D, current.y / 2.0D + strength) : current.y,
                current.z / 2.0D - impulse.z
        );
        player.hasImpulse = true;
        if (player.connection != null) {
            player.connection.send(new ClientboundSetEntityMotionPacket(player));
        }
    }

    @SubscribeEvent
    public static void onCommand(CommandEvent event) {
        CommandSourceStack source = event.getParseResults().getContext().getSource();
        String input = event.getParseResults().getReader().getString();
        String command = input.startsWith("/") ? input.substring(1) : input;
        String root = command.stripLeading().split("\\s+", 2)[0];
        if (!root.equals("kill") && !root.equals("damage") && !root.equals("effect")) {
            return;
        }

        com.mojang.brigadier.context.CommandContext<CommandSourceStack> context;
        try {
            context = event.getParseResults().getContext().build(command);
        } catch (RuntimeException ignored) {
            return;
        }

        try {
            if (root.equals("kill") && redirectKillCommand(context, source)) {
                event.setCanceled(true);
            } else if (root.equals("damage") && redirectDamageCommand(context, source)) {
                event.setCanceled(true);
            } else if (root.equals("effect") && redirectEffectCommand(context, command, source)) {
                event.setCanceled(true);
            }
        } catch (Exception ignored) {
            // Let vanilla handle commands that do not match the narrow possessed-host compatibility path.
        }
    }

    private static boolean redirectKillCommand(com.mojang.brigadier.context.CommandContext<CommandSourceStack> context,
                                               CommandSourceStack source) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        Collection<? extends Entity> targets = hasArgument(context, "targets")
                ? EntityArgument.getEntities(context, "targets")
                : Collections.singletonList(source.getEntityOrException());
        RedirectedTargets redirected = redirectPossessedTargets(targets);
        if (!redirected.changed()) {
            return false;
        }

        int killed = 0;
        for (Entity target : redirected.targets()) {
            if (target instanceof LivingEntity living && living.isAlive()) {
                living.kill();
                killed++;
            } else {
                target.kill();
                killed++;
            }
        }
        if (killed <= 0) {
            source.sendFailure(Component.translatable("commands.kill.failed"));
        } else if (killed == 1) {
            source.sendSuccess(() -> Component.translatable(
                    "commands.kill.success.single",
                    redirected.targets().iterator().next().getDisplayName()
            ), true);
        } else {
            int count = killed;
            source.sendSuccess(() -> Component.translatable("commands.kill.success.multiple", count), true);
        }
        return true;
    }

    private static boolean redirectDamageCommand(com.mojang.brigadier.context.CommandContext<CommandSourceStack> context,
                                                 CommandSourceStack source) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        Entity target = EntityArgument.getEntity(context, "target");
        RedirectedTargets redirected = redirectPossessedTargets(Collections.singletonList(target));
        if (!redirected.changed() || redirected.targets().size() != 1) {
            return false;
        }
        Entity redirectedTarget = redirected.targets().iterator().next();
        if (!(redirectedTarget instanceof LivingEntity host)) {
            return false;
        }
        float amount = com.mojang.brigadier.arguments.FloatArgumentType.getFloat(context, "amount");
        DamageSource damageSource = getCommandDamageSource(context, source);
        boolean damaged = host.hurt(damageSource, amount);
        if (damaged) {
            source.sendSuccess(() -> Component.translatable("commands.damage.success", amount, host.getDisplayName()), true);
        } else {
            source.sendFailure(Component.translatable("commands.damage.invulnerable", host.getDisplayName()));
        }
        return true;
    }

    private static boolean redirectEffectCommand(com.mojang.brigadier.context.CommandContext<CommandSourceStack> context,
                                                 String command,
                                                 CommandSourceStack source) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        Collection<? extends Entity> targets = EntityArgument.getEntities(context, "targets");
        RedirectedTargets redirected = redirectPossessedTargets(targets);
        if (!redirected.changed()) {
            return false;
        }
        List<LivingEntity> livingTargets = redirected.targets().stream()
                .filter(LivingEntity.class::isInstance)
                .map(LivingEntity.class::cast)
                .filter(LivingEntity::isAlive)
                .toList();
        String trimmed = command.stripLeading();
        if (trimmed.startsWith("effect clear ")) {
            int changed = 0;
            if (hasArgument(context, "effect")) {
                net.minecraft.world.effect.MobEffect effect = ResourceArgument.getMobEffect(context, "effect").value();
                for (LivingEntity target : livingTargets) {
                    if (target.removeEffect(effect)) {
                        changed++;
                    }
                }
            } else {
                for (LivingEntity target : livingTargets) {
                    if (target.removeAllEffects()) {
                        changed++;
                    }
                }
            }
            if (changed <= 0) {
                source.sendFailure(Component.translatable("commands.effect.clear.everything.failed"));
            } else {
                int count = changed;
                source.sendSuccess(() -> Component.translatable("commands.effect.clear.everything.success.multiple", count), true);
            }
            return true;
        }
        if (!trimmed.startsWith("effect give ")) {
            return false;
        }
        net.minecraft.world.effect.MobEffect effect = ResourceArgument.getMobEffect(context, "effect").value();
        int seconds = hasArgument(context, "seconds")
                ? com.mojang.brigadier.arguments.IntegerArgumentType.getInteger(context, "seconds")
                : 30;
        int amplifier = hasArgument(context, "amplifier")
                ? com.mojang.brigadier.arguments.IntegerArgumentType.getInteger(context, "amplifier")
                : 0;
        boolean hideParticles = hasArgument(context, "hideParticles")
                && com.mojang.brigadier.arguments.BoolArgumentType.getBool(context, "hideParticles");
        int duration = effect.isInstantenous()
                ? seconds
                : (trimmed.contains(" infinite") ? MobEffectInstance.INFINITE_DURATION : seconds * 20);
        int changed = 0;
        for (LivingEntity target : livingTargets) {
            if (effect.isInstantenous()) {
                effect.applyInstantenousEffect(source.getEntity(), source.getEntity(), target, amplifier, 1.0D);
                changed++;
            } else if (target.addEffect(new MobEffectInstance(effect, duration, amplifier, false, !hideParticles))) {
                changed++;
            }
        }
        if (changed <= 0) {
            source.sendFailure(Component.translatable("commands.effect.give.failed"));
        } else {
            int count = changed;
            source.sendSuccess(() -> Component.translatable("commands.effect.give.success.multiple", effect.getDisplayName(), count, seconds), true);
        }
        return true;
    }

    private static RedirectedTargets redirectPossessedTargets(Collection<? extends Entity> targets) {
        List<Entity> redirected = new ArrayList<>();
        boolean changed = false;
        LinkedHashSet<UUID> seen = new LinkedHashSet<>();
        for (Entity target : targets) {
            Entity effectiveTarget = target;
            if (target instanceof ServerPlayer player) {
                LivingEntity host = findPossessedHost(player);
                if (host != null && host.isAlive()) {
                    effectiveTarget = host;
                    changed = true;
                }
            }
            if (seen.add(effectiveTarget.getUUID())) {
                redirected.add(effectiveTarget);
            }
        }
        return new RedirectedTargets(redirected, changed);
    }

    private static DamageSource getCommandDamageSource(
            com.mojang.brigadier.context.CommandContext<CommandSourceStack> context,
            CommandSourceStack source
    ) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        Holder.Reference<DamageType> type = hasArgument(context, "damageType")
                ? ResourceArgument.getResource(context, "damageType", Registries.DAMAGE_TYPE)
                : source.getLevel().registryAccess().registryOrThrow(Registries.DAMAGE_TYPE)
                .getHolderOrThrow(DamageTypes.GENERIC);
        Vec3 position = hasArgument(context, "location") ? Vec3Argument.getVec3(context, "location") : null;
        Entity direct = null;
        Entity causing = null;
        if (hasArgument(context, "entity")) {
            direct = EntityArgument.getEntity(context, "entity");
            causing = direct;
        }
        if (hasArgument(context, "cause")) {
            causing = EntityArgument.getEntity(context, "cause");
        }
        return new DamageSource(type, direct, causing, position);
    }

    private static boolean hasArgument(com.mojang.brigadier.context.CommandContext<CommandSourceStack> context,
                                       String name) {
        return context.getNodes().stream().anyMatch(node -> node.getNode().getName().equals(name));
    }

    private record RedirectedTargets(List<Entity> targets, boolean changed) {
    }

    private static LivingEntity getControlledHost(ServerPlayer player) {
        if (SoulManager.getState(player) != SoulState.VAGRANT) {
            return null;
        }
        if (player.isSpectator()) {
            return null;
        }
        UUID possessedId = SoulManager.getPossessedEntityId(player);
        if (possessedId == null) {
            return null;
        }
        LivingEntity host = findHost(player, possessedId);
        if (host == null
                || !possessedId.equals(host.getUUID())
                || !player.getUUID().equals(getPossessor(host))
                || !host.isAlive()) {
            return null;
        }
        return host;
    }

    private static void jump(LivingEntity host) {
        EntityMobilityManager.MobilityProfile mobility = EntityMobilityManager.get(host.getType());
        jump(host, mobility);
    }

    private static void jump(LivingEntity host, EntityMobilityManager.MobilityProfile mobility) {
        boolean verticalMovement = canMoveVertically(host, mobility);
        if ((!isGroundedForJump(host) && !verticalMovement) || host.getType().is(ModTags.IMMOVABLE)) {
            return;
        }

        if (!verticalMovement && usesVanillaGroundJump(host)) {
            ((LivingEntityAccessor) host).requiemunofficial$jumpFromGround();
            startAlexJump(host);
            host.hasImpulse = true;
            return;
        }

        double jumpPower = jumpPower(host, mobility);
        Vec3 movement = host.getDeltaMovement();
        host.setDeltaMovement(
                movement.x,
                verticalMovement ? jumpPower : Math.max(movement.y, jumpPower),
                movement.z
        );
        startAlexJump(host);
        host.hasImpulse = true;
    }

    private static boolean usesVanillaGroundJump(LivingEntity host) {
        EntityType<?> type = host.getType();
        return type != EntityType.FROG
                && !(host instanceof AbstractHorse)
                && type != EntityType.RABBIT
                && type != EntityType.SLIME
                && type != EntityType.MAGMA_CUBE
                && alexPath(type) == null;
    }

    private static double jumpPower(LivingEntity host, EntityMobilityManager.MobilityProfile mobility) {
        EntityType<?> type = host.getType();
        String alexPath = alexPath(type);
        if (type == EntityType.FROG) {
            return 0.72D;
        }
        if (host instanceof AbstractHorse horse) {
            return Math.max(0.42D, horse.getCustomJump());
        }
        if (type == EntityType.RABBIT) {
            return 0.58D;
        }
        if (type == EntityType.SLIME || type == EntityType.MAGMA_CUBE) {
            return 0.52D;
        }
        if (alexPath != null) {
            if (alexPath.equals("kangaroo")) {
                return 0.78D;
            }
            if (alexPath.equals("bunfungus") || alexPath.equals("rocky_roller") || alexPath.equals("warped_toad")) {
                return 0.62D;
            }
            if (alexPath.equals("jerboa") || alexPath.equals("rain_frog") || alexPath.equals("mudskipper")) {
                return 0.55D;
            }
            if (alexPath.equals("elephant") || alexPath.equals("rhinoceros") || alexPath.equals("moose")) {
                return 0.36D;
            }
        }
        if (mobility.walkMode() == EntityMobilityManager.WalkMode.JUMPY) {
            return 0.52D;
        }
        return 0.42D;
    }

    private static boolean isGroundedForJump(LivingEntity host) {
        return host.onGround();
    }

    private static void attack(ServerPlayer player, LivingEntity host) {
        long gameTime = host.level().getGameTime();
        if (host.getPersistentData().getLong(NEXT_ATTACK_TIME) > gameTime) {
            return;
        }

        LivingEntity target = findAttackTarget(player, host, getAttackReach(host));
        if (target == null || !host.hasLineOfSight(target)) {
            if (tryForwardSpecialAttack(host)) {
                host.getPersistentData().putLong(NEXT_ATTACK_TIME, gameTime + getAttackCooldown(host));
                host.swing(InteractionHand.MAIN_HAND, true);
            }
            return;
        }

        host.getPersistentData().putLong(NEXT_ATTACK_TIME, gameTime + getAttackCooldown(host));
        boolean hit;
        boolean vanillaMobAttack = false;
        if (trySpecialAttack(player, host, target)) {
            hit = true;
        } else if (host instanceof Mob mob) {
            vanillaMobAttack = true;
            hit = mob.doHurtTarget(target) || tryAttributeAttack(host, target);
        } else {
            hit = tryAttributeAttack(host, target);
        }
        if (hit) {
            markProvokedByPossessedHost(host, target);
            ModCriteria.POSSESSED_HIT_ENTITY.trigger(player, host, target);
        }
        if (!vanillaMobAttack) {
            host.swing(InteractionHand.MAIN_HAND, true);
        }
    }

    private static boolean tryAttributeAttack(LivingEntity host, LivingEntity target) {
        double attackDamage = host.getAttributeValue(Attributes.ATTACK_DAMAGE);
        if (attackDamage <= 0.0D && host instanceof Mob) {
            return false;
        }

        float damage = (float) Math.max(1.0D, attackDamage);
        boolean hit = target.hurt(host.damageSources().mobAttack(host), damage);
        if (hit && target instanceof Mob sensor) {
            PossessionDetectionManager.attemptDetection(
                    host,
                    sensor,
                    PossessionDetectionManager.DetectionReason.ATTACKED
            );
        }
        return hit;
    }

    private static boolean tryForwardSpecialAttack(LivingEntity host) {
        EntityType<?> type = host.getType();
        if (type == EntityType.BLAZE) {
            startBlazeAttack(host, null);
            return true;
        }
        if (type == EntityType.GHAST) {
            shootLargeFireball(host, 1);
            return true;
        }
        if (type == EntityType.SNOW_GOLEM && host instanceof SnowGolem snowGolem) {
            shootSnowballForward(snowGolem);
            return true;
        }

        String alexPath = alexPath(type);
        if (alexPath == null) {
            return false;
        }
        if (alexPath.equals("sunbird")) {
            shootSmallFireball(host, 0.2D);
            return true;
        }
        if (alexPath.equals("straddler")) {
            shootLargeFireball(host, 1);
            return true;
        }
        if (alexPath.equals("guster")) {
            gustForward(host);
            return true;
        }
        return false;
    }

    private static void interact(ServerPlayer player, LivingEntity host) {
        long gameTime = host.level().getGameTime();
        if (host.getPersistentData().getLong(NEXT_INTERACT_TIME) > gameTime) {
            return;
        }
        EntityType<?> type = host.getType();
        if (type == EntityType.ENDERMAN) {
            if (blinkForward(host)) {
                host.getPersistentData().putLong(NEXT_INTERACT_TIME, gameTime + 20L);
            }
            return;
        }
        if (host instanceof Axolotl axolotl) {
            if (playDead(axolotl)) {
                host.getPersistentData().putLong(NEXT_INTERACT_TIME, gameTime + 600L);
            }
            return;
        }
        if (host instanceof Shulker shulker) {
            toggleShulkerPeek(shulker);
            host.getPersistentData().putLong(NEXT_INTERACT_TIME, gameTime + 20L);
            return;
        }
        if (host instanceof Evoker evoker) {
            summonVexes(evoker);
            host.getPersistentData().putLong(NEXT_INTERACT_TIME, gameTime + 340L);
            return;
        }
        if (host instanceof SnowGolem snowGolem) {
            shootSnowballForward(snowGolem);
            host.getPersistentData().putLong(NEXT_INTERACT_TIME, gameTime + 20L);
            return;
        }

        String alexPath = alexPath(type);
        if (alexPath == null) {
            return;
        }
        if (ALEX_TELEPORTERS.contains(alexPath) || alexPath.equals("farseer")) {
            if (blinkForward(host)) {
                host.getPersistentData().putLong(NEXT_INTERACT_TIME, gameTime + 40L);
            }
            return;
        }
        if (alexPath.equals("guster")) {
            gustForward(host);
            host.getPersistentData().putLong(NEXT_INTERACT_TIME, gameTime + 30L);
            return;
        }
        if (alexPath.equals("kangaroo")) {
            leapForward(host, 1.2D, 0.7D);
            host.getPersistentData().putLong(NEXT_INTERACT_TIME, gameTime + 30L);
            return;
        }
        if (alexPath.equals("roadrunner") || alexPath.equals("gazelle")) {
            leapForward(host, 1.8D, 0.25D);
            host.getPersistentData().putLong(NEXT_INTERACT_TIME, gameTime + 40L);
        }
    }

    private static long getAttackCooldown(LivingEntity host) {
        EntityType<?> type = host.getType();
        String alexPath = alexPath(type);
        if (alexPath != null) {
            if (alexPath.equals("sunbird")
                    || alexPath.equals("guster")
                    || alexPath.equals("straddler")
                    || ALEX_TELEPORTERS.contains(alexPath)) {
                return 24L;
            }
            if (ALEX_HEAVY_ATTACKERS.contains(alexPath) || ALEX_LONG_REACH.contains(alexPath)) {
                return 22L;
            }
            if (ALEX_POISON_ATTACKERS.contains(alexPath) || ALEX_SLOWING_ATTACKERS.contains(alexPath)) {
                return 18L;
            }
        }
        if (type == EntityType.GHAST || type == EntityType.EVOKER) {
            return 30L;
        }
        if (type == EntityType.GUARDIAN || type == EntityType.ELDER_GUARDIAN) {
            return 80L;
        }
        if (type == EntityType.WITCH || type == EntityType.RAVAGER || type == EntityType.GOAT
                || type == EntityType.IRON_GOLEM || type == EntityType.POLAR_BEAR) {
            return 24L;
        }
        if (type == EntityType.SNOW_GOLEM) {
            return 20L;
        }
        if (type.is(ModTags.HUMANOID_SKELETONS) || type.is(ModTags.HUMANOID_ZOMBIES)) {
            return 20L;
        }
        if (type == EntityType.BLAZE || type == EntityType.LLAMA || type == EntityType.TRADER_LLAMA
                || type == EntityType.SHULKER) {
            return 20L;
        }
        if (type == EntityType.ENDERMAN) {
            return 80L;
        }
        return 10L;
    }

    private static double getAttackReach(LivingEntity host) {
        EntityType<?> type = host.getType();
        String alexPath = alexPath(type);
        if (alexPath != null) {
            if (alexPath.equals("sunbird") || alexPath.equals("guster") || alexPath.equals("straddler")) {
                return 15.0D;
            }
            if (alexPath.equals("crimson_mosquito") || alexPath.equals("tarantula_hawk")) {
                return 5.5D;
            }
            if (ALEX_LONG_REACH.contains(alexPath)) {
                return 6.5D;
            }
            if (alexPath.equals("elephant") || alexPath.equals("rhinoceros") || alexPath.equals("void_worm")) {
                return 7.5D;
            }
        }
        if (type == EntityType.GHAST) {
            return 32.0D;
        }
        if (type == EntityType.SHULKER) {
            return 16.0D;
        }
        if (type == EntityType.GUARDIAN || type == EntityType.ELDER_GUARDIAN
                || type == EntityType.EVOKER || type == EntityType.BLAZE) {
            return 15.0D;
        }
        if (type == EntityType.LLAMA || type == EntityType.TRADER_LLAMA) {
            return 10.0D;
        }
        if (type == EntityType.WITCH || type == EntityType.SNOW_GOLEM) {
            return 10.0D;
        }
        if (type.is(ModTags.HUMANOID_SKELETONS)) {
            return 15.0D;
        }
        if (type == EntityType.IRON_GOLEM || type == EntityType.RAVAGER) {
            return 5.5D;
        }
        return 4.5D;
    }

    public static boolean trySpecialAttackForTest(ServerPlayer player, LivingEntity host, LivingEntity target) {
        return trySpecialAttack(player, host, target);
    }

    private static boolean trySpecialAttack(ServerPlayer player, LivingEntity host, LivingEntity target) {
        EntityType<?> type = host.getType();
        if (tryAlexMobsSpecialAttack(player, host, target, type)) {
            return true;
        }
        if (type == EntityType.BLAZE) {
            startBlazeAttack(host, target);
            return true;
        }
        if (type == EntityType.GHAST) {
            shootLargeFireball(host, 1);
            return true;
        }
        if ((type == EntityType.GUARDIAN || type == EntityType.ELDER_GUARDIAN)
                && host instanceof Guardian guardian) {
            startGuardianBeam(guardian, target);
            return true;
        }
        if (type == EntityType.EVOKER) {
            spawnEvokerFangs(host, target);
            target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 60, 1));
            return true;
        }
        if ((type == EntityType.WITCH || type == EntityType.SNOW_GOLEM)
                && host instanceof RangedAttackMob rangedAttackMob) {
            rangedAttackMob.performRangedAttack(target, 1.0F);
            return true;
        }
        if (type == EntityType.FROG && host instanceof Frog frog) {
            return frogTongueAttack(frog, target, player);
        }
        if (type == EntityType.PUFFERFISH && host instanceof Pufferfish pufferfish) {
            togglePufferfishInflation(pufferfish);
            target.addEffect(new MobEffectInstance(MobEffects.POISON, 80, Math.max(0, pufferfish.getPuffState() - 1)));
            return target.hurt(player.damageSources().mobAttack(host), 2.0F + pufferfish.getPuffState());
        }
        if (type == EntityType.BEE || type == EntityType.CAVE_SPIDER) {
            target.addEffect(new MobEffectInstance(MobEffects.POISON, 120, 0));
            return target.hurt(player.damageSources().mobAttack(host), 2.0F);
        }
        if (type == EntityType.IRON_GOLEM) {
            knockTarget(host, target, 1.0D, 0.8D);
            return target.hurt(player.damageSources().mobAttack(host), 10.0F);
        }
        if (type == EntityType.POLAR_BEAR) {
            knockTarget(host, target, 1.0D, 0.35D);
            return target.hurt(player.damageSources().mobAttack(host), 6.0F);
        }
        if (type == EntityType.GOAT || type == EntityType.HOGLIN || type == EntityType.RAVAGER) {
            ramTowardTarget(host, target, type);
            knockTarget(host, target, type == EntityType.RAVAGER ? 1.8D : 1.2D, 0.45D);
            return target.hurt(player.damageSources().mobAttack(host), type == EntityType.RAVAGER ? 9.0F : 6.0F);
        }
        if ((type == EntityType.LLAMA || type == EntityType.TRADER_LLAMA) && host instanceof Llama llama) {
            spawnLlamaSpit(llama, target);
            return true;
        }
        if (type == EntityType.SHULKER && host instanceof Shulker shulker) {
            host.level().addFreshEntity(new ShulkerBullet(
                    host.level(),
                    shulker,
                    target,
                    shulker.getAttachFace().getAxis()
            ));
            host.level().playSound(
                    null,
                    host.blockPosition(),
                    SoundEvents.SHULKER_SHOOT,
                    SoundSource.HOSTILE,
                    2.0F,
                    (host.getRandom().nextFloat() - host.getRandom().nextFloat()) * 0.2F + 1.0F
            );
            return true;
        }
        return false;
    }

    private static boolean tryAlexMobsSpecialAttack(ServerPlayer player,
                                                    LivingEntity host,
                                                    LivingEntity target,
                                                    EntityType<?> type) {
        String path = alexPath(type);
        if (path == null) {
            return false;
        }

        triggerAlexAttackCue(host, path);
        if (path.equals("sunbird")) {
            shootSmallFireball(host, 0.2D);
            return true;
        }
        if (path.equals("straddler")) {
            shootLargeFireball(host, 1);
            return true;
        }
        if (path.equals("guster")) {
            knockTarget(host, target, 1.6D, 0.6D);
            target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 50, 0));
            return target.hurt(player.damageSources().mobAttack(host), alexAttackDamage(host, 4.0F));
        }
        if (path.equals("kangaroo")) {
            triggerAlexAnimation(host, host.getRandom().nextBoolean() ? "ANIMATION_KICK" : "ANIMATION_PUNCH_R");
            knockTarget(host, target, 1.05D, 0.8D);
            return target.hurt(player.damageSources().mobAttack(host), alexAttackDamage(host, 3.0F));
        }
        if (ALEX_TELEPORTERS.contains(path)) {
            blinkNearTarget(host, target);
            return target.hurt(player.damageSources().mobAttack(host), alexAttackDamage(host, 3.0F));
        }

        boolean handled = false;
        if (ALEX_POISON_ATTACKERS.contains(path)) {
            target.addEffect(new MobEffectInstance(MobEffects.POISON, 100, path.equals("rattlesnake") ? 1 : 0));
            handled = true;
        }
        if (ALEX_SLOWING_ATTACKERS.contains(path)) {
            target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 80, 1));
            handled = true;
        }
        if (path.equals("skunk")) {
            target.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 80, 0));
            target.addEffect(new MobEffectInstance(MobEffects.CONFUSION, 120, 0));
            handled = true;
        }
        if (path.equals("soul_vulture")) {
            target.addEffect(new MobEffectInstance(MobEffects.WITHER, 100, 0));
            handled = true;
        }
        if (path.equals("crimson_mosquito")) {
            target.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 100, 0));
            host.heal(2.0F);
            handled = true;
        }
        if (path.equals("cachalot_whale") || path.equals("orca")) {
            knockTarget(host, target, 1.2D, 0.15D);
            target.setDeltaMovement(target.getDeltaMovement().add(0.0D, -0.25D, 0.0D));
            handled = true;
        }
        if (path.equals("alligator_snapping_turtle")
                || path.equals("anaconda")
                || path.equals("crocodile")
                || path.equals("giant_squid")
                || path.equals("mimic_octopus")) {
            pullTargetToHost(host, target, 0.85D);
            target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 60, 2));
            handled = true;
        }
        if (path.equals("froststalker")) {
            target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 120, 2));
            target.setTicksFrozen(Math.min(target.getTicksRequiredToFreeze() + 60, target.getTicksFrozen() + 80));
            handled = true;
        }
        if (path.equals("skreecher")) {
            target.addEffect(new MobEffectInstance(MobEffects.DARKNESS, 80, 0));
            target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 80, 1));
            handled = true;
        }
        if (ALEX_CHARGING_ATTACKERS.contains(path)) {
            ramTowardTarget(host, target, type);
            handled = true;
        }
        if (ALEX_HEAVY_ATTACKERS.contains(path)) {
            knockTarget(host, target, heavyKnockback(path), heavyLift(path));
            handled = true;
        }

        if (!handled) {
            return false;
        }
        return target.hurt(player.damageSources().mobAttack(host), alexAttackDamage(host, 2.0F));
    }

    private static float alexAttackDamage(LivingEntity host, float fallback) {
        return (float) Math.max(fallback, host.getAttributeValue(Attributes.ATTACK_DAMAGE));
    }

    private static double heavyKnockback(String path) {
        if (path.equals("rhinoceros") || path.equals("elephant") || path.equals("void_worm")) {
            return 1.8D;
        }
        if (path.equals("bison") || path.equals("tusklin") || path.equals("laviathan")) {
            return 1.4D;
        }
        return 1.0D;
    }

    private static double heavyLift(String path) {
        if (path.equals("kangaroo") || path.equals("mantis_shrimp")) {
            return 0.8D;
        }
        if (path.equals("rhinoceros") || path.equals("elephant")) {
            return 0.55D;
        }
        return 0.35D;
    }

    private static void shootSmallFireball(LivingEntity host, double spread) {
        Vec3 direction = host.getLookAngle().scale(10.0D);
        spawnSmallFireball(host, direction, spread);
    }

    private static void startBlazeAttack(LivingEntity host, LivingEntity target) {
        if (!(host instanceof Blaze)) {
            return;
        }
        CompoundTag data = host.getPersistentData();
        long gameTime = host.level().getGameTime();
        if (data.getInt(BLAZE_ATTACK_STEP) > 0 || data.getLong(BLAZE_ATTACK_TIME) > gameTime) {
            return;
        }

        data.putInt(BLAZE_ATTACK_STEP, 1);
        data.putLong(BLAZE_ATTACK_TIME, gameTime + 60L);
        if (target != null) {
            data.putUUID(BLAZE_ATTACK_TARGET, target.getUUID());
            data.putBoolean(BLAZE_ATTACK_FORWARD, false);
        } else {
            data.remove(BLAZE_ATTACK_TARGET);
            data.putBoolean(BLAZE_ATTACK_FORWARD, true);
        }
        setBlazeCharged(host, true);
    }

    private static void processBlazeAttack(LivingEntity host) {
        if (!(host instanceof Blaze)) {
            return;
        }
        CompoundTag data = host.getPersistentData();
        int step = data.getInt(BLAZE_ATTACK_STEP);
        if (step <= 0) {
            return;
        }

        long gameTime = host.level().getGameTime();
        if (data.getLong(BLAZE_ATTACK_TIME) > gameTime) {
            return;
        }

        if (step <= 3) {
            LivingEntity target = findBlazeTarget(host, data);
            if (target != null) {
                lookAt(host, target);
                shootSmallFireballAt(host, target, 0.25D);
            } else if (data.getBoolean(BLAZE_ATTACK_FORWARD)) {
                shootSmallFireball(host, 0.25D);
            } else {
                endBlazeAttack(host, 20L);
                return;
            }
            data.putInt(BLAZE_ATTACK_STEP, step + 1);
            data.putLong(BLAZE_ATTACK_TIME, gameTime + 6L);
            setBlazeCharged(host, true);
            return;
        }

        endBlazeAttack(host, 100L);
    }

    private static LivingEntity findBlazeTarget(LivingEntity host, CompoundTag data) {
        if (!data.hasUUID(BLAZE_ATTACK_TARGET) || host.getServer() == null) {
            return null;
        }
        UUID targetId = data.getUUID(BLAZE_ATTACK_TARGET);
        for (ServerLevel level : host.getServer().getAllLevels()) {
            Entity entity = level.getEntity(targetId);
            if (entity instanceof LivingEntity living && living.isAlive() && host.hasLineOfSight(living)) {
                return living;
            }
        }
        return null;
    }

    private static void shootSmallFireballAt(LivingEntity host, LivingEntity target, double spread) {
        Vec3 origin = fireballSpawnPosition(host, host.getLookAngle());
        double dx = target.getX() - origin.x;
        double dy = target.getY(0.5D) - origin.y;
        double dz = target.getZ() - origin.z;
        spawnSmallFireball(host, new Vec3(dx, dy, dz), spread);
    }

    private static void spawnSmallFireball(LivingEntity host, Vec3 direction, double spread) {
        Vec3 normalized = direction.lengthSqr() > 1.0E-7D ? direction.normalize() : host.getLookAngle();
        SmallFireball fireball = new SmallFireball(
                host.level(),
                host,
                direction.x + host.getRandom().nextGaussian() * spread,
                direction.y,
                direction.z + host.getRandom().nextGaussian() * spread
        );
        Vec3 origin = fireballSpawnPosition(host, normalized);
        fireball.setPos(origin.x, origin.y, origin.z);
        host.level().addFreshEntity(fireball);
        host.level().levelEvent(null, 1018, host.blockPosition(), 0);
    }

    private static Vec3 fireballSpawnPosition(LivingEntity host, Vec3 direction) {
        Vec3 normalized = direction.lengthSqr() > 1.0E-7D ? direction.normalize() : host.getLookAngle();
        double forwardOffset = host.getBbWidth() * 0.5D + 1.0D;
        return new Vec3(
                host.getX() + normalized.x * forwardOffset,
                host.getY() + host.getBbHeight() * 0.5D + 0.5D + normalized.y * 0.5D,
                host.getZ() + normalized.z * forwardOffset
        );
    }

    private static void lookAt(LivingEntity host, LivingEntity target) {
        Vec3 delta = target.getEyePosition().subtract(host.getEyePosition());
        double horizontal = Math.sqrt(delta.x * delta.x + delta.z * delta.z);
        float yRot = (float) (Math.toDegrees(Math.atan2(delta.z, delta.x)) - 90.0D);
        float xRot = (float) -Math.toDegrees(Math.atan2(delta.y, horizontal));
        host.setYRot(yRot);
        host.setYHeadRot(yRot);
        host.yBodyRot = yRot;
        host.setXRot(xRot);
    }

    private static void endBlazeAttack(LivingEntity host, long cooldownTicks) {
        CompoundTag data = host.getPersistentData();
        data.putLong(BLAZE_ATTACK_TIME, host.level().getGameTime() + cooldownTicks);
        data.remove(BLAZE_ATTACK_STEP);
        data.remove(BLAZE_ATTACK_TARGET);
        data.remove(BLAZE_ATTACK_FORWARD);
        setBlazeCharged(host, false);
    }

    private static void setBlazeCharged(LivingEntity host, boolean charged) {
        if (host instanceof Blaze) {
            ((BlazeAccessor) host).requiemunofficial$setCharged(charged);
        }
    }

    private static void shootLargeFireball(LivingEntity host, int explosionPower) {
        Vec3 look = host.getLookAngle();
        Vec3 direction = look.scale(10.0D);
        LargeFireball fireball = new LargeFireball(
                host.level(),
                host,
                direction.x,
                direction.y,
                direction.z,
                explosionPower
        );
        fireball.setPos(
                host.getX() + look.x * 2.0D,
                host.getY() + host.getBbHeight() * 0.5D + 0.5D,
                host.getZ() + look.z * 2.0D
        );
        host.level().addFreshEntity(fireball);
    }

    private static void gustForward(LivingEntity host) {
        Vec3 look = host.getLookAngle().normalize();
        AABB area = host.getBoundingBox().inflate(6.0D);
        for (LivingEntity target : host.level().getEntitiesOfClass(LivingEntity.class, area, entity -> entity != host && entity.isAlive())) {
            Vec3 offset = target.position().subtract(host.position());
            if (offset.lengthSqr() < 1.0E-4D || offset.normalize().dot(look) < 0.45D) {
                continue;
            }
            target.push(look.x * 1.1D, 0.35D, look.z * 1.1D);
            target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 40, 0));
            target.hasImpulse = true;
        }
        host.level().playSound(null, host.blockPosition(), SoundEvents.PHANTOM_FLAP, SoundSource.HOSTILE, 1.0F, 0.75F);
        if (host.level() instanceof ServerLevel level) {
            level.sendParticles(
                    ParticleTypes.CLOUD,
                    host.getX() + look.x,
                    host.getY() + host.getBbHeight() * 0.5D,
                    host.getZ() + look.z,
                    24,
                    0.4D,
                    0.4D,
                    0.4D,
                    0.08D
            );
        }
    }

    private static void leapForward(LivingEntity host, double horizontal, double vertical) {
        Vec3 look = host.getLookAngle();
        Vec3 flat = new Vec3(look.x, 0.0D, look.z);
        if (flat.lengthSqr() < 1.0E-4D) {
            return;
        }
        flat = flat.normalize();
        host.setDeltaMovement(flat.x * horizontal, vertical, flat.z * horizontal);
        host.hasImpulse = true;
        host.fallDistance = 0.0F;
    }

    private static void pullTargetToHost(LivingEntity host, LivingEntity target, double strength) {
        Vec3 pull = target.position().vectorTo(host.position());
        if (pull.lengthSqr() < 1.0E-4D) {
            return;
        }
        Vec3 current = target.getDeltaMovement();
        Vec3 impulse = pull.normalize().scale(strength);
        target.setDeltaMovement(current.add(impulse.x, Math.max(0.05D, impulse.y), impulse.z));
        target.hasImpulse = true;
    }

    private static void blinkNearTarget(LivingEntity host, LivingEntity target) {
        if (!(host.level() instanceof ServerLevel level)) {
            return;
        }
        Vec3 away = host.position().subtract(target.position());
        if (away.lengthSqr() < 1.0E-4D) {
            away = target.getLookAngle().reverse();
        }
        away = away.normalize();
        BlockPos targetPos = BlockPos.containing(target.position().add(away.scale(1.8D)));
        for (BlockPos candidate : BlockPos.betweenClosed(targetPos.offset(-1, -1, -1), targetPos.offset(1, 1, 1))) {
            if (!canStandAt(level, host, candidate)) {
                continue;
            }
            host.teleportTo(candidate.getX() + 0.5D, candidate.getY(), candidate.getZ() + 0.5D);
            host.level().playSound(null, host.blockPosition(), SoundEvents.ENDERMAN_TELEPORT, SoundSource.HOSTILE, 1.0F, 1.0F);
            return;
        }
    }

    private static boolean playDead(Axolotl axolotl) {
        float missingHealth = axolotl.getMaxHealth() - axolotl.getHealth();
        if (missingHealth <= 0.0F) {
            return false;
        }
        AttributeInstance speed = axolotl.getAttribute(Attributes.MOVEMENT_SPEED);
        if (speed != null) {
            speed.removeModifier(AXOLOTL_PLAYING_DEAD_SLOWDOWN);
            speed.addTransientModifier(new AttributeModifier(
                    AXOLOTL_PLAYING_DEAD_SLOWDOWN,
                    "Playing dead slowdown",
                    -0.9D,
                    AttributeModifier.Operation.MULTIPLY_TOTAL
            ));
        }
        axolotl.setPlayingDead(true);
        axolotl.addEffect(new MobEffectInstance(
                MobEffects.REGENERATION,
                AXOLOTL_PLAYING_DEAD_TICKS,
                0,
                true,
                false,
                false
        ));
        axolotl.getPersistentData().putLong(
                AXOLOTL_PLAYING_DEAD_END_TIME,
                axolotl.level().getGameTime() + AXOLOTL_PLAYING_DEAD_TICKS
        );
        return true;
    }

    private static void toggleShulkerPeek(Shulker shulker) {
        int nextAmount = shulker.getClientPeekAmount(1.0F) > 0.0F ? 0 : 100;
        ((ShulkerAccessor) shulker).requiemunofficial$setRawPeekAmount(nextAmount);
    }

    private static void summonVexes(Evoker evoker) {
        if (!(evoker.level() instanceof ServerLevel level)) {
            return;
        }
        level.playSound(
                null,
                evoker.blockPosition(),
                SoundEvents.EVOKER_CAST_SPELL,
                SoundSource.HOSTILE,
                1.0F,
                1.0F
        );

        for (int i = 0; i < 3; i++) {
            BlockPos pos = evoker.blockPosition().offset(
                    -2 + evoker.getRandom().nextInt(5),
                    1,
                    -2 + evoker.getRandom().nextInt(5)
            );
            Vex vex = new Vex(EntityType.VEX, level);
            vex.moveTo(pos, 0.0F, 0.0F);
            vex.setOwner(evoker);
            vex.setBoundOrigin(evoker.blockPosition());
            vex.setLimitedLife(20 * (30 + evoker.getRandom().nextInt(90)));
            level.addFreshEntity(vex);
        }
    }

    private static void shootSnowballForward(SnowGolem snowGolem) {
        Snowball snowball = new Snowball(snowGolem.level(), snowGolem);
        snowball.shootFromRotation(snowGolem, snowGolem.getXRot(), snowGolem.getYRot(), 0.0F, 1.5F, 1.0F);
        snowGolem.level().playSound(
                null,
                snowGolem.blockPosition(),
                SoundEvents.SNOW_GOLEM_SHOOT,
                SoundSource.HOSTILE,
                1.0F,
                1.0F / (snowGolem.getRandom().nextFloat() * 0.4F + 0.8F)
        );
        snowGolem.level().addFreshEntity(snowball);
    }

    private static void shootSkeletonArrowForward(LivingEntity host) {
        Arrow arrow = new Arrow(host.level(), host);
        arrow.setBaseDamage(Math.max(2.0D, host.getAttributeValue(Attributes.ATTACK_DAMAGE)));
        arrow.pickup = AbstractArrow.Pickup.CREATIVE_ONLY;
        arrow.setPos(host.getX(), host.getEyeY() - 0.1D, host.getZ());
        arrow.shootFromRotation(host, host.getXRot(), host.getYRot(), 0.0F, 1.6F, 6.0F);
        host.level().playSound(
                null,
                host.blockPosition(),
                SoundEvents.SKELETON_SHOOT,
                SoundSource.HOSTILE,
                1.0F,
                1.0F / (host.getRandom().nextFloat() * 0.4F + 0.8F)
        );
        host.level().addFreshEntity(arrow);
    }

    private static void shootSkeletonArrowAt(LivingEntity host, LivingEntity target) {
        Arrow arrow = new Arrow(host.level(), host);
        arrow.setBaseDamage(Math.max(2.0D, host.getAttributeValue(Attributes.ATTACK_DAMAGE)));
        arrow.pickup = AbstractArrow.Pickup.CREATIVE_ONLY;
        arrow.setPos(host.getX(), host.getEyeY() - 0.1D, host.getZ());
        double x = target.getX() - host.getX();
        double y = target.getY(0.3333333333333333D) - arrow.getY();
        double z = target.getZ() - host.getZ();
        double horizontal = Math.sqrt(x * x + z * z);
        arrow.shoot(x, y + horizontal * 0.2D, z, 1.6F, 6.0F);
        host.level().playSound(
                null,
                host.blockPosition(),
                SoundEvents.SKELETON_SHOOT,
                SoundSource.HOSTILE,
                1.0F,
                1.0F / (host.getRandom().nextFloat() * 0.4F + 0.8F)
        );
        host.level().addFreshEntity(arrow);
    }

    private static void processBodyInteractions(LivingEntity host) {
        if (host instanceof Axolotl axolotl
                && axolotl.getPersistentData().contains(AXOLOTL_PLAYING_DEAD_END_TIME)
                && axolotl.level().getGameTime() >= axolotl.getPersistentData().getLong(AXOLOTL_PLAYING_DEAD_END_TIME)) {
            axolotl.setPlayingDead(false);
            AttributeInstance speed = axolotl.getAttribute(Attributes.MOVEMENT_SPEED);
            if (speed != null) {
                speed.removeModifier(AXOLOTL_PLAYING_DEAD_SLOWDOWN);
            }
            axolotl.getPersistentData().remove(AXOLOTL_PLAYING_DEAD_END_TIME);
        }

        if (shouldDryOutWhenPossessed(host)
                && !host.isInWaterRainOrBubble()
                && host.level().getGameTime() % 20L == 0L) {
            host.hurt(host.damageSources().dryOut(), 1.0F);
        }

        if (isWaterHurtBody(host.getType())
                && host.isInWaterRainOrBubble()
                && host.level().getGameTime() % 20L == 0L) {
            host.hurt(host.damageSources().drown(), 1.0F);
        }
    }

    private static boolean shouldDryOutWhenPossessed(LivingEntity host) {
        EntityMobilityManager.MobilityProfile mobility = EntityMobilityManager.get(host.getType());
        return mobility.flopsOnLand()
                || (mobility.swimMode() == EntityMobilityManager.MobilityMode.FORCED
                && isAlexBody(host.getType(), ALEX_AQUATIC));
    }

    private static void clearPossessedDisguiseTargets(ServerPlayer player, LivingEntity host) {
        AABB searchBox = host.getBoundingBox().inflate(32.0D);
        for (Monster mob : host.level().getEntitiesOfClass(Monster.class, searchBox, mob -> mob.isAlive())) {
            LivingEntity target = mob.getTarget();
            if ((target == host || target == player) && !canMonsterTargetPossessedHost(mob, host)) {
                mob.setTarget(null);
                mob.getNavigation().stop();
            }
        }
    }

    private static void rememberExistingHostility(LivingEntity host) {
        for (Monster monster : host.level().getEntitiesOfClass(Monster.class,
                host.getBoundingBox().inflate(32.0D), mob -> mob != host && mob.isAlive())) {
            LivingEntity previousTarget = monster.getTarget();
            if (previousTarget != host) {
                continue;
            }
            monster.getPersistentData().putUUID(PROVOKED_BY_POSSESSED_HOST, host.getUUID());
            monster.getPersistentData().putLong(PROVOKED_BY_POSSESSED_HOST_UNTIL, Long.MAX_VALUE);
        }
    }

    private static boolean shouldCancelPossessedTarget(LivingEntity attacker, LivingEntity target) {
        if (!(attacker instanceof Monster monster)) {
            return false;
        }

        if (getPossessor(target) != null) {
            return !wasProvokedByPossessedHost(monster, target);
        }

        if (target instanceof ServerPlayer player) {
            UUID hostId = SoulManager.getPossessedEntityId(player);
            LivingEntity host = hostId == null ? null : findHost(player, hostId);
            return host != null && !wasProvokedByPossessedHost(monster, host);
        }
        return false;
    }

    public static boolean shouldCancelTargetForTest(LivingEntity attacker, LivingEntity target) {
        return shouldCancelPossessedTarget(attacker, target);
    }

    private static boolean canMonsterTargetPossessedHost(Monster monster, LivingEntity host) {
        return wasProvokedByPossessedHost(monster, host);
    }

    public static void markProvokedByPossessedHost(LivingEntity host, LivingEntity target) {
        markDetectedByPossessedHost(host, target);
        if (target instanceof Monster && getPossessor(host) != null) {
            target.getPersistentData().putLong(PROVOKED_BY_POSSESSED_HOST_UNTIL, Long.MAX_VALUE);
        }
    }

    static void markDetectedByPossessedHost(LivingEntity host, LivingEntity target) {
        if (!(target instanceof Monster monster)
                || getPossessor(host) == null) {
            return;
        }

        monster.getPersistentData().putUUID(PROVOKED_BY_POSSESSED_HOST, host.getUUID());
        monster.getPersistentData().putLong(
                PROVOKED_BY_POSSESSED_HOST_UNTIL,
                monster.level().getGameTime() + POSSESSED_HOST_PROVOKE_TICKS
        );
        monster.setTarget(host);
    }

    public static void markProvokedByPossessedHostForTest(LivingEntity host, LivingEntity target) {
        markProvokedByPossessedHost(host, target);
    }

    private static boolean wasProvokedByPossessedHost(Monster monster, LivingEntity host) {
        if (!monster.getPersistentData().hasUUID(PROVOKED_BY_POSSESSED_HOST)
                || !monster.getPersistentData().getUUID(PROVOKED_BY_POSSESSED_HOST).equals(host.getUUID())) {
            return false;
        }

        if (monster.level().getGameTime() <= monster.getPersistentData().getLong(PROVOKED_BY_POSSESSED_HOST_UNTIL)) {
            return true;
        }

        monster.getPersistentData().remove(PROVOKED_BY_POSSESSED_HOST);
        monster.getPersistentData().remove(PROVOKED_BY_POSSESSED_HOST_UNTIL);
        return false;
    }

    private static boolean frogTongueAttack(Frog frog, LivingEntity target, ServerPlayer player) {
        frog.setTongueTarget(target);
        frog.setPose(Pose.USING_TONGUE);
        Vec3 pull = target.position().vectorTo(frog.position());
        if (pull.lengthSqr() > 1.0E-4D) {
            target.setDeltaMovement(pull.normalize().scale(0.75D));
            target.hasImpulse = true;
        }
        frog.level().playSound(
                null,
                frog.blockPosition(),
                SoundEvents.FROG_TONGUE,
                SoundSource.NEUTRAL,
                2.0F,
                1.0F
        );
        boolean hit = target.hurt(player.damageSources().mobAttack(frog),
                Frog.canEat(target) ? 10.0F : 1.0F);
        frog.level().playSound(
                null,
                frog.blockPosition(),
                SoundEvents.FROG_EAT,
                SoundSource.NEUTRAL,
                2.0F,
                1.0F
        );
        frog.eraseTongueTarget();
        frog.setPose(Pose.STANDING);
        return hit;
    }

    private static void togglePufferfishInflation(Pufferfish pufferfish) {
        int nextState = pufferfish.getPuffState() == Pufferfish.STATE_FULL
                ? Pufferfish.STATE_SMALL
                : Pufferfish.STATE_FULL;
        pufferfish.setPuffState(nextState);
        pufferfish.level().playSound(
                null,
                pufferfish.blockPosition(),
                nextState == Pufferfish.STATE_FULL
                        ? SoundEvents.PUFFER_FISH_BLOW_UP
                        : SoundEvents.PUFFER_FISH_BLOW_OUT,
                SoundSource.NEUTRAL,
                1.0F,
                1.0F
        );
    }

    private static void ramTowardTarget(LivingEntity host, LivingEntity target, EntityType<?> type) {
        Vec3 direction = target.position().subtract(host.position());
        if (direction.lengthSqr() < 1.0E-4D) {
            direction = host.getLookAngle();
        }
        direction = direction.normalize();
        double speed = type == EntityType.RAVAGER ? 1.6D : type == EntityType.HOGLIN ? 1.25D : 1.05D;
        host.setDeltaMovement(direction.x * speed, Math.max(host.getDeltaMovement().y, 0.05D), direction.z * speed);
        host.hasImpulse = true;
        if (type == EntityType.GOAT) {
            host.level().playSound(
                    null,
                    host.blockPosition(),
                    SoundEvents.GOAT_RAM_IMPACT,
                    SoundSource.HOSTILE,
                    1.0F,
                    1.0F
            );
        }
    }

    private static void startGuardianBeam(Guardian guardian, LivingEntity target) {
        ((GuardianAccessor) guardian).requiemunofficial$setActiveAttackTarget(target.getId());
        guardian.getPersistentData().putUUID(GUARDIAN_BEAM_TARGET, target.getUUID());
        guardian.getPersistentData().putLong(
                GUARDIAN_BEAM_END_TIME,
                guardian.level().getGameTime() + GUARDIAN_BEAM_WARMUP_TICKS
        );
        guardian.level().playSound(
                null,
                guardian.blockPosition(),
                SoundEvents.GUARDIAN_ATTACK,
                SoundSource.HOSTILE,
                1.0F,
                1.0F
        );
    }

    private static void processGuardianBeam(ServerPlayer player, LivingEntity host) {
        if (!(host instanceof Guardian guardian)) {
            return;
        }
        if (!guardian.getPersistentData().hasUUID(GUARDIAN_BEAM_TARGET)) {
            return;
        }
        long endTime = guardian.getPersistentData().getLong(GUARDIAN_BEAM_END_TIME);
        if (guardian.level().getGameTime() < endTime) {
            return;
        }

        LivingEntity target = findHost(player, guardian.getPersistentData().getUUID(GUARDIAN_BEAM_TARGET));
        clearGuardianBeam(guardian);
        if (target == null || !target.isAlive() || !guardian.hasLineOfSight(target)) {
            return;
        }

        float damage = guardian.getType() == EntityType.ELDER_GUARDIAN ? 8.0F : 6.0F;
        if (target.hurt(player.damageSources().indirectMagic(guardian, player), damage)) {
            ModCriteria.POSSESSED_HIT_ENTITY.trigger(player, guardian, target);
        }
    }

    private static void clearGuardianBeam(Guardian guardian) {
        ((GuardianAccessor) guardian).requiemunofficial$setActiveAttackTarget(0);
        guardian.getPersistentData().remove(GUARDIAN_BEAM_TARGET);
        guardian.getPersistentData().remove(GUARDIAN_BEAM_END_TIME);
    }

    private static void spawnLlamaSpit(Llama llama, LivingEntity target) {
        LlamaSpit spit = new LlamaSpit(llama.level(), llama);
        double x = target.getX() - llama.getX();
        double y = target.getY(0.3333333333333333D) - spit.getY();
        double z = target.getZ() - llama.getZ();
        double horizontal = Math.sqrt(x * x + z * z) * 0.2D;
        spit.shoot(x, y + horizontal, z, 1.5F, 10.0F);
        llama.level().playSound(
                null,
                llama.blockPosition(),
                SoundEvents.LLAMA_SPIT,
                SoundSource.HOSTILE,
                1.0F,
                1.0F
        );
        llama.level().addFreshEntity(spit);
    }

    private static void spawnEvokerFangs(LivingEntity host, LivingEntity target) {
        double minY = Math.min(target.getY(), host.getY());
        double maxY = Math.max(target.getY(), host.getY()) + 1.0D;
        float yaw = (float) Math.atan2(target.getZ() - host.getZ(), target.getX() - host.getX());
        int delay = 0;

        for (int i = 0; i < 5; i++) {
            double distance = 1.25D * (i + 1);
            double x = host.getX() + Math.cos(yaw) * distance;
            double z = host.getZ() + Math.sin(yaw) * distance;
            spawnFang(host, x, z, minY, maxY, yaw, delay);
            delay += 2;
        }
    }

    private static void spawnFang(LivingEntity host, double x, double z, double minY,
                                  double maxY, float yaw, int warmupDelay) {
        BlockPos pos = BlockPos.containing(x, maxY, z);
        boolean foundGround = false;
        double y = 0.0D;

        do {
            BlockPos below = pos.below();
            if (host.level().getBlockState(below).isFaceSturdy(host.level(), below, Direction.UP)) {
                if (!host.level().isEmptyBlock(pos)) {
                    y = host.level().getBlockState(pos)
                            .getCollisionShape(host.level(), pos)
                            .max(Direction.Axis.Y);
                }
                foundGround = true;
                break;
            }
            pos = pos.below();
        } while (pos.getY() >= Math.floor(minY) - 1);

        if (foundGround) {
            host.level().addFreshEntity(new EvokerFangs(
                    host.level(),
                    x,
                    pos.getY() + y,
                    z,
                    yaw,
                    warmupDelay,
                    host
            ));
        }
    }

    private static void applyHostPassiveTraits(ServerPlayer player, LivingEntity host) {
        EntityType<?> type = host.getType();
        if (hasNightVision(type)) {
            player.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, 260, 0, true, false, false));
        }
        if (hasUnderwaterVision(type)) {
            player.addEffect(new MobEffectInstance(MobEffects.WATER_BREATHING, 260, 0, true, false, false));
            player.addEffect(new MobEffectInstance(MobEffects.CONDUIT_POWER, 260, 0, true, false, false));
        }
        if (canBodyBreatheUnderwater(type)) {
            host.addEffect(new MobEffectInstance(MobEffects.WATER_BREATHING, 260, 0, true, false, false));
        }
        if (hasFireVision(type)) {
            player.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, 260, 0, true, false, false));
            host.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, 260, 0, true, false, false));
        }
        if (type == EntityType.CHICKEN || type == EntityType.PARROT || type == EntityType.ALLAY
                || type == EntityType.BAT || type == EntityType.BEE
                || EntityMobilityManager.get(type).canFly()) {
            player.addEffect(new MobEffectInstance(MobEffects.SLOW_FALLING, 80, 0, true, false, false));
        }
    }

    private static boolean hasNightVision(EntityType<?> type) {
        return type.is(ModTags.NIGHT_EYED)
                || isAlexBody(type, ALEX_NIGHT_EYED)
                || type == EntityType.BAT
                || type == EntityType.CAT
                || type == EntityType.CAVE_SPIDER
                || type == EntityType.ENDERMAN
                || type == EntityType.ENDERMITE
                || type == EntityType.FOX
                || type == EntityType.OCELOT
                || type == EntityType.PIGLIN
                || type == EntityType.PIGLIN_BRUTE
                || type == EntityType.SILVERFISH
                || type == EntityType.SPIDER
                || type == EntityType.VEX
                || type == EntityType.WOLF;
    }

    private static boolean hasUnderwaterVision(EntityType<?> type) {
        return isAlexBody(type, ALEX_AQUATIC)
                || type == EntityType.COD
                || type == EntityType.DOLPHIN
                || type == EntityType.GLOW_SQUID
                || type == EntityType.GUARDIAN
                || type == EntityType.PUFFERFISH
                || type == EntityType.SALMON
                || type == EntityType.SQUID
                || type == EntityType.TADPOLE
                || type == EntityType.TROPICAL_FISH
                || type == EntityType.TURTLE;
    }

    private static boolean canBodyBreatheUnderwater(EntityType<?> type) {
        String alexPath = alexPath(type);
        if (alexPath != null) {
            return ALEX_WATER_BREATHERS.contains(alexPath);
        }
        return type == EntityType.AXOLOTL
                || type == EntityType.COD
                || type == EntityType.DROWNED
                || type == EntityType.GLOW_SQUID
                || type == EntityType.GUARDIAN
                || type == EntityType.PUFFERFISH
                || type == EntityType.SALMON
                || type == EntityType.SQUID
                || type == EntityType.TADPOLE
                || type == EntityType.TROPICAL_FISH;
    }

    private static boolean hasFireVision(EntityType<?> type) {
        return isFireproofBody(type)
                || type == EntityType.BLAZE
                || type == EntityType.MAGMA_CUBE
                || type == EntityType.STRIDER;
    }

    private static boolean isFireproofBody(EntityType<?> type) {
        return isAlexBody(type, ALEX_FIREPROOF);
    }

    private static boolean isWaterHurtBody(EntityType<?> type) {
        return type == EntityType.BLAZE
                || type == EntityType.STRIDER
                || isAlexBody(type, ALEX_WATER_HURT);
    }

    private static boolean isAlexBody(EntityType<?> type, Set<String> paths) {
        String path = alexPath(type);
        return path != null && paths.contains(path);
    }

    private static String alexPath(EntityType<?> type) {
        ResourceLocation id = BuiltInRegistries.ENTITY_TYPE.getKey(type);
        return "alexsmobs".equals(id.getNamespace()) ? id.getPath() : null;
    }

    private static void startAlexJump(LivingEntity host) {
        String path = alexPath(host.getType());
        if (!"kangaroo".equals(path) && !"jerboa".equals(path)) {
            return;
        }
        invokeNoArg(host, "startJumping");
    }

    private static void syncAlexMovementSpeed(LivingEntity host, double speed) {
        String path = alexPath(host.getType());
        if (!"kangaroo".equals(path) && !"jerboa".equals(path)) {
            return;
        }
        try {
            host.getClass().getMethod("setMovementSpeed", double.class).invoke(host, speed);
        } catch (ReflectiveOperationException ignored) {
        }
    }

    private static void triggerAlexAttackCue(LivingEntity host, String path) {
        switch (path) {
            case "anteater" -> triggerAlexAnimation(host, "ANIMATION_SLASH_R", "ANIMATION_SLASH_L");
            case "bison" -> triggerAlexAnimation(host, "ANIMATION_ATTACK", "ANIMATION_PREPARE_CHARGE");
            case "bunfungus" -> triggerAlexAnimation(host, "ANIMATION_SLAM", "ANIMATION_BELLY");
            case "capuchin_monkey" -> triggerAlexAnimation(host, "ANIMATION_THROW", "ANIMATION_SCRATCH");
            case "catfish" -> invokeNoArg(host, "spit");
            case "crimson_mosquito" -> invokeNoArg(host, "setupShooting");
            case "crocodile" -> triggerAlexAnimation(host, "ANIMATION_LUNGE", "ANIMATION_DEATHROLL");
            case "dropbear" -> triggerAlexAnimation(host, "ANIMATION_BITE", "ANIMATION_SWIPE_R", "ANIMATION_SWIPE_L");
            case "elephant" -> triggerAlexAnimation(host, "ANIMATION_STOMP", "ANIMATION_FLING");
            case "emu" -> triggerAlexAnimation(host, "ANIMATION_SCRATCH");
            case "endergrade" -> invokeNoArg(host, "bite");
            case "frilled_shark" -> triggerAlexAnimation(host, "ANIMATION_ATTACK");
            case "froststalker" -> triggerAlexAnimation(host, "ANIMATION_BITE", "ANIMATION_SLASH_L", "ANIMATION_SLASH_R", "ANIMATION_SHOVE");
            case "gelada_monkey" -> triggerAlexAnimation(host, "ANIMATION_SWIPE_R", "ANIMATION_SWIPE_L", "ANIMATION_CHEST");
            case "gorilla" -> triggerAlexAnimation(host, "ANIMATION_ATTACK", "ANIMATION_POUNDCHEST");
            case "grizzly_bear" -> triggerAlexAnimation(host, "ANIMATION_MAUL", "ANIMATION_SWIPE_R", "ANIMATION_SWIPE_L");
            case "kangaroo" -> triggerAlexAnimation(host, "ANIMATION_KICK", "ANIMATION_PUNCH_R", "ANIMATION_PUNCH_L");
            case "leafcutter_ant" -> triggerAlexAnimation(host, "ANIMATION_BITE");
            case "mantis_shrimp" -> invokeNoArg(host, "punch");
            case "moose" -> triggerAlexAnimation(host, "ANIMATION_ATTACK");
            case "orca" -> triggerAlexAnimation(host, "ANIMATION_BITE", "ANIMATION_TAILSWING");
            case "raccoon" -> triggerAlexAnimation(host, "ANIMATION_ATTACK");
            case "rattlesnake" -> triggerAlexAnimation(host, "ANIMATION_BITE");
            case "rhinoceros" -> triggerAlexAnimation(host, "ANIMATION_FLING", "ANIMATION_SLASH");
            case "sea_bear" -> triggerAlexAnimation(host, "ANIMATION_ATTACK");
            case "shoebill" -> triggerAlexAnimation(host, "ANIMATION_ATTACK", "ANIMATION_BEAKSHAKE");
            case "skelewag" -> triggerAlexAnimation(host, "ANIMATION_STAB", "ANIMATION_SLASH");
            case "straddler" -> triggerAlexAnimation(host, "ANIMATION_LAUNCH");
            case "tasmanian_devil" -> triggerAlexAnimation(host, "ANIMATION_ATTACK", "ANIMATION_HOWL");
            case "tiger" -> triggerAlexAnimation(host, "ANIMATION_PAW_R", "ANIMATION_PAW_L", "ANIMATION_LEAP");
            case "tusklin" -> triggerAlexAnimation(host, "ANIMATION_GORE_L", "ANIMATION_GORE_R", "ANIMATION_FLING", "ANIMATION_BUCK");
            case "warped_mosco" -> triggerAlexAnimation(host, "ANIMATION_PUNCH_R", "ANIMATION_PUNCH_L", "ANIMATION_SLAM", "ANIMATION_SPIT");
            default -> {
            }
        }
    }

    private static void triggerAlexAnimation(LivingEntity host, String animationField) {
        try {
            Object animation = host.getClass().getField(animationField).get(null);
            host.getClass().getMethod("setAnimation", animation.getClass()).invoke(host, animation);
            host.getClass().getMethod("setAnimationTick", int.class).invoke(host, 0);
        } catch (ReflectiveOperationException ignored) {
        }
    }

    private static void triggerAlexAnimation(LivingEntity host, String firstField, String... fallbackFields) {
        int index = host.getRandom().nextInt(fallbackFields.length + 1);
        if (index == 0) {
            triggerAlexAnimation(host, firstField);
        } else {
            triggerAlexAnimation(host, fallbackFields[index - 1]);
        }
    }

    private static void invokeNoArg(LivingEntity host, String methodName) {
        try {
            host.getClass().getMethod(methodName).invoke(host);
        } catch (ReflectiveOperationException ignored) {
        }
    }

    private static void knockTarget(LivingEntity host, LivingEntity target, double horizontal, double vertical) {
        Vec3 direction = target.position().subtract(host.position());
        if (direction.lengthSqr() < 1.0E-4D) {
            direction = host.getLookAngle();
        }
        direction = direction.normalize();
        target.push(direction.x * horizontal, vertical, direction.z * horizontal);
        target.hasImpulse = true;
    }

    private static boolean blinkForward(LivingEntity host) {
        if (!(host.level() instanceof ServerLevel level)) {
            return false;
        }

        Vec3 look = host.getLookAngle().normalize();
        Vec3 start = host.getEyePosition();
        Vec3 end = start.add(look.scale(32.0D));
        BlockHitResult hit = level.clip(new ClipContext(
                start,
                end,
                ClipContext.Block.COLLIDER,
                ClipContext.Fluid.NONE,
                host
        ));

        Vec3 target = hit.getType() == HitResult.Type.MISS
                ? findNearestStandingTeleportTarget(level, host, BlockPos.containing(end))
                : findHitBlockTeleportTarget(level, host, hit.getBlockPos());
        if (target == null) {
            return false;
        }

        level.playSound(null, host.getX(), host.getY(), host.getZ(),
                SoundEvents.ENDERMAN_TELEPORT, host.getSoundSource(), 1.0F, 1.0F);
        host.teleportTo(target.x, target.y, target.z);
        level.playSound(null, target.x, target.y, target.z,
                SoundEvents.ENDERMAN_TELEPORT, host.getSoundSource(), 1.0F, 1.0F);
        return true;
    }

    private static Vec3 findHitBlockTeleportTarget(ServerLevel level, LivingEntity host, BlockPos hitBlock) {
        BlockPos feet = hitBlock.above();
        if (canStandAt(level, host, feet)) {
            return Vec3.atBottomCenterOf(feet);
        }
        return null;
    }

    private static Vec3 findNearestStandingTeleportTarget(ServerLevel level, LivingEntity host, BlockPos origin) {
        for (int y = 0; y <= 4; y++) {
            BlockPos feet = origin.below(y);
            if (canStandAt(level, host, feet)) {
                return Vec3.atBottomCenterOf(feet);
            }
        }
        return null;
    }

    private static boolean canStandAt(ServerLevel level, LivingEntity host, BlockPos feet) {
        if (!level.getWorldBorder().isWithinBounds(feet)) {
            return false;
        }
        BlockState support = level.getBlockState(feet.below());
        if (support.getCollisionShape(level, feet.below()).isEmpty()) {
            return false;
        }
        Vec3 target = Vec3.atBottomCenterOf(feet);
        AABB targetBox = host.getBoundingBox().move(
                target.x - host.getX(),
                target.y - host.getY(),
                target.z - host.getZ()
        );
        return level.noCollision(host, targetBox);
    }

    public static LivingEntity findAttackTarget(ServerPlayer player, LivingEntity host, double reach) {
        Vec3 origin = host.getEyePosition();
        Vec3 look = player.getLookAngle().normalize();
        AABB searchBox = host.getBoundingBox().expandTowards(look.scale(reach)).inflate(1.0D);
        List<LivingEntity> candidates = host.level().getEntitiesOfClass(
                LivingEntity.class,
                searchBox,
                entity -> entity != host
                        && entity != player
                        && entity.isAlive()
                        && !entity.isPassengerOfSameVehicle(player)
        );

        return candidates.stream()
                .filter(entity -> isInAttackCone(origin, look, entity, reach))
                .min(Comparator.comparingDouble(entity -> origin.distanceToSqr(entity.getEyePosition())))
                .orElse(null);
    }

    public static boolean isInAttackCone(Vec3 origin, Vec3 look, LivingEntity target, double reach) {
        Vec3 toTarget = target.getBoundingBox().getCenter().subtract(origin);
        double distance = toTarget.length();
        return distance <= reach
                && distance > 1.0E-4D
                && toTarget.normalize().dot(look) >= 0.85D;
    }

    private static void release(ServerPlayer player, LivingEntity host, boolean notify) {
        release(player, host, notify, true);
    }

    private static void release(ServerPlayer player, LivingEntity host, boolean notify, boolean transferInventory) {
        Vec3 releasePosition = host == null ? player.position() : findReleasePosition(host);
        player.stopUsingItem();
        if (host != null) {
            host.stopUsingItem();
        }
        if (host != null) {
            storeHostFoodData(player, host);
            if (transferInventory) {
                PossessionInventoryManager.onPossessionEnded(player, host);
            }
            clearPossessorNameTag(host);
            restoreHost(host);
        }
        restorePlayerFoodData(player);
        SoulLifecycleManager.clearReclamation(player);
        player.stopRiding();
        if (player.connection != null && player.getCamera() != player) {
            player.setCamera(player);
        }
        if (notify && host != null) {
            if (player.connection != null) {
                player.teleportTo(
                        (ServerLevel) host.level(),
                        releasePosition.x,
                        releasePosition.y,
                        releasePosition.z,
                        player.getYRot(),
                        player.getXRot()
                );
            } else {
                player.absMoveTo(
                        releasePosition.x,
                        releasePosition.y,
                        releasePosition.z,
                        player.getYRot(),
                        player.getXRot()
                );
            }
        }
        SoulManager.clearPossession(player);
        ACTIVE_POSSESSORS.remove(player.getUUID());
        POSSESSION_INPUTS.remove(player.getUUID());
        POSSESSION_TARGET_HINTS.remove(player.getUUID());
        POSSESSION_STARTED_AT.remove(player.getUUID());
        clearVagrantSenseTargets(player);
        ModNetwork.sendToConnectedPlayer(player, new PossessionTargetHintPacket(false));
        restoreBeta16PlayerBodyParameters(player);
        restorePossessorBody(player);
        VagrantEvents.refreshAfterSoulStateChange(player);
        if (notify) {
            sendSystemMessage(player, Component.translatable("message.requiemunofficial.possession.released"));
        }
    }

    private static void releaseFromHostDeath(ServerPlayer player, LivingEntity host) {
        if (host == null) {
            release(player, null, false);
            applyHostDeathExperienceLoss(player, null, player.position());
            return;
        }

        ServerLevel level = (ServerLevel) host.level();
        Vec3 deathPosition = host.position();
        float yRot = host.getYRot();
        float xRot = host.getXRot();
        release(player, host, false);
        applyHostDeathExperienceLoss(player, level, deathPosition);
        if (player.connection != null) {
            player.teleportTo(level, deathPosition.x, deathPosition.y, deathPosition.z, yRot, xRot);
        } else {
            player.absMoveTo(deathPosition.x, deathPosition.y, deathPosition.z, yRot, xRot);
        }
        player.fallDistance = 0.0F;
        player.setDeltaMovement(Vec3.ZERO);
        VagrantEvents.refreshAfterSoulStateChange(player);
    }

    private static void applyHostDeathExperienceLoss(ServerPlayer player, ServerLevel level, Vec3 deathPosition) {
        int droppedExperience = Math.min(100, player.experienceLevel * 7);
        if (level != null && droppedExperience > 0) {
            ExperienceOrb.award(level, deathPosition, droppedExperience);
        }
        player.experienceLevel = 0;
        player.experienceProgress = 0.0F;
        player.totalExperience = 0;
        player.takeXpDelay = 20;
    }

    public static Vec3 findReleasePosition(LivingEntity host) {
        BlockPos base = host.blockPosition();
        BlockPos[] candidates = {
                base.north(),
                base.south(),
                base.east(),
                base.west(),
                base.above(),
                base.north().above(),
                base.south().above(),
                base.east().above(),
                base.west().above()
        };

        for (BlockPos candidate : candidates) {
            if (isSafeReleasePosition(host, candidate)) {
                return Vec3.atBottomCenterOf(candidate);
            }
        }
        return host.position().add(0.0D, host.getBbHeight() + 0.25D, 0.0D);
    }

    public static boolean isSafeReleasePosition(LivingEntity host, BlockPos pos) {
        return host.level().getWorldBorder().isWithinBounds(pos)
                && host.level().getBlockState(pos).getCollisionShape(host.level(), pos).isEmpty()
                && host.level().getBlockState(pos.above())
                .getCollisionShape(host.level(), pos.above()).isEmpty();
    }

    private static void restoreHost(LivingEntity host) {
        UUID possessor = getPossessor(host);
        if (possessor != null) {
            host.getPersistentData().putUUID(LAST_POSSESSOR, possessor);
        }
        host.getPersistentData().remove(POSSESSED);
        host.getPersistentData().remove(POSSESSOR);
        host.getPersistentData().remove(NEXT_ATTACK_TIME);
        host.getPersistentData().remove(BLAZE_ATTACK_STEP);
        host.getPersistentData().remove(BLAZE_ATTACK_TIME);
        host.getPersistentData().remove(BLAZE_ATTACK_TARGET);
        host.getPersistentData().remove(BLAZE_ATTACK_FORWARD);
        setBlazeCharged(host, false);
        if (host instanceof Guardian guardian) {
            clearGuardianBeam(guardian);
        } else {
            host.getPersistentData().remove(GUARDIAN_BEAM_TARGET);
            host.getPersistentData().remove(GUARDIAN_BEAM_END_TIME);
        }
        if (host instanceof Axolotl axolotl) {
            axolotl.setPlayingDead(false);
            AttributeInstance speed = axolotl.getAttribute(Attributes.MOVEMENT_SPEED);
            if (speed != null) {
                speed.removeModifier(AXOLOTL_PLAYING_DEAD_SLOWDOWN);
            }
        }
        host.getPersistentData().remove(AXOLOTL_PLAYING_DEAD_END_TIME);
        restoreCustomName(host);
        host.setNoGravity(host.getPersistentData().getBoolean(ORIGINAL_NO_GRAVITY));
        host.getPersistentData().remove(ORIGINAL_NO_GRAVITY);
        host.noPhysics = host.getPersistentData().getBoolean(ORIGINAL_NO_PHYSICS);
        host.getPersistentData().remove(ORIGINAL_NO_PHYSICS);
        if (host.getPersistentData().contains(ORIGINAL_MAX_UP_STEP, Tag.TAG_FLOAT)) {
            ((EntityAccessor) host).requiemunofficial$setMaxUpStep(
                    host.getPersistentData().getFloat(ORIGINAL_MAX_UP_STEP)
            );
            host.getPersistentData().remove(ORIGINAL_MAX_UP_STEP);
        }
        if (host instanceof Mob mob) {
            mob.setNoAi(host.getPersistentData().getBoolean(ORIGINAL_NO_AI));
            host.getPersistentData().remove(ORIGINAL_NO_AI);
        }
    }

    private static void applyPossessedStepHeight(LivingEntity host) {
        EntityAccessor accessor = (EntityAccessor) host;
        if (!host.getPersistentData().contains(ORIGINAL_MAX_UP_STEP, Tag.TAG_FLOAT)) {
            host.getPersistentData().putFloat(ORIGINAL_MAX_UP_STEP, accessor.requiemunofficial$getMaxUpStep());
        }
        accessor.requiemunofficial$setMaxUpStep(
                Math.max(accessor.requiemunofficial$getMaxUpStep(), MIN_POSSESSED_STEP_HEIGHT)
        );
    }

    private static void applyHostFoodData(ServerPlayer player, LivingEntity host) {
        if (!host.getType().is(ModTags.REGULAR_EATERS)) {
            return;
        }

        if (!player.getPersistentData().contains(ORIGINAL_PLAYER_FOOD, Tag.TAG_COMPOUND)) {
            CompoundTag playerFood = new CompoundTag();
            player.getFoodData().addAdditionalSaveData(playerFood);
            player.getPersistentData().put(ORIGINAL_PLAYER_FOOD, playerFood);
        }

        CompoundTag hostFood = host.getPersistentData().contains(HOST_FOOD, Tag.TAG_COMPOUND)
                ? host.getPersistentData().getCompound(HOST_FOOD)
                : defaultFoodData();
        player.getFoodData().readAdditionalSaveData(hostFood);
    }

    private static void storeHostFoodData(ServerPlayer player, LivingEntity host) {
        if (!host.getType().is(ModTags.REGULAR_EATERS)) {
            return;
        }

        CompoundTag hostFood = new CompoundTag();
        player.getFoodData().addAdditionalSaveData(hostFood);
        host.getPersistentData().put(HOST_FOOD, hostFood);
    }

    private static void restorePlayerFoodData(ServerPlayer player) {
        if (!player.getPersistentData().contains(ORIGINAL_PLAYER_FOOD, Tag.TAG_COMPOUND)) {
            return;
        }

        player.getFoodData().readAdditionalSaveData(
                player.getPersistentData().getCompound(ORIGINAL_PLAYER_FOOD)
        );
        player.getPersistentData().remove(ORIGINAL_PLAYER_FOOD);
    }

    private static CompoundTag defaultFoodData() {
        CompoundTag tag = new CompoundTag();
        tag.putInt("foodLevel", 20);
        tag.putInt("foodTickTimer", 0);
        tag.putFloat("foodSaturationLevel", 5.0F);
        tag.putFloat("foodExhaustionLevel", 0.0F);
        return tag;
    }

    private static void applyPossessorName(ServerPlayer player, LivingEntity host) {
        if (!host.level().getGameRules().getBoolean(ModGameRules.SHOW_POSSESSOR_NAME_TAG)) {
            return;
        }

        boolean hadCustomName = host.hasCustomName();
        host.getPersistentData().putBoolean(HAD_CUSTOM_NAME, hadCustomName);
        host.getPersistentData().putBoolean(
                ORIGINAL_CUSTOM_NAME_VISIBLE,
                host.isCustomNameVisible()
        );
        if (hadCustomName && host.getCustomName() != null) {
            host.getPersistentData().putString(
                    ORIGINAL_CUSTOM_NAME,
                    Component.Serializer.toJson(host.getCustomName())
            );
        }
        host.setCustomName(player.getDisplayName().copy());
        host.setCustomNameVisible(true);
    }

    private static void syncPossessorNameTag(ServerPlayer player, LivingEntity host) {
        Component name = host.level().getGameRules().getBoolean(ModGameRules.SHOW_POSSESSOR_NAME_TAG)
                ? player.getDisplayName().copy()
                : null;
        ModNetwork.CHANNEL.send(
                net.minecraftforge.network.PacketDistributor.TRACKING_ENTITY_AND_SELF.with(() -> host),
                new PossessionNameTagPacket(host.getId(), name)
        );
    }

    private static void clearPossessorNameTag(LivingEntity host) {
        ModNetwork.CHANNEL.send(
                net.minecraftforge.network.PacketDistributor.TRACKING_ENTITY_AND_SELF.with(() -> host),
                new PossessionNameTagPacket(host.getId(), null)
        );
    }

    private static void restoreCustomName(LivingEntity host) {
        if (!host.getPersistentData().contains(HAD_CUSTOM_NAME)) {
            return;
        }

        Component originalName = null;
        if (host.getPersistentData().getBoolean(HAD_CUSTOM_NAME)
                && host.getPersistentData().contains(ORIGINAL_CUSTOM_NAME)) {
            originalName = Component.Serializer.fromJson(
                    host.getPersistentData().getString(ORIGINAL_CUSTOM_NAME)
            );
        }
        host.setCustomName(originalName);
        host.setCustomNameVisible(
                host.getPersistentData().getBoolean(ORIGINAL_CUSTOM_NAME_VISIBLE)
        );
        host.getPersistentData().remove(HAD_CUSTOM_NAME);
        host.getPersistentData().remove(ORIGINAL_CUSTOM_NAME);
        host.getPersistentData().remove(ORIGINAL_CUSTOM_NAME_VISIBLE);
    }

    private static boolean canPossess(ServerPlayer player, LivingEntity target) {
        return possessionFailureMessage(player, target) == null;
    }

    private static Component possessionFailureMessage(ServerPlayer player, LivingEntity target) {
        if (SoulManager.getState(player) != SoulState.VAGRANT) {
            return Component.translatable("message.requiemunofficial.possession.not_vagrant");
        }
        if (SoulManager.getPossessedEntityId(player) != null) {
            return Component.translatable("message.requiemunofficial.possession.already_possessing");
        }
        if (hasPossessionBlockingPenance(player)) {
            return Component.translatable("message.requiemunofficial.possession.penance");
        }
        if (target instanceof net.minecraft.world.entity.player.Player) {
            return Component.translatable("message.requiemunofficial.possession.player_body");
        }
        if (!target.isAlive()) {
            return Component.translatable("message.requiemunofficial.possession.dead");
        }
        if (target.getType().is(ModTags.POSSESSION_BLACKLIST)) {
            return Component.translatable("message.requiemunofficial.possession.blacklisted");
        }
        if (target.getPersistentData().getBoolean(POSSESSED)) {
            return Component.translatable("message.requiemunofficial.possession.already_possessed");
        }
        if (!target.getPassengers().isEmpty()) {
            return Component.translatable("message.requiemunofficial.possession.occupied");
        }
        boolean listedPossessable = target.getType().is(ModTags.POSSESSABLES);
        boolean soullessHost = EntitySoulManager.canBePossessionHost(target);
        if (!listedPossessable && !soullessHost) {
            return Component.translatable("message.requiemunofficial.possession.invalid_host");
        }
        if (!soullessHost) {
            return Component.translatable("message.requiemunofficial.possession.has_soul");
        }
        return null;
    }

    private static boolean hasPossessionBlockingPenance(ServerPlayer player) {
        return PenanceMobEffect.blocksPossession(player.getEffect(ModEffects.PENANCE.get()));
    }

    private static void sendSystemMessage(ServerPlayer player, Component message) {
        if (player.connection != null && player.connection.connection != null) {
            player.sendSystemMessage(message);
        }
    }

    public static void preparePossessedHostForAiStep(LivingEntity entity) {
        PossessionControl control = findPossessionControl(entity);
        if (control != null) {
            if (usesBeta16PlayerBody(control.host())) {
                suppressControlledMobAi(control.host());
                return;
            }
            controlHost(control.player(), control.host(), true, false);
        }
    }

    public static void finishPossessedHostAiStep(LivingEntity entity) {
        PossessionControl control = findPossessionControl(entity);
        if (control == null) {
            return;
        }
        if (usesBeta16PlayerBody(control.host())) {
            suppressControlledMobAi(control.host());
            return;
        }
        PossessionInput input = POSSESSION_INPUTS.getOrDefault(
                control.player().getUUID(),
                new PossessionInput(
                        control.player().zza,
                        control.player().xxa,
                        false,
                        control.player().isShiftKeyDown(),
                        control.player().getYRot(),
                        control.player().getXRot()
                )
        );
        LivingEntity movementHost = controlledMovementEntity(control.host());
        applyControlledRotation(control.host(), input.yRot(), input.xRot(), false);
        if (movementHost != control.host()) {
            applyControlledRotation(movementHost, input.yRot(), input.xRot(), false);
        }
        stopResidualControlledMovement(movementHost, input);
        suppressControlledMobAi(control.host());
        if (movementHost != control.host()) {
            suppressControlledMobAi(movementHost);
        }
    }

    public static Vec3 redirectPossessedTravelVector(LivingEntity entity, Vec3 originalTravelVector) {
        PossessionControl control = findPossessionControl(entity);
        if (control == null) {
            return originalTravelVector;
        }
        if (usesBeta16PlayerBody(control.host())) {
            return Vec3.ZERO;
        }
        LivingEntity movementHost = controlledMovementEntity(control.host());
        controlHost(control.player(), control.host(), false, false);
        return entity == movementHost ? new Vec3(entity.xxa, entity.yya, entity.zza) : originalTravelVector;
    }

    private static PossessionControl findPossessionControl(LivingEntity entity) {
        LivingEntity host = entity;
        UUID possessorId = getPossessor(host);
        if (possessorId == null) {
            host = possessedSkeletonPassenger(entity);
            possessorId = host == null ? null : getPossessor(host);
        }
        if (host == null || possessorId == null || !(entity.level() instanceof ServerLevel level)) {
            return null;
        }
        ServerPlayer player = getActivePossessor(level, possessorId);
        if (player == null || SoulManager.getPossessedEntityId(player) == null
                || !host.getUUID().equals(SoulManager.getPossessedEntityId(player))) {
            return null;
        }
        return new PossessionControl(player, host);
    }

    private static LivingEntity possessedSkeletonPassenger(LivingEntity entity) {
        if (!(entity instanceof Spider)) {
            return null;
        }
        for (Entity passenger : entity.getPassengers()) {
            if (passenger instanceof LivingEntity living
                    && living.getType().is(ModTags.HUMANOID_SKELETONS)
                    && getPossessor(living) != null) {
                return living;
            }
        }
        return null;
    }

    private static void controlHost(ServerPlayer player, LivingEntity host, boolean allowJump) {
        controlHost(player, host, allowJump, true);
    }

    private static void controlHost(ServerPlayer player,
                                    LivingEntity host,
                                    boolean allowJump,
                                    boolean allowDirectMovement) {
        PossessionInput input = POSSESSION_INPUTS.getOrDefault(
                player.getUUID(),
                new PossessionInput(
                        player.zza,
                        player.xxa,
                        false,
                        player.isShiftKeyDown(),
                        player.getYRot(),
                        player.getXRot()
                )
        );
        if (tryDismountControlledSpider(host, input)) {
            return;
        }
        LivingEntity movementHost = controlledMovementEntity(host);
        applyControlledRotation(host, input.yRot(), input.xRot(), true);
        if (movementHost != host) {
            applyControlledRotation(movementHost, input.yRot(), input.xRot(), true);
        }
        suppressControlledMobAi(host);
        if (movementHost != host) {
            suppressControlledMobAi(movementHost);
        }
        host.setShiftKeyDown(input.shifting());

        float forwardInput = input.forward();
        float strafeInput = input.strafe();
        EntityMobilityManager.MobilityProfile mobility = EntityMobilityManager.get(movementHost.getType());
        boolean creativeFlying = player.isCreative() && player.getAbilities().flying;
        boolean verticalMovement = creativeFlying || canMoveVertically(movementHost, mobility, input);
        movementHost.noPhysics = mobility.phasesThroughWalls();
        movementHost.setNoGravity(creativeFlying || mobility.forcesFlight());

        if (allowJump && !verticalMovement && input.jumping()) {
            jump(movementHost, mobility);
        }

        double controlledSpeed = Math.max(0.02D, movementHost.getAttributeValue(Attributes.MOVEMENT_SPEED));
        boolean vanillaTravel = usesVanillaTravel(movementHost, mobility, verticalMovement);
        movementHost.setSpeed((float) controlledTravelSpeed(movementHost, controlledSpeed, vanillaTravel));
        syncAlexMovementSpeed(movementHost, controlledSpeed);

        if (mobility.climbsWalls() && movementHost.horizontalCollision && forwardInput > 0.0F) {
            Vec3 current = movementHost.getDeltaMovement();
            movementHost.setDeltaMovement(current.x, Math.max(current.y, 0.22D), current.z);
            movementHost.hasImpulse = true;
        } else if (allowJump
                && mobility.walkMode() == EntityMobilityManager.WalkMode.JUMPY
                && (Math.abs(forwardInput) > 1.0E-4F || Math.abs(strafeInput) > 1.0E-4F)
                && movementHost.onGround()) {
            jump(movementHost, mobility);
        }

        boolean movingHorizontally = Math.abs(forwardInput) > 1.0E-4F || Math.abs(strafeInput) > 1.0E-4F;
        float verticalInput = verticalMovement ? verticalInput(input) : 0.0F;
        movementHost.xxa = strafeInput;
        movementHost.yya = verticalInput;
        movementHost.zza = forwardInput;
        if (vanillaTravel) {
            movementHost.hasImpulse = movingHorizontally || verticalInput != 0.0F;
        } else if (allowDirectMovement) {
            moveSpecialMobilityHost(movementHost, input, mobility, verticalMovement, movingHorizontally);
            movementHost.hasImpulse = true;
        } else {
            movementHost.hasImpulse = movingHorizontally || verticalInput != 0.0F;
        }
        applyPassiveMobility(movementHost, mobility, movingHorizontally);
    }

    private static double controlledTravelSpeed(LivingEntity host, double movementSpeed, boolean vanillaTravel) {
        if (vanillaTravel && isHumanoidWalkingHost(host)) {
            return movementSpeed * HUMANOID_PLAYER_BODY_SPEED_SCALE;
        }
        return movementSpeed;
    }

    private static boolean isHumanoidWalkingHost(LivingEntity host) {
        return host.getType().is(ModTags.HUMANOID_SKELETONS)
                || host.getType().is(ModTags.HUMANOID_ZOMBIES);
    }

    private static void stopResidualControlledMovement(LivingEntity host, PossessionInput input) {
        if (!host.onGround()
                || Math.abs(input.forward()) > 1.0E-4F
                || Math.abs(input.strafe()) > 1.0E-4F) {
            return;
        }

        EntityMobilityManager.MobilityProfile mobility = EntityMobilityManager.get(host.getType());
        if (!usesVanillaTravel(host, mobility, false)) {
            return;
        }

        Vec3 movement = host.getDeltaMovement();
        if (Math.abs(movement.x) > 1.0E-4D || Math.abs(movement.z) > 1.0E-4D) {
            host.setDeltaMovement(0.0D, movement.y, 0.0D);
            host.hasImpulse = true;
        }
    }

    private static void suppressControlledMobAi(LivingEntity entity) {
        if (entity instanceof Mob mob) {
            mob.setTarget(null);
            mob.getNavigation().stop();
        }
    }

    private static boolean tryDismountControlledSpider(LivingEntity host, PossessionInput input) {
        if (!input.shifting()
                || !host.getType().is(ModTags.HUMANOID_SKELETONS)
                || !(host.getVehicle() instanceof Spider)) {
            return false;
        }
        host.stopRiding();
        host.setDeltaMovement(host.getDeltaMovement().add(0.0D, 0.08D, 0.0D));
        host.hasImpulse = true;
        return true;
    }

    private static LivingEntity controlledMovementEntity(LivingEntity host) {
        Entity vehicle = host.getVehicle();
        if (host.getType().is(ModTags.HUMANOID_SKELETONS)
                && vehicle instanceof Spider spider
                && spider.isAlive()) {
            return spider;
        }
        return host;
    }

    public static void controlHostForTest(ServerPlayer player, LivingEntity host) {
        if (usesBeta16PlayerBody(host)) {
            updateBeta16PlayerBodyParameters(player, host);
            applyBeta16MobilityToPlayer(player, host);
            syncBeta16HostToPlayer(player, host);
            return;
        }
        controlHost(player, host, true, false);
    }

    public static void processHostAbilitiesForTest(LivingEntity host) {
        processBlazeAttack(host);
    }

    private static float verticalInput(PossessionInput input) {
        if (input.jumping() == input.shifting()) {
            return 0.0F;
        }
        return input.jumping() ? 1.0F : -1.0F;
    }

    private static void moveSpecialMobilityHost(LivingEntity host,
                                                PossessionInput input,
                                                EntityMobilityManager.MobilityProfile mobility,
                                                boolean verticalMovement,
                                                boolean movingHorizontally) {
        Vec3 forward = Vec3.directionFromRotation(0.0F, input.yRot());
        Vec3 right = Vec3.directionFromRotation(0.0F, input.yRot() + 90.0F);
        Vec3 requested = forward.scale(input.forward()).add(right.scale(input.strafe()));
        Vec3 current = host.getDeltaMovement();

        Vec3 horizontal = movingHorizontally
                ? requested.normalize().scale(specialMobilitySpeed(host, mobility))
                : new Vec3(
                        current.x * Math.max(0.0D, Math.min(1.0D, mobility.inertia())),
                        0.0D,
                        current.z * Math.max(0.0D, Math.min(1.0D, mobility.inertia()))
                );
        double y = controlledVerticalSpeed(host, mobility, verticalMovement, current.y, input);
        Vec3 movement = new Vec3(horizontal.x, y, horizontal.z);
        host.setDeltaMovement(movement);
        host.move(net.minecraft.world.entity.MoverType.SELF, movement);
    }

    private static double specialMobilitySpeed(LivingEntity host, EntityMobilityManager.MobilityProfile mobility) {
        double multiplier = 1.0D;
        if (host.isInWater() || isInPossessedLavaFluid(host)) {
            multiplier += mobility.waterSpeedModifier();
        } else if (host.onGround()) {
            multiplier += mobility.landedSpeedModifier();
        }
        return Math.max(0.02D, host.getAttributeValue(Attributes.MOVEMENT_SPEED))
                * Math.max(0.05D, multiplier)
                * SPECIAL_MOBILITY_CONTROL_SCALE;
    }

    private static boolean usesVanillaTravel(LivingEntity host,
                                             EntityMobilityManager.MobilityProfile mobility,
                                             boolean verticalMovement) {
        return !verticalMovement
                && !mobility.phasesThroughWalls()
                && !mobility.forcesFlight()
                && !mobility.canFly()
                && mobility.walkMode() == EntityMobilityManager.WalkMode.NORMAL
                && mobility.fallSpeedModifier() <= 0.0D
                && !mobility.flopsOnLand()
                && !host.isInWater()
                && !isInPossessedLavaFluid(host);
    }

    private static double controlledVerticalSpeed(LivingEntity host,
                                                  EntityMobilityManager.MobilityProfile mobility,
                                                  boolean verticalMovement,
                                                  double currentY,
                                                  PossessionInput input) {
        if (verticalMovement) {
            if (input.jumping()) {
                return specialMobilitySpeed(host, mobility);
            }
            if (input.shifting()) {
                return -specialMobilitySpeed(host, mobility);
            }
            return currentY * Math.max(0.0D, Math.min(1.0D, mobility.inertia()));
        }
        if (host.isNoGravity()) {
            return currentY;
        }
        if (hasGroundSupport(host)) {
            return currentY < 0.0D ? 0.0D : currentY;
        }
        return currentY - mobility.gravity();
    }

    private static void applyControlledRotation(LivingEntity entity, float yRot, float xRot, boolean preservePrevious) {
        float previousYRot = entity.getYRot();
        float previousXRot = entity.getXRot();
        float previousYHeadRot = entity.yHeadRot;
        float previousYBodyRot = entity.yBodyRot;
        entity.setYRot(yRot);
        entity.setYHeadRot(yRot);
        entity.yBodyRot = yRot;
        entity.yRotO = preservePrevious ? previousYRot : yRot;
        entity.yHeadRotO = preservePrevious ? previousYHeadRot : yRot;
        entity.yBodyRotO = preservePrevious ? previousYBodyRot : yRot;
        entity.setXRot(xRot);
        entity.xRotO = preservePrevious ? previousXRot : xRot;
    }

    private static boolean hasGroundSupport(LivingEntity host) {
        return host.onGround();
    }

    private static void applyPassiveMobility(LivingEntity host,
                                             EntityMobilityManager.MobilityProfile mobility,
                                             boolean moving) {
        Vec3 current = host.getDeltaMovement();
        boolean changed = false;
        double y = current.y;

        if (!host.isNoGravity() && !host.onGround() && mobility.fallSpeedModifier() > 0.0D && y < 0.0D) {
            y *= Math.max(0.0D, 1.0D - mobility.fallSpeedModifier());
            host.fallDistance = 0.0F;
            changed = true;
        }
        if (mobility.canFly()) {
            host.fallDistance = 0.0F;
        }

        if (mobility.flopsOnLand() && !host.isInWater() && host.onGround()) {
            y = moving ? Math.max(y, 0.18D) : Math.max(y, 0.08D);
            changed = true;
        }

        boolean inSupportedFluid = host.isInWater() || isInPossessedLavaFluid(host);
        if (mobility.swimMode() == EntityMobilityManager.MobilityMode.SINKING && inSupportedFluid) {
            y = Math.min(y, -Math.max(0.02D, mobility.gravity()));
            changed = true;
        } else if (mobility.swimMode() == EntityMobilityManager.MobilityMode.FLOATING && inSupportedFluid) {
            y = Math.max(y, 0.04D);
            changed = true;
        }

        if (changed) {
            host.setDeltaMovement(current.x, y, current.z);
            host.hasImpulse = true;
        }
    }

    private static boolean canMoveVertically(LivingEntity host, EntityMobilityManager.MobilityProfile mobility) {
        return mobility.canFly() || mobility.canSwim() && (host.isInWater() || isInPossessedLavaFluid(host));
    }

    private static boolean canMoveVertically(LivingEntity host,
                                             EntityMobilityManager.MobilityProfile mobility,
                                             PossessionInput input) {
        return mobility.forcesFlight()
                || mobility.canSwim() && (host.isInWater() || isInPossessedLavaFluid(host))
                || mobility.canFly() && (input.jumping() || input.shifting());
    }

    private static boolean isInPossessedLavaFluid(LivingEntity host) {
        return host.isInLava() && hasFireVision(host.getType());
    }

    private static void syncPossessorToHost(ServerPlayer player, LivingEntity host, boolean forcePosition) {
        if (usesBeta16PlayerBody(host)) {
            syncBeta16HostToPlayer(player, host);
            return;
        }
        player.stopRiding();
        player.fallDistance = 0.0F;
        player.setDeltaMovement(Vec3.ZERO);
        suppressPossessorBody(player);
        if (player.connection != null && player.getCamera() != host) {
            player.setCamera(host);
        }

        boolean mustResyncPosition = forcePosition
                || player.level() != host.level()
                || player.distanceToSqr(host) > POSSESSOR_RESYNC_DISTANCE_SQR;
        if (!mustResyncPosition) {
            return;
        }

        if (player.connection != null) {
            player.connection.teleport(
                    host.getX(),
                    host.getY(),
                    host.getZ(),
                    player.getYRot(),
                    player.getXRot()
            );
        } else {
            player.absMoveTo(
                    host.getX(),
                    host.getY(),
                    host.getZ(),
                    player.getYRot(),
                    player.getXRot()
            );
        }
    }

    private static void suppressPossessorBody(ServerPlayer player) {
        if (!player.getPersistentData().contains(ORIGINAL_PLAYER_NO_PHYSICS, Tag.TAG_BYTE)) {
            boolean originalNoPhysics = player.getPersistentData().contains(ORIGINAL_NO_PHYSICS, Tag.TAG_BYTE)
                    ? player.getPersistentData().getBoolean(ORIGINAL_NO_PHYSICS)
                    : player.noPhysics;
            player.getPersistentData().putBoolean(ORIGINAL_PLAYER_NO_PHYSICS, originalNoPhysics);
        }
        if (!player.getPersistentData().contains(ORIGINAL_PLAYER_MAYFLY, Tag.TAG_BYTE)) {
            player.getPersistentData().putBoolean(ORIGINAL_PLAYER_MAYFLY, player.getAbilities().mayfly);
            player.getPersistentData().putBoolean(ORIGINAL_PLAYER_FLYING, player.getAbilities().flying);
        }
        if (!player.getPersistentData().contains(ORIGINAL_PLAYER_NO_GRAVITY, Tag.TAG_BYTE)) {
            player.getPersistentData().putBoolean(ORIGINAL_PLAYER_NO_GRAVITY, player.isNoGravity());
        }
        player.noPhysics = true;
        player.setNoGravity(true);
        if (player.isCreative()) {
            player.getAbilities().mayfly = true;
            updateAbilities(player);
        } else if (!player.isSpectator()) {
            player.getAbilities().mayfly = false;
            player.getAbilities().flying = false;
            updateAbilities(player);
        }
        player.setInvisible(false);
    }

    private static void startBeta16PlayerBodyPossession(ServerPlayer player, LivingEntity host) {
        player.getPersistentData().putBoolean(BETA16_PLAYER_BODY, true);
        restorePossessorBody(player);
        player.stopRiding();
        if (player.connection != null && player.getCamera() != player) {
            player.setCamera(player);
        }
        if (host instanceof Mob mob) {
            mob.setNoAi(true);
            mob.getNavigation().stop();
            mob.setTarget(null);
        }
        host.noPhysics = true;
        host.setNoGravity(true);
        applyBeta16PlayerBodyParameters(player, host);
        if (player.connection != null) {
            player.connection.teleport(host.getX(), host.getY(), host.getZ(), host.getYRot(), host.getXRot());
        } else {
            player.absMoveTo(host.getX(), host.getY(), host.getZ(), host.getYRot(), host.getXRot());
        }
        player.setDeltaMovement(host.getDeltaMovement());
        player.fallDistance = host.fallDistance;
        syncBeta16HostToPlayer(player, host);
    }

    private static void applyBeta16PlayerBodyParameters(ServerPlayer player, LivingEntity host) {
        EntityAccessor accessor = (EntityAccessor) player;
        if (!player.getPersistentData().contains(ORIGINAL_PLAYER_MAX_UP_STEP, Tag.TAG_FLOAT)) {
            player.getPersistentData().putFloat(ORIGINAL_PLAYER_MAX_UP_STEP, accessor.requiemunofficial$getMaxUpStep());
        }
        accessor.requiemunofficial$setMaxUpStep(Math.max(accessor.requiemunofficial$getMaxUpStep(), MIN_POSSESSED_STEP_HEIGHT));

        if (!player.getPersistentData().contains(ORIGINAL_PLAYER_NO_GRAVITY, Tag.TAG_BYTE)) {
            player.getPersistentData().putBoolean(ORIGINAL_PLAYER_NO_GRAVITY, player.isNoGravity());
        }
        updateBeta16PlayerBodyParameters(player, host);
        player.refreshDimensions();
        player.setInvisible(false);
        player.noPhysics = false;
        applyBeta16FlightAbilities(player, host);
    }

    private static void updateBeta16PlayerBodyParameters(ServerPlayer player, LivingEntity host) {
        AttributeInstance playerSpeed = player.getAttribute(Attributes.MOVEMENT_SPEED);
        if (playerSpeed != null) {
            playerSpeed.removeModifier(BETA16_PLAYER_SPEED_MODIFIER);
            double targetSpeed = beta16PlayerBodyMovementSpeed(player, host);
            double amount = targetSpeed - playerSpeed.getBaseValue();
            if (Math.abs(amount) > 1.0E-6D) {
                playerSpeed.addTransientModifier(new AttributeModifier(
                        BETA16_PLAYER_SPEED_MODIFIER,
                        "Requiem beta16 host movement speed",
                        amount,
                        AttributeModifier.Operation.ADDITION
                ));
            }
        }
    }

    private static double beta16PlayerBodyMovementSpeed(ServerPlayer player, LivingEntity host) {
        double hostSpeed = Math.max(0.0D, host.getAttributeValue(Attributes.MOVEMENT_SPEED));
        double targetSpeed = isHumanoidWalkingHost(host)
                ? hostSpeed * HUMANOID_PLAYER_BODY_SPEED_SCALE
                : isBeta16FlyingHost(host)
                ? hostSpeed * SPECIAL_MOBILITY_CONTROL_SCALE
                : hostSpeed;
        return Math.max(0.0D, targetSpeed);
    }

    private static void applyBeta16MobilityToPlayer(ServerPlayer player, LivingEntity host) {
        EntityMobilityManager.MobilityProfile mobility = EntityMobilityManager.get(host.getType());
        applyBeta16FlightAbilities(player, host);
        if (!isBeta16FlyingHost(host)) {
            return;
        }

        PossessionInput input = POSSESSION_INPUTS.get(player.getUUID());
        Vec3 current = player.getDeltaMovement();
        double y = controlledVerticalSpeed(host, mobility, input != null, current.y,
                input == null ? new PossessionInput(0.0F, 0.0F, false, false, player.getYRot(), player.getXRot()) : input);
        if (Math.abs(y - current.y) > 1.0E-5D) {
            player.setDeltaMovement(current.x, y, current.z);
            player.hasImpulse = true;
        }
        player.fallDistance = 0.0F;
    }

    private static void applyBeta16FlightAbilities(ServerPlayer player, LivingEntity host) {
        boolean flyingHost = isBeta16FlyingHost(host);
        player.setNoGravity(flyingHost);
        if (player.isCreative() || player.isSpectator()) {
            return;
        }
        boolean changed = player.getAbilities().mayfly != flyingHost
                || player.getAbilities().flying && !flyingHost;
        player.getAbilities().mayfly = flyingHost;
        if (!flyingHost) {
            player.getAbilities().flying = false;
        }
        if (changed) {
            updateAbilities(player);
        }
    }

    private static void restoreBeta16PlayerBodyParameters(ServerPlayer player) {
        player.getPersistentData().remove(BETA16_PLAYER_BODY);
        AttributeInstance playerSpeed = player.getAttribute(Attributes.MOVEMENT_SPEED);
        if (playerSpeed != null) {
            playerSpeed.removeModifier(BETA16_PLAYER_SPEED_MODIFIER);
        }
        if (player.getPersistentData().contains(ORIGINAL_PLAYER_MAX_UP_STEP, Tag.TAG_FLOAT)) {
            ((EntityAccessor) player).requiemunofficial$setMaxUpStep(
                    player.getPersistentData().getFloat(ORIGINAL_PLAYER_MAX_UP_STEP)
            );
            player.getPersistentData().remove(ORIGINAL_PLAYER_MAX_UP_STEP);
        }
        player.refreshDimensions();
    }

    private static void restorePossessorBody(ServerPlayer player) {
        if (player.getPersistentData().contains(ORIGINAL_PLAYER_NO_PHYSICS, Tag.TAG_BYTE)) {
            player.noPhysics = player.getPersistentData().getBoolean(ORIGINAL_PLAYER_NO_PHYSICS);
            player.getPersistentData().remove(ORIGINAL_PLAYER_NO_PHYSICS);
        } else if (SoulManager.getPossessedEntityId(player) == null) {
            player.noPhysics = false;
        }
        if (player.getPersistentData().contains(ORIGINAL_PLAYER_MAYFLY, Tag.TAG_BYTE)) {
            if (!player.isCreative()) {
                player.getAbilities().mayfly = player.getPersistentData().getBoolean(ORIGINAL_PLAYER_MAYFLY);
                player.getAbilities().flying = player.getAbilities().mayfly
                        && player.getPersistentData().getBoolean(ORIGINAL_PLAYER_FLYING);
            }
            player.getPersistentData().remove(ORIGINAL_PLAYER_MAYFLY);
            player.getPersistentData().remove(ORIGINAL_PLAYER_FLYING);
            updateAbilities(player);
        }
        if (player.getPersistentData().contains(ORIGINAL_PLAYER_NO_GRAVITY, Tag.TAG_BYTE)) {
            player.setNoGravity(player.getPersistentData().getBoolean(ORIGINAL_PLAYER_NO_GRAVITY));
            player.getPersistentData().remove(ORIGINAL_PLAYER_NO_GRAVITY);
        }
    }

    private static void updateAbilities(ServerPlayer player) {
        if (player.connection != null && player.connection.connection != null) {
            player.onUpdateAbilities();
        }
    }

    private static LivingEntity findHost(ServerPlayer player, UUID hostId) {
        for (ServerLevel level : player.getServer().getAllLevels()) {
            Entity entity = level.getEntity(hostId);
            if (entity instanceof LivingEntity living) {
                return living;
            }
        }
        return null;
    }

    private static UUID getPossessor(LivingEntity host) {
        return host.getPersistentData().hasUUID(POSSESSOR)
                ? host.getPersistentData().getUUID(POSSESSOR)
                : null;
    }

    private static boolean isWithinReleaseInputGrace(ServerPlayer player) {
        Long startedAt = POSSESSION_STARTED_AT.get(player.getUUID());
        return startedAt != null
                && player.level().getGameTime() - startedAt < RELEASE_INPUT_GRACE_TICKS;
    }

    private static float clampInput(float value) {
        if (Float.isNaN(value) || Float.isInfinite(value)) {
            return 0.0F;
        }
        return Math.max(-1.0F, Math.min(1.0F, value));
    }

    private static boolean isFinite(float value) {
        return !Float.isNaN(value) && !Float.isInfinite(value);
    }

    private record PendingPossession(UUID targetId, long completeGameTime) {
    }

    private record PossessionInput(float forward,
                                   float strafe,
                                   boolean jumping,
                                   boolean shifting,
                                   float yRot,
                                   float xRot) {
    }

    private record PossessionControl(ServerPlayer player, LivingEntity host) {
    }
}
