package com.byteclient.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

public class ByteClientMenuScreen extends Screen {
	private static final int BACKGROUND = 0x660B0D11;
	private static final int PANEL = 0xE613171E;
	private static final int PANEL_HIGHLIGHT = 0xFF1A2029;
	private static final int BORDER = 0xFF252C36;
	private static final int MUTED = 0xFF8993A1;
	private static final int TEXT = 0xFFF3F5F7;
	private static int RED = 0xFFE52B3D;
	private static int RED_DARK = 0xFF8D1D2B;

	private final String[] tabs = {"Overview", "Modules", "Settings"};
	private final String[] moduleNames = {
		"FPS BOOST", "CPS DISPLAY", "FULLBRIGHT", "FPS DISPLAY",
		"ARMOR STATUS", "KEYSTROKES", "PING DISPLAY", "PERFORMANCE MODE",
		"MOTION BLUR", "SPRINT", "NO FOG", "LOW FIRE", "LOW SHIELD",
		"COORDINATES", "DIRECTION", "ITEM DURABILITY"
	};
	private final String[] moduleDescriptions = {
		"Selective frame and render optimizations", "Shows clicks per second", "Saturates the lightmap",
		"Shows current frames per second", "Shows equipped armor", "Shows movement inputs",
		"Displays current latency", "Configurable render optimization profile", "Lightweight vanilla frame blur",
		"Automatically sprints while moving", "Removes atmospheric fog", "Moves the fire overlay down",
		"Moves the shield lower in first person", "Shows current XYZ position", "Shows facing direction",
		"Shows held-item durability"
	};
	private int selectedTab;
	private int hoveredModule = -1;
	private int moduleScroll;
	private int animationTick;
	private boolean closing;


	public ByteClientMenuScreen() {
		super(Component.literal("ByteClient"));
	}

