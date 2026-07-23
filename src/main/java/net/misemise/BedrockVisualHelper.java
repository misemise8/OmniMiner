package net.misemise;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.misemise.ClothConfig.Config;

import java.util.Set;

/**
 * Particle previews for Bedrock Edition players.
 *
 * <p>Each block is represented by one particle packet. The packet asks the
 * client to spread several particles inside the block, avoiding the previous
 * 8 to 60 packets per block.</p>
 */
public final class BedrockVisualHelper {
    private static final int MAX_PREVIEW_PACKETS_PER_UPDATE = 128;

    private BedrockVisualHelper() {
    }

    public static void showParticleOutline(
            ServerLevel world,
            Set<BlockPos> blocks,
            ServerPlayer player) {
        showParticleCloud(world, blocks, player, 8, 0.48D);
    }

    public static void showDetailedParticleOutline(
            ServerLevel world,
            Set<BlockPos> blocks,
            ServerPlayer player) {
        showParticleCloud(world, blocks, player, 16, 0.5D);
    }

    private static void showParticleCloud(
            ServerLevel world,
            Set<BlockPos> blocks,
            ServerPlayer player,
            int count,
            double spread) {
        if (blocks == null || blocks.isEmpty()) {
            return;
        }

        ParticleOptions particle = particleForColor(Config.outlineColor);
        int stride = Math.max(
                1,
                (blocks.size() + MAX_PREVIEW_PACKETS_PER_UPDATE - 1)
                        / MAX_PREVIEW_PACKETS_PER_UPDATE);
        int offset = (int) ((world.getGameTime() / 20L) % stride);
        int index = 0;
        for (BlockPos pos : blocks) {
            if (index++ % stride != offset) {
                continue;
            }
            world.sendParticles(
                    player,
                    particle,
                    true,
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

    private static ParticleOptions particleForColor(int colorIndex) {
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
