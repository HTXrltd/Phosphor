package me.jellysquid.mods.phosphor.mixin.world.lighting;

import me.jellysquid.mods.phosphor.common.chunk.ExtendedLevelBasedGraph;
import me.jellysquid.mods.phosphor.common.chunk.ExtendedLightEngine;
import me.jellysquid.mods.phosphor.common.chunk.ExtendedSectionLightStorage;
import me.jellysquid.mods.phosphor.common.chunk.ExtendedSkyLightStorage;
import me.jellysquid.mods.phosphor.common.util.BlockPosHelper;
import me.jellysquid.mods.phosphor.common.util.PhosphorDirection;
import me.jellysquid.mods.phosphor.common.util.SectionPosHelper;
import net.minecraft.block.BlockState;
import net.minecraft.util.Direction;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.SectionPos;
import net.minecraft.util.math.shapes.VoxelShape;
import net.minecraft.util.math.shapes.VoxelShapes;
import net.minecraft.world.lighting.SkyLightEngine;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

import static net.minecraft.util.math.SectionPos.toChunk;
import static net.minecraft.util.math.SectionPos.mask;

@Mixin(SkyLightEngine.class)
public abstract class MixinSkyLightEngine {
    @Shadow
    @Final
    private static Direction[] DIRECTIONS;

    /**
     * This breaks up the call to getBlockAndOpacity into smaller parts so we do not have to pass a mutable heap object
     * to the method in order to extract the light result. This has a few other advantages, allowing us to:
     * - Avoid the de-optimization that occurs from allocating and passing a heap object
     * - Avoid unpacking coordinates twice for both the call to getBlockAndOpacity and getVoxelShape.
     * - Avoid the the specific usage of AtomicInteger, which has additional overhead for the atomic get/set operations.
     * - Avoid checking if the checked block is opaque twice.
     * <p>
     * The rest of the implementation has been otherwise copied from vanilla, but is optimized to avoid constantly
     * (un)packing coordinates and to use an optimized direction lookup function.
     *
     * @author JellySquid
     */
    @Overwrite
    public int getEdgeLevel(long a, long b, int level) {
        if (b == Long.MAX_VALUE) {
            return 15;
        }

        if (a == Long.MAX_VALUE) {
            if (!((ExtendedSkyLightStorage) ((ExtendedLightEngine) this).getStorage()).bridge$func_215551_l(b)) {
                return 15;
            }

            level = 0;
        } else if (level >= 15) {
            return level;
        }

        int bX = BlockPos.unpackX(b);
        int bY = BlockPos.unpackY(b);
        int bZ = BlockPos.unpackZ(b);

        int aX = BlockPos.unpackX(a);
        int aY = BlockPos.unpackY(a);
        int aZ = BlockPos.unpackZ(a);

        boolean sameXZ = aX == bX && aZ == bZ;

        BlockState bState = ((ExtendedLightEngine) this).getBlockStateForLighting(bX, bY, bZ);

        if (bState == null) {
            return 15;
        }

        int newLight = ((ExtendedLightEngine) this).getSubtractedLight(bState, bX, bY, bZ);

        if (newLight >= 15) {
            return 15;
        }

        Direction dir;

        if (a == Long.MAX_VALUE) {
            dir = Direction.DOWN;
        } else {
            dir = PhosphorDirection.getVecDirection(bX - aX, bY - aY, bZ - aZ);
        }

        if (dir != null) {
            VoxelShape aShape = ((ExtendedLightEngine) this).getVoxelShape(aX, aY, aZ, dir);
            VoxelShape bShape = ((ExtendedLightEngine) this).getVoxelShape(bState, bX, bY, bZ, dir.getOpposite());

            if (VoxelShapes.func_223416_b(aShape, bShape)) {
                return 15;
            }
        } else {
            dir = PhosphorDirection.getVecDirection(bX - aX, sameXZ ? -1 : 0, bZ - aZ);

            if (dir == null) {
                return 15;
            }

            VoxelShape aShape = ((ExtendedLightEngine) this).getVoxelShape(aX, aY, aZ, Direction.DOWN);

            if (VoxelShapes.func_223416_b(aShape, VoxelShapes.empty())) {
                return 15;
            }

            VoxelShape bShape = ((ExtendedLightEngine) this).getVoxelShape(bState, bX, bY, bZ, dir.getOpposite());

            if (VoxelShapes.func_223416_b(VoxelShapes.empty(), bShape)) {
                return 15;
            }
        }

        if ((a == Long.MAX_VALUE || sameXZ && aY > bY) && level == 0 && newLight == 0) {
            return 0;
        } else {
            return level + Math.max(1, newLight);
        }
    }

