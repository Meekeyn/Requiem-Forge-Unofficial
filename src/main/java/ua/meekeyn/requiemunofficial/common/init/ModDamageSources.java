package ua.meekeyn.requiemunofficial.common.init;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.LivingEntity;
import ua.meekeyn.requiemunofficial.common.util.ModIds;

public final class ModDamageSources {
    public static final ResourceKey<DamageType> ATTRITION_HARDCORE_DEATH =
            ResourceKey.create(Registries.DAMAGE_TYPE, ModIds.id("attrition_hardcore_death"));

    private ModDamageSources() {
    }

    public static DamageSource attritionHardcoreDeath(LivingEntity entity) {
        return new DamageSource(entity.level().registryAccess()
                .registryOrThrow(Registries.DAMAGE_TYPE)
                .getHolderOrThrow(ATTRITION_HARDCORE_DEATH));
    }
}
