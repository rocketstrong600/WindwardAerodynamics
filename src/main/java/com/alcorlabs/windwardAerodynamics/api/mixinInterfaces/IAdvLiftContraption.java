package com.alcorlabs.windwardAerodynamics.api.mixinInterfaces;

import com.alcorlabs.windwardAerodynamics.api.block.BlockSubLevelAdvLiftProvider;
import net.minecraft.core.BlockPos;
import java.util.Map;

public interface IAdvLiftContraption {
    void windwardAerodynamics$markSpanGroupsDirty();

    Map<BlockPos, BlockSubLevelAdvLiftProvider.LiftProviderContext> windwardAerodynamics$getAdvLiftProviders();

    boolean windwardAerodynamics$areSpanGroupsDirty();
    void windwardAerodynamics$setSpanGroupsDirty(boolean dirty);
    
    java.util.List<com.alcorlabs.windwardAerodynamics.api.physics.SpanWiseGroup> windwardAerodynamics$getCachedSpanGroups();
    void windwardAerodynamics$setCachedSpanGroups(java.util.List<com.alcorlabs.windwardAerodynamics.api.physics.SpanWiseGroup> groups);
}
