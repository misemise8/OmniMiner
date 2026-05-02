package net.misemise;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.misemise.ClothConfig.Config;
import net.misemise.network.NetworkHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Set;

public class OmniMiner implements ModInitializer {
    public static final String MOD_ID = "omniminer";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    @Override
    public void onInitialize() {
        LOGGER.info("OmniMiner initialized!");

        Config.load();
        net.misemise.command.OmniMinerCommand.register();

        if (BedrockPlayerUtils.isFloodgateAvailable()) {
            BedrockPreviewSystem.register();
        }

        if (BedrockPlayerUtils.isFloodgateAvailable()) {
            LOGGER.info("Floodgate detected - Bedrock Edition player support enabled!");
        } else {
            LOGGER.info("Floodgate not detected - Java Edition only mode");
        }

        try {
            NetworkHandler.registerServer();
        } catch (Throwable t) {
            LOGGER.warn("NetworkHandler.registerServer() failed or already registered: {}", t.toString());
        }

        PlayerBlockBreakEvents.BEFORE.register((world, player, pos, state, entity) -> {
            if (world.isClientSide()) {
                return true;
            }
            if (!(world instanceof ServerLevel serverWorld)) {
                return true;
            }
            if (!(player instanceof ServerPlayer serverPlayer)) {
                return true;
            }

            ItemStack held = serverPlayer.getMainHandItem();
            if (!BlockTargetUtils.canVeinMine(serverWorld, pos, state, held)) {
                return true;
            }

            boolean isBedrockPlayer = BedrockPlayerUtils.isBedrockPlayer(serverPlayer);
            boolean shouldActivate = false;

            if (isBedrockPlayer) {
                if (Config.debugLog) {
                    LOGGER.info("Bedrock player detected: {}",
                            BedrockPlayerUtils.getPlayerPlatform(serverPlayer));
                }

                boolean isSneaking = serverPlayer.isShiftKeyDown();

                if (Config.bedrockSneakEnable && isSneaking) {
                    shouldActivate = true;
                    if (Config.debugLog) {
                        LOGGER.info("Bedrock player sneaking - vein mining activated");
                    }
                } else if (Config.bedrockAllowKeyBind
                        && NetworkHandler.isKeyPressed(serverPlayer.getUUID())) {
                    shouldActivate = true;
                    if (Config.debugLog) {
                        LOGGER.info("Bedrock player using keybind - vein mining activated");
                    }
                }

                if (shouldActivate && Config.bedrockShowParticles) {
                    try {
                        Set<BlockPos> connectedBlocks = findConnectedBlocks(serverWorld, pos, state, held);

                        if (Config.bedrockParticleMode == 1) {
                            BedrockVisualHelper.showDetailedParticleOutline(serverWorld, connectedBlocks,
                                    serverPlayer);
                        } else {
                            BedrockVisualHelper.showParticleOutline(serverWorld, connectedBlocks, serverPlayer);
                        }
                    } catch (Exception e) {
                        LOGGER.warn("Failed to show particle preview for Bedrock player", e);
                    }
                }
            } else {
                shouldActivate = NetworkHandler.isKeyPressed(serverPlayer.getUUID());
            }

            if (!shouldActivate) {
                return true;
            }

            String blockType = BlockTargetUtils.blockType(state);
            String playerType = isBedrockPlayer ? "Bedrock" : "Java";
            LOGGER.info("Vein mining {} triggered at {} by {} player {}",
                    blockType, pos, playerType, serverPlayer.getName().getString());

            OreBreaker.breakConnectedOres(serverWorld, pos, state, serverPlayer, held);
            return false;
        });
    }

    private static Set<BlockPos> findConnectedBlocks(
            ServerLevel world, BlockPos startPos, BlockState targetState, ItemStack heldItem) {
        return BlockTargetUtils.findSphericalTargets(
                world, startPos, targetState, heldItem, Config.maxBlocks, Config.searchDiagonal);
    }
}
