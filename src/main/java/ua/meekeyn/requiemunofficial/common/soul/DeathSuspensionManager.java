package ua.meekeyn.requiemunofficial.common.soul;

import net.minecraft.advancements.Advancement;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import ua.meekeyn.requiemunofficial.RequiemUnofficial;
import ua.meekeyn.requiemunofficial.common.gamerule.StartingRemnantType;
import ua.meekeyn.requiemunofficial.common.init.ModGameRules;
import ua.meekeyn.requiemunofficial.common.init.ModSounds;
import ua.meekeyn.requiemunofficial.common.network.DeathSuspensionFxPacket;
import ua.meekeyn.requiemunofficial.common.network.ModNetwork;
import ua.meekeyn.requiemunofficial.common.network.OpenSoulChoiceScreenPacket;
import ua.meekeyn.requiemunofficial.common.network.SoulChoiceFxPacket;
import ua.meekeyn.requiemunofficial.common.util.ModIds;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Mod.EventBusSubscriber(modid = RequiemUnofficial.MODID)
public final class DeathSuspensionManager {
    private static final ResourceLocation THE_CHOICE = ModIds.id("adventure/the_choice");
    private static final String TAG = "RequiemDeathSuspension";
    private static final String SUSPENDED = "Suspended";
    private static final String COUNTDOWN = "Countdown";
    private static final String CHOICE_OPENED = "ChoiceOpened";
    private static final String RESUME_FALLBACK = "ResumeWithFallbackCause";
    private static final int CHOICE_DELAY_TICKS = 20;

    private static final Map<UUID, DamageSource> LIVE_DEATH_CAUSES = new HashMap<>();
    private static final ThreadLocal<Boolean> RESUMING_VANILLA_DEATH =
            ThreadLocal.withInitial(() -> false);

    private DeathSuspensionManager() {
    }

    public enum DeathInterception {
        PASS,
        CANCEL,
        ALLOW_VANILLA_DEATH
    }

    public static DeathInterception handleInitialDeath(LivingDeathEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return DeathInterception.PASS;
        }
        if (isResumingVanillaDeath()) {
            clear(player);
            return DeathInterception.ALLOW_VANILLA_DEATH;
        }
        if (isSuspended(player)) {
            event.setCanceled(true);
            keepSuspendedPlayerStable(player);
            return DeathInterception.CANCEL;
        }
        if (InitialSoulChoiceManager.hasChosen(player)
                || SoulManager.getState(player) != SoulState.MORTAL) {
            return DeathInterception.PASS;
        }
        if (player.serverLevel().getLevelData().isHardcore()) {
            return DeathInterception.PASS;
        }

        StartingRemnantType mode = ModGameRules.getStartingRemnantType(player.level().getGameRules());
        if (mode == StartingRemnantType.FORCE_REMNANT
                || mode == StartingRemnantType.FORCE_VANILLA) {
            InitialSoulChoiceManager.applyChoice(player, InitialSoulChoiceManager.forcedChoice(player));
            return DeathInterception.ALLOW_VANILLA_DEATH;
        }

