package net.misemise.keybind;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.block.BlockState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.item.ItemStack;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.misemise.BlockTargetUtils;
import net.misemise.ClothConfig.Config;
import net.misemise.ClothConfig.ConfigScreen;
import net.misemise.OmniMiner;
import net.misemise.client.BlockHighlightRenderer;
import net.misemise.client.ClientMiningConfig;
import net.misemise.client.VeinMiningHud;
import net.misemise.network.NetworkHandler;

import java.util.Set;

public class KeyStateTracker {
    private static boolean lastKeyState = false;
    private static String lastTargetKey = null;
    private static int lastBlockCount = 0;
    private static boolean toggledOn = false;
    private static boolean lastToggleMode = Config.toggleMode;

    public static void register() {
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (client.player == null || client.world == null) {
                return;
            }

            if (KeyBindings.wasOpenConfigPressed()) {
                MinecraftClient.getInstance().setScreen(
                        ConfigScreen.createConfigScreen(client.currentScreen));
            }

            if (!NetworkHandler.canSendKeyState()) {
                resetConnectionState();
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
                    if (Config.debugLog) {
                        OmniMiner.LOGGER.info("Toggle mode switched: {}", toggledOn);
                    }
                    NetworkHandler.sendKeyState(toggledOn);
                }
                lastKeyState = currentKeyPressed;

                if (toggledOn) {
                    updateHighlight(client);
                } else if (lastTargetKey != null) {
                    clearHighlight();
                }
            } else {
                if (currentKeyPressed != lastKeyState) {
                    if (Config.debugLog) {
                        OmniMiner.LOGGER.info(
                                "Key state changed: {} -> {}",
                                lastKeyState,
                                currentKeyPressed);
                    }
                    NetworkHandler.sendKeyState(currentKeyPressed);
                    lastKeyState = currentKeyPressed;
                }

                if (currentKeyPressed) {
                    updateHighlight(client);
                } else if (lastTargetKey != null) {
                    clearHighlight();
                }
            }
        });

        OmniMiner.LOGGER.info("KeyStateTracker registered");
    }

    public static void resetConnectionState() {
        lastKeyState = false;
        toggledOn = false;
        lastToggleMode = Config.toggleMode;
        clearHighlight();
    }

    private static void updateHighlight(MinecraftClient client) {
        if (client.crosshairTarget == null || client.crosshairTarget.getType() != HitResult.Type.BLOCK) {
            if (lastTargetKey != null) {
                clearHighlight();
            }
            return;
        }

        BlockHitResult blockHit = (BlockHitResult) client.crosshairTarget;
        BlockPos targetPos = blockHit.getBlockPos();
        BlockState targetState = client.world.getBlockState(targetPos);
        ItemStack heldItem = client.player.getMainHandStack();

        if (!BlockTargetUtils.canVeinMine(
                client.world,
                targetPos,
                targetState,
                heldItem,
                ClientMiningConfig.includeBlockEntities())) {
            if (lastTargetKey != null) {
                clearHighlight();
            }
            return;
        }

        String targetKey = BlockTargetUtils.previewCacheKey(
                client.world,
                targetPos,
                targetState,
                heldItem,
                ClientMiningConfig.maxBlocks(),
                ClientMiningConfig.searchDiagonal(),
                ClientMiningConfig.includeBlockEntities(),
                ClientMiningConfig.breakLeaves());
        if (targetKey.equals(lastTargetKey)) {
            return;
        }

        lastTargetKey = targetKey;

        Set<BlockPos> connectedBlocks = findConnectedBlocks(client, targetPos, targetState, heldItem);
        BlockHighlightRenderer.setHighlightedBlocks(connectedBlocks);

        lastBlockCount = connectedBlocks.size();
        if (Config.showBlocksPreview && lastBlockCount > 0) {
            VeinMiningHud.setPreviewCount(lastBlockCount);
        } else {
            VeinMiningHud.clearPreview();
        }
    }

    private static Set<BlockPos> findConnectedBlocks(MinecraftClient client, BlockPos startPos, BlockState targetState,
            ItemStack heldItem) {
        return BlockTargetUtils.findMiningTargets(
                client.world,
                startPos,
                targetState,
                heldItem,
                ClientMiningConfig.maxBlocks(),
                ClientMiningConfig.searchDiagonal(),
                ClientMiningConfig.includeBlockEntities(),
                ClientMiningConfig.breakLeaves());
    }

    private static void clearHighlight() {
        BlockHighlightRenderer.clearHighlights();
        VeinMiningHud.clearPreview();
        lastTargetKey = null;
        lastBlockCount = 0;
    }
}
