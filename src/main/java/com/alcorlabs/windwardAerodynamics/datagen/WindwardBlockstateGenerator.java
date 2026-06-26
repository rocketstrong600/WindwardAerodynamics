package com.alcorlabs.windwardAerodynamics.datagen;

import com.google.gson.JsonObject;
import com.google.gson.JsonArray;
import net.minecraft.data.CachedOutput;
import net.minecraft.data.DataProvider;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.ResourceLocation;

import java.util.concurrent.CompletableFuture;
import java.util.List;
import java.util.ArrayList;

public class WindwardBlockstateGenerator implements DataProvider {
    private final PackOutput output;
    private final String[] COLORS = {
            "white", "orange", "magenta", "light_blue", "yellow", "lime",
            "pink", "gray", "light_gray", "cyan", "purple", "blue", "brown",
            "green", "red", "black"
    };

    public WindwardBlockstateGenerator(PackOutput output) {
        this.output = output;
    }

    @Override
    public CompletableFuture<?> run(CachedOutput cachedOutput) {
        List<CompletableFuture<?>> futures = new ArrayList<>();

        for (String color : COLORS) {
            // Create Sail Blockstate
            JsonObject createSail = generateSailBlockstate(color, "create");
            futures.add(DataProvider.saveStable(cachedOutput, createSail, output.getOutputFolder(PackOutput.Target.RESOURCE_PACK).resolve("create/blockstates/" + color + "_sail.json")));
            
            // Create Sail Rolled Model (Child of white_sail_rolled)
            JsonObject createModelJson = new JsonObject();
            createModelJson.addProperty("parent", "create:block/white_sail_rolled");
            JsonObject createTextures = new JsonObject();
            createTextures.addProperty("0", "create:block/sail/canvas_" + color);
            createTextures.addProperty("particle", "create:block/sail/canvas_" + color);
            createModelJson.add("textures", createTextures);
            futures.add(DataProvider.saveStable(cachedOutput, createModelJson, output.getOutputFolder(PackOutput.Target.RESOURCE_PACK).resolve("windward_aerodynamics/models/block/" + color + "_sail_rolled.json")));

            // Symmetric Sail Blockstate
            JsonObject symmetricSail = generateSymmetricSailBlockstate(color);
            futures.add(DataProvider.saveStable(cachedOutput, symmetricSail, output.getOutputFolder(PackOutput.Target.RESOURCE_PACK).resolve("simulated/blockstates/" + color + "_symmetric_sail.json")));
            
            // Symmetric Sail Canvas Blockstate
            JsonObject symmetricSailCanvas = generateSymmetricSailBlockstate(color + "_symmetric_sail_canvas");
            futures.add(DataProvider.saveStable(cachedOutput, symmetricSailCanvas, output.getOutputFolder(PackOutput.Target.RESOURCE_PACK).resolve("simulated/blockstates/" + color + "_symmetric_sail_canvas.json")));
            
            // Symmetric Sail Rolled Models (Child of symmetric_sail/block_rolled)
            JsonObject simModelJson = new JsonObject();
            simModelJson.addProperty("parent", "simulated:block/symmetric_sail/block_rolled");
            JsonObject simTextures = new JsonObject();
            simTextures.addProperty("0", "create:block/sail/canvas_" + color);
            simTextures.addProperty("1", "simulated:block/symmetric_sail/side_" + color);
            simTextures.addProperty("particle", "create:block/sail/canvas_" + color);
            simModelJson.add("textures", simTextures);
            
            futures.add(DataProvider.saveStable(cachedOutput, simModelJson, output.getOutputFolder(PackOutput.Target.RESOURCE_PACK).resolve("windward_aerodynamics/models/block/" + color + "_symmetric_sail_rolled.json")));
            futures.add(DataProvider.saveStable(cachedOutput, simModelJson, output.getOutputFolder(PackOutput.Target.RESOURCE_PACK).resolve("windward_aerodynamics/models/block/" + color + "_symmetric_sail_canvas_rolled.json")));
        }

        return CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]));
    }

    private JsonObject generateSailBlockstate(String color, String namespace) {
        String base = namespace + ":block/" + color + "_sail";
        String rolled = "windward_aerodynamics:block/" + color + "_sail_rolled";
        return buildSailVariants(base, rolled);
    }

    private JsonObject generateSymmetricSailBlockstate(String name) {
        String base = "simulated:block/" + name + "/block"; // Assuming Simulated registers them this way
        String rolled = "windward_aerodynamics:block/" + name + "_rolled";
        return buildSailVariants(base, rolled);
    }

    private JsonObject buildSailVariants(String base, String rolled) {
        JsonObject variants = new JsonObject();

        // UP (facing=up)
        // chord_rotation index 0 = NORTH
        variants.add("chord_rotation=0,facing=up", createVariant(base, 0, 0));
        variants.add("chord_rotation=1,facing=up", createVariant(base, 0, 90));
        variants.add("chord_rotation=2,facing=up", createVariant(base, 0, 180));
        variants.add("chord_rotation=3,facing=up", createVariant(base, 0, 270));

        // DOWN (facing=down)
        // chord_rotation index 0 = NORTH (x=180, y=180)
        variants.add("chord_rotation=0,facing=down", createVariant(base, 180, 180));
        variants.add("chord_rotation=1,facing=down", createVariant(base, 180, 90));
        variants.add("chord_rotation=2,facing=down", createVariant(base, 180, 0));
        variants.add("chord_rotation=3,facing=down", createVariant(base, 180, 270));

        // NORTH (facing=north) - Impossible to point chord North/South, defaults to UP
        variants.add("chord_rotation=0,facing=north", createVariant(base, 270, 180)); // Default fallback
        variants.add("chord_rotation=1,facing=north", createVariant(rolled, 270, 180)); // UP
        variants.add("chord_rotation=2,facing=north", createVariant(base, 90, 0)); // Default fallback
        variants.add("chord_rotation=3,facing=north", createVariant(rolled, 90, 0)); // DOWN

        // SOUTH (facing=south)
        variants.add("chord_rotation=0,facing=south", createVariant(base, 270, 0)); // Default fallback
        variants.add("chord_rotation=1,facing=south", createVariant(rolled, 270, 0)); // UP
        variants.add("chord_rotation=2,facing=south", createVariant(base, 90, 180)); // Default fallback
        variants.add("chord_rotation=3,facing=south", createVariant(rolled, 90, 180)); // DOWN

        // WEST (facing=west)
        // chord_rotation index 0 = NORTH (x=90, y=270)
        variants.add("chord_rotation=0,facing=west", createVariant(rolled, 90, 270));
        variants.add("chord_rotation=1,facing=west", createVariant(base, 270, 90));
        variants.add("chord_rotation=2,facing=west", createVariant(rolled, 270, 90));
        variants.add("chord_rotation=3,facing=west", createVariant(base, 90, 270));

        // EAST (facing=east)
        // chord_rotation index 0 = NORTH (x=270, y=270)
        variants.add("chord_rotation=0,facing=east", createVariant(rolled, 270, 270));
        variants.add("chord_rotation=1,facing=east", createVariant(base, 270, 270));
        variants.add("chord_rotation=2,facing=east", createVariant(rolled, 90, 90));
        variants.add("chord_rotation=3,facing=east", createVariant(base, 90, 90));

        JsonObject root = new JsonObject();
        root.add("variants", variants);
        return root;
    }

    private JsonObject createVariant(String model, int x, int y) {
        JsonObject obj = new JsonObject();
        obj.addProperty("model", model);
        if (x != 0) obj.addProperty("x", x);
        if (y != 0) obj.addProperty("y", y);
        return obj;
    }

    @Override
    public String getName() {
        return "Windward Aerodynamics Blockstates";
    }
}
