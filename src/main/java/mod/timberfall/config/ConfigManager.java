package mod.timberfall.config;

import java.io.IOException;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import mod.timberfall.Mod;
import net.fabricmc.loader.api.FabricLoader;

/**
 * Loads and persists the JSON5 configuration file from the Fabric config
 * directory. The reader tolerates the JSON5 niceties that are handy to
 * hand-write (comments, trailing commas, single quotes and unquoted keys)
 * while the writer emits plain, compact JSON, so the file never carries any
 * generated comments or padding. A missing or corrupt file silently falls back
 * to defaults so the mod always starts.
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

		configPath = configDir.resolve(Mod.MOD_ID + ".json5");
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
		Path source = Files.exists(configPath) ? configPath : legacyPath();
		if (source != null) {
			try {
				Config fromDisk = GSON.fromJson(Json5.toJson(Files.readString(source)), Config.class);
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

	/** The pre-JSON5 file name, read once so existing settings are kept. */
	private static Path legacyPath() {
		Path legacy = configPath.resolveSibling(Mod.MOD_ID + ".json");
		return Files.exists(legacy) ? legacy : null;
	}
}
