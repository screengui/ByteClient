package com.byteclient.client;

import com.byteclient.ByteClient;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Properties;

/**
 * Shared configuration access for Byte Client.
 *
 * New versions keep all files under config/byteclient/. Legacy files are copied
 * only when the new file does not already exist, so a newer configuration is
 * never overwritten by migration.
 */
public final class ByteClientConfig {
	private static final Path LEGACY_DIRECTORY = FabricLoader.getInstance().getConfigDir();
	private static final Path CONFIG_DIRECTORY = LEGACY_DIRECTORY.resolve("byteclient");
	private static final String[] FILES = {"byte-client-module-settings.properties", "byte-client-hud.properties"};

	private ByteClientConfig() {
	}

	public static void initialize() {
		try {
			Files.createDirectories(CONFIG_DIRECTORY);
		} catch (IOException exception) {
			ByteClient.LOGGER.warn("Unable to create Byte Client configuration directory", exception);
		}
		for (String fileName : FILES) {
			migrateIfNeeded(fileName);
		}
	}

	public static Path path(String fileName) {
		return CONFIG_DIRECTORY.resolve(fileName);
	}

	public static Properties load(String fileName) {
		Properties properties = new Properties();
		Path file = path(fileName);
		if (!Files.isRegularFile(file)) {
			return properties;
		}
		try (InputStream input = Files.newInputStream(file)) {
			properties.load(input);
		} catch (IOException | IllegalArgumentException exception) {
			ByteClient.LOGGER.warn("Unable to load Byte Client configuration {}", file, exception);
		}
		return properties;
	}

	public static void save(String fileName, Properties properties, String comment) {
		Path file = path(fileName);
		Path temporary = file.resolveSibling(file.getFileName() + ".tmp");
		try {
			Files.createDirectories(CONFIG_DIRECTORY);
			try (OutputStream output = Files.newOutputStream(temporary)) {
				properties.store(output, comment);
			}
			try {
				Files.move(temporary, file, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
			} catch (AtomicMoveNotSupportedException exception) {
				Files.move(temporary, file, StandardCopyOption.REPLACE_EXISTING);
			}
		} catch (IOException exception) {
			ByteClient.LOGGER.warn("Unable to save Byte Client configuration {}", file, exception);
			try {
				Files.deleteIfExists(temporary);
			} catch (IOException cleanupException) {
				ByteClient.LOGGER.debug("Unable to clean up temporary Byte Client configuration", cleanupException);
			}
		}
	}

	private static void migrateIfNeeded(String fileName) {
		Path target = path(fileName);
		Path legacy = LEGACY_DIRECTORY.resolve(fileName);
		if (Files.exists(target) || !Files.isRegularFile(legacy)) {
			return;
		}
		try {
			Files.copy(legacy, target, StandardCopyOption.COPY_ATTRIBUTES);
			ByteClient.LOGGER.info("Migrated Byte Client configuration {}", fileName);
		} catch (IOException exception) {
			ByteClient.LOGGER.warn("Unable to migrate legacy Byte Client configuration {}", legacy, exception);
		}
	}
}