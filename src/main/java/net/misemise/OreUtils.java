package net.misemise;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.util.Identifier;
import net.misemise.ClothConfig.Config;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class OreUtils {
    private static final Logger LOGGER = LoggerFactory.getLogger("omniminer");
    private static final String MOD_ID = "omniminer";

    // カスタムタグ (data/omniminer/tags/block/ores.json)
    public static final TagKey<Block> OREMINER_ORES = TagKey.of(RegistryKeys.BLOCK, Identifier.of(MOD_ID, "ores"));

    /**
     * アイテムがつるはしかどうかを判定
     */
    public static boolean isPickaxe(ItemStack stack) {
        return ToolUtils.isPickaxe(stack);
    }

    /**
     * アイテムが斧かどうかを判定
     */
    public static boolean isAxe(ItemStack stack) {
        return ToolUtils.isAxe(stack);
    }

    /**
     * ブロックが鉱石かどうかを判定（他MOD対応強化版）
     */
    public static boolean isOre(BlockState state) {
        if (state == null || state.isAir()) {
            return false;
        }

        try {
            // 1. カスタムタグで判定
            if (state.isIn(OREMINER_ORES)) {
                return true;
            }
        } catch (Throwable e) {
            LOGGER.warn("Failed to check ore tag for {}", state.getBlock(), e);
        }

        try {
            // 2. Minecraftの共通タグで判定
            Block block = state.getBlock();

            // c:ores タグ（Fabric/Forge共通タグ）
            TagKey<Block> commonOresTag = TagKey.of(RegistryKeys.BLOCK, Identifier.of("c", "ores"));
            if (state.isIn(commonOresTag)) {
                return true;
            }

            // minecraft:*_ores タグ
            TagKey<Block> minecraftOresTag = TagKey.of(RegistryKeys.BLOCK, Identifier.of("minecraft", "iron_ores"));
            if (state.isIn(minecraftOresTag)) {
                return true;
            }
        } catch (Throwable e) {
            // タグが存在しない場合は無視
        }

        // 3. ブロックIDで判定（他MOD対応）
        try {
            String blockId = state.getBlock().toString().toLowerCase();

            // バニラとよくあるMODの鉱石パターン
            String[] orePatterns = {
                    "ore", // 基本的な鉱石
                    "_ore", // 末尾が_ore
                    "ore_", // ore_で始まる
                    "debris", // Ancient Debris
                    "raw_", // 粗鉱石ブロック
                    "nether_", // ネザー鉱石
                    "deepslate_", // 深層鉱石
                    "end_ore", // エンド鉱石（MOD）
                    "dense_ore", // 高密度鉱石（MOD）
                    "poor_ore", // 貧鉱石（MOD）
                    "rich_ore", // 富鉱石（MOD）
            };

            for (String pattern : orePatterns) {
                if (blockId.contains(pattern)) {
                    LOGGER.debug("Detected ore by pattern '{}': {}", pattern, blockId);
                    return true;
                }
            }

            // 4. MOD特有のパターン
            // Create MOD
            if (blockId.contains("zinc_ore") || blockId.contains("crushed_")) {
                return true;
            }

            // Mekanism
            if (blockId.contains("osmium") || blockId.contains("fluorite")) {
                return true;
            }

            // Thermal Series
            if (blockId.contains("tin_ore") || blockId.contains("lead_ore") ||
                    blockId.contains("silver_ore") || blockId.contains("nickel_ore")) {
                return true;
            }

            // Applied Energistics 2
            if (blockId.contains("certus") || blockId.contains("charged_certus")) {
                return true;
            }

            // Immersive Engineering
            if (blockId.contains("aluminum_ore") || blockId.contains("uranium_ore")) {
                return true;
            }

        } catch (Throwable e) {
            LOGGER.warn("Failed to check ore by name", e);
        }

        // カスタムブロックリストによる判定
        try {
            if (!Config.customBlocks.isEmpty()) {
                String blockId = net.minecraft.registry.Registries.BLOCK.getId(state.getBlock()).toString();
                if (Config.customBlocks.contains(blockId)) {
                    return true;
                }
            }
        } catch (Throwable e) {
            LOGGER.warn("Failed to check custom block list", e);
        }

        return false;
    }

    /**
     * ブロックが原木（ログ）かどうかを判定
     */
    public static boolean isLog(BlockState state) {
        return LogUtils.isLog(state);
    }
}