	@Override
	public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float delta) {
		RED = ByteClientModules.accentColor();
		RED_DARK = ByteClientModules.accentDarkColor();
		float animation = animationProgress(delta);
		guiGraphics.pose().pushMatrix();
		applyAnimation(guiGraphics, animation);
		guiGraphics.fill(0, 0, this.width, this.height, BACKGROUND);
		MenuLayout layout = menuLayout();
		int left = layout.left();
		int top = layout.top();
		int right = layout.right();
		int bottom = layout.bottom();
		int sidebarRight = layout.sidebarRight();

		guiGraphics.fill(left, top, right, bottom, PANEL);
		guiGraphics.fill(left, top, sidebarRight, bottom, 0xE610141A);
		guiGraphics.fill(sidebarRight, top, sidebarRight + 1, bottom, BORDER);
		guiGraphics.fill(left, top, left + 4, bottom, RED);
		guiGraphics.drawString(this.font, "BYTE", left + 24, top + 25, TEXT, true);
		guiGraphics.drawString(this.font, "CLIENT", left + 24, top + 39, RED, true);
		guiGraphics.drawString(this.font, "UTILITY CLIENT", left + 24, top + 61, MUTED, false);

		for (int index = 0; index < tabs.length; index++) {
			int tabTop = layout.tabTop(index);
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

		int contentLeft = layout.contentLeft();
		int contentRight = layout.contentRight();
		guiGraphics.drawString(this.font, tabs[selectedTab].toUpperCase(), contentLeft, top + 30, TEXT, true);
		guiGraphics.drawString(this.font, "Your setup, at a glance.", contentLeft, top + 48, MUTED, false);
		guiGraphics.fill(contentLeft, top + 68, contentRight, top + 69, BORDER);
		guiGraphics.enableScissor(left, top, right, bottom);
		if (selectedTab == 0) {
			renderOverview(guiGraphics, layout);
		} else if (selectedTab == 1) {
			renderModules(guiGraphics, layout);
		} else {
			renderSettings(guiGraphics, layout);
		}
		guiGraphics.disableScissor();

		guiGraphics.fill(contentLeft, bottom - 35, contentRight, bottom - 34, BORDER);
		guiGraphics.drawString(this.font, "BYTECLIENT  /  1.0.0", contentLeft, bottom - 23, MUTED, false);
		guiGraphics.drawString(this.font, "ONLINE", contentRight - 45, bottom - 23, RED, true);
		guiGraphics.pose().popMatrix();
		if (usesFade()) {
			int alpha = Math.max(0, Math.min(190, Math.round((1.0f - animation) * 190.0f)));
			guiGraphics.fill(0, 0, this.width, this.height, (alpha << 24) | 0x0010141A);
		}
	}

	private void renderOverview(GuiGraphics guiGraphics, MenuLayout layout) {
		int left = layout.contentLeft();
		int width = layout.contentWidth();
		int metricColumns = width >= 450 ? 3 : width >= 290 ? 2 : 1;
		int metricGap = 10;
		int metricWidth = Math.max(1, (width - metricGap * (metricColumns - 1)) / metricColumns);
		int metricsTop = layout.contentTop() + 16;
		renderMetric(guiGraphics, left, metricsTop, metricWidth, "FPS", Integer.toString(Minecraft.getInstance().getFps()), "LIVE");
		if (metricColumns > 1) {
			renderMetric(guiGraphics, left + metricWidth + metricGap, metricsTop, metricWidth, "PING",
					pingText(), "MS");
		} else {
			renderMetric(guiGraphics, left, metricsTop + 78, metricWidth, "PING", pingText(), "MS");
		}
		if (metricColumns > 2) {
			renderMetric(guiGraphics, left + (metricWidth + metricGap) * 2, metricsTop, metricWidth, "ACTIVE",
					ByteClientModules.enabledCount() + " / " + ByteClientModules.moduleCount(), "MODULES");
		} else if (metricColumns > 1) {
			renderMetric(guiGraphics, left + metricWidth + metricGap, metricsTop + 78, metricWidth, "ACTIVE",
					ByteClientModules.enabledCount() + " / " + ByteClientModules.moduleCount(), "MODULES");
		} else {
			renderMetric(guiGraphics, left, metricsTop + 156, metricWidth, "ACTIVE",
					ByteClientModules.enabledCount() + " / " + ByteClientModules.moduleCount(), "MODULES");
		}
		int panelTop = metricsTop + (metricColumns == 3 ? 92 : metricColumns == 2 ? 170 : 248);
		int panelHeight = Math.max(1, Math.min(152, layout.contentBottom() - panelTop));
		if (panelHeight < 70) {
			return;
		}
		int quickWidth = Math.max(1, Math.min(296, width));
		renderPanel(guiGraphics, left, panelTop, quickWidth, panelHeight);
		guiGraphics.drawString(this.font, "QUICK MODULES", left + 16, panelTop + 18, TEXT, true);
		for (int index = 0; index < 3; index++) {
			int rowTop = panelTop + 43 + index * 32;
			guiGraphics.drawString(this.font, moduleNames[index], left + 16, rowTop, 0xFFD7DCE2, false);
			renderToggle(guiGraphics, left + Math.max(4, quickWidth - 44), rowTop - 3, ByteClientModules.isEnabled(index));
		}
		if (width > quickWidth + 16) {
			int sessionLeft = left + quickWidth + 12;
			int sessionWidth = width - quickWidth - 12;
			renderPanel(guiGraphics, sessionLeft, panelTop, sessionWidth, panelHeight);
			guiGraphics.drawString(this.font, "SESSION", sessionLeft + 16, panelTop + 18, TEXT, true);
			guiGraphics.drawString(this.font, "Playing with ByteClient", sessionLeft + 16, panelTop + 48, 0xFFD7DCE2, false);
			guiGraphics.drawString(this.font, "Ping: " + pingText() + " ms", sessionLeft + 16, panelTop + 70, MUTED, false);
			guiGraphics.drawString(this.font, "PROFILE / PERFORMANCE", sessionLeft + 16, panelTop + 100, RED, true);
		}
	}

	private void renderModules(GuiGraphics guiGraphics, MenuLayout layout) {
		int left = layout.contentLeft();
		int viewportTop = layout.contentTop() + 8;
		int viewportBottom = layout.contentBottom();
		int viewportHeight = Math.max(1, viewportBottom - viewportTop);
		int columns = moduleColumns(layout);
		int gap = moduleGap(layout);
		int cardWidth = Math.max(1, (layout.contentWidth() - gap * (columns - 1)) / columns);
		int rowStep = 80;
		int rows = (moduleNames.length + columns - 1) / columns;
		int totalHeight = Math.max(0, rows * rowStep - 12);
		int maxScroll = Math.max(0, totalHeight - viewportHeight);
		moduleScroll = clamp(moduleScroll, 0, maxScroll);
		guiGraphics.enableScissor(left, viewportTop, layout.contentRight(), viewportBottom);
		for (int index = 0; index < moduleNames.length; index++) {
			int cardLeft = left + (index % columns) * (cardWidth + gap);
			int cardTop = viewportTop + (index / columns) * rowStep - moduleScroll;
			renderPanel(guiGraphics, cardLeft, cardTop, cardWidth, 68);
			if (hoveredModule == index) {
				guiGraphics.fill(cardLeft, cardTop, Math.min(cardLeft + 3, layout.contentRight()), cardTop + 68, RED);
			}
			guiGraphics.drawString(this.font, fittedText(moduleNames[index], Math.max(1, cardWidth - 54)),
					cardLeft + 15, cardTop + 17, TEXT, true);
			if (cardWidth >= 180) {
				guiGraphics.drawString(this.font, moduleDescriptions[index], cardLeft + 15, cardTop + 36, MUTED, false);
			}
			renderToggle(guiGraphics, Math.max(cardLeft + 4, cardLeft + cardWidth - 43), cardTop + 15,
					ByteClientModules.isEnabled(index));
		}
		guiGraphics.disableScissor();
		if (maxScroll > 0) {
			int scrollbarHeight = Math.max(18, viewportHeight * viewportHeight / totalHeight);
			int scrollbarTop = viewportTop + (viewportHeight - scrollbarHeight) * moduleScroll / maxScroll;
			guiGraphics.fill(layout.contentRight() - 4, viewportTop, layout.contentRight(), viewportBottom, BORDER);
			guiGraphics.fill(layout.contentRight() - 4, scrollbarTop, layout.contentRight(), scrollbarTop + scrollbarHeight, RED);
		}
	}

	private void renderSettings(GuiGraphics guiGraphics, MenuLayout layout) {
		int left = layout.contentLeft();
		int panelTop = layout.contentTop() + 16;
		int panelHeight = Math.max(1, Math.min(250, layout.contentBottom() - panelTop));
		renderPanel(guiGraphics, left, panelTop, layout.contentWidth(), panelHeight);
		guiGraphics.drawString(this.font, "CLIENT SETTINGS", left + 18, panelTop + 19, TEXT, true);
		renderSettingRow(guiGraphics, left + 18, panelTop + 47, "Interface scale", "ADAPTIVE", layout.contentWidth());
		renderSettingRow(guiGraphics, left + 18, panelTop + 82, "Theme", ByteClientModules.themeName(), layout.contentWidth());
		renderSettingRow(guiGraphics, left + 18, panelTop + 117, "Animation style",
				ByteClientModules.animationStyleName(), layout.contentWidth());
		renderSettingRow(guiGraphics, left + 18, panelTop + 152, "Animation speed",
				ByteClientModules.animationDurationName(), layout.contentWidth());
		int buttonTop = Math.min(panelTop + 187, layout.contentBottom() - 32);
		guiGraphics.fill(left + 18, buttonTop, Math.min(left + 172, layout.contentRight()), buttonTop + 28, RED_DARK);
		guiGraphics.fill(left + 18, buttonTop, Math.min(left + 21, layout.contentRight()), buttonTop + 28, RED);
		guiGraphics.drawString(this.font, "HUD EDITOR", left + 34, buttonTop + 10, TEXT, true);
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

	private void renderSettingRow(GuiGraphics guiGraphics, int left, int top, String label, String value, int width) {
		guiGraphics.drawString(this.font, label, left, top, 0xFFD7DCE2, false);
		int valueX = Math.max(left + 110, left + width - 110);
		guiGraphics.drawString(this.font, value, valueX, top, RED, true);
		guiGraphics.fill(left, top + 19, left + Math.max(1, width - 36), top + 20, BORDER);
	}

	@Override
	public boolean mouseClicked(MouseButtonEvent event, boolean bl) {
		if (closing) {
			return true;
		}
		double mouseX = event.x();
		double mouseY = event.y();
		MenuLayout layout = menuLayout();
		int left = layout.left();
		int top = layout.top();
		int sidebarRight = layout.sidebarRight();
		for (int index = 0; index < tabs.length; index++) {
			int tabTop = layout.tabTop(index);
			if (event.button() == 0 && mouseX >= left + 14 && mouseX <= sidebarRight - 14
					&& mouseY >= tabTop - 5 && mouseY <= tabTop + 23) {
				selectedTab = index;
				moduleScroll = 0;
				return true;
			}
		}
		if (selectedTab == 1) {
			int module = moduleAt(mouseX, mouseY, layout);
			if (module >= 0) {
				if (event.button() == 1) {
					Minecraft.getInstance().setScreen(new ByteClientModuleSettingsScreen(this, module));
				} else if (event.button() == 0) {
					ByteClientModules.setEnabled(module, !ByteClientModules.isEnabled(module));
				}
				return true;
			}
		} else if (selectedTab == 0) {
			int contentLeft = layout.contentLeft();
			int panelTop = layout.contentTop() + 16;
			for (int index = 0; index < 3; index++) {
				int rowTop = panelTop + 43 + index * 32;
				if (event.button() == 0 && mouseX >= contentLeft && mouseX <= layout.contentRight()
						&& mouseY >= rowTop - 8 && mouseY <= rowTop + 18) {
					ByteClientModules.setEnabled(index, !ByteClientModules.isEnabled(index));
					return true;
				}
			}
		} else if (selectedTab == 2) {
			int contentLeft = layout.contentLeft();
			int themeTop = layout.contentTop() + 16 + 82;
			int buttonTop = Math.min(layout.contentTop() + 16 + 152, layout.contentBottom() - 32);
			if (event.button() == 0 && mouseX >= contentLeft + 18 && mouseX <= layout.contentRight()
					&& mouseY >= themeTop - 8 && mouseY <= themeTop + 18) {
				ByteClientModules.cycleTheme();
				return true;
			}
			int animationTop = layout.contentTop() + 16 + 117;
			if (event.button() == 0 && mouseX >= contentLeft + 18 && mouseX <= layout.contentRight()
					&& mouseY >= animationTop - 8 && mouseY <= animationTop + 18) {
				ByteClientModules.cycleAnimationStyle();
				return true;
			}
			int durationTop = layout.contentTop() + 16 + 152;
			if (event.button() == 0 && mouseX >= contentLeft + 18 && mouseX <= layout.contentRight()
					&& mouseY >= durationTop - 8 && mouseY <= durationTop + 18) {
				ByteClientModules.cycleAnimationDuration();
				return true;
			}
			if (event.button() == 0 && mouseX >= contentLeft + 18 && mouseX <= Math.min(contentLeft + 172, layout.contentRight())
					&& mouseY >= buttonTop && mouseY <= buttonTop + 28) {
				Minecraft.getInstance().setScreen(new ByteClientHudEditorScreen(this));
				return true;
			}
		}
		return super.mouseClicked(event, bl);
	}

	@Override
	public void mouseMoved(double mouseX, double mouseY) {
		hoveredModule = -1;
		if (selectedTab == 1) {
			hoveredModule = moduleAt(mouseX, mouseY, menuLayout());
		}
	}

	@Override
	public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
		if (selectedTab != 1) {
			return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
		}
		MenuLayout layout = menuLayout();
		if (mouseX < layout.contentLeft() || mouseX > layout.contentRight()
				|| mouseY < layout.contentTop() + 8 || mouseY > layout.contentBottom()) {
			return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
		}
		int columns = moduleColumns(layout);
		int rows = (moduleNames.length + columns - 1) / columns;
		int totalHeight = Math.max(0, rows * 80 - 12);
		int viewportHeight = Math.max(1, layout.contentBottom() - layout.contentTop() - 8);
		int maxScroll = Math.max(0, totalHeight - viewportHeight);
		moduleScroll = clamp(moduleScroll - (int) Math.round(scrollY * 40), 0, maxScroll);
		return true;
	}

	private int moduleAt(double mouseX, double mouseY, MenuLayout layout) {
		int viewportTop = layout.contentTop() + 8;
		int viewportBottom = layout.contentBottom();
		if (mouseX < layout.contentLeft() || mouseX > layout.contentRight()
				|| mouseY < viewportTop || mouseY > viewportBottom) {
			return -1;
		}
		int columns = moduleColumns(layout);
		int gap = moduleGap(layout);
		int cardWidth = Math.max(1, (layout.contentWidth() - gap * (columns - 1)) / columns);
		int cardRow = (int) Math.floor((mouseY - viewportTop + moduleScroll) / 80.0);
		int column = (int) ((mouseX - layout.contentLeft()) / (cardWidth + gap));
		if (column < 0 || column >= columns) {
			return -1;
		}
		int cardLeft = layout.contentLeft() + column * (cardWidth + gap);
		if (mouseX > cardLeft + cardWidth) {
			return -1;
		}
		int rowTop = viewportTop + cardRow * 80 - moduleScroll;
		if (mouseY < rowTop || mouseY > rowTop + 68) {
			return -1;
		}
		int module = cardRow * columns + column;
		return module >= 0 && module < moduleNames.length ? module : -1;
	}

	private String pingText() {
		int ping = ByteClientModules.currentPing(Minecraft.getInstance());
		return ping < 0 ? "--" : Integer.toString(ping);
	}

	private int moduleColumns(MenuLayout layout) {
		return layout.contentWidth() >= 150 ? 2 : 1;
	}

	private int moduleGap(MenuLayout layout) {
		return layout.contentWidth() < 300 ? 8 : 12;
	}

	private String fittedText(String text, int maxWidth) {
		if (this.font.width(text) <= maxWidth) {
			return text;
		}
		String ellipsis = "...";
		int end = text.length();
		while (end > 1 && this.font.width(text.substring(0, end) + ellipsis) > maxWidth) {
			end--;
		}
		return end <= 1 ? ellipsis : text.substring(0, end) + ellipsis;
	}

	private MenuLayout menuLayout() {
		int horizontalMargin = Math.min(18, Math.max(8, this.width / 24));
		int verticalMargin = Math.min(18, Math.max(8, this.height / 24));
		int left = horizontalMargin;
		int top = verticalMargin;
		int right = Math.max(left + 1, this.width - horizontalMargin);
		int bottom = Math.max(top + 1, this.height - verticalMargin);
		int panelWidth = right - left;
		int sidebarWidth = Math.min(172, Math.max(112, panelWidth / 3));
		int contentGap = Math.min(32, Math.max(12, panelWidth / 20));
		int contentLeft = Math.min(right - 1, left + sidebarWidth + contentGap);
		int contentRight = Math.max(contentLeft + 1, right - contentGap);
		int contentTop = Math.min(bottom - 1, top + 76);
		int contentBottom = Math.max(contentTop + 1, bottom - 40);
		int tabGap = Math.max(26, Math.min(38, (bottom - top - 100) / 3));
		return new MenuLayout(left, top, right, bottom, left + sidebarWidth, contentLeft, contentRight,
				contentTop, contentBottom, tabGap);
	}

	private int clamp(int value, int min, int max) {
		return Math.max(min, Math.min(max, value));
	}

	@Override
	public void tick() {
		if (closing) {
			animationTick++;
			if (animationTick >= ByteClientModules.animationDurationTicks()) {
				Minecraft.getInstance().setScreen(null);
			}
		} else {
			animationTick = Math.min(ByteClientModules.animationDurationTicks(), animationTick + 1);
		}
	}

	private float animationProgress(float delta) {
		int duration = ByteClientModules.animationDurationTicks();
		if (ByteClientModules.animationStyle() == 0 || duration <= 0) {
			return closing ? 0.0f : 1.0f;
		}
		float raw = Math.min(1.0f, (animationTick + delta) / duration);
		float eased = raw * raw * (3.0f - 2.0f * raw);
		return closing ? 1.0f - eased : eased;
	}

	private boolean usesFade() {
		int style = ByteClientModules.animationStyle();
		return style == 1 || style == 4 || style == 5;
	}

	private void applyAnimation(GuiGraphics guiGraphics, float progress) {
		int style = ByteClientModules.animationStyle();
		if (style == 2 || style == 4) {
			guiGraphics.pose().translate(0.0f, (1.0f - progress) * this.height);
		}
		if (style == 3 || style == 5) {
			float scale = progress;
			guiGraphics.pose().scaleAround(scale, this.width / 2.0f, this.height / 2.0f);
		}
	}

	private record MenuLayout(int left, int top, int right, int bottom, int sidebarRight,
			int contentLeft, int contentRight, int contentTop, int contentBottom, int tabGap) {
		int contentWidth() {
			return Math.max(1, contentRight - contentLeft);
		}

		int tabTop(int index) {
			return top + 78 + index * tabGap;
		}
	}

	@Override
	public boolean isPauseScreen() {
		return false;
	}

	@Override
	public void onClose() {
		if (closing) {
			return;
		}
		if (ByteClientModules.animationStyle() == 0) {
			Minecraft.getInstance().setScreen(null);
			return;
		}
		closing = true;
		animationTick = 0;
	}
}