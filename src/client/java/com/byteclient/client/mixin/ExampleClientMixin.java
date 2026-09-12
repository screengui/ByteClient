package com.byteclient.client.mixin;

import net.minecraft.client.MouseHandler;
import net.minecraft.client.input.MouseButtonInfo;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.byteclient.client.ByteClientModules;

@Mixin(MouseHandler.class)
public class ExampleClientMixin {
	@Inject(at = @At("HEAD"), method = "onButton")
	private void recordClick(long window, MouseButtonInfo button, int action, CallbackInfo info) {
		if (action == 1) {
			ByteClientModules.recordClick(button.button());
		}
	}
}