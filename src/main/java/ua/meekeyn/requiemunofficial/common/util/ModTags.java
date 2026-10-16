package ua.meekeyn.requiemunofficial.common.util;

import net.minecraft.core.registries.Registries;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.block.Block;

public final class ModTags {

    public static final TagKey<EntityType<?>> POSSESSABLES =
            TagKey.create(Registries.ENTITY_TYPE, ModIds.id("possession/possessables"));
    public static final TagKey<EntityType<?>> POSSESSION_BLACKLIST =
            TagKey.create(Registries.ENTITY_TYPE, ModIds.id("possession/possession_blacklist"));
    public static final TagKey<EntityType<?>> SOULLESS =
            TagKey.create(Registries.ENTITY_TYPE, ModIds.id("possession/soulless"));
    public static final TagKey<EntityType<?>> FRICTIONLESS_HOSTS =
            TagKey.create(Registries.ENTITY_TYPE, ModIds.id("possession/frictionless_hosts"));
    public static final TagKey<EntityType<?>> ITEM_USERS =
            TagKey.create(Registries.ENTITY_TYPE, ModIds.id("inventory/item_users"));
    public static final TagKey<EntityType<?>> INVENTORY_CARRIERS =
            TagKey.create(Registries.ENTITY_TYPE, ModIds.id("inventory/inventory_carriers"));
    public static final TagKey<EntityType<?>> HOTBAR_CARRIERS =
            TagKey.create(Registries.ENTITY_TYPE, ModIds.id("inventory/hotbar_carriers"));
    public static final TagKey<EntityType<?>> SUPERCRAFTERS =
            TagKey.create(Registries.ENTITY_TYPE, ModIds.id("inventory/supercrafters"));
    public static final TagKey<EntityType<?>> ARMOR_USERS =
            TagKey.create(Registries.ENTITY_TYPE, ModIds.id("inventory/armor_users"));
    public static final TagKey<EntityType<?>> ARMOR_BANNED =
            TagKey.create(Registries.ENTITY_TYPE, ModIds.id("inventory/armor_banned"));
    public static final TagKey<EntityType<?>> DICHROMATS =
            TagKey.create(Registries.ENTITY_TYPE, ModIds.id("vision/dichromats"));
    public static final TagKey<EntityType<?>> HEMERALOPES =
            TagKey.create(Registries.ENTITY_TYPE, ModIds.id("vision/hemeralopes"));
    public static final TagKey<EntityType<?>> NIGHT_EYED =
            TagKey.create(Registries.ENTITY_TYPE, ModIds.id("vision/night_eyed"));
    public static final TagKey<EntityType<?>> TETRACHROMATS =
            TagKey.create(Registries.ENTITY_TYPE, ModIds.id("vision/tetrachromats"));
    public static final TagKey<EntityType<?>> IMMOVABLE =
            TagKey.create(Registries.ENTITY_TYPE, ModIds.id("behavior/immovable"));
    public static final TagKey<EntityType<?>> REGULAR_EATERS =
            TagKey.create(Registries.ENTITY_TYPE, ModIds.id("behavior/regular_eaters"));
    public static final TagKey<EntityType<?>> ARROW_GENERATORS =
            TagKey.create(Registries.ENTITY_TYPE, ModIds.id("behavior/arrow_generators"));
    public static final TagKey<EntityType<?>> SKELETONIZABLE =
            TagKey.create(Registries.ENTITY_TYPE, ModIds.id("transformation/skeletonizable"));
    public static final TagKey<EntityType<?>> REPLACEABLE_SKELETONS =
            TagKey.create(Registries.ENTITY_TYPE, ModIds.id("transformation/replaceable_skeletons"));
    public static final TagKey<EntityType<?>> HUMANOID_ZOMBIES =
            TagKey.create(Registries.ENTITY_TYPE, ModIds.id("humanoid_zombies"));
    public static final TagKey<EntityType<?>> HUMANOID_SKELETONS =
            TagKey.create(Registries.ENTITY_TYPE, ModIds.id("humanoid_skeletons"));
    public static final TagKey<EntityType<?>> SKELETONS =
            TagKey.create(Registries.ENTITY_TYPE, ModIds.id("skeletons"));
    public static final TagKey<EntityType<?>> UNDEAD =
            TagKey.create(Registries.ENTITY_TYPE, ModIds.id("undead"));
    public static final TagKey<EntityType<?>> PIGLINS =
            TagKey.create(Registries.ENTITY_TYPE, ModIds.id("piglins"));
    public static final TagKey<EntityType<?>> HUMANS =
            TagKey.create(Registries.ENTITY_TYPE, ModIds.id("humans"));
    public static final TagKey<EntityType<?>> VILLAGERS =
            TagKey.create(Registries.ENTITY_TYPE, ModIds.id("villagers"));
    public static final TagKey<EntityType<?>> ILLAGERS =
            TagKey.create(Registries.ENTITY_TYPE, ModIds.id("illagers"));
    public static final TagKey<EntityType<?>> SOUL_AGGREGATES =
            TagKey.create(Registries.ENTITY_TYPE, ModIds.id("soul_capture/soul_aggregates"));
    public static final TagKey<EntityType<?>> SOUL_CAPTURE_BLACKLIST =
            TagKey.create(Registries.ENTITY_TYPE, ModIds.id("soul_capture/soul_capture_blacklist"));
    public static final TagKey<Block> SOUL_INTERACTABLE =
            TagKey.create(Registries.BLOCK, ModIds.id("soul_interactable"));
    public static final TagKey<Block> SOUL_IMPERMEABLE =
            TagKey.create(Registries.BLOCK, ModIds.id("soul_impermeable"));
    public static final TagKey<Block> WANDERING_SOUL_TRAVERSABLE =
            TagKey.create(Registries.BLOCK, ModIds.id("wandering_soul_traversable"));

    private ModTags() {
    }
}
