package net.misemise;

import net.minecraft.block.BlockState;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.state.property.Properties;
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
     * 一括破壊全体で耐久値は1だけ消費
     */
    public static void breakConnectedOres(ServerWorld world, BlockPos startPos,
                                          BlockState originalState, ServerPlayerEntity player,
                                          ItemStack heldItem) {
        // ★★★ 一括破壊の開始時に耐久値を1だけ消費 ★★★
        if (heldItem != null && !heldItem.isEmpty() && heldItem.isDamageable()) {
            heldItem.damage(1, player, net.minecraft.entity.EquipmentSlot.MAINHAND);

            if (Config.debugLog) {
                LOGGER.info("Consumed 1 durability for vein mining: {}/{}",
                        heldItem.getMaxDamage() - heldItem.getDamage(), heldItem.getMaxDamage());
            }

            // ツールが壊れたかチェック
            if (heldItem.isEmpty() || heldItem.getCount() == 0) {
                if (Config.debugLog) {
                    LOGGER.info("Tool broke at the start of vein mining");
                }
                return; // ツールが壊れたので処理を中断
            }
        }

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

        // ブロックを破壊してドロップを自動回収（耐久値は消費しない）
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

    private static class LeafNode {
        BlockPos pos;
        int distance;

        LeafNode(BlockPos pos, int distance) {
            this.pos = pos;
            this.distance = distance;
        }
    }

    /**
     * 原木の周りの葉っぱを破壊（BFS方式で広範囲を探索）
     */
    private static void breakNearbyLeaves(ServerWorld world, Set<BlockPos> logPositions,
                                          ServerPlayerEntity player, ItemStack heldItem) {
        Set<BlockPos> visited = new HashSet<>();
        Queue<LeafNode> queue = new LinkedList<>();

        // 設定から最大距離を取得（デフォルトで8〜10程度がおすすめ）
        int maxLeafDistance = 10;

        // 1. 各原木の隣接26方向にある葉っぱを距離「1」として登録
        for (BlockPos logPos : logPositions) {
            for (int dx = -1; dx <= 1; dx++) {
                for (int dy = -1; dy <= 1; dy++) {
                    for (int dz = -1; dz <= 1; dz++) {
                        if (dx == 0 && dy == 0 && dz == 0) continue;

                        BlockPos neighbor = logPos.add(dx, dy, dz);
                        if (visited.contains(neighbor)) continue;

                        BlockState state = world.getBlockState(neighbor);
                        if (LeafUtils.isLeaf(state) && !state.get(Properties.PERSISTENT)) {
                            visited.add(neighbor);
                            queue.add(new LeafNode(neighbor, 1));
                        }
                    }
                }
            }
        }

        // 2. BFS（幅優先探索）で、設定した最大距離まで探索
        while (!queue.isEmpty()) {
            LeafNode current = queue.poll();
            BlockPos currentPos = current.pos;
            int currentDist = current.distance;

            // 指定した距離を超えたら、その先の探索を打ち切り
            if (currentDist > maxLeafDistance) continue;

            BlockState currentState = world.getBlockState(currentPos);
            if (!LeafUtils.isLeaf(currentState)) continue;

            // 葉っぱを破壊（耐久値は消費しない）
            AutoCollector.breakAndCollect(world, currentPos, currentState, player, heldItem);

            // 隣接する葉っぱを探索（距離を +1 する）
            for (int dx = -1; dx <= 1; dx++) {
                for (int dy = -1; dy <= 1; dy++) {
                    for (int dz = -1; dz <= 1; dz++) {
                        if (dx == 0 && dy == 0 && dz == 0) continue;

                        BlockPos nextPos = currentPos.add(dx, dy, dz);
                        if (visited.contains(nextPos)) continue;

                        BlockState nextState = world.getBlockState(nextPos);

                        // 自然な葉っぱ（!persistent）かつ、バニラの距離設定でも有効な範囲内か確認
                        if (LeafUtils.isLeaf(nextState) && !nextState.get(Properties.PERSISTENT)) {
                            // バニラの distance も併用すると、より精度が上がります
                            if (nextState.get(Properties.DISTANCE_1_7) < 7) {
                                visited.add(nextPos);
                                queue.add(new LeafNode(nextPos, currentDist + 1));
                            }
                        }
                    }
                }
            }
        }
    }
}