        suspend(player, event.getSource());
        event.setCanceled(true);
        return DeathInterception.CANCEL;
    }

    @SubscribeEvent
    public static void onLivingHurt(LivingHurtEvent event) {
        if (event.getEntity() instanceof ServerPlayer player && isSuspended(player)) {
            event.setCanceled(true);
            keepSuspendedPlayerStable(player);
        }
    }

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.START || !(event.player instanceof ServerPlayer player)) {
            return;
        }
        if (!isSuspended(player)) {
            return;
        }

        keepSuspendedPlayerStable(player);
        CompoundTag state = state(player);
        int countdown = Math.max(0, state.getInt(COUNTDOWN));
        if (countdown > 0) {
            state.putInt(COUNTDOWN, countdown - 1);
            return;
        }
        if (!state.getBoolean(CHOICE_OPENED)) {
            state.putBoolean(CHOICE_OPENED, true);
            ModNetwork.sendToPlayer(player, new OpenSoulChoiceScreenPacket());
        }
    }

    @SubscribeEvent
    public static void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || !isSuspended(player)) {
            return;
        }

        state(player).putBoolean(RESUME_FALLBACK, true);
        state(player).putBoolean(CHOICE_OPENED, false);
        keepSuspendedPlayerStable(player);
        ModNetwork.sendToPlayer(player, new DeathSuspensionFxPacket(true));
    }

    @SubscribeEvent
    public static void onPlayerLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            LIVE_DEATH_CAUSES.remove(player.getUUID());
        }
    }

    public static void choose(ServerPlayer player, SoulState choice) {
        if (!isSuspended(player)
                || InitialSoulChoiceManager.hasChosen(player)
                || choice != SoulState.MORTAL && choice != SoulState.REMNANT) {
            return;
        }

        InitialSoulChoiceManager.applyChoice(player, choice);
        playChoiceFeedback(player, choice);
        resumeDeath(player);
    }

    public static boolean isSuspended(Player player) {
        return state(player).getBoolean(SUSPENDED);
    }

    public static boolean hasOpenedChoiceForTest(Player player) {
        return state(player).getBoolean(CHOICE_OPENED);
    }

    public static int countdownForTest(Player player) {
        return state(player).getInt(COUNTDOWN);
    }

    public static boolean isResumingVanillaDeath() {
        return RESUMING_VANILLA_DEATH.get();
    }

    private static void suspend(ServerPlayer player, DamageSource source) {
        CompoundTag state = state(player);
        if (state.getBoolean(SUSPENDED)) {
            return;
        }

        LIVE_DEATH_CAUSES.put(player.getUUID(), source);
        state.putBoolean(SUSPENDED, true);
        state.putInt(COUNTDOWN, CHOICE_DELAY_TICKS);
        state.putBoolean(CHOICE_OPENED, false);
        state.putBoolean(RESUME_FALLBACK, false);
        keepSuspendedPlayerStable(player);
        ModNetwork.sendToPlayer(player, new DeathSuspensionFxPacket(true));
    }

    private static void resumeDeath(ServerPlayer player) {
        DamageSource source = LIVE_DEATH_CAUSES.remove(player.getUUID());
        if (source == null) {
            source = player.damageSources().generic();
            state(player).putBoolean(RESUME_FALLBACK, true);
        }

        clear(player);
        ModNetwork.sendToPlayer(player, new DeathSuspensionFxPacket(false));
        player.setInvulnerable(false);
        player.setHealth(1.0F);
        RESUMING_VANILLA_DEATH.set(true);
        try {
            player.hurt(source, Float.MAX_VALUE);
        } finally {
            RESUMING_VANILLA_DEATH.set(false);
        }
    }

    private static void keepSuspendedPlayerStable(ServerPlayer player) {
        player.setHealth(Math.max(1.0F, player.getHealth()));
        player.setInvulnerable(true);
        player.stopUsingItem();
        player.setDeltaMovement(0.0D, 0.0D, 0.0D);
        player.fallDistance = 0.0F;
    }

    private static void clear(Player player) {
        LIVE_DEATH_CAUSES.remove(player.getUUID());
        CompoundTag state = state(player);
        state.putBoolean(SUSPENDED, false);
        state.putInt(COUNTDOWN, 0);
        state.putBoolean(CHOICE_OPENED, false);
    }

    private static void playChoiceFeedback(ServerPlayer player, SoulState choice) {
        if (choice != SoulState.REMNANT) {
            ModNetwork.sendToPlayer(player, new SoulChoiceFxPacket(choice));
            return;
        }

        player.level().playSound(
                null,
                player.getX(),
                player.getY(),
                player.getZ(),
                ModSounds.EFFECT_BECOME_REMNANT.get(),
                SoundSource.PLAYERS,
                1.4F,
                0.1F
        );
        ModNetwork.sendToPlayer(player, new SoulChoiceFxPacket(choice));
    }

    static boolean advancementChoiceDone(ServerPlayer player) {
        Advancement advancement = player.getServer().getAdvancements().getAdvancement(THE_CHOICE);
        return advancement != null
                && player.getAdvancements().getOrStartProgress(advancement).isDone();
    }

    private static CompoundTag state(Player player) {
        CompoundTag root = player.getPersistentData();
        if (!root.contains(Player.PERSISTED_NBT_TAG, Tag.TAG_COMPOUND)) {
            root.put(Player.PERSISTED_NBT_TAG, new CompoundTag());
        }
        CompoundTag persisted = root.getCompound(Player.PERSISTED_NBT_TAG);
        if (!persisted.contains(TAG, Tag.TAG_COMPOUND)) {
            persisted.put(TAG, new CompoundTag());
        }
        return persisted.getCompound(TAG);
    }
}
