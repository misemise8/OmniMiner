package net.misemise;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.misemise.ClothConfig.Config;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * 統合版プレイヤーがしゃがんでいる間、パーティクルでプレビューを表示
 */
public class BedrockPreviewSystem {
    private static final Logger LOGGER = LoggerFactory.getLogger("omniminer");
    private static final Map<UUID, PreviewData> previewCache = new HashMap<>();
    private static int tickCounter = 0;

    public static void register() {
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            tickCounter++;

            // 5tick（0.25秒）ごとに更新
            if (tickCounter % 5 != 0) {
                return;
            }

            for (ServerLevel world : server.getAllLevels()) {
                for (ServerPlayer player : world.players()) {
                    updatePreview(player, world);
                }
            }
        });

        OmniMiner.LOGGER.info("BedrockPreviewSystem registered");
    }

    private static void updatePreview(ServerPlayer player, ServerLevel world) {
        UUID playerId = player.getUUID();

        // 統合版プレイヤーかチェック
        if (!BedrockPlayerUtils.isBedrockPlayer(player)) {
            previewCache.remove(playerId);
            return;
        }

        // パーティクル表示が無効ならスキップ
        if (!Config.bedrockShowParticles || !Config.bedrockSneakEnable) {
            previewCache.remove(playerId);
            return;
        }

        // しゃがんでいるかチェック
        if (!player.isShiftKeyDown()) {
            previewCache.remove(playerId);
            return;
        }

        // プレイヤーが見ているブロックを取得
        HitResult hitResult = player.pick(5.0, 0.0f, false);
        if (hitResult.getType() != HitResult.Type.BLOCK) {
            previewCache.remove(playerId);
            return;
        }

        BlockHitResult blockHit = (BlockHitResult) hitResult;
        BlockPos targetPos = blockHit.getBlockPos();
        BlockState targetState = world.getBlockState(targetPos);

        // ツールチェック
        boolean isPickaxe = ToolUtils.isPickaxe(player.getMainHandItem());
        boolean isAxe = ToolUtils.isAxe(player.getMainHandItem());
        boolean isOre = isPickaxe && OreUtils.isOre(targetState);
        boolean isLog = isAxe && LogUtils.isLog(targetState);

        if (!isOre && !isLog) {
            previewCache.remove(playerId);
            return;
        }

        // キャッシュチェック（同じ位置なら再計算しない）
        PreviewData cached = previewCache.get(playerId);
        if (cached != null && cached.targetPos.equals(targetPos)) {
            // パーティクルを再表示
            showPreviewParticles(world, cached.blocks, player);
            return;
        }

        // 新しい範囲を計算
        Set<BlockPos> connectedBlocks = findConnectedBlocks(world, targetPos, targetState);
        previewCache.put(playerId, new PreviewData(targetPos, connectedBlocks));

        // パーティクル表示
        showPreviewParticles(world, connectedBlocks, player);

        // デバッグログ
        if (Config.debugLog) {
            LOGGER.info("Bedrock preview for {}: {} blocks at {}",
                    player.getName().getString(), connectedBlocks.size(), targetPos);
        }
    }

    private static void showPreviewParticles(ServerLevel world, Set<BlockPos> blocks, ServerPlayer player) {
        if (Config.bedrockParticleMode == 1) {
            BedrockVisualHelper.showDetailedParticleOutline(world, blocks, player);
        } else {
            BedrockVisualHelper.showParticleOutline(world, blocks, player);
        }
    }

    private static Set<BlockPos> findConnectedBlocks(ServerLevel world, BlockPos startPos, BlockState targetState) {
        Set<BlockPos> visited = new HashSet<>();
        dfs(world, startPos, targetState, visited);
        return visited;
    }

    private static void dfs(ServerLevel world, BlockPos pos, BlockState targetState, Set<BlockPos> visited) {
        if (visited.size() >= Config.maxBlocks || visited.contains(pos)) {
            return;
        }

        BlockState currentState = world.getBlockState(pos);

        // 原木を破壊している場合で、葉っぱ破壊が有効なら葉っぱも対象に含める
        boolean isTargetLog = LogUtils.isLog(targetState);
        boolean isCurrentLog = LogUtils.isLog(currentState);
        boolean isCurrentLeaf = LeafUtils.isLeaf(currentState);

        boolean shouldInclude = false;
        if (isTargetLog && Config.breakLeaves) {
            shouldInclude = isCurrentLog || isCurrentLeaf;
        } else {
            shouldInclude = currentState.is(targetState.getBlock());
        }

        if (!shouldInclude) {
            return;
        }

        visited.add(pos);

        if (Config.searchDiagonal) {
            for (int dx = -1; dx <= 1; dx++) {
                for (int dy = -1; dy <= 1; dy++) {
                    for (int dz = -1; dz <= 1; dz++) {
                        if (dx == 0 && dy == 0 && dz == 0) continue;
                        dfs(world, pos.offset(dx, dy, dz), targetState, visited);
                    }
                }
            }
        } else {
            BlockPos[] neighbors = {
                    pos.above(), pos.below(), pos.north(), pos.south(), pos.east(), pos.west()
            };
            for (BlockPos neighbor : neighbors) {
                dfs(world, neighbor, targetState, visited);
            }
        }
    }

    private static class PreviewData {
        final BlockPos targetPos;
        final Set<BlockPos> blocks;

        PreviewData(BlockPos targetPos, Set<BlockPos> blocks) {
            this.targetPos = targetPos;
            this.blocks = blocks;
        }
    }
}