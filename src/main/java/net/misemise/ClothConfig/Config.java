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
    public static boolean bedrockSneakEnable = true;
    public static boolean bedrockAllowKeyBind = false;
    public static boolean bedrockShowParticles = true;
    public static int bedrockParticleMode = 0;
    public static List<String> customBlocks = new ArrayList<>();

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final File CONFIG_FILE = new File(
            FabricLoader.getInstance().getConfigDir().toFile(),
            "omniminer.json");

    public static void load() {
        if (!CONFIG_FILE.exists()) {
            save();
            return;
        }

        try (FileReader reader = new FileReader(CONFIG_FILE)) {
            ConfigData data = GSON.fromJson(reader, ConfigData.class);
            if (data != null) {
                apply(data);
            }
            OmniMiner.LOGGER.info("Config loaded from file");
        } catch (IOException e) {
            OmniMiner.LOGGER.error("Failed to load config", e);
        }
    }

    public static void save() {
        normalize();

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
        boolean bedrockSneakEnable = true;
        boolean bedrockAllowKeyBind = false;
        boolean bedrockShowParticles = true;
        int bedrockParticleMode = 0;
        List<String> customBlocks = new ArrayList<>();
    }
}
