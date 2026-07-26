package net.misemise;

import net.minecraft.block.BlockState;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.state.property.Properties;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.misemise.ClothConfig.Config;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Queue;
import java.util.Set;

public final class BlockTargetUtils {
    private static final Logger LOGGER = LoggerFactory.getLogger("omniminer");
    private static final int MAX_LEAF_DISTANCE = 10;
    private static final int[][] ADJACENT_OFFSETS = {
            {0, 1, 0}, {0, -1, 0},
            {0, 0, -1}, {0, 0, 1},
            {1, 0, 0}, {-1, 0, 0}
    };
    private static final int[][] DIAGONAL_OFFSETS = createDiagonalOffsets();

    private BlockTargetUtils() {
    }

    public static boolean canVeinMine(
            World world,
            BlockPos pos,
            BlockState state,
            ItemStack heldItem) {
        return canVeinMine(world, pos, state, heldItem, Config.includeBlockEntities);
    }

    public static boolean canVeinMine(
            World world,
            BlockPos pos,
            BlockState state,
            ItemStack heldItem,
            boolean includeBlockEntities) {
        if (world == null || pos == null || state == null || state.isAir()) {
            return false;
        }

        try {
            if (state.getHardness(world, pos) < 0.0F) {
                return false;
            }
        } catch (Throwable error) {
            LOGGER.warn("Failed to check block hardness for {}", state.getBlock(), error);
            return false;
        }

        if (!includeBlockEntities && state.hasBlockEntity()) {
            return false;
        }

        return hasEffectiveTool(state, heldItem);
    }

    public static boolean hasEffectiveTool(BlockState state, ItemStack heldItem) {
        if (state == null || state.isAir() || heldItem == null || heldItem.isEmpty()) {
            return false;
        }

        if (state.isToolRequired()) {
            return heldItem.isSuitableFor(state);
        }

        try {
            return heldItem.getMiningSpeedMultiplier(state) > 1.0F;
        } catch (Throwable error) {
            LOGGER.warn("Failed to check mining speed for {}", state.getBlock(), error);
            return false;
        }
    }

    public static String previewCacheKey(
            World world,
            BlockPos pos,
            BlockState state,
            ItemStack heldItem) {
        return previewCacheKey(
                world,
                pos,
                state,
                heldItem,
                Config.maxBlocks,
                Config.searchDiagonal,
                Config.includeBlockEntities,
                Config.breakLeaves);
    }

    public static String previewCacheKey(
            World world,
            BlockPos pos,
            BlockState state,
            ItemStack heldItem,
            int maxBlocks,
            boolean searchDiagonal,
            boolean includeBlockEntities,
            boolean breakLeaves) {
        String blockId = state == null
                ? "null"
                : Registries.BLOCK.getId(state.getBlock()).toString();
        String itemId = heldItem == null || heldItem.isEmpty()
                ? "empty"
                : Registries.ITEM.getId(heldItem.getItem()).toString();

        return System.identityHashCode(world)
                + "|" + (world.getTime() / 10L)
                + "|" + pos.asLong() + "|" + blockId + "|" + itemId
                + "|" + maxBlocks
                + "|" + searchDiagonal
                + "|" + includeBlockEntities
                + "|" + breakLeaves;
    }

    public static Set<BlockPos> findMiningTargets(
            World world,
            BlockPos startPos,
            BlockState targetState,
            ItemStack heldItem,
            int maxBlocks,
            boolean searchDiagonal,
            boolean includeBlockEntities,
            boolean breakLeaves) {
        Set<BlockPos> targets = findSphericalTargets(
                world,
                startPos,
                targetState,
                heldItem,
                maxBlocks,
                searchDiagonal,
                includeBlockEntities);

        if (!breakLeaves || !LogUtils.isLog(targetState) || targets.size() >= maxBlocks) {
            return targets;
        }

        addConnectedLeaves(world, targets, maxBlocks);
        return targets;
    }

    public static Set<BlockPos> findSphericalTargets(
            World world,
            BlockPos startPos,
            BlockState targetState,
            ItemStack heldItem,
            int maxBlocks,
            boolean searchDiagonal) {
        return findSphericalTargets(
                world,
                startPos,
                targetState,
                heldItem,
                maxBlocks,
                searchDiagonal,
                Config.includeBlockEntities);
    }

