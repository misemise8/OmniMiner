package net.misemise;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.misemise.ClothConfig.Config;
import net.misemise.network.NetworkHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class OmniMiner implements ModInitializer {
    public static final String MOD_ID = "omniminer";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    @Override
    public void onInitialize() {
        LOGGER.info("OmniMiner initialized!");

        Config.load();
        net.misemise.command.OmniMinerCommand.register();
        AutoCollector.register();
        OreBreaker.register();

        if (BedrockPlayerUtils.isFloodgateAvailable()) {
            BedrockPreviewSystem.register();
            LOGGER.info("Floodgate detected - Bedrock Edition player support enabled!");
        } else {
            LOGGER.info("Floodgate not detected - Java Edition only mode");
        }

        try {
            NetworkHandler.registerServer();
        } catch (Throwable t) {
            LOGGER.warn("NetworkHandler.registerServer() failed or already registered: {}", t.toString());
        }

        registerBlockBreakEvents();
    }

    private static void registerBlockBreakEvents() {
        PlayerBlockBreakEvents.BEFORE.register((world, player, pos, state, entity) -> {
            if (!(world instanceof ServerLevel serverWorld)
                    || !(player instanceof ServerPlayer serverPlayer)) {
                return true;
            }

            // Additional blocks are sent through the normal game-mode break
            // path. Let this callback and every other Fabric listener approve
            // or cancel each one without starting another vein-mining session.
            if (OreBreaker.isProcessingAdditionalBreak(serverPlayer)
                    || OreBreaker.isBusy(serverPlayer)) {
                return true;
            }

            ItemStack held = serverPlayer.getMainHandItem();
            if (!BlockTargetUtils.canVeinMine(serverWorld, pos, state, held)) {
                return true;
            }

            boolean isBedrockPlayer = BedrockPlayerUtils.isBedrockPlayer(serverPlayer);
            boolean shouldActivate;
            if (isBedrockPlayer) {
                boolean isSneaking = serverPlayer.isShiftKeyDown();
                shouldActivate = (Config.bedrockSneakEnable && isSneaking)
                        || (Config.bedrockAllowKeyBind
                        && NetworkHandler.isKeyPressed(serverPlayer.getUUID()));
            } else {
                shouldActivate = NetworkHandler.isKeyPressed(serverPlayer.getUUID());
            }

            if (!shouldActivate) {
                return true;
            }

            if (Config.debugLog) {
                LOGGER.info("Preparing vein mining {} at {} for {} player {}",
                        BlockTargetUtils.blockType(state),
                        pos,
                        isBedrockPlayer ? "Bedrock" : "Java",
                        serverPlayer.getName().getString());
            }

            OreBreaker.prepare(serverWorld, pos, state, serverPlayer, held);

            // The starting block must be broken by Minecraft itself.
            return true;
        });

        PlayerBlockBreakEvents.AFTER.register((world, player, pos, state, entity) -> {
            if (world instanceof ServerLevel serverWorld
                    && player instanceof ServerPlayer serverPlayer
                    && !OreBreaker.isProcessingAdditionalBreak(serverPlayer)) {
                OreBreaker.startPreparedBreak(serverWorld, pos, state, serverPlayer);
            }
        });

        PlayerBlockBreakEvents.CANCELED.register((world, player, pos, state, entity) -> {
            if (player instanceof ServerPlayer serverPlayer
                    && !OreBreaker.isProcessingAdditionalBreak(serverPlayer)) {
                OreBreaker.cancelPreparedBreak(serverPlayer, pos);
            }
        });
    }
}
