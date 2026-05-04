package net.misemise.client;

import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.text.Text;
import net.misemise.OmniMiner;

/**
 * 一括破壊したブロックの数をHUDに表示
 */
public class VeinMiningHud {
    private static int blocksMinedCount = 0;
    private static String blockType = "ore";
    private static long lastMineTime = 0;
    private static final long DISPLAY_DURATION = 3000; // 3秒間表示

    private static int previewCount = 0;
    private static boolean showPreview = false;

    public static void register() {
        // 破壊後のブロック数表示
        HudRenderCallback.EVENT.register((drawContext, tickDelta) -> {
            MinecraftClient client = MinecraftClient.getInstance();
            if (client.player == null) return;

            // プレビュー表示中は破壊後の表示を隠す
            if (showPreview) {
                return;
            }

            // 設定で破壊後の表示がオフなら何も表示しない
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

            if (blocksMinedCount <= 0) return;

            TextRenderer textRenderer = client.textRenderer;

            int screenWidth = client.getWindow().getScaledWidth();
            int screenHeight = client.getWindow().getScaledHeight();

            // ブロックタイプに応じてメッセージを変更
            String messageStr;
            if ("log".equals(blockType)) {
                messageStr = blocksMinedCount + " logs chopped!";
            } else if ("ore".equals(blockType)) {
                messageStr = blocksMinedCount + " ores mined!";
            } else {
                messageStr = blocksMinedCount + " blocks mined!";
            }

            Text message = Text.literal(messageStr);
            int textWidth = textRenderer.getWidth(messageStr);

            int x = (screenWidth - textWidth) / 2 + 40;
            int y = screenHeight / 2 + 30;

            // テキストを描画
            drawContext.drawTextWithShadow(textRenderer, message, x, y, 0xFFFFAA00);
        });

        // プレビュー表示用のHUDコールバック
        HudRenderCallback.EVENT.register((drawContext, tickDelta) -> {
            MinecraftClient client = MinecraftClient.getInstance();
            if (client.player == null || !showPreview) return;

            // 設定で破壊前プレビューがオフなら何も表示しない
            if (!net.misemise.ClothConfig.Config.showBlocksPreview) {
                return;
            }

            TextRenderer textRenderer = client.textRenderer;
            int screenWidth = client.getWindow().getScaledWidth();
            int screenHeight = client.getWindow().getScaledHeight();

            String previewStr = previewCount + " blocks";
            Text previewMessage = Text.literal(previewStr);
            int textWidth = textRenderer.getWidth(previewStr);

            int x = (screenWidth - textWidth) / 2 + 40;
            int y = screenHeight / 2 + 30;

            // プレビューテキストを描画（破壊後と同じオレンジ色）
            drawContext.drawTextWithShadow(textRenderer, previewMessage, x, y, 0xFFFFAA00);
        });
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
