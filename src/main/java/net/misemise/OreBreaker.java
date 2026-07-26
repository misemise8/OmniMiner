package net.misemise;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.AttackBlockCallback;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.item.ItemStack;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.misemise.AutoCollector.DropCapture;
import net.misemise.ClothConfig.Config;
import net.misemise.network.NetworkHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.UUID;

/**
 * Coordinates vein-mining sessions without replacing Minecraft's normal block
 * breaking path.
 */
public final class OreBreaker {
    private static final Logger LOGGER = LoggerFactory.getLogger("omniminer");
    private static final int MAX_ADDITIONAL_BREAKS_PER_TICK = 32;
    private static final int LEAF_DISTANCE_UPDATE_TICKS = 7;
    private static final Map<UUID, PreparedBreak> PREPARED_BREAKS = new HashMap<>();
    private static final Queue<MiningSession> SESSIONS = new ArrayDeque<>();
    private static final Set<UUID> BUSY_PLAYERS = new HashSet<>();
    private static final Set<UUID> PROCESSING_PLAYERS = new HashSet<>();
    private static boolean registered;

    private OreBreaker() {
    }

    public static void register() {
        if (registered) {
            return;
        }

        ServerTickEvents.END_SERVER_TICK.register(OreBreaker::tick);
        ServerLifecycleEvents.SERVER_STOPPED.register(server -> clearAllState());
        registered = true;
    }

    public static boolean isProcessingAdditionalBreak(ServerPlayerEntity player) {
        return PROCESSING_PLAYERS.contains(player.getUuid());
    }

    public static boolean isBusy(ServerPlayerEntity player) {
        return BUSY_PLAYERS.contains(player.getUuid());
    }

    public static void prepare(
            ServerWorld world,
            BlockPos startPos,
            BlockState originalState,
            ServerPlayerEntity player,
            ItemStack heldItem) {
        UUID playerId = player.getUuid();
        if (BUSY_PLAYERS.contains(playerId) || Config.maxBlocks <= 0) {
            return;
        }

        PreparedBreak prepared = new PreparedBreak(
                world,
                startPos.toImmutable(),
                originalState.getBlock(),
                LogUtils.isLog(originalState),
                player,
                heldItem,
                Config.maxBlocks,
                Config.searchDiagonal,
                Config.breakLeaves,
                Config.autoCollect,
                Config.autoCollectExp,
                Config.includeBlockEntities,
                BlockTargetUtils.blockType(originalState),
                world.getTime());

        PREPARED_BREAKS.put(playerId, prepared);
        BUSY_PLAYERS.add(playerId);
    }

    public static void startPreparedBreak(
            ServerWorld world,
            BlockPos pos,
            BlockState originalState,
            ServerPlayerEntity player) {
        UUID playerId = player.getUuid();
        PreparedBreak prepared = PREPARED_BREAKS.remove(playerId);
        if (prepared == null) {
            return;
        }

        if (prepared.world() != world
                || !prepared.startPos().equals(pos)
                || prepared.originalBlock() != originalState.getBlock()) {
            BUSY_PLAYERS.remove(playerId);
            return;
        }

        AutoCollector.captureOriginalBreak(
                prepared.world(),
                prepared.startPos(),
                prepared.player(),
                prepared.collectItems(),
                prepared.collectExperience());
        SESSIONS.add(new MiningSession(prepared));
    }

    public static void cancelPreparedBreak(ServerPlayerEntity player, BlockPos pos) {
        UUID playerId = player.getUuid();
        PreparedBreak prepared = PREPARED_BREAKS.get(playerId);
        if (prepared != null && prepared.startPos().equals(pos)) {
            PREPARED_BREAKS.remove(playerId);
            BUSY_PLAYERS.remove(playerId);
        }
    }

    public static void clearPlayerState(UUID playerId) {
        PREPARED_BREAKS.remove(playerId);
        BUSY_PLAYERS.remove(playerId);
        PROCESSING_PLAYERS.remove(playerId);
        SESSIONS.removeIf(session -> session.playerId().equals(playerId));
    }

