package me.jellysquid.mods.phosphor.mixin.world.lighting;

import me.jellysquid.mods.phosphor.common.chunk.ExtendedSectionLightStorage;
import me.jellysquid.mods.phosphor.common.chunk.ExtendedSkyLightStorage;
import me.jellysquid.mods.phosphor.common.chunk.ExtendedSkyLightStorageMap;
import me.jellysquid.mods.phosphor.common.util.SectionPosHelper;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.SectionPos;
import net.minecraft.world.chunk.NibbleArray;
import net.minecraft.world.lighting.SkyLightStorage;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(SkyLightStorage.class)
public abstract class MixinSkyLightStorage implements ExtendedSkyLightStorage {
    @Shadow
    protected abstract boolean func_215551_l(long long_1);

    @Shadow
    protected abstract boolean func_215550_a(int int_1);

    @Override
    public boolean bridge$func_215551_l(long long_1) {
        return this.func_215551_l(long_1);
    }

    @Override
    public boolean bridge$isAboveMinimumHeight(int y) {
        return this.func_215550_a(y);
    }

    /**
     * An optimized implementation which avoids constantly unpacking and repacking integer coordinates.
     *
     * @author JellySquid
     */
    @SuppressWarnings({"ConstantConditions", "unchecked"})
    @Overwrite
    public int getLightOrDefault(long pos) {
        int posX = BlockPos.unpackX(pos);
        int posY = BlockPos.unpackY(pos);
        int posZ = BlockPos.unpackZ(pos);

        int chunkX = SectionPos.toChunk(posX);
        int chunkY = SectionPos.toChunk(posY);
        int chunkZ = SectionPos.toChunk(posZ);

        long chunk = SectionPos.asLong(chunkX, chunkY, chunkZ);

        SkyLightStorage.StorageMap data = (SkyLightStorage.StorageMap) ((ExtendedSectionLightStorage) this).bridge$getStorageUncached();

        int h = ((ExtendedSkyLightStorageMap) (Object) data).bridge$heightMap().get(SectionPos.toSectionColumnPos(chunk));

        if (h != ((ExtendedSkyLightStorageMap) (Object) data).bridge$defaultHeight() && chunkY < h) {
            NibbleArray array = ((ExtendedSectionLightStorage) this).bridge$getDataForChunk(data, chunk);

            if (array == null) {
                posY &= ~16;

                while (array == null) {
                    ++chunkY;

                    if (chunkY >= h) {
                        return 15;
                    }

                    chunk = SectionPosHelper.updateYLong(chunk, chunkY);
                    posY += 16;
                    array = ((ExtendedSectionLightStorage) this).bridge$getDataForChunk(data, chunk);
                }
            }

            return array.get(
                    SectionPos.mask(posX),
                    SectionPos.mask(posY),
                    SectionPos.mask(posZ)
            );
        } else {
            return 15;
        }
    }
}
