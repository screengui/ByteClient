package com.byteclient.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

public final class ByteClientModuleSettingsScreen extends Screen {
	private static final int BACKGROUND = 0x660B0D11;
	private static final int PANEL = 0xE613171E;
	private static final int BORDER = 0xFF252C36;
	private static final int MUTED = 0xFF8993A1;
	private static final int TEXT = 0xFFF3F5F7;
	private static int RED = 0xFFE52B3D;
	private static int RED_DARK = 0xFF8D1D2B;

	private final Screen parent;
	private final int module;
	private int scroll;
	private boolean draggingScrollbar;
	private boolean draggingMotionBlurStrength;

	public ByteClientModuleSettingsScreen(Screen parent, int module) {
		super(Component.literal(ByteClientModules.moduleName(module) + " Settings"));
		this.parent = parent;
		this.module = module;
	}

	@Override
	public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float delta) {
		RED = ByteClientModules.accentColor();
		RED_DARK = ByteClientModules.accentDarkColor();
		guiGraphics.fill(0, 0, width, height, BACKGROUND);
		PanelBounds panel = panelBounds();
		guiGraphics.fill(panel.left(), panel.top(), panel.right(), panel.bottom(), PANEL);
		guiGraphics.fill(panel.left(), panel.top(), Math.min(panel.left() + 4, panel.right()), panel.bottom(), RED);
		guiGraphics.drawString(this.font, ByteClientModules.moduleName(module), panel.left() + 24, panel.top() + 28, TEXT, true);
		guiGraphics.drawString(this.font, "MODULE SETTINGS", panel.left() + 24, panel.top() + 46, MUTED, false);
		guiGraphics.fill(panel.left() + 24, panel.top() + 64, panel.right() - 24, panel.top() + 65, BORDER);

		int buttonLeft = panel.left() + 24;
		int buttonRight = panel.right() - 24;
		int settingTop = panel.top() + 88;
		int viewportBottom = panel.bottom() - 58;
		int rowCount = ByteClientModules.moduleSettingCount(module) + 1;
		int contentHeight = rowCount * 54 + 32;
		int viewportHeight = Math.max(1, viewportBottom - settingTop);
		int maxScroll = Math.max(0, contentHeight - viewportHeight);
		scroll = clamp(scroll, 0, maxScroll);

		guiGraphics.enableScissor(buttonLeft, settingTop, buttonRight, viewportBottom);
		int settingsCount = ByteClientModules.moduleSettingCount(module);
		for (int setting = 0; setting < settingsCount; setting++) {
			int rowTop = settingTop + setting * 54 - scroll;
			if (module == 8 && setting == 0) {
				drawMotionBlurStrength(guiGraphics, buttonLeft, rowTop, buttonRight, rowTop + 42);
			} else {
				drawButton(guiGraphics, buttonLeft, rowTop, buttonRight, rowTop + 42,
						ByteClientModules.moduleSettingName(module, setting),
						ByteClientModules.moduleSettingValue(module, setting), RED_DARK);
			}
		}
		int enabledTop = settingTop + settingsCount * 54 - scroll;
		drawButton(guiGraphics, buttonLeft, enabledTop, buttonRight, enabledTop + 42,
				"Enabled", ByteClientModules.isEnabled(module) ? "ON" : "OFF",
				ByteClientModules.isEnabled(module) ? RED_DARK : BORDER);
		String summary = ByteClientModules.isHudModule(module)
				? "Position and snap alignment are available in the HUD Editor."
				: ByteClientModules.moduleSummary(module);
		guiGraphics.drawString(this.font, summary, buttonLeft, enabledTop + 68, MUTED, false);
		guiGraphics.disableScissor();

		if (maxScroll > 0) {
			int scrollbarLeft = panel.right() - 12;
			int scrollbarTop = settingTop;
			int scrollbarHeight = Math.max(24, viewportHeight * viewportHeight / contentHeight);
			int scrollbarY = scrollbarTop + (viewportHeight - scrollbarHeight) * scroll / maxScroll;
			guiGraphics.fill(scrollbarLeft, scrollbarTop, scrollbarLeft + 4, viewportBottom, BORDER);
			guiGraphics.fill(scrollbarLeft, scrollbarY, scrollbarLeft + 4, scrollbarY + scrollbarHeight, RED);
		}

		int backTop = panel.bottom() - 42;
		guiGraphics.fill(buttonLeft, backTop, buttonLeft + 110, backTop + 28, BORDER);
		guiGraphics.drawString(this.font, "BACK", buttonLeft + 38, backTop + 10, TEXT, true);
		if (ByteClientModules.isHudModule(module)) {
			guiGraphics.fill(buttonRight - 140, backTop, buttonRight, backTop + 28, RED_DARK);
			guiGraphics.drawString(this.font, "HUD EDITOR", buttonRight - 112, backTop + 10, TEXT, true);
		}
	}

	private void drawButton(GuiGraphics guiGraphics, int left, int top, int right, int bottom,
			String label, String value, int color) {
		guiGraphics.fill(left, top, right, bottom, color);
		guiGraphics.fill(left, top, Math.min(left + 3, right), bottom, RED);
		guiGraphics.drawString(this.font, label, left + 16, top + 10, TEXT, true);
		guiGraphics.drawString(this.font, value, Math.max(left + 120, right - 100), top + 10, RED, true);
		guiGraphics.drawString(this.font, "CLICK TO CHANGE", left + 16, top + 25, MUTED, false);
	}

	private void drawMotionBlurStrength(GuiGraphics graphics, int left, int top, int right, int bottom) {
		graphics.fill(left, top, right, bottom, RED_DARK);
		graphics.fill(left, top, Math.min(left + 3, right), bottom, RED);
		graphics.drawString(this.font, "Strength", left + 16, top + 10, TEXT, true);
		int trackLeft = left + 120;
		int trackRight = right - 52;
		int trackY = top + 20;
		int value = ByteClientModules.motionBlurStrengthPercent();
		int handleX = trackLeft + (trackRight - trackLeft) * value / 100;
		graphics.fill(trackLeft, trackY - 2, trackRight, trackY + 2, BORDER);
		graphics.fill(trackLeft, trackY - 2, handleX, trackY + 2, RED);
		graphics.fill(handleX - 3, trackY - 6, handleX + 4, trackY + 7, TEXT);
		graphics.drawString(this.font, value + "%", right - 42, top + 10, RED, true);
	}

	@Override
	public boolean mouseClicked(MouseButtonEvent event, boolean bl) {
		PanelBounds panel = panelBounds();
		int buttonLeft = panel.left() + 24;
		int buttonRight = panel.right() - 24;
		int settingTop = panel.top() + 88;
		int viewportBottom = panel.bottom() - 58;
		int settingsCount = ByteClientModules.moduleSettingCount(module);
		int rowCount = settingsCount + 1;
		int viewportHeight = Math.max(1, viewportBottom - settingTop);
		int contentHeight = rowCount * 54 + 32;
		int maxScroll = Math.max(0, contentHeight - viewportHeight);

		if (event.button() == 0 && maxScroll > 0
				&& event.x() >= panel.right() - 16 && event.x() <= panel.right()
				&& event.y() >= settingTop && event.y() <= viewportBottom) {
			draggingScrollbar = true;
			updateScrollFromMouse(event.y(), settingTop, viewportHeight, contentHeight, maxScroll);
			return true;
		}
		if (event.button() == 0) {
			for (int setting = 0; setting < settingsCount; setting++) {
				int rowTop = settingTop + setting * 54 - scroll;
				if (inside(event, buttonLeft, buttonRight, rowTop, rowTop + 42)) {
					if (module == 8 && setting == 0) {
						draggingMotionBlurStrength = true;
						updateMotionBlurStrength(event.x(), buttonLeft, buttonRight);
						return true;
					}
					ByteClientModules.cycleModuleSetting(module, setting);
					return true;
				}
			}
			int enabledTop = settingTop + settingsCount * 54 - scroll;
			if (inside(event, buttonLeft, buttonRight, enabledTop, enabledTop + 42)) {
				ByteClientModules.setEnabled(module, !ByteClientModules.isEnabled(module));
				return true;
			}
		}

		int backTop = panel.bottom() - 42;
		if (event.button() == 0 && inside(event, buttonLeft, buttonLeft + 110, backTop, backTop + 28)) {
			onClose();
			return true;
		}
		if (ByteClientModules.isHudModule(module) && event.button() == 0
				&& inside(event, buttonRight - 140, buttonRight, backTop, backTop + 28)) {
			Minecraft.getInstance().setScreen(new ByteClientHudEditorScreen(this));
			return true;
		}
		return super.mouseClicked(event, bl);
	}

	@Override
	public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
		PanelBounds panel = panelBounds();
		int settingTop = panel.top() + 88;
		int viewportBottom = panel.bottom() - 58;
		if (mouseX < panel.left() + 20 || mouseX > panel.right() - 20
				|| mouseY < settingTop || mouseY > viewportBottom) {
			return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
		}
		int viewportHeight = Math.max(1, viewportBottom - settingTop);
		int contentHeight = (ByteClientModules.moduleSettingCount(module) + 1) * 54 + 32;
		int maxScroll = Math.max(0, contentHeight - viewportHeight);
		scroll = clamp(scroll - (int) Math.round(scrollY * 42), 0, maxScroll);
		return true;
	}

	@Override
	public boolean mouseDragged(MouseButtonEvent event, double deltaX, double deltaY) {
		if (draggingMotionBlurStrength && event.button() == 0) {
				PanelBounds panel = panelBounds();
				updateMotionBlurStrength(event.x(), panel.left() + 24, panel.right() - 24);
				return true;
		}
		if (draggingScrollbar && event.button() == 0) {
			PanelBounds panel = panelBounds();
			int settingTop = panel.top() + 88;
			int viewportBottom = panel.bottom() - 58;
			int viewportHeight = Math.max(1, viewportBottom - settingTop);
			int contentHeight = (ByteClientModules.moduleSettingCount(module) + 1) * 54 + 32;
			int maxScroll = Math.max(0, contentHeight - viewportHeight);
			updateScrollFromMouse(event.y(), settingTop, viewportHeight, contentHeight, maxScroll);
			return true;
		}
		return super.mouseDragged(event, deltaX, deltaY);
	}

	@Override
	public boolean mouseReleased(MouseButtonEvent event) {
		if (event.button() == 0) {
			draggingScrollbar = false;
			draggingMotionBlurStrength = false;
		}
		return super.mouseReleased(event);
	}

	private void updateMotionBlurStrength(double mouseX, int left, int right) {
		int trackLeft = left + 120;
		int trackRight = right - 52;
		int percent = (int) Math.round((mouseX - trackLeft) * 100.0 / Math.max(1, trackRight - trackLeft));
		ByteClientModules.setMotionBlurStrengthPercent(percent);
	}

	private void updateScrollFromMouse(double mouseY, int top, int viewportHeight, int contentHeight, int maxScroll) {
		int thumbHeight = Math.max(24, viewportHeight * viewportHeight / contentHeight);
		int travel = Math.max(1, viewportHeight - thumbHeight);
		scroll = clamp((int) Math.round((mouseY - top - thumbHeight / 2.0) * maxScroll / travel), 0, maxScroll);
	}

	private boolean inside(MouseButtonEvent event, int left, int right, int top, int bottom) {
		return event.x() >= left && event.x() <= right && event.y() >= top && event.y() <= bottom;
	}

	private PanelBounds panelBounds() {
		int panelWidth = Math.min(520, Math.max(260, width - 24));
		int panelHeight = Math.min(430, Math.max(240, height - 24));
		int left = Math.max(12, (width - panelWidth) / 2);
		int top = Math.max(12, (height - panelHeight) / 2);
		return new PanelBounds(left, top, Math.min(width - 12, left + panelWidth),
				Math.min(height - 12, top + panelHeight));
	}

	private int clamp(int value, int min, int max) {
		return Math.max(min, Math.min(max, value));
	}

	private record PanelBounds(int left, int top, int right, int bottom) {
	}

	@Override
	public boolean isPauseScreen() {
		return false;
	}

	@Override
	public void onClose() {
		Minecraft.getInstance().setScreen(parent);
	}
}