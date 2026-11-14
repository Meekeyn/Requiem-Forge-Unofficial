package ua.meekeyn.requiemunofficial.client.compat;

import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.neoforged.fml.ModList;
import ua.meekeyn.requiemunofficial.RequiemUnofficial;
import ua.meekeyn.requiemunofficial.common.soul.SoulManager;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.UUID;
import java.util.function.Predicate;

/**
 * Сумісність із Jade (лише клієнт).
 *
 * Jade 11 ({@code snownee.jade.overlay.RayTracing}) бере ванільний {@code Minecraft.hitResult}
 * тільки коли той вказує на сутність; інакше (погляд на блок або в порожнечу) робить власний
 * промінь {@code RayTracing.getEntityHitResult}, який вважає влучанням будь-яку сутність, чия
 * рамка (+0.3) містить точку камери. Камера гравця, що вселився, завжди всередині власного
 * тіла-носія, тож Jade показував підказку цього носія замість блока чи порожнечі. Ванільний
 * {@code GameRenderer.pick} носія вже виключає ({@code GameRendererMixin}), а Jade — ні.
 *
 * Тут до публічного {@code RayTracing.ENTITY_FILTER} додається умова «не власний носій
 * локального гравця». Рефлексія, бо Jade підключено лише як runtimeOnly.
 */
public final class JadeRequiemCompat {

    private static final String JADE_MOD_ID = "jade";
    private static final String RAY_TRACING_CLASS = "snownee.jade.overlay.RayTracing";
    private static final String ENTITY_FILTER_FIELD = "ENTITY_FILTER";

    private static boolean installed;

    private JadeRequiemCompat() {
    }

    public static void install() {
        if (installed || !ModList.get().isLoaded(JADE_MOD_ID)) {
            return;
        }
        try {
            Class<?> rayTracing = Class.forName(RAY_TRACING_CLASS);
            Field field = rayTracing.getField(ENTITY_FILTER_FIELD);
            if (!Modifier.isStatic(field.getModifiers()) || Modifier.isFinal(field.getModifiers())) {
                RequiemUnofficial.LOGGER.warn("Jade RayTracing.ENTITY_FILTER is not a mutable static field, skipping");
                return;
            }
            @SuppressWarnings("unchecked")
            Predicate<Entity> original = (Predicate<Entity>) field.get(null);
            Predicate<Entity> base = original == null ? entity -> true : original;
            field.set(null, base.and(entity -> !isOwnPossessedHost(entity)));
            installed = true;
            RequiemUnofficial.LOGGER.info("Jade compat: own possessed host excluded from Jade ray tracing");
        } catch (ReflectiveOperationException | ClassCastException | LinkageError e) {
            RequiemUnofficial.LOGGER.warn("Jade compat could not be installed", e);
        }
    }

    /**
     * Чи є сутність тілом, яким зараз керує локальний гравець.
     * Інші скелети/моби того ж типу не зачіпаються — порівняння за UUID.
     */
    public static boolean isOwnPossessedHost(Entity entity) {
        Player player = Minecraft.getInstance().player;
        if (player == null || entity == null || entity == player) {
            return false;
        }
        UUID possessedId = SoulManager.getPossessedEntityId(player);
        return possessedId != null && possessedId.equals(entity.getUUID());
    }
}
