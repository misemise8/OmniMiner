package net.misemise.keybind;

import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.minecraft.client.KeyMapping;
import net.minecraft.resources.Identifier;
import net.misemise.OmniMiner;
import org.lwjgl.glfw.GLFW;

/**
 * キーバインド管理クラス
 */
public class KeyBindings {
    public static KeyMapping veinMinerKey;
    public static KeyMapping openConfigKey;

    public static final KeyMapping.Category OREMINER_CATEGORY = KeyMapping.Category.register(
            Identifier.fromNamespaceAndPath("omniminer", "general")
    );

    public static void register() {
        // 一括採掘キー (V) - ゲームプレイカテゴリー
        veinMinerKey = KeyMappingHelper.registerKeyMapping(new KeyMapping(
                "key.omniminer.veinminer",
                InputConstants.Type.KEYSYM,
                GLFW.GLFW_KEY_V,
                OREMINER_CATEGORY
        ));

        // 設定画面を開くキー (O) - その他カテゴリー
        openConfigKey = KeyMappingHelper.registerKeyMapping(new KeyMapping(
                "key.omniminer.openconfig",
                InputConstants.Type.KEYSYM,
                GLFW.GLFW_KEY_O,
                OREMINER_CATEGORY
        ));

        OmniMiner.LOGGER.info("KeyBindings registered");
    }

    public static boolean isVeinMinerKeyPressed() {
        return veinMinerKey != null && veinMinerKey.isDown();
    }

    public static boolean wasOpenConfigPressed() {
        return openConfigKey != null && openConfigKey.consumeClick();
    }
}
