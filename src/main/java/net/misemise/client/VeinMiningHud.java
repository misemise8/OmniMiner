package net.misemise.client;

import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.misemise.OmniMiner;

public class VeinMiningHud {
    private static int blocksMinedCount = 0;
    private static String blockType = "ore";
    private static long lastMineTime = 0;
    private static final long DISPLAY_DURATION = 3000;

    private static int previewCount = 0;
    private static boolean showPreview = false;

    public static void register() {
        HudElementRegistry.attachElementBefore(
                VanillaHudElements.CHAT,
                Identifier.fromNamespaceAndPath(OmniMiner.MOD_ID, "vein_mining_hud"),
                VeinMiningHud::render
        );
    }

    private static void render(GuiGraphicsExtractor graphics, DeltaTracker deltaTracker) {
        Minecraft client = Minecraft.getInstance();
        if (client.player == null) {
            return;
        }

        if (showPreview) {
            if (net.misemise.ClothConfig.Config.showBlocksPreview) {
                drawMessage(graphics, client.font, previewCount + " blocks");
            }
            return;
        }

        if (!net.misemise.ClothConfig.Config.showBlocksMinedCount) {
            return;
        }

        long currentTime = System.currentTimeMillis();
        if (currentTime - lastMineTime > DISPLAY_DURATION) {
            if (blocksMinedCount > 0) {
                blocksMinedCount = 0;
            }
            return;
        }

        if (blocksMinedCount <= 0) {
            return;
        }

        String message = "log".equals(blockType)
                ? blocksMinedCount + " logs chopped!"
                : blocksMinedCount + " ores mined!";
        drawMessage(graphics, client.font, message);
    }

    private static void drawMessage(GuiGraphicsExtractor graphics, Font textRenderer, String message) {
        Component component = Component.literal(message);
        int textWidth = textRenderer.width(message);
        int x = (graphics.guiWidth() - textWidth) / 2 + 40;
        int y = graphics.guiHeight() / 2 + 30;

        graphics.text(textRenderer, component, x, y, 0xFFFFAA00);
    }

    public static void setBlocksMinedCount(int count, String type) {
        blocksMinedCount = count;
        blockType = type;
        lastMineTime = System.currentTimeMillis();
    }

    public static void setPreviewCount(int count) {
        previewCount = count;
        showPreview = true;
    }

    public static void clearPreview() {
        showPreview = false;
        previewCount = 0;
    }
}
