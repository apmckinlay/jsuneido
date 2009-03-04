package suneido.database.query;

import suneido.language.StringGenerator;

public class StringQueryGenerator extends StringGenerator
		implements QueryGenerator<String> {

	public String columns(String columns, String column) {
		return str("", columns, ", ") + column;
	}

	public String delete(String query) {
		return "delete " + query;
	}

	public String history(String table) {
		return "history(" + table + ")";
	}

	public String insertQuery(String query, String table) {
		return "insert " + query + " into " + table;
	}

	public String insertRecord(String record, String query) {
		return "insert " + record + " into " + query;
	}

	public String sort(String query, boolean reverse, String columns) {
		return query + " sort" + (reverse ? " reverse" : "") + " " + columns;
	}

	public String table(String table) {
		return table;
	}

	public String update(String query, String updates) {
		return "update " + query + " set " + updates;
	}

	public String updates(String updates, String column, String expr) {
		return str("", updates, ", ") + column + " = " + expr;
	}

}
