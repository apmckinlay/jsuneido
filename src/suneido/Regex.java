package suneido;

import java.util.regex.Pattern;

public class Regex {
	private static LruCache<String, Pattern> cache = 
		new LruCache<String, Pattern>(32);

	public static boolean contains(String s, String rx) {
		return getPat(rx).matcher(s).find();
	}

	/* package */static Pattern getPat(String rx) {
		Pattern p = cache.get(rx);
		if (p == null)
			cache.put(rx, p = Pattern.compile(convertRegex(rx)));
		return p;
	}

	/**
	 * Convert from Suneido's regular expression syntax to Java's.
	 */
	private static String convertRegex(String rx) {
		return rx.replace("(?q)", "\\Q")
				.replace("(?-q)", "\\E")
				.replace("\\<", "\\b") // TODO improve
				.replace("\\>", "\\b") // TODO improve
				.replace("[:alnum:]", "\\p{Alnum}")
				.replace("[:alpha:]", "\\p{Alpha}")
				.replace("[:blank:]", "\\p{Blank}")
				.replace("[:cntrl:]", "\\p{Cntrl}")
				.replace("[:digit:]", "\\p{Digit}")
				.replace("[:graph:]", "\\p{Graph}")
				.replace("[:lower:]", "\\p{Lower}")
				.replace("[:print:]", "\\p{Print}")
				.replace("[:punct:]", "\\p{Punct}")
				.replace("[:space:]", "\\p{Space}")
				.replace("[:upper:]", "\\p{Upper}")
				.replace("[:xdigit:]", "\\p{XDigit}");
	}
}
