package me.jellysquid.mods.phosphor.mixin.world.lighting;

import me.jellysquid.mods.phosphor.common.chunk.ExtendedLevelBasedGraph;
import me.jellysquid.mods.phosphor.common.chunk.ExtendedLightEngine;
import me.jellysquid.mods.phosphor.common.chunk.ExtendedSectionLightStorage;
import me.jellysquid.mods.phosphor.common.util.PhosphorDirection;
import net.minecraft.block.BlockState;
import net.minecraft.util.Direction;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.SectionPos;
import net.minecraft.util.math.shapes.VoxelShape;
import net.minecraft.util.math.shapes.VoxelShapes;
import net.minecraft.world.lighting.BlockLightEngine;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

import static net.minecraft.util.math.SectionPos.toChunk;

@Mixin(BlockLightEngine.class)
public abstract class MixinBlockLightEngine {
    @Shadow
    protected abstract int getLightValue(long worldPos);

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
        } else if (a == Long.MAX_VALUE) {
            return level + 15 - this.getLightValue(b);
        } else if (level >= 15) {
            return level;
        }

        int bX = BlockPos.unpackX(b);
        int bY = BlockPos.unpackY(b);
        int bZ = BlockPos.unpackZ(b);

        int aX = BlockPos.unpackX(a);
        int aY = BlockPos.unpackY(a);
        int aZ = BlockPos.unpackZ(a);

        Direction dir = PhosphorDirection.getVecDirection(bX - aX, bY - aY, bZ - aZ);

        if (dir != null) {
            BlockState bState = ((ExtendedLightEngine) this).getBlockStateForLighting(bX, bY, bZ);

            if (bState == null) {
                return 15;
            }

            int newLevel = ((ExtendedLightEngine) this).getSubtractedLight(bState, bX, bY, bZ);

            if (newLevel >= 15) {
                return 15;
            }

            VoxelShape bShape = ((ExtendedLightEngine) this).getVoxelShape(bState, bX, bY, bZ, dir.getOpposite());
            VoxelShape aShape = ((ExtendedLightEngine) this).getVoxelShape(aX, aY, aZ, dir);

            if (!VoxelShapes.func_223416_b(aShape, bShape)) {
                return level + Math.max(1, newLevel);
            }
        }

        return 15;
    }

    /**
     * Avoids constantly (un)packing coordinates. This strictly copies vanilla's implementation.
     *
     * @author JellySquid
     */
    @Overwrite
    public void notifyNeighbors(long pos, int level, boolean isDecreasing) {
        int x = BlockPos.unpackX(pos);
        int y = BlockPos.unpackY(pos);
        int z = BlockPos.unpackZ(pos);

        long chunk = SectionPos.asLong(toChunk(x), toChunk(y), toChunk(z));

        for (Direction dir : DIRECTIONS) {
            int adjX = x + dir.getXOffset();
            int adjY = y + dir.getYOffset();
            int adjZ = z + dir.getZOffset();

            long adjChunk = SectionPos.asLong(toChunk(adjX), toChunk(adjY), toChunk(adjZ));

            if ((chunk == adjChunk) || ((ExtendedSectionLightStorage) ((ExtendedLightEngine) this).getStorage()).bridge$hasChunk(adjChunk)) {
                ((ExtendedLevelBasedGraph) this).bridge$propagateLevel(pos, BlockPos.pack(adjX, adjY, adjZ), level, isDecreasing);
            }
        }
    }
}
