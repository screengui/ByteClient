package com.byteclient.client;

import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.buffers.Std140Builder;
import com.mojang.blaze3d.buffers.Std140SizeCalculator;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.platform.DepthTestFunction;
import com.mojang.blaze3d.systems.CommandEncoder;
import com.mojang.blaze3d.systems.GpuDevice;
import com.mojang.blaze3d.systems.RenderPass;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.FilterMode;
import com.mojang.blaze3d.textures.GpuTexture;
import com.mojang.blaze3d.textures.GpuTextureView;
import com.mojang.blaze3d.textures.TextureFormat;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MappableRingBuffer;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Matrix4fc;

import java.util.OptionalInt;

public final class ByteClientMotionBlurRenderer {
	private static final int MAX_SAMPLES = 48;
	private static final int UNIFORM_SIZE = new Std140SizeCalculator()
			.putMat4f().putMat4f().putMat4f().putMat4f()
			.putVec3().putVec2().putFloat().putInt().putInt().putInt().putInt().get();
	private static final RenderPipeline PIPELINE = RenderPipeline.builder()
			.withLocation(Identifier.fromNamespaceAndPath("byte-client", "pipeline/motion_blur"))
			.withVertexShader("core/screenquad")
			.withFragmentShader(Identifier.fromNamespaceAndPath("byte-client", "post/motion_blur"))
			.withSampler("MainSampler")
			.withSampler("MainDepthSampler")
			.withUniform("MotionBlurUniforms", com.mojang.blaze3d.shaders.UniformType.UNIFORM_BUFFER)
			.withoutBlend()
			.withDepthWrite(false)
			.withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
			.withVertexFormat(DefaultVertexFormat.EMPTY, VertexFormat.Mode.TRIANGLES)
			.build();

	private static final Matrix4f currentModelView = new Matrix4f();
	private static final Matrix4f currentProjection = new Matrix4f();
	private static final Matrix4f previousModelView = new Matrix4f();
	private static final Matrix4f previousProjection = new Matrix4f();
	private static final Matrix4f modelViewInverse = new Matrix4f();
	private static final Matrix4f projectionInverse = new Matrix4f();
	private static double cameraX;
	private static double cameraY;
	private static double cameraZ;
	private static double previousCameraX;
	private static double previousCameraY;
	private static double previousCameraZ;
	private static boolean frameCaptured;
	private static boolean previousFrameReady;
	private static Object previousLevel;
	private static GpuTexture blurTarget;
	private static GpuTextureView blurTargetView;
	private static MappableRingBuffer uniformBuffer;

	private ByteClientMotionBlurRenderer() {
	}

	static void capture(Matrix4fc modelView, Matrix4fc projection, Vec3 camera) {
		if (frameCaptured) {
			return;
		}
		currentModelView.set(modelView);
		currentProjection.set(projection);
		cameraX = camera.x;
		cameraY = camera.y;
		cameraZ = camera.z;
		frameCaptured = true;
	}

	static void render(Minecraft client) {
		boolean shouldRun = ByteClientModules.isMotionBlurEnabled()
				&& ByteClientModules.motionBlurStrengthPercent() > 0
				&& client.level != null && client.screen == null;
		if (client.level != previousLevel) {
			previousLevel = client.level;
			previousFrameReady = false;
		}
		if (!frameCaptured) {
			return;
		}
		frameCaptured = false;
		if (shouldRun && previousFrameReady) {
			renderPass(client);
		}
		previousModelView.set(currentModelView);
		previousProjection.set(currentProjection);
		previousCameraX = cameraX;
		previousCameraY = cameraY;
		previousCameraZ = cameraZ;
		previousFrameReady = shouldRun;
	}

	static void reset() {
		frameCaptured = false;
		previousFrameReady = false;
	}

	public static void close() {
		closeTarget();
		if (uniformBuffer != null) {
			uniformBuffer.close();
			uniformBuffer = null;
		}
		reset();
		previousLevel = null;
	}

