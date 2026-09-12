package com.byteclient.client.mixin;

import com.byteclient.client.ByteClientModules;
import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.resource.GraphicsResourceAllocator;
import net.minecraft.client.Camera;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.renderer.LevelRenderer;
import org.joml.Matrix4f;
import org.joml.Vector4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LevelRenderer.class)
public class ByteClientLevelRendererMixin {
	@Inject(method = "renderLevel", at = @At("HEAD"))
	private void byteClient$captureMotionFrame(GraphicsResourceAllocator allocator, DeltaTracker deltaTracker,
			boolean renderBlockOutline, Camera camera, Matrix4f positionMatrix, Matrix4f basicProjectionMatrix,
			Matrix4f projectionMatrix, GpuBufferSlice fogBuffer, Vector4f fogColor, boolean renderSky,
			CallbackInfo callbackInfo) {
		ByteClientModules.captureMotionFrame(positionMatrix, basicProjectionMatrix, camera.position());
	}
}