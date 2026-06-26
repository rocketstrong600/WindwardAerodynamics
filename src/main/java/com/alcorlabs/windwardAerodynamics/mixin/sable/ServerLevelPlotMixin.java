package com.alcorlabs.windwardAerodynamics.mixin.sable;

import com.alcorlabs.windwardAerodynamics.api.block.BlockSubLevelAdvLiftProvider;
import com.alcorlabs.windwardAerodynamics.api.mixinInterfaces.IAdvLiftPlot;
import com.alcorlabs.windwardAerodynamics.api.mixinInterfaces.IAdvLiftSubLevel;
import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import dev.ryanhcode.sable.sublevel.plot.ServerLevelPlot;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerLevelPlot.class)
public abstract class ServerLevelPlotMixin implements IAdvLiftPlot {

    @Unique
    private final Long2ObjectMap<BlockSubLevelAdvLiftProvider.LiftProviderContext> windwardAerodynamics$advLiftProviders = new Long2ObjectOpenHashMap<>();

    @Override
    public Long2ObjectMap<BlockSubLevelAdvLiftProvider.LiftProviderContext> windwardAerodynamics$getAdvLiftProviders() {
        return this.windwardAerodynamics$advLiftProviders;
    }

    @Final
    @Shadow(remap = false)
    private Long2ObjectMap<dev.ryanhcode.sable.api.block.BlockSubLevelLiftProvider.LiftProviderContext> liftProviders;

    @Shadow
    public abstract ServerSubLevel getSubLevel();

    @Inject(method = "onBlockChange", at = @At("TAIL"), remap = false)
    private void windwardAerodynamics$tail$onBlockChange(BlockPos pos, BlockState state, CallbackInfo ci) {
        if (state.getBlock() instanceof final BlockSubLevelAdvLiftProvider prov) {
            // Remove it from Sable's lift providers directly by key so arcady aero doesn't run!
            this.liftProviders.remove(pos.asLong());
            this.windwardAerodynamics$advLiftProviders.put(pos.asLong(), new BlockSubLevelAdvLiftProvider.LiftProviderContext(pos, state, Vec3.atLowerCornerOf(prov.windwardAerodynamics$getChordNormal(state).getNormal()),Vec3.atLowerCornerOf(prov.windwardAerodynamics$getNormal(state).getNormal())));
            ((IAdvLiftSubLevel) this.getSubLevel()).windwardAerodynamics$markSpanGroupsDirty();
        }
    }

    @Inject(method = "onBlockChange", at = @At("HEAD"), remap = false)
    private void windwardAerodynamics$head$onBlockChange(BlockPos pos, BlockState state, CallbackInfo ci) {
        this.windwardAerodynamics$advLiftProviders.remove(pos.asLong());
    }
}
