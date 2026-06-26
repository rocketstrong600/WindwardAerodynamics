package com.alcorlabs.windwardAerodynamics.mixin.create;

import com.alcorlabs.windwardAerodynamics.api.block.BlockSubLevelAdvLiftProvider;
import com.alcorlabs.windwardAerodynamics.foils.Foils;
import com.alcorlabs.windwardAerodynamics.foils.PolarLiftDragCoef;
import com.simibubi.create.content.contraptions.bearing.SailBlock;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin(SailBlock.class)
public class SailBlockMixin implements BlockSubLevelAdvLiftProvider {
    
    @Override
    public @NotNull Direction windwardAerodynamics$getNormal(final BlockState state) {
        return state.getValue(BlockStateProperties.FACING);
    }

    @Override
    public @NotNull Direction windwardAerodynamics$getChordNormal(final BlockState state) {
        return Direction.NORTH;
    }

    @Unique
    @Override
    public @NotNull PolarLiftDragCoef windwardAerodynamics$getFoil(BlockState state) {
        return Foils.SYMMETRICFOIL; 
    }
}
