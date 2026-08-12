package com.dali951.xpfarmchestscan.scan;

import com.dali951.xpfarmchestscan.config.ConfigStore;
import com.dali951.xpfarmchestscan.config.ModConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ServerboundContainerClosePacket;
import net.minecraft.network.protocol.game.ServerboundUseItemOnPacket;
import net.minecraft.resources.Identifier;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.BucketItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class ChestScanner {

    public static final ChestScanner INSTANCE = new ChestScanner();

    private enum Phase {
        IDLE, WAITING_FOR_OPEN
    }

    private Phase phase = Phase.IDLE;
    private final List<BlockPos> queue = new ArrayList<>();
    private BlockPos current = null;
    private int waitingTicks = 0;
    private int sequence = 0;
    private final ScanResult result = new ScanResult();

    public static boolean scanning = false;
    public static int expectedContainerId = -1;
    public static List<ItemStack> capturedItems = null;

    public void start() {
        Minecraft mc = Minecraft.getInstance();
        if (phase != Phase.IDLE) {
            return;
        }
        if (mc.player == null || mc.level == null || mc.getConnection() == null) {
            sendMessage(mc, Component.translatable("xpfarm-chestscan.msg.notOnServer"));
            return;
        }
        if (mc.gui.screen() != null) {
            sendMessage(mc, Component.translatable("xpfarm-chestscan.msg.closeScreen"));
            return;
        }

        ModConfig config = ConfigStore.get();
        if (!anySafeHand(mc)) {
            sendMessage(mc, Component.literal("Both hands hold placeable items (blocks/buckets). Empty one hand so the scan can't place anything."));
            return;
        }

        queue.clear();
        queue.addAll(positionsToScan(mc, config));

        if (queue.isEmpty()) {
            sendMessage(mc, Component.translatable("xpfarm-chestscan.msg.noChests", config.radius));
            return;
        }

        scanning = true;
        result.chests.clear();
        result.totals.clear();
        result.skipped.clear();
        result.generatedAt = System.currentTimeMillis();
        sendMessage(mc, Component.literal("Scanning " + queue.size() + " chest" + (queue.size() == 1 ? "" : "s") + "..."));
        tick();
    }

    public void tick() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.getConnection() == null) {
            resetToIdle();
            return;
        }
        if (phase != Phase.IDLE) {
            if (capturedItems != null) {
                finishCurrent(mc);
                return;
            }
            waitingTicks--;
            if (waitingTicks <= 0) {
                if (current != null) {
                    result.skipped.add(current);
                }
                closePending(mc);
                clearIntercept();
                phase = Phase.IDLE;
            }
            return;
        }
        if (queue.isEmpty()) {
            if (scanning) {
                finishScan(mc);
            }
            return;
        }
        current = queue.remove(0);
        if (!(mc.level.getBlockState(current).getBlock() instanceof ChestBlock)) {
            result.skipped.add(current);
            return;
        }
        sendUseItemOn(mc, current);
        clearIntercept();
        phase = Phase.WAITING_FOR_OPEN;
        waitingTicks = 80;
    }

    private void finishCurrent(Minecraft mc) {
        ChestEntry entry = new ChestEntry(current);
        if (capturedItems != null) {
            for (ItemStack stack : capturedItems) {
                if (stack == null || stack.isEmpty()) {
                    continue;
                }
                Item item = stack.getItem();
                Identifier key = BuiltInRegistries.ITEM.getKey(item);
                if (key == null) {
                    continue;
                }
                entry.addItem(key.toString(), stack.getCount());
            }
        }
        result.add(entry);
        closePending(mc);
        restoreScreen(mc);
        clearIntercept();
        phase = Phase.IDLE;
    }

    private void finishScan(Minecraft mc) {
        scanning = false;
        phase = Phase.IDLE;
        clearIntercept();
        ChestDumpWriter.write(mc, result);
        if (mc.player != null) {
            mc.player.sendSystemMessage(Component.translatable(
                    "xpfarm-chestscan.msg.saved",
                    result.chests.size(),
                    result.totalItems(),
                    ChestDumpWriter.outputPath(mc).toAbsolutePath()));
        }
        mc.gui.setScreen(new com.dali951.xpfarmchestscan.gui.ScanSummaryScreen(result));
    }

    public static void onContainerOpened(int containerId) {
        if (scanning) {
            expectedContainerId = containerId;
        }
    }

    public static void onContainerContent(int containerId, List<ItemStack> items) {
        if (scanning && expectedContainerId != -1 && containerId == expectedContainerId) {
            capturedItems = items;
        }
    }

    private void sendUseItemOn(Minecraft mc, BlockPos pos) {
        Vec3 location = Vec3.atCenterOf(pos);
        BlockHitResult hit = new BlockHitResult(location, Direction.UP, pos, false);
        InteractionHand hand = pickSafeHand(mc);
        sequence++;
        mc.getConnection().send(new ServerboundUseItemOnPacket(hand, hit, sequence));
    }

    private void closePending(Minecraft mc) {
        if (expectedContainerId != -1 && mc.getConnection() != null) {
            mc.getConnection().send(new ServerboundContainerClosePacket(expectedContainerId));
        }
    }

    private void restoreScreen(Minecraft mc) {
        if (mc.gui.screen() != null) {
            mc.gui.setScreen(null);
        }
    }

    private void clearIntercept() {
        expectedContainerId = -1;
        capturedItems = null;
    }

    private void resetToIdle() {
        phase = Phase.IDLE;
        scanning = false;
        queue.clear();
        current = null;
        clearIntercept();
    }

    private List<BlockPos> positionsToScan(Minecraft mc, ModConfig config) {
        List<BlockPos> out = new ArrayList<>();
        double r2 = config.radius * config.radius;
        Set<Long> seenPairKeys = new HashSet<>();
        BlockPos playerPos = mc.player.blockPosition();
        for (int[] p : config.positions) {
            BlockPos pos = new BlockPos(p[0], p[1], p[2]);
            double dx = pos.getX() + 0.5 - (playerPos.getX() + 0.5);
            double dy = pos.getY() + 0.5 - (playerPos.getY() + 0.5);
            double dz = pos.getZ() + 0.5 - (playerPos.getZ() + 0.5);
            if (dx * dx + dy * dy + dz * dz > r2) {
                continue;
            }
            if (!(mc.level.getBlockState(pos).getBlock() instanceof ChestBlock)) {
                continue;
            }
            BlockPos primary = pairPrimary(mc, pos);
            if (seenPairKeys.add(primary.asLong())) {
                out.add(primary);
            }
        }
        out.sort(null);
        return out;
    }

    private static BlockPos pairPrimary(Minecraft mc, BlockPos pos) {
        if (mc.level.getBlockState(pos.west()).getBlock() instanceof ChestBlock) {
            return pos.west();
        }
        if (mc.level.getBlockState(pos.north()).getBlock() instanceof ChestBlock) {
            return pos.north();
        }
        return pos;
    }

    private boolean anySafeHand(Minecraft mc) {
        return !isPlaceable(mc.player.getMainHandItem()) || !isPlaceable(mc.player.getOffhandItem());
    }

    private InteractionHand pickSafeHand(Minecraft mc) {
        if (isPlaceable(mc.player.getMainHandItem())) {
            return InteractionHand.OFF_HAND;
        }
        return InteractionHand.MAIN_HAND;
    }

    private boolean isPlaceable(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return false;
        }
        Item item = stack.getItem();
        return item instanceof BlockItem || item instanceof BucketItem;
    }

    private void sendMessage(Minecraft mc, Component message) {
        if (mc.player != null) {
            mc.player.sendSystemMessage(message);
        }
    }

    public static boolean isScanning() {
        return scanning;
    }
}
