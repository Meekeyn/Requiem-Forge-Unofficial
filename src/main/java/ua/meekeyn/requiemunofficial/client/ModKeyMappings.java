package ua.meekeyn.requiemunofficial.client;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import org.lwjgl.glfw.GLFW;

public final class ModKeyMappings {

    /**
     * Власна категорія клавіш мода: уся низка Requiem-клавіш показується в
     * «Options → Controls» під окремим заголовком «Requiem», а не у ванільній
     * категорії Gameplay. (У beta.16 дисоціація жила в категорії Gameplay; тут
     * категорія свідомо винесена окремо за вимогою специфікації порту.)
     */
    public static final String CATEGORY = "key.categories.requiemunofficial";

    // Безумовний вихід із тіла (завжди увімкнений, M). Замінює beta.16-клавішу дисоціації (',').
    public static final KeyMapping UNCONDITIONAL_EXIT = new KeyMapping(
            "key.requiemunofficial.unconditional_exit",
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_M,
            CATEGORY
    );

    // Здібність тіла (телепорт ендермена, прикликання векс, випад тощо). За замовчуванням — права кнопка миші,
    // як в оригінальному Requiem; перепризначається в «Options → Controls → Requiem».
    public static final KeyMapping ABILITY = new KeyMapping(
            "key.requiemunofficial.ability",
            InputConstants.Type.MOUSE,
            GLFW.GLFW_MOUSE_BUTTON_RIGHT,
            CATEGORY
    );

    private ModKeyMappings() {
    }

    public static void register(RegisterKeyMappingsEvent event) {
        event.register(UNCONDITIONAL_EXIT);
        event.register(ABILITY);
    }
}
