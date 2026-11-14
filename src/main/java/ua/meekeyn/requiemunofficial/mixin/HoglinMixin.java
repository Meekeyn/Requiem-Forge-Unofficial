package ua.meekeyn.requiemunofficial.mixin;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.monster.hoglin.Hoglin;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import ua.meekeyn.requiemunofficial.common.possession.PossessedEnvironment;
import ua.meekeyn.requiemunofficial.common.transformation.PossessedConversionHost;

@Mixin(Hoglin.class)
public abstract class HoglinMixin implements PossessedConversionHost {

    @Shadow
    private int timeInOverworld;

    @Shadow
    public abstract boolean isConverting();

    @Shadow
    private void finishConversion(ServerLevel level) {
    }

    @Redirect(method = "isConverting", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/entity/monster/hoglin/Hoglin;isNoAi()Z"))
    private boolean requiemunofficial$zombifyWhilePossessed(Hoglin self) {
        return self.isNoAi() && !PossessedEnvironment.isPossessedHost(self);
    }

    /** Копія ванільного таймера з Hoglin#customServerAiStep. */
    @Override
    public void requiemunofficial$tickPossessedConversion() {
        Hoglin self = (Hoglin) (Object) this;
        if (!this.isConverting()) {
            this.timeInOverworld = 0;
            return;
        }
        ++this.timeInOverworld;
        if (this.timeInOverworld > 300
                && self.level() instanceof ServerLevel level
                && net.neoforged.neoforge.event.EventHooks.canLivingConvert(self, EntityType.ZOGLIN,
                        timer -> this.timeInOverworld = timer)) {
            self.playSound(SoundEvents.HOGLIN_CONVERTED_TO_ZOMBIFIED, 1.0F, 1.0F);
            this.finishConversion(level);
        }
    }
}
