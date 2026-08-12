package com.dali951.xpfarmchestscan.gui;

import com.dali951.xpfarmchestscan.config.ConfigStore;
import com.dali951.xpfarmchestscan.config.ModConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.ChestBlock;

import java.util.ArrayList;
import java.util.List;

public class ConfigScreen extends Screen {

    private static final int SCREEN_WIDTH = 340;
    private static final int SCREEN_HEIGHT = 300;
    private static final int BUTTON_HEIGHT = 20;
    private static final int ROW_HEIGHT = 22;
    private static final int VISIBLE_ROWS = 5;

    private final Screen parent;
    private ModConfig working;

    private final List<PositionRow> rows = new ArrayList<>();
    private EditBox outputBox;
    private int scrollOffset = 0;

    private static class PositionRow {
        final int[] pos;
        final Button remove;

        PositionRow(int[] pos, Button remove) {
            this.pos = pos;
            this.remove = remove;
        }
    }

    public ConfigScreen(Screen parent) {
        super(Component.translatable("xpfarm-chestscan.config.title"));
        this.parent = parent;
        this.working = ConfigStore.get();
    }

    @Override
    protected void init() {
        int left = (this.width - SCREEN_WIDTH) / 2;
        int top = (this.height - SCREEN_HEIGHT) / 2;
        int listTop = top + 28;
        int listBottom = listTop + VISIBLE_ROWS * ROW_HEIGHT + 6;
        scrollOffset = 0;

        Button addTargetButton = Button.builder(Component.translatable("xpfarm-chestscan.config.addTarget"), b -> {
            addTargetedChest();
            init();
        }).bounds(left, listBottom + 6, 122, BUTTON_HEIGHT).build();

        Button addSelfButton = Button.builder(Component.translatable("xpfarm-chestscan.config.addSelf"), b -> {
            Minecraft mc = Minecraft.getInstance();
            if (mc.player != null) {
                BlockPos p = mc.player.blockPosition();
                working.addPosition(p.getX(), p.getY(), p.getZ());
            }
            init();
        }).bounds(left + 128, listBottom + 6, SCREEN_WIDTH - 128, BUTTON_HEIGHT).build();

        int radiusRow = listBottom + 34;
        Button closerRadiusButton = Button.builder(Component.literal("-"), b -> {
            adjustRadius(-0.5);
            init();
        }).bounds(left, radiusRow, 20, BUTTON_HEIGHT).build();

        Button fartherRadiusButton = Button.builder(Component.literal("+"), b -> {
            adjustRadius(0.5);
            init();
        }).bounds(left + 130, radiusRow, 20, BUTTON_HEIGHT).build();

        int outputRow = radiusRow + 28;
        outputBox = new EditBox(this.font, left, outputRow, SCREEN_WIDTH, BUTTON_HEIGHT,
                Component.translatable("xpfarm-chestscan.config.output"));
        outputBox.setValue(working.outputFileName);
        outputBox.setMaxLength(120);

        int buttonsRow = outputRow + 32;
        Button saveButton = Button.builder(Component.translatable("xpfarm-chestscan.config.save"), b -> {
            working.outputFileName = outputBox.getValue().trim();
            ConfigStore.set(working);
            ConfigStore.save();
            this.onClose();
        }).bounds(left, buttonsRow, 160, BUTTON_HEIGHT).build();

        Button cancelButton = Button.builder(Component.translatable("xpfarm-chestscan.config.cancel"), b -> this.onClose())
                .bounds(left + 168, buttonsRow, SCREEN_WIDTH - 168, BUTTON_HEIGHT).build();

        rebuildRows(left, listTop);
        addRenderableWidget(addTargetButton);
        addRenderableWidget(addSelfButton);
        addRenderableWidget(closerRadiusButton);
        addRenderableWidget(fartherRadiusButton);
        addRenderableWidget(outputBox);
        addRenderableWidget(saveButton);
        addRenderableWidget(cancelButton);
    }

