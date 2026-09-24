package mod.timberfall.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.Gson;

import org.junit.jupiter.api.Test;

class ConfigManagerRenderTest {

	@Test
	void renderContainsEverySettingWithACommentAbove() {
		String rendered = ConfigManager.render(new Config());

		assertTrue(rendered.startsWith("// timberfall schema: " + ConfigManager.SCHEMA_VERSION + "\n{\n"));
		assertTrue(rendered.trim().endsWith("}"));

		for (java.lang.reflect.Field field : Config.class.getDeclaredFields()) {
			if (java.lang.reflect.Modifier.isStatic(field.getModifiers())) {
				continue;
			}
			assertTrue(rendered.contains("\"" + field.getName() + "\":"),
					"Missing rendered field: " + field.getName());
		}

		assertTrue(rendered.contains("// Master switch for the entire mod."));
		assertTrue(rendered.contains("// Break logs one by one, rippling out from the chopped block. Disable to remove the tree in per-tick batches instead."));
	}

	@Test
	void renderedOutputRoundTripsThroughJson5BackToConfig() {
		Config source = new Config();
		source.minLogsToChop = 7;
		source.breakSpeedFactor = 0.25f;
		source.applyInCreative = false;

		String rendered = ConfigManager.render(source);
		String json = Json5.toJson(rendered);

		Config parsed = new Gson().fromJson(json, Config.class);
		assertEquals(7, parsed.minLogsToChop);
		assertEquals(0.25f, parsed.breakSpeedFactor);
		assertEquals(false, parsed.applyInCreative);
		assertEquals(source.enabled, parsed.enabled);
	}

	@Test
	void detectsSchemaVersionInHeaderComment() {
		// Any file without a header is treated as schema 1 (first legacy layout).
		assertEquals(1, ConfigManager.readSchemaVersion("{\n  \"enabled\": true\n}\n"));
		assertEquals(1, ConfigManager.readSchemaVersion("// timberfall schema: nope\n{\n"));

		// The current schema must be read back exactly.
		assertEquals(ConfigManager.SCHEMA_VERSION,
				ConfigManager.readSchemaVersion("// timberfall schema: " + ConfigManager.SCHEMA_VERSION + "\n{\n"));

		// A schema newer than the mod knows must be detected so it is never rewritten.
		assertEquals(7, ConfigManager.readSchemaVersion("// timberfall schema: 7\n{\n"));
		assertNotEquals(ConfigManager.SCHEMA_VERSION, 7);
	}
}