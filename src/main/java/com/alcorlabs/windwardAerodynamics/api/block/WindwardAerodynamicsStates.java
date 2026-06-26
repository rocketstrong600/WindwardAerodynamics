package com.alcorlabs.windwardAerodynamics.api.block;

import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.properties.IntegerProperty;

public class WindwardAerodynamicsStates {
    /**
     * Represents a rotation around the block's primary axis (Normal).
     * 0 = Default (e.g. North/Up)
     * 1 = 90 deg clockwise
     * 2 = 180 deg
     * 3 = 270 deg clockwise
     */
    public static final IntegerProperty CHORD_ROTATION = IntegerProperty.create("chord_rotation", 0, 3);
    
    /**
     * Helper to determine the chord direction given a normal vector and a rotation index (0-3).
     */
    public static Direction getChordFromRotation(Direction normal, int rotation) {
        Direction[] chords = CHORDS_BY_NORMAL[normal.get3DDataValue()];
        return chords[rotation % 4];
    }

    /**
     * Helper to determine the best rotation index when placing a block.
     */
    public static int getRotationForPlacement(Direction normal, net.minecraft.world.item.context.BlockPlaceContext context) {
        Direction desiredChord = null;
        net.minecraft.world.entity.player.Player player = context.getPlayer();
        
        // 1. Try to inherit from the clicked block (unless sneaking)
        if (player == null || !player.isShiftKeyDown()) {
            net.minecraft.world.level.Level level = context.getLevel();
            net.minecraft.core.BlockPos clickedPos = context.getClickedPos().relative(context.getClickedFace().getOpposite());
            net.minecraft.world.level.block.state.BlockState clickedState = level.getBlockState(clickedPos);
            
            if (clickedState.getBlock() instanceof BlockSubLevelAdvLiftProvider provider) {
                desiredChord = provider.windwardAerodynamics$getChordNormal(clickedState);
            }
        }
        
        // 2. Jigsaw block style orientation (if not inheriting)
        if (desiredChord == null && player != null) {
            net.minecraft.world.phys.Vec3 lookVec = player.getLookAngle();
            double maxDot = -Double.MAX_VALUE;
            Direction bestDir = null;
            
            for (Direction dir : Direction.values()) {
                if (dir.getAxis() != normal.getAxis()) {
                    double dot = dir.getStepX() * lookVec.x + dir.getStepY() * lookVec.y + dir.getStepZ() * lookVec.z;
                    if (dot > maxDot) {
                        maxDot = dot;
                        bestDir = dir;
                    }
                }
            }
            
            // If maxDot is significant, use the look direction.
            // Otherwise, the player is looking directly at the face (all valid chords are orthogonal to look vector).
            // In that case, fallback to edge-click detection so they can point it by clicking near an edge!
            if (maxDot > 0.3 && bestDir != null) {
                desiredChord = bestDir;
            } else {
                net.minecraft.world.phys.Vec3 clickLoc = context.getClickLocation();
                net.minecraft.core.BlockPos pos = context.getClickedPos();
                double dx = clickLoc.x - pos.getX() - 0.5;
                double dy = clickLoc.y - pos.getY() - 0.5;
                double dz = clickLoc.z - pos.getZ() - 0.5;
                
                switch (normal.getAxis()) {
                    case X:
                        if (Math.abs(dy) > Math.abs(dz)) {
                            desiredChord = dy > 0 ? Direction.UP : Direction.DOWN;
                        } else {
                            desiredChord = dz > 0 ? Direction.SOUTH : Direction.NORTH;
                        }
                        break;
                    case Y:
                        if (Math.abs(dx) > Math.abs(dz)) {
                            desiredChord = dx > 0 ? Direction.EAST : Direction.WEST;
                        } else {
                            desiredChord = dz > 0 ? Direction.SOUTH : Direction.NORTH;
                        }
                        break;
                    case Z:
                        if (Math.abs(dx) > Math.abs(dy)) {
                            desiredChord = dx > 0 ? Direction.EAST : Direction.WEST;
                        } else {
                            desiredChord = dy > 0 ? Direction.UP : Direction.DOWN;
                        }
                        break;
                }
            }
        }
        
        if (desiredChord == null) desiredChord = Direction.UP;

        Direction[] chords = CHORDS_BY_NORMAL[normal.get3DDataValue()];
        
        // Find the matching rotation index
        for (int i = 0; i < 4; i++) {
            if (chords[i] == desiredChord) {
                return i;
            }
        }

        return 0; // Default fallback
    }

    public static final Direction[][] CHORDS_BY_NORMAL = new Direction[6][4];
    static {
        // DOWN (0)
        CHORDS_BY_NORMAL[Direction.DOWN.get3DDataValue()] = new Direction[]{Direction.NORTH, Direction.WEST, Direction.SOUTH, Direction.EAST};
        // UP (1)
        CHORDS_BY_NORMAL[Direction.UP.get3DDataValue()] = new Direction[]{Direction.NORTH, Direction.EAST, Direction.SOUTH, Direction.WEST};
        // NORTH (2)
        CHORDS_BY_NORMAL[Direction.NORTH.get3DDataValue()] = new Direction[]{Direction.UP, Direction.WEST, Direction.DOWN, Direction.EAST};
        // SOUTH (3)
        CHORDS_BY_NORMAL[Direction.SOUTH.get3DDataValue()] = new Direction[]{Direction.UP, Direction.EAST, Direction.DOWN, Direction.WEST};
        // WEST (4)
        CHORDS_BY_NORMAL[Direction.WEST.get3DDataValue()] = new Direction[]{Direction.UP, Direction.SOUTH, Direction.DOWN, Direction.NORTH};
        // EAST (5)
        CHORDS_BY_NORMAL[Direction.EAST.get3DDataValue()] = new Direction[]{Direction.UP, Direction.NORTH, Direction.DOWN, Direction.SOUTH};
    }
}
