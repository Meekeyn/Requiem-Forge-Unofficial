package ua.meekeyn.requiemunofficial.common.advancement;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.advancements.CriterionTrigger;
import net.minecraft.advancements.critereon.ContextAwarePredicate;
import net.minecraft.advancements.critereon.EntityPredicate;
import net.minecraft.advancements.critereon.SimpleCriterionTrigger;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.storage.loot.LootContext;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;
import ua.meekeyn.requiemunofficial.RequiemUnofficial;
import ua.meekeyn.requiemunofficial.common.soul.SoulState;

import java.util.Optional;

public final class ModCriteria {

    private static final DeferredRegister<CriterionTrigger<?>> TRIGGERS =
            DeferredRegister.create(Registries.TRIGGER_TYPE, RequiemUnofficial.MODID);

    public static final ChoiceTrigger MADE_REMNANT_CHOICE = new ChoiceTrigger();
    public static final EntityTrigger PLAYER_POSSESSED_ENTITY = new EntityTrigger("entity");
    public static final DeathTrigger ENTITY_DIED_AFTER_POSSESSION = new DeathTrigger();
    public static final HitTrigger POSSESSED_HIT_ENTITY = new HitTrigger();
    public static final TransformationTrigger TRANSFORMED_POSSESSED_ENTITY = new TransformationTrigger();
    public static final EntityTrigger PLAYER_RESURRECTED_AS_ENTITY = new EntityTrigger("body");

    static {
        TRIGGERS.register("made_remnant_choice", () -> MADE_REMNANT_CHOICE);
        TRIGGERS.register("player_possessed_entity", () -> PLAYER_POSSESSED_ENTITY);
        TRIGGERS.register("entity_died_after_possession", () -> ENTITY_DIED_AFTER_POSSESSION);
        TRIGGERS.register("possessed_hit_entity", () -> POSSESSED_HIT_ENTITY);
        TRIGGERS.register("transformed_possessed_entity", () -> TRANSFORMED_POSSESSED_ENTITY);
        TRIGGERS.register("player_resurrected_as_entity", () -> PLAYER_RESURRECTED_AS_ENTITY);
    }

    private ModCriteria() {
    }

    public static void register(IEventBus modEventBus) {
        TRIGGERS.register(modEventBus);
    }

    private static boolean canTrigger(ServerPlayer player) {
        return player.connection != null && player.connection.getConnection() != null;
    }

    private static boolean matches(Optional<ContextAwarePredicate> predicate, LootContext context) {
        return predicate.isEmpty() || predicate.get().matches(context);
    }

    // ---- made_remnant_choice ----

    public static final class ChoiceTrigger extends SimpleCriterionTrigger<ChoiceTrigger.Instance> {
        public record Instance(Optional<ContextAwarePredicate> player, String remnantType)
                implements SimpleCriterionTrigger.SimpleInstance {
            public static final Codec<Instance> CODEC = RecordCodecBuilder.create(i -> i.group(
                    EntityPredicate.ADVANCEMENT_CODEC.optionalFieldOf("player").forGetter(Instance::player),
                    Codec.STRING.optionalFieldOf("remnant_type", "").forGetter(Instance::remnantType)
            ).apply(i, Instance::new));

            private boolean matches(String type) {
                return remnantType.isEmpty() || remnantType.equals(type);
            }
        }

        @Override
        public Codec<Instance> codec() {
            return Instance.CODEC;
        }

        public void trigger(ServerPlayer player, SoulState choice) {
            if (!canTrigger(player)) {
                return;
            }
            String type = choice == SoulState.REMNANT
                    ? "requiemunofficial:remnant"
                    : "requiemunofficial:mortal";
            trigger(player, instance -> instance.matches(type));
        }
    }

    // ---- player_possessed_entity / player_resurrected_as_entity ----

    public static class EntityTrigger extends SimpleCriterionTrigger<EntityTrigger.Instance> {
        public record Instance(Optional<ContextAwarePredicate> player, Optional<ContextAwarePredicate> entity)
                implements SimpleCriterionTrigger.SimpleInstance {
        }

        private final Codec<Instance> codec;

        private EntityTrigger(String field) {
            this.codec = RecordCodecBuilder.create(i -> i.group(
                    EntityPredicate.ADVANCEMENT_CODEC.optionalFieldOf("player").forGetter(Instance::player),
                    EntityPredicate.ADVANCEMENT_CODEC.optionalFieldOf(field).forGetter(Instance::entity)
            ).apply(i, Instance::new));
        }

        @Override
        public Codec<Instance> codec() {
            return codec;
        }

        public void trigger(ServerPlayer player, Entity entity) {
            if (!canTrigger(player)) {
                return;
            }
            LootContext entityContext = EntityPredicate.createContext(player, entity);
            trigger(player, instance -> ModCriteria.matches(instance.entity(), entityContext));
        }
    }

