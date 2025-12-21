package net.misemise;

import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Set;

/**
 * 統合版プレイヤー向けの視覚的フィードバック
 * パーティクルを使って一括破壊範囲を表示
 */
public class BedrockVisualHelper {
    private static final Logger LOGGER = LoggerFactory.getLogger("omniminer");

    /**
     * 一括破壊対象のブロックをパーティクルで表示（統合版プレイヤー向け）
     */
    public static void showParticleOutline(ServerWorld world, Set<BlockPos> blocks, ServerPlayerEntity player) {
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
    private static void spawnCornerParticles(ServerWorld world, BlockPos pos, ServerPlayerEntity player, int colorIndex) {
        double x = pos.getX();
        double y = pos.getY();
        double z = pos.getZ();

        // 8つの角の座標
        Vec3d[] corners = {
                new Vec3d(x, y, z),
                new Vec3d(x + 1, y, z),
                new Vec3d(x, y + 1, z),
                new Vec3d(x + 1, y + 1, z),
                new Vec3d(x, y, z + 1),
                new Vec3d(x + 1, y, z + 1),
                new Vec3d(x, y + 1, z + 1),
                new Vec3d(x + 1, y + 1, z + 1)
        };

        // 各角にパーティクルを配置（プレイヤーにのみ表示）
        for (Vec3d corner : corners) {
            spawnColoredParticle(world, corner, player, colorIndex);
        }
    }

    /**
     * 色付きパーティクルを生成
     * 正しいメソッドシグネチャ: spawnParticles(player, particle, longDistance, alwaysSpawn, x, y, z, count, dx, dy, dz, speed)
     */
    private static void spawnColoredParticle(ServerWorld world, Vec3d pos, ServerPlayerEntity player, int colorIndex) {
        // 色に応じてパーティクルタイプを選択
        switch (colorIndex) {
            case 1: // Red
                world.spawnParticles(player, ParticleTypes.DUST_PLUME, true, true,
                        pos.x, pos.y, pos.z, 1, 0.0, 0.0, 0.0, 0.0);
                break;
            case 2: // Yellow
                world.spawnParticles(player, ParticleTypes.GLOW, true, true,
                        pos.x, pos.y, pos.z, 1, 0.0, 0.0, 0.0, 0.0);
                break;
            case 3: // Green
                world.spawnParticles(player, ParticleTypes.HAPPY_VILLAGER, true, true,
                        pos.x, pos.y, pos.z, 1, 0.0, 0.0, 0.0, 0.0);
                break;
            case 4: // Purple
                world.spawnParticles(player, ParticleTypes.PORTAL, true, true,
                        pos.x, pos.y, pos.z, 1, 0.0, 0.0, 0.0, 0.0);
                break;
            case 5: // White
                world.spawnParticles(player, ParticleTypes.END_ROD, true, true,
                        pos.x, pos.y, pos.z, 1, 0.0, 0.0, 0.0, 0.0);
                break;
            default: // Cyan
                world.spawnParticles(player, ParticleTypes.ELECTRIC_SPARK, true, true,
                        pos.x, pos.y, pos.z, 1, 0.0, 0.0, 0.0, 0.0);
                break;
        }
    }

    /**
     * ブロックの輪郭をパーティクルで描画（より詳細版）
     */
    public static void showDetailedParticleOutline(ServerWorld world, Set<BlockPos> blocks, ServerPlayerEntity player) {
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
    private static void drawEdgeParticles(ServerWorld world, BlockPos pos, ServerPlayerEntity player, int colorIndex) {
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
    private static void drawLine(ServerWorld world, ServerPlayerEntity player, int colorIndex,
                                 double x1, double y1, double z1, double x2, double y2, double z2, double step) {
        double dx = x2 - x1;
        double dy = y2 - y1;
        double dz = z2 - z1;
        double distance = Math.sqrt(dx * dx + dy * dy + dz * dz);

        int steps = (int) (distance / step);
        if (steps == 0) steps = 1;

        for (int i = 0; i <= steps; i++) {
            double t = (double) i / steps;
            Vec3d pos = new Vec3d(
                    x1 + dx * t,
                    y1 + dy * t,
                    z1 + dz * t
            );
            spawnColoredParticle(world, pos, player, colorIndex);
        }
    }
}