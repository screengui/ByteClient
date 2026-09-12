package com.byteclient.client.mixin;

import com.byteclient.client.ByteClientModules;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.ScreenEffectRenderer;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ScreenEffectRenderer.class)
public final class ByteClientScreenEffectRendererMixin {
	@Inject(method = "renderFire", at = @At(
			value = "INVOKE",
			target = "Lcom/mojang/blaze3d/vertex/PoseStack;pushPose()V",
			shift = At.Shift.AFTER))
	private static void byteClient$lowerFire(PoseStack poseStack, MultiBufferSource bufferSource,
			TextureAtlasSprite sprite, CallbackInfo callbackInfo) {
		if (ByteClientModules.isLowFireEnabled()) {
			poseStack.translate(0.0f, -ByteClientModules.lowFireOffset(), 0.0f);
			float scale = ByteClientModules.lowFireScale();
			poseStack.scale(scale, scale, scale);
		}
	}
}