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

    @Override
    public void addAttributeModifiers(LivingEntity entity, AttributeMap attributes, int amplifier) {
        super.addAttributeModifiers(entity, attributes, amplifier);
        applyPenance(entity, amplifier);
    }

    @Override
    public boolean shouldStick(LivingEntity entity) {
        return entity instanceof ServerPlayer player && SoulManager.getState(player) == SoulState.REMNANT;
    }

    @Override
    public boolean shouldFreezeDuration(LivingEntity entity) {
        return false;
    }

    @Override
    public boolean isDurationEffectTick(int duration, int amplifier) {
        return amplifier >= PLAYER_BAN_THRESHOLD && duration % 20 == 0;
    }

    @Override
    public void applyEffectTick(LivingEntity entity, int amplifier) {
        applyPenance(entity, amplifier);
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

        if (SoulManager.getState(player) == SoulState.REMNANT) {
            return SoulLifecycleManager.splitPlayer(player, false);
        }
        return false;
    }
}
