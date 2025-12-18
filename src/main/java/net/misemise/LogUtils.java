package net.misemise;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.util.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LogUtils {
    private static final Logger LOGGER = LoggerFactory.getLogger("oreminer");
    private static final String MOD_ID = "oreminer";

    // カスタムタグ (data/oreminer/tags/blocks/logs.json)
    public static final TagKey<Block> OREMINER_LOGS =
            TagKey.of(RegistryKeys.BLOCK, Identifier.of(MOD_ID, "logs"));

    /**
     * ブロックが原木（ログ）かどうかを判定
     */
    public static boolean isLog(BlockState state) {
        if (state == null || state.isAir()) {
            return false;
        }

        try {
            // 1. カスタムタグで判定
            if (state.isIn(OREMINER_LOGS)) {
                return true;
            }
        } catch (Throwable e) {
            LOGGER.warn("Failed to check log tag for {}", state.getBlock(), e);
        }

        try {
            // 2. Minecraftの標準タグで判定
            TagKey<Block> logsTag = TagKey.of(RegistryKeys.BLOCK, Identifier.of("minecraft", "logs"));
            if (state.isIn(logsTag)) {
                return true;
            }
        } catch (Throwable e) {
            // タグが存在しない場合は無視
        }

        // 3. ブロックIDで判定（原木のみ）
        try {
            String blockId = state.getBlock().toString().toLowerCase();

            // 原木パターン（_log, _stem のみ。_wood, _hyphae は除外）
            if ((blockId.contains("_log") || blockId.contains("_stem")) &&
                    !blockId.contains("_wood") && !blockId.contains("_hyphae")) {
                return true;
            }
        } catch (Throwable e) {
            LOGGER.warn("Failed to check log by name", e);
        }

        return false;
    }
}