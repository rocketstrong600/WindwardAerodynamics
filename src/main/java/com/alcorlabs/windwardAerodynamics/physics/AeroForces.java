package com.alcorlabs.windwardAerodynamics.physics;

import com.alcorlabs.windwardAerodynamics.api.physics.SpanWiseGroup;
import com.alcorlabs.windwardAerodynamics.api.physics.SpanWiseSection;
import dev.ryanhcode.sable.api.physics.force.ForceGroups;
import dev.ryanhcode.sable.companion.math.Pose3d;
import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix3d;
import org.joml.Vector3d;
import org.joml.Vector3dc;

public class AeroForces {

    /**
     * Calculates All Aerodynamic forces for a group of wings. <br/>
     * @param serverSubLevel  an instance of the ServerSublLevel
     * @param groups          an iterable collection of wing groups
     * @param localPose       The pose of the contraption this lift provider is in, if any
     * @param timeStep        The time step between physics ticks
     * @param linearVelocity  The linear velocity of the data in global space
     * @param angularVelocity The angular velocity of the data in global space
     * @param linearImpulse   Mutable vector to sum the linear impulse in local space
     * @param angularImpulse  Mutable vector to sum the angular impulse in local space
     * @param groupIntgWeight if the function will accumulate group totals.
     */
    public void calculateAeroForces(final ServerSubLevel serverSubLevel, final Iterable<? extends SpanWiseGroup> groups, @Nullable final Pose3d localPose, final double timeStep, final Vector3dc linearVelocity, final Vector3dc angularVelocity, final Vector3d linearImpulse, final Vector3d angularImpulse, final double groupIntgWeight) {
        for (final SpanWiseGroup spanWiseGroup: groups) {

            for (final SpanWiseSection spanWiseSection: spanWiseGroup.sections() ) {
                spanWiseSection.contributeLiftAndDrag(serverSubLevel, localPose, timeStep, linearVelocity, angularVelocity, linearImpulse, angularImpulse, spanWiseGroup, groupIntgWeight);
            }
        }
    }

    private final Vector3d rkK1Linear = new Vector3d();
    private final Vector3d rkK1Angular = new Vector3d();
    private final Vector3d rkK2Linear = new Vector3d();
    private final Vector3d rkK2Angular = new Vector3d();
    private final Vector3d rkK3Linear = new Vector3d();
    private final Vector3d rkK3Angular = new Vector3d();
    private final Vector3d rkMidVelocity = new Vector3d();
    private final Vector3d rkMidAngularVelocity = new Vector3d();
    private final Vector3d rkEndVelocity = new Vector3d();
    private final Vector3d rkEndAngularVelocity = new Vector3d();
    private final Vector3d rkTemp1 = new Vector3d();
    private final Vector3d rkTemp2 = new Vector3d();
    private final Matrix3d rkTempMatrix = new Matrix3d();
    private final Vector3d projOmega = new Vector3d();

