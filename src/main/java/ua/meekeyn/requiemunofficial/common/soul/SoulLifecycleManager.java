package ua.meekeyn.requiemunofficial.common.soul;

import net.minecraft.core.Holder;
import net.neoforged.fml.common.EventBusSubscriber;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;
import ua.meekeyn.requiemunofficial.RequiemUnofficial;
import ua.meekeyn.requiemunofficial.common.entity.PlayerShellEntity;
import ua.meekeyn.requiemunofficial.common.effect.AttritionMobEffect;
import ua.meekeyn.requiemunofficial.common.effect.PenanceMobEffect;
import ua.meekeyn.requiemunofficial.common.effect.StickyMobEffect;
import ua.meekeyn.requiemunofficial.common.init.ModEntities;
import ua.meekeyn.requiemunofficial.common.init.ModEffects;
import ua.meekeyn.requiemunofficial.common.init.ModGameRules;
import ua.meekeyn.requiemunofficial.common.possession.PossessionManager;
import ua.meekeyn.requiemunofficial.common.ritual.DeathResurrectionManager;

import java.util.UUID;

@EventBusSubscriber(modid = RequiemUnofficial.MODID)
public final class SoulLifecycleManager {

    private SoulLifecycleManager() {
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onLivingDeath(LivingDeathEvent event) {
        if (event.getEntity().level().isClientSide()) {
            return;
        }

        DeathSuspensionManager.DeathInterception initialDeath =
                DeathSuspensionManager.handleInitialDeath(event);
        if (initialDeath == DeathSuspensionManager.DeathInterception.CANCEL) {
            return;
        }

        if (event.getEntity() instanceof ServerPlayer player
                && SoulManager.getState(player) == SoulState.REMNANT
                && initialDeath != DeathSuspensionManager.DeathInterception.ALLOW_VANILLA_DEATH) {
            if (player.serverLevel().getLevelData().isHardcore()) {
                SoulManager.setState(player, SoulState.MORTAL);
                return;
            }
            if (DeathResurrectionManager.tryHandleRemnantDeath(player, event.getSource())) {
                event.setCanceled(true);
                return;
            }
            if (SoulManager.getPossessedEntityId(player) != null) {
                SoulManager.clearPossession(player);
            }
            SoulManager.clearPlayerShell(player);
            PlayerSoulEvents.rememberPossessedBodyDeath(
                    player,
                    player.level().dimension(),
                    player.position(),
                    player.getYRot(),
                    player.getXRot()
            );
            return;
        }

        if (event.getEntity() instanceof ServerPlayer player
                && SoulManager.getState(player) == SoulState.VAGRANT) {
            PlayerSoulEvents.rememberPossessedBodyDeath(
                    player,
                    player.level().dimension(),
                    player.position(),
                    player.getYRot(),
                    player.getXRot()
            );
        }

        if (event.getEntity() instanceof PlayerShellEntity shell && shell.getOwnerId() != null) {
            ServerPlayer owner = shell.getServer() == null
                    ? null
                    : shell.getServer().getPlayerList().getPlayer(shell.getOwnerId());
            if (owner != null && shell.getUUID().equals(SoulManager.getPlayerShellId(owner))) {
                SoulManager.clearPlayerShell(owner);
            }
        }
    }

    private static void becomeVagrantOnDeath(ServerPlayer player) {
        AttritionMobEffect.apply(player, 1, 20 * 60 * 5);
        if (SoulManager.getPossessedEntityId(player) != null) {
            SoulManager.clearPossession(player);
        }

        player.setHealth(Math.max(1.0F, player.getMaxHealth()));
        player.fallDistance = 0.0F;
        player.setDeltaMovement(0.0D, 0.0D, 0.0D);
        player.stopUsingItem();

        PlayerShellEntity shell = createShell(player);
        finishBodySplitAfterCapture(player, shell);
        SoulManager.setState(player, SoulState.VAGRANT);
        player.setInvisible(true);
        VagrantEvents.refreshAfterSoulStateChange(player);
        SoulManager.sync(player);
    }

    public static PlayerShellEntity createShell(ServerPlayer player) {
        PlayerShellEntity shell = new PlayerShellEntity(ModEntities.PLAYER_SHELL.get(), player.serverLevel());
        shell.moveTo(player.getX(), player.getY(), player.getZ(), player.getYRot(), player.getXRot());
        if (player.getVehicle() == null) {
            Vec3 safe = findShellPosition(player, shell);
            shell.moveTo(safe.x, safe.y, safe.z, player.getYRot(), player.getXRot());
        }
        shell.captureBody(player);
        player.serverLevel().addFreshEntity(shell);
        if (player.getVehicle() != null) {
            shell.startRiding(player.getVehicle(), true);
        }
        SoulManager.setPlayerShell(player, shell.getUUID());
        return shell;
    }

    /**
     * Позиція оболонки без колізій із блоками: поточна, якщо вільна, інакше найближча вільна
     * клітинка поруч (та сама логіка кандидатів, що й PossessionManager.findReleasePosition).
     */
    private static Vec3 findShellPosition(ServerPlayer player, PlayerShellEntity shell) {
        if (player.level().noCollision(shell, shell.getBoundingBox())) {
            return shell.position();
        }
        BlockPos base = player.blockPosition();
        BlockPos[] candidates = {
                base,
                base.above(),
                base.north(),
                base.south(),
                base.east(),
                base.west(),
                base.north().above(),
                base.south().above(),
                base.east().above(),
                base.west().above()
        };
        for (BlockPos candidate : candidates) {
            if (!PossessionManager.isSafeReleasePosition(player, candidate)) {
                continue;
            }
            Vec3 position = Vec3.atBottomCenterOf(candidate);
            if (player.level().noCollision(shell, shell.getType().getDimensions().makeBoundingBox(position))) {
                return position;
            }
        }
        return shell.position();
    }

    public static boolean splitPlayer(ServerPlayer player) {
        if (SoulManager.getState(player) != SoulState.REMNANT
                || SoulManager.getPossessedEntityId(player) != null) {
            return false;
        }

        PlayerShellEntity shell = createShell(player);
        player.stopUsingItem();
        finishBodySplitAfterCapture(player, shell);
        SoulManager.setState(player, SoulState.VAGRANT);
        player.setInvisible(true);
        VagrantEvents.refreshAfterSoulStateChange(player);
        SoulManager.sync(player);
        return true;
    }

    public static boolean isSoulboundStack(ItemStack stack, int slot) {
        // FORGE_ADAPTATION: original Requiem exposes SoulboundStackCheckCallback.
        // The Forge port has no callback bus, so Stage 8 uses a minimal NBT marker.
        return !stack.isEmpty()
                && ua.meekeyn.requiemunofficial.common.util.ItemNbt.hasTag(stack)
                && ua.meekeyn.requiemunofficial.common.util.ItemNbt.getTag(stack).getBoolean("RequiemSoulbound");
    }

    public static boolean isSoulboundEffect(Holder<MobEffect> effect) {
        return effect.is(ModEffects.ATTRITION)
                || effect.is(ModEffects.EMANCIPATION)
                || effect.is(ModEffects.PENANCE)
                || effect.is(ModEffects.RECLAMATION);
    }

    public static boolean shouldKeepEffectThroughSoulRespawn(ServerPlayer player, MobEffectInstance effect) {
        return StickyMobEffect.shouldStick(effect, player);
    }

    public static void finishBodySplitAfterCapture(ServerPlayer player, PlayerShellEntity shell) {
        player.getInventory().clearContent();
        retainSoulboundEffects(player);
        clearReclamation(player);
        transferSoulboundOrKeptStacks(shell, player);
    }

    public static boolean tryReclaimShell(ServerPlayer player, PlayerShellEntity shell) {
        if (SoulManager.getState(player) != SoulState.VAGRANT
                || !shell.belongsTo(player.getUUID())
                || !shell.hasCapturedBodyInventory()
                || !shell.isAlive()) {
            return false;
        }
        if (PenanceMobEffect.blocksMerge(player.getEffect(ModEffects.PENANCE))) {
            return false;
        }

        shell.restoreBody(player);
        shell.discard();
        if (shell.getUUID().equals(SoulManager.getPlayerShellId(player))) {
            SoulManager.clearPlayerShell(player);
        }
        SoulManager.setState(player, SoulState.REMNANT);
        player.setInvisible(false);
        return true;
    }

    public static boolean mergeShellForCommand(ServerPlayer player, PlayerShellEntity shell) {
        // Оригінал /requiem shell merge: RemnantComponentImpl.merge без перевірки власника
        // (лише PRE_MERGE/Penance і setVagrant(false, MERGE)).
        if (SoulManager.getState(player) != SoulState.VAGRANT || !shell.isAlive()) {
            return false;
        }
        if (PenanceMobEffect.blocksMerge(player.getEffect(ModEffects.PENANCE))) {
            return false;
        }

        UUID ownerId = shell.getOwnerId();
        shell.restoreBody(player);
        shell.discard();
        if (shell.getUUID().equals(SoulManager.getPlayerShellId(player))) {
            SoulManager.clearPlayerShell(player);
        }
        // Чужа оболонка: знімаємо прив'язку в справжнього власника, якщо він онлайн.
        if (ownerId != null && !ownerId.equals(player.getUUID()) && player.getServer() != null) {
            ServerPlayer owner = player.getServer().getPlayerList().getPlayer(ownerId);
            if (owner != null && shell.getUUID().equals(SoulManager.getPlayerShellId(owner))) {
                SoulManager.clearPlayerShell(owner);
            }
        }
        SoulManager.setState(player, SoulState.REMNANT);
        player.setInvisible(false);
        return true;
    }

    public static void removeShell(ServerPlayer player) {
        PlayerShellEntity shell = findShell(player);
        if (shell == null) {
            return;
        }

        shell.discard();
        SoulManager.clearPlayerShell(player);
    }

    public static PlayerShellEntity findShell(ServerPlayer player) {
        UUID shellId = SoulManager.getPlayerShellId(player);
        if (shellId == null || player.getServer() == null) {
            return null;
        }
        for (ServerLevel level : player.getServer().getAllLevels()) {
            Entity entity = level.getEntity(shellId);
            if (entity instanceof PlayerShellEntity shell) {
                return shell;
            }
        }
        return null;
    }

    private static boolean hasLiveConnection(ServerPlayer player) {
        return player.connection != null && player.connection.getConnection() != null;
    }

    private static void transferSoulboundOrKeptStacks(PlayerShellEntity shell, ServerPlayer player) {
        boolean transferBodyInventory = ModGameRules.shouldTransferPossessionInventory(
                player.level().getGameRules(),
                shell.isAlive()
        );
        shell.transferCapturedStacksToSoul(player, !transferBodyInventory);
    }

    private static void retainSoulboundEffects(ServerPlayer player) {
        for (Holder<MobEffect> effect : player.getActiveEffectsMap().keySet().toArray(Holder[]::new)) {
            if (!isSoulboundEffect(effect)) {
                if (hasLiveConnection(player)) {
                    player.removeEffect(effect);
                } else {
                    player.getActiveEffectsMap().remove(effect);
                }
            }
        }
    }

    public static void clearReclamation(ServerPlayer soul) {
        if (soul.hasEffect(ModEffects.RECLAMATION)) {
            soul.level().playSound(null, soul.getX(), soul.getY(), soul.getZ(),
                    ua.meekeyn.requiemunofficial.common.init.ModSounds.EFFECT_RECLAMATION_CLEAR.get(),
                    net.minecraft.sounds.SoundSource.PLAYERS, 1.0F, 0.8F);
            if (hasLiveConnection(soul)) {
                soul.removeEffect(ModEffects.RECLAMATION);
            } else {
                soul.getActiveEffectsMap().remove(ModEffects.RECLAMATION);
            }
        }
    }
}
