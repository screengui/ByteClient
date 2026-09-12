package com.byteclient.client.mixin;

import com.byteclient.client.ByteClientModules;
import net.minecraft.client.OptionInstance;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(OptionInstance.class)
public class ByteClientGammaOptionMixin<T> {
	@Shadow @Final private Component caption;

	@Inject(method = "get", at = @At("HEAD"), cancellable = true)
	private void byteClient$applyFullbright(CallbackInfoReturnable<T> callback) {
		if (caption.equals(Component.translatable("options.gamma"))
				&& ByteClientModules.isFullbrightEnabled()) {
			@SuppressWarnings("unchecked")
			T gamma = (T) Double.valueOf(ByteClientModules.fullbrightGamma());
			callback.setReturnValue(gamma);
		}
	}
}