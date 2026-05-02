package net.misemise;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.item.ItemStack;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Locale;

public class ToolUtils {
    private static final Logger LOGGER = LoggerFactory.getLogger("omniminer");

    public static boolean isPickaxe(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return false;
        }

        try {
            if (stack.is(ItemTags.PICKAXES)) {
                return true;
            }
        } catch (Throwable e) {
            LOGGER.warn("Failed to check pickaxe tag", e);
        }

        try {
            String itemPath = BuiltInRegistries.ITEM.getKey(stack.getItem()).getPath().toLowerCase(Locale.ROOT);
            String className = stack.getItem().getClass().getSimpleName().toLowerCase(Locale.ROOT);
            return itemPath.endsWith("_pickaxe") || className.contains("pickaxe");
        } catch (Throwable e) {
            LOGGER.warn("Failed to check pickaxe by name", e);
            return false;
        }
    }

    public static boolean isAxe(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return false;
        }

        try {
            if (stack.is(ItemTags.AXES)) {
                return true;
            }
        } catch (Throwable e) {
            LOGGER.warn("Failed to check axe tag", e);
        }

        try {
            String itemPath = BuiltInRegistries.ITEM.getKey(stack.getItem()).getPath().toLowerCase(Locale.ROOT);
            String className = stack.getItem().getClass().getSimpleName().toLowerCase(Locale.ROOT);
            return !itemPath.endsWith("_pickaxe")
                    && !className.contains("pickaxe")
                    && (itemPath.endsWith("_axe") || className.contains("axe"));
        } catch (Throwable e) {
            LOGGER.warn("Failed to check axe by name", e);
            return false;
        }
    }
}
