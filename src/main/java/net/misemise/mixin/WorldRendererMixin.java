package net.misemise.mixin;

import com.mojang.blaze3d.buffers.GpuBufferSlice;
import net.minecraft.client.render.*;
import net.minecraft.client.util.ObjectAllocator;
import net.misemise.OmniMiner;
import net.misemise.client.BlockHighlightRenderer;
import org.joml.Matrix4f;
import org.joml.Vector4f;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(WorldRenderer.class)
public abstract class WorldRendererMixin {

    @Shadow
    @Final
    private BufferBuilderStorage bufferBuilders;

    @Inject(
            method = "render",
            at = @At("TAIL")
    )
    private void onRenderEnd(
            ObjectAllocator allocator,
            RenderTickCounter tickCounter,
            boolean renderBlockOutline,
            Camera camera,
            Matrix4f positionMatrix,
            Matrix4f matrix4f,
            Matrix4f projectionMatrix,
            GpuBufferSlice fogBuffer,
            Vector4f fogColor,
            boolean renderSky,
            CallbackInfo ci
    ) {
        try {
            VertexConsumerProvider.Immediate immediate = this.bufferBuilders.getEntityVertexConsumers();

            // positionMatrixとcameraを渡す
            BlockHighlightRenderer.render(immediate, camera, positionMatrix);

        } catch (Exception e) {
            OmniMiner.LOGGER.error("Failed to render block highlights", e);
        }
    }
}