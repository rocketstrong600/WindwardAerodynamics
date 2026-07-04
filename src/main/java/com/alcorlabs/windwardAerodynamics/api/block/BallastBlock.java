package com.alcorlabs.windwardAerodynamics.api.block;

import dev.ryanhcode.sable.api.block.BlockSubLevelCustomCenterOfMass;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import org.joml.Vector3d;
import org.joml.Vector3dc;

public class BallastBlock extends Block implements BlockSubLevelCustomCenterOfMass {

    public BallastBlock(Properties properties) {
        super(properties);
    }

    @Override
    public Vector3dc getCenterOfMass(BlockGetter blockGetter, BlockState state) {
        // Shift the center of mass straight down by 10 meters
        return new Vector3d(0.5, -10.0, 0.5);
    }
}
