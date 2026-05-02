package net.misemise;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.misemise.ClothConfig.Config;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Locale;

public class OreUtils {
    private static final Logger LOGGER = LoggerFactory.getLogger("omniminer");
    private static final String MOD_ID = "omniminer";

    public static final TagKey<Block> OREMINER_ORES =
            TagKey.create(Registries.BLOCK, Identifier.fromNamespaceAndPath(MOD_ID, "ores"));
    private static final TagKey<Block> COMMON_ORES =
            TagKey.create(Registries.BLOCK, Identifier.fromNamespaceAndPath("c", "ores"));

    public static boolean isPickaxe(ItemStack stack) {
        return ToolUtils.isPickaxe(stack);
    }

    public static boolean isAxe(ItemStack stack) {
        return ToolUtils.isAxe(stack);
    }

    public static boolean isOre(BlockState state) {
        if (state == null || state.isAir()) {
            return false;
        }

        try {
            if (state.is(OREMINER_ORES) || state.is(COMMON_ORES)) {
                return true;
            }
        } catch (Throwable e) {
            LOGGER.warn("Failed to check ore tags for {}", state.getBlock(), e);
        }

        try {
            String blockId = BuiltInRegistries.BLOCK.getKey(state.getBlock()).toString();
            if (Config.customBlocks.contains(blockId)) {
                return true;
            }

            String path = BuiltInRegistries.BLOCK.getKey(state.getBlock()).getPath().toLowerCase(Locale.ROOT);
            return isOreLikePath(path);
        } catch (Throwable e) {
            LOGGER.warn("Failed to check ore by id", e);
            return false;
        }
    }

    private static boolean isOreLikePath(String path) {
        return path.equals("ancient_debris")
                || path.endsWith("_ore")
                || path.endsWith("_ores")
                || path.endsWith("_debris")
                || path.contains("_ore_")
                || path.contains("_ores_")
                || path.startsWith("ore_");
    }

    public static boolean isLog(BlockState state) {
        return LogUtils.isLog(state);
    }
}
