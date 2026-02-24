package net.misemise;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.entity.ExperienceOrbEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.misemise.ClothConfig.Config;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * ブロックを破壊してドロップを自動回収するユーティリティクラス
 */
public class AutoCollector {
    private static final Logger LOGGER = LoggerFactory.getLogger("omniminer");

    /**
     * ブロックを破壊し、ドロップアイテムと経験値を処理
     * 注：耐久値の消費はOreBreaker側で一括破壊の最初に1回だけ行う
     */
    public static void breakAndCollect(ServerWorld world, BlockPos pos, BlockState state,
                                       ServerPlayerEntity player, ItemStack tool) {
        if (world == null || state == null || player == null) {
            return;
        }

        try {
            if (Config.debugLog) {
                LOGGER.info("Breaking block at {} - autoCollect={}, autoCollectExp={}",
                        pos, Config.autoCollect, Config.autoCollectExp);
            }

            // ★★★ ツールの適正チェックを追加 ★★★
            // ツールが適正でない場合はドロップを出さない
            boolean canHarvest = state.isToolRequired() ?
                    (tool != null && tool.isSuitableFor(state)) : true;

            if (!canHarvest && Config.debugLog) {
                LOGGER.info("Tool not suitable for block {} - no drops", state.getBlock());
            }

            // ブロックを破壊（ドロップなし）
            world.breakBlock(pos, false, player);

            // ツールが適正な場合のみドロップを処理
            if (canHarvest) {
                // アイテムドロップの処理
                List<ItemStack> drops = Block.getDroppedStacks(state, world, pos,
                        world.getBlockEntity(pos), player, tool);

                if (Config.autoCollect) {
                    // 自動回収：インベントリに直接追加
                    for (ItemStack drop : drops) {
                        if (!drop.isEmpty()) {
                            boolean inserted = player.getInventory().insertStack(drop);
                            if (!inserted) {
                                Block.dropStack(world, pos, drop);
                            }
                        }
                    }
                    if (Config.debugLog) {
                        LOGGER.info("Auto-collected {} items", drops.size());
                    }
                } else {
                    // 通常ドロップ：地面に落とす
                    for (ItemStack drop : drops) {
                        if (!drop.isEmpty()) {
                            Block.dropStack(world, pos, drop);
                        }
                    }
                    if (Config.debugLog) {
                        LOGGER.info("Dropped {} items normally", drops.size());
                    }
                }

                // 経験値の処理（シルクタッチの場合は経験値を出さない）
                if (!hasSilkTouch(tool)) {
                    int expAmount = getExperienceFromOre(state);
                    if (expAmount > 0) {
                        if (Config.autoCollectExp) {
                            // 自動回収：プレイヤーに直接経験値を付与
                            player.addExperience(expAmount);

                            // 経験値取得音を再生
                            world.playSound(null, player.getBlockPos(),
                                    net.minecraft.sound.SoundEvents.ENTITY_EXPERIENCE_ORB_PICKUP,
                                    net.minecraft.sound.SoundCategory.PLAYERS,
                                    0.1f, // 音量（0.1 = 小さめ）
                                    (float)(0.5 + Math.random() * 0.5)); // ピッチ（0.5-1.0でランダム）

                            if (Config.debugLog) {
                                LOGGER.info("Auto-collected {} experience", expAmount);
                            }
                        } else {
                            // 通常ドロップ：経験値オーブを生成
                            ExperienceOrbEntity.spawn(world, player.getBlockPos().toCenterPos(), expAmount);
                            if (Config.debugLog) {
                                LOGGER.info("Dropped {} experience as orb", expAmount);
                            }
                        }
                    }
                } else if (Config.debugLog) {
                    LOGGER.info("Silk Touch detected - no experience dropped");
                }
            }

        } catch (Exception e) {
            LOGGER.error("Failed to break and collect block at {}", pos, e);
        }
    }

    /**
     * 鉱石ブロックから得られる経験値量を取得
     */
    private static int getExperienceFromOre(BlockState state) {
        String blockName = state.getBlock().toString().toLowerCase();

        // ダイヤモンド鉱石: 3-7
        if (blockName.contains("diamond_ore")) {
            return 3 + (int)(Math.random() * 5);
        }
        // エメラルド鉱石: 3-7
        if (blockName.contains("emerald_ore")) {
            return 3 + (int)(Math.random() * 5);
        }
        // ラピスラズリ鉱石: 2-5
        if (blockName.contains("lapis_ore")) {
            return 2 + (int)(Math.random() * 4);
        }
        // レッドストーン鉱石: 1-5
        if (blockName.contains("redstone_ore")) {
            return 1 + (int)(Math.random() * 5);
        }
        // 石炭鉱石: 0-2
        if (blockName.contains("coal_ore")) {
            return (int)(Math.random() * 3);
        }
        // ネザークォーツ鉱石: 2-5
        if (blockName.contains("quartz_ore")) {
            return 2 + (int)(Math.random() * 4);
        }
        // ネザー金鉱石: 0-1
        if (blockName.contains("nether_gold_ore")) {
            return (int)(Math.random() * 2);
        }

        return 0;
    }

    /**
     * ツールにシルクタッチエンチャントがついているか確認
     */
    private static boolean hasSilkTouch(ItemStack tool) {
        if (tool == null || tool.isEmpty()) {
            return false;
        }

        try {
            return tool.hasEnchantments() &&
                    tool.getEnchantments().getEnchantments().stream()
                            .anyMatch(entry -> {
                                String desc = entry.value().description().getString().toLowerCase();
                                String id = entry.getIdAsString();
                                return desc.contains("silk touch") || id.contains("silk_touch");
                            });
        } catch (Throwable e) {
            LOGGER.warn("Failed to check for Silk Touch enchantment", e);
            return false;
        }
    }
}