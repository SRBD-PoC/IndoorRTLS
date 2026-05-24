package com.example.indoorrtls.algorithms;

import org.apache.commons.math3.linear.Array2DRowRealMatrix;
import org.apache.commons.math3.linear.EigenDecomposition;
import org.apache.commons.math3.linear.RealMatrix;

import java.util.Random;

public class MdsEngine {

    private static final Random random = new Random();

    /**
     * Classical Multidimensional Scaling (MDS).
     * Maps nodes to 2D coordinates based on a distance matrix.
     */
    public static double[][] calculateClassicalMDS(double[][] distanceMatrix) {
        int n = distanceMatrix.length;
        if (n < 1) return new double[0][2];
        if (n == 1) return new double[][]{{0, 0}};
        if (n == 2) {
            double d = distanceMatrix[0][1];
            return new double[][]{{0, 0}, {d, 0}};
        }

        // 1. Compute squared distance matrix D2
        double[][] d2 = new double[n][n];
        for (int i = 0; i < n; i++) {
            for (int j = 0; j < n; j++) {
                d2[i][j] = distanceMatrix[i][j] * distanceMatrix[i][j];
            }
        }

        // 2. Double centering
        double[][] b = new double[n][n];
        double[] rowMeans = new double[n];
        double totalMean = 0;

        for (int i = 0; i < n; i++) {
            for (int j = 0; j < n; j++) {
                rowMeans[i] += d2[i][j];
                totalMean += d2[i][j];
            }
        }

        for (int i = 0; i < n; i++) rowMeans[i] /= n;
        totalMean /= (n * n);

        for (int i = 0; i < n; i++) {
            for (int j = 0; j < n; j++) {
                b[i][j] = -0.5 * (d2[i][j] - rowMeans[i] - rowMeans[j] + totalMean);
            }
        }

        // 3. Eigenvalue Decomposition
        RealMatrix matrixB = new Array2DRowRealMatrix(b);
        EigenDecomposition ed = new EigenDecomposition(matrixB);

        double[] eigenvalues = ed.getRealEigenvalues();
        double[][] coordinates = new double[n][2];

        // We use the two largest positive eigenvalues
        // Commons Math EigenDecomposition sorts them in descending order
        double ev1 = Math.max(0.001, eigenvalues[0]);
        double ev2 = Math.max(0.001, eigenvalues[1]);

        for (int i = 0; i < n; i++) {
            coordinates[i][0] = ed.getEigenvector(0).getEntry(i) * Math.sqrt(ev1);
            coordinates[i][1] = ed.getEigenvector(1).getEntry(i) * Math.sqrt(ev2);
            
            // If the second dimension is collapsed (collinear), add a tiny jitter
            // This helps Iterative MDS break out into 2D if needed.
            if (ev2 <= 0.001) {
                coordinates[i][1] += (random.nextDouble() - 0.5) * 0.01;
            }
        }

        return coordinates;
    }

    /**
     * SMACOF (Scaling by Majorizing a Complicated Function) - Iterative MDS refinement.
     * 
     * @param d Target distance matrix
     * @param x Initial positions (n x 2)
     * @param iterations Number of iterations
     * @return Refined positions
     */
    public static double[][] calculateIterativeMDS(double[][] d, double[][] x, int iterations) {
        int n = d.length;
        if (n < 2) return x;
        
        double[][] xCurr = x;

        for (int iter = 0; iter < iterations; iter++) {
            double[][] xNext = new double[n][2];
            for (int i = 0; i < n; i++) {
                double sumX = 0;
                double sumY = 0;
                for (int j = 0; j < n; j++) {
                    if (i == j) continue;

                    double dx = xCurr[i][0] - xCurr[j][0];
                    double dy = xCurr[i][1] - xCurr[j][1];
                    double distActual = Math.sqrt(dx * dx + dy * dy);
                    
                    if (distActual < 1e-6) distActual = 1e-6;

                    // Guttman Transform update step
                    double ratio = d[i][j] / distActual;
                    sumX += xCurr[j][0] + ratio * (xCurr[i][0] - xCurr[j][0]);
                    sumY += xCurr[j][1] + ratio * (xCurr[i][1] - xCurr[j][1]);
                }
                xNext[i][0] = sumX / n;
                xNext[i][1] = sumY / n;
            }
            xCurr = xNext;
        }
        return xCurr;
    }
}
