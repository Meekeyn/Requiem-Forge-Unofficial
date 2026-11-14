package ua.meekeyn.requiemunofficial.common.effect;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.attributes.AttributeMap;
import org.jetbrains.annotations.Nullable;
import ua.meekeyn.requiemunofficial.common.possession.PossessionManager;
import ua.meekeyn.requiemunofficial.common.soul.SoulLifecycleManager;
import ua.meekeyn.requiemunofficial.common.soul.SoulManager;
import ua.meekeyn.requiemunofficial.common.soul.SoulState;

import java.util.UUID;

public class PenanceMobEffect extends MobEffect implements StickyMobEffect {

    public static final int PREVENT_CURE_THRESHOLD = 0;
    public static final int PLAYER_BAN_THRESHOLD = 1;
    public static final int MOB_BAN_THRESHOLD = 2;

    public PenanceMobEffect() {
        super(MobEffectCategory.HARMFUL, 0x8A6E3B);
    }

    public static boolean blocksPossession(@Nullable MobEffectInstance penance) {
        return penance != null && penance.getAmplifier() >= MOB_BAN_THRESHOLD;
    }

    public static boolean blocksCure(@Nullable MobEffectInstance penance) {
        return penance != null && penance.getAmplifier() >= PREVENT_CURE_THRESHOLD;
    }

    public static boolean blocksMerge(@Nullable MobEffectInstance penance) {
        return penance != null && penance.getAmplifier() >= PLAYER_BAN_THRESHOLD;
    }

    /** beta.16 PenanceComponent.PENANCE_WARNING_TIME: 30 с попередження перед покаранням. */
    public static final int PENANCE_WARNING_TIME = 30 * 20;
    private static final String PENANCE_TIME_TAG = "requiemunofficial:penance_time";

    @Override
    public void onEffectAdded(LivingEntity entity, int amplifier) {
        super.onEffectAdded(entity, amplifier);
        // beta.16 onApplied: якщо попередження вже минуло (рівень підвищили), карає одразу.
        if (entity instanceof ServerPlayer player && getPenanceTime(player) >= PENANCE_WARNING_TIME) {
            applyPenance(player, amplifier);
        }
    }

    @Override
    public boolean shouldStick(LivingEntity entity) {
        // beta.16: липне до будь-якого демона, тобто і до залишку, і до блукача.
        return entity instanceof ServerPlayer player
                && (SoulManager.getState(player) == SoulState.REMNANT
                || SoulManager.getState(player) == SoulState.VAGRANT);
    }

    @Override
    public boolean shouldFreezeDuration(LivingEntity entity) {
        return false;
    }

    /**
     * beta.16 PenanceComponent#shouldSplitFromCurrentBody: II+ виганяє з людського тіла,
     * III+ — ще й з тіла моба.
     */
    public static boolean shouldSplitFromCurrentBody(net.minecraft.world.entity.player.Player player, int amplifier) {
        SoulState state = SoulManager.getState(player);
        if (amplifier >= MOB_BAN_THRESHOLD
                && (state != SoulState.VAGRANT || SoulManager.getPossessedEntityId(player) != null)) {
            return true;
        }
        return amplifier >= PLAYER_BAN_THRESHOLD && state != SoulState.VAGRANT;
    }

    /** beta.16 PenanceComponent#serverTick: лічильник попередження, після нього — покарання щотіку. */
    public static void tickPlayer(ServerPlayer player) {
        MobEffectInstance penance = player.getEffect(ua.meekeyn.requiemunofficial.common.init.ModEffects.PENANCE);
        net.minecraft.nbt.CompoundTag data = player.getPersistentData();
        if (penance != null && shouldSplitFromCurrentBody(player, penance.getAmplifier())) {
            int time = getPenanceTime(player) + 1;
            data.putInt(PENANCE_TIME_TAG, time);
            if (time >= PENANCE_WARNING_TIME) {
                applyPenance(player, penance.getAmplifier());
            }
        } else if (data.contains(PENANCE_TIME_TAG)) {
            data.remove(PENANCE_TIME_TAG);
        }
    }

    public static int getPenanceTime(ServerPlayer player) {
        net.minecraft.nbt.CompoundTag data = player.getPersistentData();
        return data.contains(PENANCE_TIME_TAG) ? data.getInt(PENANCE_TIME_TAG) : -1;
    }

    public static boolean applyPenance(LivingEntity entity, int amplifier) {
        if (amplifier < PLAYER_BAN_THRESHOLD) {
            return false;
        }
        if (!(entity instanceof ServerPlayer player)) {
            return false;
        }
        UUID possessedId = amplifier >= MOB_BAN_THRESHOLD ? SoulManager.getPossessedEntityId(player) : null;
        if (possessedId != null) {
            PossessionManager.release(player, PossessionManager.findPossessedHost(player, possessedId));
            return true;
        }
        SoulState state = SoulManager.getState(player);
        if (state == SoulState.REMNANT) {
            return SoulLifecycleManager.splitPlayer(player);
        }
        if (state == SoulState.MORTAL) {
            // beta.16: смертного не розділиш — б'є магією й перезапускає попередження.
            player.hurt(player.damageSources().magic(), (amplifier + 1) * 4.0F);
            player.getPersistentData().putInt(PENANCE_TIME_TAG, 0);
        }
        return false;
    }
}
