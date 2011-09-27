package suneido.database.query;

import suneido.intfc.database.Fkmode;

public class StringRequestGenerator implements RequestGenerator<String> {

	@Override
	public String drop(String name) {
		return "drop(" + name + ")";
	}

	@Override
	public String rename(String from, String to) {
		return "rename(" + from + ", " + to + ")";
	}

	@Override
	public String view(String name, String definition) {
		return "view(" + name + ", '" + definition + "')";
	}

	@Override
	public String sview(String name, String definition) {
		return "s" + view(name, definition);
	}

	@Override
	public String columns(String columns, String column) {
		return str("", columns, ",") + column;
	}

	@Override
	public String create(String table, String schema) {
		return "create " + table + " " + schema;
	}

	@Override
	public String ensure(String table, String schema) {
		return "ensure " + table + " " + schema;
	}

	@Override
	public String foreignKey(String table, String columns, int mode) {
		return "in " + table + str(" (", columns, ")") +
				(mode != Fkmode.BLOCK ? " cascade" : "") +
				(mode == Fkmode.CASCADE_UPDATES ? " update" : "");
	}

	@Override
	public String index(boolean key, boolean unique,
			String columns, String foreignKey) {
		return (key ? "key" : "index") +
				(unique ? " unique" : "") +
				"(" + columns + ")" + str(" ", foreignKey, "");
	}

	@Override
	public String indexes(String indexes, String index) {
		return str("", indexes, " ") + index;
	}

	@Override
	public String schema(String columns, String indexes) {
		return str("(", columns, ")") +
				(columns != null && indexes != null ? " " : "") + str(indexes);
	}

	@Override
	public String alterCreate(String table, String schema) {
		return "alter " + table + " create " + schema;
	}

	@Override
	public String alterDrop(String table, String schema) {
		return "alter " + table + " drop " + schema;
	}

	@Override
	public String alterRename(String table, String renames) {
		return "alter " + table + " rename " + renames;
	}

	@Override
	public String renames(String renames, String from, String to) {
		return str("", renames, ", ") + from + " to " + to;
	}

	private static String str(String x) {
		return x == null ? "" : (String) x;
	}

	private static String str(String s, String x, String t) {
		return x == null ? "" : s + x + t;
	}

}
