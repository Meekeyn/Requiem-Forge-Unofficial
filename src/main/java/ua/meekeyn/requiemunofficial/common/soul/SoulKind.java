package ua.meekeyn.requiemunofficial.common.soul;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobType;
import ua.meekeyn.requiemunofficial.common.util.ModTags;

public enum SoulKind {
    MORTAL("mortal", 2),
    UNDEAD("undead", 2),
    BEAST("beast", 1),
    AGGREGATE("aggregate", 3);

    private final String id;
    private final int energyPerSoul;

    SoulKind(String id, int energyPerSoul) {
        this.id = id;
        this.energyPerSoul = energyPerSoul;
    }

    public String id() {
        return id;
    }

    public int energyFor(int amount) {
        return Math.max(0, amount) * energyPerSoul;
    }

    public static SoulKind classify(LivingEntity entity) {
        EntityType<?> type = entity.getType();
        if (type.is(ModTags.SOUL_AGGREGATES)) {
            return AGGREGATE;
        }
        if (entity.getMobType() == MobType.UNDEAD) {
            return UNDEAD;
        }
        if (type.is(ModTags.HUMANS)
                || type.is(ModTags.VILLAGERS)
                || type.is(ModTags.ILLAGERS)
                || type.is(ModTags.PIGLINS)) {
            return MORTAL;
        }
        return BEAST;
    }
}
