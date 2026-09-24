package mod.timberfall.config;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import org.junit.jupiter.api.Test;

class Json5Test {

	@Test
	void stripsCommentsAndNormalizesToPlainJson() {
		String json5 = """
				{
				  // line comment
				  enabled: true, /* block comment */
				  'requireAxe': false,
				  "heightLimit": 5,
				}
				""";

		String json = Json5.toJson(json5);
		JsonObject parsed = JsonParser.parseString(json).getAsJsonObject();

		assertEquals(true, parsed.get("enabled").getAsBoolean());
		assertEquals(false, parsed.get("requireAxe").getAsBoolean());
		assertEquals(5, parsed.get("heightLimit").getAsInt());
	}

	@Test
	void keepsEscapeSequencesInsideStrings() {
		String json5 = "{ \"name\": \"it\\\"s a \\\\ path\", value: 1 }";

		JsonObject parsed = JsonParser.parseString(Json5.toJson(json5)).getAsJsonObject();

		assertEquals("it\"s a \\ path", parsed.get("name").getAsString());
		assertEquals(1, parsed.get("value").getAsInt());
	}

	@Test
	void singleQuotesBecomeDoubleQuotes() {
		String json5 = "{ 'key': 'val' }";

		assertEquals("{ \"key\": \"val\" }", Json5.toJson(json5));
	}
}