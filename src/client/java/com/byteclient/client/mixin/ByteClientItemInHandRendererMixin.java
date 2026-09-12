package com.byteclient.client.mixin;

import com.byteclient.client.ByteClientModules;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.ItemInHandRenderer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ItemInHandRenderer.class)
public final class ByteClientItemInHandRendererMixin {
	@Inject(method = "renderItem", at = @At("HEAD"))
	private void byteClient$lowerShield(LivingEntity entity, ItemStack stack, ItemDisplayContext displayContext,
			PoseStack poseStack, SubmitNodeCollector submitNodeCollector, int light, CallbackInfo callbackInfo) {
		if (ByteClientModules.shouldLowerShield(stack)
				&& (displayContext == ItemDisplayContext.FIRST_PERSON_RIGHT_HAND
				|| displayContext == ItemDisplayContext.FIRST_PERSON_LEFT_HAND)) {
			poseStack.translate(0.0f, -ByteClientModules.lowShieldOffset(), 0.12f);
		}
	}
}