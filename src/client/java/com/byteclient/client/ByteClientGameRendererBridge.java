package com.byteclient.client;

import net.minecraft.client.renderer.GameRenderer;

public final class ByteClientGameRendererBridge {
	private ByteClientGameRendererBridge() {
	}

	public static void setPostEffect(GameRenderer renderer, String identifier) {
		ByteClientGameRendererAdapter.set(renderer, identifier);
	}
}