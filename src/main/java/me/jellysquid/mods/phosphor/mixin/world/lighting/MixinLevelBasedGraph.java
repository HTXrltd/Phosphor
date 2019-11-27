package me.jellysquid.mods.phosphor.mixin.world.lighting;

import me.jellysquid.mods.phosphor.common.chunk.ExtendedLevelBasedGraph;
import net.minecraft.world.lighting.LevelBasedGraph;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(LevelBasedGraph.class)
public abstract class MixinLevelBasedGraph implements ExtendedLevelBasedGraph {
    @Shadow protected abstract void propagateLevel(long fromPos, long toPos, int sourceLevel, boolean isDecreasing);

    @Override
    public void bridge$propagateLevel(long fromPos, long toPos, int sourceLevel, boolean isDecreasing) {
        this.propagateLevel(fromPos, toPos, sourceLevel, isDecreasing);
    }
}
