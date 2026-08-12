package com.dali951.xpfarmchestscan.scan;

import com.dali951.xpfarmchestscan.config.ConfigStore;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.minecraft.client.Minecraft;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;

public final class ChestDumpWriter {

    private ChestDumpWriter() {
    }

    public static Path outputPath(Minecraft mc) {
        return mc.gameDirectory.toPath().resolve(ConfigStore.get().outputFileName);
    }

    public static void write(Minecraft mc, ScanResult result) {
        JsonObject root = new JsonObject();
        root.addProperty("schema", 1);
        root.addProperty("generatedAt", new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss").format(new Date(result.generatedAt)));
        root.addProperty("player", mc.player != null ? mc.player.getName().getString() : "?");
        root.addProperty("radius", ConfigStore.get().radius);

        JsonArray chests = new JsonArray();
        for (ChestEntry entry : result.chests) {
            JsonObject chest = new JsonObject();
            JsonArray pos = new JsonArray();
            pos.add(entry.pos.getX());
            pos.add(entry.pos.getY());
            pos.add(entry.pos.getZ());
            chest.add("pos", pos);
            JsonArray items = new JsonArray();
            for (Map.Entry<String, Integer> e : entry.items.entrySet()) {
                JsonObject item = new JsonObject();
                item.addProperty("id", e.getKey());
                item.addProperty("count", e.getValue());
                items.add(item);
            }
            chest.add("items", items);
            chests.add(chest);
        }
        root.add("chests", chests);

        JsonObject totals = new JsonObject();
        for (Map.Entry<String, Integer> e : result.totals.entrySet()) {
            totals.addProperty(e.getKey(), e.getValue());
        }
        root.add("totals", totals);

        JsonArray skipped = new JsonArray();
        for (net.minecraft.core.BlockPos p : result.skipped) {
            JsonArray a = new JsonArray();
            a.add(p.getX());
            a.add(p.getY());
            a.add(p.getZ());
            skipped.add(a);
        }
        root.add("skipped", skipped);

        try {
            Files.writeString(outputPath(mc), root.toString(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            if (mc.player != null) {
                mc.player.sendSystemMessage(net.minecraft.network.chat.Component.literal("Failed to write chest dump: " + e.getMessage()));
            }
        }
    }
}