package net.misemise.keybind;

import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.util.Identifier;
import net.misemise.OmniMiner;
import org.lwjgl.glfw.GLFW;

/**
 * キーバインド管理クラス
 */
public class KeyBindings {
    public static KeyBinding veinMinerKey;
    public static KeyBinding openConfigKey;

    public static final KeyBinding.Category OREMINER_CATEGORY = KeyBinding.Category.create(
            Identifier.of("oreminer", "general")
    );

    public static void register() {
        // 一括採掘キー (V) - ゲームプレイカテゴリー
        veinMinerKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.oreminer.veinminer",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_V,
                OREMINER_CATEGORY
        ));

        // 設定画面を開くキー (O) - その他カテゴリー
        openConfigKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.oreminer.openconfig",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_O,
                OREMINER_CATEGORY
        ));

        OmniMiner.LOGGER.info("KeyBindings registered");
    }

    public static boolean isVeinMinerKeyPressed() {
        return veinMinerKey != null && veinMinerKey.isPressed();
    }

    public static boolean wasOpenConfigPressed() {
        return openConfigKey != null && openConfigKey.wasPressed();
    }
}