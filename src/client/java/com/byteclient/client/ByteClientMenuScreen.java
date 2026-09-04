package com.byteclient.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

public class ByteClientMenuScreen extends Screen {
	private static final int BACKGROUND = 0xFF0B0D11;
	private static final int PANEL = 0xFF13171E;
	private static final int PANEL_HIGHLIGHT = 0xFF1A2029;
	private static final int BORDER = 0xFF252C36;
	private static final int MUTED = 0xFF8993A1;
	private static final int TEXT = 0xFFF3F5F7;
	private static final int RED = 0xFFE52B3D;
	private static final int RED_DARK = 0xFF8D1D2B;

	private final String[] tabs = {"Overview", "Modules", "Settings"};
	private final String[] moduleNames = {"FPS BOOST", "CPS DISPLAY", "FULLBRIGHT", "ARMOR STATUS", "KEYSTROKES", "PING DISPLAY"};
	private final String[] moduleDescriptions = {
		"Optimized rendering profile", "Shows clicks per second", "Brightens dark areas", "Shows equipped armor", "Shows movement inputs", "Displays current latency"
	};
	private int selectedTab;
	private int hoveredModule = -1;


	public ByteClientMenuScreen() {
		super(Component.literal("ByteClient"));
	}

	@Override
	public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float delta) {
		guiGraphics.fill(0, 0, this.width, this.height, BACKGROUND);
		int left = Math.max(18, (this.width - 920) / 2);
		int top = Math.max(18, (this.height - 560) / 2);
		int right = Math.min(this.width - 18, left + 920);
		int bottom = Math.min(this.height - 18, top + 560);
		int sidebarRight = left + 172;

		guiGraphics.fill(left, top, right, bottom, PANEL);
		guiGraphics.fill(left, top, sidebarRight, bottom, 0xFF10141A);
		guiGraphics.fill(sidebarRight, top, sidebarRight + 1, bottom, BORDER);
		guiGraphics.fill(left, top, left + 4, bottom, RED);
		guiGraphics.drawString(this.font, "BYTE", left + 24, top + 25, TEXT, true);
		guiGraphics.drawString(this.font, "CLIENT", left + 24, top + 39, RED, true);
		guiGraphics.drawString(this.font, "UTILITY CLIENT", left + 24, top + 61, MUTED, false);

		for (int index = 0; index < tabs.length; index++) {
			int tabTop = top + 96 + index * 38;
			boolean active = selectedTab == index;
			if (active) {
				guiGraphics.fill(left + 14, tabTop - 5, sidebarRight - 14, tabTop + 23, RED_DARK);
				guiGraphics.fill(left + 14, tabTop - 5, left + 17, tabTop + 23, RED);
			}
			guiGraphics.drawString(this.font, tabs[index], left + 30, tabTop + 4, active ? TEXT : MUTED, active);
		}

		guiGraphics.drawString(this.font, "RSHIFT", left + 24, bottom - 43, MUTED, false);
		guiGraphics.drawString(this.font, "TOGGLE MENU", left + 24, bottom - 29, 0xFFB7BEC8, false);
		guiGraphics.fill(left + 24, bottom - 16, left + 148, bottom - 15, BORDER);

		int contentLeft = sidebarRight + 32;
		int contentRight = right - 32;
		guiGraphics.drawString(this.font, tabs[selectedTab].toUpperCase(), contentLeft, top + 30, TEXT, true);
		guiGraphics.drawString(this.font, "Your setup, at a glance.", contentLeft, top + 48, MUTED, false);
		guiGraphics.fill(contentLeft, top + 68, contentRight, top + 69, BORDER);
		if (selectedTab == 0) {
			renderOverview(guiGraphics, contentLeft, contentRight, top);
		} else if (selectedTab == 1) {
			renderModules(guiGraphics, contentLeft, top);
		} else {
			renderSettings(guiGraphics, contentLeft, contentRight, top);
		}

		guiGraphics.fill(contentLeft, bottom - 35, contentRight, bottom - 34, BORDER);
		guiGraphics.drawString(this.font, "BYTECLIENT  /  1.0.0", contentLeft, bottom - 23, MUTED, false);
		guiGraphics.drawString(this.font, "ONLINE", contentRight - 45, bottom - 23, RED, true);
	}

	private void renderOverview(GuiGraphics guiGraphics, int left, int right, int top) {
		renderMetric(guiGraphics, left, top + 92, 142, "FPS", Integer.toString(Minecraft.getInstance().getFps()), "STABLE");
		renderMetric(guiGraphics, left + 154, top + 92, 142, "PING", "24", "MS");
		renderMetric(guiGraphics, left + 308, top + 92, 142, "ACTIVE", "4 / 6", "MODULES");
		int panelTop = top + 184;
		renderPanel(guiGraphics, left, panelTop, 296, 152);
		guiGraphics.drawString(this.font, "QUICK MODULES", left + 16, panelTop + 18, TEXT, true);
		for (int index = 0; index < 3; index++) {
			int rowTop = panelTop + 43 + index * 32;
			guiGraphics.drawString(this.font, moduleNames[index], left + 16, rowTop, 0xFFD7DCE2, false);
			renderToggle(guiGraphics, left + 243, rowTop - 3, ByteClientModules.isEnabled(index));
		}
		renderPanel(guiGraphics, left + 308, panelTop, right - left - 308, 152);
		guiGraphics.drawString(this.font, "SESSION", left + 324, panelTop + 18, TEXT, true);
		guiGraphics.drawString(this.font, "Playing with ByteClient", left + 324, panelTop + 48, 0xFFD7DCE2, false);
		guiGraphics.drawString(this.font, "All systems operational", left + 324, panelTop + 70, MUTED, false);
		guiGraphics.fill(left + 324, panelTop + 101, right - 16, panelTop + 105, BORDER);
		guiGraphics.fill(left + 324, panelTop + 101, left + 470, panelTop + 105, RED);
		guiGraphics.drawString(this.font, "PROFILE  /  PERFORMANCE", left + 324, panelTop + 124, RED, true);
	}

	private void renderModules(GuiGraphics guiGraphics, int left, int top) {
		for (int index = 0; index < moduleNames.length; index++) {
			int cardLeft = left + (index % 2) * 236;
			int cardTop = top + 92 + (index / 2) * 84;
			renderPanel(guiGraphics, cardLeft, cardTop, 220, 68);
			if (hoveredModule == index) {
				guiGraphics.fill(cardLeft, cardTop, cardLeft + 3, cardTop + 68, RED);
			}
			guiGraphics.drawString(this.font, moduleNames[index], cardLeft + 15, cardTop + 17, TEXT, true);
			guiGraphics.drawString(this.font, moduleDescriptions[index], cardLeft + 15, cardTop + 36, MUTED, false);
			renderToggle(guiGraphics, cardLeft + 177, cardTop + 15, ByteClientModules.isEnabled(index));
		}
	}

	private void renderSettings(GuiGraphics guiGraphics, int left, int right, int top) {
		renderPanel(guiGraphics, left, top + 92, right - left, 205);
		guiGraphics.drawString(this.font, "CLIENT SETTINGS", left + 18, top + 111, TEXT, true);
		renderSettingRow(guiGraphics, left + 18, top + 139, "Interface scale", "100%");
		renderSettingRow(guiGraphics, left + 18, top + 174, "Accent color", "BYTE RED");
		renderSettingRow(guiGraphics, left + 18, top + 209, "Animations", "ON");
	}

	private void renderMetric(GuiGraphics guiGraphics, int left, int top, int width, String label, String value, String suffix) {
		renderPanel(guiGraphics, left, top, width, 70);
		guiGraphics.drawString(this.font, label, left + 14, top + 16, MUTED, true);
		guiGraphics.drawString(this.font, value, left + 14, top + 35, TEXT, true);
		guiGraphics.drawString(this.font, suffix, left + width - 48, top + 42, RED, true);
	}

	private void renderPanel(GuiGraphics guiGraphics, int left, int top, int width, int height) {
		guiGraphics.fill(left, top, left + width, top + height, BORDER);
		guiGraphics.fill(left + 1, top + 1, left + width - 1, top + height - 1, PANEL_HIGHLIGHT);
	}

	private void renderToggle(GuiGraphics guiGraphics, int left, int top, boolean enabled) {
		guiGraphics.fill(left, top, left + 28, top + 14, enabled ? RED : 0xFF343B45);
		guiGraphics.fill(enabled ? left + 16 : left + 2, top + 2, enabled ? left + 26 : left + 12, top + 12, TEXT);
	}

	private void renderSettingRow(GuiGraphics guiGraphics, int left, int top, String label, String value) {
		guiGraphics.drawString(this.font, label, left, top, 0xFFD7DCE2, false);
		guiGraphics.drawString(this.font, value, left + 270, top, RED, true);
		guiGraphics.fill(left, top + 19, left + 430, top + 20, BORDER);
	}

	@Override
	public boolean mouseClicked(MouseButtonEvent event, boolean bl) {
		double mouseX = event.x();
		double mouseY = event.y();
		if (event.button() != 0) {
			return super.mouseClicked(event, bl);
		}
		int left = Math.max(18, (this.width - 920) / 2);
		int top = Math.max(18, (this.height - 560) / 2);
		int sidebarRight = left + 172;
		for (int index = 0; index < tabs.length; index++) {
			int tabTop = top + 91 + index * 38;
			if (mouseX >= left + 14 && mouseX <= sidebarRight - 14 && mouseY >= tabTop && mouseY <= tabTop + 28) {
				selectedTab = index;
				return true;
			}
		}
		if (selectedTab == 1) {
			int contentLeft = sidebarRight + 32;
			for (int index = 0; index < moduleNames.length; index++) {
				int cardLeft = contentLeft + (index % 2) * 236;
				int cardTop = top + 92 + (index / 2) * 84;
				if (mouseX >= cardLeft && mouseX <= cardLeft + 220 && mouseY >= cardTop && mouseY <= cardTop + 68) {
					ByteClientModules.setEnabled(index, !ByteClientModules.isEnabled(index));
					return true;
				}
			}
		} else if (selectedTab == 0) {
			int contentLeft = sidebarRight + 32;
			int panelTop = top + 184;
			for (int index = 0; index < 3; index++) {
				int rowTop = panelTop + 43 + index * 32;
				if (mouseX >= contentLeft && mouseX <= contentLeft + 280 && mouseY >= rowTop - 8 && mouseY <= rowTop + 18) {
					ByteClientModules.setEnabled(index, !ByteClientModules.isEnabled(index));
					return true;
				}
			}
		}
		return super.mouseClicked(event, bl);
	}

	@Override
	public void mouseMoved(double mouseX, double mouseY) {
		hoveredModule = -1;
		int left = Math.max(18, (this.width - 920) / 2);
		int top = Math.max(18, (this.height - 560) / 2);
		int sidebarRight = left + 172;
		if (selectedTab == 1) {
			int contentLeft = sidebarRight + 32;
			for (int index = 0; index < moduleNames.length; index++) {
				int cardLeft = contentLeft + (index % 2) * 236;
				int cardTop = top + 92 + (index / 2) * 84;
				if (mouseX >= cardLeft && mouseX <= cardLeft + 220 && mouseY >= cardTop && mouseY <= cardTop + 68) {
					hoveredModule = index;
				}
			}
		}
	}

	@Override
	public boolean isPauseScreen() {
		return false;
	}
}