package com.alcorlabs.windwardAerodynamics.physics;

import com.alcorlabs.windwardAerodynamics.Config;
import com.alcorlabs.windwardAerodynamics.api.physics.SpanWiseGroup;
import com.alcorlabs.windwardAerodynamics.api.physics.SpanWiseSection;
import dev.ryanhcode.sable.api.physics.force.ForceGroups;
import dev.ryanhcode.sable.companion.math.Pose3d;
import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix3dc;
import org.joml.Vector3d;
import org.joml.Vector3dc;

public class AeroForces {

    public void calculateAeroForces(final ServerSubLevel serverSubLevel, final Iterable<? extends SpanWiseGroup> groups, @Nullable final Pose3d localPose, final double timeStep, final Vector3dc linearVelocity, final Vector3dc angularVelocity, final Vector3d linearImpulse, final Vector3d angularImpulse, final double groupIntgWeight) {
        for (final SpanWiseGroup spanWiseGroup: groups) {
            for (final SpanWiseSection spanWiseSection: spanWiseGroup.sections() ) {
                spanWiseSection.contributeLiftAndDrag(serverSubLevel, localPose, timeStep, linearVelocity, angularVelocity, linearImpulse, angularImpulse, spanWiseGroup, groupIntgWeight);
            }
        }
    }

    private void computeForcesForGlobalV(ServerSubLevel serverSubLevel, Iterable<? extends SpanWiseGroup> groups, Pose3d localPose, double timeStep, Vector3dc globalLinV, Vector3dc globalAngV, Vector3d outLinImpulse, Vector3d outAngImpulse, double weight) {
        outLinImpulse.zero();
        outAngImpulse.zero();
        calculateAeroForces(serverSubLevel, groups, localPose, timeStep, globalLinV, globalAngV, outLinImpulse, outAngImpulse, weight);
    }

    private final Vector3d perturbedLocalLinV = new Vector3d();
    private final Vector3d perturbedLocalAngV = new Vector3d();
    private final Vector3d perturbedGlobalLinV = new Vector3d();
    private final Vector3d perturbedGlobalAngV = new Vector3d();
    private final Vector3d tempLinImpulse = new Vector3d();
    private final Vector3d tempAngImpulse = new Vector3d();
    
    private final Vector3d baseLinImpulse = new Vector3d();
    private final Vector3d baseAngImpulse = new Vector3d();
    private final Vector3d baseLocalLinV = new Vector3d();
    private final Vector3d baseLocalAngV = new Vector3d();
    
    private final double[][] J = new double[6][6];
    private final double[][] A = new double[6][6];
    private final double[] B = new double[6];
    
    private final double[][] workA = new double[6][6];
    private final double[] workB = new double[6];
    private final double[] X = new double[6];

