package com.alcorlabs.windwardAerodynamics;

import net.neoforged.neoforge.common.ModConfigSpec;

public class Config {
    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

    public static final ModConfigSpec.DoubleValue AERO_FORCE_MUL = BUILDER.comment("Aerodynamic Force Multiplier" +
                    "\nTranslate real-life Performance to Sable's low block weights." +
                    "\nPast 0.4 certain builds will be unstable in the physics engine." +
                    "\nThis may seem unrealistic however sables block masses are lower that real counterparts.")
            .translation("config.windward_aerodynamics.aerodynamic_force_multiplier")
            .defineInRange("aeroForceMul", 0.2, 0.001, 2.0);
    public static final ModConfigSpec.BooleanValue ENABLE_WIND = BUILDER.comment("Enable wind\nRequired For Sailing")
            .translation("config.windward_aerodynamics.enable_wind")
            .define("enableWind", true);

    public static final ModConfigSpec.BooleanValue DEBUG_PARTICLES = BUILDER.comment("Debug Particles\nEnable Aerofoil Normal visualization particles.")
            .translation("config.windward_aerodynamics.debug_particles")
            .define("debugParticles", false);


    static final ModConfigSpec SPEC = BUILDER.build();
}
