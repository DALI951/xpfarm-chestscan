package com.dali951.xpfarmchestscan;

import com.dali951.xpfarmchestscan.scan.ChestScanner;
import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.minecraft.client.KeyMapping;
import net.minecraft.resources.Identifier;
import org.lwjgl.glfw.GLFW;

public class XpFarmChestscanClient implements ClientModInitializer {

    public static final String MOD_ID = "xpfarm-chestscan";

    public static final KeyMapping.Category CATEGORY = KeyMapping.Category.register(
            Identifier.fromNamespaceAndPath(MOD_ID, "main"));

    public static final KeyMapping SCAN_KEY = new KeyMapping(
            "key." + MOD_ID + ".scan",
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_V,
            CATEGORY
    );

    @Override
    public void onInitializeClient() {
        KeyMappingHelper.registerKeyMapping(SCAN_KEY);
        ChestScanner scanner = ChestScanner.INSTANCE;
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (SCAN_KEY.consumeClick()) {
                scanner.start();
            }
            scanner.tick();
        });
    }
}