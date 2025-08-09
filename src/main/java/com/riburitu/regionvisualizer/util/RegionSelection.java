// RegionSelection.java

package com.riburitu.regionvisualizer.util;

import net.minecraft.core.BlockPos;

public class RegionSelection {
    public BlockPos pos1;
    public BlockPos pos2;

    public void setPos1(BlockPos pos) {
        this.pos1 = pos;
    }

    public void setPos2(BlockPos pos) {
        this.pos2 = pos;
    }

    public boolean isComplete() {
        return pos1 != null && pos2 != null;
    }
}
