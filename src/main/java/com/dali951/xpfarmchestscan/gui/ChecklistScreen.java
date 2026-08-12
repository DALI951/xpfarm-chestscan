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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class ChecklistScreen extends Screen {

    private static final Logger LOGGER = LoggerFactory.getLogger("xpfarm-chestscan");

    private enum Tab {
        BUILD, GATHER, CHESTS
    }

    private static final int SCREEN_WIDTH = 470;
    private static final int SCREEN_HEIGHT = 370;
    private static final int ROW_HEIGHT = 22;
    private static final int VISIBLE_ROWS = 9;

    private final List<GoalRow> rows = new ArrayList<>();
    private final List<Button> tabButtons = new ArrayList<>();
    private int scrollOffset = 0;
    private Tab tab = Tab.BUILD;

    private static class GoalRow {
        final String builtKey;
        final String label;
        final Button minus;
        final Button plusOne;
        final Button plusStack;

        GoalRow(String builtKey, String label, Button minus, Button plusOne, Button plusStack) {
            this.builtKey = builtKey;
            this.label = label;
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
        int listBottom = listTop(left, top) + VISIBLE_ROWS * ROW_HEIGHT;
        scrollOffset = 0;

        rebuildUi(left, top, listBottom);
        LOGGER.info("xpfarm checklist opened: tab={} goalRows={} gatherRows={} chests={} width={} height={}",
                tab, ChecklistData.GOALS.size(), ChecklistData.GATHER.size(), ChestStore.chests().size(),
                this.width, this.height);
    }

    private int listTop(int left, int top) {
        return top + 76;
    }

    private void rebuildUi(int left, int top, int listBottom) {
        for (GoalRow row : rows) {
            removeWidget(row.minus);
            removeWidget(row.plusOne);
            removeWidget(row.plusStack);
        }
        rows.clear();
        for (Button b : tabButtons) {
            removeWidget(b);
        }
        tabButtons.clear();

        int tabY = top + 48;
        int tabW = (SCREEN_WIDTH - 8) / 3;
        tabButtons.add(Button.builder(Component.translatable("xpfarm-chestscan.checklist.tabBuild"),
                b -> setTab(Tab.BUILD)).bounds(left, tabY, tabW, 20).build());
        tabButtons.add(Button.builder(Component.translatable("xpfarm-chestscan.checklist.tabGather"),
                b -> setTab(Tab.GATHER)).bounds(left + tabW + 4, tabY, tabW, 20).build());
        tabButtons.add(Button.builder(Component.translatable("xpfarm-chestscan.checklist.tabChests"),
                b -> setTab(Tab.CHESTS)).bounds(left + 2 * (tabW + 4), tabY, tabW, 20).build());
        for (Button b : tabButtons) {
            addRenderableWidget(b);
        }

        int listTop = listTop(left, top);
        if (tab == Tab.BUILD || tab == Tab.GATHER) {
            buildListRows(left, listTop);
        }

        Button scanButton = Button.builder(Component.translatable("xpfarm-chestscan.checklist.scan"), b -> {
            Minecraft m = Minecraft.getInstance();
            m.gui.setScreen(null);
            ChestScanner.INSTANCE.start();
        }).bounds(left, listBottom + 10, SCREEN_WIDTH / 3, 20).build();

        Button resetButton = Button.builder(Component.translatable("xpfarm-chestscan.checklist.reset"), b -> {
            if (tab == Tab.CHESTS) {
                return;
            }
            for (GoalRow row : new ArrayList<>(rows)) {
                ConfigStore.get().built.remove(row.builtKey);
            }
            ConfigStore.save();
            rebuildUi(left, top, listBottom);
        }).bounds(left + SCREEN_WIDTH / 3 + 5, listBottom + 10, SCREEN_WIDTH / 3, 20).build();

        Button closeButton = Button.builder(Component.translatable("xpfarm-chestscan.checklist.close"), b -> this.onClose())
                .bounds(left + 2 * SCREEN_WIDTH / 3 + 10, listBottom + 10, SCREEN_WIDTH / 3 - 10, 20).build();

        addRenderableWidget(scanButton);
        addRenderableWidget(resetButton);
        addRenderableWidget(closeButton);
    }

    private void buildListRows(int left, int listTop) {
        int index = 0;
        if (tab == Tab.BUILD) {
            for (ChecklistData.Goal goal : ChecklistData.GOALS) {
                String builtKey = "b:" + goal.id();
                addGoalRow(left, listTop, index++, builtKey, goal.label());
            }
        } else {
            for (ChecklistData.GatherGoal goal : ChecklistData.GATHER) {
                String builtKey = "g:" + goal.id();
                addGoalRow(left, listTop, index++, builtKey, goal.label());
            }
        }
    }

    private void addGoalRow(int left, int listTop, int index, String builtKey, String label) {
        int y = listTop + 24 + index * ROW_HEIGHT;
        Button minus = Button.builder(Component.literal("-1"), b -> {
            ConfigStore.get().built.merge(builtKey, -1, Integer::sum);
            if (ConfigStore.get().built.get(builtKey) <= 0) {
                ConfigStore.get().built.remove(builtKey);
            }
            ConfigStore.save();
            reInit();
        }).bounds(left + 356, y, 28, 16).build();
        Button plusOne = Button.builder(Component.literal("+1"), b -> {
            ConfigStore.get().built.merge(builtKey, 1, Integer::sum);
            ConfigStore.save();
            reInit();
        }).bounds(left + 388, y, 28, 16).build();
        Button plusStack = Button.builder(Component.literal("+64"), b -> {
            ConfigStore.get().built.merge(builtKey, 64, Integer::sum);
            ConfigStore.save();
            reInit();
        }).bounds(left + 420, y, 46, 16).build();
        rows.add(new GoalRow(builtKey, label, minus, plusOne, plusStack));
        addRenderableWidget(minus);
        addRenderableWidget(plusOne);
        addRenderableWidget(plusStack);
    }

    private void reInit() {
        int left = (this.width - SCREEN_WIDTH) / 2;
        int top = (this.height - SCREEN_HEIGHT) / 2;
        rebuildUi(left, top, listTop(left, top) + VISIBLE_ROWS * ROW_HEIGHT);
    }

    private void setTab(Tab newTab) {
        tab = newTab;
        scrollOffset = 0;
        reInit();
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
        int listTop = listTop(left, top);
        int listBottom = listTop + VISIBLE_ROWS * ROW_HEIGHT;

        ensureRowsBuilt(left, listTop);
        layoutRows(left, listTop);

        context.fill(left, listTop, left + SCREEN_WIDTH, listBottom, 0xCC181818);

        context.centeredText(this.font, this.title, this.width / 2, top + 10, 0xFFFFFF);

        if (tab != Tab.CHESTS) {
            drawTotalsBar(context, left, top);
        } else {
            String info = ChestStore.chests().isEmpty()
                    ? Component.translatable("xpfarm-chestscan.checklist.noChests").getString()
                    : String.format(Locale.US, "%,d chests scanned — %,d items total",
                    ChestStore.chests().size(), ChestStore.totalStored());
            context.text(this.font, info, left, top + 26, ChestStore.chests().isEmpty() ? 0xFFAA66 : 0x9AD3FF);
        }

        if (tab == Tab.BUILD || tab == Tab.GATHER) {
            context.text(this.font, "Item", left + 6, listTop + 6, 0x888888);
            context.text(this.font, "Needed", left + 158, listTop + 6, 0x888888);
            context.text(this.font, "Stored", left + 214, listTop + 6, 0x888888);
            context.text(this.font, "Built", left + 272, listTop + 6, 0x888888);
            context.text(this.font, "Left", left + 322, listTop + 6, 0x888888);

            int index = 0;
            for (GoalRow row : rows) {
                int y = listTop + 24 + index * ROW_HEIGHT - scrollOffset;
                if (y >= listTop + 22 && y < listBottom - 4) {
                    int needed = goalNeeded(row.builtKey);
                    int stored = goalStored(row.builtKey);
                    int built = ConfigStore.get().built.getOrDefault(row.builtKey, 0);
                    int leftN = Math.max(0, needed - stored - built);
                    int color = leftN == 0 ? 0x55FF7F : 0xE0E0E0;
                    context.text(this.font, row.label, left + 6, y + 3, color);
                    context.text(this.font, String.format(Locale.US, "%,d", needed), left + 160, y + 3, 0x9AD3FF);
                    context.text(this.font, String.format(Locale.US, "%,d", stored), left + 218, y + 3, 0xAAAAAA);
                    context.text(this.font, String.format(Locale.US, "%,d", built), left + 276, y + 3, 0xCCCCCC);
                    String leftText = String.format(Locale.US, "%,d (%,d st)", leftN, leftN / 64);
                    context.text(this.font, leftText, left + 316, y + 3,
                            leftN == 0 ? 0x55FF7F : 0xFFFFFF);
                }
                index++;
            }
        } else {
            context.text(this.font, "Chest (x, y, z)", left + 6, listTop + 6, 0x888888);
            int index = 0;
            for (ChestStore.StoredChest c : ChestStore.chests()) {
                int y = listTop + 24 + index * ROW_HEIGHT - scrollOffset;
                if (y >= listTop + 22 && y < listBottom - 4) {
                    context.text(this.font,
                            String.format("(%d, %d, %d)  ·  %,d items",
                                    c.pos().getX(), c.pos().getY(), c.pos().getZ(), c.total()),
                            left + 6, y + 3, 0xE0E0E0);
                    String topLabel = topItems(c);
                    if (!topLabel.isEmpty()) {
                        context.text(this.font, topLabel, left + 300, y + 3, 0x9AD3FF);
                    }
                }
                index++;
            }
        }

        super.extractRenderState(context, mouseX, mouseY, delta);
    }

    private void ensureRowsBuilt(int left, int listTop) {
        if (rows.isEmpty() && (tab == Tab.BUILD || tab == Tab.GATHER)) {
            buildListRows(left, listTop);
        }
    }

    private void drawTotalsBar(GuiGraphicsExtractor context, int left, int top) {
        int doneTotal = 0;
        int neededAll;
        int doneItems;
        if (tab == Tab.BUILD) {
            neededAll = ChecklistData.totalNeeded();
            doneItems = 0;
            for (ChecklistData.Goal g : ChecklistData.GOALS) {
                int have = Math.min(g.needed(), ConfigStore.get().built.getOrDefault("b:" + g.id(), 0)
                        + ChestStore.get(g.id()));
                doneTotal += have;
                if (have >= g.needed()) {
                    doneItems++;
                }
            }
        } else {
            neededAll = ChecklistData.totalGather();
            doneItems = 0;
            for (ChecklistData.GatherGoal g : ChecklistData.GATHER) {
                int stored = ChestStore.sumOf(g.matchIds());
                int have = Math.min(g.needed(), ConfigStore.get().built.getOrDefault("g:" + g.id(), 0) + stored);
                doneTotal += have;
                if (have >= g.needed()) {
                    doneItems++;
                }
            }
        }
        int pct = (int) Math.round(doneTotal * 100.0 / neededAll);
        String name = tab == Tab.BUILD ? "built+stored" : "gathered+stored";
        String overall = String.format(Locale.US, "%,d / %,d %s — %,d left — %d%% — %d/%d items done",
                doneTotal, neededAll, name, Math.max(0, neededAll - doneTotal), pct, doneItems, rowCount());
        context.text(this.font, overall, left, top + 26, pct >= 100 ? 0x55FF7F : 0x9AD3FF);
        int barY = top + 38;
        context.fill(left, barY, left + SCREEN_WIDTH, barY + 5, 0xFF2A2A2A);
        int barW = (int) (SCREEN_WIDTH * Math.min(1.0, (double) doneTotal / neededAll));
        if (barW > 0) {
            context.fill(left, barY, left + barW, barY + 5, pct >= 100 ? 0xFF2ECC71 : 0xFF4C8CFF);
        }
    }

    private int rowCount() {
        return tab == Tab.BUILD ? ChecklistData.GOALS.size() : ChecklistData.GATHER.size();
    }

    private int goalNeeded(String builtKey) {
        String id = builtKey.substring(2);
        return tab == Tab.BUILD ? ChecklistData.needed(id) : gatherNeeded(id);
    }

    private int gatherNeeded(String id) {
        for (ChecklistData.GatherGoal g : ChecklistData.GATHER) {
            if (g.id().equals(id)) {
                return g.needed();
            }
        }
        return 0;
    }

    private int goalStored(String builtKey) {
        String id = builtKey.substring(2);
        if (tab == Tab.GATHER) {
            for (ChecklistData.GatherGoal g : ChecklistData.GATHER) {
                if (g.id().equals(id)) {
                    return ChestStore.sumOf(g.matchIds());
                }
            }
            return 0;
        }
        return ChestStore.get(id);
    }

    private String topItems(ChestStore.StoredChest chest) {
        List<Map.Entry<String, Integer>> sorted = new ArrayList<>(chest.items().entrySet());
        sorted.sort((a, b) -> b.getValue().compareTo(a.getValue()));
        StringBuilder sb = new StringBuilder();
        int count = 0;
        for (Map.Entry<String, Integer> e : sorted) {
            if (count++ == 2) {
                break;
            }
            if (sb.length() > 0) {
                sb.append(", ");
            }
            sb.append(e.getKey().replace("minecraft:", "")).append(" ×").append(e.getValue());
        }
        String s = sb.toString();
        return s.length() > 26 ? s.substring(0, 26) : s;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        int maxScroll = Math.max(0, rowCount() * ROW_HEIGHT - VISIBLE_ROWS * ROW_HEIGHT);
        if (tab == Tab.CHESTS) {
            maxScroll = Math.max(0, ChestStore.chests().size() * ROW_HEIGHT - VISIBLE_ROWS * ROW_HEIGHT);
        }
        scrollOffset = (int) Math.max(0, Math.min(maxScroll, scrollOffset - verticalAmount * 10));
        return true;
    }

    @Override
    public void onClose() {
        Minecraft.getInstance().gui.setScreen(null);
    }
}