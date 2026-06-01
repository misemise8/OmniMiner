package net.misemise;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.util.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LeafUtils {
    private static final Logger LOGGER = LoggerFactory.getLogger("omniminer");
    private static final String MOD_ID = "omniminer";

    // カスタムタグ (data/omniminer/tags/block/leaves.json)
    public static final TagKey<Block> OREMINER_LEAVES =
            TagKey.of(RegistryKeys.BLOCK, Identifier.of(MOD_ID, "leaves"));

    /**
     * ブロックが葉っぱかどうかを判定
     */
    public static boolean isLeaf(BlockState state) {
        if (state == null || state.isAir()) {
            return false;
        }

        try {
            // 1. カスタムタグで判定
            if (state.isIn(OREMINER_LEAVES)) {
                return true;
            }
        } catch (Throwable e) {
            LOGGER.warn("Failed to check leaf tag for {}", state.getBlock(), e);
        }

        try {
            // 2. Minecraftの標準タグで判定
            TagKey<Block> leavesTag = TagKey.of(RegistryKeys.BLOCK, Identifier.of("minecraft", "leaves"));
            if (state.isIn(leavesTag)) {
                return true;
            }
        } catch (Throwable e) {
            // タグが存在しない場合は無視
        }

        // 3. ブロックIDで判定
        try {
            String blockId = state.getBlock().toString().toLowerCase();
            if (blockId.contains("_leaves") || blockId.contains("leaves_")) {
                return true;
            }
        } catch (Throwable e) {
            LOGGER.warn("Failed to check leaf by name", e);
        }

        return false;
    }
}
