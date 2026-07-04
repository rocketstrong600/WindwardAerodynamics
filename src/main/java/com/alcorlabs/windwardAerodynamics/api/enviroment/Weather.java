package com.alcorlabs.windwardAerodynamics.api.enviroment;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.storage.WorldData;
import org.joml.Vector3dc;

public interface Weather {
    Wind getWind(Vector3dc position, ServerLevel level);

}
