package com.byteclient.client;

import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayDeque;
import java.util.Deque;

public final class ByteClientModules {
	private static final int RED = 0xFFE52B3D;
	private static final int PANEL = 0xCC10141A;
	private static final int TEXT = 0xFFF3F5F7;
	private static final String[] MODULE_NAMES = {"FPS BOOST", "CPS DISPLAY", "FULLBRIGHT", "ARMOR STATUS", "KEYSTROKES", "PING DISPLAY"};
	private static final boolean[] ENABLED = {true, true, false, true, false, true};
	private static final Deque<Long> CLICK_TIMES = new ArrayDeque<>();
	private static double originalGamma;
	private static boolean gammaCaptured;
	private static int originalFramerateLimit;
	private static boolean framerateCaptured;

	private ByteClientModules() {
	}

	public static void initialize() {
		HudRenderCallback.EVENT.register((graphics, tickCounter) -> render(Minecraft.getInstance(), graphics));
	}

	public static String moduleName(int index) {
		return MODULE_NAMES[index];
	}

	public static boolean isEnabled(int index) {
		return ENABLED[index];
	}

	public static void setEnabled(int index, boolean enabled) {
		ENABLED[index] = enabled;
	}

	public static int enabledCount() {
		int count = 0;
		for (boolean enabled : ENABLED) {
			if (enabled) {
				count++;
			}
		}
		return count;
	}

	public static void tick(Minecraft client) {
		if (client.player == null) {
			return;
		}
		updateFramerateLimit(client);
		updateGamma(client);
	}

	private static void updateFramerateLimit(Minecraft client) {
		if (ENABLED[0] && !framerateCaptured) {
			originalFramerateLimit = ((Number) client.options.framerateLimit().get()).intValue();
			framerateCaptured = true;
		}
		if (framerateCaptured) {
			client.options.framerateLimit().set(ENABLED[0] ? 260 : originalFramerateLimit);
			if (!ENABLED[0]) {
				framerateCaptured = false;
			}
		}
	}

	private static void updateGamma(Minecraft client) {
		if (ENABLED[2] && !gammaCaptured) {
			originalGamma = ((Number) client.options.gamma().get()).doubleValue();
			gammaCaptured = true;
		}
		if (gammaCaptured) {
			client.options.gamma().set(ENABLED[2] ? 16.0 : originalGamma);
			if (!ENABLED[2]) {
				gammaCaptured = false;
			}
		}
	}

	public static void recordClick(int button) {
		if (button != 0 && button != 1) {
			return;
		}
		synchronized (CLICK_TIMES) {
			CLICK_TIMES.addLast(System.currentTimeMillis());
		}
	}

	private static int clicksPerSecond() {
		long cutoff = System.currentTimeMillis() - 1000L;
		synchronized (CLICK_TIMES) {
			while (!CLICK_TIMES.isEmpty() && CLICK_TIMES.peekFirst() <= cutoff) {
				CLICK_TIMES.removeFirst();
			}
			return CLICK_TIMES.size();
		}
	}

	private static void render(Minecraft client, GuiGraphics graphics) {
		if (client.player == null || client.options.hideGui) {
			return;
		}
		int right = graphics.guiWidth() - 10;
		int y = 10;
		if (ENABLED[1]) {
			drawPanel(graphics, right - 82, y, 82, 28);
			graphics.drawString(client.font, "CPS  " + clicksPerSecond(), right - 70, y + 10, TEXT, true);
			y += 36;
		}
		if (ENABLED[5]) {
			int ping = 0;
			if (client.getConnection() != null && client.getConnection().getPlayerInfo(client.player.getUUID()) != null) {
				ping = client.getConnection().getPlayerInfo(client.player.getUUID()).getLatency();
			}
			drawPanel(graphics, right - 92, y, 92, 28);
			graphics.drawString(client.font, "PING  " + ping, right - 80, y + 10, TEXT, true);
		}
		if (ENABLED[3]) {
			renderArmor(client, graphics, 10, 10);
		}
		if (ENABLED[4]) {
			renderKeys(client, graphics, 10, graphics.guiHeight() - 84);
		}
	}

	private static void renderArmor(Minecraft client, GuiGraphics graphics, int left, int top) {
		drawPanel(graphics, left, top, 28, 116);
		EquipmentSlot[] slots = {EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET};
		for (int index = 0; index < slots.length; index++) {
			ItemStack stack = client.player.getItemBySlot(slots[index]);
			graphics.renderItem(stack, left + 2, top + 2 + index * 28);
			if (!stack.isEmpty()) {
				graphics.renderItemDecorations(client.font, stack, left + 2, top + 2 + index * 28);
			}
		}
	}

	private static void renderKeys(Minecraft client, GuiGraphics graphics, int left, int top) {
		String[] labels = {"W", "A", "S", "D"};
		boolean[] pressed = {client.options.keyUp.isDown(), client.options.keyLeft.isDown(), client.options.keyDown.isDown(), client.options.keyRight.isDown()};
		int[] positions = {left + 22, left, left + 22, left + 44};
		int[] rows = {top, top + 22, top + 22, top + 22};
		for (int index = 0; index < labels.length; index++) {
			int color = pressed[index] ? RED : PANEL;
			graphics.fill(positions[index], rows[index], positions[index] + 20, rows[index] + 20, color);
			graphics.drawString(client.font, labels[index], positions[index] + 7, rows[index] + 6, TEXT, true);
		}
	}

	private static void drawPanel(GuiGraphics graphics, int left, int top, int width, int height) {
		graphics.fill(left, top, left + width, top + height, PANEL);
		graphics.fill(left, top, left + 2, top + height, RED);
	}
}