    private void rebuildRows(int left, int listTop) {
        for (PositionRow row : rows) {
            removeWidget(row.remove);
        }
        rows.clear();
        int index = 0;
        for (int[] p : working.positions) {
            final int removeIndex = index++;
            Button remove = Button.builder(Component.translatable("xpfarm-chestscan.config.remove"), b -> {
                working.removePosition(removeIndex);
                init();
            }).bounds(left + 216, listTop + 4 + (index - 1) * ROW_HEIGHT, 44, 18).build();
            rows.add(new PositionRow(p, remove));
            addRenderableWidget(remove);
        }
    }

    private void addTargetedChest() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.hitResult == null || mc.level == null) {
            return;
        }
        if (mc.hitResult instanceof net.minecraft.world.phys.BlockHitResult hit
                && mc.level.getBlockState(hit.getBlockPos()).getBlock() instanceof ChestBlock) {
            BlockPos p = hit.getBlockPos();
            working.addPosition(p.getX(), p.getY(), p.getZ());
        }
    }

    private void adjustRadius(double delta) {
        working.radius = Math.min(6.0, Math.max(1.0, working.radius + delta));
        working.radius = Math.round(working.radius * 2.0) / 2.0;
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor context, int mouseX, int mouseY, float delta) {
        int left = (this.width - SCREEN_WIDTH) / 2;
        int top = (this.height - SCREEN_HEIGHT) / 2;
        int listTop = top + 28;
        int listBottom = listTop + VISIBLE_ROWS * ROW_HEIGHT + 6;

        context.fill(left, listTop, left + SCREEN_WIDTH, listBottom, 0xCC181818);

        super.extractRenderState(context, mouseX, mouseY, delta);

        context.centeredText(this.font, this.title, this.width / 2, top + 8, 0xFFFFFF);
        context.text(this.font, Component.translatable("xpfarm-chestscan.config.positions").getString(),
                left + 6, listTop + 4, 0x9AD3FF);

        if (working.size() == 0) {
            context.text(this.font, Component.translatable("xpfarm-chestscan.config.positions.empty").getString(),
                    left + 8, listTop + 26, 0x888888);
        } else {
            int index = 0;
            for (PositionRow row : rows) {
                int y = listTop + 26 + index * ROW_HEIGHT - scrollOffset;
                if (y >= listTop + 6 && y < listBottom - 8) {
                    context.text(this.font,
                            "[" + row.pos[0] + ", " + row.pos[1] + ", " + row.pos[2] + "]",
                            left + 10, y, 0xE0E0E0);
                }
                index++;
            }
        }

        int radiusRow = listBottom + 34;
        context.text(this.font, Component.translatable("xpfarm-chestscan.config.radius").getString(),
                left + 26, radiusRow + 6, 0x9AD3FF);
        String radiusText = working.radius == Math.floor(working.radius)
                ? String.valueOf((int) working.radius) : String.valueOf(working.radius);
        context.centeredText(this.font, Component.literal(radiusText + " blocks"),
                left + 82, radiusRow + 6, 0xFFFFFF);

        int outputRow = radiusRow + 28;
        context.text(this.font, Component.translatable("xpfarm-chestscan.config.output").getString(),
                left + 6, outputRow - 12, 0x9AD3FF);
        if (working.radius > 4.5) {
            context.text(this.font,
                    "! vanilla reach is 4.5 — farther chests will be skipped",
                    left, outputRow + 26, 0xFFAA66);
        }
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        int maxScroll = Math.max(0, working.size() * ROW_HEIGHT - VISIBLE_ROWS * ROW_HEIGHT);
        scrollOffset = (int) Math.max(0, Math.min(maxScroll, scrollOffset - verticalAmount * 8));
        return true;
    }

    @Override
    public void onClose() {
        Minecraft.getInstance().gui.setScreen(parent);
    }
}
