package com.alcorlabs.windwardAerodynamics.api.enviroment;

import org.joml.Vector3d;
import org.joml.Vector3dc;

public class Wind {
    Vector3dc windVelocity;
    public Wind(Vector3dc windVelocity){
        this.windVelocity = windVelocity;
    }
    Vector3dc getWindDirection() {
        return this.windVelocity.normalize(new Vector3d());
    }

    public double getWindAngle() {
        return (Math.toDegrees(Math.atan2(this.windVelocity.x(), -this.windVelocity.z()))+360d)%360d;
    }

    public Vector3dc getWindVelocity() {
        return this.windVelocity;
    }
    public double getWindSpeed() {
        return this.windVelocity.length();
    }
}