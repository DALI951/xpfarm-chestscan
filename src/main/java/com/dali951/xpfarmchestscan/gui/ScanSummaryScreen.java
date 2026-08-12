package com.dali951.xpfarmchestscan.gui;

import com.dali951.xpfarmchestscan.scan.ScanResult;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class ScanSummaryScreen extends Screen {

    private final ScanResult result;
    private final List<Map.Entry<String, Integer>> sortedItems;
    private int scrollOffset = 0;

    public ScanSummaryScreen(ScanResult result) {
        super(Component.translatable("xpfarm-chestscan.summary.title"));
        this.result = result;
        this.sortedItems = new ArrayList<>(result.totals.entrySet());
        this.sortedItems.sort((a, b) -> b.getValue().compareTo(a.getValue()));
    }

    @Override
    protected void init() {
        int width = 320;
        int left = (this.width - width) / 2;
        int top = (this.height - 250) / 2;

        Button closeButton = Button.builder(Component.translatable("xpfarm-chestscan.summary.close"), b -> this.onClose())
                .bounds(left, top + 206, width, 20).build();

        addRenderableWidget(closeButton);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor context, int mouseX, int mouseY, float delta) {
        int width = 320;
        int left = (this.width - width) / 2;
        int top = (this.height - 250) / 2;

        context.fill(left - 2, top + 34, left + width + 2, top + 200, 0xCC181818);

        super.extractRenderState(context, mouseX, mouseY, delta);

        context.centeredText(this.font, this.title, this.width / 2, top + 8, 0xFFFFFF);
        context.centeredText(this.font,
                Component.literal(result.chests.size() + " " + Component.translatable("xpfarm-chestscan.summary.chests").getString()
                        + " — " + result.totalItems() + " " + Component.translatable("xpfarm-chestscan.summary.items").getString()),
                this.width / 2, top + 22, 0x9AD3FF);

        int y = top + 40;
        int index = 0;
        for (Map.Entry<String, Integer> e : sortedItems) {
            int cy = y + index * 12 - scrollOffset;
            if (cy >= top + 34 && cy < top + 196) {
                String label = e.getKey().replace("minecraft:", "");
                context.text(this.font, label, left + 8, cy, 0xE0E0E0);
                context.text(this.font, e.getValue().toString(), left + width - 54, cy, 0xFFFFFF);
            }
            index++;
        }
        if (!result.skipped.isEmpty()) {
            context.text(this.font,
                    result.skipped.size() + " " + Component.translatable("xpfarm-chestscan.summary.skipped").getString(),
                    left + 8, top + 198, 0xFFAA66);
        }

        context.text(this.font, Component.translatable("xpfarm-chestscan.summary.savedTo").getString(),
                left + 8, top + 232, 0x888888);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        int maxScroll = Math.max(0, sortedItems.size() * 12 - 150);
        scrollOffset = (int) Math.max(0, Math.min(maxScroll, scrollOffset - verticalAmount * 8));
        return true;
    }
}
