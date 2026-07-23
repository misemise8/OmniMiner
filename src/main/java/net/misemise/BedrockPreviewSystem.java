package net.misemise;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.misemise.ClothConfig.Config;
import net.misemise.network.NetworkHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class BedrockPreviewSystem {
    private static final Logger LOGGER = LoggerFactory.getLogger("omniminer");
    private static final int PREVIEW_INTERVAL_TICKS = 20;
    private static final Map<UUID, PreviewData> previewCache = new HashMap<>();
    private static int tickCounter = 0;

    public static void register() {
        ServerLifecycleEvents.SERVER_STOPPED.register(server -> {
            previewCache.clear();
            tickCounter = 0;
        });
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            tickCounter++;

            if (tickCounter % PREVIEW_INTERVAL_TICKS != 0) {
                return;
            }

            for (ServerLevel world : server.getAllLevels()) {
                for (ServerPlayer player : world.players()) {
                    updatePreview(player, world);
                }
            }
        });

        OmniMiner.LOGGER.info("BedrockPreviewSystem registered");
    }

    public static void clearPlayer(UUID playerId) {
        previewCache.remove(playerId);
    }

    private static void updatePreview(ServerPlayer player, ServerLevel world) {
        UUID playerId = player.getUUID();

        if (!BedrockPlayerUtils.isBedrockPlayer(player)) {
            previewCache.remove(playerId);
            return;
        }

        if (!Config.bedrockShowParticles) {
            previewCache.remove(playerId);
            return;
        }

        boolean previewActive = (Config.bedrockSneakEnable && player.isShiftKeyDown())
                || (Config.bedrockAllowKeyBind
                && NetworkHandler.isKeyPressed(playerId));
        if (!previewActive) {
            previewCache.remove(playerId);
            return;
        }

        HitResult hitResult = player.pick(5.0, 0.0f, false);
        if (hitResult.getType() != HitResult.Type.BLOCK) {
            previewCache.remove(playerId);
            return;
        }

        BlockHitResult blockHit = (BlockHitResult) hitResult;
        BlockPos targetPos = blockHit.getBlockPos();
        BlockState targetState = world.getBlockState(targetPos);
        ItemStack heldItem = player.getMainHandItem();

        if (!BlockTargetUtils.canVeinMine(world, targetPos, targetState, heldItem)) {
            previewCache.remove(playerId);
            return;
        }

        String targetKey = BlockTargetUtils.previewCacheKey(
                world, targetPos, targetState, heldItem);
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

    private static void showPreviewParticles(ServerLevel world, Set<BlockPos> blocks, ServerPlayer player) {
        if (Config.bedrockParticleMode == 1) {
            BedrockVisualHelper.showDetailedParticleOutline(world, blocks, player);
        } else {
            BedrockVisualHelper.showParticleOutline(world, blocks, player);
        }
    }

    private static Set<BlockPos> findConnectedBlocks(ServerLevel world, BlockPos startPos, BlockState targetState,
            ItemStack heldItem) {
        return BlockTargetUtils.findMiningTargets(
                world,
                startPos,
                targetState,
                heldItem,
                Config.maxBlocks,
                Config.searchDiagonal,
                Config.includeBlockEntities,
                Config.breakLeaves);
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
