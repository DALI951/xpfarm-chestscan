package com.dali951.xpfarmchestscan.checklist;

import com.dali951.xpfarmchestscan.config.ConfigStore;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.client.Minecraft;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.TreeMap;

public final class ChestStore {

    private static final Map<String, Integer> stored = new TreeMap<>();
    private static boolean loaded = false;

    private ChestStore() {
    }

    public static int get(String id) {
        return stored.getOrDefault(id, 0);
    }

    public static int totalStored() {
        int total = 0;
        for (int v : stored.values()) {
            total += v;
        }
        return total;
    }

    public static void applyScan(Map<String, Integer> totals) {
        stored.clear();
        stored.putAll(totals);
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
        } catch (IOException | RuntimeException ignored) {
        }
    }
}