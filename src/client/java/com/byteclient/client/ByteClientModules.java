package com.byteclient.client;

import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.CloudStatus;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.fog.FogRenderer;
import net.minecraft.server.level.ParticleStatus;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.util.Mth;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Properties;

public final class ByteClientModules {
	private static int RED = 0xFFE52B3D;
	private static final int PANEL = 0xCC10141A;
	private static final int TEXT = 0xFFF3F5F7;
	private static final int DISABLED_PANEL = 0x88343B45;
	private static final int[] HUD_MODULES = {1, 3, 4, 5, 6, 13, 14, 15};
	private static final int[] DEFAULT_HUD_X = {0, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10};
	private static final int[] DEFAULT_HUD_Y = {0, 10, 10, 46, 10, 10, 82, 10, 10, 10, 10, 10, 10, 118, 154, 190};
	private static final String[] MODULE_NAMES = {
			"FPS BOOST", "CPS DISPLAY", "FULLBRIGHT", "FPS DISPLAY",
			"ARMOR STATUS", "KEYSTROKES", "PING DISPLAY", "PERFORMANCE MODE",
			"MOTION BLUR", "SPRINT", "NO FOG", "LOW FIRE", "LOW SHIELD",
			"COORDINATES", "DIRECTION", "ITEM DURABILITY"
	};
	private static final String[] MODULE_DESCRIPTIONS = {
			"Selective frame and render optimizations", "Shows clicks per second", "Saturates the lightmap",
			"Shows current frames per second", "Shows equipped armor", "Shows movement inputs",
			"Displays current latency", "Configurable render optimization profile", "Lightweight vanilla frame blur",
			"Automatically sprints while moving", "Removes atmospheric fog", "Moves the fire overlay down",
			"Moves the shield lower in first person", "Shows current XYZ position", "Shows facing direction",
			"Shows held-item durability"
	};
	private static final boolean[] ENABLED = {
			true, true, false, true, true, false, true, false,
			false, false, false, false, false, true, false, false
	};
	private static final int[] FULLBRIGHT_MODES = {0, 1};
	private static final int[] HUD_OPACITIES = {60, 80, 100};
	private static final int[] PERFORMANCE_LEVELS = {1, 2, 3};
	private static final int[] MOTION_BLUR_LEVELS = {2, 4, 6};
private static final int[] LOW_FIRE_LEVELS = {25, 50, 75};
private static final int[] LOW_SHIELD_LEVELS = {20, 35, 50};
private static final int[] ANIMATION_STYLES = {0, 1, 2, 3, 4, 5};
private static final int[] ANIMATION_DURATIONS = {8, 12, 18};
	private static final String[] THEMES = {"Byte Red", "Ocean Blue", "Neon Lime", "Royal Purple"};
	private static final int[] THEME_ACCENTS = {0xFFE52B3D, 0xFF2D9CDB, 0xFF8BC34A, 0xFF9B6DFF};
	private static final int[] THEME_DARKS = {0xFF8D1D2B, 0xFF176087, 0xFF527A27, 0xFF60409A};
	private static final HudPositionStore HUD_POSITIONS = new HudPositionStore(
			() -> ByteClientConfig.path("byte-client-hud.properties"),
			HUD_MODULES,
			DEFAULT_HUD_X,
			DEFAULT_HUD_Y
	);
	private static final Deque<Long> CLICK_TIMES = new ArrayDeque<>();
	private static ParticleStatus originalParticleStatus;
	private static CloudStatus originalCloudStatus;
	private static boolean originalEntityShadows;
	private static boolean originalAmbientOcclusion;
	private static boolean performanceCaptured;
	private static boolean noFogApplied;
	private static int fullbrightMode;
private static final int[] HUD_OPACITIES_BY_MODULE = {
		80, 80, 80, 80, 80, 80, 80, 80,
		80, 80, 80, 80, 80, 80, 80, 80
};
	private static int performanceLevel = 3;
	private static int armorOrientation;
	private static int motionBlurRadius = 4;
	private static int themeIndex;
private static int lowFireLevel = 50;
private static int lowShieldLevel = 35;
private static int animationStyle = 1;
private static int animationDuration = 12;
	private static boolean fpsReduceParticles = true;
	private static boolean fpsDisableClouds = true;
	private static boolean fpsDisableEntityShadows = true;
	private static boolean fpsDisableAmbientOcclusion;
private static double previousCameraX;
private static double previousCameraY;
private static double previousCameraZ;
private static float previousCameraYaw;
private static float previousCameraPitch;
private static float motionBlurStrength;
private static boolean cameraSampled;
	private static final String MOTION_BLUR_RENDERER_ID = "byte-client:motion_blur";

