package com.byteclient.client;

import com.byteclient.ByteClient;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Properties;
import java.util.function.Supplier;

final class HudPositionStore {
	private static final int MAX_SAVED_POSITION = 1_000_000;
	private static final int EDGE_ANCHOR_THRESHOLD = 32;

	private final Supplier<Path> pathSupplier;
	private final int[] hudModules;
	private final int[] defaultX;
	private final int[] defaultY;
	private final int[] x;
	private final int[] y;
	private int savedScreenWidth;
	private int savedScreenHeight;

	HudPositionStore(Supplier<Path> pathSupplier, int[] hudModules, int[] defaultX, int[] defaultY) {
		if (defaultX.length != defaultY.length) {
			throw new IllegalArgumentException("HUD default coordinate arrays must have the same length");
		}
		this.pathSupplier = pathSupplier;
		this.hudModules = hudModules.clone();
		this.defaultX = defaultX.clone();
		this.defaultY = defaultY.clone();
		this.x = defaultX.clone();
		this.y = defaultY.clone();
	}

	int x(int module) {
		return x[module];
	}

	int y(int module) {
		return y[module];
	}

	int x(int module, int screenWidth, int moduleWidth) {
		return adaptiveCoordinate(x[module], savedScreenWidth, screenWidth, moduleWidth);
	}

	int y(int module, int screenHeight, int moduleHeight) {
		return adaptiveCoordinate(y[module], savedScreenHeight, screenHeight, moduleHeight);
	}

	void load() {
		System.arraycopy(defaultX, 0, x, 0, x.length);
		System.arraycopy(defaultY, 0, y, 0, y.length);
		savedScreenWidth = 0;
		savedScreenHeight = 0;

		Path path = pathSupplier.get();
		if (!Files.isRegularFile(path)) {
			return;
		}

		Properties positions = new Properties();
		try (InputStream input = Files.newInputStream(path)) {
			positions.load(input);
		} catch (IOException | IllegalArgumentException exception) {
			ByteClient.LOGGER.warn("Unable to load HUD positions; using defaults", exception);
			return;
		}

		savedScreenWidth = savedDimension(positions, "screen.width");
		savedScreenHeight = savedDimension(positions, "screen.height");
		for (int module : hudModules) {
			x[module] = savedPosition(positions, "module." + module + ".x", defaultX[module]);
			y[module] = savedPosition(positions, "module." + module + ".y", defaultY[module]);
		}
	}

	void setPosition(int module, int requestedX, int requestedY, int screenWidth, int screenHeight,
			int moduleWidth, int moduleHeight) {
		setPosition(module, requestedX, requestedY, screenWidth, screenHeight, moduleWidth, moduleHeight, true);
	}

	void setPositionTransient(int module, int requestedX, int requestedY, int screenWidth, int screenHeight,
			int moduleWidth, int moduleHeight) {
		setPosition(module, requestedX, requestedY, screenWidth, screenHeight, moduleWidth, moduleHeight, false);
	}

	private void setPosition(int module, int requestedX, int requestedY, int screenWidth, int screenHeight,
			int moduleWidth, int moduleHeight, boolean persist) {
		if (!isHudModule(module)) {
			return;
		}

		int clampedX = clamp(requestedX, 0, Math.max(0, screenWidth - moduleWidth));
		int clampedY = clamp(requestedY, 0, Math.max(0, screenHeight - moduleHeight));
		if (x[module] != clampedX || y[module] != clampedY) {
			x[module] = clampedX;
			y[module] = clampedY;
		}
		savedScreenWidth = validDimension(screenWidth) ? screenWidth : savedScreenWidth;
		savedScreenHeight = validDimension(screenHeight) ? screenHeight : savedScreenHeight;
		if (persist) {
			save();
		}
	}

	void reset() {
		System.arraycopy(defaultX, 0, x, 0, x.length);
		System.arraycopy(defaultY, 0, y, 0, y.length);
		save();
	}

