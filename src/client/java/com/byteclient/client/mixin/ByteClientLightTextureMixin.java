package com.byteclient.client.mixin;

import com.byteclient.client.ByteClientModules;
import net.minecraft.client.renderer.LightTexture;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(LightTexture.class)
public class ByteClientLightTextureMixin {
	@ModifyVariable(method = "updateLightTexture", at = @At("STORE"), index = 16)
	private float byteClient$applyFullbright(float inGameGamma) {
		return ByteClientModules.applyFullbrightGamma(inGameGamma);
	}
}