package net.misemise;

import net.minecraft.server.network.ServerPlayerEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 統合版（Bedrock Edition）プレイヤーの検出と処理を管理
 */
public class BedrockPlayerUtils {
    private static final Logger LOGGER = LoggerFactory.getLogger("omniminer");
    private static boolean floodgateAvailable = false;
    private static Class<?> floodgateApiClass = null;

    static {
        // Floodgate APIが利用可能かチェック
        try {
            floodgateApiClass = Class.forName("org.geysermc.floodgate.api.FloodgateApi");
            floodgateAvailable = true;
            LOGGER.info("Floodgate API detected - Bedrock Edition support enabled");
        } catch (ClassNotFoundException e) {
            floodgateAvailable = false;
            LOGGER.info("Floodgate API not found - Bedrock Edition support disabled");
        }
    }

    /**
     * プレイヤーが統合版（Bedrock Edition）かどうかを判定
     */
    public static boolean isBedrockPlayer(ServerPlayerEntity player) {
        if (!floodgateAvailable) {
            return false;
        }

        try {
            // FloodgateApi.getInstance().isFloodgatePlayer(player.getUuid())
            Object apiInstance = floodgateApiClass.getMethod("getInstance").invoke(null);
            Boolean result = (Boolean) apiInstance.getClass()
                    .getMethod("isFloodgatePlayer", java.util.UUID.class)
                    .invoke(apiInstance, player.getUuid());

            return result != null && result;
        } catch (Exception e) {
            LOGGER.warn("Failed to check if player is Bedrock Edition: {}", e.getMessage());
            return false;
        }
    }

    /**
     * 統合版プレイヤーのXbox gamertag/プレフィックスを取得
     */
    public static String getBedrockPrefix(ServerPlayerEntity player) {
        if (!floodgateAvailable || !isBedrockPlayer(player)) {
            return null;
        }

        try {
            Object apiInstance = floodgateApiClass.getMethod("getInstance").invoke(null);
            Object floodgatePlayer = apiInstance.getClass()
                    .getMethod("getPlayer", java.util.UUID.class)
                    .invoke(apiInstance, player.getUuid());

            if (floodgatePlayer != null) {
                String username = (String) floodgatePlayer.getClass()
                        .getMethod("getUsername")
                        .invoke(floodgatePlayer);
                return username;
            }
        } catch (Exception e) {
            LOGGER.warn("Failed to get Bedrock player info: {}", e.getMessage());
        }

        return null;
    }

    /**
     * Floodgate APIが利用可能かどうか
     */
    public static boolean isFloodgateAvailable() {
        return floodgateAvailable;
    }

    /**
     * プレイヤーのプラットフォーム情報を取得（デバッグ用）
     */
    public static String getPlayerPlatform(ServerPlayerEntity player) {
        if (isBedrockPlayer(player)) {
            String prefix = getBedrockPrefix(player);
            return "Bedrock Edition" + (prefix != null ? " (" + prefix + ")" : "");
        }
        return "Java Edition";
    }
}