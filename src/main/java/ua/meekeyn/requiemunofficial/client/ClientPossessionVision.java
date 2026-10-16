package ua.meekeyn.requiemunofficial.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.PostChain;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.animal.Bee;
import net.minecraft.world.entity.animal.Dolphin;
import net.minecraft.world.entity.animal.MushroomCow;
import net.minecraft.world.entity.animal.WaterAnimal;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import ua.meekeyn.requiemunofficial.RequiemUnofficial;
import ua.meekeyn.requiemunofficial.common.soul.ModCapabilities;
import ua.meekeyn.requiemunofficial.common.util.ModIds;
import ua.meekeyn.requiemunofficial.common.util.ModTags;

import java.util.UUID;

@Mod.EventBusSubscriber(modid = RequiemUnofficial.MODID, value = Dist.CLIENT)
public final class ClientPossessionVision {

    private static final ResourceLocation DICHROMATIC_SHADER = shader("dichromatic");
    private static final ResourceLocation TETRACHROMATIC_SHADER = shader("tetrachromatic");
    private static final ResourceLocation BEE_SHADER = shader("bee");
    private static final ResourceLocation DOLPHIN_SHADER = shader("dolphin");
    private static final ResourceLocation FISH_EYE_SHADER = shader("fish_eye");
    private static final ResourceLocation MOOSHROOM_SHADER = shader("mooshroom");
    private static final ResourceLocation CREEPER_SHADER =
            new ResourceLocation("minecraft", "shaders/post/creeper.json");
    private static final ResourceLocation INVERT_SHADER =
            new ResourceLocation("minecraft", "shaders/post/invert.json");
    private static final ResourceLocation SPIDER_SHADER =
            new ResourceLocation("minecraft", "shaders/post/spider.json");
    private static final ResourceLocation SPECTRE_SHADER = shader("spectre");
    private static final int MISSING_HOST_GRACE_TICKS = 10;
    /** Длительность синей "вспышки" при входе в новое тело (2 секунды, как в beta.16). */
    private static final int POSSESSION_ENTRY_FLASH_TICKS = 40;

    private static ResourceLocation appliedShader;
    /** Цепь пост-эффекта, загруженная нами; ванидька может выгрузить её самостоятельно. */
    private static PostChain appliedEffect;
    private static UUID lastPossessedHostId;
    private static ResourceLocation lastPossessedShader;
    private static int missingHostTicks;
    private static UUID entryFlashHostId;
    private static int entryFlashTicks;