	private ByteClientModules() {
	}

	public static void initialize() {
		ByteClientConfig.initialize();
		HUD_POSITIONS.load();
		loadModuleSettings();
		HudRenderCallback.EVENT.register((graphics, tickCounter) -> render(Minecraft.getInstance(), graphics));
	}

	public static String moduleName(int index) {
		return MODULE_NAMES[index];
	}

	public static String moduleDescription(int index) {
		return MODULE_DESCRIPTIONS[index];
	}

	public static int moduleCount() {
		return MODULE_NAMES.length;
	}

	public static boolean isEnabled(int index) {
		return ENABLED[index];
	}

	public static void setEnabled(int index, boolean enabled) {
		ENABLED[index] = enabled;
		saveModuleSettings();
	}

	public static String moduleSettingName(int index) {
		return moduleSettingName(index, 0);
	}

	public static String moduleSettingValue(int index) {
		return moduleSettingValue(index, 0);
	}

	public static void cycleModuleSetting(int index) {
		cycleModuleSetting(index, 0);
	}

	public static int moduleSettingCount(int index) {
		return switch (index) {
			case 0 -> 4;
			case 7 -> 1;
			case 4 -> 2;
			case 2 -> 1;
			case 8, 11, 12 -> 1;
			default -> isHudModule(index) ? 1 : 0;
		};
	}

	public static String moduleSettingName(int module, int setting) {
		return switch (module) {
			case 0 -> switch (setting) {
				case 0 -> "Reduce particles";
				case 1 -> "Disable clouds";
				case 2 -> "Disable shadows";
				default -> "Disable ambient occlusion";
			};
			case 2 -> "Fullbright mode";
			case 4 -> setting == 0 ? "HUD opacity" : "Armor layout";
			case 7 -> "Optimization profile";
			case 8 -> "Blur radius";
			case 11 -> "Fire reduction";
			case 12 -> "Shield reduction";
			default -> "HUD opacity";
		};
	}

	public static String moduleSettingValue(int module, int setting) {
		return switch (module) {
			case 0 -> switch (setting) {
				case 0 -> onOff(fpsReduceParticles);
				case 1 -> onOff(fpsDisableClouds);
				case 2 -> onOff(fpsDisableEntityShadows);
				default -> onOff(fpsDisableAmbientOcclusion);
			};
			case 2 -> fullbrightMode == 0 ? "Gamma" : "Night Vision";
			case 4 -> setting == 0 ? HUD_OPACITIES_BY_MODULE[module] + "%" : armorOrientation == 0 ? "Vertical" : "Horizontal";
			case 7 -> performanceProfile();
			case 8 -> motionBlurRadius + " px";
			case 11 -> lowFireLevel + "%";
			case 12 -> lowShieldLevel + "%";
			default -> isHudModule(module) ? HUD_OPACITIES_BY_MODULE[module] + "%" : "";
		};
	}

	public static void cycleModuleSetting(int module, int setting) {
		switch (module) {
			case 0 -> {
				if (setting == 0) fpsReduceParticles = !fpsReduceParticles;
				else if (setting == 1) fpsDisableClouds = !fpsDisableClouds;
				else if (setting == 2) fpsDisableEntityShadows = !fpsDisableEntityShadows;
				else fpsDisableAmbientOcclusion = !fpsDisableAmbientOcclusion;
			}
			case 2 -> fullbrightMode = nextValue(fullbrightMode, FULLBRIGHT_MODES);
			case 4 -> {
				if (setting == 0) HUD_OPACITIES_BY_MODULE[module] =
						nextValue(HUD_OPACITIES_BY_MODULE[module], HUD_OPACITIES);
				else armorOrientation = armorOrientation == 0 ? 1 : 0;
			}
			case 7 -> {
				performanceLevel = nextValue(performanceLevel, PERFORMANCE_LEVELS);
			}
			case 8 -> {
				if (setting == 0) motionBlurRadius = nextValue(motionBlurRadius, MOTION_BLUR_LEVELS);
			}
			case 11 -> lowFireLevel = nextValue(lowFireLevel, LOW_FIRE_LEVELS);
			case 12 -> lowShieldLevel = nextValue(lowShieldLevel, LOW_SHIELD_LEVELS);
			default -> {
				if (isHudModule(module)) {
					HUD_OPACITIES_BY_MODULE[module] = nextValue(HUD_OPACITIES_BY_MODULE[module], HUD_OPACITIES);
				}
			}
		}
		saveModuleSettings();
	}

