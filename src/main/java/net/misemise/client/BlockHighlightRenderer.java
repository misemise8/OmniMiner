package net.misemise.client;

import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.misemise.OmniMiner;
import org.joml.Matrix4f;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

public class BlockHighlightRenderer {
    private static final Set<BlockPos> highlightedBlocks = new HashSet<>();

    public static void register() {
        WorldRenderEvents.AFTER_ENTITIES.register(BlockHighlightRenderer::render);
        OmniMiner.LOGGER.info("BlockHighlightRenderer initialized");
    }

    private static float[] getColor() {
        switch (net.misemise.ClothConfig.Config.outlineColor) {
            case 1:
                return new float[] { 1.0f, 0.0f, 0.0f, 0.6f };
            case 2:
                return new float[] { 1.0f, 1.0f, 0.0f, 0.6f };
            case 3:
                return new float[] { 0.0f, 1.0f, 0.0f, 0.6f };
            case 4:
                return new float[] { 0.8f, 0.2f, 1.0f, 0.6f };
            case 5:
                return new float[] { 1.0f, 1.0f, 1.0f, 0.6f };
            default:
                return new float[] { 0.0f, 0.8f, 1.0f, 0.6f };
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

    private static void render(WorldRenderContext context) {
        VertexConsumerProvider consumers = context.consumers();
        MatrixStack matrices = context.matrixStack();
        if (consumers == null || matrices == null) {
            return;
        }

        Set<BlockPos> blocksCopy;
        synchronized (highlightedBlocks) {
            if (highlightedBlocks.isEmpty()) {
                return;
            }
            blocksCopy = new HashSet<>(highlightedBlocks);
        }

        matrices.push();
        try {
            Camera camera = context.camera();
            Vec3d cameraPos = camera.getPos();
            matrices.translate(-cameraPos.x, -cameraPos.y, -cameraPos.z);
            Matrix4f mat = matrices.peek().getPositionMatrix();
            VertexConsumer vc = consumers.getBuffer(RenderLayer.getLines());

            float[] color = getColor();
            int ri = (int) (color[0] * 255);
            int gi = (int) (color[1] * 255);
            int bi = (int) (color[2] * 255);
            int ai = (int) (color[3] * 255);

            Set<Edge> edges = collectEdges(blocksCopy);
            float thickness = net.misemise.ClothConfig.Config.outlineThickness;
            float baseOffset = 0.002f;

            for (float layer = 0; layer < thickness; layer += 0.5f) {
                float layerOffset = layer * baseOffset;

                for (Edge e : edges) {
                    emitLine(vc, mat, e.x1, e.y1, e.z1, e.x2, e.y2, e.z2, ri, gi, bi, ai);

                    if (layer > 0) {
                        emitLine(vc, mat, e.x1 + layerOffset, e.y1, e.z1, e.x2 + layerOffset, e.y2, e.z2, ri, gi, bi, ai);
                        emitLine(vc, mat, e.x1 - layerOffset, e.y1, e.z1, e.x2 - layerOffset, e.y2, e.z2, ri, gi, bi, ai);
                        emitLine(vc, mat, e.x1, e.y1 + layerOffset, e.z1, e.x2, e.y2 + layerOffset, e.z2, ri, gi, bi, ai);
                        emitLine(vc, mat, e.x1, e.y1 - layerOffset, e.z1, e.x2, e.y2 - layerOffset, e.z2, ri, gi, bi, ai);
                        emitLine(vc, mat, e.x1, e.y1, e.z1 + layerOffset, e.x2, e.y2, e.z2 + layerOffset, ri, gi, bi, ai);
                        emitLine(vc, mat, e.x1, e.y1, e.z1 - layerOffset, e.x2, e.y2, e.z2 - layerOffset, ri, gi, bi, ai);
                    }
                }
            }
        } catch (Throwable t) {
            OmniMiner.LOGGER.error("Error rendering highlights", t);
        } finally {
            matrices.pop();
        }
    }

    private static Set<Edge> collectEdges(Set<BlockPos> blocks) {
        Set<Edge> edges = new HashSet<>();

        for (BlockPos p : blocks) {
            float x0 = p.getX(), y0 = p.getY(), z0 = p.getZ();
            float x1 = x0 + 1, y1 = y0 + 1, z1 = z0 + 1;

            boolean down = blocks.contains(p.down());
            boolean up = blocks.contains(p.up());
            boolean north = blocks.contains(p.north());
            boolean south = blocks.contains(p.south());
            boolean west = blocks.contains(p.west());
            boolean east = blocks.contains(p.east());

            if (!down && !north) {
                edges.add(new Edge(x0, y0, z0, x1, y0, z0));
            }
            if (!down && !east) {
                edges.add(new Edge(x1, y0, z0, x1, y0, z1));
            }
            if (!down && !south) {
                edges.add(new Edge(x1, y0, z1, x0, y0, z1));
            }
            if (!down && !west) {
                edges.add(new Edge(x0, y0, z1, x0, y0, z0));
            }

            if (!up && !north) {
                edges.add(new Edge(x0, y1, z0, x1, y1, z0));
            }
            if (!up && !east) {
                edges.add(new Edge(x1, y1, z0, x1, y1, z1));
            }
            if (!up && !south) {
                edges.add(new Edge(x1, y1, z1, x0, y1, z1));
            }
            if (!up && !west) {
                edges.add(new Edge(x0, y1, z1, x0, y1, z0));
            }

            if (!north && !west) {
                edges.add(new Edge(x0, y0, z0, x0, y1, z0));
            }
            if (!north && !east) {
                edges.add(new Edge(x1, y0, z0, x1, y1, z0));
            }
            if (!south && !east) {
                edges.add(new Edge(x1, y0, z1, x1, y1, z1));
            }
            if (!south && !west) {
                edges.add(new Edge(x0, y0, z1, x0, y1, z1));
            }
        }

        return edges;
    }

    private static void emitLine(VertexConsumer vc, Matrix4f mat, float x1, float y1, float z1,
            float x2, float y2, float z2, int red, int green, int blue, int alpha) {
        vc.vertex(mat, x1, y1, z1).color(red, green, blue, alpha).normal(0.0f, 1.0f, 0.0f);
        vc.vertex(mat, x2, y2, z2).color(red, green, blue, alpha).normal(0.0f, 1.0f, 0.0f);
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
            if (c != 0) {
                return c;
            }
            c = Float.compare(ay, by);
            if (c != 0) {
                return c;
            }
            return Float.compare(az, bz);
        }

        public int hashCode() {
            return hash;
        }

        public boolean equals(Object o) {
            if (!(o instanceof Edge)) {
                return false;
            }
            Edge e = (Edge) o;
            return x1 == e.x1 && y1 == e.y1 && z1 == e.z1 && x2 == e.x2 && y2 == e.y2 && z2 == e.z2;
        }
    }
}
