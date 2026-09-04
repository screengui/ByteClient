package com.byteclient.client;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public class ByteClientMenuScreen extends Screen {

	public ByteClientMenuScreen() {
		super(Component.literal("ByteClient Menu"));
	}

	@Override
	protected void init() {
		super.init();
		// buttons/widgets go here later
	}

	@Override
	public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float delta) {
		super.render(guiGraphics, mouseX, mouseY, delta);
		guiGraphics.drawCenteredString(this.font, this.title, this.width / 2, 20, 0xFFFFFF);
	}

	@Override
	public boolean isPauseScreen() {
		return false;
	}
}