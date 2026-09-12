package com.byteclient.client.mixin;

import com.byteclient.client.ByteClientModules;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(GameRenderer.class)
public class ByteClientNightVisionMixin {
	@Inject(method = "getNightVisionScale", at = @At("HEAD"), cancellable = true)
	private static void byteClient$applyNightVision(LivingEntity entity, float partialTick,
			CallbackInfoReturnable<Float> callback) {
		if (ByteClientModules.isFullbrightNightVision()) {
			callback.setReturnValue(1.0F);
		}
	}
}