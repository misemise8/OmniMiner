package net.misemise;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
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
        } catch (Throwable error) {
            LOGGER.warn(
                    "NetworkHandler.registerServer() failed or already registered: {}",
                    error.toString());
        }

        registerBlockBreakEvents();
    }

    private static void registerBlockBreakEvents() {
        PlayerBlockBreakEvents.BEFORE.register((world, player, pos, state, entity) -> {
            if (!(world instanceof ServerWorld serverWorld)
                    || !(player instanceof ServerPlayerEntity serverPlayer)) {
                return true;
            }

            if (OreBreaker.isProcessingAdditionalBreak(serverPlayer)
                    || OreBreaker.isBusy(serverPlayer)) {
                return true;
            }

            ItemStack held = serverPlayer.getMainHandStack();
            if (!BlockTargetUtils.canVeinMine(serverWorld, pos, state, held)) {
                return true;
            }

            boolean isBedrockPlayer = BedrockPlayerUtils.isBedrockPlayer(serverPlayer);
            boolean shouldActivate;
            if (isBedrockPlayer) {
                boolean isSneaking = serverPlayer.isSneaking();
                shouldActivate = (Config.bedrockSneakEnable && isSneaking)
                        || (Config.bedrockAllowKeyBind
                        && NetworkHandler.isKeyPressed(serverPlayer.getUuid()));
            } else {
                shouldActivate = NetworkHandler.isKeyPressed(serverPlayer.getUuid());
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
            return true;
        });

        PlayerBlockBreakEvents.AFTER.register((world, player, pos, state, entity) -> {
            if (world instanceof ServerWorld serverWorld
                    && player instanceof ServerPlayerEntity serverPlayer
                    && !OreBreaker.isProcessingAdditionalBreak(serverPlayer)) {
                OreBreaker.startPreparedBreak(serverWorld, pos, state, serverPlayer);
            }
        });

        PlayerBlockBreakEvents.CANCELED.register((world, player, pos, state, entity) -> {
            if (player instanceof ServerPlayerEntity serverPlayer
                    && !OreBreaker.isProcessingAdditionalBreak(serverPlayer)) {
                OreBreaker.cancelPreparedBreak(serverPlayer, pos);
            }
        });
    }
}
