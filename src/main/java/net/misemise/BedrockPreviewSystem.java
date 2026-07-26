package net.misemise;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.block.BlockState;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.misemise.ClothConfig.Config;
import net.misemise.network.NetworkHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public final class BedrockPreviewSystem {
    private static final Logger LOGGER = LoggerFactory.getLogger("omniminer");
    private static final int PREVIEW_INTERVAL_TICKS = 20;
    private static final Map<UUID, PreviewData> PREVIEW_CACHE = new HashMap<>();
    private static int tickCounter;

    private BedrockPreviewSystem() {
    }

    public static void register() {
        ServerLifecycleEvents.SERVER_STOPPED.register(server -> {
            PREVIEW_CACHE.clear();
            tickCounter = 0;
        });
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            tickCounter++;
            if (tickCounter % PREVIEW_INTERVAL_TICKS != 0) {
                return;
            }

            for (ServerWorld world : server.getWorlds()) {
                for (ServerPlayerEntity player : world.getPlayers()) {
                    updatePreview(player, world);
                }
            }
        });
        OmniMiner.LOGGER.info("BedrockPreviewSystem registered");
    }

    public static void clearPlayer(UUID playerId) {
        PREVIEW_CACHE.remove(playerId);
    }

    private static void updatePreview(
            ServerPlayerEntity player,
            ServerWorld world) {
        UUID playerId = player.getUuid();
        if (!BedrockPlayerUtils.isBedrockPlayer(player)
                || !Config.bedrockShowParticles) {
            PREVIEW_CACHE.remove(playerId);
            return;
        }

        boolean previewActive = (Config.bedrockSneakEnable && player.isSneaking())
                || (Config.bedrockAllowKeyBind
                && NetworkHandler.isKeyPressed(playerId));
        if (!previewActive) {
            PREVIEW_CACHE.remove(playerId);
            return;
        }

        HitResult hitResult = player.raycast(5.0D, 0.0F, false);
        if (hitResult.getType() != HitResult.Type.BLOCK) {
            PREVIEW_CACHE.remove(playerId);
            return;
        }

        BlockPos targetPos = ((BlockHitResult) hitResult).getBlockPos();
        BlockState targetState = world.getBlockState(targetPos);
        ItemStack heldItem = player.getMainHandStack();
        if (!BlockTargetUtils.canVeinMine(
                world,
                targetPos,
                targetState,
                heldItem)) {
            PREVIEW_CACHE.remove(playerId);
            return;
        }

        String targetKey = BlockTargetUtils.previewCacheKey(
                world,
                targetPos,
                targetState,
                heldItem);
        PreviewData cached = PREVIEW_CACHE.get(playerId);
        if (cached != null && cached.cacheKey().equals(targetKey)) {
            showPreviewParticles(world, cached.blocks(), player);
            return;
        }

        Set<BlockPos> connectedBlocks = BlockTargetUtils.findMiningTargets(
                world,
                targetPos,
                targetState,
                heldItem,
                Config.maxBlocks,
                Config.searchDiagonal,
                Config.includeBlockEntities,
                Config.breakLeaves);
        PREVIEW_CACHE.put(
                playerId,
                new PreviewData(targetKey, connectedBlocks));
        showPreviewParticles(world, connectedBlocks, player);

        if (Config.debugLog) {
            LOGGER.info("Bedrock preview for {}: {} blocks at {}",
                    player.getName().getString(),
                    connectedBlocks.size(),
                    targetPos);
        }
    }

    private static void showPreviewParticles(
            ServerWorld world,
            Set<BlockPos> blocks,
            ServerPlayerEntity player) {
        if (Config.bedrockParticleMode == 1) {
            BedrockVisualHelper.showDetailedParticleOutline(world, blocks, player);
        } else {
            BedrockVisualHelper.showParticleOutline(world, blocks, player);
        }
    }

    private record PreviewData(String cacheKey, Set<BlockPos> blocks) {
    }
}
