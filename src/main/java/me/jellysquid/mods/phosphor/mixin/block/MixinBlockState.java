package me.jellysquid.mods.phosphor.mixin.block;

import me.jellysquid.mods.phosphor.common.chunk.ExtendedBlockState;
import me.jellysquid.mods.phosphor.common.chunk.PhosphorBlockStateCache;
import net.minecraft.block.BlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.Direction;
import net.minecraft.util.math.shapes.VoxelShape;
import net.minecraft.util.math.shapes.VoxelShapes;
import net.minecraft.world.IBlockReader;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(BlockState.class)
public abstract class MixinBlockState implements ExtendedBlockState {
    private boolean shouldFetchCullState;

    @Shadow
    public abstract VoxelShape getShape(IBlockReader reader, BlockPos pos);

    @Shadow
    public abstract boolean isSolid();

    @Shadow
    public abstract boolean func_215691_g();

    private PhosphorBlockStateCache phosphorBlockStateCache;

    @Inject(method = "cacheState", at = @At(value = "RETURN"))
    private void onConstructed(CallbackInfo ci) {
        this.phosphorBlockStateCache = new PhosphorBlockStateCache(((BlockState) (Object) this));
        this.shouldFetchCullState = this.isSolid() && this.func_215691_g();
    }

    @Override
    public boolean hasDynamicShape() {
        return this.phosphorBlockStateCache == null;
    }

    @Override
    public boolean hasSpecialLightingShape() {
        return this.shouldFetchCullState;
    }

    @Override
    public VoxelShape getStaticLightShape(Direction dir) {
        return this.phosphorBlockStateCache.shapes[dir.ordinal()];
    }

    @Override
    public VoxelShape getDynamicLightShape(IBlockReader view, BlockPos pos, Direction dir) {
        return VoxelShapes.func_216387_a(this.getShape(view, pos), dir);
    }
}
