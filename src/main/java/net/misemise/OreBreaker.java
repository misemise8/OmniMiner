package net.misemise;

import net.minecraft.block.BlockState;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.state.property.Properties;
import net.minecraft.util.math.BlockPos;
import net.misemise.ClothConfig.Config;
import net.misemise.network.NetworkHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.Queue;
import java.util.Set;

public class OreBreaker {
    private static final Logger LOGGER = LoggerFactory.getLogger("omniminer");

    public static void breakConnectedOres(ServerWorld world, BlockPos startPos,
            BlockState originalState, ServerPlayerEntity player, ItemStack heldItem) {
        if (heldItem != null && !heldItem.isEmpty() && heldItem.isDamageable()) {
            heldItem.damage(1, player, net.minecraft.entity.EquipmentSlot.MAINHAND);

            if (Config.debugLog) {
                LOGGER.info("Consumed 1 durability for vein mining: {}/{}",
                        heldItem.getMaxDamage() - heldItem.getDamage(), heldItem.getMaxDamage());
            }

            if (heldItem.isEmpty() || heldItem.getCount() == 0) {
                if (Config.debugLog) {
                    LOGGER.info("Tool broke at the start of vein mining");
                }
                return;
            }
        }

        Set<BlockPos> targets = BlockTargetUtils.findSphericalTargets(
                world, startPos, originalState, heldItem, Config.maxBlocks, Config.searchDiagonal);
        Set<BlockPos> logPositions = new HashSet<>();
        boolean isLog = LogUtils.isLog(originalState);

        for (BlockPos pos : targets) {
            BlockState currentState = world.getBlockState(pos);
            if (isLog && LogUtils.isLog(currentState)) {
                logPositions.add(pos);
            }
            AutoCollector.breakAndCollect(world, pos, currentState, player, heldItem);
        }

        if (isLog && Config.breakLeaves && !logPositions.isEmpty()) {
            breakNearbyLeaves(world, logPositions, player, heldItem);
        }

        NetworkHandler.sendBlocksMinedCount(player, targets.size(), BlockTargetUtils.blockType(originalState));
    }

    private static class LeafNode {
        BlockPos pos;
        int distance;

        LeafNode(BlockPos pos, int distance) {
            this.pos = pos;
            this.distance = distance;
        }
    }

    private static void breakNearbyLeaves(ServerWorld world, Set<BlockPos> logPositions,
            ServerPlayerEntity player, ItemStack heldItem) {
        Set<BlockPos> visited = new HashSet<>();
        Queue<LeafNode> queue = new LinkedList<>();
        int maxLeafDistance = 10;

        for (BlockPos logPos : logPositions) {
            for (int dx = -1; dx <= 1; dx++) {
                for (int dy = -1; dy <= 1; dy++) {
                    for (int dz = -1; dz <= 1; dz++) {
                        if (dx == 0 && dy == 0 && dz == 0) {
                            continue;
                        }

                        BlockPos neighbor = logPos.add(dx, dy, dz);
                        if (visited.contains(neighbor)) {
                            continue;
                        }

                        BlockState state = world.getBlockState(neighbor);
                        if (LeafUtils.isLeaf(state) && !state.get(Properties.PERSISTENT)) {
                            visited.add(neighbor);
                            queue.add(new LeafNode(neighbor, 1));
                        }
                    }
                }
            }
        }

        while (!queue.isEmpty()) {
            LeafNode current = queue.poll();
            BlockPos currentPos = current.pos;
            int currentDist = current.distance;

            if (currentDist > maxLeafDistance) {
                continue;
            }

            BlockState currentState = world.getBlockState(currentPos);
            if (!LeafUtils.isLeaf(currentState)) {
                continue;
            }

            AutoCollector.breakAndCollect(world, currentPos, currentState, player, heldItem);

            for (int dx = -1; dx <= 1; dx++) {
                for (int dy = -1; dy <= 1; dy++) {
                    for (int dz = -1; dz <= 1; dz++) {
                        if (dx == 0 && dy == 0 && dz == 0) {
                            continue;
                        }

                        BlockPos nextPos = currentPos.add(dx, dy, dz);
                        if (visited.contains(nextPos)) {
                            continue;
                        }

                        BlockState nextState = world.getBlockState(nextPos);
                        if (LeafUtils.isLeaf(nextState)
                                && !nextState.get(Properties.PERSISTENT)
                                && nextState.get(Properties.DISTANCE_1_7) < 7) {
                            visited.add(nextPos);
                            queue.add(new LeafNode(nextPos, currentDist + 1));
                        }
                    }
                }
            }
        }
    }
}
