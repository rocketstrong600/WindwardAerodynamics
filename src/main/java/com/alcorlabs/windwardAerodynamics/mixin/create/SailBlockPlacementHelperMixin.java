package com.alcorlabs.windwardAerodynamics.mixin.create;

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

@Mixin(targets = "com.simibubi.create.content.contraptions.bearing.SailBlock$PlacementHelper")
public class SailBlockPlacementHelperMixin {

    @Inject(method = "getOffset", at = @At("RETURN"), cancellable = true, remap = false)
    private void windwardAerodynamics$copyChordState(Player player, Level world, BlockState state, BlockPos pos, BlockHitResult ray, CallbackInfoReturnable<PlacementOffset> cir) {
        PlacementOffset offset = cir.getReturnValue();
        if (offset != null && offset.isSuccessful() && state.hasProperty(WindwardAerodynamicsStates.CHORD_ROTATION)) {
            int rotation = state.getValue(WindwardAerodynamicsStates.CHORD_ROTATION);
            
            // We need to modify the state transform to also include our CHORD_ROTATION
            // We can do this by wrapping the existing transform or providing a new one
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
