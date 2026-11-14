
package ua.meekeyn.requiemunofficial.client;

import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.minecraft.client.Minecraft;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;
import ua.meekeyn.requiemunofficial.common.soul.SoulManager;
import ua.meekeyn.requiemunofficial.common.soul.SoulState;

@EventBusSubscriber(modid = "requiemunofficial", value = net.neoforged.api.distmarker.Dist.CLIENT)
public final class ClientVagrantNightVision {

    /**
     * Длительность ночного зрения души. Ваниль заставляет яркость ПУЛЬСИРОВАТЬ
     * (2 Гц мерцание в {@code GameRenderer#getNightVisionScale}), когда длительность
     * опускается до {@code <= 200} тиков. Держим запас выше этого порога.
     */
    private static final int SOUL_NV_DURATION = 400;
    /** Обновляем эффект, пока он не дотянул до этой отметки (запас ~40 тиков до порога 200). */
    private static final int SOUL_NV_REFRESH = 240;

    /**
     * Установлен, когда ночное зрения наложено модом (своя душа либо унаследованное
     * от тела). Наш эффект всегда {@code ambient=true} — по этому признаку отличаем
     * его от «чужого» ночного зрения (зелье/маяк, {@code ambient=false}), которое
     * снимать нельзя.
     */
    private static boolean appliedByMod;

    private ClientVagrantNightVision() {
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {

        Minecraft minecraft = Minecraft.getInstance();
        Player player = minecraft.player;

        if (player == null || minecraft.level == null) {
            appliedByMod = false;
            return;
        }

        boolean isVagrant = SoulManager.getState(player) == SoulState.VAGRANT;
        MobEffectInstance current = player.getEffect(MobEffects.NIGHT_VISION);

        if (isVagrant) {
            if (current == null) {
                // Ночного зору немає — накладаємо власне (мі дбаємо про нього надалі).
                addSoulNightVision(player);
                appliedByMod = true;
            } else if (appliedByMod && current.isAmbient()
                    && current.getDuration() <= SOUL_NV_REFRESH) {
                // Наш власний ambient-зір спадає — продовжуємо його ДО того, як він
                // увійде в зону пульсації (<=200 тиків) чи вичерпається (щоб не було
                // мерцання та «пробелів»). Власність відстежуємо лише за тим, що
                // наклали самі (appliedByMod), а не за ambient — так не зачепимо чужий зір.
                addSoulNightVision(player);
            }
            // Чужий нічний зір (зілля/маяк, ambient=false або накладений до нас) не
            // чіпаємо взагалі: не продовжуємо і власність не заявляємо.
        } else if (appliedByMod) {
            // Гравець вийшов зі стану VAGRANT — повертаємо звичайний зір. Знімаємо лише
            // власний ефект (ambient=true); якщо зараз активний чужий — не чіпаємо.
            if (current == null || current.isAmbient()) {
                player.removeEffect(MobEffects.NIGHT_VISION);
            }
            appliedByMod = false;
        }
    }

    private static void addSoulNightVision(Player player) {
        player.addEffect(new MobEffectInstance(
                MobEffects.NIGHT_VISION,
                SOUL_NV_DURATION,
                0,
                true,
                false,
                false
        ));
    }
}
