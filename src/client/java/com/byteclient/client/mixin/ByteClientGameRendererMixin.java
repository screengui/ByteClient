package com.byteclient.client.mixin;

import com.byteclient.client.ByteClientModules;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.renderer.GameRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GameRenderer.class)
public final class ByteClientGameRendererMixin {
	@Inject(method = "renderLevel", at = @At("TAIL"))
	private void byteClient$applyMotionBlur(DeltaTracker deltaTracker, CallbackInfo callbackInfo) {
		GameRenderer renderer = (GameRenderer) (Object) this;
		if (ByteClientModules.isMotionBlurEnabled()
				&& renderer.getMinecraft().level != null
				&& renderer.getMinecraft().screen == null) {
			renderer.processBlurEffect();
		}
	}
}