	private static String onOff(boolean enabled) {
		return enabled ? "ON" : "OFF";
	}

	private static String performanceProfile() {
		return switch (performanceLevel) {
			case 1 -> "Balanced";
			case 2 -> "Aggressive";
			default -> "Maximum";
		};
	}

	public static String moduleSummary(int module) {
		if (module == 0) {
			return "Active: "
					+ (fpsReduceParticles ? "minimal particles" : "")
					+ (fpsDisableClouds ? " + clouds off" : "")
					+ (fpsDisableEntityShadows ? " + shadows off" : "")
					+ (fpsDisableAmbientOcclusion ? " + AO off" : "");
		}
		if (module == 7) {
			return "Active: " + performanceProfile() + " profile";
		}
		return MODULE_DESCRIPTIONS[module];
	}

	public static boolean isFullbrightEnabled() {
		return ENABLED[2];
	}

	public static boolean isFullbrightNightVision() {
		return ENABLED[2] && fullbrightMode == 1;
	}

	public static double fullbrightGamma() {
		return 1500.0D;
	}

	public static int currentPing(Minecraft client) {
		if (client.player == null || client.getConnection() == null) {
			return -1;
		}
		var playerInfo = client.getConnection().getPlayerInfo(client.player.getUUID());
		return playerInfo == null ? -1 : playerInfo.getLatency();
	}

	public static String themeName() {
		return THEMES[themeIndex];
	}

	public static int accentColor() {
		return THEME_ACCENTS[themeIndex];
	}

	public static int accentDarkColor() {
		return THEME_DARKS[themeIndex];
	}

	public static void cycleTheme() {
		themeIndex = (themeIndex + 1) % THEMES.length;
		saveModuleSettings();
	}

	public static boolean isMotionBlurEnabled() {
		return ENABLED[8];
	}

	public static float motionBlurRadius() {
		return motionBlurRadius;
	}

	public static boolean shouldApplyMotionBlur() {
		return ENABLED[8] && motionBlurStrength > 0.04f;
	}

	public static int activeMotionBlurRadius() {
		return Math.max(1, Math.round(motionBlurRadius * motionBlurStrength));
	}

	public static boolean shouldLowerShield(ItemStack stack) {
		return ENABLED[12] && stack.is(net.minecraft.world.item.Items.SHIELD);
	}

	public static boolean isLowFireEnabled() {
		return ENABLED[11];
	}

	public static float lowFireOffset() {
		return 0.12f + lowFireLevel / 100.0f * 0.24f;
	}

	public static float lowFireScale() {
		return 1.0f - lowFireLevel / 100.0f * 0.25f;
	}

	public static float lowShieldOffset() {
		return 0.08f + lowShieldLevel / 100.0f * 0.30f;
	}

	public static String animationStyleName() {
		return switch (animationStyle) {
			case 0 -> "None";
			case 1 -> "Fade";
			case 2 -> "Slide";
			case 3 -> "Scale";
			case 4 -> "Slide + Fade";
			default -> "Scale + Fade";
		};
	}

	public static int animationStyle() {
		return animationStyle;
	}

	public static void cycleAnimationStyle() {
		animationStyle = nextValue(animationStyle, ANIMATION_STYLES);
		saveModuleSettings();
	}

	public static String animationDurationName() {
		return switch (animationDuration) {
			case 8 -> "Fast";
			case 18 -> "Slow";
			default -> "Smooth";
		};
	}

