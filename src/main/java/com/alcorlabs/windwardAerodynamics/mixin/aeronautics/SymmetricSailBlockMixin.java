package com.alcorlabs.windwardAerodynamics.mixin.aeronautics;

import com.alcorlabs.windwardAerodynamics.api.block.BlockSubLevelAdvLiftProvider;
import com.alcorlabs.windwardAerodynamics.foils.Foils;
import com.alcorlabs.windwardAerodynamics.foils.PolarLiftDragCoef;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Mixin;
import dev.simulated_team.simulated.content.blocks.symmetric_sail.SymmetricSailBlock;

@Mixin(SymmetricSailBlock.class)
public class SymmetricSailBlockMixin implements BlockSubLevelAdvLiftProvider {

    @Override
    public @NotNull Direction windwardAerodynamics$getNormal(BlockState state) {
        return Direction.get(Direction.AxisDirection.POSITIVE, state.getValue(SymmetricSailBlock.AXIS));
    }

    @Override
    public @NotNull PolarLiftDragCoef windwardAerodynamics$getFoil(BlockState state) {
        return Foils.SYMMETRICFOIL;
    }
}
