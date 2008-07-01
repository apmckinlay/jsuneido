package suneido.database;

import java.util.ArrayList;

public class TableSpec {
	String name;
	ArrayList<String> columns = new ArrayList<String>();
	ArrayList<IndexSpec> indexes = new ArrayList<IndexSpec>();

	TableSpec(String name) {
		this.name = name;
	}

	public TableSpec addColumn(String name) {
		columns.add(name);
		return this;
	}

	public TableSpec addIndex(String columns, boolean isKey, String fktable,
			String fkcolumns, int fkmode, boolean unique, boolean lower) {
		indexes.add(new IndexSpec(columns, isKey, fktable, fkcolumns, fkmode,
				unique, lower));
		return this;
	}

	private static class IndexSpec {
		String columns;
		boolean isKey;
		String fktable;
		String fkcolumns;
		int fkmode;
		boolean unique;
		boolean lower;

		IndexSpec(String columns, boolean isKey, String fktable,
				String fkcolumns, int fkmode, boolean unique, boolean lower) {
			this.columns = columns;
			this.isKey = isKey;
			this.fktable = fktable;
			this.fkcolumns = fkcolumns;
			this.fkmode = fkmode;
			this.unique = unique;
			this.lower = lower;
		}
	}

}
