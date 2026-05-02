package net.misemise;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LeafUtils {
    private static final Logger LOGGER = LoggerFactory.getLogger("omniminer");
    private static final String MOD_ID = "omniminer";

    // カスタムタグ (data/omniminer/tags/blocks/leaves.json)
    public static final TagKey<Block> OREMINER_LEAVES =
            TagKey.create(Registries.BLOCK, Identifier.fromNamespaceAndPath(MOD_ID, "leaves"));

    /**
     * ブロックが葉っぱかどうかを判定
     */
    public static boolean isLeaf(BlockState state) {
        if (state == null || state.isAir()) {
            return false;
        }

        try {
            // 1. カスタムタグで判定
            if (state.is(OREMINER_LEAVES)) {
                return true;
            }
        } catch (Throwable e) {
            LOGGER.warn("Failed to check leaf tag for {}", state.getBlock(), e);
        }

        try {
            // 2. Minecraftの標準タグで判定
            TagKey<Block> leavesTag = TagKey.create(Registries.BLOCK, Identifier.fromNamespaceAndPath("minecraft", "leaves"));
            if (state.is(leavesTag)) {
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