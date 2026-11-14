package ua.meekeyn.requiemunofficial.common.soul;

import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import ua.meekeyn.requiemunofficial.RequiemUnofficial;
import ua.meekeyn.requiemunofficial.common.network.ModNetwork;
import ua.meekeyn.requiemunofficial.common.possession.PossessionManager;
import ua.meekeyn.requiemunofficial.common.remnant.DroppedVesselTracker;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@EventBusSubscriber(modid = RequiemUnofficial.MODID)
public final class PlayerSoulEvents {
    private static final Map<UUID, PendingSoulReleasePosition> PENDING_DEATH_RELEASE_POSITIONS = new HashMap<>();

    private PlayerSoulEvents() {
    }

    public static void rememberPossessedBodyDeath(net.minecraft.server.level.ServerPlayer player,
                                                  ResourceKey<Level> dimension,
                                                  Vec3 position,
                                                  float yRot,
                                                  float xRot) {
        PENDING_DEATH_RELEASE_POSITIONS.put(
                player.getUUID(),
                new PendingSoulReleasePosition(dimension, position, yRot, xRot)
        );
    }

    @SubscribeEvent
    public static void onPlayerClone(PlayerEvent.Clone event) {

        boolean wasPossessingOnDeath = event.isWasDeath()
                && event.getOriginal() instanceof net.minecraft.server.level.ServerPlayer oldPlayerForState
                && SoulManager.getPossessedEntityId(oldPlayerForState) != null;
        if (event.isWasDeath()
                && event.getOriginal() instanceof net.minecraft.server.level.ServerPlayer oldPlayer
                && SoulManager.getPossessedEntityId(oldPlayer) != null) {
            java.util.UUID possessedId = SoulManager.getPossessedEntityId(oldPlayer);
            net.minecraft.world.entity.LivingEntity host =
                    possessedId == null ? null : PossessionManager.findPossessedHost(oldPlayer, possessedId);
            if (host != null) {
                PossessionManager.releaseFromHostDeathForTest(oldPlayer, host);
            } else {
                PossessionManager.release(oldPlayer, null);
            }
        }

        ModCapabilities.soul(event.getOriginal()).ifPresent(oldSoul -> {
            ModCapabilities.soul(event.getEntity()).ifPresent(newSoul -> {
                // Копіює дані душі в нового гравця після смерті/респавну
                newSoul.copyFrom(oldSoul);
                if (event.isWasDeath()
                        && (oldSoul.getState() == SoulState.REMNANT
                        || oldSoul.getState() == SoulState.VAGRANT
                        || oldSoul.getPossessedEntityId() != null
                        || wasPossessingOnDeath)) {
                    newSoul.setState(SoulState.VAGRANT);
                    newSoul.clearPossessedEntityId();
                    if (event.getOriginal() instanceof net.minecraft.server.level.ServerPlayer oldPlayer
                            && event.getEntity() instanceof net.minecraft.server.level.ServerPlayer newPlayer) {
                        copyStickySoulEffects(oldPlayer, newPlayer);
                    }
                }
            });
        });

        SoulManager.sync(event.getEntity());
    }

    @SubscribeEvent
    public static void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof net.minecraft.server.level.ServerPlayer serverPlayer) {
            boolean restoredVessel = DroppedVesselTracker.tryMergeWithVessel(serverPlayer);
            java.util.UUID possessedId = SoulManager.getPossessedEntityId(serverPlayer);
            if (!restoredVessel && possessedId != null) {
                net.minecraft.world.entity.LivingEntity host =
                        PossessionManager.findPossessedHost(serverPlayer, possessedId);
                if (host == null) {
                    host = PossessionManager.restoreDisconnectedHost(serverPlayer);
                }
                if (host != null && !PossessionManager.resumeAfterDisconnect(serverPlayer, host)) {
                    PossessionManager.clearHostPossessionFlags(host);
                    SoulManager.clearPossession(serverPlayer);
                    serverPlayer.setInvisible(false);
                } else if (host == null) {
                    // Тіло ще не завантажене (чанк вантажиться пізніше): чекаємо, а не викидаємо душу.
                    PossessionManager.beginReconnectGrace(serverPlayer);
                }
            } else if (!restoredVessel && SoulManager.getState(serverPlayer) == SoulState.REMNANT) {
                SoulManager.clearPossession(serverPlayer);
                serverPlayer.setInvisible(false);
            }
            ModNetwork.sendSoulData(serverPlayer);
            InitialSoulChoiceManager.initialize(serverPlayer);
            VagrantEvents.refreshAfterSoulStateChange(serverPlayer);
        }
    }

    @SubscribeEvent
    public static void onPlayerRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if (event.getEntity() instanceof net.minecraft.server.level.ServerPlayer serverPlayer) {
            PendingSoulReleasePosition pending = PENDING_DEATH_RELEASE_POSITIONS.remove(serverPlayer.getUUID());
            if (pending != null && serverPlayer.getServer() != null) {
                net.minecraft.server.level.ServerLevel level = serverPlayer.getServer().getLevel(pending.dimension());
                if (level != null) {
                    serverPlayer.teleportTo(level, pending.position().x, pending.position().y, pending.position().z, pending.yRot(), pending.xRot());
                    serverPlayer.fallDistance = 0.0F;
                    serverPlayer.setDeltaMovement(Vec3.ZERO);
                }
            }
            VagrantEvents.refreshAfterSoulStateChange(serverPlayer);
        }
        SoulManager.sync(event.getEntity());
    }

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (event.getEntity() instanceof net.minecraft.server.level.ServerPlayer serverPlayer) {
            ua.meekeyn.requiemunofficial.common.effect.PenanceMobEffect.tickPlayer(serverPlayer);
        }
    }

    /** Новий спостерігач має знати, чи гравець у формі душі (інакше бачитиме його модель). */
    @SubscribeEvent
    public static void onStartTracking(PlayerEvent.StartTracking event) {
        if (event.getEntity() instanceof net.minecraft.server.level.ServerPlayer tracker
                && event.getTarget() instanceof net.minecraft.server.level.ServerPlayer target) {
            ModNetwork.sendSoulVisibilityTo(tracker, target);
        }
    }

    @SubscribeEvent
    public static void onPlayerChangedDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        SoulManager.sync(event.getEntity());
    }

    private static void copyStickySoulEffects(net.minecraft.server.level.ServerPlayer oldPlayer,
                                              net.minecraft.server.level.ServerPlayer newPlayer) {
        for (MobEffectInstance effect : oldPlayer.getActiveEffects()) {
            if (SoulLifecycleManager.shouldKeepEffectThroughSoulRespawn(oldPlayer, effect)) {
                newPlayer.addEffect(new MobEffectInstance(effect));
            }
        }
    }

    private record PendingSoulReleasePosition(ResourceKey<Level> dimension, Vec3 position, float yRot, float xRot) {
    }
}
