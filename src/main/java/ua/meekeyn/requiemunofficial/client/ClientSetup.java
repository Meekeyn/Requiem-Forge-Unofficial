package ua.meekeyn.requiemunofficial.client;

import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.ConfigScreenHandler;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.client.event.RegisterParticleProvidersEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import ua.meekeyn.requiemunofficial.RequiemUnofficial;
import ua.meekeyn.requiemunofficial.client.particle.ObeliskSoulParticle;
import ua.meekeyn.requiemunofficial.client.particle.SimpleGhostParticle;
import ua.meekeyn.requiemunofficial.client.particle.SoulTrailParticle;
import ua.meekeyn.requiemunofficial.client.render.block.RunestoneBlockEntityRenderer;
import ua.meekeyn.requiemunofficial.client.render.entity.model.MorticianModel;
import ua.meekeyn.requiemunofficial.client.render.entity.MorticianRenderer;
import ua.meekeyn.requiemunofficial.client.render.entity.ObeliskSoulRenderer;
import ua.meekeyn.requiemunofficial.client.render.entity.PlayerShellRenderer;
import ua.meekeyn.requiemunofficial.client.render.entity.ReleasedSoulRenderer;
import ua.meekeyn.requiemunofficial.client.render.entity.CuredPiglinBruteRenderer;
import ua.meekeyn.requiemunofficial.client.render.entity.CuredPiglinRenderer;
import ua.meekeyn.requiemunofficial.client.render.entity.CuredVillagerRenderer;
import ua.meekeyn.requiemunofficial.client.screen.RiftScreen;
import ua.meekeyn.requiemunofficial.client.screen.RequiemConfigScreen;
import ua.meekeyn.requiemunofficial.common.init.ModEntities;
import ua.meekeyn.requiemunofficial.common.init.ModBlockEntities;
import ua.meekeyn.requiemunofficial.common.init.ModMenuTypes;
import ua.meekeyn.requiemunofficial.common.init.ModParticles;

@Mod.EventBusSubscriber(modid = RequiemUnofficial.MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class ClientSetup {

    private ClientSetup() {
    }

    public static void init(IEventBus modEventBus) {
        ModLoadingContext.get().registerExtensionPoint(
                ConfigScreenHandler.ConfigScreenFactory.class,
                () -> new ConfigScreenHandler.ConfigScreenFactory((minecraft, parent) -> new RequiemConfigScreen(parent))
        );
        modEventBus.addListener(ClientSetup::onClientSetup);
        modEventBus.addListener(ClientSetup::registerEntityRenderers);
        modEventBus.addListener(ClientSetup::registerLayerDefinitions);
        modEventBus.addListener(ClientSetup::registerParticleProviders);
        modEventBus.addListener(ModKeyMappings::register);
    }

    private static void onClientSetup(FMLClientSetupEvent event) {
        event.enqueueWork(() -> {
            // Реєструє клієнтський екран для меню рифту
            MenuScreens.register(ModMenuTypes.RIFT.get(), RiftScreen::new);
        });

        RequiemUnofficial.LOGGER.info("Client setup initialized");
    }

    private static void registerEntityRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(ModEntities.MORTICIAN.get(), MorticianRenderer::new);
        event.registerEntityRenderer(ModEntities.PLAYER_SHELL.get(), PlayerShellRenderer::new);
        event.registerEntityRenderer(ModEntities.OBELISK_SOUL.get(), ObeliskSoulRenderer::new);
        event.registerEntityRenderer(ModEntities.RELEASED_SOUL.get(), ReleasedSoulRenderer::new);
        event.registerEntityRenderer(ModEntities.CURED_VILLAGER.get(), CuredVillagerRenderer::new);
        event.registerEntityRenderer(ModEntities.CURED_PIGLIN.get(), CuredPiglinRenderer::new);
        event.registerEntityRenderer(ModEntities.CURED_PIGLIN_BRUTE.get(), CuredPiglinBruteRenderer::new);
        event.registerBlockEntityRenderer(ModBlockEntities.RUNESTONE.get(), RunestoneBlockEntityRenderer::new);
    }

    private static void registerLayerDefinitions(EntityRenderersEvent.RegisterLayerDefinitions event) {
        event.registerLayerDefinition(MorticianModel.MODEL_LAYER, MorticianModel::createBodyLayer);
    }

    private static void registerParticleProviders(RegisterParticleProvidersEvent event) {
        event.registerSpriteSet(ModParticles.OBELISK_SOUL.get(), ObeliskSoulParticle.Provider::new);
        event.registerSpriteSet(ModParticles.SOUL_TRAIL.get(), SoulTrailParticle.Provider::new);
        event.registerSpriteSet(ModParticles.GHOST.get(), SimpleGhostParticle.Provider::new);
    }
}
