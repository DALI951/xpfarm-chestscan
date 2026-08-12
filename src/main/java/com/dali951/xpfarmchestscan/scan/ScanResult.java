package com.dali951.xpfarmchestscan.scan;

import net.minecraft.core.BlockPos;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

public class ScanResult {

    public final List<ChestEntry> chests = new ArrayList<>();
    public final Map<String, Integer> totals = new TreeMap<>();
    public final List<BlockPos> skipped = new ArrayList<>();
    public long generatedAt = System.currentTimeMillis();

    public void add(ChestEntry entry) {
        chests.add(entry);
        for (Map.Entry<String, Integer> e : entry.items.entrySet()) {
            totals.merge(e.getKey(), e.getValue(), Integer::sum);
        }
    }

    public int totalItems() {
        int total = 0;
        for (int c : totals.values()) {
            total += c;
        }
        return total;
    }
}