package net.misemise;

import net.minecraft.block.BlockState;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.misemise.ClothConfig.Config;
import net.misemise.network.NetworkHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.Queue;
import java.util.Set;

/**
 * OreBreaker - 隣接する同じ種類の鉱石・原木・葉っぱを一括破壊
 */
public class OreBreaker {
    private static final Logger LOGGER = LoggerFactory.getLogger("omniminer");

    /**
     * 指定位置から同じ種類のブロックを探して一括破壊
     */
    public static void breakConnectedOres(ServerWorld world, BlockPos startPos,
                                          BlockState originalState, ServerPlayerEntity player,
                                          ItemStack heldItem) {
        Set<BlockPos> visited = new HashSet<>();
        Set<BlockPos> logPositions = new HashSet<>();

        // 原木かどうかを判定
        boolean isLog = LogUtils.isLog(originalState);

        // 最初のブロックから開始（原木/鉱石のみ）
        dfs(world, startPos, originalState, player, heldItem, visited, logPositions, isLog);

        // 原木を破壊した数（葉っぱを除く）
        int mainBlockCount = visited.size();

        // 葉っぱ破壊が有効で、原木を破壊した場合
        if (isLog && Config.breakLeaves && !logPositions.isEmpty()) {
            breakNearbyLeaves(world, logPositions, player, heldItem);
        }

        // ブロックタイプを判定して送信（葉っぱの数は含めない）
        String blockType = isLog ? "log" : "ore";
        NetworkHandler.sendBlocksMinedCount(player, mainBlockCount, blockType);
    }

    private static void dfs(ServerWorld world, BlockPos pos, BlockState targetState,
                            ServerPlayerEntity player, ItemStack heldItem, Set<BlockPos> visited,
                            Set<BlockPos> logPositions, boolean isTreeMining) {
        // 上限チェック
        if (visited.size() >= Config.maxBlocks) {
            return;
        }

        // 既に訪問済み
        if (visited.contains(pos)) {
            return;
        }

        BlockState currentState = world.getBlockState(pos);

        // 同じ種類のブロックのみ
        if (!currentState.isOf(targetState.getBlock())) {
            return;
        }

        // 訪問済みにマーク
        visited.add(pos);

        // 原木の場合は位置を記録
        if (isTreeMining && LogUtils.isLog(currentState)) {
            logPositions.add(pos);
        }

        // ブロックを破壊してドロップを自動回収
        AutoCollector.breakAndCollect(world, pos, currentState, player, heldItem);

        // 隣接ブロックを再帰的に処理
        if (Config.searchDiagonal) {
            // 26方向探索（上下左右前後 + 斜め）
            for (int dx = -1; dx <= 1; dx++) {
                for (int dy = -1; dy <= 1; dy++) {
                    for (int dz = -1; dz <= 1; dz++) {
                        if (dx == 0 && dy == 0 && dz == 0) continue;
                        BlockPos neighbor = pos.add(dx, dy, dz);
                        dfs(world, neighbor, targetState, player, heldItem, visited, logPositions, isTreeMining);
                    }
                }
            }
        } else {
            // 6方向探索（上下左右前後のみ）
            BlockPos[] neighbors = {
                    pos.up(), pos.down(),
                    pos.north(), pos.south(),
                    pos.east(), pos.west()
            };
            for (BlockPos neighbor : neighbors) {
                dfs(world, neighbor, targetState, player, heldItem, visited, logPositions, isTreeMining);
            }
        }
    }

    /**
     * 原木の周りの葉っぱを破壊（BFS方式で広範囲を探索）
     */
    private static void breakNearbyLeaves(ServerWorld world, Set<BlockPos> logPositions,
                                          ServerPlayerEntity player, ItemStack heldItem) {
        Set<BlockPos> leavesVisited = new HashSet<>();
        Queue<BlockPos> leafQueue = new LinkedList<>();

        // 各原木の周囲5ブロック以内から葉っぱを探す
        for (BlockPos logPos : logPositions) {
            for (int dx = -5; dx <= 5; dx++) {
                for (int dy = -5; dy <= 5; dy++) {
                    for (int dz = -5; dz <= 5; dz++) {
                        BlockPos checkPos = logPos.add(dx, dy, dz);

                        if (leavesVisited.contains(checkPos)) {
                            continue;
                        }

                        BlockState state = world.getBlockState(checkPos);
                        if (LeafUtils.isLeaf(state)) {
                            leavesVisited.add(checkPos);
                            leafQueue.add(checkPos);
                        }
                    }
                }
            }
        }

        // BFSで葉っぱを連鎖的に探索して破壊
        while (!leafQueue.isEmpty()) {
            BlockPos leafPos = leafQueue.poll();
            BlockState leafState = world.getBlockState(leafPos);

            if (LeafUtils.isLeaf(leafState)) {
                // 葉っぱを破壊
                AutoCollector.breakAndCollect(world, leafPos, leafState, player, heldItem);

                // 隣接する葉っぱも探索（3ブロック以内）
                for (int dx = -3; dx <= 3; dx++) {
                    for (int dy = -3; dy <= 3; dy++) {
                        for (int dz = -3; dz <= 3; dz++) {
                            if (dx == 0 && dy == 0 && dz == 0) continue;

                            BlockPos nearbyPos = leafPos.add(dx, dy, dz);

                            if (leavesVisited.contains(nearbyPos)) {
                                continue;
                            }

                            BlockState nearbyState = world.getBlockState(nearbyPos);
                            if (LeafUtils.isLeaf(nearbyState)) {
                                leavesVisited.add(nearbyPos);
                                leafQueue.add(nearbyPos);
                            }
                        }
                    }
                }
            }
        }
    }
}