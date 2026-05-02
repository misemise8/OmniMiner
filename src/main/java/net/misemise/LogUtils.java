package net.misemise;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LogUtils {
    private static final Logger LOGGER = LoggerFactory.getLogger("omniminer");
    private static final String MOD_ID = "omniminer";

    // カスタムタグ (data/omniminer/tags/blocks/logs.json)
    public static final TagKey<Block> OREMINER_LOGS =
            TagKey.create(Registries.BLOCK, Identifier.fromNamespaceAndPath(MOD_ID, "logs"));

    /**
     * ブロックが原木（ログ）かどうかを判定
     */
    public static boolean isLog(BlockState state) {
        if (state == null || state.isAir()) {
            return false;
        }

        try {
            // 1. カスタムタグで判定
            if (state.is(OREMINER_LOGS)) {
                return true;
            }
        } catch (Throwable e) {
            LOGGER.warn("Failed to check log tag for {}", state.getBlock(), e);
        }

        try {
            // 2. Minecraftの標準タグで判定
            TagKey<Block> logsTag = TagKey.create(Registries.BLOCK, Identifier.fromNamespaceAndPath("minecraft", "logs"));
            if (state.is(logsTag)) {
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