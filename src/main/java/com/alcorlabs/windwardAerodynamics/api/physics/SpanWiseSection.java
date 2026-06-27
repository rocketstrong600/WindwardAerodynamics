package com.alcorlabs.windwardAerodynamics.api.physics;

import com.alcorlabs.windwardAerodynamics.WindwardAerodynamics;
import com.alcorlabs.windwardAerodynamics.Config;
import dev.ryanhcode.sable.companion.math.Pose3d;
import dev.ryanhcode.sable.physics.config.dimension_physics.DimensionPhysicsData;
import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3d;
import org.joml.Vector3dc;
import com.alcorlabs.windwardAerodynamics.foils.PolarLiftDragCoef;

public class SpanWiseSection {
    private final BlockPos lead;
    private final BlockState leadState;
    private final int length;
    private final Vec3 chord;
    private final Vec3 normal;
    private final PolarLiftDragCoef foil;


    private final Vector3d AERO_FORCE = new Vector3d();
    private final Vector3d AERO_ANGULAR = new Vector3d();
    private final Vector3d AERO_CENTER = new Vector3d();
    private final Vector3d LIFT_NORMAL = new Vector3d();
    private final Vector3d WING_NORMAL = new Vector3d();
    private final Vector3d SPAN_NORMAL = new Vector3d();
    private final Vector3d CHORD_NORMAL = new Vector3d();

    private final float[] AERO_COEF = new float[3];
    private final float[] PREV_AERO_COEF = new float[3];
    private boolean isFirstTick = true;

    private final Vector3d AERO_CENTER_VELO = new Vector3d();
    private final Vector3d MOMENT_TORQUE = new Vector3d();
    private final Vector3d TEMP = new Vector3d();

    public SpanWiseSection(BlockPos lead, BlockState leadState, int length, Vec3 chord, Vec3 normal, PolarLiftDragCoef foil) {
        this.lead = lead;
        this.leadState = leadState;
        this.length = length;
        this.chord = chord;
        this.normal = normal;
        this.foil = foil;
    }


    void resetVectors() {
        AERO_FORCE.zero();
        AERO_ANGULAR.zero();
        AERO_CENTER.zero();
        LIFT_NORMAL.zero();
        WING_NORMAL.zero();
        SPAN_NORMAL.zero();
        CHORD_NORMAL.zero();

        AERO_CENTER_VELO.zero();
        MOMENT_TORQUE.zero();
        TEMP.zero();
    }

