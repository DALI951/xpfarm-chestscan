package com.dali951.xpfarmchestscan;

import com.dali951.xpfarmchestscan.gui.ChecklistScreen;
import com.dali951.xpfarmchestscan.scan.ChestScanner;
import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
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

    public static final KeyMapping CHECKLIST_KEY = new KeyMapping(
            "key." + MOD_ID + ".checklist",
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_B,
            CATEGORY
    );

    @Override
    public void onInitializeClient() {
        KeyMappingHelper.registerKeyMapping(SCAN_KEY);
        KeyMappingHelper.registerKeyMapping(CHECKLIST_KEY);
        ChestScanner scanner = ChestScanner.INSTANCE;
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (SCAN_KEY.consumeClick()) {
                scanner.start();
            }
            while (CHECKLIST_KEY.consumeClick()) {
                Minecraft mc = Minecraft.getInstance();
                if (mc.gui.screen() instanceof ChecklistScreen) {
                    mc.gui.setScreen(null);
                } else if (mc.gui.screen() != null) {
                    if (mc.player != null) {
                        mc.player.sendSystemMessage(Component.translatable("xpfarm-chestscan.msg.closeScreen"));
                    }
                } else {
                    mc.gui.setScreen(new ChecklistScreen());
                }
            }
            scanner.tick();
        });
    }
}