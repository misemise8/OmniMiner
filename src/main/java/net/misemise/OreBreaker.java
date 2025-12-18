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

        // 最初のブロックから開始
        dfs(world, startPos, originalState, player, heldItem, visited);

        int blocksCount = visited.size();

        // クライアントに破壊したブロック数を送信
        NetworkHandler.sendBlocksMinedCount(player, blocksCount);
    }

    private static void dfs(ServerWorld world, BlockPos pos, BlockState targetState,
                            ServerPlayerEntity player, ItemStack heldItem, Set<BlockPos> visited) {
        // 上限チェック
        if (visited.size() >= Config.maxBlocks) {
            return;
        }

        // 既に訪問済み
        if (visited.contains(pos)) {
            return;
        }

        BlockState currentState = world.getBlockState(pos);

        // 原木を破壊している場合で、葉っぱ破壊が有効なら葉っぱも対象に含める
        boolean isTargetLog = LogUtils.isLog(targetState);
        boolean isCurrentLog = LogUtils.isLog(currentState);
        boolean isCurrentLeaf = LeafUtils.isLeaf(currentState);

        boolean shouldBreak = false;
        if (isTargetLog && Config.breakLeaves) {
            // 原木 → 原木または葉っぱ
            shouldBreak = isCurrentLog || isCurrentLeaf;
        } else {
            // 通常：同じ種類のブロックのみ
            shouldBreak = currentState.isOf(targetState.getBlock());
        }

        if (!shouldBreak) {
            return;
        }

        // 訪問済みにマーク
        visited.add(pos);

        // ブロックを破壊してドロップを自動回収
        AutoCollector.breakAndCollect(world, pos, currentState, player, heldItem);

        // 隣接ブロックを再帰的に処理
        if (Config.searchDiagonal) {
            // 26方向探索（上下左右前後 + 斜め）
            for (int dx = -1; dx <= 1; dx++) {
                for (int dy = -1; dy <= 1; dy++) {
                    for (int dz = -1; dz <= 1; dz++) {
                        // 中心（0,0,0）はスキップ
                        if (dx == 0 && dy == 0 && dz == 0) continue;

                        BlockPos neighbor = pos.add(dx, dy, dz);
                        dfs(world, neighbor, targetState, player, heldItem, visited);
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
                dfs(world, neighbor, targetState, player, heldItem, visited);
            }
        }
    }
}