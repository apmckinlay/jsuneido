package suneido.database.query;

import suneido.language.StringGenerator;
import suneido.language.Token;

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

	public String project(String query, String columns) {
		return query + " project " + columns;
	}

	public String remove(String query, String columns) {
		return query + " remove " + columns;
	}

	public String times(String query1, String query2) {
		return query1 + " times " + query2;
	}

	public String union(String query1, String query2) {
		return query1 + " union " + query2;
	}

	public String minus(String query1, String query2) {
		return query1 + " minus " + query2;
	}

	public String intersect(String query1, String query2) {
		return query1 + " intersect " + query2;
	}

	public String join(String query1, String by, String query2) {
		return query1 + " join " + str("by(", by, ") ") + query2;
	}

	public String leftjoin(String query1, String by, String query2) {
		return query1 + " leftjoin " + str("by(", by, ") ") + query2;
	}

	public String rename(String query, String renames) {
		return query + " rename " + renames;
	}

	public String renames(String renames, String from, String to) {
		return str("", renames, ", ") + from + " to " + to;
	}

	public String extend(String query, String list) {
		return query + " extend " + list;
	}

	public String extendList(String list, String column, String expr) {
		return str("", list, ", ") + column + " = " + expr;
	}

	public String where(String query, String expr) {
		return query + " where " + expr;
	}

	public String summarize(String query, String by, String ops) {
		return query + " summarize " + str("", by, ", ") + ops;
	}

	public String sumops(String sumops, String name, Token op, String field) {
		return str("", sumops, ", ") + str("", name, " = ") + op.string
				+ str(" ", field, "");
	}

}
