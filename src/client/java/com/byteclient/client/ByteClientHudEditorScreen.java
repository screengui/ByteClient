package com.byteclient.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

public class ByteClientHudEditorScreen extends Screen {
	private static final int BACKGROUND = 0x660B0D11;
	private static final int PANEL = 0xE613171E;
	private static final int BORDER = 0xFF252C36;
	private static final int MUTED = 0xFF8993A1;
	private static final int TEXT = 0xFFF3F5F7;
	private static final int RED = 0xFFE52B3D;
	private static final int RED_DARK = 0xFF8D1D2B;

	private final Screen parent;
	private int selectedModule = -1;
	private int dragOffsetX;
	private int dragOffsetY;
	private boolean dragging;

	public ByteClientHudEditorScreen(Screen parent) {
		super(Component.literal("ByteClient HUD Editor"));
		this.parent = parent;
	}

	@Override
	public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float delta) {
		guiGraphics.fill(0, 0, this.width, this.height, BACKGROUND);
		guiGraphics.fill(18, 18, this.width - 18, this.height - 18, PANEL);
		guiGraphics.fill(18, 18, 22, this.height - 18, RED);
		guiGraphics.drawString(this.font, "HUD EDITOR", 42, 34, TEXT, true);
		guiGraphics.drawString(this.font, "Drag modules to move them. Right-click a module to toggle it.", 42, 51, MUTED, false);

		ByteClientModules.renderHudEditor(Minecraft.getInstance(), guiGraphics);
		if (selectedModule >= 0) {
			int left = ByteClientModules.hudX(selectedModule, this.width);
			int top = ByteClientModules.hudY(selectedModule, this.height);
			int right = left + ByteClientModules.hudWidth(selectedModule);
			int bottom = top + ByteClientModules.hudHeight(selectedModule);
			guiGraphics.fill(left - 2, top - 2, right + 2, top, RED);
			guiGraphics.fill(left - 2, bottom, right + 2, bottom + 2, RED);
			guiGraphics.fill(left - 2, top, left, bottom, RED);
			guiGraphics.fill(right, top, right + 2, bottom, RED);
			guiGraphics.drawString(this.font, ByteClientModules.moduleName(selectedModule), left, Math.max(64, top - 12), RED, true);
		}

		int bottom = this.height - 48;
		guiGraphics.fill(42, bottom, 168, bottom + 28, RED_DARK);
		guiGraphics.fill(42, bottom, 45, bottom + 28, RED);
		guiGraphics.drawString(this.font, "RESET POSITIONS", 58, bottom + 10, TEXT, true);
		guiGraphics.fill(this.width - 168, bottom, this.width - 42, bottom + 28, BORDER);
		guiGraphics.drawString(this.font, "DONE", this.width - 126, bottom + 10, TEXT, true);
	}

	@Override
	public boolean mouseClicked(MouseButtonEvent event, boolean bl) {
		int mouseX = (int) event.x();
		int mouseY = (int) event.y();
		int bottom = this.height - 48;
		if (event.button() == 0 && mouseX >= this.width - 168 && mouseX <= this.width - 42
				&& mouseY >= bottom && mouseY <= bottom + 28) {
			onClose();
			return true;
		}
		if (event.button() == 0 && mouseX >= 42 && mouseX <= 168
				&& mouseY >= bottom && mouseY <= bottom + 28) {
			ByteClientModules.resetHudPositions(this.width, this.height);
			selectedModule = -1;
			dragging = false;
			return true;
		}

		int module = ByteClientModules.hudModuleAt(mouseX, mouseY, this.width, this.height);
		if (module >= 0 && event.button() == 1) {
			ByteClientModules.setEnabled(module, !ByteClientModules.isEnabled(module));
			return true;
		}
		if (module >= 0 && event.button() == 0) {
			selectedModule = module;
			dragOffsetX = mouseX - ByteClientModules.hudX(module, this.width);
			dragOffsetY = mouseY - ByteClientModules.hudY(module, this.height);
			dragging = true;
			return true;
		}
		if (event.button() == 0) {
			dragging = false;
			selectedModule = -1;
		}
		return super.mouseClicked(event, bl);
	}

	@Override
	public boolean mouseDragged(MouseButtonEvent event, double deltaX, double deltaY) {
		if (dragging && selectedModule >= 0 && event.button() == 0) {
			ByteClientModules.setHudPositionSnapped(
					selectedModule,
					(int) event.x() - dragOffsetX,
					(int) event.y() - dragOffsetY,
					this.width,
					this.height
			);
			return true;
		}
		return super.mouseDragged(event, deltaX, deltaY);
	}

	@Override
	public boolean mouseReleased(MouseButtonEvent event) {
		if (event.button() == 0) {
			dragging = false;
			ByteClientModules.saveHudPositions();
		}
		return super.mouseReleased(event);
	}

	@Override
	public boolean isPauseScreen() {
		return false;
	}

	@Override
	public void onClose() {
		ByteClientModules.saveHudPositions();
		Minecraft.getInstance().setScreen(parent);
	}
}