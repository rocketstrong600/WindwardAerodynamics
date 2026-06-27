package com.alcorlabs.windwardAerodynamics.api.physics;

import com.alcorlabs.windwardAerodynamics.api.block.BlockSubLevelAdvLiftProvider;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectCollection;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3d;

import java.util.Collection;
import java.util.List;

public class SpanWiseGroup {
    static Direction[] DIRECTIONS = Direction.values();

    private final ObjectCollection<SpanWiseSection> sections;
    public final Vector3d totalLift = new Vector3d();
    public final Vector3d liftCenter = new Vector3d();
    public final Vector3d totalDrag = new Vector3d();
    public final Vector3d dragCenter = new Vector3d();
    public double totalLiftStrength;
    public double totalDragStrength;
    private final double aspectRatio;

    public SpanWiseGroup(final ObjectCollection<SpanWiseSection> sections, final double aspectRatio) {
        this.sections = sections;

        this.aspectRatio = aspectRatio;
    }

    public static List<SpanWiseGroup> groupSpanSections(final Collection<BlockSubLevelAdvLiftProvider.LiftProviderContext> liftProviders) {
        final List<SpanWiseGroup> groups = new ObjectArrayList<>();

        final Object2ObjectMap<BlockPos, BlockSubLevelAdvLiftProvider.LiftProviderContext> positions = new Object2ObjectOpenHashMap<>(liftProviders.size());

        for (final BlockSubLevelAdvLiftProvider.LiftProviderContext liftProvider : liftProviders) {
            positions.putIfAbsent(liftProvider.pos(), liftProvider);
        }

        while (!positions.isEmpty()) {

            // run a flood-fill
            final Int2ObjectOpenHashMap<SpanWiseSection> wingSlices = new Int2ObjectOpenHashMap<>(liftProviders.size());

            final List<BlockPos> toVisit = new ObjectArrayList<>();

            toVisit.add(positions.keySet().iterator().next());

            int area = 0;

            while (!toVisit.isEmpty()) {

                final BlockPos pos = toVisit.removeLast();

                final BlockSubLevelAdvLiftProvider.LiftProviderContext context = positions.get(pos);
                if (context == null) {
                    continue;
                }

                final Vec3 chord = context.chord();
                final Vec3 normal = context.normal();

                final Direction spanDir = Direction.getNearest(chord.cross(normal));
                final Direction chordDir = Direction.getNearest(chord);
                final Direction normalDir = Direction.getNearest(normal);

                final int spanWisePos = pos.get(spanDir.getAxis());
                final int chordWisePos = pos.get(chordDir.getAxis());

                // NOTE: slice length is positional EXTENT along the chord, not block count.
                // Gaps inside a wing (e.g. bearings driving control surfaces) intentionally
                // count toward area and aspect ratio.
                if (!wingSlices.containsKey(spanWisePos)) {
                    final BlockSubLevelAdvLiftProvider liftProvider = (BlockSubLevelAdvLiftProvider) context.state().getBlock();
                    wingSlices.put(spanWisePos, new SpanWiseSection(pos, context.state(), 1, chord, normal, liftProvider.windwardAerodynamics$getFoil(context.state())));
                    area += 1;
                } else {

                    final SpanWiseSection oldWingSlice = wingSlices.get(spanWisePos);

                    final int oldChordWisePos = oldWingSlice.getLead().get(chordDir.getAxis());

                    final int chordWisePosDir = chordWisePos * chordDir.getAxisDirection().getStep();
                    final int oldChordWisePosDir = oldChordWisePos * chordDir.getAxisDirection().getStep();


                    // if forward is growing else if aft is growing
                    if (chordWisePosDir > oldChordWisePosDir) {
                        final int newChordLength = (chordWisePosDir-oldChordWisePosDir)+oldWingSlice.getLength();
                        final BlockSubLevelAdvLiftProvider liftProvider = (BlockSubLevelAdvLiftProvider) context.state().getBlock();
                        final SpanWiseSection newSpanWiseSection = new SpanWiseSection(pos, context.state(), newChordLength, chord, normal, liftProvider.windwardAerodynamics$getFoil(context.state()));
                        area += newChordLength - oldWingSlice.getLength();
                        wingSlices.replace(spanWisePos, newSpanWiseSection);
                    } else if (chordWisePosDir < oldChordWisePosDir - (oldWingSlice.getLength()-1)) {
                        final int newChordLength = (oldChordWisePosDir-chordWisePosDir)+1;
                        final SpanWiseSection newSpanWiseSection = new SpanWiseSection(oldWingSlice.getLead(), oldWingSlice.getLeadState(), newChordLength, oldWingSlice.getChord(), oldWingSlice.getNormal(), oldWingSlice.getFoil());
                        area += newChordLength - oldWingSlice.getLength();
                        wingSlices.replace(spanWisePos, newSpanWiseSection);
                    }
                }

                positions.remove(pos);

                for (final Direction direction : DIRECTIONS) {
                    // Don't spread along the wing normal: vertically stacked wings
                    // (e.g. biplanes) are separate lifting surfaces.
                    if (direction.getAxis() == normalDir.getAxis()) {
                        continue;
                    }

                    final BlockPos offsetPos = pos.relative(direction);
                    final BlockSubLevelAdvLiftProvider.LiftProviderContext neighbor = positions.get(offsetPos);

                    // Require matching normal AND chord so span/chord axes are uniform
                    // within a group; otherwise spanwise slice keys from different axes
                    // would collide in the int-keyed map.
                    if (neighbor != null && neighbor.normal().equals(normal) && neighbor.chord().equals(chord)) {
                        toVisit.add(offsetPos);
                    }
                }
            }

            final double span = wingSlices.size();
            final double aspectRatio = span * span / (double) area;

            groups.add(new SpanWiseGroup(wingSlices.values(), aspectRatio));
        }

        return groups;
    }

    public void resetTotals() {
        this.totalLift.set(0, 0, 0);
        this.totalDrag.set(0, 0, 0);
        this.liftCenter.set(0, 0, 0);
        this.dragCenter.set(0, 0, 0);
        this.totalLiftStrength = 0;
        this.totalDragStrength = 0;
    }

    public ObjectCollection<SpanWiseSection> sections() {
        return this.sections;
    }

    public Vector3d totalLift() {
        return this.totalLift;
    }

    public Vector3d liftCenter() {
        return this.liftCenter;
    }

    public Vector3d totalDrag() {
        return this.totalDrag;
    }

    public Vector3d dragCenter() {
        return this.dragCenter;
    }

    public double aspectRatio() { return aspectRatio; }
}
