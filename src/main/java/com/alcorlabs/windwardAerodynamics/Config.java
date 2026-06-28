package com.alcorlabs.windwardAerodynamics;

import net.neoforged.neoforge.common.ModConfigSpec;

public class Config {
    private static final ModConfigSpec.Builder SERVER_BUILDER = new ModConfigSpec.Builder();
    private static final ModConfigSpec.Builder CLIENT_BUILDER = new ModConfigSpec.Builder();

    // SERVER CONFIG
    public static final ModConfigSpec.DoubleValue AERO_FORCE_MULTIPLIER = SERVER_BUILDER.comment("Aerodynamic Force Multiplier" +
                    "\nTranslate real-life Performance to Sable's low block weights." +
                    "\nThis may seem unrealistic however sables block masses are lower than real counterparts.")
            .translation("windward_aerodynamics.configuration.aerodynamic_force_multiplier")
            .defineInRange("aeroForceMul", 0.2, 0.001, 2.0);
            
    public static final ModConfigSpec.BooleanValue ENABLE_WIND = SERVER_BUILDER.comment("Enable wind\nRequired For Sailing\nNot Implemented Yet")
            .translation("windward_aerodynamics.configuration.enable_wind")
            .define("enableWind", true);

    public static final ModConfigSpec.DoubleValue OSWALD_EFFICIENCY = SERVER_BUILDER.comment("Oswald Efficiency (e)\nControls induced drag penalty for aspect ratio. 1.0 is theoretically perfect wing. Lower values increase drag.")
            .translation("windward_aerodynamics.configuration.oswald_efficiency")
            .defineInRange("advanced_aerodynamics.oswaldEfficiency", 0.98, 0.5, 2.0);

    public static final ModConfigSpec.DoubleValue DYNAMIC_STALL_TIME_CONSTANT = SERVER_BUILDER.comment("Dynamic Stall Time Constant (k)\nControls how many chords of travel the wake takes to settle (Unsteady Aerodynamics).\nHigher values = more 'sticky' lag. Lower values = instantaneous snap.")
            .translation("windward_aerodynamics.configuration.dynamic_stall_time_constant")
            .defineInRange("advanced_aerodynamics.dynamicStallTimeConstant", 1.5, 0.1, 10.0);

    public static final ModConfigSpec.BooleanValue ENABLE_FINITE_WING_CORRECTION = SERVER_BUILDER.comment("Enable Finite Wing Lift Correction\nApplies Prandtl Lifting-Line theory to scale down lift based on aspect ratio.\nWARNING: Low aspect ratio wings (common in Minecraft) will generate significantly less lift if this is true!")
            .translation("windward_aerodynamics.configuration.enable_finite_wing_correction")
            .define("advanced_aerodynamics.enableFiniteWingCorrection", false);

    public static final ModConfigSpec.DoubleValue MAX_POSITIVE_FEEDBACK_SCALE = SERVER_BUILDER.comment("Max Positive Jacobian Feedback Scale\nControls how much 'positive feedback' (stalls, aggressive cross-coupling) is allowed before clamping.\n0.0 = Safest, strips positive feedback to prevent physics explosions.\n1.0 = Highly realistic, but might cause physics engine instability")
            .translation("windward_aerodynamics.configuration.max_positive_feedback_scale")
            .defineInRange("advanced_aerodynamics.maxPositiveFeedbackScale", 0.0, 0.0, 1.0);

    // CLIENT CONFIG
    public static final ModConfigSpec.BooleanValue DEBUG_PARTICLES = CLIENT_BUILDER.comment("Debug Particles\nEnable Aerofoil Normal visualization particles.")
            .translation("windward_aerodynamics.configuration.debug_particles")
            .define("dev.debugParticles", false);

    static final ModConfigSpec SERVER_SPEC = SERVER_BUILDER.build();
    static final ModConfigSpec CLIENT_SPEC = CLIENT_BUILDER.build();
}