    // ---- entity_died_after_possession ----

    public static final class DeathTrigger extends SimpleCriterionTrigger<DeathTrigger.Instance> {
        public record Instance(Optional<ContextAwarePredicate> player, Optional<ContextAwarePredicate> entity,
                               Optional<Boolean> seppukku) implements SimpleCriterionTrigger.SimpleInstance {
            public static final Codec<Instance> CODEC = RecordCodecBuilder.create(i -> i.group(
                    EntityPredicate.ADVANCEMENT_CODEC.optionalFieldOf("player").forGetter(Instance::player),
                    EntityPredicate.ADVANCEMENT_CODEC.optionalFieldOf("entity").forGetter(Instance::entity),
                    Codec.BOOL.optionalFieldOf("seppukku").forGetter(Instance::seppukku)
            ).apply(i, Instance::new));

            private boolean matches(LootContext context, boolean actualSeppukku) {
                return ModCriteria.matches(entity, context)
                        && (seppukku.isEmpty() || seppukku.get() == actualSeppukku);
            }
        }

        @Override
        public Codec<Instance> codec() {
            return Instance.CODEC;
        }

        public void trigger(ServerPlayer player, LivingEntity entity, boolean seppukku) {
            if (!canTrigger(player)) {
                return;
            }
            LootContext entityContext = EntityPredicate.createContext(player, entity);
            trigger(player, instance -> instance.matches(entityContext, seppukku));
        }
    }

    // ---- possessed_hit_entity ----

    public static final class HitTrigger extends SimpleCriterionTrigger<HitTrigger.Instance> {
        public record Instance(Optional<ContextAwarePredicate> player, Optional<ContextAwarePredicate> possessed,
                               Optional<ContextAwarePredicate> target) implements SimpleCriterionTrigger.SimpleInstance {
            public static final Codec<Instance> CODEC = RecordCodecBuilder.create(i -> i.group(
                    EntityPredicate.ADVANCEMENT_CODEC.optionalFieldOf("player").forGetter(Instance::player),
                    EntityPredicate.ADVANCEMENT_CODEC.optionalFieldOf("possessed").forGetter(Instance::possessed),
                    EntityPredicate.ADVANCEMENT_CODEC.optionalFieldOf("entity").forGetter(Instance::target)
            ).apply(i, Instance::new));

            private boolean matches(LootContext possessedContext, LootContext targetContext) {
                return ModCriteria.matches(possessed, possessedContext) && ModCriteria.matches(target, targetContext);
            }
        }

        @Override
        public Codec<Instance> codec() {
            return Instance.CODEC;
        }

        public void trigger(ServerPlayer player, LivingEntity possessed, LivingEntity target) {
            if (!canTrigger(player)) {
                return;
            }
            LootContext possessedContext = EntityPredicate.createContext(player, possessed);
            LootContext targetContext = EntityPredicate.createContext(player, target);
            trigger(player, instance -> instance.matches(possessedContext, targetContext));
        }
    }

    // ---- transformed_possessed_entity ----

    public static final class TransformationTrigger extends SimpleCriterionTrigger<TransformationTrigger.Instance> {
        public record Instance(Optional<ContextAwarePredicate> player, Optional<ContextAwarePredicate> before,
                               Optional<ContextAwarePredicate> after, Optional<Boolean> cure)
                implements SimpleCriterionTrigger.SimpleInstance {
            public static final Codec<Instance> CODEC = RecordCodecBuilder.create(i -> i.group(
                    EntityPredicate.ADVANCEMENT_CODEC.optionalFieldOf("player").forGetter(Instance::player),
                    EntityPredicate.ADVANCEMENT_CODEC.optionalFieldOf("before").forGetter(Instance::before),
                    EntityPredicate.ADVANCEMENT_CODEC.optionalFieldOf("after").forGetter(Instance::after),
                    Codec.BOOL.optionalFieldOf("cure").forGetter(Instance::cure)
            ).apply(i, Instance::new));

            private boolean matches(LootContext beforeContext, LootContext afterContext, boolean actualCure) {
                return ModCriteria.matches(before, beforeContext)
                        && ModCriteria.matches(after, afterContext)
                        && (cure.isEmpty() || cure.get() == actualCure);
            }
        }

        @Override
        public Codec<Instance> codec() {
            return Instance.CODEC;
        }

        public void trigger(ServerPlayer player, LivingEntity before, LivingEntity after, boolean cure) {
            if (!canTrigger(player)) {
                return;
            }
            LootContext beforeContext = EntityPredicate.createContext(player, before);
            LootContext afterContext = EntityPredicate.createContext(player, after);
            trigger(player, instance -> instance.matches(beforeContext, afterContext, cure));
        }
    }
}
