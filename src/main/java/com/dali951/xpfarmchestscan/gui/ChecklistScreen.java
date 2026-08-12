package com.dali951.xpfarmchestscan.gui;

import com.dali951.xpfarmchestscan.checklist.ChecklistData;
import com.dali951.xpfarmchestscan.checklist.ChestStore;
import com.dali951.xpfarmchestscan.config.ConfigStore;
import com.dali951.xpfarmchestscan.scan.ChestScanner;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Renderable;
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

    private static class RenderedRow {
        final String builtKey;
        final Button minus;
        final Button plusOne;
        final Button plusStack;

        RenderedRow(String builtKey, Button minus, Button plusOne, Button plusStack) {
            this.builtKey = builtKey;
            this.minus = minus;
            this.plusOne = plusOne;
            this.plusStack = plusStack;
        }
    }

    private final List<RenderedRow> rows = new ArrayList<>();
    private final List<Button> tabButtons = new ArrayList<>();
    private int scrollOffset = 0;
    private final Tab tab;

    public ChecklistScreen() {
        this(Tab.BUILD);
    }

    private ChecklistScreen(Tab tab) {
        super(Component.translatable("xpfarm-chestscan.checklist.title"));
        this.tab = tab;
    }

    @Override
    protected void init() {
        Minecraft mc = Minecraft.getInstance();
        ChestStore.ensureLoaded(mc);
        int left = (this.width - SCREEN_WIDTH) / 2;
        int top = (this.height - SCREEN_HEIGHT) / 2;
        scrollOffset = 0;
        rebuildUi(left, top);
        LOGGER.info("xpfarm checklist opened: tab={} goals={} gather={} chests={} width={} height={}",
                tab, ChecklistData.GOALS.size(), ChecklistData.GATHER.size(), ChestStore.chests().size(),
                this.width, this.height);
    }

    private void rebuildUi(int left, int top) {
        int tabY = top + 48;
        int tabW = (SCREEN_WIDTH - 8) / 3;
        tabButtons.add(Button.builder(Component.translatable("xpfarm-chestscan.checklist.tabBuild"),
                b -> openTab(Tab.BUILD)).bounds(left, tabY, tabW, 20).build());
        tabButtons.add(Button.builder(Component.translatable("xpfarm-chestscan.checklist.tabGather"),
                b -> openTab(Tab.GATHER)).bounds(left + tabW + 4, tabY, tabW, 20).build());
        tabButtons.add(Button.builder(Component.translatable("xpfarm-chestscan.checklist.tabChests"),
                b -> openTab(Tab.CHESTS)).bounds(left + 2 * (tabW + 4), tabY, tabW, 20).build());
        for (Button b : tabButtons) {
            addRenderableWidget(b);
        }

        int listTop = listTop(top);
        int listBottom = listTop + VISIBLE_ROWS * ROW_HEIGHT;

        addRenderableOnly((ctx, mx, my, dt) -> ctx.centeredText(this.font, this.title, this.width / 2, top + 10, 0xFFFFFF));
        addRenderableOnly((ctx, mx, my, dt) -> ctx.fill(left, listTop, left + SCREEN_WIDTH, listBottom, 0xCC181818));
        if (tab == Tab.BUILD || tab == Tab.GATHER) {
            addRenderableOnly(drawTotalsBar(left, top, listBottom));
            addRenderableOnly((ctx, mx, my, dt) -> ctx.text(this.font, "Item", left + 6, listTop + 6, 0x888888));
            addRenderableOnly((ctx, mx, my, dt) -> ctx.text(this.font, "Needed", left + 158, listTop + 6, 0x888888));
            addRenderableOnly((ctx, mx, my, dt) -> ctx.text(this.font, "Stored", left + 214, listTop + 6, 0x888888));
            addRenderableOnly((ctx, mx, my, dt) -> ctx.text(this.font, "Built", left + 272, listTop + 6, 0x888888));
            addRenderableOnly((ctx, mx, my, dt) -> ctx.text(this.font, "Left", left + 322, listTop + 6, 0x888888));
            buildListRows(left, listTop);
        } else {
            addRenderableOnly((ctx, mx, my, dt) -> {
                String info = ChestStore.chests().isEmpty()
                        ? Component.translatable("xpfarm-chestscan.checklist.noChests").getString()
                        : String.format(Locale.US, "%,d chests scanned — %,d items total",
                        ChestStore.chests().size(), ChestStore.totalStored());
                ctx.text(this.font, info, left, top + 26, ChestStore.chests().isEmpty() ? 0xFFAA66 : 0x9AD3FF);
            });
            addRenderableOnly((ctx, mx, my, dt) -> ctx.text(this.font, "Chest (x, y, z)", left + 6, listTop + 6, 0x888888));
            buildChestRows(left, listTop);
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
            for (RenderedRow row : new ArrayList<>(rows)) {
                ConfigStore.get().built.remove(row.builtKey);
            }
            ConfigStore.save();
            Minecraft.getInstance().gui.setScreen(new ChecklistScreen(tab));
        }).bounds(left + SCREEN_WIDTH / 3 + 5, listBottom + 10, SCREEN_WIDTH / 3, 20).build();

        Button closeButton = Button.builder(Component.translatable("xpfarm-chestscan.checklist.close"), b -> this.onClose())
                .bounds(left + 2 * SCREEN_WIDTH / 3 + 10, listBottom + 10, SCREEN_WIDTH / 3 - 10, 20).build();

        addRenderableWidget(scanButton);
        addRenderableWidget(resetButton);
        addRenderableWidget(closeButton);
    }

    private Renderable drawTotalsBar(int left, int top, int listBottom) {
        return (ctx, mx, my, dt) -> {
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
            ctx.text(this.font, overall, left, top + 26, pct >= 100 ? 0x55FF7F : 0x9AD3FF);
            int barY = top + 38;
            ctx.fill(left, barY, left + SCREEN_WIDTH, barY + 5, 0xFF2A2A2A);
            int barW = (int) (SCREEN_WIDTH * Math.min(1.0, (double) doneTotal / neededAll));
            if (barW > 0) {
                ctx.fill(left, barY, left + barW, barY + 5, pct >= 100 ? 0xFF2ECC71 : 0xFF4C8CFF);
            }
        };
    }

    private void buildListRows(int left, int listTop) {
        int index = 0;
        if (tab == Tab.BUILD) {
            for (ChecklistData.Goal goal : ChecklistData.GOALS) {
                addGoalRow(left, listTop, index++, "b:" + goal.id(), goal.label());
            }
        } else {
            for (ChecklistData.GatherGoal goal : ChecklistData.GATHER) {
                addGoalRow(left, listTop, index++, "g:" + goal.id(), goal.label());
            }
        }
    }

    private void addGoalRow(int left, int listTop, int index, String builtKey, String label) {
        int y = listTop + 24 + index * ROW_HEIGHT;
        Renderable rowText = (ctx, mx, my, dt) -> {
            int yNow = listTop + 24 + index * ROW_HEIGHT - scrollOffset;
            if (yNow < listTop + 22 || yNow >= listTop + 24 + VISIBLE_ROWS * ROW_HEIGHT - 4) {
                return;
            }
            int needed = goalNeeded(builtKey);
            int stored = goalStored(builtKey);
            int built = ConfigStore.get().built.getOrDefault(builtKey, 0);
            int leftN = Math.max(0, needed - stored - built);
            int color = leftN == 0 ? 0x55FF7F : 0xE0E0E0;
            ctx.text(this.font, label, left + 6, yNow + 3, color);
            ctx.text(this.font, String.format(Locale.US, "%,d", needed), left + 160, yNow + 3, 0x9AD3FF);
            ctx.text(this.font, String.format(Locale.US, "%,d", stored), left + 218, yNow + 3, 0xAAAAAA);
            ctx.text(this.font, String.format(Locale.US, "%,d", built), left + 276, yNow + 3, 0xCCCCCC);
            ctx.text(this.font, String.format(Locale.US, "%,d (%,d st)", leftN, leftN / 64), left + 316, yNow + 3,
                    leftN == 0 ? 0x55FF7F : 0xFFFFFF);
        };
        Button minus = Button.builder(Component.literal("-1"), b -> {
            ConfigStore.get().built.merge(builtKey, -1, Integer::sum);
            if (ConfigStore.get().built.get(builtKey) <= 0) {
                ConfigStore.get().built.remove(builtKey);
            }
            ConfigStore.save();
            Minecraft.getInstance().gui.setScreen(new ChecklistScreen(tab));
        }).bounds(left + 356, y, 28, 16).build();
        Button plusOne = Button.builder(Component.literal("+1"), b -> {
            ConfigStore.get().built.merge(builtKey, 1, Integer::sum);
            ConfigStore.save();
            Minecraft.getInstance().gui.setScreen(new ChecklistScreen(tab));
        }).bounds(left + 388, y, 28, 16).build();
        Button plusStack = Button.builder(Component.literal("+64"), b -> {
            ConfigStore.get().built.merge(builtKey, 64, Integer::sum);
            ConfigStore.save();
            Minecraft.getInstance().gui.setScreen(new ChecklistScreen(tab));
        }).bounds(left + 420, y, 46, 16).build();
        rows.add(new RenderedRow(builtKey, minus, plusOne, plusStack));
        addRenderableOnly(rowText);
        addRenderableWidget(minus);
        addRenderableWidget(plusOne);
        addRenderableWidget(plusStack);
    }

    private void buildChestRows(int left, int listTop) {
        int index = 0;
        for (ChestStore.StoredChest c : ChestStore.chests()) {
            final int rowIndex = index;
            Renderable rowText = (ctx, mx, my, dt) -> {
                int yNow = listTop + 24 + rowIndex * ROW_HEIGHT - scrollOffset;
                if (yNow < listTop + 22 || yNow >= listTop + 24 + VISIBLE_ROWS * ROW_HEIGHT - 4) {
                    return;
                }
                ctx.text(this.font,
                        String.format("(%d, %d, %d)  ·  %,d items",
                                c.pos().getX(), c.pos().getY(), c.pos().getZ(), c.total()),
                        left + 6, yNow + 3, 0xE0E0E0);
                String top = topItems(c);
                if (!top.isEmpty()) {
                    ctx.text(this.font, top, left + 300, yNow + 3, 0x9AD3FF);
                }
            };
            rows.add(new RenderedRow(null, null, null, null));
            addRenderableOnly(rowText);
            index++;
        }
    }

    private void layoutRows(int left, int listTop) {
        int index = 0;
        for (RenderedRow row : rows) {
            int y = listTop + 24 + index * ROW_HEIGHT - scrollOffset;
            if (row.minus != null) {
                row.minus.setPosition(left + 356, y);
                row.plusOne.setPosition(left + 388, y);
                row.plusStack.setPosition(left + 420, y);
            }
            index++;
        }
    }

    private int listTop(int top) {
        return top + 76;
    }

    private void openTab(Tab newTab) {
        Minecraft.getInstance().gui.setScreen(new ChecklistScreen(newTab));
    }

    private int rowCount() {
        return tab == Tab.BUILD ? ChecklistData.GOALS.size() : ChecklistData.GATHER.size();
    }

    private int goalNeeded(String builtKey) {
        String id = builtKey.substring(2);
        if (tab == Tab.BUILD) {
            return ChecklistData.needed(id);
        }
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
    public void extractRenderState(GuiGraphicsExtractor context, int mouseX, int mouseY, float delta) {
        int left = (this.width - SCREEN_WIDTH) / 2;
        int top = (this.height - SCREEN_HEIGHT) / 2;
        layoutRows(left, listTop(top));
        super.extractRenderState(context, mouseX, mouseY, delta);
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