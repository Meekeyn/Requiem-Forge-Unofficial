package ua.meekeyn.requiemunofficial.common.soul;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import ua.meekeyn.requiemunofficial.RequiemUnofficial;
import ua.meekeyn.requiemunofficial.common.entity.PlayerShellEntity;
import ua.meekeyn.requiemunofficial.common.effect.AttritionMobEffect;
import ua.meekeyn.requiemunofficial.common.effect.PenanceMobEffect;
import ua.meekeyn.requiemunofficial.common.effect.StickyMobEffect;
import ua.meekeyn.requiemunofficial.common.init.ModEntities;
import ua.meekeyn.requiemunofficial.common.init.ModEffects;
import ua.meekeyn.requiemunofficial.common.init.ModGameRules;
import ua.meekeyn.requiemunofficial.common.ritual.DeathResurrectionManager;

import java.util.UUID;

@Mod.EventBusSubscriber(modid = RequiemUnofficial.MODID)
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
                if (hasLiveConnection(owner)) {
                    owner.sendSystemMessage(Component.translatable(
                            "message.requiemunofficial.soul.shell_destroyed"
                    ));
                }
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
        shell.captureBody(player);
        player.serverLevel().addFreshEntity(shell);
        if (player.getVehicle() != null) {
            shell.startRiding(player.getVehicle(), true);
        }
        SoulManager.setPlayerShell(player, shell.getUUID());
        return shell;
    }

    public static boolean splitPlayer(ServerPlayer player, boolean notify) {
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
        if (notify && hasLiveConnection(player)) {
            player.sendSystemMessage(Component.translatable(
                    "message.requiemunofficial.soul.shell_split"
            ));
        }
        return true;
    }

    public static boolean isSoulboundStack(ItemStack stack, int slot) {
        // FORGE_ADAPTATION: original Requiem exposes SoulboundStackCheckCallback.
        // The Forge port has no callback bus, so Stage 8 uses a minimal NBT marker.
        return !stack.isEmpty()
                && stack.hasTag()
                && stack.getTag().getBoolean("RequiemSoulbound");
    }

    public static boolean isSoulboundEffect(MobEffect effect) {
        return effect == ModEffects.ATTRITION.get()
                || effect == ModEffects.EMANCIPATION.get()
                || effect == ModEffects.PENANCE.get()
                || effect == ModEffects.RECLAMATION.get();
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
        if (PenanceMobEffect.blocksMerge(player.getEffect(ModEffects.PENANCE.get()))) {
            return false;
        }

        shell.restoreBody(player);
        shell.discard();
        if (shell.getUUID().equals(SoulManager.getPlayerShellId(player))) {
            SoulManager.clearPlayerShell(player);
        }
        SoulManager.setState(player, SoulState.REMNANT);
        player.setInvisible(false);
        if (hasLiveConnection(player)) {
            player.sendSystemMessage(Component.translatable(
                    "message.requiemunofficial.soul.shell_reclaimed"
            ));
        }
        return true;
    }

    public static void removeShell(ServerPlayer player, boolean notify) {
        PlayerShellEntity shell = findShell(player);
        if (shell == null) {
            return;
        }

        shell.discard();
        SoulManager.clearPlayerShell(player);
        if (notify && hasLiveConnection(player)) {
            player.sendSystemMessage(Component.translatable(
                    "message.requiemunofficial.soul.shell_removed"
            ));
        }
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
        return player.connection != null && player.connection.connection != null;
    }

    private static void transferSoulboundOrKeptStacks(PlayerShellEntity shell, ServerPlayer player) {
        boolean transferBodyInventory = ModGameRules.shouldTransferPossessionInventory(
                player.level().getGameRules(),
                shell.isAlive()
        );
        shell.transferCapturedStacksToSoul(player, !transferBodyInventory);
    }

    private static void retainSoulboundEffects(ServerPlayer player) {
        for (MobEffect effect : player.getActiveEffectsMap().keySet().toArray(MobEffect[]::new)) {
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
        if (soul.hasEffect(ModEffects.RECLAMATION.get())) {
            if (hasLiveConnection(soul)) {
                soul.removeEffect(ModEffects.RECLAMATION.get());
            } else {
                soul.getActiveEffectsMap().remove(ModEffects.RECLAMATION.get());
            }
        }
    }
}
