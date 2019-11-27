package me.jellysquid.mods.phosphor.mixin.world.lighting;

import me.jellysquid.mods.phosphor.common.chunk.ExtendedBlockState;
import me.jellysquid.mods.phosphor.common.chunk.ExtendedLightEngine;
import me.jellysquid.mods.phosphor.common.util.cache.CachedChunkSectionAccess;
import net.minecraft.block.BlockState;
import net.minecraft.util.Direction;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.shapes.VoxelShape;
import net.minecraft.util.math.shapes.VoxelShapes;
import net.minecraft.world.LightType;
import net.minecraft.world.chunk.IChunkLightProvider;
import net.minecraft.world.lighting.LightDataMap;
import net.minecraft.world.lighting.LightEngine;
import net.minecraft.world.lighting.SectionLightStorage;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LightEngine.class)
public class MixinLightEngine<M extends LightDataMap<M>, S extends SectionLightStorage<M>> implements ExtendedLightEngine<S> {
    @Shadow
    @Final
    protected BlockPos.MutableBlockPos scratchPos;

    @Shadow
    @Final
    protected IChunkLightProvider chunkProvider;

    @Shadow @Final protected S storage;
    private CachedChunkSectionAccess cacher;

    @Inject(method = "<init>", at = @At("RETURN"))
    private void onConstructed(IChunkLightProvider provider, LightType lightType, S storage, CallbackInfo ci) {
        this.cacher = new CachedChunkSectionAccess(provider);
    }

    @Inject(method = "invalidateCaches", at = @At("RETURN"))
    private void onCleanup(CallbackInfo ci) {
        // This callback may be executed from the constructor above, and the object won't be initialized then
        if (this.cacher != null) {
            this.cacher.cleanup();
        }
    }

    // [VanillaCopy] getBlockAndOpacity
    @Override
    public BlockState getBlockStateForLighting(int x, int y, int z) {
        return this.cacher.getBlockState(x, y, z);
    }

    // [VanillaCopy] getBlockAndOpacity
    @Override
    public int getSubtractedLight(BlockState state, int x, int y, int z) {
        return state.getOpacity(this.chunkProvider.getWorld(), this.scratchPos.setPos(x, y, z));
    }

    // [VanillaCopy] getVoxelShape
    @Override
    public VoxelShape getVoxelShape(BlockState state, int x, int y, int z, Direction dir) {
        ExtendedBlockState estate = ((ExtendedBlockState) state);

        if (state.isSolid() && state.func_215691_g()) {
            if (estate.hasDynamicShape()) {
                return estate.getDynamicLightShape(this.chunkProvider.getWorld(), this.scratchPos.setPos(x, y, z), dir);
            } else {
                return estate.getStaticLightShape(dir);
            }
        } else {
            return VoxelShapes.empty();
        }
    }

    // [VanillaCopy] getBlockAndOpacity
    @Override
    public VoxelShape getVoxelShape(int x, int y, int z, Direction dir) {
        BlockState state = this.cacher.getBlockState(x, y, z);

        if (state == null) {
            return VoxelShapes.fullCube();
        }

        return this.getVoxelShape(state, x, y, z, dir);
    }

    @Override
    public S getStorage() {
        return this.storage;
    }
}
