package suneido.language.jsdi.tools;

import java.util.ArrayList;

abstract class LineEditor {
	public final String matchToken;
	protected final ArrayList<String> lines;

	public LineEditor(String matchToken) {
		this.matchToken = matchToken;
		this.lines = new ArrayList<String>();
	}

	public abstract ArrayList<String> makeLines();

	public static StringBuilder indent(int count) {
		StringBuilder result = new StringBuilder(4 * count);
		while (0 < count) {
			result.append("    ");
			--count;
		}
		return result;
	}

	public static StringBuilder quote(CharSequence str) {
		StringBuilder result = new StringBuilder(str.length() + 2);
		return result.append('"').append(str).append('"');
	}

	protected final void add(String line) {
		lines.add(line);
	}

	protected final void add(CharSequence line) {
		lines.add(line.toString());
	}
}