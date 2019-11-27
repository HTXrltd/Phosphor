package me.jellysquid.mods.phosphor.common.chunk;

import net.minecraft.block.BlockState;
import net.minecraft.util.Direction;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.shapes.VoxelShape;
import net.minecraft.util.math.shapes.VoxelShapes;
import net.minecraft.world.EmptyBlockReader;

/**
 * We can't access the package-private BlockState cache, so we re-implement a small part here.
 */
public class PhosphorBlockStateCache {
    private static final Direction[] DIRECTIONS = Direction.values();

    public final VoxelShape[] shapes;

    public PhosphorBlockStateCache(BlockState state) {
        this.shapes = new VoxelShape[DIRECTIONS.length];

        if (state.isSolid()) {
            VoxelShape shape = state.getBlock().getRenderShape(state, EmptyBlockReader.INSTANCE, BlockPos.ZERO);

            for (Direction dir : DIRECTIONS) {
                this.shapes[dir.ordinal()] = VoxelShapes.func_216387_a(shape, dir);
            }
        }
    }
}
