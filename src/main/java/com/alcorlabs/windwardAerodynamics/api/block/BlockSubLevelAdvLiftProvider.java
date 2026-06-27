package com.alcorlabs.windwardAerodynamics.api.block;

import com.alcorlabs.windwardAerodynamics.foils.PolarLiftDragCoef;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Unique;

public interface BlockSubLevelAdvLiftProvider {

    /**
     * @param state The current blockstate of this lift provider
     * @return The normal of this lift provider
     */
    @NotNull
    Direction windwardAerodynamics$getNormal(BlockState state);

    /**
     * @param state The current blockstate of this lift provider
     * @return The chord normal of this lift provider
     */
    @NotNull
    default Direction windwardAerodynamics$getChordNormal(BlockState state) {
        return Direction.NORTH;
    }


    /**
     * Determines if this surface uses a symmetric foil.
     * Defaults to the legacy getLiftScalar() behavior to maintain add-on compatibility.
     * @deprecated Use {@link #windwardAerodynamics$getFoil(BlockState)} instead to specify custom foils.
     */
    @Deprecated
    default boolean windwardAerodynamics$usesSymmetricFoil() {
        return false;
    }

    /**
     * @param state The current blockstate of this lift provider
     * @return The aerofoil used by this lift provider
     */
    @NotNull
    default PolarLiftDragCoef windwardAerodynamics$getFoil(BlockState state) {
        return windwardAerodynamics$usesSymmetricFoil() ? com.alcorlabs.windwardAerodynamics.foils.AerofoilManager.getSymmetric() : com.alcorlabs.windwardAerodynamics.foils.AerofoilManager.getCambered();
    }


    record LiftProviderContext(BlockPos pos, BlockState state, Vec3 chord, Vec3 normal) {
    }
}