package ua.meekeyn.requiemunofficial.common.init;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import ua.meekeyn.requiemunofficial.RequiemUnofficial;
import ua.meekeyn.requiemunofficial.common.entity.MorticianEntity;
import ua.meekeyn.requiemunofficial.common.entity.ObeliskSoulEntity;
import ua.meekeyn.requiemunofficial.common.entity.PlayerShellEntity;
import ua.meekeyn.requiemunofficial.common.entity.ReleasedSoulEntity;
import ua.meekeyn.requiemunofficial.common.entity.CuredPiglinBruteEntity;
import ua.meekeyn.requiemunofficial.common.entity.CuredPiglinEntity;
import ua.meekeyn.requiemunofficial.common.entity.CuredVillagerEntity;

public final class ModEntities {

    public static final DeferredRegister<EntityType<?>> ENTITY_TYPES =
            DeferredRegister.create(ForgeRegistries.ENTITY_TYPES, RequiemUnofficial.MODID);

    public static final RegistryObject<EntityType<MorticianEntity>> MORTICIAN =
            ENTITY_TYPES.register("mortician", () ->
                    EntityType.Builder.of(MorticianEntity::new, MobCategory.CREATURE)
                            .sized(0.6F, 1.95F)
                            .build("mortician"));

    public static final RegistryObject<EntityType<PlayerShellEntity>> PLAYER_SHELL =
            ENTITY_TYPES.register("player_shell", () ->
                    EntityType.Builder.of(PlayerShellEntity::new, MobCategory.CREATURE)
                            .sized(0.6F, 1.95F)
                            .build("player_shell"));

    public static final RegistryObject<EntityType<ObeliskSoulEntity>> OBELISK_SOUL =
            ENTITY_TYPES.register("obelisk_soul", () ->
                    EntityType.Builder.of(ObeliskSoulEntity::new, MobCategory.MISC)
                            .sized(0.5F, 0.5F)
                            .clientTrackingRange(8)
                            .updateInterval(1)
                            .build("obelisk_soul"));

    public static final RegistryObject<EntityType<ReleasedSoulEntity>> RELEASED_SOUL =
            ENTITY_TYPES.register("released_soul", () ->
                    EntityType.Builder.of(ReleasedSoulEntity::new, MobCategory.MISC)
                            .sized(0.5F, 0.5F)
                            .clientTrackingRange(8)
                            .updateInterval(1)
                            .build("released_soul"));

    public static final RegistryObject<EntityType<CuredVillagerEntity>> CURED_VILLAGER =
            ENTITY_TYPES.register("cured_villager", () ->
                    EntityType.Builder.of(CuredVillagerEntity::new, MobCategory.CREATURE)
                            .sized(0.6F, 1.95F)
                            .clientTrackingRange(10)
                            .build("cured_villager"));

    public static final RegistryObject<EntityType<CuredPiglinEntity>> CURED_PIGLIN =
            ENTITY_TYPES.register("cured_piglin", () ->
                    EntityType.Builder.of(CuredPiglinEntity::new, MobCategory.CREATURE)
                            .sized(0.6F, 1.95F)
                            .clientTrackingRange(10)
                            .build("cured_piglin"));

    public static final RegistryObject<EntityType<CuredPiglinBruteEntity>> CURED_PIGLIN_BRUTE =
            ENTITY_TYPES.register("cured_piglin_brute", () ->
                    EntityType.Builder.of(CuredPiglinBruteEntity::new, MobCategory.CREATURE)
                            .sized(0.6F, 1.95F)
                            .clientTrackingRange(10)
                            .build("cured_piglin_brute"));

    private ModEntities() {
    }

    public static void register(IEventBus eventBus) {
        ENTITY_TYPES.register(eventBus);
    }
}
