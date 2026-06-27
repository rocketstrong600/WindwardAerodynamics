package com.alcorlabs.windwardAerodynamics.foils;

import java.util.Map;
import java.util.TreeMap;

public class PolarLiftDragCoef {

    // Pre-baked array for O(1) lookups.
    // Format: [aoa_index][0=Lift, 1=Drag, 2=Moment]
    private float[][] bakedData;

    // Configuration for the LUT
    private final float minAoA = -180f;
    private final float maxAoA = 180f;
    private final float resolution = 10f; // 10 entries per degree (0.1 degree accuracy)
    private final int arraySize = (int) ((this.maxAoA - this.minAoA) * this.resolution) + 1;

    public PolarLiftDragCoef(final String name, final TreeMap<Float, float[]> rawData) {
        this.bakeData(rawData);
    }

    private void bakeData(final TreeMap<Float, float[]> rawData) {
        this.bakedData = new float[this.arraySize][3];

        for (int i = 0; i < this.arraySize; i++) {
            final float currentAoA = this.minAoA + (i / this.resolution);
            this.bakedData[i][0] = this.interpolate(rawData, currentAoA, 0); // Lift
            this.bakedData[i][1] = this.interpolate(rawData, currentAoA, 1); // Drag
            this.bakedData[i][2] = this.interpolate(rawData, currentAoA, 2); // Moment
        }
    }

    private float interpolate(TreeMap<Float, float[]> map, float key, int index) {
        if (map.isEmpty()) return 0f;

        final float[] exactValue = map.get(key);
        if (exactValue != null) return exactValue[index];

        final Map.Entry<Float, float[]> floor = map.floorEntry(key);
        final Map.Entry<Float, float[]> ceiling = map.ceilingEntry(key);

        if (floor == null) return ceiling != null ? ceiling.getValue()[index] : 0f;
        if (ceiling == null) return floor.getValue()[index];

        final float alpha1 = floor.getKey();
        final float alpha2 = ceiling.getKey();
        final float ld1 = floor.getValue()[index];
        final float ld2 = ceiling.getValue()[index];

        if (alpha1 == alpha2) return ld1;

        // Linear interpolation between the two nearest data points
        final float t = (key - alpha1) / (alpha2 - alpha1);
        return ld1 + t * (ld2 - ld1);
    }

    /**
     * Gets all Aerodynamic coefficients for respective angle of attack.
     * @param angleAttack The angle of attack in degrees.
     * @param outArray an array of length 3 contains [Lift, Drag, Moment].
     */
    public void getCoefficients(float angleAttack, float[] outArray) {
        // Clamp AoA to our bounds
        angleAttack = Math.clamp(angleAttack, this.minAoA, this.maxAoA);

        // O(1) index calculation, rounded to the nearest LUT entry
        int index = Math.round((angleAttack - this.minAoA) * this.resolution);

        // Safety bound check
        if (index < 0) index = 0;
        if (index >= this.arraySize) index = this.arraySize - 1;

        outArray[0] = this.bakedData[index][0];
        outArray[1] = this.bakedData[index][1];
        outArray[2] = this.bakedData[index][2];
    }
}