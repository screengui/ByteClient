package com.byteclient.client;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.assertEquals;

class HudPositionStoreTest {
	private static final int[] HUD_MODULES = {1, 3, 4, 5, 6};
	private static final int[] DEFAULT_X = {0, 10, 10, 10, 10, 10, 10};
	private static final int[] DEFAULT_Y = {0, 10, 10, 46, 10, 10, 82};

	@TempDir
	Path tempDirectory;

	@Test
	void loadsValidCoordinatesFromConfig() throws IOException {
		Path configPath = configPath();
		Properties positions = new Properties();
		positions.setProperty("module.1.x", "125");
		positions.setProperty("module.1.y", "240");
		positions.setProperty("module.3.x", "0");
		positions.setProperty("module.3.y", "1000000");
		writeProperties(configPath, positions);

		HudPositionStore store = newStore(configPath);
		store.load();

		assertEquals(125, store.x(1));
		assertEquals(240, store.y(1));
		assertEquals(0, store.x(3));
		assertEquals(1_000_000, store.y(3));
	}

	@Test
	void invalidOrMissingCoordinatesUseTheirCorrespondingDefaults() throws IOException {
		Path configPath = configPath();
		Properties positions = new Properties();
		positions.setProperty("module.1.x", "not-a-number");
		positions.setProperty("module.3.x", "-1");
		positions.setProperty("module.3.y", "46");
		positions.setProperty("module.4.x", "1000001");
		positions.setProperty("module.5.x", "55");
		positions.setProperty("module.5.y", " ");
		positions.setProperty("module.6.x", "123");
		positions.setProperty("module.6.y", "456");
		writeProperties(configPath, positions);

		HudPositionStore store = newStore(configPath);
		store.load();

		assertEquals(DEFAULT_X[1], store.x(1));
		assertEquals(DEFAULT_Y[1], store.y(1));
		assertEquals(DEFAULT_X[3], store.x(3));
		assertEquals(46, store.y(3));
		assertEquals(DEFAULT_X[4], store.x(4));
		assertEquals(DEFAULT_Y[4], store.y(4));
		assertEquals(55, store.x(5));
		assertEquals(DEFAULT_Y[5], store.y(5));
		assertEquals(123, store.x(6));
		assertEquals(456, store.y(6));
	}

	@Test
	void malformedPropertiesFileFallsBackToDefaults() throws IOException {
		Path configPath = configPath();
		Files.createDirectories(configPath.getParent());
		Files.writeString(configPath, "module.1.x=\\uZZZZ\nmodule.3.x=300\n");

		HudPositionStore store = newStore(configPath);
		store.load();

		assertEquals(DEFAULT_X[1], store.x(1));
		assertEquals(DEFAULT_Y[1], store.y(1));
		assertEquals(DEFAULT_X[3], store.x(3));
		assertEquals(DEFAULT_Y[3], store.y(3));
	}

	@Test
	void draggingSavesCoordinatesAndResetWritesAllDefaults() throws IOException {
		Path configPath = configPath();
		HudPositionStore store = newStore(configPath);

		store.setPosition(3, 120, 240, 800, 600, 104, 28);

		Properties afterDrag = readProperties(configPath);
		assertEquals("120", afterDrag.getProperty("module.3.x"));
		assertEquals("240", afterDrag.getProperty("module.3.y"));

		store.reset();

		Properties afterReset = readProperties(configPath);
		for (int module : HUD_MODULES) {
			assertEquals(Integer.toString(DEFAULT_X[module]), afterReset.getProperty("module." + module + ".x"));
			assertEquals(Integer.toString(DEFAULT_Y[module]), afterReset.getProperty("module." + module + ".y"));
		}
	}

	@Test
	void adaptsSavedCoordinatesToTheCurrentScreenSize() throws IOException {
		HudPositionStore store = newStore(configPath());

		store.setPosition(3, 120, 240, 800, 600, 104, 28);

		assertEquals(258, store.x(3, 1600, 104));
		assertEquals(366, store.y(3, 900, 28));

		store.setPositionTransient(3, 320, 180, 1600, 900, 104, 28);
		assertEquals(320, store.x(3, 1600, 104));
		assertEquals(180, store.y(3, 900, 28));
	}

	@Test
	void preservesHudEdgeAnchorsAcrossResolutionChanges() throws IOException {
		HudPositionStore store = newStore(configPath());

		store.setPosition(3, 10, 10, 800, 600, 104, 28);
		assertEquals(10, store.x(3, 1600, 104));
		assertEquals(10, store.y(3, 900, 28));

		store.setPosition(3, 686, 562, 800, 600, 104, 28);
		assertEquals(1486, store.x(3, 1600, 104));
		assertEquals(862, store.y(3, 900, 28));
	}

	private HudPositionStore newStore(Path configPath) {
		return new HudPositionStore(() -> configPath, HUD_MODULES, DEFAULT_X, DEFAULT_Y);
	}

	private Path configPath() {
		return tempDirectory.resolve("config/byte-client-hud.properties");
	}

	private static void writeProperties(Path path, Properties positions) throws IOException {
		Files.createDirectories(path.getParent());
		try (OutputStream output = Files.newOutputStream(path)) {
			positions.store(output, "test");
		}
	}

	private static Properties readProperties(Path path) throws IOException {
		Properties positions = new Properties();
		try (InputStream input = Files.newInputStream(path)) {
			positions.load(input);
		}
		return positions;
	}
}