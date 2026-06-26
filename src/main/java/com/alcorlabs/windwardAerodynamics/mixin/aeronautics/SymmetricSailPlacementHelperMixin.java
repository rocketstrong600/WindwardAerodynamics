package com.alcorlabs.windwardAerodynamics.mixin.aeronautics;

import com.alcorlabs.windwardAerodynamics.api.block.WindwardAerodynamicsStates;
import net.createmod.catnip.placement.PlacementOffset;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(targets = "dev.simulated_team.simulated.util.placement_helpers.SymmetricSailPlacementHelper")
public class SymmetricSailPlacementHelperMixin {

    @Inject(method = "getOffset", at = @At("RETURN"), cancellable = true, remap = false)
    private void windwardAerodynamics$copyChordState(Player player, Level world, BlockState state, BlockPos pos, BlockHitResult ray, CallbackInfoReturnable<PlacementOffset> cir) {
        PlacementOffset offset = cir.getReturnValue();
        if (offset != null && offset.isSuccessful() && state.hasProperty(WindwardAerodynamicsStates.CHORD_ROTATION)) {
            int rotation = state.getValue(WindwardAerodynamicsStates.CHORD_ROTATION);
            
            PlacementOffset newOffset = PlacementOffset.success(offset.getPos(), s -> {
                BlockState transformed = offset.getTransform().apply(s);
                if (transformed.hasProperty(WindwardAerodynamicsStates.CHORD_ROTATION)) {
                    transformed = transformed.setValue(WindwardAerodynamicsStates.CHORD_ROTATION, rotation);
                }
                return transformed;
            });
            cir.setReturnValue(newOffset);
        }
    }
}
