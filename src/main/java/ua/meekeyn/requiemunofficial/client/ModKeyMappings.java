package ua.meekeyn.requiemunofficial.client;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import org.lwjgl.glfw.GLFW;

public final class ModKeyMappings {

    public static final KeyMapping DISSOCIATION = new KeyMapping(
            "key.requiemunofficial.dissociation",
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_G,
            "key.categories.requiemunofficial"
    );

    public static final KeyMapping POSSESSION_INTERACT = new KeyMapping(
            "key.requiemunofficial.possession_interact",
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_V,
            "key.categories.requiemunofficial"
    );

    private ModKeyMappings() {
    }

    public static void register(RegisterKeyMappingsEvent event) {
        event.register(DISSOCIATION);
        event.register(POSSESSION_INTERACT);
    }
}
