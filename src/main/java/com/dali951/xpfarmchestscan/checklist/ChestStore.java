package com.dali951.xpfarmchestscan.checklist;

import com.dali951.xpfarmchestscan.config.ConfigStore;
import com.dali951.xpfarmchestscan.scan.ChestEntry;
import com.dali951.xpfarmchestscan.scan.ScanResult;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

public final class ChestStore {

    public record StoredChest(BlockPos pos, Map<String, Integer> items, int total) {
    }

    private static final Map<String, Integer> stored = new TreeMap<>();
    private static final List<StoredChest> chests = new ArrayList<>();
    private static boolean loaded = false;

    private ChestStore() {
    }

    public static int get(String id) {
        return stored.getOrDefault(id, 0);
    }

    public static int sumOf(List<String> ids) {
        int total = 0;
        for (String id : ids) {
            total += get(id);
        }
        return total;
    }

    public static int totalStored() {
        int total = 0;
        for (int v : stored.values()) {
            total += v;
        }
        return total;
    }

    public static List<StoredChest> chests() {
        return chests;
    }

    public static void applyScan(ScanResult result) {
        stored.clear();
        stored.putAll(result.totals);
        chests.clear();
        for (ChestEntry entry : result.chests) {
            chests.add(new StoredChest(entry.pos, new TreeMap<>(entry.items), entry.totalItems()));
        }
        loaded = true;
    }

    public static void ensureLoaded(Minecraft mc) {
        if (loaded) {
            return;
        }
        try {
            Path path = mc.gameDirectory.toPath().resolve(ConfigStore.get().outputFileName);
            if (!Files.exists(path)) {
                return;
            }
            JsonObject root = JsonParser.parseString(Files.readString(path, StandardCharsets.UTF_8)).getAsJsonObject();
            if (root.has("totals") && root.get("totals").isJsonObject()) {
                for (Map.Entry<String, JsonElement> e : root.getAsJsonObject("totals").entrySet()) {
                    stored.put(e.getKey(), e.getValue().getAsInt());
                }
            }
            if (root.has("chests") && root.get("chests").isJsonArray()) {
                for (JsonElement el : root.getAsJsonArray("chests")) {
                    JsonObject c = el.getAsJsonObject();
                    JsonElement posEl = c.get("pos");
                    if (posEl == null || !posEl.isJsonArray() || posEl.getAsJsonArray().size() != 3) {
                        continue;
                    }
                    var arr = posEl.getAsJsonArray();
                    BlockPos pos = new BlockPos(arr.get(0).getAsInt(), arr.get(1).getAsInt(), arr.get(2).getAsInt());
                    Map<String, Integer> items = new TreeMap<>();
                    if (c.has("items") && c.get("items").isJsonArray()) {
                        for (JsonElement ie : c.getAsJsonArray("items")) {
                            JsonObject io = ie.getAsJsonObject();
                            items.put(io.get("id").getAsString(), io.get("count").getAsInt());
                        }
                    }
                    int total = 0;
                    for (int v : items.values()) {
                        total += v;
                    }
                    chests.add(new StoredChest(pos, items, total));
                }
            }
        } catch (IOException | RuntimeException ignored) {
        }
    }
}