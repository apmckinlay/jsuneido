package suneido.database.query;

import suneido.database.Index;

public class StringRequestGenerator implements RequestGenerator<String> {

	public String drop(String name) {
		return "drop(" + name + ")";
	}

	public String rename(String from, String to) {
		return "rename(" + from + ", " + to + ")";
	}

	public String view(String name, String definition) {
		return "view(" + name + ", '" + definition + "')";
	}

	public String sview(String name, String definition) {
		return "s" + view(name, definition);
	}

	public String columns(String columns, String column) {
		return str("", columns, ",") + column;
	}

	public String create(String table, String schema) {
		return "create " + table + " " + schema;
	}

	public String ensure(String table, String schema) {
		return "ensure " + table + " " + schema;
	}

	public String foreignKey(String table, String columns, int mode) {
		return "in " + table + str(" (", columns, ")") +
				(mode != Index.BLOCK ? " cascade" : "") + (mode == Index.CASCADE_UPDATES ? " update" : "");
	}

	public String index(boolean key, boolean unique, boolean lower,
			String columns, String foreignKey) {
		return (key ? "key" : "index") +
				(unique ? " unique" : "") + (lower ? " lower" : "") +
				"(" + columns + ")" + str(" ", foreignKey, "");
	}

	public String indexes(String indexes, String index) {
		return str("", indexes, " ") + index;
	}

	public String schema(String columns, String indexes) {
		return str("(", columns, ")") +
				(columns != null && indexes != null ? " " : "") + str(indexes);
	}

	public String alterCreate(String table, String schema) {
		return "alter " + table + " create " + schema;
	}

	public String alterDrop(String table, String schema) {
		return "alter " + table + " drop " + schema;
	}

	public String alterRename(String table, String renames) {
		return "alter " + table + " rename " + renames;
	}

	public String renames(String renames, String from, String to) {
		return str("", renames, ", ") + from + " to " + to;
	}

	private String str(String x) {
		return x == null ? "" : (String) x;
	}

	private String str(String s, String x, String t) {
		return x == null ? "" : s + x + t;
	}

}
