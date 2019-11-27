package me.jellysquid.mods.phosphor.common.util;

import net.minecraft.util.math.SectionPos;

public class SectionPosHelper {
    /**
     * Quicker than re-encoding an integer {@link SectionPos} when you only need to update one coordinate.
     * @param pos The integer position containing the old X/Z coordinate values
     * @param y The new y-coordinate to update {@param pos} with
     * @return A new integer SectionPos which is identical to SectionPos.asLong(pos.x, y, pos.z)
     */
    public static long updateYLong(long pos, int y) {
        // [VanillaCopy] SectionPos static fields
        return (pos & ~0xFFFFF) | ((long) y & 0xFFFFF);
    }
}
