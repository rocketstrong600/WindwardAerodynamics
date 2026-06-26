package com.alcorlabs.windwardAerodynamics;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.config.ModConfigEvent;
import net.neoforged.neoforge.common.ModConfigSpec;

// An example config class. This is not required, but it's a good idea to have one to keep your config organized.
// Demonstrates how to use Neo's config APIs
public class Config {
    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

    public static final ModConfigSpec.DoubleValue AERO_FORCE_MUL = BUILDER.comment("Aerodynamic Force Multiplier\nTranslate real-life Performance to Sable's low block weights.")
            .translation("config.windward_aerodynamics.aerodynamic_force_multiplier")
            .defineInRange("aeroForceMul", 0.2, 0.001, 2.0);
    public static final ModConfigSpec.BooleanValue ENABLE_WIND = BUILDER.comment("Enable wind\nRequired For Sailing")
            .translation("config.windward_aerodynamics.enable_wind")
            .define("enableWind", true);

    public static final ModConfigSpec.BooleanValue DEBUG_PARTICLES = BUILDER.comment("Debug Particles\nEnable aerodynamic vector visualization particles.")
            .translation("config.windward_aerodynamics.debug_particles")
            .define("debugParticles", false);


    static final ModConfigSpec SPEC = BUILDER.build();
}
