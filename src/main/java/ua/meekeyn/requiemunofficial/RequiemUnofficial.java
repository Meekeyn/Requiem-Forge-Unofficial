package ua.meekeyn.requiemunofficial;

import com.mojang.logging.LogUtils;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.fml.ModList;
import net.neoforged.fml.config.ModConfig;
import ua.meekeyn.requiemunofficial.client.ClientSetup;
import ua.meekeyn.requiemunofficial.common.config.RequiemClientConfig;
import ua.meekeyn.requiemunofficial.common.config.RequiemConfig;
import ua.meekeyn.requiemunofficial.common.init.*;
import ua.meekeyn.requiemunofficial.common.network.ModNetwork;
import org.slf4j.Logger;
import ua.meekeyn.requiemunofficial.common.init.ModEntityAttributes;
import ua.meekeyn.requiemunofficial.common.soul.ModCapabilities;
import ua.meekeyn.requiemunofficial.common.advancement.ModCriteria;

@Mod(RequiemUnofficial.MODID)
public class RequiemUnofficial {
    public static final String MODID = "requiemunofficial";
    public static final Logger LOGGER = LogUtils.getLogger();

    public RequiemUnofficial(IEventBus modEventBus, ModContainer modContainer) {
        logStartupDiagnosticsMarker();

        modContainer.registerConfig(ModConfig.Type.COMMON, RequiemConfig.SPEC);
        modContainer.registerConfig(ModConfig.Type.CLIENT, RequiemClientConfig.SPEC);
        ua.meekeyn.requiemunofficial.common.recipe.RecipeConditions.register(modEventBus);

        ModItems.register(modEventBus);
        ModBlocks.register(modEventBus);
        ModBlocks.registerBlockItems();
        ModBlockEntities.register(modEventBus);
        ModEntities.register(modEventBus);
        ModEffects.register(modEventBus);
        ModParticles.register(modEventBus);
        ModSounds.register(modEventBus);
        ModCreativeTabs.register(modEventBus);
        ModStructures.register(modEventBus);
        ModMenuTypes.register(modEventBus);
        ua.meekeyn.requiemunofficial.common.loot.RequiemLootConditions.register(modEventBus);
        ModGameRules.init();
        ModGameRules.register(modEventBus);
        ModCriteria.register(modEventBus);
        ModCapabilities.register(modEventBus);

        modEventBus.addListener(ModNetwork::register);
        ModEntityAttributes.init(modEventBus);
        if (FMLEnvironment.dist == Dist.CLIENT) {
            ClientSetup.init(modEventBus, modContainer);
        }
    }

    private static void logStartupDiagnosticsMarker() {
        if (!Boolean.getBoolean("requiem.vagrantDiagnostics")) {
            return;
        }

        String version = ModList.get().getModContainerById(MODID)
                .map(container -> container.getModInfo().getVersion().toString())
                .orElse("unknown");
        String source = "unknown";
        if (RequiemUnofficial.class.getProtectionDomain() != null
                && RequiemUnofficial.class.getProtectionDomain().getCodeSource() != null
                && RequiemUnofficial.class.getProtectionDomain().getCodeSource().getLocation() != null) {
            source = RequiemUnofficial.class.getProtectionDomain().getCodeSource().getLocation().toString();
        }
        LOGGER.info("[REQ-VAGRANT-DIAG-STARTUP-20261013] mod={} version={} classSource={}", MODID, version, source);
    }
}
