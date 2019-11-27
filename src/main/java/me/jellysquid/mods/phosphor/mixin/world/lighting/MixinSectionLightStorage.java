package me.jellysquid.mods.phosphor.mixin.world.lighting;

import it.unimi.dsi.fastutil.longs.LongSet;
import me.jellysquid.mods.phosphor.common.chunk.ExtendedSectionLightStorage;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.SectionPos;
import net.minecraft.world.chunk.NibbleArray;
import net.minecraft.world.lighting.LightDataMap;
import net.minecraft.world.lighting.SectionLightStorage;
import org.spongepowered.asm.mixin.*;

@Mixin(SectionLightStorage.class)
public abstract class MixinSectionLightStorage<M extends LightDataMap<M>> implements ExtendedSectionLightStorage<M> {
    @Shadow
    @Final
    protected M cachedLightData;

    @Mutable
    @Shadow
    @Final
    protected LongSet dirtyCachedSections;

    @Shadow
    protected abstract NibbleArray getArray(long sectionPos, boolean cached);

    @Mutable
    @Shadow
    @Final
    protected LongSet changedLightPositions;

    @Shadow
    protected abstract boolean hasSection(long sectionPos);

    @Shadow
    protected abstract int getLevel(long sectionPos);

    @Mutable
    @Shadow
    @Final
    protected LongSet activeLightSections;

    @Mutable
    @Shadow
    @Final
    protected LongSet addedActiveLightSections;

    @Mutable
    @Shadow
    @Final
    protected LongSet addedEmptySections;

    @Mutable
    @Shadow
    @Final
    private LongSet noLightSections;

    @Shadow
    protected abstract void func_215524_j(long long_1);

    @SuppressWarnings("unused")
    @Shadow
    protected volatile boolean hasSectionsToUpdate;

    @Shadow
    protected volatile M uncachedLightData;

    @Shadow
    protected abstract NibbleArray getArray(M storage, long pos);

    @Shadow protected abstract NibbleArray getOrCreateArray(long sectionPosIn);

    /**
     * Replaces the two set of calls to unpack the XYZ coordinates from the input to just one, storing the result as local
     * variables.
     *
     * @author JellySquid
     */
    @Overwrite
    public int getLight(long pos) {
        int x = BlockPos.unpackX(pos);
        int y = BlockPos.unpackY(pos);
        int z = BlockPos.unpackZ(pos);

        long chunk = SectionPos.asLong(SectionPos.toChunk(x), SectionPos.toChunk(y), SectionPos.toChunk(z));

        NibbleArray array = this.getArray(chunk, true);

        return array.get(SectionPos.mask(x), SectionPos.mask(y), SectionPos.mask(z));
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
    public void setLight(long pos, int level) {
        int x = BlockPos.unpackX(pos);
        int y = BlockPos.unpackY(pos);
        int z = BlockPos.unpackZ(pos);

        long chunkPos = SectionPos.asLong(x >> 4, y >> 4, z >> 4);

        if (this.dirtyCachedSections.add(chunkPos)) {
            this.cachedLightData.copyArray(chunkPos);
        }

        NibbleArray nibble = this.getArray(chunkPos, true);
        nibble.set(x & 15, y & 15, z & 15, level);

        for (int z2 = (z - 1) >> 4; z2 <= (z + 1) >> 4; ++z2) {
            for (int x2 = (x - 1) >> 4; x2 <= (x + 1) >> 4; ++x2) {
                for (int y2 = (y - 1) >> 4; y2 <= (y + 1) >> 4; ++y2) {
                    this.changedLightPositions.add(SectionPos.asLong(x2, y2, z2));
                }
            }
        }
    }

    /**
     * Combines the contains/remove call to the queued removals set into a single remove call. See {@link MixinSectionLightStorage#set(long, int)}
     * for additional information.
     *
     * @author JellySquid
     */
    @Overwrite
    public void setLevel(long pos, int level) {
        int prevLevel = this.getLevel(pos);

        if (prevLevel != 0 && level == 0) {
            this.activeLightSections.add(pos);
            this.addedActiveLightSections.remove(pos);
        }

        if (prevLevel == 0 && level != 0) {
            this.activeLightSections.remove(pos);
            this.addedEmptySections.remove(pos);
        }

        if (prevLevel >= 2 && level != 2) {
            if (!this.noLightSections.remove(pos)) {
                this.cachedLightData.setArray(pos, this.getOrCreateArray(pos));

                this.dirtyCachedSections.add(pos);
                this.func_215524_j(pos);

                int x = BlockPos.unpackX(pos);
                int y = BlockPos.unpackY(pos);
                int z = BlockPos.unpackZ(pos);

                for (int z2 = (z - 1) >> 4; z2 <= (z + 1) >> 4; ++z2) {
                    for (int x2 = (x - 1) >> 4; x2 <= (x + 1) >> 4; ++x2) {
                        for (int y2 = (y - 1) >> 4; y2 <= (y + 1) >> 4; ++y2) {
                            this.changedLightPositions.add(SectionPos.asLong(x2, y2, z2));
                        }
                    }
                }
            }
        }

        if (prevLevel != 2 && level >= 2) {
            this.noLightSections.add(pos);
        }

        this.hasSectionsToUpdate = !this.noLightSections.isEmpty();
    }


    @Override
    public boolean bridge$hasChunk(long pos) {
        return this.hasSection(pos);
    }

    @Override
    public NibbleArray bridge$getDataForChunk(M data, long chunk) {
        return this.getArray(data, chunk);
    }

    @Override
    public M bridge$getStorageUncached() {
        return this.uncachedLightData;
    }
}
