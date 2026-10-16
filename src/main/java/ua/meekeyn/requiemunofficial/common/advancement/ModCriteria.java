package ua.meekeyn.requiemunofficial.common.advancement;

import com.google.gson.JsonObject;
import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.advancements.critereon.AbstractCriterionTriggerInstance;
import net.minecraft.advancements.critereon.ContextAwarePredicate;
import net.minecraft.advancements.critereon.DeserializationContext;
import net.minecraft.advancements.critereon.EntityPredicate;
import net.minecraft.advancements.critereon.SerializationContext;
import net.minecraft.advancements.critereon.SimpleCriterionTrigger;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import ua.meekeyn.requiemunofficial.common.soul.SoulState;
import ua.meekeyn.requiemunofficial.common.util.ModIds;

import javax.annotation.Nullable;

public final class ModCriteria {

    public static final ChoiceTrigger MADE_REMNANT_CHOICE =
            new ChoiceTrigger(ModIds.id("made_remnant_choice"));
    public static final EntityTrigger PLAYER_POSSESSED_ENTITY =
            new EntityTrigger(ModIds.id("player_possessed_entity"), "entity");
    public static final DeathTrigger ENTITY_DIED_AFTER_POSSESSION =
            new DeathTrigger(ModIds.id("entity_died_after_possession"));
    public static final HitTrigger POSSESSED_HIT_ENTITY =
            new HitTrigger(ModIds.id("possessed_hit_entity"));
    public static final TransformationTrigger TRANSFORMED_POSSESSED_ENTITY =
            new TransformationTrigger(ModIds.id("transformed_possessed_entity"));
    public static final EntityTrigger PLAYER_RESURRECTED_AS_ENTITY =
            new EntityTrigger(ModIds.id("player_resurrected_as_entity"), "body");

    private ModCriteria() {
    }

    public static void init() {
        CriteriaTriggers.register(MADE_REMNANT_CHOICE);
        CriteriaTriggers.register(PLAYER_POSSESSED_ENTITY);
        CriteriaTriggers.register(ENTITY_DIED_AFTER_POSSESSION);
        CriteriaTriggers.register(POSSESSED_HIT_ENTITY);
        CriteriaTriggers.register(TRANSFORMED_POSSESSED_ENTITY);
        CriteriaTriggers.register(PLAYER_RESURRECTED_AS_ENTITY);
    }

    private static boolean canTrigger(ServerPlayer player) {
        return player.connection != null && player.connection.connection != null;
    }

    public static final class ChoiceTrigger extends SimpleCriterionTrigger<ChoiceInstance> {
        private final ResourceLocation id;

        private ChoiceTrigger(ResourceLocation id) {
            this.id = id;
        }

        @Override
        public ResourceLocation getId() {
            return id;
        }

