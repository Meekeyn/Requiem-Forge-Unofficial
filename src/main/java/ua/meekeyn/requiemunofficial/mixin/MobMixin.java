package ua.meekeyn.requiemunofficial.mixin;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.Level;
import net.minecraft.world.entity.ai.goal.Goal;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import ua.meekeyn.requiemunofficial.common.possession.PossessedEnvironment;
import ua.meekeyn.requiemunofficial.common.possession.PossessionInventoryManager;
import ua.meekeyn.requiemunofficial.common.possession.PossessionManager;
import ua.meekeyn.requiemunofficial.common.transformation.TransformationManager;

@Mixin(Mob.class)
public abstract class MobMixin {

    @Inject(method = "serverAiStep", at = @At("HEAD"), cancellable = true)
    private void requiemunofficial$skipAutonomousAiWhilePossessed(CallbackInfo ci) {
        Mob self = (Mob) (Object) this;
        if (PossessionManager.getPossessorId(self) != null
                || PossessionManager.shouldSuppressRiddenVehicleAi(self)) {
            ci.cancel();
        }
    }

    // A ridden spider carries a possessed skeleton whose Mob-ness would otherwise clear the spider's
    // MOVE/JUMP/LOOK control flags (Mob#getControllingPassenger returns the Mob rider). Keep them
    // enabled so the spider's own chase/flee AI can still drive it while the rider is aboard.
    @Inject(method = "updateControlFlags", at = @At("HEAD"), cancellable = true)
    private void requiemunofficial$preserveRiddenSpiderAiFlags(CallbackInfo ci) {
        Mob self = (Mob) (Object) this;
        if (PossessionManager.shouldPreserveVehicleAi(self)) {
            self.goalSelector.setControlFlag(Goal.Flag.MOVE, true);
            self.goalSelector.setControlFlag(Goal.Flag.JUMP, true);
            self.goalSelector.setControlFlag(Goal.Flag.LOOK, true);
            ci.cancel();
        }
    }

    // Предмети-дзеркала гравця (dropChance 2.0) не повинні копіюватися в нове тіло як справжні.
    @Inject(method = "convertTo", at = @At("HEAD"))
    private void requiemunofficial$clearMirrorsBeforeConversion(EntityType<?> type, boolean keepEquipment,
                                                               CallbackInfoReturnable<Mob> cir) {
        Mob self = (Mob) (Object) this;
        if (!self.isRemoved() && PossessedEnvironment.isPossessedHost(self)) {
            PossessionInventoryManager.clearVisualMirrors(self);
        }
    }

    // Ванільне перетворення (утоплення, замерзання, зомбіфікація): душа переходить у нове тіло
    // до source.discard(), решта convertTo (вершник, атрибути, Forge-подія) лишається ванільною.
    @Redirect(method = "convertTo", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/level/Level;addFreshEntity(Lnet/minecraft/world/entity/Entity;)Z"))
    private boolean requiemunofficial$transferPossessionOnConversion(Level level, Entity replacement) {
        boolean added = level.addFreshEntity(replacement);
        Mob self = (Mob) (Object) this;
        if (added && replacement instanceof LivingEntity outcome && PossessedEnvironment.isPossessedHost(self)) {
            TransformationManager.transferPossessionOnConversion(self, outcome);
        }
        return added;
    }
}
