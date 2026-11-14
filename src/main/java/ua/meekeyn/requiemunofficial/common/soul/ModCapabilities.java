package ua.meekeyn.requiemunofficial.common.soul;

import java.util.Optional;
import java.util.function.Supplier;
import net.minecraft.world.entity.Entity;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;
import ua.meekeyn.requiemunofficial.RequiemUnofficial;

public final class ModCapabilities {

    private static final DeferredRegister<AttachmentType<?>> ATTACHMENTS =
            DeferredRegister.create(NeoForgeRegistries.ATTACHMENT_TYPES, RequiemUnofficial.MODID);

    public static final Supplier<AttachmentType<PlayerSoul>> PLAYER_SOUL = ATTACHMENTS.register(
            "player_soul",
            () -> AttachmentType.builder(PlayerSoul::new).serialize(new PlayerSoulProvider()).build()
    );

    private ModCapabilities() {
    }

    public static void register(IEventBus modEventBus) {
        ATTACHMENTS.register(modEventBus);
    }

    /** Заміна старого getCapability(PLAYER_SOUL): душа є в кожного гравця. */
    public static Optional<IPlayerSoul> soul(Entity entity) {
        if (entity instanceof net.minecraft.world.entity.player.Player) {
            return Optional.of(entity.getData(PLAYER_SOUL));
        }
        return Optional.empty();
    }
}
