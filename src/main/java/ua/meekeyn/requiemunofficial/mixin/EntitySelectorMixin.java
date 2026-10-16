package ua.meekeyn.requiemunofficial.mixin;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySelector;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import ua.meekeyn.requiemunofficial.common.soul.VagrantRules;

import java.util.function.Predicate;

@Mixin(EntitySelector.class)
public abstract class EntitySelectorMixin {
    @Shadow
    @Final
    @Mutable
    public static Predicate<Entity> NO_CREATIVE_OR_SPECTATOR;

    @Inject(method = "<clinit>", at = @At("TAIL"))
    private static void requiemunofficial$treatFreeVagrantsAsUntargetable(CallbackInfo ci) {
        Predicate<Entity> vanilla = NO_CREATIVE_OR_SPECTATOR;
        NO_CREATIVE_OR_SPECTATOR = entity -> vanilla.test(entity)
                && !VagrantRules.shouldBeIgnoredByOrdinaryTargeting(entity);
    }
}
