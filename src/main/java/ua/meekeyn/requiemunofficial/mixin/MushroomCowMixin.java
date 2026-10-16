package ua.meekeyn.requiemunofficial.mixin;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.MushroomCow;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import ua.meekeyn.requiemunofficial.common.advancement.ModCriteria;
import ua.meekeyn.requiemunofficial.common.possession.PossessionManager;

import java.util.UUID;

@Mixin(MushroomCow.class)
public abstract class MushroomCowMixin {

    @ModifyArg(
            method = "shearInternal",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/level/Level;addFreshEntity(Lnet/minecraft/world/entity/Entity;)Z"
            )
    )
    private Entity requiemunofficial$transferPossessionToShearedCow(Entity replacement) {
        MushroomCow self = (MushroomCow) (Object) this;
        if (!(replacement instanceof LivingEntity livingReplacement)
                || !(self.level() instanceof ServerLevel level)) {
            return replacement;
        }

        UUID possessorId = PossessionManager.getPossessorId(self);
        if (possessorId == null) {
            return replacement;
        }

        ServerPlayer possessor = PossessionManager.getActivePossessor(level, possessorId);
        if (possessor != null && PossessionManager.transferPossession(possessor, self, livingReplacement)) {
            ModCriteria.TRANSFORMED_POSSESSED_ENTITY.trigger(possessor, self, livingReplacement, false);
        }
        return replacement;
    }
}
