package com.alcorlabs.windwardAerodynamics;

import com.mojang.logging.LogUtils;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.client.gui.ConfigurationScreen;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;
import net.neoforged.neoforge.common.NeoForge;
import org.slf4j.Logger;
import com.tterrag.registrate.Registrate;

// The value here should match an entry in the META-INF/neoforge.mods.toml file
@Mod(WindwardAerodynamics.MODID)
public class WindwardAerodynamics {
    // Define mod id in a common place for everything to reference
    public static final String MODID = "windward_aerodynamics";
    // Directly reference a slf4j logger
    public static final Logger LOGGER = LogUtils.getLogger();
    
    // Create the Registrate instance for the mod
    public static final Registrate REGISTRATE = Registrate.create(MODID);

    // The constructor for the mod class is the first code that is run when your mod is loaded.
    // FML will recognize some parameter types like IEventBus or ModContainer and pass them in automatically.
    public WindwardAerodynamics(IEventBus modEventBus, ModContainer modContainer) {
        // Register the commonSetup method for modloading
        modEventBus.addListener(this::commonSetup);

        //register configs
        modContainer.registerConfig(ModConfig.Type.SERVER, Config.SERVER_SPEC);
        modContainer.registerConfig(ModConfig.Type.CLIENT, Config.CLIENT_SPEC);

        modContainer.registerExtensionPoint(IConfigScreenFactory.class, ConfigurationScreen::new);

        NeoForge.EVENT_BUS.addListener(this::addReloadListeners);
    }

    private void commonSetup(final FMLCommonSetupEvent event) {
    }

    public void addReloadListeners(net.neoforged.neoforge.event.AddReloadListenerEvent event) {
        event.addListener(new com.alcorlabs.windwardAerodynamics.foils.AerofoilManager());
    }

    // You can use EventBusSubscriber to automatically register all static methods in the class annotated with @SubscribeEvent
    @EventBusSubscriber(modid = MODID, value = Dist.CLIENT)
    public static class ClientModEvents {
        @SubscribeEvent
        public static void onClientSetup(FMLClientSetupEvent event) {
        }
    }

    @EventBusSubscriber(modid = MODID)
    public static class DataGenerators {
        @SubscribeEvent
        public static void gatherData(net.neoforged.neoforge.data.event.GatherDataEvent event) {
            net.minecraft.data.DataGenerator generator = event.getGenerator();
            net.minecraft.data.PackOutput packOutput = generator.getPackOutput();
            
            // Register our blockstate generator
            generator.addProvider(
                event.includeClient(), 
                new com.alcorlabs.windwardAerodynamics.datagen.WindwardBlockstateGenerator(packOutput)
            );
        }
    }
}
