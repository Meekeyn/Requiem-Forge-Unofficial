package ua.meekeyn.requiemunofficial.common.possession;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;

/** Заміна Entity#maxUpStep: у 1.21 висота кроку — атрибут STEP_HEIGHT. */
public final class StepHeight {

    private StepHeight() {
    }

    public static float get(Entity entity) {
        return entity.maxUpStep();
    }

    public static void set(Entity entity, float value) {
        if (entity instanceof LivingEntity living) {
            AttributeInstance step = living.getAttribute(Attributes.STEP_HEIGHT);
            if (step != null) {
                step.setBaseValue(value);
            }
        }
    }
}