        @Override
        protected ChoiceInstance createInstance(JsonObject json, ContextAwarePredicate player,
                                                DeserializationContext context) {
            String remnantType = json.has("remnant_type") ? json.get("remnant_type").getAsString() : "";
            return new ChoiceInstance(id, player, remnantType);
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

    public static final class ChoiceInstance extends AbstractCriterionTriggerInstance {
        private final String remnantType;

        private ChoiceInstance(ResourceLocation id, ContextAwarePredicate player, String remnantType) {
            super(id, player);
            this.remnantType = remnantType;
        }

        private boolean matches(String type) {
            return remnantType.isEmpty() || remnantType.equals(type);
        }

        @Override
        public JsonObject serializeToJson(SerializationContext context) {
            JsonObject json = super.serializeToJson(context);
            if (!remnantType.isEmpty()) {
                json.addProperty("remnant_type", remnantType);
            }
            return json;
        }
    }

    public static class EntityTrigger extends SimpleCriterionTrigger<EntityInstance> {
        private final ResourceLocation id;
        private final String field;

        private EntityTrigger(ResourceLocation id, String field) {
            this.id = id;
            this.field = field;
        }

        @Override
        public ResourceLocation getId() {
            return id;
        }

        @Override
        protected EntityInstance createInstance(JsonObject json, ContextAwarePredicate player,
                                                DeserializationContext context) {
            return new EntityInstance(id, player, field, EntityPredicate.fromJson(json, field, context));
        }

        public void trigger(ServerPlayer player, Entity entity) {
            if (!canTrigger(player)) {
                return;
            }
            var entityContext = EntityPredicate.createContext(player, entity);
            trigger(player, instance -> instance.matches(entityContext));
        }
    }

    public static final class EntityInstance extends AbstractCriterionTriggerInstance {
        private final String field;
        private final ContextAwarePredicate entity;

        private EntityInstance(ResourceLocation id, ContextAwarePredicate player, String field,
                               ContextAwarePredicate entity) {
            super(id, player);
            this.field = field;
            this.entity = entity;
        }

        private boolean matches(net.minecraft.world.level.storage.loot.LootContext context) {
            return entity.matches(context);
        }

        @Override
        public JsonObject serializeToJson(SerializationContext context) {
            JsonObject json = super.serializeToJson(context);
            json.add(field, entity.toJson(context));
            return json;
        }
    }

    public static final class DeathTrigger extends SimpleCriterionTrigger<DeathInstance> {
        private final ResourceLocation id;

        private DeathTrigger(ResourceLocation id) {
            this.id = id;
        }

        @Override
        public ResourceLocation getId() {
            return id;
        }

        @Override
        protected DeathInstance createInstance(JsonObject json, ContextAwarePredicate player,
                                               DeserializationContext context) {
            Boolean seppukku = json.has("seppukku") ? json.get("seppukku").getAsBoolean() : null;
            return new DeathInstance(
                    id,
                    player,
                    EntityPredicate.fromJson(json, "entity", context),
                    seppukku
            );
        }

        public void trigger(ServerPlayer player, LivingEntity entity, boolean seppukku) {
            if (!canTrigger(player)) {
                return;
            }
            var entityContext = EntityPredicate.createContext(player, entity);
            trigger(player, instance -> instance.matches(entityContext, seppukku));
        }
    }

    public static final class DeathInstance extends AbstractCriterionTriggerInstance {
        private final ContextAwarePredicate entity;
        private final @Nullable Boolean seppukku;

        private DeathInstance(ResourceLocation id, ContextAwarePredicate player,
                              ContextAwarePredicate entity, @Nullable Boolean seppukku) {
            super(id, player);
            this.entity = entity;
            this.seppukku = seppukku;
        }

        private boolean matches(net.minecraft.world.level.storage.loot.LootContext context,
                                boolean actualSeppukku) {
            return entity.matches(context) && (seppukku == null || seppukku == actualSeppukku);
        }

        @Override
        public JsonObject serializeToJson(SerializationContext context) {
            JsonObject json = super.serializeToJson(context);
            json.add("entity", entity.toJson(context));
            if (seppukku != null) {
                json.addProperty("seppukku", seppukku);
            }
            return json;
        }
    }

    public static final class HitTrigger extends SimpleCriterionTrigger<HitInstance> {
        private final ResourceLocation id;

        private HitTrigger(ResourceLocation id) {
            this.id = id;
        }

        @Override
        public ResourceLocation getId() {
            return id;
        }

        @Override
        protected HitInstance createInstance(JsonObject json, ContextAwarePredicate player,
                                             DeserializationContext context) {
            return new HitInstance(
                    id,
                    player,
                    EntityPredicate.fromJson(json, "possessed", context),
                    EntityPredicate.fromJson(json, "entity", context)
            );
        }

        public void trigger(ServerPlayer player, LivingEntity possessed, LivingEntity target) {
            if (!canTrigger(player)) {
                return;
            }
            var possessedContext = EntityPredicate.createContext(player, possessed);
            var targetContext = EntityPredicate.createContext(player, target);
            trigger(player, instance -> instance.matches(possessedContext, targetContext));
        }
    }

    public static final class HitInstance extends AbstractCriterionTriggerInstance {
        private final ContextAwarePredicate possessed;
        private final ContextAwarePredicate target;

        private HitInstance(ResourceLocation id, ContextAwarePredicate player,
                            ContextAwarePredicate possessed, ContextAwarePredicate target) {
            super(id, player);
            this.possessed = possessed;
            this.target = target;
        }

        private boolean matches(net.minecraft.world.level.storage.loot.LootContext possessedContext,
                                net.minecraft.world.level.storage.loot.LootContext targetContext) {
            return possessed.matches(possessedContext) && target.matches(targetContext);
        }

        @Override
        public JsonObject serializeToJson(SerializationContext context) {
            JsonObject json = super.serializeToJson(context);
            json.add("possessed", possessed.toJson(context));
            json.add("entity", target.toJson(context));
            return json;
        }
    }

    public static final class TransformationTrigger
            extends SimpleCriterionTrigger<TransformationInstance> {
        private final ResourceLocation id;

        private TransformationTrigger(ResourceLocation id) {
            this.id = id;
        }

        @Override
        public ResourceLocation getId() {
            return id;
        }

        @Override
        protected TransformationInstance createInstance(JsonObject json, ContextAwarePredicate player,
                                                         DeserializationContext context) {
            Boolean cure = json.has("cure") ? json.get("cure").getAsBoolean() : null;
            return new TransformationInstance(
                    id,
                    player,
                    EntityPredicate.fromJson(json, "before", context),
                    EntityPredicate.fromJson(json, "after", context),
                    cure
            );
        }

        public void trigger(ServerPlayer player, LivingEntity before, LivingEntity after, boolean cure) {
            if (!canTrigger(player)) {
                return;
            }
            var beforeContext = EntityPredicate.createContext(player, before);
            var afterContext = EntityPredicate.createContext(player, after);
            trigger(player, instance -> instance.matches(beforeContext, afterContext, cure));
        }
    }

    public static final class TransformationInstance extends AbstractCriterionTriggerInstance {
        private final ContextAwarePredicate before;
        private final ContextAwarePredicate after;
        private final @Nullable Boolean cure;

        private TransformationInstance(ResourceLocation id, ContextAwarePredicate player,
                                       ContextAwarePredicate before, ContextAwarePredicate after,
                                       @Nullable Boolean cure) {
            super(id, player);
            this.before = before;
            this.after = after;
            this.cure = cure;
        }

        private boolean matches(net.minecraft.world.level.storage.loot.LootContext beforeContext,
                                net.minecraft.world.level.storage.loot.LootContext afterContext,
                                boolean actualCure) {
            return before.matches(beforeContext)
                    && after.matches(afterContext)
                    && (cure == null || cure == actualCure);
        }

        @Override
        public JsonObject serializeToJson(SerializationContext context) {
            JsonObject json = super.serializeToJson(context);
            json.add("before", before.toJson(context));
            json.add("after", after.toJson(context));
            if (cure != null) {
                json.addProperty("cure", cure);
            }
            return json;
        }
    }
}
