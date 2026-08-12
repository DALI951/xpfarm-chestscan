package com.dali951.xpfarmchestscan.checklist;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class ChecklistData {

    public record Goal(String id, String label, int needed) {
    }

    public static final List<Goal> GOALS = List.of(
            new Goal("minecraft:stone_bricks", "Stone bricks", 6978),
            new Goal("minecraft:stone_brick_stairs", "Stone brick stairs", 5508),
            new Goal("minecraft:stone_brick_slab", "Stone brick slab", 1584),
            new Goal("minecraft:dark_oak_trapdoor", "Dark oak trapdoors", 776),
            new Goal("minecraft:white_carpet", "White carpet", 720),
            new Goal("minecraft:dark_oak_planks", "Dark oak planks", 373),
            new Goal("minecraft:glass", "Glass", 214),
            new Goal("minecraft:ladder", "Ladders", 134),
            new Goal("minecraft:dark_oak_log", "Dark oak logs", 92),
            new Goal("minecraft:dark_oak_fence", "Dark oak fences", 78),
            new Goal("minecraft:chest", "Chests", 44),
            new Goal("minecraft:hopper", "Hoppers", 41),
            new Goal("minecraft:torch", "Torches", 27),
            new Goal("minecraft:redstone", "Redstone dust", 17),
            new Goal("minecraft:dark_oak_sign", "Dark oak signs", 16),
            new Goal("minecraft:bookshelf", "Bookshelves", 15),
            new Goal("minecraft:comparator", "Comparators", 5),
            new Goal("minecraft:repeater", "Repeaters", 5),
            new Goal("minecraft:campfire", "Campfires", 4),
            new Goal("minecraft:redstone_torch", "Redstone torches", 4),
            new Goal("minecraft:enchanting_table", "Enchanting table", 1),
            new Goal("minecraft:anvil", "Anvil", 1),
            new Goal("minecraft:dropper", "Dropper", 1),
            new Goal("minecraft:lava_bucket", "Lava bucket", 1),
            new Goal("minecraft:water_bucket", "Water sources", 92)
    );

    private static final Map<String, Goal> BY_ID = new LinkedHashMap<>();

    static {
        for (Goal g : GOALS) {
            BY_ID.put(g.id(), g);
        }
    }

    private ChecklistData() {
    }

    public static int needed(String id) {
        Goal g = BY_ID.get(id);
        return g != null ? g.needed() : 0;
    }

    public static int totalNeeded() {
        int total = 0;
        for (Goal g : GOALS) {
            total += g.needed();
        }
        return total;
    }

    public static String label(String id) {
        Goal g = BY_ID.get(id);
        return g != null ? g.label() : id.replace("minecraft:", "");
    }
}