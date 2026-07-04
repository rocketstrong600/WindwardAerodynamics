package com.alcorlabs.windwardAerodynamics;

import com.mojang.logging.LogUtils;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
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

@Mod(WindwardAerodynamics.MODID)
public class WindwardAerodynamics {
    public static final String MODID = "windward_aerodynamics";
    public static final Logger LOGGER = LogUtils.getLogger();
    
    public static final Registrate REGISTRATE = Registrate.create(MODID)
        .defaultCreativeTab("windward_tab", builder ->
            builder.title(net.minecraft.network.chat.Component.translatable("itemGroup.windward_aerodynamics"))
                   .withTabsBefore(net.minecraft.world.item.CreativeModeTabs.COMBAT)
                   .icon(() -> new net.minecraft.world.item.ItemStack(Items.COPPER_BLOCK))
        ).build();

    public static final com.tterrag.registrate.util.entry.BlockEntry<com.alcorlabs.windwardAerodynamics.api.block.BallastBlock> BALLAST_BLOCK = REGISTRATE
            .block("ballast", com.alcorlabs.windwardAerodynamics.api.block.BallastBlock::new)
            .initialProperties(() -> Blocks.IRON_BLOCK)
            .simpleItem()
            .register();

    public static final com.tterrag.registrate.util.entry.BlockEntry<net.minecraft.world.level.block.Block> BUOYANCY_TANK_BLOCK = REGISTRATE
            .block("buoyancy_tank", net.minecraft.world.level.block.Block::new)
            .initialProperties(() -> Blocks.COPPER_BLOCK)
            .simpleItem()
            .register();

    public WindwardAerodynamics(IEventBus modEventBus, ModContainer modContainer) {
        modEventBus.addListener(this::commonSetup);

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
            
            generator.addProvider(
                event.includeClient(), 
                new com.alcorlabs.windwardAerodynamics.datagen.WindwardBlockstateGenerator(packOutput)
            );
        }
    }
}
