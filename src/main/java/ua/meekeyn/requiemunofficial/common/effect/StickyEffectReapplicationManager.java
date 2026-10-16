package ua.meekeyn.requiemunofficial.common.effect;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.server.level.ServerPlayer;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public final class StickyEffectReapplicationManager {
    private static final ThreadLocal<Set<Key>> DEFINITIVE_CLEARS = ThreadLocal.withInitial(HashSet::new);

    private StickyEffectReapplicationManager() {
    }

    public static void definitivelyClear(LivingEntity entity, MobEffect effect, Runnable removal) {
        Key key = new Key(entity.getUUID(), effect);
        Set<Key> clears = DEFINITIVE_CLEARS.get();
        clears.add(key);
        try {
            removal.run();
        } finally {
            clears.remove(key);
        }
    }

    public static void onEffectRemoved(LivingEntity entity, MobEffectInstance effect) {
        if (entity.level().isClientSide()
                || DEFINITIVE_CLEARS.get().contains(new Key(entity.getUUID(), effect.getEffect()))
                || !StickyMobEffect.shouldStick(effect, entity)) {
            return;
        }
        MobEffectInstance copy = new MobEffectInstance(effect);
        if (entity instanceof ServerPlayer player && !hasLiveConnection(player)) {
            player.getActiveEffectsMap().put(copy.getEffect(), copy);
        } else {
            entity.addEffect(copy);
        }
    }

    private static boolean hasLiveConnection(ServerPlayer player) {
        return player.connection != null && player.connection.connection != null;
    }

    private record Key(UUID entityId, MobEffect effect) {
    }
}
