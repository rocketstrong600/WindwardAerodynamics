package com.alcorlabs.windwardAerodynamics.foils;

import com.alcorlabs.windwardAerodynamics.math.CubicBSpline;

import java.util.Map;
import java.util.TreeMap;

public class PolarLiftDragCoef {

    private final CubicBSpline liftSpline;
    private final CubicBSpline dragSpline;
    private final CubicBSpline momentSpline;

    public PolarLiftDragCoef(final String name, final TreeMap<Float, float[]> rawData) {
        int D = rawData.size();
        double[] xData = new double[D];
        double[] liftData = new double[D];
        double[] dragData = new double[D];
        double[] momentData = new double[D];

        int k = 0;
        for (Map.Entry<Float, float[]> entry : rawData.entrySet()) {
            xData[k] = entry.getKey();
            liftData[k] = entry.getValue()[0];
            dragData[k] = entry.getValue()[1];
            momentData[k] = entry.getValue()[2];
            k++;
        }

        // 36 intervals for 360 degrees (1 interval per 10 degrees).
        // This compresses 360 data points into 39 control points using Least Squares.
        int intervals = 36;
        this.liftSpline = new CubicBSpline(xData, liftData, intervals);
        this.dragSpline = new CubicBSpline(xData, dragData, intervals);
        this.momentSpline = new CubicBSpline(xData, momentData, intervals);
    }

    /**
     * Gets all Aerodynamic coefficients for respective angle of attack.
     * @param angleAttack The angle of attack in degrees.
     * @param outArray an array of length 3 contains [Lift, Drag, Moment].
     */
    public void getCoefficients(float angleAttack, float[] outArray) {
        outArray[0] = (float) this.liftSpline.evaluate(angleAttack);
        outArray[1] = (float) this.dragSpline.evaluate(angleAttack);
        outArray[2] = (float) this.momentSpline.evaluate(angleAttack);
    }

    /**
     * Gets the derivative of the Aerodynamic coefficients with respect to angle of attack.
     * Useful for Jacobians and implicit solvers.
     * @param angleAttack The angle of attack in degrees.
     * @param outArray an array of length 3 contains [dLift/dAoA, dDrag/dAoA, dMoment/dAoA].
     */
    public void getCoefficientDerivatives(float angleAttack, float[] outArray) {
        outArray[0] = (float) this.liftSpline.evaluateDerivative(angleAttack);
        outArray[1] = (float) this.dragSpline.evaluateDerivative(angleAttack);
        outArray[2] = (float) this.momentSpline.evaluateDerivative(angleAttack);
    }
}