	void reset(int screenWidth, int screenHeight) {
		System.arraycopy(defaultX, 0, x, 0, x.length);
		System.arraycopy(defaultY, 0, y, 0, y.length);
		savedScreenWidth = validDimension(screenWidth) ? screenWidth : savedScreenWidth;
		savedScreenHeight = validDimension(screenHeight) ? screenHeight : savedScreenHeight;
		save();
	}

	void ensureReferenceSize(int screenWidth, int screenHeight) {
		if (savedScreenWidth > 0 && savedScreenHeight > 0) {
			return;
		}
		if (validDimension(screenWidth) && validDimension(screenHeight)) {
			savedScreenWidth = screenWidth;
			savedScreenHeight = screenHeight;
			save();
		}
	}

	void save() {
		Properties positions = new Properties();
		if (savedScreenWidth > 0) {
			positions.setProperty("screen.width", Integer.toString(savedScreenWidth));
		}
		if (savedScreenHeight > 0) {
			positions.setProperty("screen.height", Integer.toString(savedScreenHeight));
		}
		for (int module : hudModules) {
			positions.setProperty("module." + module + ".x", Integer.toString(x[module]));
			positions.setProperty("module." + module + ".y", Integer.toString(y[module]));
		}

		Path path = pathSupplier.get();
		Path temporaryPath = path.resolveSibling(path.getFileName() + ".tmp");
		try {
			Path parent = path.getParent();
			if (parent != null) {
				Files.createDirectories(parent);
			}
			try (OutputStream output = Files.newOutputStream(temporaryPath)) {
				positions.store(output, "Byte Client HUD positions");
			}
			try {
				Files.move(temporaryPath, path, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
			} catch (AtomicMoveNotSupportedException exception) {
				Files.move(temporaryPath, path, StandardCopyOption.REPLACE_EXISTING);
			}
		} catch (IOException exception) {
			ByteClient.LOGGER.warn("Unable to save HUD positions", exception);
			try {
				Files.deleteIfExists(temporaryPath);
			} catch (IOException cleanupException) {
				ByteClient.LOGGER.debug("Unable to clean up temporary HUD positions file", cleanupException);
			}
		}
	}

	private boolean isHudModule(int module) {
		for (int hudModule : hudModules) {
			if (hudModule == module) {
				return true;
			}
		}
		return false;
	}

	private static int savedPosition(Properties positions, String key, int defaultPosition) {
		String value = positions.getProperty(key);
		if (value == null) {
			return defaultPosition;
		}
		try {
			int position = Integer.parseInt(value.trim());
			return position >= 0 && position <= MAX_SAVED_POSITION ? position : defaultPosition;
		} catch (NumberFormatException exception) {
			return defaultPosition;
		}
	}

	private static int savedDimension(Properties positions, String key) {
		String value = positions.getProperty(key);
		if (value == null) {
			return 0;
		}
		try {
			int dimension = Integer.parseInt(value.trim());
			return validDimension(dimension) ? dimension : 0;
		} catch (NumberFormatException exception) {
			return 0;
		}
	}

	private static int adaptiveCoordinate(int coordinate, int savedDimension, int currentDimension, int elementDimension) {
		if (savedDimension <= 0 || currentDimension <= 0) {
			return clamp(coordinate, 0, Math.max(0, currentDimension - elementDimension));
		}
		int savedAvailable = Math.max(1, savedDimension - elementDimension);
		int currentAvailable = Math.max(0, currentDimension - elementDimension);
		int rightInset = savedDimension - elementDimension - coordinate;
		if (coordinate <= EDGE_ANCHOR_THRESHOLD) {
			return clamp(coordinate, 0, currentAvailable);
		}
		if (rightInset <= EDGE_ANCHOR_THRESHOLD) {
			return clamp(currentAvailable - Math.max(0, rightInset), 0, currentAvailable);
		}
		int scaled = (int) Math.round((double) coordinate * currentAvailable / savedAvailable);
		return clamp(scaled, 0, Math.max(0, currentDimension - elementDimension));
	}

	private static boolean validDimension(int dimension) {
		return dimension > 0 && dimension <= MAX_SAVED_POSITION;
	}

	private static int clamp(int value, int min, int max) {
		return Math.max(min, Math.min(max, value));
	}
}