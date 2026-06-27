package com.alcorlabs.windwardAerodynamics.math;

public class CubicBSpline {
    private final double[] controlPoints;
    private final double minX;
    private final double maxX;
    private final int numIntervals;

    public CubicBSpline(double[] xData, double[] yData, int numIntervals) {
        this.minX = xData[0];
        this.maxX = xData[xData.length - 1];
        this.numIntervals = numIntervals;
        
        int D = xData.length;
        int N = numIntervals + 3;
        
        double[][] ATA = new double[N][N];
        double[] ATY = new double[N];
        
        for (int k = 0; k < D; k++) {
            double x = xData[k];
            double y = yData[k];
            
            double t = (x - minX) / (maxX - minX) * numIntervals;
            int i = (int) Math.floor(t);
            if (i >= numIntervals) {
                i = numIntervals - 1;
            }
            if (i < 0) {
                i = 0;
            }
            double u = t - i;
            
            double[] B = new double[4];
            B[0] = (1 - u) * (1 - u) * (1 - u) / 6.0;
            B[1] = (3 * u * u * u - 6 * u * u + 4) / 6.0;
            B[2] = (-3 * u * u * u + 3 * u * u + 3 * u + 1) / 6.0;
            B[3] = u * u * u / 6.0;
            
            for (int j1 = 0; j1 < 4; j1++) {
                int c1 = i + j1;
                ATY[c1] += B[j1] * y;
                for (int j2 = 0; j2 < 4; j2++) {
                    int c2 = i + j2;
                    ATA[c1][c2] += B[j1] * B[j2];
                }
            }
        }
        
        // Solve ATA * P = ATY using Gaussian elimination
        this.controlPoints = solveLinearSystem(ATA, ATY);
    }
    
    public double evaluate(double x) {
        if (x <= minX) x = minX;
        if (x >= maxX) x = maxX;
        
        double t = (x - minX) / (maxX - minX) * numIntervals;
        int i = (int) Math.floor(t);
        if (i >= numIntervals) i = numIntervals - 1;
        if (i < 0) i = 0;
        double u = t - i;
        
        double b0 = (1 - u) * (1 - u) * (1 - u) / 6.0;
        double b1 = (3 * u * u * u - 6 * u * u + 4) / 6.0;
        double b2 = (-3 * u * u * u + 3 * u * u + 3 * u + 1) / 6.0;
        double b3 = u * u * u / 6.0;
        
        return controlPoints[i] * b0 + controlPoints[i+1] * b1 + controlPoints[i+2] * b2 + controlPoints[i+3] * b3;
    }
    
    public double evaluateDerivative(double x) {
        if (x <= minX) x = minX;
        if (x >= maxX) x = maxX;
        
        double t = (x - minX) / (maxX - minX) * numIntervals;
        int i = (int) Math.floor(t);
        if (i >= numIntervals) i = numIntervals - 1;
        if (i < 0) i = 0;
        double u = t - i;
        
        double b0p = -3 * (1 - u) * (1 - u) / 6.0;
        double b1p = (9 * u * u - 12 * u) / 6.0;
        double b2p = (-9 * u * u + 6 * u + 3) / 6.0;
        double b3p = 3 * u * u / 6.0;
        
        double dudx = numIntervals / (maxX - minX);
        
        double dydu = controlPoints[i] * b0p + controlPoints[i+1] * b1p + controlPoints[i+2] * b2p + controlPoints[i+3] * b3p;
        return dydu * dudx;
    }
    
    private static double[] solveLinearSystem(double[][] A, double[] b) {
        int n = b.length;
        double[] x = new double[n];
        
        // Forward elimination
        for (int p = 0; p < n; p++) {
            // find pivot
            int max = p;
            for (int i = p + 1; i < n; i++) {
                if (Math.abs(A[i][p]) > Math.abs(A[max][p])) {
                    max = i;
                }
            }
            
            // swap rows
            double[] temp = A[p]; A[p] = A[max]; A[max] = temp;
            double t = b[p]; b[p] = b[max]; b[max] = t;
            
            if (Math.abs(A[p][p]) <= 1e-10) {
                continue; // Singular matrix, skip to avoid division by zero
            }
            
            // pivot
            for (int i = p + 1; i < n; i++) {
                double alpha = A[i][p] / A[p][p];
                b[i] -= alpha * b[p];
                for (int j = p; j < n; j++) {
                    A[i][j] -= alpha * A[p][j];
                }
            }
        }
        
        // Back substitution
        for (int i = n - 1; i >= 0; i--) {
            double sum = 0.0;
            for (int j = i + 1; j < n; j++) {
                sum += A[i][j] * x[j];
            }
            if (Math.abs(A[i][i]) > 1e-10) {
                x[i] = (b[i] - sum) / A[i][i];
            } else {
                x[i] = 0;
            }
        }
        return x;
    }
}