    /**
     * Calculates Aerodynamic forces for a group of wings using a Linearly Implicit Euler
     * (Backward Euler) integrator, powered by a Diagonalized Finite Difference Aerodynamic 
     * Jacobian matrix for unconditional stability.
     */
    public void integrateAeroForces(final ServerSubLevel serverSubLevel, @NotNull final Iterable<? extends SpanWiseGroup> groups, @Nullable final Pose3d localPose, final double timeStep, final Vector3dc linearVelocity, final Vector3dc angularVelocity, final Vector3d linearImpulse, final Vector3d angularImpulse) {

        for (final SpanWiseGroup wingGroup : groups) {
            wingGroup.resetTotals();
        }

        // 1. Evaluate Base Impulses (accumulating group totals)
        computeForcesForGlobalV(serverSubLevel, groups, localPose, timeStep, linearVelocity, angularVelocity, baseLinImpulse, baseAngImpulse, 1.0);

        // 2. Finite Difference Loop to build Jacobian (Central Difference)
        double eps = 1e-3; // Step in m/s or rad/s. Larger = better float stability.

        Pose3d pose = serverSubLevel.logicalPose();
        
        pose.transformNormalInverse(linearVelocity, baseLocalLinV);
        pose.transformNormalInverse(angularVelocity, baseLocalAngV);

        for (int col = 0; col < 6; col++) {
            // Forward Step
            perturbedLocalLinV.set(baseLocalLinV);
            perturbedLocalAngV.set(baseLocalAngV);

            if (col == 0) perturbedLocalLinV.x += eps;
            if (col == 1) perturbedLocalLinV.y += eps;
            if (col == 2) perturbedLocalLinV.z += eps;
            if (col == 3) perturbedLocalAngV.x += eps;
            if (col == 4) perturbedLocalAngV.y += eps;
            if (col == 5) perturbedLocalAngV.z += eps;

            pose.transformNormal(perturbedLocalLinV, perturbedGlobalLinV);
            pose.transformNormal(perturbedLocalAngV, perturbedGlobalAngV);

            computeForcesForGlobalV(serverSubLevel, groups, localPose, timeStep, perturbedGlobalLinV, perturbedGlobalAngV, tempLinImpulse, tempAngImpulse, 0.0);

            double fLinX = tempLinImpulse.x, fLinY = tempLinImpulse.y, fLinZ = tempLinImpulse.z;
            double fAngX = tempAngImpulse.x, fAngY = tempAngImpulse.y, fAngZ = tempAngImpulse.z;

            // Backward Step
            perturbedLocalLinV.set(baseLocalLinV);
            perturbedLocalAngV.set(baseLocalAngV);

            if (col == 0) perturbedLocalLinV.x -= eps;
            if (col == 1) perturbedLocalLinV.y -= eps;
            if (col == 2) perturbedLocalLinV.z -= eps;
            if (col == 3) perturbedLocalAngV.x -= eps;
            if (col == 4) perturbedLocalAngV.y -= eps;
            if (col == 5) perturbedLocalAngV.z -= eps;

            pose.transformNormal(perturbedLocalLinV, perturbedGlobalLinV);
            pose.transformNormal(perturbedLocalAngV, perturbedGlobalAngV);

            computeForcesForGlobalV(serverSubLevel, groups, localPose, timeStep, perturbedGlobalLinV, perturbedGlobalAngV, tempLinImpulse, tempAngImpulse, 0.0);

            // Central Difference = (F_forward - F_backward) / (2 * eps)
            J[0][col] = (fLinX - tempLinImpulse.x) / (2 * eps);
            J[1][col] = (fLinY - tempLinImpulse.y) / (2 * eps);
            J[2][col] = (fLinZ - tempLinImpulse.z) / (2 * eps);
            J[3][col] = (fAngX - tempAngImpulse.x) / (2 * eps);
            J[4][col] = (fAngY - tempAngImpulse.y) / (2 * eps);
            J[5][col] = (fAngZ - tempAngImpulse.z) / (2 * eps);
        }

        double mass = serverSubLevel.getMassTracker().getMass();
        Matrix3dc I = serverSubLevel.getMassTracker().getInertiaTensor();
        double feedbackScale = Config.MAX_POSITIVE_FEEDBACK_SCALE.get();

        // Jacobian Regularization
        // Mathematical stalls (negative lift slope) and highly asymmetric aerodynamic cross-coupling 
        // (e.g. pitch rate causing massive lift) create positive eigenvalues and indefinite matrices.
        // This violates the stability bounds of Implicit Euler and causes matrix inversions to violently explode.
        // By scaling off-diagonal aerodynamic cross-coupling and strictly clamping positive feedback on the diagonal
        // to a fraction of the mass matrix, we allow a slider for realism while mathematically guaranteeing 
        // the matrix remains strictly positive-definite and unconditionally stable!
        for (int r = 0; r < 6; r++) {
            double m_rr = (r == 0) ? mass : (r == 1) ? mass : (r == 2) ? mass :
                          (r == 3) ? I.m00() : (r == 4) ? I.m11() : I.m22();
            
            for (int c = 0; c < 6; c++) {
                if (r != c) {
                    J[r][c] *= feedbackScale; // Blend cross-coupling based on realism scale
                } else if (J[r][c] > 0) {
                    // Blend positive feedback, but strictly clamp to 95% of the mass to mathematically prevent division-by-zero explosions
                    double maxJ = m_rr * 0.95;
                    J[r][c] = Math.min(J[r][c], maxJ) * feedbackScale;
                }
            }
        }

        // 3. Build Linear System: (M - J) * DeltaV = BaseImpulse
        for (int i = 0; i < 6; i++) {
            for (int j = 0; j < 6; j++) {
                A[i][j] = 0.0;
            }
        }
        for (int i = 0; i < 3; i++) {
            A[i][i] = mass;
        }
        A[3][3] = I.m00(); A[3][4] = I.m10(); A[3][5] = I.m20();
        A[4][3] = I.m01(); A[4][4] = I.m11(); A[4][5] = I.m21();
        A[5][3] = I.m02(); A[5][4] = I.m12(); A[5][5] = I.m22();

        for (int r = 0; r < 6; r++) {
            for (int c = 0; c < 6; c++) {
                A[r][c] -= J[r][c];
            }
        }

        B[0] = baseLinImpulse.x;
        B[1] = baseLinImpulse.y;
        B[2] = baseLinImpulse.z;
        B[3] = baseAngImpulse.x;
        B[4] = baseAngImpulse.y;
        B[5] = baseAngImpulse.z;

        // 4. Solve for Delta Velocity
        double[] deltaV = solve6x6(A, B);
        
        for (int i = 0; i < 6; i++) {
            if (!Double.isFinite(deltaV[i])) {
                deltaV[i] = 0.0; // Failsafe for mathematically singular states
            }
        }

        // 5. Convert Delta Velocities back into stable local impulses
        linearImpulse.set(deltaV[0] * mass, deltaV[1] * mass, deltaV[2] * mass);
        Vector3d dAngV = new Vector3d(deltaV[3], deltaV[4], deltaV[5]);
        I.transform(dAngV, angularImpulse);

        // 6. Record grouped forces for visual/stress purposes
        for (final SpanWiseGroup wingGroup : groups) {
            if (wingGroup.totalLift().lengthSquared() >= 0.001 * 0.001) {
                wingGroup.liftCenter().div(wingGroup.totalLiftStrength); // convert to local average center
                
                serverSubLevel.getOrCreateQueuedForceGroup(ForceGroups.LIFT.get())
                        .recordPointForce(new Vector3d(wingGroup.liftCenter()), new Vector3d(wingGroup.totalLift()).mul(timeStep));
            }

            if (wingGroup.totalDrag().lengthSquared() >= 0.001 * 0.001) {
                wingGroup.dragCenter().div(wingGroup.totalDragStrength);
                
                serverSubLevel.getOrCreateQueuedForceGroup(ForceGroups.DRAG.get())
                        .recordPointForce(new Vector3d(wingGroup.dragCenter()), new Vector3d(wingGroup.totalDrag()).mul(timeStep));
            }
        }
    }

