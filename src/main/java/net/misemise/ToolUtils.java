package net.misemise;

import net.minecraft.item.ItemStack;
import net.minecraft.registry.tag.ItemTags;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ToolUtils {
    private static final Logger LOGGER = LoggerFactory.getLogger("omniminer");

    /**
     * アイテムがつるはしかどうかを判定
     */
    public static boolean isPickaxe(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return false;
        }

        // タグで判定
        try {
            if (stack.isIn(ItemTags.PICKAXES)) {
                return true;
            }
        } catch (Throwable e) {
            LOGGER.warn("Failed to check pickaxe tag", e);
        }

        // フォールバック：クラス名チェック
        try {
            String itemName = stack.getItem().toString().toLowerCase();
            String className = stack.getItem().getClass().getSimpleName().toLowerCase();
            return itemName.contains("pickaxe") || className.contains("pick");
        } catch (Throwable e) {
            LOGGER.warn("Failed to check pickaxe by name", e);
        }

        return false;
    }

    /**
     * アイテムが斧かどうかを判定
     */
    public static boolean isAxe(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return false;
        }

        // タグで判定
        try {
            if (stack.isIn(ItemTags.AXES)) {
                return true;
            }
        } catch (Throwable e) {
            LOGGER.warn("Failed to check axe tag", e);
        }

        // フォールバック：クラス名チェック
        try {
            String itemName = stack.getItem().toString().toLowerCase();
            String className = stack.getItem().getClass().getSimpleName().toLowerCase();
            return itemName.contains("axe") || className.contains("axe");
        } catch (Throwable e) {
            LOGGER.warn("Failed to check axe by name", e);
        }

        return false;
    }
}