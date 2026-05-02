package net.misemise;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.misemise.ClothConfig.Config;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.PriorityQueue;
import java.util.Set;

public final class BlockTargetUtils {
    private static final Logger LOGGER = LoggerFactory.getLogger("omniminer");
    private static final int[][] ADJACENT_OFFSETS = {
            { 0, 1, 0 }, { 0, -1, 0 },
            { 0, 0, -1 }, { 0, 0, 1 },
            { 1, 0, 0 }, { -1, 0, 0 }
    };
    private static final int[][] DIAGONAL_OFFSETS = createDiagonalOffsets();

    private BlockTargetUtils() {
    }

    public static boolean canVeinMine(Level world, BlockPos pos, BlockState state, ItemStack heldItem) {
        if (world == null || pos == null || state == null || state.isAir()) {
            return false;
        }

        try {
            if (state.getDestroySpeed(world, pos) < 0.0f) {
                return false;
            }
        } catch (Throwable e) {
            LOGGER.warn("Failed to check block destroy speed for {}", state.getBlock(), e);
            return false;
        }

        if (!Config.includeBlockEntities && state.hasBlockEntity()) {
            return false;
        }

        return hasEffectiveTool(state, heldItem);
    }

    public static boolean hasEffectiveTool(BlockState state, ItemStack heldItem) {
        if (state == null || state.isAir()) {
            return false;
        }

        if (heldItem == null || heldItem.isEmpty()) {
            return false;
        }

        if (state.requiresCorrectToolForDrops()) {
            return heldItem.isCorrectToolForDrops(state);
        }

        try {
            return heldItem.getDestroySpeed(state) > 1.0f;
        } catch (Throwable e) {
            LOGGER.warn("Failed to check tool destroy speed for {}", state.getBlock(), e);
            return false;
        }
    }

    public static boolean canHarvestDrops(BlockState state, ItemStack heldItem) {
        if (state == null || state.isAir()) {
            return false;
        }

        if (!state.requiresCorrectToolForDrops()) {
            return true;
        }

        return heldItem != null && !heldItem.isEmpty() && heldItem.isCorrectToolForDrops(state);
    }

    public static String previewCacheKey(BlockPos pos, BlockState state, ItemStack heldItem) {
        String blockId = state == null
                ? "null"
                : BuiltInRegistries.BLOCK.getKey(state.getBlock()).toString();
        String itemId = heldItem == null || heldItem.isEmpty()
                ? "empty"
                : BuiltInRegistries.ITEM.getKey(heldItem.getItem()).toString();

        return pos.asLong() + "|" + blockId + "|" + itemId
                + "|" + Config.maxBlocks
                + "|" + Config.searchDiagonal
                + "|" + Config.includeBlockEntities;
    }

    public static Set<BlockPos> findSphericalTargets(Level world, BlockPos startPos, BlockState targetState,
            ItemStack heldItem, int maxBlocks, boolean searchDiagonal) {
        Set<BlockPos> targets = new LinkedHashSet<>();
        if (maxBlocks <= 0 || !canVeinMine(world, startPos, targetState, heldItem)) {
            return targets;
        }

        int radius = radiusForMaxBlocks(maxBlocks);
        int radiusSq = radius * radius;
        int[][] offsets = searchDiagonal ? DIAGONAL_OFFSETS : ADJACENT_OFFSETS;
        Set<BlockPos> visited = new HashSet<>();
        PriorityQueue<BlockPos> queue = new PriorityQueue<>(Comparator
                .comparingInt((BlockPos pos) -> distanceSq(startPos, pos))
                .thenComparingInt(BlockPos::getY)
                .thenComparingInt(BlockPos::getX)
                .thenComparingInt(BlockPos::getZ));
        queue.add(startPos);

        while (!queue.isEmpty() && targets.size() < maxBlocks) {
            BlockPos pos = queue.poll();
            if (!visited.add(pos) || distanceSq(startPos, pos) > radiusSq) {
                continue;
            }

            BlockState currentState = world.getBlockState(pos);
            if (!currentState.is(targetState.getBlock()) || !canVeinMine(world, pos, currentState, heldItem)) {
                continue;
            }

            targets.add(pos);

            for (int[] offset : offsets) {
                BlockPos next = pos.offset(offset[0], offset[1], offset[2]);
                if (!visited.contains(next) && distanceSq(startPos, next) <= radiusSq) {
                    queue.add(next);
                }
            }
        }

        return targets;
    }

    public static String blockType(BlockState state) {
        if (LogUtils.isLog(state)) {
            return "log";
        }
        if (OreUtils.isOre(state)) {
            return "ore";
        }
        return "block";
    }

    private static int[][] createDiagonalOffsets() {
        int[][] offsets = new int[26][3];
        int index = 0;
        for (int dx = -1; dx <= 1; dx++) {
            for (int dy = -1; dy <= 1; dy++) {
                for (int dz = -1; dz <= 1; dz++) {
                    if (dx == 0 && dy == 0 && dz == 0) {
                        continue;
                    }
                    offsets[index++] = new int[] { dx, dy, dz };
                }
            }
        }
        return offsets;
    }

    private static int radiusForMaxBlocks(int maxBlocks) {
        int radius = 0;
        while (countBlocksInSphere(radius) < maxBlocks) {
            radius++;
        }
        return radius;
    }

    private static int countBlocksInSphere(int radius) {
        int radiusSq = radius * radius;
        int count = 0;
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dy = -radius; dy <= radius; dy++) {
                for (int dz = -radius; dz <= radius; dz++) {
                    if (dx * dx + dy * dy + dz * dz <= radiusSq) {
                        count++;
                    }
                }
            }
        }
        return count;
    }

    private static int distanceSq(BlockPos origin, BlockPos pos) {
        int dx = pos.getX() - origin.getX();
        int dy = pos.getY() - origin.getY();
        int dz = pos.getZ() - origin.getZ();
        return dx * dx + dy * dy + dz * dz;
    }
}
