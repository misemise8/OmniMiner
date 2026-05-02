package net.misemise.keybind;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.misemise.BlockTargetUtils;
import net.misemise.ClothConfig.Config;
import net.misemise.ClothConfig.ConfigScreen;
import net.misemise.OmniMiner;
import net.misemise.client.BlockHighlightRenderer;
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
                } else if (lastTargetKey != null) {
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
                } else if (lastTargetKey != null) {
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
            if (lastTargetKey != null) {
                clearHighlight();
            }
            return;
        }

        BlockHitResult blockHit = (BlockHitResult) client.hitResult;
        BlockPos targetPos = blockHit.getBlockPos();
        BlockState targetState = client.level.getBlockState(targetPos);
        ItemStack heldItem = client.player.getMainHandItem();

        if (!BlockTargetUtils.canVeinMine(client.level, targetPos, targetState, heldItem)) {
            if (lastTargetKey != null) {
                clearHighlight();
            }
            return;
        }

        String targetKey = BlockTargetUtils.previewCacheKey(targetPos, targetState, heldItem);
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

    private static Set<BlockPos> findConnectedBlocks(Minecraft client, BlockPos startPos, BlockState targetState,
            ItemStack heldItem) {
        return BlockTargetUtils.findSphericalTargets(
                client.level, startPos, targetState, heldItem, Config.maxBlocks, Config.searchDiagonal);
    }

    private static void clearHighlight() {
        BlockHighlightRenderer.clearHighlights();
        VeinMiningHud.clearPreview();
        lastTargetKey = null;
        lastBlockCount = 0;
    }
}
