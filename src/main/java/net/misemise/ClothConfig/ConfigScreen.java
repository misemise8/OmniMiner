package net.misemise.ClothConfig;

import me.shedaniel.clothconfig2.api.ConfigBuilder;
import me.shedaniel.clothconfig2.api.ConfigCategory;
import me.shedaniel.clothconfig2.api.ConfigEntryBuilder;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.misemise.BedrockPlayerUtils;
import net.misemise.client.ClientMiningConfig;
import net.misemise.network.NetworkHandler;

public class ConfigScreen {
        private static String lastOpenedCategory = "config.omniminer.category.mining"; // デフォルトは採掘設定

        public static Screen createConfigScreen(Screen parent) {
                ConfigBuilder builder = ConfigBuilder.create()
                                .setParentScreen(parent)
                                .setTitle(Component.translatable("config.omniminer.title"))
                                .setSavingRunnable(() -> {
                                        Config.save();
                                        var integratedServer = Minecraft.getInstance().getSingleplayerServer();
                                        if (integratedServer != null) {
                                                integratedServer.execute(() ->
                                                                NetworkHandler.broadcastMiningConfig(integratedServer));
                                        }
                                });

                ConfigEntryBuilder entryBuilder = builder.entryBuilder();

                // ==================== 採掘設定カテゴリ ====================
                ConfigCategory mining = builder.getOrCreateCategory(
                                Component.translatable("config.omniminer.category.mining"));

                boolean remoteServer = ClientMiningConfig.isSynchronizedWithServer()
                                && !Minecraft.getInstance().hasSingleplayerServer();

                if (remoteServer) {
                        mining.addEntry(entryBuilder.startTextDescription(
                                        Component.translatable("config.omniminer.serverManaged"))
                                        .build());
                } else {
                // 最大ブロック数
                mining.addEntry(entryBuilder.startIntSlider(
                                Component.translatable("config.omniminer.maxBlocks"),
                                Config.maxBlocks,
                                1, 512)
                                .setDefaultValue(64)
                                .setTooltip(Component.translatable("config.omniminer.maxBlocks.tooltip"))
                                .setSaveConsumer(value -> Config.maxBlocks = value)
                                .build());

                // 斜め探索
                mining.addEntry(entryBuilder.startBooleanToggle(
                                Component.translatable("config.omniminer.searchDiagonal"),
                                Config.searchDiagonal)
                                .setDefaultValue(true)
                                .setTooltip(Component.translatable("config.omniminer.searchDiagonal.tooltip"))
                                .setSaveConsumer(value -> Config.searchDiagonal = value)
                                .build());

                // 自動回収
                mining.addEntry(entryBuilder.startBooleanToggle(
                                Component.translatable("config.omniminer.autoCollect"),
                                Config.autoCollect)
                                .setDefaultValue(true)
                                .setTooltip(Component.translatable("config.omniminer.autoCollect.tooltip"))
                                .setSaveConsumer(value -> Config.autoCollect = value)
                                .build());

                // 経験値自動回収
                mining.addEntry(entryBuilder.startBooleanToggle(
                                Component.translatable("config.omniminer.autoCollectExp"),
                                Config.autoCollectExp)
                                .setDefaultValue(true)
                                .setTooltip(Component.translatable("config.omniminer.autoCollectExp.tooltip"))
                                .setSaveConsumer(value -> Config.autoCollectExp = value)
                                .build());

                // 葉っぱも破壊
                mining.addEntry(entryBuilder.startBooleanToggle(
                                Component.translatable("config.omniminer.breakLeaves"),
                                Config.breakLeaves)
                                .setDefaultValue(false)
                                .setTooltip(Component.translatable("config.omniminer.breakLeaves.tooltip"))
                                .setSaveConsumer(value -> Config.breakLeaves = value)
                                .build());

                // ブロックエンティティを含める
                mining.addEntry(entryBuilder.startBooleanToggle(
                                Component.translatable("config.omniminer.includeBlockEntities"),
                                Config.includeBlockEntities)
                                .setDefaultValue(false)
                                .setTooltip(Component.translatable("config.omniminer.includeBlockEntities.tooltip"))
                                .setSaveConsumer(value -> Config.includeBlockEntities = value)
                                .build());
                }

                // トグルモード
                mining.addEntry(entryBuilder.startBooleanToggle(
                                Component.translatable("config.omniminer.toggleMode"),
                                Config.toggleMode)
                                .setDefaultValue(false)
                                .setTooltip(Component.translatable("config.omniminer.toggleMode.tooltip"))
                                .setSaveConsumer(value -> Config.toggleMode = value)
                                .build());

                // ==================== アウトライン設定カテゴリ ====================
                ConfigCategory outline = builder.getOrCreateCategory(
                                Component.translatable("config.omniminer.category.outline"));

                // アウトラインの色
                outline.addEntry(entryBuilder.startSelector(
                                Component.translatable("config.omniminer.outlineColor"),
                                new String[] { "Cyan", "Red", "Yellow", "Green", "Purple", "White" },
                                getColorName(Config.outlineColor))
                                .setDefaultValue("Cyan")
                                .setTooltip(Component.translatable("config.omniminer.outlineColor.tooltip"))
                                .setSaveConsumer(value -> {
                                        Config.outlineColor = getColorIndex(value);
                                })
                                .build());

                // アウトラインの太さ
                outline.addEntry(entryBuilder.startFloatField(
                                Component.translatable("config.omniminer.outlineThickness"),
                                Config.outlineThickness)
                                .setDefaultValue(2.0f)
                                .setMin(1.0f)
                                .setMax(5.0f)
                                .setTooltip(Component.translatable("config.omniminer.outlineThickness.tooltip"))
                                .setSaveConsumer(value -> Config.outlineThickness = value)
                                .build());

                // ==================== 統合版設定カテゴリ ====================
                // Floodgateが利用可能な場合のみ統合版設定を表示
                if (BedrockPlayerUtils.isFloodgateAvailable()) {
                        ConfigCategory bedrock = builder.getOrCreateCategory(
                                        Component.translatable("config.omniminer.category.bedrock"));

                        // スニークで有効化
                        bedrock.addEntry(entryBuilder.startBooleanToggle(
                                        Component.translatable("config.omniminer.bedrockSneakEnable"),
                                        Config.bedrockSneakEnable)
                                        .setDefaultValue(true)
                                        .setTooltip(Component.translatable("config.omniminer.bedrockSneakEnable.tooltip"))
                                        .setSaveConsumer(value -> Config.bedrockSneakEnable = value)
                                        .build());

                        // キーバインドも許可
                        bedrock.addEntry(entryBuilder.startBooleanToggle(
                                        Component.translatable("config.omniminer.bedrockAllowKeyBind"),
                                        Config.bedrockAllowKeyBind)
                                        .setDefaultValue(false)
                                        .setTooltip(Component.translatable("config.omniminer.bedrockAllowKeyBind.tooltip"))
                                        .setSaveConsumer(value -> Config.bedrockAllowKeyBind = value)
                                        .build());

                        // パーティクル表示
                        bedrock.addEntry(entryBuilder.startBooleanToggle(
                                        Component.translatable("config.omniminer.bedrockShowParticles"),
                                        Config.bedrockShowParticles)
                                        .setDefaultValue(true)
                                        .setTooltip(Component.translatable("config.omniminer.bedrockShowParticles.tooltip"))
                                        .setSaveConsumer(value -> Config.bedrockShowParticles = value)
                                        .build());

                        // パーティクルモード
                        bedrock.addEntry(entryBuilder.startSelector(
                                        Component.translatable("config.omniminer.bedrockParticleMode"),
                                        new String[] { "Corner", "Detailed" },
                                        getParticleModeName(Config.bedrockParticleMode))
                                        .setDefaultValue("Corner")
                                        .setTooltip(Component.translatable("config.omniminer.bedrockParticleMode.tooltip"))
                                        .setSaveConsumer(value -> {
                                                Config.bedrockParticleMode = getParticleModeIndex(value);
                                        })
                                        .build());
                }

                // ==================== その他設定カテゴリ ====================
                ConfigCategory other = builder.getOrCreateCategory(
                                Component.translatable("config.omniminer.category.other"));

                // 破壊後のブロック数表示
                other.addEntry(entryBuilder.startBooleanToggle(
                                Component.translatable("config.omniminer.showBlocksMinedCount"),
                                Config.showBlocksMinedCount)
                                .setDefaultValue(true)
                                .setTooltip(Component.translatable("config.omniminer.showBlocksMinedCount.tooltip"))
                                .setSaveConsumer(value -> Config.showBlocksMinedCount = value)
                                .build());

                // 破壊前のブロック数プレビュー
                other.addEntry(entryBuilder.startBooleanToggle(
                                Component.translatable("config.omniminer.showBlocksPreview"),
                                Config.showBlocksPreview)
                                .setDefaultValue(true)
                                .setTooltip(Component.translatable("config.omniminer.showBlocksPreview.tooltip"))
                                .setSaveConsumer(value -> Config.showBlocksPreview = value)
                                .build());

                // デバッグログ
                other.addEntry(entryBuilder.startBooleanToggle(
                                Component.translatable("config.omniminer.debugLog"),
                                Config.debugLog)
                                .setDefaultValue(false)
                                .setTooltip(Component.translatable("config.omniminer.debugLog.tooltip"))
                                .setSaveConsumer(value -> Config.debugLog = value)
                                .build());

                // 最後に開いたカテゴリを復元
                if (lastOpenedCategory.equals("config.omniminer.category.outline")) {
                        builder.setFallbackCategory(outline);
                } else if (lastOpenedCategory.equals("config.omniminer.category.other")) {
                        builder.setFallbackCategory(other);
                } else {
                        builder.setFallbackCategory(mining);
                }

                return builder.build();
        }

        private static String getColorName(int index) {
                String[] colors = { "Cyan", "Red", "Yellow", "Green", "Purple", "White" };
                if (index >= 0 && index < colors.length) {
                        return colors[index];
                }
                return "Cyan";
        }

        private static int getColorIndex(String name) {
                switch (name) {
                        case "Red":
                                return 1;
                        case "Yellow":
                                return 2;
                        case "Green":
                                return 3;
                        case "Purple":
                                return 4;
                        case "White":
                                return 5;
                        default:
                                return 0; // Cyan
                }
        }

        private static String getParticleModeName(int index) {
                return index == 1 ? "Detailed" : "Corner";
        }

        private static int getParticleModeIndex(String name) {
                return name.equals("Detailed") ? 1 : 0;
        }
}
