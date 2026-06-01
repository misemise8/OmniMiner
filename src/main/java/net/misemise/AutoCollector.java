package net.misemise;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.misemise.ClothConfig.Config;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public class AutoCollector {
    private static final Logger LOGGER = LoggerFactory.getLogger("omniminer");

    public static void breakAndCollect(ServerLevel world, BlockPos pos, BlockState state,
            ServerPlayer player, ItemStack tool) {
        if (world == null || state == null || player == null) {
            return;
        }

        try {
            if (Config.debugLog) {
                LOGGER.info("Breaking block at {} - autoCollect={}, autoCollectExp={}",
                        pos, Config.autoCollect, Config.autoCollectExp);
            }

            boolean canHarvest = BlockTargetUtils.canHarvestDrops(state, tool);

            if (!canHarvest && Config.debugLog) {
                LOGGER.info("Tool not suitable for block {} - no drops", state.getBlock());
            }

            BlockEntity blockEntity = world.getBlockEntity(pos);
            world.removeBlock(pos, false);

            if (canHarvest) {
                List<ItemStack> drops = Block.getDrops(state, world, pos,
                        blockEntity, player, tool);

                if (Config.autoCollect) {
                    for (ItemStack drop : drops) {
                        if (!drop.isEmpty()) {
                            boolean inserted = player.getInventory().add(drop);
                            if (!inserted) {
                                Block.popResource(world, pos, drop);
                            }
                        }
                    }
                    if (Config.debugLog) {
                        LOGGER.info("Auto-collected {} items", drops.size());
                    }
                } else {
                    for (ItemStack drop : drops) {
                        if (!drop.isEmpty()) {
                            Block.popResource(world, pos, drop);
                        }
                    }
                    if (Config.debugLog) {
                        LOGGER.info("Dropped {} items normally", drops.size());
                    }
                }

                dropExperience(world, pos, state, player, tool);
            }
        } catch (Exception e) {
            LOGGER.error("Failed to break and collect block at {}", pos, e);
        }
    }

    private static void dropExperience(ServerLevel world, BlockPos pos, BlockState state,
            ServerPlayer player, ItemStack tool) {
        BlockPos expPos = Config.autoCollectExp ? player.blockPosition() : pos;
        state.spawnAfterBreak(world, expPos, tool, true);

        if (Config.debugLog) {
            LOGGER.info("Dropped vanilla experience for {} at {}", state.getBlock(), expPos);
        }
    }
}
