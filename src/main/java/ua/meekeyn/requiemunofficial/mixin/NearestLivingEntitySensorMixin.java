package ua.meekeyn.requiemunofficial.mixin;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.sensing.NearestLivingEntitySensor;
import net.minecraft.world.phys.AABB;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import ua.meekeyn.requiemunofficial.common.soul.VagrantRules;

import java.util.List;
import java.util.function.Predicate;

@Mixin(NearestLivingEntitySensor.class)
public abstract class NearestLivingEntitySensorMixin {

    @Redirect(
            method = "doTick",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/server/level/ServerLevel;getEntitiesOfClass(Ljava/lang/Class;Lnet/minecraft/world/phys/AABB;Ljava/util/function/Predicate;)Ljava/util/List;"
            )
    )
    private <E extends LivingEntity> List<E> requiemunofficial$filterFreeVagrantsFromBrainSensors(ServerLevel level,
                                                                                                   Class<E> entityClass,
                                                                                                   AABB bounds,
                                                                                                   Predicate<? super E> predicate,
                                                                                                   ServerLevel originalLevel,
                                                                                                   LivingEntity subject) {
        return level.getEntitiesOfClass(
                entityClass,
                bounds,
                entity -> predicate.test(entity) && !VagrantRules.shouldBeIgnoredByOrdinarySensors(entity)
        );
    }
}
