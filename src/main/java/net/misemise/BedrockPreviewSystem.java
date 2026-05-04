package net.misemise;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.block.BlockState;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.misemise.ClothConfig.Config;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class BedrockPreviewSystem {
    private static final Logger LOGGER = LoggerFactory.getLogger("omniminer");
    private static final Map<UUID, PreviewData> previewCache = new HashMap<>();
    private static int tickCounter = 0;

    public static void register() {
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            tickCounter++;
            if (tickCounter % 5 != 0) {
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

    private static void updatePreview(ServerPlayerEntity player, ServerWorld world) {
        UUID playerId = player.getUuid();

        if (!BedrockPlayerUtils.isBedrockPlayer(player)) {
            previewCache.remove(playerId);
            return;
        }

        if (!Config.bedrockShowParticles || !Config.bedrockSneakEnable) {
            previewCache.remove(playerId);
            return;
        }

        if (!player.isSneaking()) {
            previewCache.remove(playerId);
            return;
        }

        HitResult hitResult = player.raycast(5.0, 0.0f, false);
        if (hitResult.getType() != HitResult.Type.BLOCK) {
            previewCache.remove(playerId);
            return;
        }

        BlockHitResult blockHit = (BlockHitResult) hitResult;
        BlockPos targetPos = blockHit.getBlockPos();
        BlockState targetState = world.getBlockState(targetPos);
        ItemStack heldItem = player.getMainHandStack();

        if (!BlockTargetUtils.canVeinMine(world, targetPos, targetState, heldItem)) {
            previewCache.remove(playerId);
            return;
        }

        String targetKey = BlockTargetUtils.previewCacheKey(targetPos, targetState, heldItem);
        PreviewData cached = previewCache.get(playerId);
        if (cached != null && cached.cacheKey.equals(targetKey)) {
            showPreviewParticles(world, cached.blocks, player);
            return;
        }

        Set<BlockPos> connectedBlocks = findConnectedBlocks(world, targetPos, targetState, heldItem);
        previewCache.put(playerId, new PreviewData(targetKey, connectedBlocks));
        showPreviewParticles(world, connectedBlocks, player);

        if (Config.debugLog) {
            LOGGER.info("Bedrock preview for {}: {} blocks at {}",
                    player.getName().getString(), connectedBlocks.size(), targetPos);
        }
    }

    private static void showPreviewParticles(ServerWorld world, Set<BlockPos> blocks, ServerPlayerEntity player) {
        if (Config.bedrockParticleMode == 1) {
            BedrockVisualHelper.showDetailedParticleOutline(world, blocks, player);
        } else {
            BedrockVisualHelper.showParticleOutline(world, blocks, player);
        }
    }

    private static Set<BlockPos> findConnectedBlocks(ServerWorld world, BlockPos startPos, BlockState targetState,
            ItemStack heldItem) {
        return BlockTargetUtils.findSphericalTargets(
                world, startPos, targetState, heldItem, Config.maxBlocks, Config.searchDiagonal);
    }

    private static class PreviewData {
        final String cacheKey;
        final Set<BlockPos> blocks;

        PreviewData(String cacheKey, Set<BlockPos> blocks) {
            this.cacheKey = cacheKey;
            this.blocks = blocks;
        }
    }
}
