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
 * A clean file is never rewritten on restart (hand-added comments and
 * formatting survive); a missing or corrupt file silently falls back to
 * defaults (the corrupt one is kept as a {@code .corrupt} backup) so the mod
 * always starts.
 */
public final class ConfigManager {

	/**
	 * Bumped whenever a mod release adds or renames a config setting. Files
	 * written by an older schema are rebuilt once on load so the new setting
	 * shows up; already-up-to-date files stay byte-for-byte untouched.
	 */
	static final int SCHEMA_VERSION = 3;

	/** Header comment carrying the schema version (parsed by {@link #readSchemaVersion}). */
	private static final String SCHEMA_MARKER = "// timberfall schema: ";

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
		out.append(SCHEMA_MARKER).append(SCHEMA_VERSION).append('\n');
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

		Path json5 = Files.exists(configPath) ? configPath : null;
		Path legacy = json5 == null ? legacyPath() : null;

		// The file is only written when the defaults must be materialised:
		// on first run, when migrating from the legacy JSON file, or after a
		// corrupt file was quarantined. A clean hand-edited JSON5 file is
		// left exactly as the user wrote it.
		boolean write = json5 == null;

Path source = json5 != null ? json5 : legacy;
		if (source != null) {
			try {
				String text = Files.readString(source);
				Config fromDisk = GSON.fromJson(Json5.toJson(text), Config.class);
				if (fromDisk != null) {
					loaded = fromDisk;
					if (json5 != null) {
						// A clean up-to-date file is kept byte-for-byte, but a
						// file from an older schema is rebuilt exactly once so
						// newly added settings surface in the config. Values
						// already chosen by the user are preserved.
						write = readSchemaVersion(text) < SCHEMA_VERSION;
					}
				}
			} catch (IOException | RuntimeException ignored) {
				// Corrupt file: keep the user's text as a backup instead of
				// silently losing it, then fall back to defaults.
				quarantine(json5);
				loaded = new Config();
				write = true;
			}
		}

		loaded.sanitize();
		config = loaded;

		if (write) {
			save();
		}
	}

	/** Reads the schema version from the header comment; legacy is 1. */
	static int readSchemaVersion(String text) {
		int idx = text.indexOf(SCHEMA_MARKER);
		if (idx < 0) {
			return 1;
		}
		String tail = text.substring(idx + SCHEMA_MARKER.length()).trim();
		int end = 0;
		while (end < tail.length() && Character.isDigit(tail.charAt(end))) {
			end++;
		}
		if (end == 0) {
			return 1;
		}
		try {
			return Integer.parseInt(tail.substring(0, end));
		} catch (NumberFormatException ignored) {
			return 1;
		}
	}

	/** Keeps a corrupt JSON5 file around as {@code <name>.json5.corrupt}. */
	private static void quarantine(Path path) {
		if (path == null) {
			return;
		}
		try {
			Files.move(path, path.resolveSibling(path.getFileName() + ".corrupt"));
		} catch (IOException ignored) {
		}
	}

	/** The pre-JSON5 file name, read once so existing settings are kept. */
	private static Path legacyPath() {
		Path legacy = configPath.resolveSibling(Mod.MOD_ID + ".json");
		return Files.exists(legacy) ? legacy : null;
	}
}
