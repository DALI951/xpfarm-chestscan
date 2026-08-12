package com.dali951.xpfarmchestscan.scan;

import com.dali951.xpfarmchestscan.config.ConfigStore;
import com.dali951.xpfarmchestscan.gui.ChecklistScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.SlabType;
import net.minecraft.world.level.chunk.ChunkSource;
import net.minecraft.world.level.chunk.LevelChunk;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PlacedBlockScanner {

    public static final PlacedBlockScanner INSTANCE = new PlacedBlockScanner();

    private static final int RANGE_CHUNKS = 6;
    private static final int CHUNKS_PER_TICK = 3;

    private final Map<String, List<String>> targets = new HashMap<>();
    private final Deque<Long> queue = new ArrayDeque<>();
    private final Map<String, Integer> counts = new HashMap<>();
    private boolean scanning = false;
    private int scannedBlocks = 0;

    private PlacedBlockScanner() {
        targets.put("minecraft:stone_bricks", List.of("stone_bricks"));
        targets.put("minecraft:stone_brick_stairs", List.of("stone_brick_stairs"));
        targets.put("minecraft:stone_brick_slab", List.of("stone_brick_slab"));
        targets.put("minecraft:dark_oak_trapdoor", List.of("dark_oak_trapdoor"));
        targets.put("minecraft:white_carpet", List.of("white_carpet"));
        targets.put("minecraft:dark_oak_planks", List.of("dark_oak_planks"));
        targets.put("minecraft:glass", List.of("glass"));
        targets.put("minecraft:ladder", List.of("ladder"));
        targets.put("minecraft:dark_oak_log", List.of("dark_oak_log"));
        targets.put("minecraft:dark_oak_fence", List.of("dark_oak_fence"));
        targets.put("minecraft:chest", List.of("chest"));
        targets.put("minecraft:hopper", List.of("hopper"));
        targets.put("minecraft:torch", List.of("torch", "wall_torch"));
        targets.put("minecraft:redstone", List.of("redstone_wire"));
        targets.put("minecraft:dark_oak_sign", List.of("dark_oak_sign", "dark_oak_wall_sign"));
        targets.put("minecraft:bookshelf", List.of("bookshelf"));
        targets.put("minecraft:comparator", List.of("comparator"));
        targets.put("minecraft:repeater", List.of("repeater"));
        targets.put("minecraft:campfire", List.of("campfire"));
        targets.put("minecraft:redstone_torch", List.of("redstone_torch", "redstone_wall_torch"));
        targets.put("minecraft:enchanting_table", List.of("enchanting_table"));
        targets.put("minecraft:anvil", List.of("anvil", "chipped_anvil", "damaged_anvil"));
        targets.put("minecraft:dropper", List.of("dropper"));
    }

    public boolean isScanning() {
        return scanning;
    }

    public void start() {
        Minecraft mc = Minecraft.getInstance();
        if (scanning) {
            return;
        }
        if (mc.player == null || mc.level == null) {
            sendMessage(mc, Component.literal("You must be in a world to scan placed blocks."));
            return;
        }
        queue.clear();
        counts.clear();
        scannedBlocks = 0;
        BlockPos p = mc.player.blockPosition();
        int pcx = p.getX() >> 4;
        int pcz = p.getZ() >> 4;
        ChunkSource src = mc.level.getChunkSource();
        for (int cx = pcx - RANGE_CHUNKS; cx <= pcx + RANGE_CHUNKS; cx++) {
            for (int cz = pcz - RANGE_CHUNKS; cz <= pcz + RANGE_CHUNKS; cz++) {
                if (src.hasChunk(cx, cz)) {
                    queue.addLast((((long) cx) << 32) | (cz & 0xFFFFFFFFL));
                }
            }
        }
        if (queue.isEmpty()) {
            sendMessage(mc, Component.literal("No loaded chunks nearby — stand near the farm and try again."));
            return;
        }
        scanning = true;
        sendMessage(mc, Component.literal("Scanning placed farm blocks around you (" + queue.size() + " chunks)..."));
    }

    public void tick() {
        if (!scanning) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.getConnection() == null) {
            scanning = false;
            return;
        }
        int done = 0;
        while (done < CHUNKS_PER_TICK && !queue.isEmpty()) {
            long packed = queue.removeFirst();
            scanChunk(mc, (int) (packed >> 32), (int) packed);
            done++;
        }
        if (queue.isEmpty()) {
            finish(mc);
        }
    }

    private void scanChunk(Minecraft mc, int cx, int cz) {
        ChunkSource src = mc.level.getChunkSource();
        if (!src.hasChunk(cx, cz)) {
            return;
        }
        LevelChunk chunk = mc.level.getChunk(cx, cz);
        int minY = mc.level.getMinY();
        int maxY = mc.level.getMaxY();
        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                for (int y = minY; y < maxY; y++) {
                    BlockState state = chunk.getBlockState(new BlockPos(x, y, z));
                    if (state.isAir()) {
                        continue;
                    }
                    Identifier key = BuiltInRegistries.BLOCK.getKey(state.getBlock());
                    if (key == null || !"minecraft".equals(key.getNamespace())) {
                        continue;
                    }
                    String path = key.getPath();
                    for (Map.Entry<String, List<String>> t : targets.entrySet()) {
                        if (t.getValue().contains(path)) {
                            int n = 1;
                            if (state.getBlock() instanceof SlabBlock
                                    && state.getValue(SlabBlock.TYPE) == SlabType.DOUBLE) {
                                n = 2;
                            }
                            scannedBlocks++;
                            counts.merge(t.getKey(), n, Integer::sum);
                            break;
                        }
                    }
                }
            }
        }
    }

    private void finish(Minecraft mc) {
        scanning = false;
        ConfigStore.get().placed.clear();
        for (Map.Entry<String, Integer> e : counts.entrySet()) {
            if (e.getValue() > 0) {
                ConfigStore.get().placed.put(e.getKey(), e.getValue());
            }
        }
        ConfigStore.save();
        sendMessage(mc, Component.literal("Placed scan done: " + scannedBlocks
                + " farm block" + (scannedBlocks == 1 ? "" : "s") + " found in loaded chunks."));
        if (mc.gui.screen() instanceof ChecklistScreen screen) {
            screen.refresh();
        }
    }

    private void sendMessage(Minecraft mc, Component message) {
        if (mc.player != null) {
            mc.player.sendSystemMessage(message);
        }
    }
}