    /**
     * Calculates All Aerodynamic forces for a group of wings. With RK3 Predictor Corrector<br/>
     * @param serverSubLevel  an instance of the ServerSublLevel
     * @param groups          an iterable collection of wing groups
     * @param localPose       The pose of the contraption this lift provider is in, if any
     * @param timeStep        The time step between physics ticks
     * @param linearVelocity  The linear velocity of the data in global space
     * @param angularVelocity The angular velocity of the data in global space
     * @param linearImpulse   Mutable vector to sum the linear impulse in local space
     * @param angularImpulse  Mutable vector to sum the angular impulse in local space
     */
    public void integrateAeroForces(final ServerSubLevel serverSubLevel, @NotNull final Iterable<? extends SpanWiseGroup> groups, @Nullable final Pose3d localPose, final double timeStep, final Vector3dc linearVelocity, final Vector3dc angularVelocity, final Vector3d linearImpulse, final Vector3d angularImpulse) {

        for (final SpanWiseGroup wingGroup : groups) {
            wingGroup.resetTotals();
        }

        final double mass = serverSubLevel.getMassTracker().getMass();
        final Matrix3d invLocalInertialTensor = serverSubLevel.getMassTracker().getInertiaTensor().invert(this.rkTempMatrix);

        // Max Angular velocity caused by Aerodynamics
        final double maxAnglularVelocity = 18;

        // Reset our pre-allocated intermediate impulse vectors
        this.rkK1Linear.zero(); this.rkK1Angular.zero();
        this.rkK2Linear.zero(); this.rkK2Angular.zero();
        this.rkK3Linear.zero(); this.rkK3Angular.zero();

        final double weight1 = 1.0 / 6.0;
        final double weight2 = 4.0 / 6.0;
        final double weight3 = 1.0 / 6.0;

        // PASS 1 (k1)
        this.calculateAeroForces(serverSubLevel, groups, localPose, timeStep, linearVelocity, angularVelocity, this.rkK1Linear, this.rkK1Angular, weight1);

        // Predict Midpoint Velocity and AngularVelocity (using 0.5 * k1)
        serverSubLevel.logicalPose().transformNormal(this.rkK1Linear, this.rkTemp1);
        this.rkTemp1.mul(0.5 / mass);
        linearVelocity.add(this.rkTemp1, this.rkMidVelocity);

        invLocalInertialTensor.transform(this.rkK1Angular, this.rkTemp1);
        serverSubLevel.logicalPose().transformNormal(this.rkTemp1);
        this.rkTemp1.mul(0.5);
        angularVelocity.add(this.rkTemp1, this.rkMidAngularVelocity);


        // PASS 2 (k2)
        this.calculateAeroForces(serverSubLevel, groups, localPose, timeStep, this.rkMidVelocity, this.rkMidAngularVelocity, this.rkK2Linear, this.rkK2Angular, weight2);

        // Predict Endpoint Velocity and AngularVelocity (using -k1 + 2*k2)
        this.rkK2Linear.mul(2.0, this.rkTemp1).sub(this.rkK1Linear); // temp1 = (k2 * 2) - k1
        serverSubLevel.logicalPose().transformNormal(this.rkTemp1);
        this.rkTemp1.div(mass);
        linearVelocity.add(this.rkTemp1, this.rkEndVelocity);

        this.rkK2Angular.mul(2.0, this.rkTemp1).sub(this.rkK1Angular);
        invLocalInertialTensor.transform(this.rkTemp1, this.rkTemp2);
        serverSubLevel.logicalPose().transformNormal(this.rkTemp2);
        angularVelocity.add(this.rkTemp2, this.rkEndAngularVelocity);


        // PASS 3 (k3)
        this.calculateAeroForces(serverSubLevel, groups, localPose, timeStep, this.rkEndVelocity, this.rkEndAngularVelocity, this.rkK3Linear, this.rkK3Angular, weight3);


        // FINAL INTEGRATION
        // weightedSum = (1/6)*k1 + (4/6)*k2 + (1/6)*k3

        // add final impulse
        linearImpulse.fma(weight1, this.rkK1Linear);
        linearImpulse.fma(weight2, this.rkK2Linear);
        linearImpulse.fma(weight3, this.rkK3Linear);

        angularImpulse.fma(weight1, this.rkK1Angular);
        angularImpulse.fma(weight2, this.rkK2Angular);
        angularImpulse.fma(weight3, this.rkK3Angular);

        // ANGULAR STIFFNESS CLAMP

        // Get the current Global angular velocity in LOCAL space
        serverSubLevel.logicalPose().transformNormalInverse(angularVelocity, this.rkTemp2);
        // Calculate the proposed change in LOCAL angular velocity (Delta Omega)
        invLocalInertialTensor.transform(angularImpulse, this.rkTemp1);

        // Projected Change in Angular Velocity
        this.rkTemp2.add(this.rkTemp1, this.projOmega);

        if (this.projOmega.length() > maxAnglularVelocity) {
            this.projOmega.normalize().mul(maxAnglularVelocity);
            this.rkTemp1.set(this.projOmega).sub(this.rkTemp2);
        }


        // Clamp damping overshoot uniformly.
        // If the change in velocity opposes the current rotation AND exceeds its magnitude,
        // the RK solver has overshot. We scale the change down to exactly stop the rotation.
        final double dotOmega = this.rkTemp2.dot(this.rkTemp1);
        if (dotOmega < 0) {
            final double omegaSq = this.rkTemp2.lengthSquared();
            if (-dotOmega > omegaSq) {
                final double t = omegaSq / -dotOmega;
                this.rkTemp1.mul(t);
            }
        }

        // Convert clamped Delta Omega back into the final Angular Impulse
        serverSubLevel.getMassTracker().getInertiaTensor().transform(this.rkTemp1, angularImpulse);



        // LINEAR STIFFNESS CLAMP

        // Get the current Global linear velocity in LOCAL space
        serverSubLevel.logicalPose().transformNormalInverse(linearVelocity, this.rkTemp2);

        // Calculate the proposed change in LOCAL linear velocity (Delta V)
        // deltaV = linearImpulse / mass
        this.rkTemp1.set(linearImpulse).div(mass);

        // Clamp linear overshoot uniformly.
        // If the drag/lift force opposes movement and exceeds current speed, scale it to exactly stop the plane.
        final double dotV = this.rkTemp2.dot(this.rkTemp1);
        if (dotV < 0) {
            final double vSq = this.rkTemp2.lengthSquared();
            if (-dotV > vSq) {
                final double t = vSq / -dotV;
                this.rkTemp1.mul(t);
            }
        }

        // Convert the clamped Delta V back into the final Linear Impulse
        // impulse = clamped Delta V * mass
        linearImpulse.set(this.rkTemp1).mul(mass);

        for (final SpanWiseGroup wingGroup : groups) {
            if (wingGroup.totalLift().lengthSquared() >= 0.001 * 0.001)
                serverSubLevel.getOrCreateQueuedForceGroup(ForceGroups.LIFT.get())
                        .recordPointForce(wingGroup.liftCenter().div(wingGroup.totalLiftStrength), wingGroup.totalLift());

            if (wingGroup.totalDrag().lengthSquared() >= 0.001 * 0.001)
                serverSubLevel.getOrCreateQueuedForceGroup(ForceGroups.DRAG.get())
                        .recordPointForce(wingGroup.dragCenter().div(wingGroup.totalDragStrength), wingGroup.totalDrag());
        }
    }
}
