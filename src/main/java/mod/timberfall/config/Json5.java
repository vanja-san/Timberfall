package mod.timberfall.config;

/**
 * Minimal JSON5-to-JSON preprocessor.
 *
 * <p>Only the parts of JSON5 that are comfortable to hand-write are supported:
 * line and block comments, trailing commas, single-quoted strings and unquoted
 * keys. Everything else (numbers, escapes, nesting) is passed through
 * unchanged and parsed by Gson afterwards.
 */
final class Json5 {

	private Json5() {
	}

	/** Returns a plain JSON representation of the given JSON5 text. */
	static String toJson(String source) {
		StringBuilder out = new StringBuilder(source.length());
		int i = 0;
		int length = source.length();

		while (i < length) {
			char c = source.charAt(i);

			if (c == '"' || c == '\'') {
				i = copyString(source, i, out);
				continue;
			}

			if (c == '/' && i + 1 < length && source.charAt(i + 1) == '/') {
				i += 2;
				while (i < length && source.charAt(i) != '\n') {
					i++;
				}
				continue;
			}

			if (c == '/' && i + 1 < length && source.charAt(i + 1) == '*') {
				i += 2;
				while (i + 1 < length && !(source.charAt(i) == '*' && source.charAt(i + 1) == '/')) {
					i++;
				}
				i = Math.min(i + 2, length);
				continue;
			}

			if (c == ',') {
				int next = skipInsignificant(source, i + 1);
				if (next < length && (source.charAt(next) == '}' || source.charAt(next) == ']')) {
					i++;
					continue;
				}
			}

			if (isKeyStart(c)) {
				int end = i;
				while (end < length && isKeyPart(source.charAt(end))) {
					end++;
				}
				int colon = skipInsignificant(source, end);
				if (colon < length && source.charAt(colon) == ':') {
					out.append('"').append(source, i, end).append('"');
					i = end;
					continue;
				}
			}

			out.append(c);
			i++;
		}

		return out.toString();
	}

	/** Copies a single- or double-quoted string, normalising it to JSON. */
	private static int copyString(String source, int start, StringBuilder out) {
		char quote = source.charAt(start);
		int i = start + 1;
		int length = source.length();
		out.append('"');

		while (i < length) {
			char c = source.charAt(i);

			if (c == '\\') {
				out.append(c);
				if (i + 1 < length) {
					out.append(source.charAt(i + 1));
				}
				i += 2;
				continue;
			}

			if (c == quote) {
				out.append('"');
				return i + 1;
			}

			if (c == '"') {
				out.append("\\\"");
			} else {
				out.append(c);
			}
			i++;
		}

		out.append('"');
		return i;
	}

	/** Skips whitespace and comments, returning the next significant index. */
	private static int skipInsignificant(String source, int i) {
		int length = source.length();

		while (i < length) {
			char c = source.charAt(i);

			if (Character.isWhitespace(c)) {
				i++;
				continue;
			}

			if (c == '/' && i + 1 < length && source.charAt(i + 1) == '/') {
				i += 2;
				while (i < length && source.charAt(i) != '\n') {
					i++;
				}
				continue;
			}

			if (c == '/' && i + 1 < length && source.charAt(i + 1) == '*') {
				i += 2;
				while (i + 1 < length && !(source.charAt(i) == '*' && source.charAt(i + 1) == '/')) {
					i++;
				}
				i = Math.min(i + 2, length);
				continue;
			}

			break;
		}

		return i;
	}

	private static boolean isKeyStart(char c) {
		return Character.isLetter(c) || c == '_' || c == '$';
	}

	private static boolean isKeyPart(char c) {
		return Character.isLetterOrDigit(c) || c == '_' || c == '$';
	}
}