    private static void tick(MinecraftServer server) {
        expireStalePreparedBreaks();

        int remainingBudget = MAX_ADDITIONAL_BREAKS_PER_TICK;
        int skippedInRow = 0;
        while (remainingBudget > 0
                && !SESSIONS.isEmpty()
                && skippedInRow < SESSIONS.size()) {
            MiningSession session = SESSIONS.remove();
            try {
                if (session.isComplete()) {
                    finish(session);
                    continue;
                }

                if (!session.isReadyForNextBreak()) {
                    if (session.isComplete()) {
                        finish(session);
                    } else {
                        SESSIONS.add(session);
                        skippedInRow++;
                    }
                    continue;
                }

                session.breakNext();
                remainingBudget--;
                skippedInRow = 0;

                if (session.isComplete()) {
                    finish(session);
                } else {
                    SESSIONS.add(session);
                }
            } catch (Throwable error) {
                session.abort();
                LOGGER.error(
                        "Vein mining aborted for player {} after an unexpected block callback failure",
                        session.playerId(),
                        error);
                finish(session);
            } finally {
                PROCESSING_PLAYERS.remove(session.playerId());
            }
        }
    }

    private static void expireStalePreparedBreaks() {
        Iterator<Map.Entry<UUID, PreparedBreak>> iterator = PREPARED_BREAKS.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<UUID, PreparedBreak> entry = iterator.next();
            PreparedBreak prepared = entry.getValue();
            if (prepared.world().getTime() > prepared.createdAtGameTime() + 1L) {
                iterator.remove();
                BUSY_PLAYERS.remove(entry.getKey());
            }
        }
    }

    private static void finish(MiningSession session) {
        BUSY_PLAYERS.remove(session.playerId());
        PROCESSING_PLAYERS.remove(session.playerId());

        try {
            NetworkHandler.sendBlocksMinedCount(
                    session.player(),
                    session.displayBlocksBroken(),
                    session.blockType());
        } catch (Throwable error) {
            LOGGER.warn("Failed to send OmniMiner completion message", error);
        }

        if (Config.debugLog) {
            LOGGER.info("Vein mining completed for {}: {} primary blocks, {} total blocks",
                    session.player().getName().getString(),
                    session.displayBlocksBroken(),
                    session.totalBlocksBroken());
        }
    }

    private static void clearAllState() {
        PREPARED_BREAKS.clear();
        SESSIONS.clear();
        BUSY_PLAYERS.clear();
        PROCESSING_PLAYERS.clear();
    }

    private record PreparedBreak(
            ServerWorld world,
            BlockPos startPos,
            Block originalBlock,
            boolean startedFromLog,
            ServerPlayerEntity player,
            ItemStack tool,
            int maxBlocks,
            boolean searchDiagonal,
            boolean breakLeaves,
            boolean collectItems,
            boolean collectExperience,
            boolean includeBlockEntities,
            String blockType,
            long createdAtGameTime) {
    }

    private static final class MiningSession {
        private final PreparedBreak prepared;
        private final Queue<BlockPos> primaryTargets = new ArrayDeque<>();
        private final Set<BlockPos> primaryVisited = new HashSet<>();
        private final Queue<BlockPos> leafTargets = new ArrayDeque<>();
        private final Set<BlockPos> leafVisited = new HashSet<>();
        private final Set<BlockPos> removedLogPositions = new HashSet<>();
        private int totalBlocksBroken = 1;
        private int primaryBlocksBroken = 1;
        private boolean stopped;
        private boolean leafPhaseInitialized;
        private long leavesReadyAtGameTime = -1L;

        private MiningSession(PreparedBreak prepared) {
            this.prepared = prepared;
            primaryVisited.add(prepared.startPos());
            enqueuePrimaryNeighbors(prepared.startPos());
            if (prepared.startedFromLog()) {
                removedLogPositions.add(prepared.startPos());
            }
        }

        UUID playerId() {
            return prepared.player().getUuid();
        }

        ServerPlayerEntity player() {
            return prepared.player();
        }

        String blockType() {
            return prepared.blockType();
        }

        int totalBlocksBroken() {
            return totalBlocksBroken;
        }

        int displayBlocksBroken() {
            return primaryBlocksBroken;
        }

        void abort() {
            stopped = true;
        }

        boolean isComplete() {
            if (stopped || totalBlocksBroken >= prepared.maxBlocks()) {
                return true;
            }
            if (!primaryTargets.isEmpty()) {
                return false;
            }
            if (!shouldProcessLeaves()) {
                return true;
            }
            return leafPhaseInitialized && leafTargets.isEmpty();
        }

        boolean isReadyForNextBreak() {
            if (!primaryTargets.isEmpty()) {
                return true;
            }
            if (!shouldProcessLeaves()) {
                return false;
            }

            if (leavesReadyAtGameTime < 0L) {
                leavesReadyAtGameTime =
                        prepared.world().getTime() + LEAF_DISTANCE_UPDATE_TICKS;
                return false;
            }
            if (prepared.world().getTime() < leavesReadyAtGameTime) {
                return false;
            }
            if (!leafPhaseInitialized) {
                initializeLeafTargets();
            }
            return !leafTargets.isEmpty();
        }

        void breakNext() {
            if (!isPlayerStillValid()
                    || prepared.player().getMainHandStack() != prepared.tool()) {
                stopped = true;
                return;
            }

            ItemStack currentTool = prepared.player().getMainHandStack();
            if (currentTool.isEmpty()) {
                stopped = true;
                return;
            }

            boolean leafTarget = primaryTargets.isEmpty() && leafPhaseInitialized;
            BlockPos pos = leafTarget ? leafTargets.remove() : primaryTargets.remove();
            if (!prepared.world().isInBuildLimit(pos)
                    || !prepared.world().isPosLoaded(pos)
                    || !prepared.player().canModifyAt(prepared.world(), pos)) {
                return;
            }

            BlockState currentState = prepared.world().getBlockState(pos);
            if (!isStillAPlannedTarget(pos, currentState, currentTool, leafTarget)) {
                return;
            }

            ActionResult attackResult = AttackBlockCallback.EVENT.invoker().interact(
                    prepared.player(),
                    prepared.world(),
                    Hand.MAIN_HAND,
                    pos,
                    Direction.UP);
            if (attackResult != ActionResult.PASS) {
                return;
            }
            currentState.onBlockBreakStart(
                    prepared.world(),
                    pos,
                    prepared.player());

            boolean broken;
            DropCapture capture = null;
            PROCESSING_PLAYERS.add(playerId());
            try {
                capture = AutoCollector.beginAdditionalBreak(
                        prepared.world(),
                        pos,
                        prepared.player(),
                        prepared.collectItems(),
                        prepared.collectExperience());
                broken = prepared.player().interactionManager.tryBreakBlock(pos);
            } finally {
                AutoCollector.finishAdditionalBreak(capture);
                PROCESSING_PLAYERS.remove(playerId());
            }

            BlockState stateAfterBreak = prepared.world().getBlockState(pos);
            if (!broken || stateAfterBreak.isOf(currentState.getBlock())) {
                return;
            }

            totalBlocksBroken++;
            if (leafTarget) {
                enqueueLeafNeighbors(pos);
            } else {
                primaryBlocksBroken++;
                if (prepared.startedFromLog()) {
                    removedLogPositions.add(pos);
                }
                enqueuePrimaryNeighbors(pos);
            }
        }

        private boolean isStillAPlannedTarget(
                BlockPos pos,
                BlockState state,
                ItemStack tool,
                boolean leafTarget) {
            if (!leafTarget && state.isOf(prepared.originalBlock())) {
                return BlockTargetUtils.canVeinMine(
                        prepared.world(),
                        pos,
                        state,
                        tool,
                        prepared.includeBlockEntities());
            }

            return leafTarget
                    && prepared.startedFromLog()
                    && BlockTargetUtils.isDecayingNaturalLeaf(state);
        }

        private boolean shouldProcessLeaves() {
            return prepared.breakLeaves()
                    && prepared.startedFromLog()
                    && totalBlocksBroken < prepared.maxBlocks();
        }

        private void initializeLeafTargets() {
            leafPhaseInitialized = true;
            leafVisited.addAll(removedLogPositions);
            for (BlockPos logPos : removedLogPositions) {
                enqueueLeafNeighbors(logPos);
            }
        }

        private void enqueuePrimaryNeighbors(BlockPos center) {
            if (totalBlocksBroken >= prepared.maxBlocks()) {
                return;
            }

            for (BlockPos neighbor
                    : BlockTargetUtils.neighbors(center, prepared.searchDiagonal())) {
                if (primaryVisited.add(neighbor)) {
                    primaryTargets.add(neighbor);
                }
            }
        }

        private void enqueueLeafNeighbors(BlockPos center) {
            if (totalBlocksBroken >= prepared.maxBlocks()) {
                return;
            }

            for (BlockPos neighbor : BlockTargetUtils.neighbors(center, true)) {
                if (!leafVisited.add(neighbor)
                        || !prepared.world().isInBuildLimit(neighbor)
                        || !prepared.world().isPosLoaded(neighbor)) {
                    continue;
                }
                if (BlockTargetUtils.isDecayingNaturalLeaf(
                        prepared.world().getBlockState(neighbor))) {
                    leafTargets.add(neighbor);
                }
            }
        }

        private boolean isPlayerStillValid() {
            return !prepared.player().isRemoved()
                    && prepared.player().getEntityWorld() == prepared.world();
        }
    }
}
