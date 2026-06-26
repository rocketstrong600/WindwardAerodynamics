package com.alcorlabs.windwardAerodynamics.mixin.sable;

import com.alcorlabs.windwardAerodynamics.api.block.BlockSubLevelAdvLiftProvider;
import com.alcorlabs.windwardAerodynamics.api.mixinInterfaces.IAdvLiftContraption;
import com.alcorlabs.windwardAerodynamics.api.mixinInterfaces.IAdvLiftPlot;
import com.alcorlabs.windwardAerodynamics.api.physics.SpanWiseGroup;
import com.alcorlabs.windwardAerodynamics.physics.AeroForces;
import com.alcorlabs.windwardAerodynamics.api.mixinInterfaces.IAdvLiftSubLevel;
import com.llamalad7.mixinextras.sugar.Local;
import dev.ryanhcode.sable.api.physics.handle.RigidBodyHandle;
import dev.ryanhcode.sable.physics.floating_block.FloatingBlockController;
import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import dev.ryanhcode.sable.sublevel.plot.ServerLevelPlot;
import dev.ryanhcode.sable.sublevel.system.SubLevelPhysicsSystem;
import net.minecraft.core.BlockPos;
import org.joml.Vector3d;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.ArrayList;
import java.util.List;

@Mixin(ServerSubLevel.class)
public abstract class ServerSubLevelMixin implements IAdvLiftSubLevel {

    @Unique
    private final AeroForces windwardAerodynamics$aeroForces = new AeroForces();

    @Unique
    private List<SpanWiseGroup> windwardAerodynamics$cachedSpanGroups = new ArrayList<>();
    
    @Unique
    private boolean windwardAerodynamics$spanGroupsDirty = true;

    // Mark groups dirty when Sable does
    @Override
    public void windwardAerodynamics$markSpanGroupsDirty() {
        this.windwardAerodynamics$spanGroupsDirty = true;
    }

    @Redirect(
        method = "prePhysicsTick",
        at = @At(
            value = "INVOKE",
            target = "Ldev/ryanhcode/sable/api/sublevel/KinematicContraption;sable$liftProviders()Ljava/util/Map;"
        ),
        remap = false
    )
    private java.util.Map<BlockPos, dev.ryanhcode.sable.api.block.BlockSubLevelLiftProvider.LiftProviderContext> windwardAerodynamics$filterContraptionProviders(dev.ryanhcode.sable.api.sublevel.KinematicContraption instance) {
        java.util.Map<BlockPos, dev.ryanhcode.sable.api.block.BlockSubLevelLiftProvider.LiftProviderContext> providers = instance.sable$liftProviders();
        
        // Remove advanced providers from Sable's list so they are ignored by arcady aerodynamics!
        providers.values().removeIf(ctx -> ctx.state().getBlock() instanceof BlockSubLevelAdvLiftProvider);
        
        return providers;
    }

    @Redirect(
            method = "prePhysicsTick",
            at = @At(
                value = "INVOKE",
                target = "Ldev/ryanhcode/sable/physics/floating_block/FloatingBlockController;needsTicking()Z"
            ),
            remap = false
    )
    private boolean windwardAerodynamics$forcePhysicsTick(FloatingBlockController instance, @Local(name = "plot") ServerLevelPlot plot) {
        return instance.needsTicking() || !((IAdvLiftPlot) plot).windwardAerodynamics$getAdvLiftProviders().isEmpty();
    }

    @Inject(method = "prePhysicsTick", at = {
            // Target the TRUE path of the ternary
            @At(value = "INVOKE", target = "Ldev/ryanhcode/sable/api/block/BlockSubLevelLiftProvider;groupLiftProviders(Ljava/util/Collection;)Ljava/util/List;", ordinal = 0),
            // Target the FALSE path of the ternary
            @At(value = "INVOKE", target = "Ljava/util/List;of()Ljava/util/List;", ordinal = 0)
    }, remap = false)
    private void windwardAerodynamics$addAeroForces(SubLevelPhysicsSystem physicsSystem, RigidBodyHandle handle, double timeStep, CallbackInfo ci,
                                                    @Local(name = "linearVelocity") final Vector3d linearVelocity,
                                                    @Local(name = "angularVelocity") final Vector3d angularVelocity,
                                                    @Local(name = "linearImpulse") final Vector3d linearImpulse,
                                                    @Local(name = "angularImpulse") final Vector3d angularImpulse,
                                                    @Local(name = "plot") ServerLevelPlot plot
                                                    ) {
        ServerSubLevel self = (ServerSubLevel) (Object) this;

        if (this.windwardAerodynamics$spanGroupsDirty) {
            this.windwardAerodynamics$spanGroupsDirty = false;
            this.windwardAerodynamics$cachedSpanGroups = SpanWiseGroup.groupSpanSections(((IAdvLiftPlot) plot).windwardAerodynamics$getAdvLiftProviders().values());
        }

        if (!this.windwardAerodynamics$cachedSpanGroups.isEmpty()) {
            windwardAerodynamics$aeroForces.integrateAeroForces(self, this.windwardAerodynamics$cachedSpanGroups, null, timeStep, linearVelocity, angularVelocity, linearImpulse, angularImpulse);
        }
    }

    @Inject(method = "prePhysicsTick", at = {
            // Target the TRUE path of the contraption ternary
            @At(value = "INVOKE", target = "Ldev/ryanhcode/sable/api/block/BlockSubLevelLiftProvider;groupLiftProviders(Ljava/util/Collection;)Ljava/util/List;", ordinal = 1),
            // Target the FALSE path of the contraption ternary
            @At(value = "INVOKE", target = "Ljava/util/List;of()Ljava/util/List;", ordinal = 1)
    }, remap = false)
    private void windwardAerodynamics$addContraptionAeroForces(SubLevelPhysicsSystem physicsSystem, RigidBodyHandle handle, double timeStep, CallbackInfo ci,
                                                    @Local(name = "linearVelocity") final Vector3d linearVelocity,
                                                    @Local(name = "angularVelocity") final Vector3d angularVelocity,
                                                    @Local(name = "linearImpulse") final Vector3d linearImpulse,
                                                    @Local(name = "angularImpulse") final Vector3d angularImpulse,
                                                    @Local(name = "localContraptionPose") dev.ryanhcode.sable.companion.math.Pose3d localContraptionPose,
                                                    @Local(name = "contraption") dev.ryanhcode.sable.api.sublevel.KinematicContraption contraption
                                                    ) {
        ServerSubLevel self = (ServerSubLevel) (Object) this;
        IAdvLiftContraption advContraption = (IAdvLiftContraption) contraption;

        if (advContraption.windwardAerodynamics$areSpanGroupsDirty()) {
            advContraption.windwardAerodynamics$setSpanGroupsDirty(false);
            advContraption.windwardAerodynamics$setCachedSpanGroups(SpanWiseGroup.groupSpanSections(advContraption.windwardAerodynamics$getAdvLiftProviders().values()));
        }

        if (!advContraption.windwardAerodynamics$getCachedSpanGroups().isEmpty()) {
            windwardAerodynamics$aeroForces.integrateAeroForces(self, advContraption.windwardAerodynamics$getCachedSpanGroups(), localContraptionPose, timeStep, linearVelocity, angularVelocity, linearImpulse, angularImpulse);
        }
    }
}
