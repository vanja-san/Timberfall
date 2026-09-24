package mod.timberfall.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.Gson;

import org.junit.jupiter.api.Test;

class ConfigManagerRenderTest {

	@Test
	void renderContainsEverySettingWithACommentAbove() {
		String rendered = ConfigManager.render(new Config());

		assertTrue(rendered.startsWith("{\n"));
		assertTrue(rendered.trim().endsWith("}"));

		for (java.lang.reflect.Field field : Config.class.getDeclaredFields()) {
			if (java.lang.reflect.Modifier.isStatic(field.getModifiers())) {
				continue;
			}
			assertTrue(rendered.contains("\"" + field.getName() + "\":"),
					"Missing rendered field: " + field.getName());
		}

		assertTrue(rendered.contains("// Master switch for the entire mod."));
		assertTrue(rendered.contains("// Logs removed every server tick while a chop is in progress."));
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
}