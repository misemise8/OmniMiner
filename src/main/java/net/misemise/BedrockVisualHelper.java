package net.misemise;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;

/**
 * 統合版プレイヤー向けの視覚的フィードバック
 * パーティクルを使って一括破壊範囲を表示
 */
public class BedrockVisualHelper {
    private static final Logger LOGGER = LoggerFactory.getLogger("omniminer");

    /**
     * 一括破壊対象のブロックをパーティクルで表示（統合版プレイヤー向け）
     */
    public static void showParticleOutline(ServerLevel world, Set<BlockPos> blocks, ServerPlayer player) {
        if (blocks == null || blocks.isEmpty()) {
            return;
        }

        // パーティクルの色を設定に応じて変更
        int colorIndex = net.misemise.ClothConfig.Config.outlineColor;

        for (BlockPos pos : blocks) {
            // ブロックの8つの角にパーティクルを配置
            spawnCornerParticles(world, pos, player, colorIndex);
        }
    }

    /**
     * ブロックの角にパーティクルを配置
     */
    private static void spawnCornerParticles(ServerLevel world, BlockPos pos, ServerPlayer player, int colorIndex) {
        double x = pos.getX();
        double y = pos.getY();
        double z = pos.getZ();

        // 8つの角の座標
        Vec3[] corners = {
                new Vec3(x, y, z),
                new Vec3(x + 1, y, z),
                new Vec3(x, y + 1, z),
                new Vec3(x + 1, y + 1, z),
                new Vec3(x, y, z + 1),
                new Vec3(x + 1, y, z + 1),
                new Vec3(x, y + 1, z + 1),
                new Vec3(x + 1, y + 1, z + 1)
        };

        // 各角にパーティクルを配置（プレイヤーにのみ表示）
        for (Vec3 corner : corners) {
            spawnColoredParticle(world, corner, player, colorIndex);
        }
    }

    /**
     * 色付きパーティクルを生成
     * 正しいメソッドシグネチャ: spawnParticles(player, particle, longDistance, alwaysSpawn, x, y, z, count, dx, dy, dz, speed)
     */
    private static void spawnColoredParticle(ServerLevel world, Vec3 pos, ServerPlayer player, int colorIndex) {
        // 色に応じてパーティクルタイプを選択
        switch (colorIndex) {
            case 1: // Red
                world.sendParticles(player, ParticleTypes.DUST_PLUME, true, true,
                        pos.x, pos.y, pos.z, 1, 0.0, 0.0, 0.0, 0.0);
                break;
            case 2: // Yellow
                world.sendParticles(player, ParticleTypes.GLOW, true, true,
                        pos.x, pos.y, pos.z, 1, 0.0, 0.0, 0.0, 0.0);
                break;
            case 3: // Green
                world.sendParticles(player, ParticleTypes.HAPPY_VILLAGER, true, true,
                        pos.x, pos.y, pos.z, 1, 0.0, 0.0, 0.0, 0.0);
                break;
            case 4: // Purple
                world.sendParticles(player, ParticleTypes.PORTAL, true, true,
                        pos.x, pos.y, pos.z, 1, 0.0, 0.0, 0.0, 0.0);
                break;
            case 5: // White
                world.sendParticles(player, ParticleTypes.END_ROD, true, true,
                        pos.x, pos.y, pos.z, 1, 0.0, 0.0, 0.0, 0.0);
                break;
            default: // Cyan
                world.sendParticles(player, ParticleTypes.ELECTRIC_SPARK, true, true,
                        pos.x, pos.y, pos.z, 1, 0.0, 0.0, 0.0, 0.0);
                break;
        }
    }

    /**
     * ブロックの輪郭をパーティクルで描画（より詳細版）
     */
    public static void showDetailedParticleOutline(ServerLevel world, Set<BlockPos> blocks, ServerPlayer player) {
        if (blocks == null || blocks.isEmpty()) {
            return;
        }

        int colorIndex = net.misemise.ClothConfig.Config.outlineColor;

        for (BlockPos pos : blocks) {
            // 12本のエッジ（辺）をパーティクルで描画
            drawEdgeParticles(world, pos, player, colorIndex);
        }
    }

    /**
     * ブロックのエッジ（辺）をパーティクルで描画
     */
    private static void drawEdgeParticles(ServerLevel world, BlockPos pos, ServerPlayer player, int colorIndex) {
        double x = pos.getX();
        double y = pos.getY();
        double z = pos.getZ();

        // 各辺に沿ってパーティクルを配置（0.25ブロック間隔）
        double step = 0.25;

        // 底面の4つの辺
        drawLine(world, player, colorIndex, x, y, z, x + 1, y, z, step);        // 北
        drawLine(world, player, colorIndex, x + 1, y, z, x + 1, y, z + 1, step); // 東
        drawLine(world, player, colorIndex, x + 1, y, z + 1, x, y, z + 1, step); // 南
        drawLine(world, player, colorIndex, x, y, z + 1, x, y, z, step);        // 西

        // 上面の4つの辺
        drawLine(world, player, colorIndex, x, y + 1, z, x + 1, y + 1, z, step);
        drawLine(world, player, colorIndex, x + 1, y + 1, z, x + 1, y + 1, z + 1, step);
        drawLine(world, player, colorIndex, x + 1, y + 1, z + 1, x, y + 1, z + 1, step);
        drawLine(world, player, colorIndex, x, y + 1, z + 1, x, y + 1, z, step);

        // 縦の4つの辺
        drawLine(world, player, colorIndex, x, y, z, x, y + 1, z, step);
        drawLine(world, player, colorIndex, x + 1, y, z, x + 1, y + 1, z, step);
        drawLine(world, player, colorIndex, x + 1, y, z + 1, x + 1, y + 1, z + 1, step);
        drawLine(world, player, colorIndex, x, y, z + 1, x, y + 1, z + 1, step);
    }

    /**
     * 2点間にパーティクルの線を描画
     */
    private static void drawLine(ServerLevel world, ServerPlayer player, int colorIndex,
                                 double x1, double y1, double z1, double x2, double y2, double z2, double step) {
        double dx = x2 - x1;
        double dy = y2 - y1;
        double dz = z2 - z1;
        double distance = Math.sqrt(dx * dx + dy * dy + dz * dz);

        int steps = (int) (distance / step);
        if (steps == 0) steps = 1;

        for (int i = 0; i <= steps; i++) {
            double t = (double) i / steps;
            Vec3 pos = new Vec3(
                    x1 + dx * t,
                    y1 + dy * t,
                    z1 + dz * t
            );
            spawnColoredParticle(world, pos, player, colorIndex);
        }
    }
}