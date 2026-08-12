package com.dali951.xpfarmchestscan.scan;

import net.minecraft.core.BlockPos;

import java.util.Map;
import java.util.TreeMap;

public class ChestEntry {

    public final BlockPos pos;
    public final Map<String, Integer> items = new TreeMap<>();

    public ChestEntry(BlockPos pos) {
        this.pos = pos;
    }

    public void addItem(String id, int count) {
        items.merge(id, count, Integer::sum);
    }

    public int totalItems() {
        int total = 0;
        for (int c : items.values()) {
            total += c;
        }
        return total;
    }
}