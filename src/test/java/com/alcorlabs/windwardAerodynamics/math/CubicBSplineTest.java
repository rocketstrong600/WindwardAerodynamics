package com.alcorlabs.windwardAerodynamics.math;

public class CubicBSplineTest {

    public static void main(String[] args) {
        // Generate a synthetic polar curve: A smooth sine wave for Lift
        int numPoints = 360;
        double[] xData = new double[numPoints];
        double[] yData = new double[numPoints];
        
        for (int i = 0; i < numPoints; i++) {
            xData[i] = -180.0 + i;
            yData[i] = Math.sin(Math.toRadians(xData[i])) * 1.5; // Simulate a max lift of 1.5
        }
        
        // Fit a B-spline with 36 intervals (39 control points)
        int intervals = 36;
        CubicBSpline spline = new CubicBSpline(xData, yData, intervals);
        
        // Check error
        double maxError = 0.0;
        double sumSqError = 0.0;
        for (int i = 0; i < numPoints; i++) {
            double actual = yData[i];
            double predicted = spline.evaluate(xData[i]);
            double error = Math.abs(actual - predicted);
            maxError = Math.max(maxError, error);
            sumSqError += error * error;
        }
        
        double meanSqError = sumSqError / numPoints;
        
        System.out.println("Max Error: " + maxError);
        System.out.println("Mean Squared Error: " + meanSqError);
        
        // The error should be very small for a smooth curve
        if (maxError >= 0.05) {
            throw new AssertionError("Max error should be small");
        }
        if (meanSqError >= 0.001) {
            throw new AssertionError("MSE should be very small");
        }
        System.out.println("Test Passed!");
    }
}
