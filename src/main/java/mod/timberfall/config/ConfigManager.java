package mod.timberfall.config;

import java.io.IOException;
import java.io.Writer;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import com.google.gson.Gson;

import mod.timberfall.Mod;
import net.fabricmc.loader.api.FabricLoader;

/**
 * Loads and persists the JSON5 configuration file from the Fabric config
 * directory. The reader tolerates the JSON5 niceties that are handy to
 * hand-write (comments, trailing commas, single quotes and unquoted keys)
 * while the writer emits a compact file with one {@link Comment} description
 * above every setting, so the file stays self-documenting without any padding.
 * A missing or corrupt file silently falls back to defaults so the mod always
 * starts.
 */
public final class ConfigManager {

	private static final Gson GSON = new Gson();

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
			writer.write(render(config));
		} catch (IOException ignored) {
		}
	}

	/** Renders the config as JSON5 with a {@code //} comment above every entry. */
	static String render(Config config) {
		List<Field> entries = new ArrayList<>();
		for (Field field : Config.class.getDeclaredFields()) {
			if (!Modifier.isStatic(field.getModifiers()) && !field.isSynthetic()) {
				entries.add(field);
			}
		}

		StringBuilder out = new StringBuilder();
		out.append("{\n");

		for (int i = 0; i < entries.size(); i++) {
			Field field = entries.get(i);

			Comment comment = field.getAnnotation(Comment.class);
			if (comment != null) {
				out.append("  // ").append(comment.value()).append('\n');
			}

			Object value;
			try {
				value = field.get(config);
			} catch (IllegalAccessException ignored) {
				continue;
			}

			out.append("  \"").append(field.getName()).append("\": ").append(GSON.toJson(value));
			if (i < entries.size() - 1) {
				out.append(',');
			}
			out.append('\n');
		}

		out.append("}\n");
		return out.toString();
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
