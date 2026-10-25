package ua.meekeyn.requiemunofficial;

import com.mojang.logging.LogUtils;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
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

    public RequiemUnofficial() {
        logStartupDiagnosticsMarker();
        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();

        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, RequiemConfig.SPEC);
        ModLoadingContext.get().registerConfig(ModConfig.Type.CLIENT, RequiemClientConfig.SPEC);

        ModItems.register(modEventBus);
        ModEnchantments.register(modEventBus);
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
        ModGameRules.init();
        ModGameRules.register(modEventBus);
        ModCriteria.init();
        modEventBus.addListener(ModCapabilities::register);

        ModNetwork.register();
        modEventBus.addListener(ModEntityAttributes::register);
        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> ClientSetup.init(modEventBus));
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
