package com.alcorlabs.windwardAerodynamics.api.block;

import net.minecraft.core.Direction;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.shapes.BooleanOp;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import java.util.HashMap;
import java.util.Map;

public class WindwardAerodynamicsShapes {

    private static final Map<Integer, VoxelShape> SAIL_SHAPES = new HashMap<>();

    static {
        // Base shape for normal = UP (+Y), chord = NORTH (-Z)
        // Main body (16x16 square slab, 4 pixels thick)
        AABB main = new AABB(0.0, 6.0/16.0, 0.0, 1.0, 10.0/16.0, 1.0);
        
        VoxelShape baseShape = Shapes.create(main);

        for (Direction facing : Direction.values()) {
            for (int chordRot = 0; chordRot < 4; chordRot++) {
                // Find the target Chord from the rotation index
                Direction chord = WindwardAerodynamicsStates.getChordFromRotation(facing, chordRot);
                // Rotate the shape
                VoxelShape rotated = rotateShape(baseShape, facing, chord);
                SAIL_SHAPES.put(getStateHash(facing, chordRot), rotated);
            }
        }
    }

    public static VoxelShape getShape(Direction normal, int chordRot) {
        return SAIL_SHAPES.getOrDefault(getStateHash(normal, chordRot), Shapes.block());
    }

    private static int getStateHash(Direction normal, int chordRot) {
        return normal.get3DDataValue() * 10 + chordRot;
    }

    private static VoxelShape rotateShape(VoxelShape shape, Direction targetNormal, Direction targetChord) {
        VoxelShape result = Shapes.empty();
        
        // Base coordinate system:
        // Y = UP (Normal)
        // -Z = NORTH (Chord)
        // -X = WEST (UP x NORTH) -> so +X is East.
        
        Direction targetY = targetNormal;
        Direction targetNegZ = targetChord;
        Direction targetZ = targetChord.getOpposite();
        
        // Target X = Cross product of Target Y and Target Z

        Direction targetX = getCrossProduct(targetY, targetZ);
        
        for (AABB box : shape.toAabbs()) {
            // Box is defined by min and max.
            // Move origin to center (0.5, 0.5, 0.5)
            double x1 = box.minX - 0.5;
            double y1 = box.minY - 0.5;
            double z1 = box.minZ - 0.5;
            double x2 = box.maxX - 0.5;
            double y2 = box.maxY - 0.5;
            double z2 = box.maxZ - 0.5;

            // Transform all 8 corners
            double[][] corners = {
                {x1, y1, z1}, {x1, y1, z2}, {x1, y2, z1}, {x1, y2, z2},
                {x2, y1, z1}, {x2, y1, z2}, {x2, y2, z1}, {x2, y2, z2}
            };
            
            double minX = Double.MAX_VALUE, minY = Double.MAX_VALUE, minZ = Double.MAX_VALUE;
            double maxX = -Double.MAX_VALUE, maxY = -Double.MAX_VALUE, maxZ = -Double.MAX_VALUE;
            
            for (double[] c : corners) {
                // Vector in base coords is: c[0]*X_base + c[1]*Y_base + c[2]*Z_base
                // Where X_base = +X, Y_base = +Y, Z_base = +Z
                // We map them to the new axes:
                double nx = c[0] * targetX.getStepX() + c[1] * targetY.getStepX() + c[2] * targetZ.getStepX();
                double ny = c[0] * targetX.getStepY() + c[1] * targetY.getStepY() + c[2] * targetZ.getStepY();
                double nz = c[0] * targetX.getStepZ() + c[1] * targetY.getStepZ() + c[2] * targetZ.getStepZ();
                
                if (nx < minX) minX = nx; if (nx > maxX) maxX = nx;
                if (ny < minY) minY = ny; if (ny > maxY) maxY = ny;
                if (nz < minZ) minZ = nz; if (nz > maxZ) maxZ = nz;
            }
            
            result = Shapes.join(result, Shapes.create(
                minX + 0.5, minY + 0.5, minZ + 0.5,
                maxX + 0.5, maxY + 0.5, maxZ + 0.5
            ), BooleanOp.OR);
        }
        return result;
    }

    private static Direction getCrossProduct(Direction a, Direction b) {
        int cx = a.getStepY() * b.getStepZ() - a.getStepZ() * b.getStepY();
        int cy = a.getStepZ() * b.getStepX() - a.getStepX() * b.getStepZ();
        int cz = a.getStepX() * b.getStepY() - a.getStepY() * b.getStepX();
        for (Direction d : Direction.values()) {
            if (d.getStepX() == cx && d.getStepY() == cy && d.getStepZ() == cz) {
                return d;
            }
        }
        return Direction.NORTH; // Fallback
    }
}
