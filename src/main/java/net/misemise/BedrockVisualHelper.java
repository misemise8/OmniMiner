package net.misemise;

import net.minecraft.particle.ParticleEffect;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.misemise.ClothConfig.Config;

import java.util.Set;

public final class BedrockVisualHelper {
    private static final int MAX_PREVIEW_PACKETS_PER_UPDATE = 128;

    private BedrockVisualHelper() {
    }

    public static void showParticleOutline(
            ServerWorld world,
            Set<BlockPos> blocks,
            ServerPlayerEntity player) {
        showParticleCloud(world, blocks, player, 8, 0.48D);
    }

    public static void showDetailedParticleOutline(
            ServerWorld world,
            Set<BlockPos> blocks,
            ServerPlayerEntity player) {
        showParticleCloud(world, blocks, player, 16, 0.5D);
    }

    private static void showParticleCloud(
            ServerWorld world,
            Set<BlockPos> blocks,
            ServerPlayerEntity player,
            int count,
            double spread) {
        if (blocks == null || blocks.isEmpty()) {
            return;
        }

        ParticleEffect particle = particleForColor(Config.outlineColor);
        int stride = Math.max(
                1,
                (blocks.size() + MAX_PREVIEW_PACKETS_PER_UPDATE - 1)
                        / MAX_PREVIEW_PACKETS_PER_UPDATE);
        int offset = (int) ((world.getTime() / 20L) % stride);
        int index = 0;
        for (BlockPos pos : blocks) {
            if (index++ % stride != offset) {
                continue;
            }
            world.spawnParticles(
                    player,
                    particle,
                    true,
                    pos.getX() + 0.5D,
                    pos.getY() + 0.5D,
                    pos.getZ() + 0.5D,
                    count,
                    spread,
                    spread,
                    spread,
                    0.0D);
        }
    }

    private static ParticleEffect particleForColor(int colorIndex) {
        return switch (colorIndex) {
            case 1 -> ParticleTypes.DUST_PLUME;
            case 2 -> ParticleTypes.GLOW;
            case 3 -> ParticleTypes.HAPPY_VILLAGER;
            case 4 -> ParticleTypes.PORTAL;
            case 5 -> ParticleTypes.END_ROD;
            default -> ParticleTypes.ELECTRIC_SPARK;
        };
    }
}
