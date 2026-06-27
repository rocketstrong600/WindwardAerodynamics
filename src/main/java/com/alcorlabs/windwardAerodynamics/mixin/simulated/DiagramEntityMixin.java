package com.alcorlabs.windwardAerodynamics.mixin.simulated;

import com.llamalad7.mixinextras.sugar.Local;

import dev.ryanhcode.sable.api.block.BlockEntitySubLevelActor;
import dev.ryanhcode.sable.api.physics.force.ForceGroup;
import dev.ryanhcode.sable.api.physics.force.ForceGroups;
import dev.ryanhcode.sable.api.physics.force.QueuedForceGroup;
import dev.ryanhcode.sable.physics.config.dimension_physics.DimensionPhysicsData;
import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import dev.ryanhcode.sable.sublevel.SubLevel;
import dev.ryanhcode.sable.sublevel.system.SubLevelPhysicsSystem;
import dev.simulated_team.simulated.content.entities.diagram.DiagramEntity;
import dev.simulated_team.simulated.network.packets.contraption_diagram.DiagramDataPacket;
import it.unimi.dsi.fastutil.objects.Object2ObjectMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.server.level.ServerLevel;
import org.joml.Vector3d;
import org.joml.Vector3dc;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;

@Mixin(value = DiagramEntity.class, remap = false)
public class DiagramEntityMixin {

    private static Set<ServerSubLevel> getConnectedSubLevels(ServerSubLevel root) {
        Set<ServerSubLevel> visited = new HashSet<>();
        Queue<ServerSubLevel> queue = new LinkedList<>();
        queue.add(root);
        visited.add(root);

        while (!queue.isEmpty()) {
            ServerSubLevel current = queue.poll();
            for (BlockEntitySubLevelActor actor : current.getPlot().getBlockEntityActors()) {
                Iterable<SubLevel> deps = actor.sable$getConnectionDependencies();
                if (deps != null) {
                    for (SubLevel dep : deps) {
                        if (dep instanceof ServerSubLevel serverDep && !visited.contains(serverDep)) {
                            visited.add(serverDep);
                            queue.add(serverDep);
                        }
                    }
                }
            }
        }
        return visited;
    }

    @Redirect(method = {"queueDiagramDataFor", "postPhysicsTick"}, at = @At(value = "INVOKE", target = "Ldev/ryanhcode/sable/sublevel/ServerSubLevel;enableIndividualQueuedForcesTracking(Z)V"))
    private static void redirectEnableTracking(ServerSubLevel instance, boolean tracking) {
        for (ServerSubLevel connected : getConnectedSubLevels(instance)) {
            connected.enableIndividualQueuedForcesTracking(tracking);
        }
    }

    @Inject(method = "makeDiagramDataPacket", at = @At("RETURN"), cancellable = true)
    private static void onMakeDiagramDataPacket(ServerSubLevel root, CallbackInfoReturnable<DiagramDataPacket> cir, @Local ServerLevel level, @Local double timeStep) {
        DiagramDataPacket originalPacket = cir.getReturnValue();
        Map<ForceGroup, List<QueuedForceGroup.PointForce>> sentForces = originalPacket.forces();
        double totalMass = originalPacket.mass();

        boolean addedAny = false;

        for (ServerSubLevel current : getConnectedSubLevels(root)) {
            if (current == root) continue;
            addedAny = true;

            totalMass += current.getMassTracker().getMass();

            final Object2ObjectMap<ForceGroup, QueuedForceGroup> queuedForceGroups = current.getQueuedForceGroups();
            if (queuedForceGroups != null) {
                for (final Map.Entry<ForceGroup, QueuedForceGroup> entry : queuedForceGroups.entrySet()) {
                    final ForceGroup key = entry.getKey();
                    final QueuedForceGroup value = entry.getValue();

                    List<QueuedForceGroup.PointForce> pointForces = sentForces.get(key);
                    if (pointForces == null) {
                        pointForces = new ObjectArrayList<>();
                        sentForces.put(key, pointForces);
                    } else if (!(pointForces instanceof ObjectArrayList)) {
                        pointForces = new ObjectArrayList<>(pointForces);
                        sentForces.put(key, pointForces);
                    }

                    for (final QueuedForceGroup.PointForce pointForce : value.getRecordedPointForces()) {
                        Vector3d localForce = new Vector3d(pointForce.force()).div(timeStep);
                        Vector3d localPoint = new Vector3d(pointForce.point());

                        current.logicalPose().transformNormal(localForce);
                        current.logicalPose().transformPosition(localPoint);

                        root.logicalPose().transformNormalInverse(localForce);
                        root.logicalPose().transformPositionInverse(localPoint);

                        pointForces.add(new QueuedForceGroup.PointForce(localPoint, localForce));
                    }
                }
            }

            final Vector3dc centerOfMass = current.getMassTracker().getCenterOfMass();
            final Vector3d localGravity = current.logicalPose().transformNormalInverse(DimensionPhysicsData.getGravity(level)).mul(current.getMassTracker().getMass());

            Vector3d gravPoint = new Vector3d(centerOfMass);
            current.logicalPose().transformPosition(gravPoint);
            root.logicalPose().transformPositionInverse(gravPoint);

            current.logicalPose().transformNormal(localGravity);
            root.logicalPose().transformNormalInverse(localGravity);

            ForceGroup gravKey = ForceGroups.GRAVITY.get();
            List<QueuedForceGroup.PointForce> gravForces = sentForces.get(gravKey);
            if (gravForces == null) {
                gravForces = new ObjectArrayList<>();
                sentForces.put(gravKey, gravForces);
            } else if (!(gravForces instanceof ObjectArrayList)) {
                gravForces = new ObjectArrayList<>(gravForces);
                sentForces.put(gravKey, gravForces);
            }
            gravForces.add(new QueuedForceGroup.PointForce(gravPoint, localGravity));
        }

        if (addedAny) {
            cir.setReturnValue(new DiagramDataPacket(sentForces, totalMass));
        }
    }
}
