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

    // Оригінальна beta.16 клавіша дисоціації: кома (","), власна категорія мода.
    public static final KeyMapping DISSOCIATION = new KeyMapping(
            "key.requiemunofficial.dissociation",
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_COMMA,
            CATEGORY
    );

    // Port-only клавіша безумовного виходу з тіла. Працює лише коли серверний конфіг
    // allowUnconditionalPossessionExit увімкнено; не поєднана з жодною здібністю.
    public static final KeyMapping UNCONDITIONAL_EXIT = new KeyMapping(
            "key.requiemunofficial.unconditional_exit",
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_V,
            CATEGORY
    );

    private ModKeyMappings() {
    }

    public static void register(RegisterKeyMappingsEvent event) {
        event.register(DISSOCIATION);
        event.register(UNCONDITIONAL_EXIT);
    }
}
