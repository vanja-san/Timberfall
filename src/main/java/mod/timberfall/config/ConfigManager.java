package mod.timberfall.config;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import mod.timberfall.Mod;
import net.fabricmc.loader.api.FabricLoader;

/**
 * Loads and persists the JSON configuration file from the Fabric config
 * directory. A missing or corrupt file silently falls back to defaults so the
 * mod always starts.
 */
public final class ConfigManager {

	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

	private static Config config = new Config();
	private static Path configPath;

	private ConfigManager() {
	}

	/** The active configuration. Never null. */
	public static Config get() {
		return config;
	}

	/**
	 * Reads the config from disk (creating it with defaults on first run) and
	 * makes it the active instance.
	 */
	public static void initialize() {
		Path configDir = FabricLoader.getInstance().getConfigDir();
		try {
			Files.createDirectories(configDir);
		} catch (IOException ignored) {
			config = new Config();
			return;
		}

		configPath = configDir.resolve(Mod.MOD_ID + ".json");
		load();
	}

	/** Persists the current configuration back to disk. */
	public static void save() {
		if (configPath == null) {
			return;
		}
		try (Writer writer = Files.newBufferedWriter(configPath)) {
			GSON.toJson(config, writer);
		} catch (IOException ignored) {
		}
	}

	private static void load() {
		Config loaded = new Config();
		if (Files.exists(configPath)) {
			try (Reader reader = Files.newBufferedReader(configPath)) {
				Config fromDisk = GSON.fromJson(reader, Config.class);
				if (fromDisk != null) {
					loaded = fromDisk;
				}
			} catch (IOException | RuntimeException ignored) {
			}
		}

		loaded.sanitize();
		config = loaded;
		save();
	}
}