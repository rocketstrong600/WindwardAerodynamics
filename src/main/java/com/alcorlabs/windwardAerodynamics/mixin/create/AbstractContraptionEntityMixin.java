package com.alcorlabs.windwardAerodynamics.mixin.create;

import com.alcorlabs.windwardAerodynamics.api.block.BlockSubLevelAdvLiftProvider;
import com.alcorlabs.windwardAerodynamics.api.mixinInterfaces.IAdvLiftContraption;
import com.simibubi.create.content.contraptions.AbstractContraptionEntity;
import com.simibubi.create.content.contraptions.Contraption;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.HashMap;
import java.util.Map;

@Mixin(AbstractContraptionEntity.class)
public abstract class AbstractContraptionEntityMixin implements IAdvLiftContraption {
    
    @Shadow protected Contraption contraption;

    @Unique
    private final Map<BlockPos, BlockSubLevelAdvLiftProvider.LiftProviderContext> windwardAerodynamics$liftProviders = new HashMap<>();

    @Unique
    private boolean windwardAerodynamics$initialized = false;

    @Override
    public Map<BlockPos, BlockSubLevelAdvLiftProvider.LiftProviderContext> windwardAerodynamics$getAdvLiftProviders() {
        return this.windwardAerodynamics$liftProviders;
    }

    @Unique
    private boolean windwardAerodynamics$spanGroupsDirty = true;
    
    @Unique
    private java.util.List<com.alcorlabs.windwardAerodynamics.api.physics.SpanWiseGroup> windwardAerodynamics$cachedSpanGroups = new java.util.ArrayList<>();

    @Override
    public void windwardAerodynamics$markSpanGroupsDirty() {
        this.windwardAerodynamics$spanGroupsDirty = true;
    }

    @Override
    public boolean windwardAerodynamics$areSpanGroupsDirty() {
        return this.windwardAerodynamics$spanGroupsDirty;
    }

    @Override
    public void windwardAerodynamics$setSpanGroupsDirty(boolean dirty) {
        this.windwardAerodynamics$spanGroupsDirty = dirty;
    }

    @Override
    public java.util.List<com.alcorlabs.windwardAerodynamics.api.physics.SpanWiseGroup> windwardAerodynamics$getCachedSpanGroups() {
        return this.windwardAerodynamics$cachedSpanGroups;
    }

    @Override
    public void windwardAerodynamics$setCachedSpanGroups(java.util.List<com.alcorlabs.windwardAerodynamics.api.physics.SpanWiseGroup> groups) {
        this.windwardAerodynamics$cachedSpanGroups = groups;
    }

    // 1. Gather all advanced lift providers from the contraption's blocks
    @Inject(method = "tick", at = @At("TAIL"))
    private void windwardAerodynamics$onTick(CallbackInfo ci) {
        if (!this.windwardAerodynamics$initialized && this.contraption != null && this.contraption.getBlocks() != null) {
            
            // Do not calculate lift on Propellers, they handle their own aerodynamics
            if (((Object) this) instanceof dev.eriksonn.aeronautics.content.blocks.propeller.bearing.contraption.PropellerBearingContraptionEntity) {
                this.windwardAerodynamics$initialized = true;
                return;
            }

            for (Map.Entry<BlockPos, StructureTemplate.StructureBlockInfo> entry : this.contraption.getBlocks().entrySet()) {
                BlockState state = entry.getValue().state();
                if (state.getBlock() instanceof BlockSubLevelAdvLiftProvider prov) {
                    Vec3 normal = Vec3.atLowerCornerOf(prov.windwardAerodynamics$getNormal(state).getNormal());
                    Vec3 chord = Vec3.atLowerCornerOf(prov.windwardAerodynamics$getChordNormal(state).getNormal());
                    
                    windwardAerodynamics$liftProviders.put(
                        entry.getKey(), 
                        new BlockSubLevelAdvLiftProvider.LiftProviderContext(entry.getKey(), state, chord, normal)
                    );
                }
            }
            this.windwardAerodynamics$initialized = true;
        }
    }
}
