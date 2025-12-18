package net.misemise.keybind;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.block.BlockState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.misemise.ClothConfig.Config;
import net.misemise.ClothConfig.ConfigScreen;
import net.misemise.LogUtils;
import net.misemise.OmniMiner;
import net.misemise.OreUtils;
import net.misemise.ToolUtils;
import net.misemise.client.BlockHighlightRenderer;
import net.misemise.client.VeinMiningHud;
import net.misemise.network.NetworkHandler;

import java.util.HashSet;
import java.util.Set;

/**
 * クライアント側でキーの状態を監視してサーバーに送信
 */
public class KeyStateTracker {
    private static boolean lastKeyState = false;
    private static BlockPos lastTargetPos = null;
    private static int lastBlockCount = 0;
    private static boolean toggledOn = false; // トグルモード用の状態

    public static void register() {
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (client.player == null || client.world == null) return;

            // 一括採掘キーの状態を監視
            boolean currentKeyPressed = KeyBindings.isVeinMinerKeyPressed();

            // トグルモードの場合
            if (Config.toggleMode) {
                // キーが押された瞬間（前回は押されていなかった）
                if (currentKeyPressed && !lastKeyState) {
                    toggledOn = !toggledOn; // 状態を反転
                    OmniMiner.LOGGER.info("Toggle mode switched: {}", toggledOn);
                    NetworkHandler.sendKeyState(toggledOn);
                }
                lastKeyState = currentKeyPressed;

                // トグル状態に応じてハイライトを更新
                if (toggledOn) {
                    updateHighlight(client);
                } else {
                    if (lastTargetPos != null) {
                        BlockHighlightRenderer.clearHighlights();
                        VeinMiningHud.clearPreview();
                        lastTargetPos = null;
                        lastBlockCount = 0;
                    }
                }
            } else {
                // 通常モード（押している間だけ有効）
                // キー状態が変わった場合、サーバーに通知
                if (currentKeyPressed != lastKeyState) {
                    OmniMiner.LOGGER.info("Key state changed: {} -> {}", lastKeyState, currentKeyPressed);
                    NetworkHandler.sendKeyState(currentKeyPressed);
                    lastKeyState = currentKeyPressed;
                }

                // キーが押されている間、ハイライトを更新
                if (currentKeyPressed) {
                    updateHighlight(client);
                } else {
                    // キーが離されたらハイライトをクリア
                    if (lastTargetPos != null) {
                        BlockHighlightRenderer.clearHighlights();
                        VeinMiningHud.clearPreview();
                        lastTargetPos = null;
                        lastBlockCount = 0;
                    }
                }
            }

            // 設定画面を開くキー
            if (KeyBindings.wasOpenConfigPressed()) {
                MinecraftClient.getInstance().setScreen(ConfigScreen.createConfigScreen(client.currentScreen));
            }
        });

        OmniMiner.LOGGER.info("KeyStateTracker registered");
    }

    /**
     * ハイライト表示を更新
     */
    private static void updateHighlight(MinecraftClient client) {
        if (client.crosshairTarget == null || client.crosshairTarget.getType() != HitResult.Type.BLOCK) {
            // ブロックを見ていない場合はクリア
            if (lastTargetPos != null) {
                BlockHighlightRenderer.clearHighlights();
                VeinMiningHud.clearPreview();
                lastTargetPos = null;
                lastBlockCount = 0;
            }
            return;
        }

        BlockHitResult blockHit = (BlockHitResult) client.crosshairTarget;
        BlockPos targetPos = blockHit.getBlockPos();
        BlockState targetState = client.world.getBlockState(targetPos);

        // ツールの種類を判定
        boolean isPickaxe = ToolUtils.isPickaxe(client.player.getMainHandStack());
        boolean isAxe = ToolUtils.isAxe(client.player.getMainHandStack());

        // ブロックの種類を判定
        boolean isOre = isPickaxe && OreUtils.isOre(targetState);
        boolean isLog = isAxe && LogUtils.isLog(targetState);

        // つるはし+鉱石、または斧+原木の組み合わせでのみハイライト
        if (!isOre && !isLog) {
            if (lastTargetPos != null) {
                BlockHighlightRenderer.clearHighlights();
                VeinMiningHud.clearPreview();
                lastTargetPos = null;
                lastBlockCount = 0;
            }
            return;
        }

        // 同じブロックを見ている場合は再計算しない
        if (targetPos.equals(lastTargetPos)) {
            return;
        }

        lastTargetPos = targetPos;

        // 一括破壊対象のブロックを計算
        Set<BlockPos> connectedBlocks = findConnectedBlocks(client, targetPos, targetState);
        BlockHighlightRenderer.setHighlightedBlocks(connectedBlocks);

        // ブロック数を保存してHUDに表示（設定がオンの場合）
        lastBlockCount = connectedBlocks.size();
        if (Config.showBlocksPreview && lastBlockCount > 0) {
            VeinMiningHud.setPreviewCount(lastBlockCount);
        } else {
            VeinMiningHud.clearPreview();
        }
    }

    /**
     * 接続された鉱石または原木を探す（サーバー側のOreBreaker.dfsと同じロジック）
     */
    private static Set<BlockPos> findConnectedBlocks(MinecraftClient client, BlockPos startPos, BlockState targetState) {
        Set<BlockPos> visited = new HashSet<>();
        dfs(client, startPos, targetState, visited);
        return visited;
    }

    private static void dfs(MinecraftClient client, BlockPos pos, BlockState targetState, Set<BlockPos> visited) {
        // 上限チェック
        if (visited.size() >= Config.maxBlocks) {
            return;
        }

        // 既に訪問済み
        if (visited.contains(pos)) {
            return;
        }

        BlockState currentState = client.world.getBlockState(pos);

        // 同じ種類のブロックかチェック
        if (!currentState.isOf(targetState.getBlock())) {
            return;
        }

        // 訪問済みにマーク
        visited.add(pos);

        // 隣接ブロックを再帰的に処理
        if (Config.searchDiagonal) {
            // 26方向探索（上下左右前後 + 斜め）
            for (int dx = -1; dx <= 1; dx++) {
                for (int dy = -1; dy <= 1; dy++) {
                    for (int dz = -1; dz <= 1; dz++) {
                        if (dx == 0 && dy == 0 && dz == 0) continue;
                        BlockPos neighbor = pos.add(dx, dy, dz);
                        dfs(client, neighbor, targetState, visited);
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
                dfs(client, neighbor, targetState, visited);
            }
        }
    }
}