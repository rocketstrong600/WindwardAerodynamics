package com.alcorlabs.windwardAerodynamics.enviroment;

import com.alcorlabs.windwardAerodynamics.Config;
import com.alcorlabs.windwardAerodynamics.api.enviroment.Weather;
import com.alcorlabs.windwardAerodynamics.api.enviroment.Wind;
import net.minecraft.server.level.ServerLevel;
import org.joml.Vector3d;
import org.joml.Vector3dc;

public class BasicWeather implements Weather {
    @Override
    public Wind getWind(Vector3dc position, ServerLevel level) {
        return new Wind(new Vector3d(0,0, Config.WIND_SPEED_TEST.get()));
    }
}
