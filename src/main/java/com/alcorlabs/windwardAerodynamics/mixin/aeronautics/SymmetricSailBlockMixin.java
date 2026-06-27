package com.alcorlabs.windwardAerodynamics.mixin.aeronautics;

import com.alcorlabs.windwardAerodynamics.api.block.BlockSubLevelAdvLiftProvider;

import com.alcorlabs.windwardAerodynamics.foils.PolarLiftDragCoef;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Mixin;
import dev.simulated_team.simulated.content.blocks.symmetric_sail.SymmetricSailBlock;
import com.simibubi.create.content.equipment.wrench.IWrenchable;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import com.alcorlabs.windwardAerodynamics.api.block.WindwardAerodynamicsShapes;

import com.alcorlabs.windwardAerodynamics.api.block.WindwardAerodynamicsStates;
import net.minecraft.world.level.block.state.StateDefinition;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(SymmetricSailBlock.class)
public abstract class SymmetricSailBlockMixin extends RotatedPillarBlock implements BlockSubLevelAdvLiftProvider, IWrenchable {

    public SymmetricSailBlockMixin(Properties properties) {
        super(properties);
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(WindwardAerodynamicsStates.CHORD_ROTATION);
    }

    @Inject(method = "getStateForPlacement", at = @At("RETURN"), cancellable = true)
    private void windwardAerodynamics$setChordState(BlockPlaceContext context, CallbackInfoReturnable<BlockState> cir) {
        BlockState state = cir.getReturnValue();
        if (state != null) {
            Direction normal = Direction.get(Direction.AxisDirection.POSITIVE, state.getValue(SymmetricSailBlock.AXIS));
            int rotation = WindwardAerodynamicsStates.getRotationForPlacement(normal, context);
            cir.setReturnValue(state.setValue(WindwardAerodynamicsStates.CHORD_ROTATION, rotation));
        }
    }

    @Inject(method = "getShape", at = @At("HEAD"), cancellable = true)
    private void windwardAerodynamics$getCustomShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context, CallbackInfoReturnable<VoxelShape> cir) {
        Direction normal = Direction.get(Direction.AxisDirection.POSITIVE, state.getValue(SymmetricSailBlock.AXIS));
        int chordRot = state.hasProperty(WindwardAerodynamicsStates.CHORD_ROTATION) ? state.getValue(WindwardAerodynamicsStates.CHORD_ROTATION) : 0;
        cir.setReturnValue(WindwardAerodynamicsShapes.getShape(normal, chordRot));
    }

    @Override
    public BlockState getRotatedBlockState(BlockState originalState, Direction targetedFace) {
        Direction.Axis axis = originalState.getValue(SymmetricSailBlock.AXIS);
        int currentRot = originalState.getValue(WindwardAerodynamicsStates.CHORD_ROTATION);
        
        if (axis == targetedFace.getAxis()) {
            // Wrench clicked on the flat face: Cycle the chord rotation.
            // Positive faces (e.g., South) rotate clockwise (+1). Negative faces (e.g., North) rotate counter-clockwise (-1).
            int delta = (targetedFace.getAxisDirection() == Direction.AxisDirection.POSITIVE) ? 1 : 3;
            return originalState.setValue(WindwardAerodynamicsStates.CHORD_ROTATION, (currentRot + delta) % 4);
        } else {
            // Wrench clicked on the side edge: The block's primary axis changes.
            // The new axis will be the axis orthogonal to both the old axis and the targeted face's axis.
            Direction.Axis newAxis = Direction.Axis.X;
            for (Direction.Axis a : Direction.Axis.values()) {
                if (a != axis && a != targetedFace.getAxis()) {
                    newAxis = a;
                    break;
                }
            }
            
            // To preserve the visual orientation of the sail as much as possible, we determine the old chord in world space.
            Direction oldNormal = Direction.get(Direction.AxisDirection.POSITIVE, axis);
            Direction oldChord = WindwardAerodynamicsStates.getChordFromRotation(oldNormal, currentRot);
            
            // Calculate how the chord direction vector rotates around the targeted face when wrenched.
            Direction newChord = oldChord;
            if (oldChord.getAxis() != targetedFace.getAxis()) {
                // Chords not parallel to the rotation axis rotate synchronously with the block.
                newChord = oldChord.getClockWise(targetedFace.getAxis());
            }
            
            // Determine the closest valid chord rotation for the newly assigned axis.
            Direction newNormal = Direction.get(Direction.AxisDirection.POSITIVE, newAxis);
            int newRot = 0;
            Direction[] possibleChords = WindwardAerodynamicsStates.CHORDS_BY_NORMAL[newNormal.get3DDataValue()];
            for (int i = 0; i < 4; i++) {
                if (possibleChords[i] == newChord || possibleChords[i] == newChord.getOpposite()) {
                    newRot = i;
                    // Exact matches take priority over opposites.
                    if (possibleChords[i] == newChord) break;
                }
            }
            
            return originalState.setValue(SymmetricSailBlock.AXIS, newAxis)
                                .setValue(WindwardAerodynamicsStates.CHORD_ROTATION, newRot);
        }
    }

    @Override
    public @NotNull Direction windwardAerodynamics$getNormal(BlockState state) {
        return Direction.get(Direction.AxisDirection.POSITIVE, state.getValue(SymmetricSailBlock.AXIS));
    }

    @Override
    public @NotNull Direction windwardAerodynamics$getChordNormal(BlockState state) {
        Direction normal = Direction.get(Direction.AxisDirection.POSITIVE, state.getValue(SymmetricSailBlock.AXIS));
        int rotation = state.hasProperty(WindwardAerodynamicsStates.CHORD_ROTATION) ? state.getValue(WindwardAerodynamicsStates.CHORD_ROTATION) : 0;
        return WindwardAerodynamicsStates.getChordFromRotation(normal, rotation);
    }

    @Unique
    @Override
    public @NotNull PolarLiftDragCoef windwardAerodynamics$getFoil(BlockState state) {
        return com.alcorlabs.windwardAerodynamics.foils.AerofoilManager.getSymmetric();
    }

    @Override
    public void animateTick(BlockState state, net.minecraft.world.level.Level level, BlockPos pos, net.minecraft.util.RandomSource random) {
        if (!com.alcorlabs.windwardAerodynamics.Config.DEBUG_PARTICLES.get()) {
            super.animateTick(state, level, pos, random);
            return;
        }

        super.animateTick(state, level, pos, random);
        Direction normal = windwardAerodynamics$getNormal(state);
        Direction chord = windwardAerodynamics$getChordNormal(state);
        
        double x = pos.getX() + 0.5;
        double y = pos.getY() + 0.5;
        double z = pos.getZ() + 0.5;
        
        // Spawn 3 particles per animate tick to make the stream much denser and more visible
        for (int i = 0; i < 3; i++) {
            // Give them a tiny bit of velocity in the direction they are pointing so they stretch out slightly
            level.addParticle(net.minecraft.core.particles.ParticleTypes.FLAME, 
                x + normal.getStepX() * 0.6, y + normal.getStepY() * 0.6, z + normal.getStepZ() * 0.6, 
                normal.getStepX() * 0.02, normal.getStepY() * 0.02, normal.getStepZ() * 0.02);
                
            level.addParticle(net.minecraft.core.particles.ParticleTypes.SOUL_FIRE_FLAME, 
                x + chord.getStepX() * 0.6, y + chord.getStepY() * 0.6, z + chord.getStepZ() * 0.6, 
                chord.getStepX() * 0.02, chord.getStepY() * 0.02, chord.getStepZ() * 0.02);
        }
    }
}
