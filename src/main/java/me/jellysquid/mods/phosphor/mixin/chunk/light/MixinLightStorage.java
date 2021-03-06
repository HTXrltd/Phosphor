package me.jellysquid.mods.phosphor.mixin.chunk.light;

import it.unimi.dsi.fastutil.longs.LongSet;
import me.jellysquid.mods.phosphor.common.chunk.ExtendedLightStorage;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkSectionPos;
import net.minecraft.world.chunk.ChunkNibbleArray;
import net.minecraft.world.chunk.WorldNibbleStorage;
import net.minecraft.world.chunk.light.LightStorage;
import org.spongepowered.asm.mixin.*;

@Mixin(LightStorage.class)
public abstract class MixinLightStorage<M extends WorldNibbleStorage<M>> implements ExtendedLightStorage<M> {
    @Shadow
    @Final
    protected M dataStorage;

    @Mutable
    @Shadow
    @Final
    protected LongSet field_15802;

    @Shadow
    protected abstract ChunkNibbleArray getDataForChunk(long chunkPos, boolean cached);

    @Mutable
    @Shadow
    @Final
    protected LongSet toNotify;

    @Shadow
    protected abstract boolean hasChunk(long chunkPos);

    @Shadow
    protected abstract int getLevel(long id);

    @Mutable
    @Shadow
    @Final
    protected LongSet field_15808;

    @Mutable
    @Shadow
    @Final
    protected LongSet field_15804;

    @Mutable
    @Shadow
    @Final
    protected LongSet field_15797;

    @Mutable
    @Shadow
    @Final
    private LongSet toRemove;

    @Shadow
    protected abstract void method_15523(long blockPos);

    @Shadow
    protected abstract ChunkNibbleArray getDataForChunk(long chunkPos);

    @SuppressWarnings("unused")
    @Shadow
    protected volatile boolean hasLightUpdates;

    @Shadow
    protected volatile M dataStorageUncached;

    @Shadow
    protected abstract ChunkNibbleArray getDataForChunk(M storage, long pos);

    /**
     * Replaces the two set of calls to unpack the XYZ coordinates from the input to just one, storing the result as local
     * variables.
     *
     * @author JellySquid
     */
    @Overwrite
    public int get(long blockPos) {
        int x = BlockPos.unpackLongX(blockPos);
        int y = BlockPos.unpackLongY(blockPos);
        int z = BlockPos.unpackLongZ(blockPos);

        long chunk = ChunkSectionPos.asLong(ChunkSectionPos.toChunkCoord(x), ChunkSectionPos.toChunkCoord(y), ChunkSectionPos.toChunkCoord(z));

        ChunkNibbleArray array = this.getDataForChunk(chunk, true);

        return array.get(ChunkSectionPos.toLocalCoord(x), ChunkSectionPos.toLocalCoord(y), ChunkSectionPos.toLocalCoord(z));
    }

    /**
     * An extremely important optimization is made here in regards to adding items to the pending notification set. The
     * original implementation attempts to add the coordinate of every chunk which contains a neighboring block position
     * even though a huge number of loop iterations will simply map to block positions within the same updating chunk.
     * <p>
     * Our implementation here avoids this by pre-calculating the min/max chunk coordinates so we can iterate over only
     * the relevant chunk positions once. This reduces what would always be 27 iterations to just 1-8 iterations.
     *
     * @author JellySquid
     */
    @Overwrite
    public void set(long blockPos, int value) {
        int x = BlockPos.unpackLongX(blockPos);
        int y = BlockPos.unpackLongY(blockPos);
        int z = BlockPos.unpackLongZ(blockPos);

        long chunkPos = ChunkSectionPos.asLong(x >> 4, y >> 4, z >> 4);

        if (this.field_15802.add(chunkPos)) {
            this.dataStorage.cloneChunkData(chunkPos);
        }

        ChunkNibbleArray nibble = this.getDataForChunk(chunkPos, true);
        nibble.set(x & 15, y & 15, z & 15, value);

        for (int z2 = (z - 1) >> 4; z2 <= (z + 1) >> 4; ++z2) {
            for (int x2 = (x - 1) >> 4; x2 <= (x + 1) >> 4; ++x2) {
                for (int y2 = (y - 1) >> 4; y2 <= (y + 1) >> 4; ++y2) {
                    this.toNotify.add(ChunkSectionPos.asLong(x2, y2, z2));
                }
            }
        }
    }

    /**
     * Combines the contains/remove call to the queued removals set into a single remove call. See {@link MixinLightStorage#set(long, int)}
     * for additional information.
     *
     * @author JellySquid
     */
    @Overwrite
    public void setLevel(long id, int level) {
        int prevLevel = this.getLevel(id);

        if (prevLevel != 0 && level == 0) {
            this.field_15808.add(id);
            this.field_15804.remove(id);
        }

        if (prevLevel == 0 && level != 0) {
            this.field_15808.remove(id);
            this.field_15797.remove(id);
        }

        if (prevLevel >= 2 && level != 2) {
            if (!this.toRemove.remove(id)) {
                this.dataStorage.addForChunk(id, this.getDataForChunk(id));

                this.field_15802.add(id);
                this.method_15523(id);

                int x = BlockPos.unpackLongX(id);
                int y = BlockPos.unpackLongY(id);
                int z = BlockPos.unpackLongZ(id);

                for (int z2 = (z - 1) >> 4; z2 <= (z + 1) >> 4; ++z2) {
                    for (int x2 = (x - 1) >> 4; x2 <= (x + 1) >> 4; ++x2) {
                        for (int y2 = (y - 1) >> 4; y2 <= (y + 1) >> 4; ++y2) {
                            this.toNotify.add(ChunkSectionPos.asLong(x2, y2, z2));
                        }
                    }
                }

                this.hasLightUpdates = !this.toRemove.isEmpty();
            }
        }

        if (prevLevel != 2 && level >= 2) {
            this.toRemove.add(id);
            this.hasLightUpdates = !this.toRemove.isEmpty();
        }
    }


    @Override
    public boolean bridge$hasChunk(long chunkPos) {
        return this.hasChunk(chunkPos);
    }

    @Override
    public ChunkNibbleArray bridge$getDataForChunk(M data, long chunkPos) {
        return this.getDataForChunk(data, chunkPos);
    }

    @Override
    public M bridge$getStorageUncached() {
        return this.dataStorageUncached;
    }
}
