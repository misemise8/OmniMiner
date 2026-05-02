package net.misemise.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderContext;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderEvents;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;
import net.misemise.OmniMiner;
import org.joml.Matrix4f;
import org.lwjgl.opengl.GL11;
import java.util.*;

public class BlockHighlightRenderer {
    private static final Set<BlockPos> highlightedBlocks = new HashSet<>();

    public static void register() {
        LevelRenderEvents.END_MAIN.register(BlockHighlightRenderer::render);
        OmniMiner.LOGGER.info("BlockHighlightRenderer initialized");
    }

    /**
     * 設定された色を取得
     */
    private static float[] getColor() {
        switch (net.misemise.ClothConfig.Config.outlineColor) {
            case 1:
                return new float[] { 1.0f, 0.0f, 0.0f, 0.6f }; // Red
            case 2:
                return new float[] { 1.0f, 1.0f, 0.0f, 0.6f }; // Yellow
            case 3:
                return new float[] { 0.0f, 1.0f, 0.0f, 0.6f }; // Green
            case 4:
                return new float[] { 0.8f, 0.2f, 1.0f, 0.6f }; // Purple
            case 5:
                return new float[] { 1.0f, 1.0f, 1.0f, 0.6f }; // White
            default:
                return new float[] { 0.0f, 0.8f, 1.0f, 0.6f }; // Cyan (default)
        }
    }

    public static void setHighlightedBlocks(Set<BlockPos> blocks) {
        synchronized (highlightedBlocks) {
            highlightedBlocks.clear();
            if (blocks != null) {
                highlightedBlocks.addAll(blocks);
            }
        }
    }

    public static void clearHighlights() {
        synchronized (highlightedBlocks) {
            highlightedBlocks.clear();
        }
    }

