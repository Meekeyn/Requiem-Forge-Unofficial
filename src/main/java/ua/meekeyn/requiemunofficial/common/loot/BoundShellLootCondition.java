package ua.meekeyn.requiemunofficial.common.loot;

import com.google.common.collect.ImmutableSet;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.parameters.LootContextParam;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.minecraft.world.level.storage.loot.predicates.LootItemConditionType;
import ua.meekeyn.requiemunofficial.common.entity.PlayerShellEntity;
import ua.meekeyn.requiemunofficial.common.soul.SoulLifecycleManager;
import ua.meekeyn.requiemunofficial.common.soul.SoulManager;

import java.util.Set;

/**
 * Порт {@code requiem:bound_shell}: ціль має бути гравцем, у якого є прив'язана оболонка
 * (beta.16 PlayerBodyTracker anchor -> ENTITY_REF; у порту — SoulManager#getPlayerShellId).
 * JSON: {@code {"entity": "this", "predicate": {"distance": ..., "location": ..., "entity": ...}}}.
 */
public class BoundShellLootCondition implements LootItemCondition {
    private final LootContext.EntityTarget entity;
    private final EntityRefPredicate predicate;

    public BoundShellLootCondition(LootContext.EntityTarget entity, EntityRefPredicate predicate) {
        this.entity = entity;
        this.predicate = predicate;
    }

    @Override
    public LootItemConditionType getType() {
        return RequiemLootConditions.BOUND_SHELL.get();
    }

    @Override
    public Set<LootContextParam<?>> getReferencedContextParams() {
        return ImmutableSet.of(LootContextParams.ORIGIN, this.entity.getParam());
    }

    @Override
    public boolean test(LootContext lootContext) {
        if (!(lootContext.getParamOrNull(this.entity.getParam()) instanceof ServerPlayer player)) {
            return false;
        }

        // Аналог EntityPointer: є id оболонки — є посилання; сама сутність може бути не завантажена.
        EntityRefPredicate.Ref shellRef = null;
        if (SoulManager.getPlayerShellId(player) != null) {
            PlayerShellEntity shell = SoulLifecycleManager.findShell(player);
            shellRef = shell == null
                    ? new EntityRefPredicate.Ref(null, null, null)
                    : new EntityRefPredicate.Ref((ServerLevel) shell.level(), shell.position(), shell);
        }

        return this.predicate.test(
                lootContext.getLevel(),
                lootContext.getParamOrNull(LootContextParams.ORIGIN),
                shellRef
        );
    }

    public static final MapCodec<BoundShellLootCondition> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            LootContext.EntityTarget.CODEC.fieldOf("entity").forGetter(c -> c.entity),
            EntityRefPredicate.CODEC.optionalFieldOf("predicate", EntityRefPredicate.ANY).forGetter(c -> c.predicate)
    ).apply(i, BoundShellLootCondition::new));
}
