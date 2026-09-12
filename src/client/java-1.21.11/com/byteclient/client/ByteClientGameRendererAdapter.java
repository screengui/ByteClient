package com.byteclient.client;

import com.byteclient.client.mixin.ByteClientGameRendererAccessor;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.resources.Identifier;

public final class ByteClientGameRendererAdapter {
	private ByteClientGameRendererAdapter() {
	}

	public static void set(GameRenderer renderer, String identifier) {
		((ByteClientGameRendererAccessor) renderer).byteClient$setPostEffect(Identifier.parse(identifier));
	}
}