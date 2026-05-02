package net.misemise.keybind;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
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
import java.util.LinkedList;
import java.util.Queue;
import java.util.Set;

public class KeyStateTracker {
    private static boolean lastKeyState = false;
    private static BlockPos lastTargetPos = null;
    private static int lastBlockCount = 0;
    private static boolean toggledOn = false;
    private static boolean lastToggleMode = Config.toggleMode;

    public static void register() {
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (client.player == null || client.level == null) {
                return;
            }

            boolean currentKeyPressed = KeyBindings.isVeinMinerKeyPressed();

            if (lastToggleMode && !Config.toggleMode && toggledOn) {
                toggledOn = false;
                NetworkHandler.sendKeyState(false);
                clearHighlight();
            }
            lastToggleMode = Config.toggleMode;

            if (Config.toggleMode) {
                if (currentKeyPressed && !lastKeyState) {
                    toggledOn = !toggledOn;
                    OmniMiner.LOGGER.info("Toggle mode switched: {}", toggledOn);
                    NetworkHandler.sendKeyState(toggledOn);
                }
                lastKeyState = currentKeyPressed;

                if (toggledOn) {
                    updateHighlight(client);
                } else if (lastTargetPos != null) {
                    clearHighlight();
                }
            } else {
                if (currentKeyPressed != lastKeyState) {
                    OmniMiner.LOGGER.info("Key state changed: {} -> {}", lastKeyState, currentKeyPressed);
                    NetworkHandler.sendKeyState(currentKeyPressed);
                    lastKeyState = currentKeyPressed;
                }

                if (currentKeyPressed) {
                    updateHighlight(client);
                } else if (lastTargetPos != null) {
                    clearHighlight();
                }
            }

            if (KeyBindings.wasOpenConfigPressed()) {
                Minecraft.getInstance().setScreen(ConfigScreen.createConfigScreen(client.screen));
            }
        });

        OmniMiner.LOGGER.info("KeyStateTracker registered");
    }

    private static void updateHighlight(Minecraft client) {
        if (client.hitResult == null || client.hitResult.getType() != HitResult.Type.BLOCK) {
            if (lastTargetPos != null) {
                clearHighlight();
            }
            return;
        }

        BlockHitResult blockHit = (BlockHitResult) client.hitResult;
        BlockPos targetPos = blockHit.getBlockPos();
        BlockState targetState = client.level.getBlockState(targetPos);

        boolean isPickaxe = ToolUtils.isPickaxe(client.player.getMainHandItem());
        boolean isAxe = ToolUtils.isAxe(client.player.getMainHandItem());
        boolean isOre = isPickaxe && OreUtils.isOre(targetState);
        boolean isLog = isAxe && LogUtils.isLog(targetState);

        if (!isOre && !isLog) {
            if (lastTargetPos != null) {
                clearHighlight();
            }
            return;
        }

        if (targetPos.equals(lastTargetPos)) {
            return;
        }

        lastTargetPos = targetPos;

        Set<BlockPos> connectedBlocks = findConnectedBlocks(client, targetPos, targetState);
        BlockHighlightRenderer.setHighlightedBlocks(connectedBlocks);

        lastBlockCount = connectedBlocks.size();
        if (Config.showBlocksPreview && lastBlockCount > 0) {
            VeinMiningHud.setPreviewCount(lastBlockCount);
        } else {
            VeinMiningHud.clearPreview();
        }
    }

    private static Set<BlockPos> findConnectedBlocks(Minecraft client, BlockPos startPos, BlockState targetState) {
        Set<BlockPos> visited = new HashSet<>();
        bfs(client, startPos, targetState, visited);
        return visited;
    }

    private static void bfs(Minecraft client, BlockPos startPos, BlockState targetState, Set<BlockPos> visited) {
        Queue<BlockPos> queue = new LinkedList<>();
        queue.add(startPos);

        while (!queue.isEmpty()) {
            if (visited.size() >= Config.maxBlocks) {
                break;
            }

            BlockPos pos = queue.poll();
            if (visited.contains(pos)) {
                continue;
            }

            BlockState currentState = client.level.getBlockState(pos);
            if (!currentState.is(targetState.getBlock())) {
                continue;
            }

            visited.add(pos);

            if (Config.searchDiagonal) {
                for (int dx = -1; dx <= 1; dx++) {
                    for (int dy = -1; dy <= 1; dy++) {
                        for (int dz = -1; dz <= 1; dz++) {
                            if (dx == 0 && dy == 0 && dz == 0) {
                                continue;
                            }
                            addUnchecked(queue, visited, pos.offset(dx, dy, dz));
                        }
                    }
                }
            } else {
                addUnchecked(queue, visited, pos.above());
                addUnchecked(queue, visited, pos.below());
                addUnchecked(queue, visited, pos.north());
                addUnchecked(queue, visited, pos.south());
                addUnchecked(queue, visited, pos.east());
                addUnchecked(queue, visited, pos.west());
            }
        }
    }

    private static void addUnchecked(Queue<BlockPos> queue, Set<BlockPos> visited, BlockPos pos) {
        if (!visited.contains(pos)) {
            queue.add(pos);
        }
    }

    private static void clearHighlight() {
        BlockHighlightRenderer.clearHighlights();
        VeinMiningHud.clearPreview();
        lastTargetPos = null;
        lastBlockCount = 0;
    }
}
