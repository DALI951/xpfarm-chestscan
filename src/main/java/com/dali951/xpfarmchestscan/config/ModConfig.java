package com.dali951.xpfarmchestscan.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

public class ModConfig {

    public double radius = 4.5;
    public String outputFileName = "xpfarm-chests.json";
    public List<int[]> positions = new ArrayList<>();
    public boolean autoDetect = true;
    public Map<String, Integer> built = new TreeMap<>();
    public Map<String, Integer> placed = new TreeMap<>();

    public void addBuilt(String id, int delta) {
        built.merge(id, Math.max(0, delta), Integer::sum);
        if (built.get(id) == 0) {
            built.remove(id);
        }
    }

    public int getBuilt(String id) {
        return built.getOrDefault(id, 0);
    }

    public int getPlaced(String id) {
        return placed.getOrDefault(id, 0);
    }

    public void resetBuilt() {
        built.clear();
        placed.clear();
    }

    public void addPosition(int x, int y, int z) {
        for (int[] p : positions) {
            if (p[0] == x && p[1] == y && p[2] == z) {
                return;
            }
        }
        positions.add(new int[]{x, y, z});
    }

    public void removePosition(int index) {
        if (index >= 0 && index < positions.size()) {
            positions.remove(index);
        }
    }

    public int size() {
        return positions.size();
    }

    public int[] get(int index) {
        return positions.get(index);
    }

    public static ModConfig load() {
        Path path = configPath();
        ModConfig config = new ModConfig();
        if (!Files.exists(path)) {
            return config;
        }
        try {
            String raw = Files.readString(path, StandardCharsets.UTF_8);
            JsonObject obj = new Gson().fromJson(raw, JsonObject.class);
            if (obj == null) {
                return config;
            }
            if (obj.has("radius")) {
                config.radius = Math.min(8.0, Math.max(1.0, obj.get("radius").getAsDouble()));
            }
            if (obj.has("outputFileName")) {
                config.outputFileName = obj.get("outputFileName").getAsString();
            }
            if (obj.has("positions") && obj.get("positions").isJsonArray()) {
                for (JsonElement e : obj.getAsJsonArray("positions")) {
                    JsonArray a = e.getAsJsonArray();
                    if (a.size() == 3) {
                        config.positions.add(new int[]{a.get(0).getAsInt(), a.get(1).getAsInt(), a.get(2).getAsInt()});
                    }
                }
            }
            if (obj.has("autoDetect")) {
                config.autoDetect = obj.get("autoDetect").getAsBoolean();
            }
            if (obj.has("built") && obj.get("built").isJsonObject()) {
                for (Map.Entry<String, JsonElement> e : obj.getAsJsonObject("built").entrySet()) {
                    config.built.put(e.getKey(), e.getValue().getAsInt());
                }
            }
            if (obj.has("placed") && obj.get("placed").isJsonObject()) {
                for (Map.Entry<String, JsonElement> e : obj.getAsJsonObject("placed").entrySet()) {
                    config.placed.put(e.getKey(), e.getValue().getAsInt());
                }
            }
        } catch (Exception ignored) {
        }
        return config;
    }

    public void save() {
        JsonObject obj = new JsonObject();
        obj.addProperty("radius", radius);
        obj.addProperty("outputFileName", outputFileName);
        obj.addProperty("autoDetect", autoDetect);
        JsonArray arr = new JsonArray();
        for (int[] p : positions) {
            JsonArray a = new JsonArray();
            a.add(p[0]);
            a.add(p[1]);
            a.add(p[2]);
            arr.add(a);
        }
        obj.add("positions", arr);
        JsonObject builtObj = new JsonObject();
        for (Map.Entry<String, Integer> e : built.entrySet()) {
            builtObj.addProperty(e.getKey(), e.getValue());
        }
        obj.add("built", builtObj);
        JsonObject placedObj = new JsonObject();
        for (Map.Entry<String, Integer> e : placed.entrySet()) {
            placedObj.addProperty(e.getKey(), e.getValue());
        }
        obj.add("placed", placedObj);
        Path path = configPath();
        try {
            Files.createDirectories(path.getParent());
            Files.writeString(path, new GsonBuilder().setPrettyPrinting().create().toJson(obj), StandardCharsets.UTF_8);
        } catch (IOException ignored) {
        }
    }

    private static Path configPath() {
        return FabricLoader.getInstance().getConfigDir().resolve("xpfarm-chestscan.json");
    }
}