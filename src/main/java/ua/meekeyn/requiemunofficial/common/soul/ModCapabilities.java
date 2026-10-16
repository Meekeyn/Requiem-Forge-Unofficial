package ua.meekeyn.requiemunofficial.common.soul;

import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.CapabilityManager;
import net.minecraftforge.common.capabilities.CapabilityToken;
import net.minecraftforge.common.capabilities.RegisterCapabilitiesEvent;

public final class ModCapabilities {

    public static final Capability<IPlayerSoul> PLAYER_SOUL = CapabilityManager.get(new CapabilityToken<>() {});

    private ModCapabilities() {
    }

    public static void register(RegisterCapabilitiesEvent event) {
        // Реєструє capability-інтерфейс у Forge
        event.register(IPlayerSoul.class);
    }
}