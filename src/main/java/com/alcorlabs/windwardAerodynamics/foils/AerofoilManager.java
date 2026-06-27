package com.alcorlabs.windwardAerodynamics.foils;

import com.alcorlabs.windwardAerodynamics.WindwardAerodynamics;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimplePreparableReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import org.jetbrains.annotations.NotNull;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

public class AerofoilManager extends SimplePreparableReloadListener<Map<ResourceLocation, PolarLiftDragCoef>> {

    private static final String FOLDER = "aerofoils";
    private static final Map<ResourceLocation, PolarLiftDragCoef> FOILS = new HashMap<>();

    public static PolarLiftDragCoef get(ResourceLocation id) {
        return FOILS.get(id);
    }

    public static PolarLiftDragCoef getSymmetric() {
        return get(ResourceLocation.fromNamespaceAndPath(WindwardAerodynamics.MODID, "symmetric"));
    }

    public static PolarLiftDragCoef getCambered() {
        return get(ResourceLocation.fromNamespaceAndPath(WindwardAerodynamics.MODID, "cambered"));
    }

    @Override
    protected @NotNull Map<ResourceLocation, PolarLiftDragCoef> prepare(@NotNull ResourceManager resourceManager, @NotNull ProfilerFiller profiler) {
        Map<ResourceLocation, PolarLiftDragCoef> map = new HashMap<>();

        for (Map.Entry<ResourceLocation, Resource> entry : resourceManager.listResources(FOLDER, loc -> loc.getPath().endsWith(".plr")).entrySet()) {
            ResourceLocation location = entry.getKey();
            String path = location.getPath();
            // path is like "aerofoils/foo.plr"
            String name = path.substring(FOLDER.length() + 1, path.length() - 4);
            ResourceLocation id = ResourceLocation.fromNamespaceAndPath(location.getNamespace(), name);

            try (InputStream stream = entry.getValue().open();
                 BufferedReader br = new BufferedReader(new InputStreamReader(stream))) {

                TreeMap<Float, float[]> rawData = new TreeMap<>();
                String line;
                boolean dataStarted = false;

                while ((line = br.readLine()) != null) {
                    if (dataStarted) {
                        String[] values = line.split("\\s+");
                        if (values.length >= 4) {
                            try {
                                float[] data = { Float.parseFloat(values[1]), Float.parseFloat(values[2]), Float.parseFloat(values[3]) };
                                rawData.put(Float.parseFloat(values[0]), data);
                            } catch (NumberFormatException e) {
                                WindwardAerodynamics.LOGGER.warn("Skipping malformed polar data line in {}: {}", location, line);
                            }
                        }
                    }
                    if (line.startsWith("AOA") && !dataStarted) {
                        dataStarted = true;
                    }
                }

                if (rawData.isEmpty()) {
                    WindwardAerodynamics.LOGGER.warn("No polar data rows parsed for AeroFoil: {}", location);
                } else {
                    map.put(id, new PolarLiftDragCoef(id.toString(), rawData));
                    WindwardAerodynamics.LOGGER.info("Loaded AeroFoil: {} ({} rows)", id, rawData.size());
                }
            } catch (Exception e) {
                WindwardAerodynamics.LOGGER.error("Error Loading Foil Polar Data: {}", location, e);
            }
        }

        return map;
    }

    @Override
    protected void apply(@NotNull Map<ResourceLocation, PolarLiftDragCoef> prepared, @NotNull ResourceManager resourceManager, @NotNull ProfilerFiller profiler) {
        FOILS.clear();
        FOILS.putAll(prepared);
        WindwardAerodynamics.LOGGER.info("Applied {} AeroFoils from datapacks.", FOILS.size());
    }
}