    public static void render(LevelRenderContext context) {
        Set<BlockPos> blocksCopy;
        synchronized (highlightedBlocks) {
            if (highlightedBlocks.isEmpty())
                return;
            blocksCopy = new HashSet<>(highlightedBlocks);
        }

        PoseStack matrices = context.poseStack();
        matrices.pushPose();

        try {
            GL11.glDisable(GL11.GL_DEPTH_TEST);
            GL11.glDepthMask(false);

            MultiBufferSource.BufferSource immediate = context.bufferSource();

            // Use the shared line render type for the outline buffer.
            VertexConsumer vc = immediate.getBuffer(RenderTypes.lines());

            // 色を取得
            float[] color = getColor();
            int ri = (int) (color[0] * 255);
            int gi = (int) (color[1] * 255);
            int bi = (int) (color[2] * 255);
            int ai = (int) (color[3] * 255);

            Vec3 cameraPos = context.levelState().cameraRenderState.pos;
            matrices.translate(-cameraPos.x, -cameraPos.y, -cameraPos.z);
            Matrix4f mat = new Matrix4f(matrices.last().pose());

            Set<Edge> edges = new HashSet<>();

            // アウトラインの太さを取得
            float thickness = net.misemise.ClothConfig.Config.outlineThickness;
            float baseOffset = 0.002f; // 基本オフセット量

            for (BlockPos p : blocksCopy) {
                float x0 = p.getX(), y0 = p.getY(), z0 = p.getZ();
                float x1 = x0 + 1, y1 = y0 + 1, z1 = z0 + 1;

                // すべての隣接ブロックをチェック
                boolean down = blocksCopy.contains(p.below());
                boolean up = blocksCopy.contains(p.above());
                boolean north = blocksCopy.contains(p.north());
                boolean south = blocksCopy.contains(p.south());
                boolean west = blocksCopy.contains(p.west());
                boolean east = blocksCopy.contains(p.east());

                // 各エッジについて、そのエッジが外側かどうかをチェック
                // エッジが外側 = そのエッジに隣接する2つの面が両方とも外側を向いている

                // 底面の4つのエッジ
                if (!down && !north)
                    edges.add(new Edge(x0, y0, z0, x1, y0, z0)); // 北側
                if (!down && !east)
                    edges.add(new Edge(x1, y0, z0, x1, y0, z1)); // 東側
                if (!down && !south)
                    edges.add(new Edge(x1, y0, z1, x0, y0, z1)); // 南側
                if (!down && !west)
                    edges.add(new Edge(x0, y0, z1, x0, y0, z0)); // 西側

                // 上面の4つのエッジ
                if (!up && !north)
                    edges.add(new Edge(x0, y1, z0, x1, y1, z0)); // 北側
                if (!up && !east)
                    edges.add(new Edge(x1, y1, z0, x1, y1, z1)); // 東側
                if (!up && !south)
                    edges.add(new Edge(x1, y1, z1, x0, y1, z1)); // 南側
                if (!up && !west)
                    edges.add(new Edge(x0, y1, z1, x0, y1, z0)); // 西側

                // 縦の4つのエッジ
                if (!north && !west)
                    edges.add(new Edge(x0, y0, z0, x0, y1, z0)); // 北西
                if (!north && !east)
                    edges.add(new Edge(x1, y0, z0, x1, y1, z0)); // 北東
                if (!south && !east)
                    edges.add(new Edge(x1, y0, z1, x1, y1, z1)); // 南東
                if (!south && !west)
                    edges.add(new Edge(x0, y0, z1, x0, y1, z1)); // 南西
            }

            // 太さを表現するために複数レイヤー描画
            for (float layer = 0; layer < thickness; layer += 0.5f) {
                float layerOffset = layer * baseOffset;

                for (Edge e : edges) {
                    // 元の線
                    vc.addVertex(mat, e.x1, e.y1, e.z1).setColor(ri, gi, bi, ai).setUv(0f, 0f).setOverlay(0).setLight(0xF000F0)
                            .setLineWidth(1.0f).setNormal(0f, 1f, 0f);
                    vc.addVertex(mat, e.x2, e.y2, e.z2).setColor(ri, gi, bi, ai).setUv(0f, 0f).setOverlay(0).setLight(0xF000F0)
                            .setLineWidth(1.0f).setNormal(0f, 1f, 0f);

                    if (layer > 0) {
                        // オフセット付きの線を6方向に描画
                        vc.addVertex(mat, e.x1 + layerOffset, e.y1, e.z1).setColor(ri, gi, bi, ai).setUv(0f, 0f).setOverlay(0)
                                .setLight(0xF000F0).setLineWidth(1.0f).setNormal(0f, 1f, 0f);
                        vc.addVertex(mat, e.x2 + layerOffset, e.y2, e.z2).setColor(ri, gi, bi, ai).setUv(0f, 0f).setOverlay(0)
                                .setLight(0xF000F0).setLineWidth(1.0f).setNormal(0f, 1f, 0f);

                        vc.addVertex(mat, e.x1 - layerOffset, e.y1, e.z1).setColor(ri, gi, bi, ai).setUv(0f, 0f).setOverlay(0)
                                .setLight(0xF000F0).setLineWidth(1.0f).setNormal(0f, 1f, 0f);
                        vc.addVertex(mat, e.x2 - layerOffset, e.y2, e.z2).setColor(ri, gi, bi, ai).setUv(0f, 0f).setOverlay(0)
                                .setLight(0xF000F0).setLineWidth(1.0f).setNormal(0f, 1f, 0f);

                        vc.addVertex(mat, e.x1, e.y1 + layerOffset, e.z1).setColor(ri, gi, bi, ai).setUv(0f, 0f).setOverlay(0)
                                .setLight(0xF000F0).setLineWidth(1.0f).setNormal(0f, 1f, 0f);
                        vc.addVertex(mat, e.x2, e.y2 + layerOffset, e.z2).setColor(ri, gi, bi, ai).setUv(0f, 0f).setOverlay(0)
                                .setLight(0xF000F0).setLineWidth(1.0f).setNormal(0f, 1f, 0f);

                        vc.addVertex(mat, e.x1, e.y1 - layerOffset, e.z1).setColor(ri, gi, bi, ai).setUv(0f, 0f).setOverlay(0)
                                .setLight(0xF000F0).setLineWidth(1.0f).setNormal(0f, 1f, 0f);
                        vc.addVertex(mat, e.x2, e.y2 - layerOffset, e.z2).setColor(ri, gi, bi, ai).setUv(0f, 0f).setOverlay(0)
                                .setLight(0xF000F0).setLineWidth(1.0f).setNormal(0f, 1f, 0f);

                        vc.addVertex(mat, e.x1, e.y1, e.z1 + layerOffset).setColor(ri, gi, bi, ai).setUv(0f, 0f).setOverlay(0)
                                .setLight(0xF000F0).setLineWidth(1.0f).setNormal(0f, 1f, 0f);
                        vc.addVertex(mat, e.x2, e.y2, e.z2 + layerOffset).setColor(ri, gi, bi, ai).setUv(0f, 0f).setOverlay(0)
                                .setLight(0xF000F0).setLineWidth(1.0f).setNormal(0f, 1f, 0f);

                        vc.addVertex(mat, e.x1, e.y1, e.z1 - layerOffset).setColor(ri, gi, bi, ai).setUv(0f, 0f).setOverlay(0)
                                .setLight(0xF000F0).setLineWidth(1.0f).setNormal(0f, 1f, 0f);
                        vc.addVertex(mat, e.x2, e.y2, e.z2 - layerOffset).setColor(ri, gi, bi, ai).setUv(0f, 0f).setOverlay(0)
                                .setLight(0xF000F0).setLineWidth(1.0f).setNormal(0f, 1f, 0f);
                    }
                }
            }

            // Flush the line buffer.
            immediate.endBatch(RenderTypes.lines());

            GL11.glDepthMask(true);
            GL11.glEnable(GL11.GL_DEPTH_TEST);
        } catch (Throwable t) {
            OmniMiner.LOGGER.error("Error rendering highlights", t);
            try {
                GL11.glDepthMask(true);
                GL11.glEnable(GL11.GL_DEPTH_TEST);
            } catch (Throwable ignored) {
            }
        } finally {
            matrices.popPose();
        }
    }

    private static class Edge {
        final float x1, y1, z1, x2, y2, z2;
        private final int hash;

        Edge(float x1, float y1, float z1, float x2, float y2, float z2) {
            if (cmp(x1, y1, z1, x2, y2, z2) <= 0) {
                this.x1 = x1;
                this.y1 = y1;
                this.z1 = z1;
                this.x2 = x2;
                this.y2 = y2;
                this.z2 = z2;
            } else {
                this.x1 = x2;
                this.y1 = y2;
                this.z1 = z2;
                this.x2 = x1;
                this.y2 = y1;
                this.z2 = z1;
            }
            hash = Objects.hash(this.x1, this.y1, this.z1, this.x2, this.y2, this.z2);
        }

        int cmp(float ax, float ay, float az, float bx, float by, float bz) {
            int c = Float.compare(ax, bx);
            if (c != 0)
                return c;
            c = Float.compare(ay, by);
            if (c != 0)
                return c;
            return Float.compare(az, bz);
        }

        public int hashCode() {
            return hash;
        }

        public boolean equals(Object o) {
            if (!(o instanceof Edge))
                return false;
            Edge e = (Edge) o;
            return x1 == e.x1 && y1 == e.y1 && z1 == e.z1 && x2 == e.x2 && y2 == e.y2 && z2 == e.z2;
        }
    }
}
