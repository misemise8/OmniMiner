package net.misemise.ClothConfig;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonIOException;
import com.google.gson.JsonSyntaxException;
import net.fabricmc.loader.api.FabricLoader;
import net.misemise.OmniMiner;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;

public class Config {
    public static int maxBlocks = 64;
    public static boolean searchDiagonal = true;
    public static boolean autoCollect = true;
    public static boolean autoCollectExp = true;
    public static boolean debugLog = false;
    public static int outlineColor = 0;
    public static boolean showBlocksMinedCount = true;
    public static boolean showBlocksPreview = true;
    public static float outlineThickness = 2.0f;
    public static boolean toggleMode = false;
    public static boolean breakLeaves = false;
    public static boolean includeBlockEntities = false;
    public static boolean bedrockSneakEnable = true;
    public static boolean bedrockAllowKeyBind = false;
    public static boolean bedrockShowParticles = true;
    public static int bedrockParticleMode = 0;
    public static List<String> customBlocks = new ArrayList<>();

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final File CONFIG_FILE = new File(
            FabricLoader.getInstance().getConfigDir().toFile(),
            "omniminer.json");

    public static synchronized void load() {
        if (!CONFIG_FILE.exists()) {
            save();
            return;
        }

        try (var reader = Files.newBufferedReader(
                CONFIG_FILE.toPath(),
                StandardCharsets.UTF_8)) {
            ConfigData data = GSON.fromJson(reader, ConfigData.class);
            if (data == null) {
                throw new JsonSyntaxException("Config file contains null");
            }
            apply(data);
            OmniMiner.LOGGER.info("Config loaded from file");
        } catch (JsonSyntaxException e) {
            OmniMiner.LOGGER.error("Config is invalid and will be replaced with defaults", e);
            if (backupInvalidConfig()) {
                apply(new ConfigData());
                save();
            }
        } catch (JsonIOException e) {
            OmniMiner.LOGGER.error(
                    "Failed to read config; the existing file was not moved or overwritten",
                    e);
        } catch (IOException e) {
            OmniMiner.LOGGER.error(
                    "Failed to read config; the existing file was not moved or overwritten",
                    e);
        }
    }

    public static synchronized void save() {
        normalize();

        Path configPath = CONFIG_FILE.toPath();
        Path temporaryPath = null;

        try {
            Files.createDirectories(configPath.getParent());
            temporaryPath = Files.createTempFile(
                    configPath.getParent(),
                    "omniminer-",
                    ".json.tmp");

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
            data.includeBlockEntities = includeBlockEntities;
            data.bedrockSneakEnable = bedrockSneakEnable;
            data.bedrockAllowKeyBind = bedrockAllowKeyBind;
            data.bedrockShowParticles = bedrockShowParticles;
            data.bedrockParticleMode = bedrockParticleMode;
            data.customBlocks = new ArrayList<>(customBlocks);

            try (BufferedWriter writer = Files.newBufferedWriter(
                    temporaryPath,
                    StandardCharsets.UTF_8)) {
                GSON.toJson(data, writer);
            }

            try {
                Files.move(
                        temporaryPath,
                        configPath,
                        StandardCopyOption.ATOMIC_MOVE,
                        StandardCopyOption.REPLACE_EXISTING);
            } catch (AtomicMoveNotSupportedException e) {
                Files.move(temporaryPath, configPath, StandardCopyOption.REPLACE_EXISTING);
            }

            OmniMiner.LOGGER.info("Config saved to file");
        } catch (IOException e) {
            OmniMiner.LOGGER.error("Failed to save config", e);
        } finally {
            if (temporaryPath != null) {
                try {
                    Files.deleteIfExists(temporaryPath);
                } catch (IOException e) {
                    OmniMiner.LOGGER.warn(
                            "Failed to delete temporary config file {}",
                            temporaryPath,
                            e);
                }
            }
        }
    }

    private static boolean backupInvalidConfig() {
        Path configPath = CONFIG_FILE.toPath();
        Path backupPath = configPath.resolveSibling(
                "omniminer.invalid-" + System.currentTimeMillis() + ".json");

        try {
            Files.move(configPath, backupPath);
            OmniMiner.LOGGER.warn("Invalid config moved to {}", backupPath);
            return true;
        } catch (IOException e) {
            OmniMiner.LOGGER.error(
                    "Failed to preserve invalid config; the original file was not overwritten",
                    e);
            return false;
        }
    }

    private static void apply(ConfigData data) {
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
        includeBlockEntities = data.includeBlockEntities;
        bedrockSneakEnable = data.bedrockSneakEnable;
        bedrockAllowKeyBind = data.bedrockAllowKeyBind;
        bedrockShowParticles = data.bedrockShowParticles;
        bedrockParticleMode = data.bedrockParticleMode;
        customBlocks = data.customBlocks == null ? new ArrayList<>() : new ArrayList<>(data.customBlocks);

        normalize();
    }

    private static void normalize() {
        maxBlocks = clamp(maxBlocks, 1, 512);
        outlineColor = clamp(outlineColor, 0, 5);
        outlineThickness = clamp(outlineThickness, 1.0f, 5.0f);
        bedrockParticleMode = clamp(bedrockParticleMode, 0, 1);
        customBlocks = customBlocks == null ? new ArrayList<>() : new ArrayList<>(customBlocks);
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    private static float clamp(float value, float min, float max) {
        return Math.max(min, Math.min(max, value));
    }

    private static class ConfigData {
        int maxBlocks = 64;
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
        boolean includeBlockEntities = false;
        boolean bedrockSneakEnable = true;
        boolean bedrockAllowKeyBind = false;
        boolean bedrockShowParticles = true;
        int bedrockParticleMode = 0;
        List<String> customBlocks = new ArrayList<>();
    }
}
