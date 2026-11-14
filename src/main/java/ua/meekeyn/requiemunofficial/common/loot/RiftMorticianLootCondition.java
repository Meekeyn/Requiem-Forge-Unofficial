package ua.meekeyn.requiemunofficial.common.loot;

import com.google.common.collect.ImmutableSet;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.parameters.LootContextParam;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.minecraft.world.level.storage.loot.predicates.LootItemConditionType;
import ua.meekeyn.requiemunofficial.common.entity.MorticianEntity;

import java.util.Set;

/**
 * Порт {@code requiem:rift_mortician}: істина, якщо сутність — Мортиціан-проєкція обеліска
 * (у beta.16 {@code MorticianEntity#isObeliskProjection}). JSON: {@code {"entity": "this"}}.
 */
public class RiftMorticianLootCondition implements LootItemCondition {
    private final LootContext.EntityTarget entity;

    public RiftMorticianLootCondition(LootContext.EntityTarget entity) {
        this.entity = entity;
    }

    @Override
    public LootItemConditionType getType() {
        return RequiemLootConditions.RIFT_MORTICIAN.get();
    }

    @Override
    public Set<LootContextParam<?>> getReferencedContextParams() {
        return ImmutableSet.of(this.entity.getParam());
    }

    @Override
    public boolean test(LootContext lootContext) {
        return lootContext.getParamOrNull(this.entity.getParam()) instanceof MorticianEntity mortician
                && mortician.isObeliskProjection();
    }

    public static final MapCodec<RiftMorticianLootCondition> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            LootContext.EntityTarget.CODEC.fieldOf("entity").forGetter(c -> c.entity)
    ).apply(i, RiftMorticianLootCondition::new));
}
