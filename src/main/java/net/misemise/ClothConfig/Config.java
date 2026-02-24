package net.misemise.ClothConfig;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;
import net.misemise.OmniMiner;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * OreMiner設定クラス
 */
public class Config {
    // 一括破壊の最大ブロック数
    public static int maxBlocks = 64;

    // 斜め方向も探索するか（26方向 vs 6方向）
    public static boolean searchDiagonal = true;

    // 自動回収を有効にするか
    public static boolean autoCollect = true;

    // 経験値も自動回収するか
    public static boolean autoCollectExp = true;

    // デバッグログを出力するか
    public static boolean debugLog = false;

    // アウトラインの色 (0: シアン, 1: 赤, 2: 黄色, 3: 緑, 4: 紫, 5: 白)
    public static int outlineColor = 0;

    // 破壊後のブロック数表示
    public static boolean showBlocksMinedCount = true;

    // 破壊前のブロック数プレビュー
    public static boolean showBlocksPreview = true;

    // アウトラインの太さ (1.0 ~ 5.0)
    public static float outlineThickness = 2.0f;

    // トグルモード（true: 押すとオン/オフ切り替え, false: 押している間だけ有効）
    public static boolean toggleMode = false;

    // 葉っぱも一括破壊するか
    public static boolean breakLeaves = false;

    // ========== 統合版（Bedrock Edition）関連の設定 ==========

    // 統合版プレイヤーがスニークで一括破壊を有効化
    public static boolean bedrockSneakEnable = true;

    // 統合版プレイヤーも通常のキーバインドを使用可能にする（GeyserMCでキーバインドが使える場合）
    public static boolean bedrockAllowKeyBind = false;

    // 統合版プレイヤーにパーティクルで範囲を表示
    public static boolean bedrockShowParticles = true;

    // パーティクル表示モード (0: 角のみ, 1: 詳細な輪郭)
    public static int bedrockParticleMode = 0;

    // カスタムブロックリスト（ブロックIDを追加すると一括破壊対象になる）
    // 例: ["minecraft:stone", "mymod:special_ore"]
    public static List<String> customBlocks = new ArrayList<>();

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final File CONFIG_FILE = new File(
            FabricLoader.getInstance().getConfigDir().toFile(),
            "omniminer.json");

    /**
     * 設定を読み込む
     */
    public static void load() {
        if (CONFIG_FILE.exists()) {
            try (FileReader reader = new FileReader(CONFIG_FILE)) {
                ConfigData data = GSON.fromJson(reader, ConfigData.class);
                if (data != null) {
                    maxBlocks = data.maxBlocks;
                    searchDiagonal = data.searchDiagonal;
                    autoCollect = data.autoCollect;
                    autoCollectExp = data.autoCollectExp;
                    debugLog = data.debugLog;
                    outlineColor = data.outlineColor;
                    showBlocksMinedCount = data.showBlocksMinedCount;
                    showBlocksPreview = data.showBlocksPreview;
                    outlineThickness = data.outlineThickness;
                    toggleMode = data.toggleMode;
                    breakLeaves = data.breakLeaves;
                    bedrockSneakEnable = data.bedrockSneakEnable;
                    bedrockAllowKeyBind = data.bedrockAllowKeyBind;
                    bedrockShowParticles = data.bedrockShowParticles;
                    bedrockParticleMode = data.bedrockParticleMode;
                    if (data.customBlocks != null) {
                        customBlocks = new ArrayList<>(data.customBlocks);
                    }
                }
                OmniMiner.LOGGER.info("Config loaded from file");
            } catch (IOException e) {
                OmniMiner.LOGGER.error("Failed to load config", e);
            }
        } else {
            save(); // デフォルト設定で保存
        }
    }

    /**
     * 設定を保存する
     */
    public static void save() {
        try (FileWriter writer = new FileWriter(CONFIG_FILE)) {
            ConfigData data = new ConfigData();
            data.maxBlocks = maxBlocks;
            data.searchDiagonal = searchDiagonal;
            data.autoCollect = autoCollect;
            data.autoCollectExp = autoCollectExp;
            data.debugLog = debugLog;
            data.outlineColor = outlineColor;
            data.showBlocksMinedCount = showBlocksMinedCount;
            data.showBlocksPreview = showBlocksPreview;
            data.outlineThickness = outlineThickness;
            data.toggleMode = toggleMode;
            data.breakLeaves = breakLeaves;
            data.bedrockSneakEnable = bedrockSneakEnable;
            data.bedrockAllowKeyBind = bedrockAllowKeyBind;
            data.bedrockShowParticles = bedrockShowParticles;
            data.bedrockParticleMode = bedrockParticleMode;
            data.customBlocks = new ArrayList<>(customBlocks);

            GSON.toJson(data, writer);
            OmniMiner.LOGGER.info("Config saved to file");
        } catch (IOException e) {
            OmniMiner.LOGGER.error("Failed to save config", e);
        }
    }

    private static class ConfigData {
        int maxBlocks = 512;
        boolean searchDiagonal = true;
        boolean autoCollect = true;
        boolean autoCollectExp = true;
        boolean debugLog = false;
        int outlineColor = 0;
        boolean showBlocksMinedCount = true;
        boolean showBlocksPreview = true;
        float outlineThickness = 2.0f;
        boolean toggleMode = false;
        boolean breakLeaves = false;
        boolean bedrockSneakEnable = true;
        boolean bedrockAllowKeyBind = false;
        boolean bedrockShowParticles = true;
        int bedrockParticleMode = 0;
        List<String> customBlocks = new ArrayList<>();
    }
}