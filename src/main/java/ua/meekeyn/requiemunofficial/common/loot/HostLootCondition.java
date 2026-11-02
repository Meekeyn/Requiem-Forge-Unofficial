package ua.meekeyn.requiemunofficial.common.loot;

import com.google.common.collect.ImmutableSet;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonObject;
import com.google.gson.JsonSerializationContext;
import net.minecraft.advancements.critereon.EntityPredicate;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.parameters.LootContextParam;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.minecraft.world.level.storage.loot.predicates.LootItemConditionType;
import ua.meekeyn.requiemunofficial.common.possession.PossessionManager;

import java.util.Set;
import java.util.UUID;

/**
 * Порт {@code requiem:host} / {@code requiem:possessor}.
 * JSON: {@code {"entity": "this", "predicate": {...}}}.
 * HOST — перевіряє тіло, яким керує гравець-ціль (beta.16 {@code PossessionComponent.getHost});
 * POSSESSOR — перевіряє гравця, що вселився в ціль (beta.16 {@code Possessable#getPossessor}).
 * Як і в оригіналі, {@code "predicate": {}} вимагає, щоб перевірювана сутність існувала,
 * а відсутній предикат ({@link EntityPredicate#ANY}) пропускає все.
 */
public class HostLootCondition implements LootItemCondition {
    private final CheckedEntity checkedEntity;
    private final LootContext.EntityTarget entity;
    private final EntityPredicate predicate;

    public HostLootCondition(CheckedEntity checkedEntity, LootContext.EntityTarget entity, EntityPredicate predicate) {
        this.checkedEntity = checkedEntity;
        this.entity = entity;
        this.predicate = predicate;
    }

    @Override
    public LootItemConditionType getType() {
        return switch (this.checkedEntity) {
            case HOST -> RequiemLootConditions.HOST.get();
            case POSSESSOR -> RequiemLootConditions.POSSESSOR.get();
        };
    }

    @Override
    public Set<LootContextParam<?>> getReferencedContextParams() {
        return ImmutableSet.of(LootContextParams.ORIGIN, this.entity.getParam());
    }

    @Override
    public boolean test(LootContext lootContext) {
        if (!(lootContext.getParamOrNull(this.entity.getParam()) instanceof LivingEntity e)) {
            return false;
        }
        Entity checked = switch (this.checkedEntity) {
            case HOST -> findHost(e);
            case POSSESSOR -> findPossessor(e);
        };
        return this.predicate.matches(lootContext.getLevel(), lootContext.getParamOrNull(LootContextParams.ORIGIN), checked);
    }

    /** Тіло, у яке вселився гравець (null, якщо це не гравець або він ні в кого не вселився). */
    private static Entity findHost(LivingEntity entity) {
        return entity instanceof ServerPlayer player ? PossessionManager.findPossessedHost(player) : null;
    }

    /** Гравець, що зараз керує цим тілом (null, якщо тіло вільне). */
    private static Entity findPossessor(LivingEntity entity) {
        UUID possessorId = PossessionManager.getPossessorId(entity);
        if (possessorId == null || !(entity.level() instanceof ServerLevel level)) {
            return null;
        }
        return PossessionManager.getActivePossessor(level, possessorId);
    }

    public static class Serializer implements net.minecraft.world.level.storage.loot.Serializer<HostLootCondition> {
        private final CheckedEntity checkedEntity;

        public Serializer(CheckedEntity checkedEntity) {
            this.checkedEntity = checkedEntity;
        }

        @Override
        public void serialize(JsonObject json, HostLootCondition condition, JsonSerializationContext ctx) {
            json.add("entity", ctx.serialize(condition.entity));
            json.add("predicate", condition.predicate.serializeToJson());
        }

        @Override
        public HostLootCondition deserialize(JsonObject json, JsonDeserializationContext ctx) {
            return new HostLootCondition(
                    this.checkedEntity,
                    GsonHelper.getAsObject(json, "entity", ctx, LootContext.EntityTarget.class),
                    EntityPredicate.fromJson(json.get("predicate"))
            );
        }
    }

    public enum CheckedEntity {
        HOST, POSSESSOR
    }
}
