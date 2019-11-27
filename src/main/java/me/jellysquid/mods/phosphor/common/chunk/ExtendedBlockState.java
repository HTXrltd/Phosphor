package me.jellysquid.mods.phosphor.common.chunk;

import net.minecraft.util.Direction;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.shapes.VoxelShape;
import net.minecraft.world.IBlockReader;

public interface ExtendedBlockState {
    /**
     * @return True if the block state has a static shape, otherwise false if it doesn't.
     */
    boolean hasDynamicShape();

    /**
     * @return True if the block state has a special state it is represented by for lighting.
     */
    boolean hasSpecialLightingShape();

    /**
     * @return The cached VoxelShape which represents the light volume in the specified direction.
     */
    VoxelShape getStaticLightShape(Direction dir);

    /**
     * Creates a new VoxelShape which represents the light volume for the block in the specified context. This
     * will not be cached.
     */
    VoxelShape getDynamicLightShape(IBlockReader view, BlockPos pos, Direction dir);
}