    public static Set<BlockPos> findSphericalTargets(
            World world,
            BlockPos startPos,
            BlockState targetState,
            ItemStack heldItem,
            int maxBlocks,
            boolean searchDiagonal,
            boolean includeBlockEntities) {
        Set<BlockPos> targets = new LinkedHashSet<>();
        if (maxBlocks <= 0
                || world == null
                || !world.isInBuildLimit(startPos)
                || !world.isPosLoaded(startPos.getX(), startPos.getZ())
                || !canVeinMine(
                        world,
                        startPos,
                        targetState,
                        heldItem,
                        includeBlockEntities)) {
            return targets;
        }

        Set<BlockPos> visited = new HashSet<>();
        Queue<BlockPos> queue = new ArrayDeque<>();
        queue.add(startPos);
        visited.add(startPos);

        while (!queue.isEmpty() && targets.size() < maxBlocks) {
            BlockPos pos = queue.remove();
            if (!world.isInBuildLimit(pos)
                    || !world.isPosLoaded(pos.getX(), pos.getZ())) {
                continue;
            }

            BlockState currentState = world.getBlockState(pos);
            if (!currentState.isOf(targetState.getBlock())
                    || !canVeinMine(
                            world,
                            pos,
                            currentState,
                            heldItem,
                            includeBlockEntities)) {
                continue;
            }

            targets.add(pos);
            for (BlockPos neighbor : neighbors(pos, searchDiagonal)) {
                if (visited.add(neighbor)) {
                    queue.add(neighbor);
                }
            }
        }

        return targets;
    }

    public static boolean isBreakableNaturalLeaf(BlockState state) {
        if (!LeafUtils.isLeaf(state)) {
            return false;
        }

        Optional<Boolean> persistent = state.getOrEmpty(Properties.PERSISTENT);
        Optional<Integer> distance = state.getOrEmpty(Properties.DISTANCE_1_7);
        return persistent.isPresent()
                && distance.isPresent()
                && !persistent.get()
                && distance.get() < 7;
    }

    public static boolean isDecayingNaturalLeaf(BlockState state) {
        if (!LeafUtils.isLeaf(state)) {
            return false;
        }

        Optional<Boolean> persistent = state.getOrEmpty(Properties.PERSISTENT);
        Optional<Integer> distance = state.getOrEmpty(Properties.DISTANCE_1_7);
        return persistent.isPresent()
                && distance.isPresent()
                && !persistent.get()
                && distance.get() == 7;
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

    public static List<BlockPos> neighbors(BlockPos center, boolean includeDiagonals) {
        int[][] offsets = includeDiagonals ? DIAGONAL_OFFSETS : ADJACENT_OFFSETS;
        List<BlockPos> neighbors = new ArrayList<>(offsets.length);
        for (int[] offset : offsets) {
            neighbors.add(center.add(offset[0], offset[1], offset[2]));
        }
        return neighbors;
    }

    private static void addConnectedLeaves(
            World world,
            Set<BlockPos> targets,
            int maxBlocks) {
        Set<BlockPos> logPositions = new LinkedHashSet<>(targets);
        Set<BlockPos> visited = new HashSet<>(targets);
        Queue<LeafNode> queue = new ArrayDeque<>();

        for (BlockPos logPos : logPositions) {
            enqueueLeafNeighbors(world, logPos, 1, visited, queue);
        }

        while (!queue.isEmpty() && targets.size() < maxBlocks) {
            LeafNode current = queue.remove();
            if (current.distance() > MAX_LEAF_DISTANCE) {
                continue;
            }

            BlockState state = world.getBlockState(current.pos());
            if (!isBreakableNaturalLeaf(state)) {
                continue;
            }

            targets.add(current.pos());
            enqueueLeafNeighbors(
                    world,
                    current.pos(),
                    current.distance() + 1,
                    visited,
                    queue);
        }
    }

    private static void enqueueLeafNeighbors(
            World world,
            BlockPos center,
            int distance,
            Set<BlockPos> visited,
            Queue<LeafNode> queue) {
        if (distance > MAX_LEAF_DISTANCE) {
            return;
        }

        for (BlockPos next : neighbors(center, true)) {
            if (!visited.add(next)) {
                continue;
            }

            if (world.isInBuildLimit(next)
                    && world.isPosLoaded(next.getX(), next.getZ())
                    && isBreakableNaturalLeaf(world.getBlockState(next))) {
                queue.add(new LeafNode(next, distance));
            }
        }
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
                    offsets[index++] = new int[]{dx, dy, dz};
                }
            }
        }
        return offsets;
    }

    private record LeafNode(BlockPos pos, int distance) {
    }
}