	private static void renderPass(Minecraft client) {
		var main = client.getMainRenderTarget();
		GpuTexture color = main.getColorTexture();
		GpuTextureView colorView = main.getColorTextureView();
		GpuTextureView depthView = main.getDepthTextureView();
		if (color == null || colorView == null || depthView == null) {
			return;
		}
		GpuDevice device = RenderSystem.getDevice();
		int width = main.width;
		int height = main.height;
		if (blurTarget == null || blurTarget.getWidth(0) != width || blurTarget.getHeight(0) != height) {
			closeTarget();
			blurTarget = device.createTexture(() -> "Byte Client motion blur target",
					GpuTexture.USAGE_RENDER_ATTACHMENT | GpuTexture.USAGE_TEXTURE_BINDING,
					TextureFormat.RGBA8, width, height, 1, 1);
			blurTargetView = device.createTextureView(blurTarget);
		}
		if (uniformBuffer == null) {
			uniformBuffer = new MappableRingBuffer(() -> "Byte Client motion blur uniforms",
					GpuBuffer.USAGE_UNIFORM | GpuBuffer.USAGE_MAP_WRITE, UNIFORM_SIZE);
		}
		modelViewInverse.set(currentModelView).invert();
		projectionInverse.set(currentProjection).invert();
		CommandEncoder encoder = device.createCommandEncoder();
		try (GpuBuffer.MappedView mapped = encoder.mapBuffer(uniformBuffer.currentBuffer(), false, true)) {
			Std140Builder.intoBuffer(mapped.data())
					.putMat4f(modelViewInverse).putMat4f(projectionInverse)
					.putMat4f(previousModelView).putMat4f(previousProjection)
					.putVec3((float) (cameraX - previousCameraX), (float) (cameraY - previousCameraY),
							(float) (cameraZ - previousCameraZ))
					.putVec2(width, height)
					.putFloat(ByteClientModules.motionBlurBlendFactor())
					.putInt(MAX_SAMPLES).putInt(isFirstPerson(client) ? 0 : 1)
					.putInt(0).putInt(depthView != null && depthSamplingWorks() ? 1 : 0);
		}
		try (RenderPass pass = encoder.createRenderPass(() -> "Byte Client motion blur", blurTargetView,
				OptionalInt.empty())) {
			pass.setPipeline(PIPELINE);
			RenderSystem.bindDefaultUniforms(pass);
			pass.setUniform("MotionBlurUniforms", uniformBuffer.currentBuffer());
			pass.bindTexture("MainSampler", colorView, RenderSystem.getSamplerCache().getClampToEdge(FilterMode.LINEAR));
			pass.bindTexture("MainDepthSampler", depthView,
					RenderSystem.getSamplerCache().getClampToEdge(FilterMode.NEAREST));
			pass.draw(0, 3);
		}
		try (RenderPass pass = encoder.createRenderPass(() -> "Byte Client motion blur blit", colorView,
				OptionalInt.empty())) {
			pass.setPipeline(RenderPipelines.TRACY_BLIT);
			RenderSystem.bindDefaultUniforms(pass);
			pass.bindTexture("InSampler", blurTargetView,
					RenderSystem.getSamplerCache().getClampToEdge(FilterMode.NEAREST));
			pass.draw(0, 3);
		}
		uniformBuffer.rotate();
	}

	private static boolean isFirstPerson(Minecraft client) {
		return client.options.getCameraType().isFirstPerson()
				&& (client.player == null || !client.player.isPassenger());
	}

	private static boolean depthSamplingWorks() {
		String backend = RenderSystem.getDevice().getBackendName();
		return backend == null || !backend.toLowerCase().contains("vulkan");
	}

	private static void closeTarget() {
		if (blurTargetView != null) {
			blurTargetView.close();
			blurTargetView = null;
		}
		if (blurTarget != null) {
			blurTarget.close();
			blurTarget = null;
		}
	}
}