    /**
     * A few key optimizations are made here, in particular:
     * - The code avoids un-packing coordinates as much as possible and stores the results into local variables.
     * - When necessary, coordinate re-packing is reduced to the minimum number of operations. Most of them can be reduced
     * to only updating the Y-coordinate versus re-computing the entire integer.
     * - Coordinate re-packing is removed where unnecessary (such as when only comparing the Y-coordinate of two positions)
     * <p>
     * This copies the vanilla implementation as close as possible.
     *
     * @author JellySquid
     */
    @Overwrite
    public void notifyNeighbors(long longPos, int level, boolean flag) {
        int posX = BlockPos.unpackX(longPos);
        int posY = BlockPos.unpackY(longPos);
        int posZ = BlockPos.unpackZ(longPos);

        int chunkY = toChunk(posY);

        long chunk = SectionPos.asLong(toChunk(posX), chunkY, toChunk(posZ));

        int n = 0;

        if (mask(posY) == 0) {
            while (((ExtendedSkyLightStorage) ((ExtendedLightEngine) this).getStorage()).bridge$isAboveMinimumHeight(toChunk(posY) - n - 1) &&
                    !((ExtendedSectionLightStorage) ((ExtendedLightEngine) this).getStorage()).bridge$hasChunk(SectionPosHelper.updateYLong(chunk, toChunk(posY + (-n - 1))))) {
                ++n;
            }
        }

        int nY = posY - 1 - (n * 16);
        int nChunkY = toChunk(nY);

        if (chunkY == nChunkY || ((ExtendedSectionLightStorage) ((ExtendedLightEngine) this).getStorage()).bridge$hasChunk(SectionPosHelper.updateYLong(chunk, nChunkY))) {
            ((ExtendedLevelBasedGraph) this).bridge$propagateLevel(longPos, BlockPosHelper.updateYLong(longPos, nY), level, flag);
        }

        int upChunkY = toChunk(posY + 1);

        if (chunkY == upChunkY || ((ExtendedSectionLightStorage) ((ExtendedLightEngine) this).getStorage()).bridge$hasChunk(SectionPosHelper.updateYLong(chunk, upChunkY))) {
            ((ExtendedLevelBasedGraph) this).bridge$propagateLevel(longPos, BlockPosHelper.updateYLong(longPos, posY + 1), level, flag);
        }

        for (Direction dir : DIRECTIONS) {
            int k = 0;

            int adjPosX = posX + dir.getXOffset();
            int adjPosZ = posZ + dir.getZOffset();

            while (true) {
                int adjPosY = posY - k;

                long adjChunkPos = SectionPos.asLong(toChunk(adjPosX), toChunk(adjPosY), toChunk(adjPosZ));

                if (adjChunkPos == chunk) {
                    ((ExtendedLevelBasedGraph) this).bridge$propagateLevel(longPos, BlockPos.pack(adjPosX, adjPosY, adjPosZ), level, flag);

                    break;
                }

                if (((ExtendedSectionLightStorage) ((ExtendedLightEngine) this).getStorage()).bridge$hasChunk(adjChunkPos)) {
                    ((ExtendedLevelBasedGraph) this).bridge$propagateLevel(longPos, BlockPos.pack(adjPosX, adjPosY, adjPosZ), level, flag);
                }

                ++k;

                if (k > n * 16) {
                    break;
                }
            }
        }

    }
}