    /**
     * Calculates Lift And Drag for a Span Wise Section.
     *
     * @param subLevel        The sub-level this lift provider is on
     * @param localPose       The pose of the contraption this lift provider is in, if any
     * @param timeStep        The time step between physics ticks
     * @param linearVelocity  The linear velocity of the data in global space
     * @param angularVelocity The angular velocity of the data in global space
     * @param linearImpulse   Mutable vector to sum the linear impulse in local space
     * @param angularImpulse  Mutable vector to sum the angular impulse in local space
     * @param group           The wing group this section belongs to. Must not be null.
     * @param groupIntgWeight if the function will accumulate group totals.
     */
    public void contributeLiftAndDrag(final ServerSubLevel subLevel,
                                                           @Nullable final Pose3d localPose, final double timeStep,
                                                           final Vector3dc linearVelocity, final Vector3dc angularVelocity,
                                                           final Vector3d linearImpulse, final Vector3d angularImpulse,
                                                           @NotNull final SpanWiseGroup group, final double groupIntgWeight) {

        this.resetVectors();

        WING_NORMAL.set(this.normal.x(), this.normal.y(), this.normal.z()); // local space perpendicular to wing
        CHORD_NORMAL.set(this.chord.x(), this.chord.y(), this.chord.z()); // local space parallel to chord line

        CHORD_NORMAL.cross(WING_NORMAL, SPAN_NORMAL).normalize(); // local space parallel to span line

        // Calculate forces and moments around Aerodynamic Center 25% of chord
        AERO_CENTER.set(this.lead.getX() + 0.5, this.lead.getY() + 0.5, this.lead.getZ()+0.5);
        CHORD_NORMAL.mul(-((this.length*0.25)-0.5), TEMP);
        AERO_CENTER.add(TEMP);

        // if pose for contraption transform wing_normal and Lift Pos
        if (localPose != null) {
            localPose.transformNormal(WING_NORMAL);
            localPose.transformNormal(CHORD_NORMAL);
            // Compute SPAN_NORMAL after rotation to prevent any non-orthogonal distortion from localPose
            CHORD_NORMAL.cross(WING_NORMAL, SPAN_NORMAL).normalize();
            // Re-align CHORD_NORMAL to guarantee a perfect orthonormal basis (fixing lost information)
            WING_NORMAL.cross(SPAN_NORMAL, CHORD_NORMAL).normalize();

            localPose.transformPosition(AERO_CENTER);
            //TODO: make sure contraption surfaces still work
        }

        final Pose3d pose = subLevel.logicalPose();

        final Vector3dc localCoM = subLevel.getMassTracker().getCenterOfMass();

        // pressure is similar to real life density at sea_level
        final double pressure = DimensionPhysicsData.getAirPressure(subLevel.getLevel(), pose.transformPosition(AERO_CENTER, TEMP));


        // transform VELO to be the local velocity at the center of the block
        // TEMP = transformed POS
        // VELO = linVel + angVel cross TEMP
        // VELO = inv transformed VELO
        // pose.position is GlobalSpace CoM
        pose.transformPosition(AERO_CENTER, TEMP).sub(pose.position());
        AERO_CENTER_VELO.set(linearVelocity).add(angularVelocity.cross(TEMP, TEMP));
        pose.transformNormalInverse(AERO_CENTER_VELO);

        // Discard Span Wise Flow to use in-plane flow per 2D section theory
        final double spanWise = AERO_CENTER_VELO.dot(SPAN_NORMAL);
        AERO_CENTER_VELO.fma(-spanWise, SPAN_NORMAL);

        if (AERO_CENTER_VELO.lengthSquared() > 1e-12d) {

            // Calculate AoA
            final double chordWise = AERO_CENTER_VELO.dot(CHORD_NORMAL);
            final double wingWise = AERO_CENTER_VELO.dot(WING_NORMAL);

            final double angle_attack = Math.toDegrees(Math.atan2(-wingWise, chordWise));

            this.foil.getCoefficients((float) angle_attack, AERO_COEF);

            // Unsteady Aerodynamics (Wake Lag / Dynamic Stall)
            // The physical time constant for flow attachment/detachment is roughly proportional to
            // the time it takes for the airflow to travel across the chord length.
            // tau = k * (chord / V) where k is an empirical constant.
            final double vMag = Math.max(AERO_CENTER_VELO.length(), 0.1); // clamp V to prevent infinite tau
            final double tau = 0.75 * this.length / vMag; // Reduced from 2.0 to weaken the effect
            
            // Exponential decay: weight of old state is e^(-dt/tau)
            final float oldWeight = (float) Math.exp(-timeStep / tau);
            final float newWeight = 1.0f - oldWeight;

            if (this.isFirstTick) {
                // Initialize coefficients perfectly on the first tick so it doesn't slowly spool up from 0
                if (groupIntgWeight > 0.0) {
                    this.PREV_AERO_COEF[0] = AERO_COEF[0];
                    this.PREV_AERO_COEF[1] = AERO_COEF[1];
                    this.PREV_AERO_COEF[2] = AERO_COEF[2];
                    this.isFirstTick = false;
                }
            } else {
                AERO_COEF[0] = AERO_COEF[0] * newWeight + this.PREV_AERO_COEF[0] * oldWeight;
                AERO_COEF[1] = AERO_COEF[1] * newWeight + this.PREV_AERO_COEF[1] * oldWeight;
                AERO_COEF[2] = AERO_COEF[2] * newWeight + this.PREV_AERO_COEF[2] * oldWeight;
                
                // Only save state during the base force evaluation (weight > 0), NOT during Jacobian "what-if" perturbations
                if (groupIntgWeight > 0.0) {
                    this.PREV_AERO_COEF[0] = AERO_COEF[0];
                    this.PREV_AERO_COEF[1] = AERO_COEF[1];
                    this.PREV_AERO_COEF[2] = AERO_COEF[2];
                }
            }


            // Lift direction: perpendicular to the in-plane local flow
            LIFT_NORMAL.set(AERO_CENTER_VELO.negate(TEMP).normalize(TEMP).cross(SPAN_NORMAL, TEMP)).normalize();
            
            // If flying backwards, the relative wind comes from the trailing edge.
            // The cross product will point downwards, so we invert it to keep lift pointing "UP" relative to the wing.
            if (chordWise < 0) {
                LIFT_NORMAL.negate();
            }

            // q = 1/2 * rho * V^2
            final double scalingFactor = Config.AERO_FORCE_MUL.get(); // Scaling Factor to translate real-life Performance to Sable's low block weights.
            final double dynamicPressure = (0.5d * pressure * AERO_CENTER_VELO.lengthSquared()) * scalingFactor;

            // Drag force
            // CID = CL * CL / PI * AR * Eff
            // Higher efficiency coefficient than what you would see in real-life to tune typical contraption flying speeds
            final double indDragCoef = (AERO_COEF[0]*AERO_COEF[0])/(Math.PI * group.aspectRatio() * 0.98);
            final double dragMag = dynamicPressure * this.length * (AERO_COEF[1]+indDragCoef);

            AERO_CENTER_VELO.negate(TEMP).normalize();
            TEMP.mul(dragMag);
            AERO_FORCE.add(TEMP);

            if (groupIntgWeight > 0.0) {
                group.totalDrag.add(TEMP);
                group.dragCenter.fma(Math.abs(dragMag) * groupIntgWeight, AERO_CENTER);
                group.totalDragStrength += Math.abs(dragMag) * groupIntgWeight;
            }

            // Lift force; skipped when there is no in-plane flow (no flow, no lift).
            // Drag and moment still apply.
            if (LIFT_NORMAL.lengthSquared() > 1e-12d) {
                LIFT_NORMAL.normalize();

                final double liftMag = dynamicPressure * this.length * AERO_COEF[0];
                LIFT_NORMAL.mul(liftMag, TEMP);
                AERO_FORCE.add(TEMP);

                if (groupIntgWeight > 0.0) {
                    group.totalLift.add(TEMP);
                    group.liftCenter.fma(Math.abs(liftMag) * groupIntgWeight, AERO_CENTER);
                    group.totalLiftStrength += Math.abs(liftMag) * groupIntgWeight;
                }
            }

            final double momentMag = dynamicPressure * this.length * this.length * AERO_COEF[2];
            MOMENT_TORQUE.fma(momentMag, SPAN_NORMAL);
        }

        // linearImpulse, angularImpulse and COM are all local space

        //Offset Aerodynamic Force from CoM
        AERO_FORCE.mul(timeStep);
        MOMENT_TORQUE.mul(timeStep);
        AERO_CENTER.sub(localCoM, TEMP);
        AERO_ANGULAR.set(TEMP.cross(AERO_FORCE)).add(MOMENT_TORQUE);

        // Last-resort fuse: never let a non-finite impulse escape into the physics engine
        if (AERO_FORCE.isFinite() && AERO_ANGULAR.isFinite()) {
            linearImpulse.add(AERO_FORCE);
            angularImpulse.add(AERO_ANGULAR);
        } else {
            WindwardAerodynamics.LOGGER.warn("Discarded non-finite aero impulse at section " + this.lead);
        }
    }

    public BlockPos getLead() {return this.lead;}
    public BlockState getLeadState() {return this.leadState;}
    public int getLength() {return this.length;}
    public Vec3 getChord() {return this.chord;}
    public Vec3 getNormal() {return this.normal;}
    public PolarLiftDragCoef getFoil() {return this.foil;}
}
