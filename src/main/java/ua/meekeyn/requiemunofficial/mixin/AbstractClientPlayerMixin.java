package ua.meekeyn.requiemunofficial.mixin;

import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import ua.meekeyn.requiemunofficial.common.possession.PossessionManager;

/**
 * Vanilla computes the FOV modifier from the camera player's movement speed attribute. A
 * possessed Player Body carries the host's speed on that attribute, which would widen the FOV
 * far beyond the player's own setting, so the speed read feeding the vanilla formula uses the
 * body-free speed instead. Sprint, Speed/Slowness effects, the FOV effect strength option and
 * the bow/spyglass zoom keep their vanilla behavior.
 */
@Mixin(AbstractClientPlayer.class)
public abstract class AbstractClientPlayerMixin {

    @Redirect(method = "getFieldOfViewModifier", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/player/AbstractClientPlayer;getAttributeValue(Lnet/minecraft/world/entity/ai/attributes/Attribute;)D"))
    private double requiemunofficial$possessionBodyFovSpeed(Player entity, Attribute attribute) {
        double bodySpeed = ((LivingEntity) entity).getAttributeValue(attribute);
        return PossessionManager.beta16BodyFovMovementSpeed(entity, bodySpeed);
    }
}