    private ClientPossessionVision() {
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }

        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.level == null) {
            resetPossessionState();
            clearShader(minecraft);
            return;
        }

        UUID possessedHostId = minecraft.player.getCapability(ModCapabilities.PLAYER_SOUL)
                .resolve()
                .map(soul -> soul.getPossessedEntityId())
                .orElse(null);
        ResourceLocation shader = null;
        if (possessedHostId != null) {
            boolean newHost = !possessedHostId.equals(entryFlashHostId);
            if (newHost) {
                // Новое тело (захват, переход, переподключение) — запускаем вспышку.
                entryFlashHostId = possessedHostId;
                entryFlashTicks = POSSESSION_ENTRY_FLASH_TICKS;
            }

            ResourceLocation entityVision = lastPossessedShader;
            LivingEntity host = findClientHost(minecraft, possessedHostId);
            if (host != null) {
                entityVision = selectShader(host);
                lastPossessedHostId = possessedHostId;
                lastPossessedShader = entityVision;
                missingHostTicks = 0;
            } else if (!possessedHostId.equals(lastPossessedHostId)
                    || missingHostTicks >= MISSING_HOST_GRACE_TICKS) {
                entityVision = null;
            } else {
                missingHostTicks++;
            }

            boolean flashing = entryFlashTicks > 0;
            if (newHost) {
                RequiemUnofficial.LOGGER.info("Spectre possession entry flash started ({} ticks) for {}",
                        POSSESSION_ENTRY_FLASH_TICKS,
                        host != null ? host.getName().getString() : possessedHostId.toString());
            }
            shader = flashing ? SPECTRE_SHADER : entityVision;
            if (flashing && --entryFlashTicks == 0) {
                RequiemUnofficial.LOGGER.info("Spectre possession entry flash finished");
            }
        } else {
            resetPossessionState();
        }

        applyShader(minecraft, shader);
    }

    private static void resetPossessionState() {
        lastPossessedHostId = null;
        lastPossessedShader = null;
        missingHostTicks = 0;
        entryFlashHostId = null;
        entryFlashTicks = 0;
    }

    public static ResourceLocation selectShaderForTest(EntityType<?> type) {
        return selectShader(type, null);
    }

    private static ResourceLocation selectShader(LivingEntity entity) {
        return selectShader(entity.getType(), entity);
    }

    private static LivingEntity findClientHost(Minecraft minecraft, UUID hostId) {
        Entity camera = minecraft.getCameraEntity();
        if (camera instanceof LivingEntity living && hostId.equals(living.getUUID())) {
            return living;
        }
        if (minecraft.level == null) {
            return null;
        }
        for (Entity entity : minecraft.level.entitiesForRendering()) {
            if (entity instanceof LivingEntity living && hostId.equals(entity.getUUID())) {
                return living;
            }
        }
        return null;
    }

    private static ResourceLocation selectShader(EntityType<?> type, LivingEntity entity) {
        if (type == EntityType.CREEPER) {
            return CREEPER_SHADER;
        }
        if (type == EntityType.ENDERMAN) {
            return INVERT_SHADER;
        }
        if (type == EntityType.SPIDER || type == EntityType.CAVE_SPIDER) {
            return SPIDER_SHADER;
        }
        if (type.is(ModTags.DICHROMATS)) {
            return DICHROMATIC_SHADER;
        }
        if (type.is(ModTags.TETRACHROMATS)) {
            return TETRACHROMATIC_SHADER;
        }
        if (entity instanceof Bee || type == EntityType.BEE) {
            return BEE_SHADER;
        }
        if (entity instanceof MushroomCow || type == EntityType.MOOSHROOM) {
            return MOOSHROOM_SHADER;
        }
        if (entity instanceof Dolphin || type == EntityType.DOLPHIN) {
            return DOLPHIN_SHADER;
        }
        if (entity instanceof WaterAnimal || waterAnimalType(type)) {
            return FISH_EYE_SHADER;
        }
        return null;
    }

    private static boolean waterAnimalType(EntityType<?> type) {
        return type == EntityType.COD
                || type == EntityType.GLOW_SQUID
                || type == EntityType.PUFFERFISH
                || type == EntityType.SALMON
                || type == EntityType.SQUID
                || type == EntityType.TADPOLE
                || type == EntityType.TROPICAL_FISH
                || type == EntityType.TURTLE;
    }

    private static void applyShader(Minecraft minecraft, ResourceLocation shader) {
        if (shader == null) {
            clearShader(minecraft);
            return;
        }
        if (shader.equals(appliedShader) && isAppliedEffectAlive(minecraft)) {
            return;
        }
        clearShader(minecraft);
        minecraft.gameRenderer.loadEffect(shader);
        appliedShader = shader;
        appliedEffect = minecraft.gameRenderer.currentEffect();
    }

    /**
     * Ванидька сама выгружает пост-эффект при смене камеры (вход в тело), F5 и паузе,
     * при этом наш кэш {@link #appliedShader} остаётся устаревшим и эффект больше не
     * загружается. Проверяем живую цепь эффекта и перезагружаем её при необходимости.
     */
    private static boolean isAppliedEffectAlive(Minecraft minecraft) {
        if (minecraft.isPaused()) {
            // Во время паузы ванидька глушит эффекты — не спорим, перезагрузим после снятия паузы.
            return true;
        }
        return minecraft.gameRenderer.currentEffect() == appliedEffect;
    }

    private static void clearShader(Minecraft minecraft) {
        if (appliedShader == null) {
            return;
        }
        minecraft.gameRenderer.shutdownEffect();
        appliedShader = null;
        appliedEffect = null;
    }

    private static ResourceLocation shader(String name) {
        return ModIds.id("shaders/post/" + name + ".json");
    }
}
