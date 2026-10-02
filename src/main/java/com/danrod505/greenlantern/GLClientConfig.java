package com.danrod505.greenlantern;

import net.minecraftforge.common.ForgeConfigSpec;

/** Client-only preferences (config/greenlantern-client.toml). */
public final class GLClientConfig {
    private static final ForgeConfigSpec.Builder BUILDER = new ForgeConfigSpec.Builder();

    public static final ForgeConfigSpec.BooleanValue FLIGHT_MUSIC;
    public static final ForgeConfigSpec.EnumValue<FlightTheme> FLIGHT_THEME;
    public static final ForgeConfigSpec.DoubleValue FLIGHT_MUSIC_VOLUME;
    public static final ForgeConfigSpec.BooleanValue CAMERA_EFFECTS;
    public static final ForgeConfigSpec.BooleanValue SPEED_LINES;
    public static final ForgeConfigSpec.BooleanValue TRAILS;
    public static final ForgeConfigSpec SPEC;

    static {
        BUILDER.push("flight");
        FLIGHT_MUSIC = BUILDER.comment("Play the flight theme while flying fast (uses the Music volume slider).").define("flightMusic", true);
        FLIGHT_THEME = BUILDER.comment("Which flight theme to play.",
                "LANTERNS: the 'Lanterns Flight Theme' song; it jumps to its climax when you break the sound barrier.",
                "ORIGINAL: the procedural orchestral theme; its epic layer joins when you break the sound barrier.")
                .defineEnum("flightTheme", FlightTheme.LANTERNS);
        FLIGHT_MUSIC_VOLUME = BUILDER.comment("Extra volume multiplier for the flight theme.").defineInRange("flightMusicVolume", 1.0, 0.0, 1.0);
        CAMERA_EFFECTS = BUILDER.comment("Camera shake, banking and barrel-roll camera rotation while flying.").define("cameraEffects", true);
        SPEED_LINES = BUILDER.comment("Speed lines and Mach meter on screen while flying fast.").define("speedLines", true);
        TRAILS = BUILDER.comment("Energy trails behind flying Lanterns.").define("trails", true);
        BUILDER.pop();
        SPEC = BUILDER.build();
    }

    public enum FlightTheme {
        LANTERNS,
        ORIGINAL
    }

    private GLClientConfig() {}
}