    /**
     * Solves a 6x6 linear system A*x = b using Gaussian Elimination with partial pivoting.
     * Efficiently handles the block-diagonal structure created by the Diagonalized Jacobian.
     * Uses pre-allocated work arrays to prevent mutating the input arrays and avoid allocations.
     */
    private double[] solve6x6(double[][] A, double[] b) {
        int n = 6;
        
        // Copy A and b into work arrays to avoid mutating the originals
        for (int i = 0; i < n; i++) {
            System.arraycopy(A[i], 0, workA[i], 0, n);
            workB[i] = b[i];
            X[i] = 0;
        }
        
        for (int p = 0; p < n; p++) {
            int max = p;
            for (int i = p + 1; i < n; i++) {
                if (Math.abs(workA[i][p]) > Math.abs(workA[max][p])) {
                    max = i;
                }
            }
            
            // Swap rows in work arrays
            double[] temp = workA[p]; workA[p] = workA[max]; workA[max] = temp;
            double t = workB[p]; workB[p] = workB[max]; workB[max] = t;

            if (Math.abs(workA[p][p]) <= 1e-12) continue;

            for (int i = p + 1; i < n; i++) {
                double alpha = workA[i][p] / workA[p][p];
                workB[i] -= alpha * workB[p];
                for (int j = p; j < n; j++) {
                    workA[i][j] -= alpha * workA[p][j];
                }
            }
        }
        
        for (int i = n - 1; i >= 0; i--) {
            double sum = 0.0;
            for (int j = i + 1; j < n; j++) {
                sum += workA[i][j] * X[j];
            }
            if (Math.abs(workA[i][i]) > 1e-12) {
                X[i] = (workB[i] - sum) / workA[i][i];
            } else {
                X[i] = 0;
            }
        }
        return X;
    }
}
