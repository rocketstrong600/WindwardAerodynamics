package com.alcorlabs.windwardAerodynamics.mixin.create;

import com.alcorlabs.windwardAerodynamics.api.block.BlockSubLevelAdvLiftProvider;

import com.alcorlabs.windwardAerodynamics.foils.PolarLiftDragCoef;
import com.simibubi.create.content.contraptions.bearing.SailBlock;
import com.simibubi.create.foundation.block.WrenchableDirectionalBlock;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import com.alcorlabs.windwardAerodynamics.api.block.WindwardAerodynamicsShapes;

import com.alcorlabs.windwardAerodynamics.api.block.WindwardAerodynamicsStates;
import net.minecraft.world.level.block.state.StateDefinition;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(SailBlock.class)
public abstract class SailBlockMixin extends WrenchableDirectionalBlock implements BlockSubLevelAdvLiftProvider {

    public SailBlockMixin(Properties properties) {
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
            Direction normal = state.getValue(BlockStateProperties.FACING);
            int rotation = WindwardAerodynamicsStates.getRotationForPlacement(normal, context);
            cir.setReturnValue(state.setValue(WindwardAerodynamicsStates.CHORD_ROTATION, rotation));
        }
    }

    @Inject(method = "getShape", at = @At("HEAD"), cancellable = true)
    private void windwardAerodynamics$getCustomShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context, CallbackInfoReturnable<VoxelShape> cir) {
        Direction normal = state.getValue(BlockStateProperties.FACING);
        int chordRot = state.hasProperty(WindwardAerodynamicsStates.CHORD_ROTATION) ? state.getValue(WindwardAerodynamicsStates.CHORD_ROTATION) : 0;
        cir.setReturnValue(WindwardAerodynamicsShapes.getShape(normal, chordRot));
    }

    @Override
    public BlockState getRotatedBlockState(BlockState originalState, Direction targetedFace) {
        BlockState rotated = super.getRotatedBlockState(originalState, targetedFace);
        Direction facing = originalState.getValue(BlockStateProperties.FACING);
        int currentRot = originalState.getValue(WindwardAerodynamicsStates.CHORD_ROTATION);
        
        if (facing.getAxis() == targetedFace.getAxis()) {
            // Wrench clicked on the flat face! Cycle the chord rotation!
            int delta = (targetedFace == facing) ? 1 : 3; // +1 if front, -1 (which is +3 mod 4) if back
            return originalState.setValue(WindwardAerodynamicsStates.CHORD_ROTATION, (currentRot + delta) % 4);
        } else {
            // Wrench clicked on the side. The normal changes!
            Direction newNormal = rotated.getValue(BlockStateProperties.FACING);
            if (newNormal == facing) return rotated; // No rotation happened
            
            // To preserve the visual chord as much as possible, let's find the old chord in world space.
            Direction oldChord = WindwardAerodynamicsStates.getChordFromRotation(facing, currentRot);
            
            // Rotate the old chord around the targeted face!
            Direction newChord = oldChord;
            if (oldChord.getAxis() != targetedFace.getAxis()) {
                newChord = oldChord.getClockWise(targetedFace.getAxis());
            }
            
            // Now we find the closest chord rotation for the new normal!
            int newRot = 0;
            Direction[] possibleChords = WindwardAerodynamicsStates.CHORDS_BY_NORMAL[newNormal.get3DDataValue()];
            for (int i = 0; i < 4; i++) {
                if (possibleChords[i] == newChord || possibleChords[i] == newChord.getOpposite()) {
                    newRot = i;
                    if (possibleChords[i] == newChord) break;
                }
            }
            
            return rotated.setValue(WindwardAerodynamicsStates.CHORD_ROTATION, newRot);
        }
    }

    @Override
    public @NotNull Direction windwardAerodynamics$getNormal(final BlockState state) {
        return state.getValue(BlockStateProperties.FACING);
    }

    @Override
    public @NotNull Direction windwardAerodynamics$getChordNormal(final BlockState state) {
        Direction normal = state.getValue(BlockStateProperties.FACING);
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
            // Normal = Orange Flame. Chord = Blue Flame.
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