	public static int animationDurationTicks() {
		return animationDuration;
	}

	public static void cycleAnimationDuration() {
		animationDuration = nextValue(animationDuration, ANIMATION_DURATIONS);
		saveModuleSettings();
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

	public static boolean isHudModule(int index) {
		for (int hudModule : HUD_MODULES) {
			if (hudModule == index) {
				return true;
			}
		}
		return false;
	}

	public static int hudX(int index) {
		return HUD_POSITIONS.x(index);
	}

	public static int hudX(int index, int screenWidth) {
		return HUD_POSITIONS.x(index, screenWidth, hudWidth(index));
	}

	public static int hudY(int index) {
		return HUD_POSITIONS.y(index);
	}

	public static int hudY(int index, int screenHeight) {
		return HUD_POSITIONS.y(index, screenHeight, hudHeight(index));
	}

	public static int hudWidth(int index) {
		return switch (index) {
			case 1 -> 82;
			case 3 -> 104;
			case 4 -> armorOrientation == 0 ? 28 : 116;
			case 5 -> 66;
			case 6 -> 92;
			case 13 -> 132;
			case 14 -> 104;
			case 15 -> 110;
			default -> 0;
		};
	}

	public static int hudHeight(int index) {
		return switch (index) {
			case 1, 3, 6, 13, 14, 15 -> 28;
			case 4 -> armorOrientation == 0 ? 116 : 28;
			case 5 -> 64;
			default -> 0;
		};
	}

	public static void setHudPosition(int index, int x, int y, int screenWidth, int screenHeight) {
		HUD_POSITIONS.setPosition(index, x, y, screenWidth, screenHeight, hudWidth(index), hudHeight(index));
	}

	public static void setHudPositionTransient(int index, int x, int y, int screenWidth, int screenHeight) {
		HUD_POSITIONS.setPositionTransient(index, x, y, screenWidth, screenHeight, hudWidth(index), hudHeight(index));
	}

	public static void setHudPositionSnapped(int index, int requestedX, int requestedY, int screenWidth, int screenHeight) {
		int snappedX = snapCoordinate(index, requestedX, screenWidth, true);
		int snappedY = snapCoordinate(index, requestedY, screenHeight, false);
		setHudPositionTransient(index, snappedX, snappedY, screenWidth, screenHeight);
	}

	public static int hudModuleAt(double mouseX, double mouseY, int screenWidth, int screenHeight) {
		for (int index = HUD_MODULES.length - 1; index >= 0; index--) {
			int module = HUD_MODULES[index];
			if (mouseX >= hudX(module, screenWidth) && mouseX <= hudX(module, screenWidth) + hudWidth(module)
					&& mouseY >= hudY(module, screenHeight) && mouseY <= hudY(module, screenHeight) + hudHeight(module)) {
				return module;
			}
		}
		return -1;
	}

	public static void resetHudPositions() {
		HUD_POSITIONS.reset();
	}

	public static void resetHudPositions(int screenWidth, int screenHeight) {
		HUD_POSITIONS.reset(screenWidth, screenHeight);
	}

	public static void saveHudPositions() {
		HUD_POSITIONS.save();
	}

	public static void tick(Minecraft client) {
		updatePerformanceMode(client);
		updateNoFog();
		updateMotionBlur(client);
		updateMotionBlurEffect(client);
		updateSprint(client);
	}

	private static void updateMotionBlurEffect(Minecraft client) {
		var renderer = client.gameRenderer;
		boolean shouldRun = ENABLED[8] && client.level != null && client.screen == null;
		boolean active = MOTION_BLUR_RENDERER_ID.equals(String.valueOf(renderer.currentPostEffect()));
		if (shouldRun && !active) {
			ByteClientGameRendererBridge.setPostEffect(renderer, MOTION_BLUR_RENDERER_ID);
		} else if (!shouldRun && active) {
			renderer.clearPostEffect();
		}
	}

	private static void updatePerformanceMode(Minecraft client) {
		boolean optimize = ENABLED[0] || ENABLED[7];
		if (optimize && !performanceCaptured) {
			originalParticleStatus = client.options.particles().get();
			originalCloudStatus = client.options.cloudStatus().get();
			originalEntityShadows = client.options.entityShadows().get();
			originalAmbientOcclusion = client.options.ambientOcclusion().get();
			performanceCaptured = true;
		}
		if (!performanceCaptured) {
			return;
		}
		if (optimize) {
			boolean reduceParticles = optimizationEnabled(0);
			boolean disableClouds = optimizationEnabled(1);
			boolean disableShadows = optimizationEnabled(2);
			boolean disableAo = optimizationEnabled(3);
			if (reduceParticles && client.options.particles().get() != ParticleStatus.MINIMAL) {
				client.options.particles().set(ParticleStatus.MINIMAL);
			}
			if (disableClouds && client.options.cloudStatus().get() != CloudStatus.OFF) {
				client.options.cloudStatus().set(CloudStatus.OFF);
			}
			if (disableShadows && client.options.entityShadows().get()) {
				client.options.entityShadows().set(false);
			}
			if (disableAo && client.options.ambientOcclusion().get()) {
				client.options.ambientOcclusion().set(false);
			}
		} else {
			if (client.options.particles().get() != originalParticleStatus) {
				client.options.particles().set(originalParticleStatus);
			}
			if (client.options.cloudStatus().get() != originalCloudStatus) {
				client.options.cloudStatus().set(originalCloudStatus);
			}
			if (client.options.entityShadows().get() != originalEntityShadows) {
				client.options.entityShadows().set(originalEntityShadows);
			}
			if (client.options.ambientOcclusion().get() != originalAmbientOcclusion) {
				client.options.ambientOcclusion().set(originalAmbientOcclusion);
			}
			performanceCaptured = false;
		}
	}

	private static void updateNoFog() {
		if (ENABLED[10] != noFogApplied) {
			FogRenderer.toggleFog();
			noFogApplied = ENABLED[10];
		}
	}

	private static void updateMotionBlur(Minecraft client) {
		if (!ENABLED[8] || client.player == null || client.screen != null) {
			motionBlurStrength = 0.0f;
			cameraSampled = false;
			return;
		}
		double x = client.player.getX();
		double y = client.player.getY() + client.player.getEyeHeight();
		double z = client.player.getZ();
		float yaw = client.player.getYRot();
		float pitch = client.player.getXRot();
		if (cameraSampled) {
			double distance = Math.sqrt(
					(x - previousCameraX) * (x - previousCameraX)
							+ (y - previousCameraY) * (y - previousCameraY)
							+ (z - previousCameraZ) * (z - previousCameraZ));
			float rotation = Math.abs(Mth.wrapDegrees(yaw - previousCameraYaw))
					+ Math.abs(Mth.wrapDegrees(pitch - previousCameraPitch));
			float movement = (float) Math.min(1.0, distance * 5.0 + rotation / 22.0);
			motionBlurStrength = Math.max(movement, motionBlurStrength * 0.72f);
		} else {
			cameraSampled = true;
		}
		previousCameraX = x;
		previousCameraY = y;
		previousCameraZ = z;
		previousCameraYaw = yaw;
		previousCameraPitch = pitch;
	}

	private static boolean optimizationEnabled(int optimization) {
		boolean fpsOptimization = ENABLED[0] && switch (optimization) {
				case 0 -> fpsReduceParticles;
				case 1 -> fpsDisableClouds;
				case 2 -> fpsDisableEntityShadows;
				default -> fpsDisableAmbientOcclusion;
			};
		boolean performanceOptimization = ENABLED[7] && switch (performanceLevel) {
			case 1 -> optimization == 0;
			case 2 -> optimization <= 2;
			default -> true;
		};
		return fpsOptimization || performanceOptimization;
	}

	private static void updateSprint(Minecraft client) {
		if (!ENABLED[9] || client.player == null || client.screen != null) return;
		if (client.player.input.hasForwardImpulse() && !client.player.isUsingItem()
				&& !client.player.isPassenger() && client.player.getFoodData().getFoodLevel() > 6) {
			client.player.setSprinting(true);
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
		RED = accentColor();
		int screenWidth = graphics.guiWidth();
		int screenHeight = graphics.guiHeight();
		HUD_POSITIONS.ensureReferenceSize(screenWidth, screenHeight);
		for (int module : HUD_MODULES) {
			if (ENABLED[module]) {
				renderHudComponent(client, graphics, module, hudX(module, screenWidth), hudY(module, screenHeight), screenWidth, false);
			}
		}
	}

	public static void renderHudEditor(Minecraft client, GuiGraphics graphics) {
		RED = accentColor();
		int screenWidth = graphics.guiWidth();
		int screenHeight = graphics.guiHeight();
		HUD_POSITIONS.ensureReferenceSize(screenWidth, screenHeight);
		for (int module : HUD_MODULES) {
			renderHudComponent(client, graphics, module, hudX(module, screenWidth), hudY(module, screenHeight), screenWidth, true);
		}
	}

	private static int snapCoordinate(int module, int requested, int screenDimension, boolean horizontal) {
		int elementDimension = horizontal ? hudWidth(module) : hudHeight(module);
		int clamped = clamp(requested, 0, Math.max(0, screenDimension - elementDimension));
		int best = Math.round(clamped / 4.0f) * 4;
		int bestDistance = Math.abs(best - clamped);
		int[] edgeCandidates = {
				0,
				Math.max(0, screenDimension - elementDimension),
				Math.max(0, screenDimension / 2 - elementDimension / 2)
		};
		for (int candidate : edgeCandidates) {
			if (Math.abs(candidate - clamped) < bestDistance) {
				best = candidate;
				bestDistance = Math.abs(candidate - clamped);
			}
		}
		for (int other : HUD_MODULES) {
			if (other == module) {
				continue;
			}
			int otherStart = horizontal ? hudX(other, screenDimension) : hudY(other, screenDimension);
			int otherSize = horizontal ? hudWidth(other) : hudHeight(other);
			int[] candidates = {
					otherStart,
					otherStart + otherSize - elementDimension,
					otherStart + otherSize,
					otherStart - elementDimension,
					otherStart + (otherSize - elementDimension) / 2
			};
			for (int candidate : candidates) {
				candidate = clamp(candidate, 0, Math.max(0, screenDimension - elementDimension));
				if (Math.abs(candidate - clamped) < bestDistance) {
					best = candidate;
					bestDistance = Math.abs(candidate - clamped);
				}
			}
		}
		return best;
	}

	private static int clamp(int value, int min, int max) {
		return Math.max(min, Math.min(max, value));
	}

	private static void renderHudComponent(Minecraft client, GuiGraphics graphics, int module, int left, int top,
			int screenWidth, boolean editor) {
		int panelColor = editor && !ENABLED[module] ? DISABLED_PANEL : hudPanelColor(module);
		boolean rightAligned = left + hudWidth(module) / 2 > screenWidth / 2;
		switch (module) {
			case 1 -> {
				drawPanel(graphics, left, top, 82, 28, panelColor);
				drawHudText(client, graphics, "CPS  " + clicksPerSecond(), left, top, 82, rightAligned);
			}
			case 3 -> {
				drawPanel(graphics, left, top, 104, 28, panelColor);
				drawHudText(client, graphics, "FPS  " + client.getFps(), left, top, 104, rightAligned);
			}
			case 4 -> renderArmor(client, graphics, left, top, panelColor, rightAligned);
			case 5 -> renderKeys(client, graphics, left, top, panelColor, rightAligned);
			case 6 -> {
				drawPanel(graphics, left, top, 92, 28, panelColor);
				int ping = currentPing(client);
				drawHudText(client, graphics, "PING  " + (ping < 0 ? "--" : ping), left, top, 92, rightAligned);
			}
			case 13 -> {
				drawPanel(graphics, left, top, 132, 28, panelColor);
				if (client.player != null) {
					int x = Mth.floor(client.player.getX());
					int y = Mth.floor(client.player.getY());
					int z = Mth.floor(client.player.getZ());
					drawHudText(client, graphics, "XYZ  " + x + " " + y + " " + z, left, top, 132, rightAligned);
				}
			}
			case 14 -> {
				drawPanel(graphics, left, top, 104, 28, panelColor);
				if (client.player != null) {
					drawHudText(client, graphics, direction(client.player.getYRot()), left, top, 104, rightAligned);
				}
			}
			case 15 -> {
				drawPanel(graphics, left, top, 110, 28, panelColor);
				if (client.player != null) {
					ItemStack held = client.player.getMainHandItem();
					String durability = held.isEmpty() || !held.isDamageableItem()
							? "DUR  --" : "DUR  " + (held.getMaxDamage() - held.getDamageValue());
					drawHudText(client, graphics, durability, left, top, 110, rightAligned);
				}
			}
			default -> {
			}
		}
	}

	private static void renderArmor(Minecraft client, GuiGraphics graphics, int left, int top, int panelColor, boolean rightAligned) {
		boolean horizontal = armorOrientation == 1;
		int width = horizontal ? 116 : 28;
		int height = horizontal ? 28 : 116;
		drawPanel(graphics, left, top, width, height, panelColor);
		if (client.player == null) {
			return;
		}
		EquipmentSlot[] slots = {EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET};
		for (int index = 0; index < slots.length; index++) {
			ItemStack stack = client.player.getItemBySlot(slots[index]);
			int itemX = horizontal ? left + 2 + index * 28 : rightAligned ? left + 8 : left + 2;
			int itemY = horizontal ? top + 2 : top + 2 + index * 28;
			graphics.renderItem(stack, itemX, itemY);
			if (!stack.isEmpty()) {
				graphics.renderItemDecorations(client.font, stack, itemX, itemY);
			}
		}
	}

	private static String direction(float yaw) {
		String[] directions = {"S", "SW", "W", "NW", "N", "NE", "E", "SE"};
		int index = Mth.floor((Mth.wrapDegrees(yaw) + 22.5f) / 45.0f) & 7;
		return "DIR  " + directions[index];
	}

	private static void renderKeys(Minecraft client, GuiGraphics graphics, int left, int top, int panelColor, boolean rightAligned) {
		String[] labels = {"W", "A", "S", "D"};
		boolean[] pressed = {
				client.options.keyUp.isDown(), client.options.keyLeft.isDown(),
				client.options.keyDown.isDown(), client.options.keyRight.isDown()
		};
		int[] positions = rightAligned
				? new int[]{left + 24, left + 46, left + 24, left + 2}
				: new int[]{left + 22, left, left + 22, left + 44};
		int[] rows = {top, top + 22, top + 22, top + 22};
		for (int index = 0; index < labels.length; index++) {
			int color = pressed[index] ? RED : panelColor;
			graphics.fill(positions[index], rows[index], positions[index] + 20, rows[index] + 20, color);
			graphics.drawString(client.font, labels[index], positions[index] + 7, rows[index] + 6, TEXT, true);
		}
	}

	private static void drawHudText(Minecraft client, GuiGraphics graphics, String text, int left, int top,
			int width, boolean rightAligned) {
		int textX = rightAligned ? left + width - 12 - client.font.width(text) : left + 12;
		graphics.drawString(client.font, text, textX, top + 10, TEXT, true);
	}

	private static void drawPanel(GuiGraphics graphics, int left, int top, int width, int height, int panelColor) {
		graphics.fill(left, top, left + width, top + height, panelColor);
		boolean rightAligned = left + width / 2 > graphics.guiWidth() / 2;
		if (rightAligned) {
			graphics.fill(left + width - 2, top, left + width, top + height, RED);
		} else {
			graphics.fill(left, top, left + 2, top + height, RED);
		}
	}

	private static int hudPanelColor(int module) {
		return (HUD_OPACITIES_BY_MODULE[module] << 24) | (PANEL & 0x00FFFFFF);
	}

	private static int nextValue(int current, int[] values) {
		for (int index = 0; index < values.length; index++) {
			if (values[index] == current) {
				return values[(index + 1) % values.length];
			}
		}
		return values[0];
	}

	private static void loadModuleSettings() {
		Properties settings = ByteClientConfig.load("byte-client-module-settings.properties");
		for (int module = 0; module < ENABLED.length; module++) {
			ENABLED[module] = validBoolean(settings.getProperty("module." + module + ".enabled"), ENABLED[module]);
		}
		fullbrightMode = validValue(settings.getProperty("fullbrightMode"), FULLBRIGHT_MODES, fullbrightMode);
		int sharedOpacity = validValue(settings.getProperty("hudOpacity"), HUD_OPACITIES, 80);
		for (int module : HUD_MODULES) {
			HUD_OPACITIES_BY_MODULE[module] = validValue(
					settings.getProperty("hudOpacity." + module), HUD_OPACITIES, sharedOpacity);
		}
		performanceLevel = validValue(settings.getProperty("performanceLevel"), PERFORMANCE_LEVELS, performanceLevel);
		armorOrientation = validValue(settings.getProperty("armorOrientation"), new int[]{0, 1}, armorOrientation);
		motionBlurRadius = validValue(settings.getProperty("motionBlurRadius"), MOTION_BLUR_LEVELS, motionBlurRadius);
		themeIndex = validValue(settings.getProperty("themeIndex"), new int[]{0, 1, 2, 3}, themeIndex);
		lowFireLevel = validValue(settings.getProperty("lowFireLevel"), LOW_FIRE_LEVELS, lowFireLevel);
		lowShieldLevel = validValue(settings.getProperty("lowShieldLevel"), LOW_SHIELD_LEVELS, lowShieldLevel);
		animationStyle = validValue(settings.getProperty("animationStyle"), ANIMATION_STYLES, animationStyle);
		animationDuration = validValue(settings.getProperty("animationDuration"), ANIMATION_DURATIONS, animationDuration);
		fpsReduceParticles = validBoolean(settings.getProperty("fpsReduceParticles"), fpsReduceParticles);
		fpsDisableClouds = validBoolean(settings.getProperty("fpsDisableClouds"), fpsDisableClouds);
		fpsDisableEntityShadows = validBoolean(settings.getProperty("fpsDisableEntityShadows"), fpsDisableEntityShadows);
		fpsDisableAmbientOcclusion = validBoolean(settings.getProperty("fpsDisableAmbientOcclusion"), fpsDisableAmbientOcclusion);
	}

	private static int validValue(String value, int[] allowed, int fallback) {
		if (value == null) {
			return fallback;
		}
		try {
			int parsed = Integer.parseInt(value.trim());
			for (int candidate : allowed) {
				if (candidate == parsed) {
					return parsed;
				}
			}
		} catch (NumberFormatException ignored) {
			// Use the default for malformed settings.
		}
		return fallback;
	}

	private static boolean validBoolean(String value, boolean fallback) {
		return value == null ? fallback : Boolean.parseBoolean(value.trim());
	}

	private static void saveModuleSettings() {
		Properties settings = new Properties();
		for (int module = 0; module < ENABLED.length; module++) {
			settings.setProperty("module." + module + ".enabled", Boolean.toString(ENABLED[module]));
		}
		settings.setProperty("fullbrightMode", Integer.toString(fullbrightMode));
		settings.setProperty("hudOpacity", Integer.toString(HUD_OPACITIES_BY_MODULE[HUD_MODULES[0]]));
		for (int module : HUD_MODULES) {
			settings.setProperty("hudOpacity." + module, Integer.toString(HUD_OPACITIES_BY_MODULE[module]));
		}
		settings.setProperty("performanceLevel", Integer.toString(performanceLevel));
		settings.setProperty("armorOrientation", Integer.toString(armorOrientation));
		settings.setProperty("motionBlurRadius", Integer.toString(motionBlurRadius));
		settings.setProperty("themeIndex", Integer.toString(themeIndex));
		settings.setProperty("lowFireLevel", Integer.toString(lowFireLevel));
		settings.setProperty("lowShieldLevel", Integer.toString(lowShieldLevel));
		settings.setProperty("animationStyle", Integer.toString(animationStyle));
		settings.setProperty("animationDuration", Integer.toString(animationDuration));
		settings.setProperty("fpsReduceParticles", Boolean.toString(fpsReduceParticles));
		settings.setProperty("fpsDisableClouds", Boolean.toString(fpsDisableClouds));
		settings.setProperty("fpsDisableEntityShadows", Boolean.toString(fpsDisableEntityShadows));
		settings.setProperty("fpsDisableAmbientOcclusion", Boolean.toString(fpsDisableAmbientOcclusion));
		ByteClientConfig.save("byte-client-module-settings.properties", settings, "Byte Client module settings");
	}

}