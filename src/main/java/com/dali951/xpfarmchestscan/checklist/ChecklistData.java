package com.dali951.xpfarmchestscan.checklist;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class ChecklistData {

    public record Goal(String id, String label, int needed) {
    }

    public record GatherGoal(String id, String label, int needed, List<String> matchIds) {
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

    private static final List<String> ALL_LOG_IDS = List.of(
            "minecraft:oak_log", "minecraft:spruce_log", "minecraft:birch_log",
            "minecraft:jungle_log", "minecraft:acacia_log", "minecraft:dark_oak_log",
            "minecraft:mangrove_log", "minecraft:cherry_log", "minecraft:pale_oak_log",
            "minecraft:crimson_stem", "minecraft:warped_stem"
    );

    public static final List<GatherGoal> GATHER = List.of(
            new GatherGoal("minecraft:stone", "Stone (silk touch)", 16032, List.of("minecraft:stone")),
            new GatherGoal("minecraft:dark_oak_log", "Dark oak logs", 716, List.of("minecraft:dark_oak_log")),
            new GatherGoal("logs_any", "Wood logs (any)", 260, ALL_LOG_IDS),
            new GatherGoal("minecraft:white_wool", "White wool", 480, List.of("minecraft:white_wool")),
            new GatherGoal("minecraft:iron_ingot", "Iron ingots", 285, List.of("minecraft:iron_ingot")),
            new GatherGoal("minecraft:redstone", "Redstone dust", 60, List.of("minecraft:redstone")),
            new GatherGoal("minecraft:quartz", "Nether quartz", 5, List.of("minecraft:quartz")),
            new GatherGoal("minecraft:coal", "Coal", 60, List.of("minecraft:coal")),
            new GatherGoal("minecraft:sand", "Sand", 214, List.of("minecraft:sand")),
            new GatherGoal("minecraft:obsidian", "Obsidian", 4, List.of("minecraft:obsidian")),
            new GatherGoal("minecraft:diamond", "Diamonds", 2, List.of("minecraft:diamond")),
            new GatherGoal("minecraft:leather", "Leather", 45, List.of("minecraft:leather")),
            new GatherGoal("minecraft:sugar_cane", "Sugar cane", 45, List.of("minecraft:sugar_cane")),
            new GatherGoal("minecraft:water_bucket", "Water buckets", 3, List.of("minecraft:water_bucket")),
            new GatherGoal("minecraft:lava_bucket", "Lava bucket", 1, List.of("minecraft:lava_bucket"))
    );

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

    public static int totalGather() {
        int total = 0;
        for (GatherGoal g : GATHER) {
            total += g.needed();
        }
        return total;
    }

    public static String label(String id) {
        Goal g = BY_ID.get(id);
        return g != null ? g.label() : id.replace("minecraft:", "");
    }
}