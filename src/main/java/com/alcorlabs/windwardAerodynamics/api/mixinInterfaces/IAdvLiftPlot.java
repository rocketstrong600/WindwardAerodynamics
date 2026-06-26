package com.alcorlabs.windwardAerodynamics.api.mixinInterfaces;

import com.alcorlabs.windwardAerodynamics.api.block.BlockSubLevelAdvLiftProvider;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;

public interface IAdvLiftPlot {
    Long2ObjectMap<BlockSubLevelAdvLiftProvider.LiftProviderContext> windwardAerodynamics$getAdvLiftProviders();
}
