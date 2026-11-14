package ua.meekeyn.requiemunofficial.mixin;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.monster.piglin.AbstractPiglin;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import ua.meekeyn.requiemunofficial.common.possession.PossessedEnvironment;
import ua.meekeyn.requiemunofficial.common.transformation.PossessedConversionHost;

@Mixin(AbstractPiglin.class)
public abstract class AbstractPiglinMixin implements PossessedConversionHost {

    @Shadow
    protected int timeInOverworld;

    @Shadow
    public abstract boolean isConverting();

    @Shadow
    protected abstract void playConvertedSound();

    @Shadow
    protected abstract void finishConversion(ServerLevel level);

    @Redirect(method = "isConverting", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/entity/monster/piglin/AbstractPiglin;isNoAi()Z"))
    private boolean requiemunofficial$zombifyWhilePossessed(AbstractPiglin self) {
        return self.isNoAi() && !PossessedEnvironment.isPossessedHost(self);
    }

    /** Копія ванільного таймера з AbstractPiglin#customServerAiStep. */
    @Override
    public void requiemunofficial$tickPossessedConversion() {
        AbstractPiglin self = (AbstractPiglin) (Object) this;
        if (this.isConverting()) {
            ++this.timeInOverworld;
        } else {
            this.timeInOverworld = 0;
        }
        if (this.timeInOverworld > 300
                && self.level() instanceof ServerLevel level
                && net.neoforged.neoforge.event.EventHooks.canLivingConvert(self, EntityType.ZOMBIFIED_PIGLIN,
                        timer -> this.timeInOverworld = timer)) {
            this.playConvertedSound();
            this.finishConversion(level);
        }
    }
}
