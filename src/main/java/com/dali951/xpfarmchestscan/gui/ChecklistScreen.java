package com.dali951.xpfarmchestscan.gui;

import com.dali951.xpfarmchestscan.checklist.ChecklistData;
import com.dali951.xpfarmchestscan.checklist.ChestStore;
import com.dali951.xpfarmchestscan.config.ConfigStore;
import com.dali951.xpfarmchestscan.scan.ChestScanner;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class ChecklistScreen extends Screen {

    private static final int SCREEN_WIDTH = 470;
    private static final int SCREEN_HEIGHT = 330;
    private static final int ROW_HEIGHT = 22;
    private static final int VISIBLE_ROWS = 9;

    private final List<GoalRow> rows = new ArrayList<>();
    private int scrollOffset = 0;

    private static class GoalRow {
        final ChecklistData.Goal goal;
        final Button minus;
        final Button plusOne;
        final Button plusStack;

        GoalRow(ChecklistData.Goal goal, Button minus, Button plusOne, Button plusStack) {
            this.goal = goal;
            this.minus = minus;
            this.plusOne = plusOne;
            this.plusStack = plusStack;
        }
    }

    public ChecklistScreen() {
        super(Component.translatable("xpfarm-chestscan.checklist.title"));
    }

    @Override
    protected void init() {
        Minecraft mc = Minecraft.getInstance();
        ChestStore.ensureLoaded(mc);
        int left = (this.width - SCREEN_WIDTH) / 2;
        int top = (this.height - SCREEN_HEIGHT) / 2;
        int listTop = top + 44;
        int listBottom = listTop + VISIBLE_ROWS * ROW_HEIGHT;
        scrollOffset = 0;

        rebuildRows(left, listTop);

        Button scanButton = Button.builder(Component.translatable("xpfarm-chestscan.checklist.scan"), b -> {
            Minecraft m = Minecraft.getInstance();
            m.gui.setScreen(null);
            ChestScanner.INSTANCE.start();
        }).bounds(left, listBottom + 10, SCREEN_WIDTH / 3, 20).build();

        Button resetButton = Button.builder(Component.translatable("xpfarm-chestscan.checklist.reset"), b -> {
            ConfigStore.get().resetBuilt();
            ConfigStore.save();
            rebuildRows(left, listTop);
        }).bounds(left + SCREEN_WIDTH / 3 + 5, listBottom + 10, SCREEN_WIDTH / 3, 20).build();

        Button closeButton = Button.builder(Component.translatable("xpfarm-chestscan.checklist.close"), b -> this.onClose())
                .bounds(left + 2 * SCREEN_WIDTH / 3 + 10, listBottom + 10, SCREEN_WIDTH / 3 - 10, 20).build();

        addRenderableWidget(scanButton);
        addRenderableWidget(resetButton);
        addRenderableWidget(closeButton);
    }

    private void rebuildRows(int left, int listTop) {
        for (GoalRow row : rows) {
            removeWidget(row.minus);
            removeWidget(row.plusOne);
            removeWidget(row.plusStack);
        }
        rows.clear();
        int index = 0;
        for (ChecklistData.Goal goal : ChecklistData.GOALS) {
            int y = listTop + 24 + index * ROW_HEIGHT;
            Button minus = Button.builder(Component.literal("-1"), b -> {
                ConfigStore.get().addBuilt(goal.id(), -1);
                ConfigStore.save();
                rebuildRows(left, listTop);
            }).bounds(left + 356, y, 28, 16).build();
            Button plusOne = Button.builder(Component.literal("+1"), b -> {
                ConfigStore.get().addBuilt(goal.id(), 1);
                ConfigStore.save();
                rebuildRows(left, listTop);
            }).bounds(left + 388, y, 28, 16).build();
            Button plusStack = Button.builder(Component.literal("+64"), b -> {
                ConfigStore.get().addBuilt(goal.id(), 64);
                ConfigStore.save();
                rebuildRows(left, listTop);
            }).bounds(left + 420, y, 46, 16).build();
            rows.add(new GoalRow(goal, minus, plusOne, plusStack));
            addRenderableWidget(minus);
            addRenderableWidget(plusOne);
            addRenderableWidget(plusStack);
            index++;
        }
    }

    private void layoutRows(int left, int listTop) {
        int index = 0;
        for (GoalRow row : rows) {
            int y = listTop + 24 + index * ROW_HEIGHT - scrollOffset;
            row.minus.setPosition(left + 356, y);
            row.plusOne.setPosition(left + 388, y);
            row.plusStack.setPosition(left + 420, y);
            index++;
        }
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor context, int mouseX, int mouseY, float delta) {
        int left = (this.width - SCREEN_WIDTH) / 2;
        int top = (this.height - SCREEN_HEIGHT) / 2;
        int listTop = top + 44;
        int listBottom = listTop + VISIBLE_ROWS * ROW_HEIGHT;

        context.fill(left, listTop, left + SCREEN_WIDTH, listBottom, 0xCC181818);
        layoutRows(left, listTop);

        super.extractRenderState(context, mouseX, mouseY, delta);

        context.centeredText(this.font, this.title, this.width / 2, top + 10, 0xFFFFFF);

        int doneTotal = 0;
        int doneItems = 0;
        for (ChecklistData.Goal g : ChecklistData.GOALS) {
            int have = Math.min(g.needed(), ConfigStore.get().getBuilt(g.id()) + ChestStore.get(g.id()));
            doneTotal += have;
            if (have >= g.needed()) {
                doneItems++;
            }
        }
        int neededAll = ChecklistData.totalNeeded();
        int pct = (int) Math.round(doneTotal * 100.0 / neededAll);
        String overall = String.format(Locale.US, "%,d / %,d built+stored — %,d left — %d%% — %d/%d items done",
                doneTotal, neededAll, Math.max(0, neededAll - doneTotal), pct, doneItems, ChecklistData.GOALS.size());
        context.text(this.font, overall, left, top + 24, pct >= 100 ? 0x55FF7F : 0x9AD3FF);
        int barY = top + 36;
        context.fill(left, barY, left + SCREEN_WIDTH, barY + 5, 0xFF2A2A2A);
        int barW = (int) (SCREEN_WIDTH * Math.min(1.0, (double) doneTotal / neededAll));
        if (barW > 0) {
            context.fill(left, barY, left + barW, barY + 5, pct >= 100 ? 0xFF2ECC71 : 0xFF4C8CFF);
        }

        context.text(this.font, "Item", left + 6, listTop + 6, 0x888888);
        context.text(this.font, "Needed", left + 158, listTop + 6, 0x888888);
        context.text(this.font, "Stored", left + 214, listTop + 6, 0x888888);
        context.text(this.font, "Built", left + 272, listTop + 6, 0x888888);
        context.text(this.font, "Left", left + 322, listTop + 6, 0x888888);

        int index = 0;
        for (GoalRow row : rows) {
            int y = listTop + 24 + index * ROW_HEIGHT - scrollOffset;
            if (y >= listTop + 22 && y < listBottom - 4) {
                int needed = row.goal.needed();
                int stored = ChestStore.get(row.goal.id());
                int built = ConfigStore.get().getBuilt(row.goal.id());
                int leftN = Math.max(0, needed - stored - built);
                int color = leftN == 0 ? 0x55FF7F : 0xE0E0E0;
                context.text(this.font, row.goal.label(), left + 6, y + 3, color);
                context.text(this.font, String.format(Locale.US, "%,d", needed), left + 160, y + 3, 0x9AD3FF);
                context.text(this.font, String.format(Locale.US, "%,d", stored), left + 218, y + 3, 0xAAAAAA);
                context.text(this.font, String.format(Locale.US, "%,d", built), left + 276, y + 3, 0xCCCCCC);
                context.text(this.font, String.format(Locale.US, "%,d", leftN), left + 324, y + 3,
                        leftN == 0 ? 0x55FF7F : 0xFFFFFF);
            }
            index++;
        }
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        int maxScroll = Math.max(0, ChecklistData.GOALS.size() * ROW_HEIGHT - VISIBLE_ROWS * ROW_HEIGHT);
        scrollOffset = (int) Math.max(0, Math.min(maxScroll, scrollOffset - verticalAmount * 10));
        return true;
    }

    @Override
    public void onClose() {
        Minecraft.getInstance().gui.setScreen(null);
    }
}