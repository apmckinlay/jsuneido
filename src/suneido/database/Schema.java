package suneido.database;

import static suneido.util.Util.commasToList;
import static suneido.util.Util.listToCommas;

import java.util.List;

import suneido.SuException;

import com.google.common.collect.ImmutableList;

public class Schema {
/*	
	// tables =======================================================

	public static void addTable(Database db, String tablename) {
		Transaction t = db.readwriteTran();
		try {
			if (db.tableExists(tablename))
				throw new SuException("add table: table already exists: " + tablename);
			int tblnum = dbhdr.getNextTableNum();
			Record r = Table.record(tablename, tblnum, 0, 0);
			add_any_record(t, "tables", r);
			updateTable(t, tblnum);
			t.complete();
		} finally {
			t.abortIfNotComplete();
		}
	}

	public void removeTable(String tablename) {
		checkForSystemTable(tablename, "drop");
		Table table = ck_getTable(tablename);
		Transaction tran = readwriteTran();
		try {
			for (Index index : table.indexes)
				removeIndex(tran, table, index.columns);
			for (Column column : table.columns)
				removeColumn(tran, table, column.name);
			remove_any_record(tran, "tables", "tablename", key(tablename));
			tran.ck_complete();
		} finally {
			tran.abortIfNotComplete();
		}
		tables = tables.without(table);
		tabledata = tabledata.without(table.num);
	}

	private void checkForSystemTable(String tablename, String operation) {
		if (is_system_table(tablename))
			throw new SuException("can't " + operation +
					" system table: " + tablename);
	}

	public void renameTable(String oldname, String newname) {
		if (oldname.equals(newname))
			return ;

		Table table = ck_getTable(oldname);
		checkForSystemTable(oldname, "rename");
		if (null != getTable(newname))
			throw new SuException("rename table: table already exists: " + newname);

		Transaction tran = readwriteTran();
		try {
			TableData td = tran.getTableData(table.num);
			update_any_record(tran, "tables", "table", key(table.num),
					Table.record(newname, table.num, td.nextfield, td.nrecords));
			tables = tables.without(table);
			updateTable(tran, table.num);
			tran.ck_complete();
		} finally {
			tran.abortIfNotComplete();
		}

	}

	// columns ======================================================

	public void addColumn(String tablename, String column) {
		Table table = ck_getTable(tablename);
		Transaction tran = readwriteTran();
		try {
			TableData td = tran.getTableData(table.num);
			int fldnum =
					Character.isUpperCase(column.charAt(0)) ? -1 : td.nextfield;
			if (!column.equals("-")) { // addition of deleted field used by dump/load
				if (fldnum == -1)
					column =
							column.substring(0, 1).toLowerCase()
									+ column.substring(1);
				if (table.hasColumn(column))
					throw new SuException("add column: column already exists: "
							+ column + " in " + tablename);
				Record rec = Column.record(table.num, column, fldnum);
				add_any_record(tran, "columns", rec);
				if (fldnum >= 0)
					tran.updateTableData(td.withField());
				updateTable(tran, table.num);
				tran.complete();
			}
		} finally {
			tran.abortIfNotComplete();
		}
	}

	public void removeColumn(String tablename, String name) {
		if (is_system_column(tablename, name))
			throw new SuException("delete column: can't delete system column: "
					+ name + " from " + tablename);

		Table table = ck_getTable(tablename);

		if (table.columns.find(name) == null)
			throw new SuException("delete column: nonexistent column: " + name
					+ " in " + tablename);

		for (Index index : table.indexes)
			if (index.hasColumn(name))
				throw new SuException(
						"delete column: can't delete column used in index: "
						+ name + " in " + tablename);

		Transaction tran = readwriteTran();
		try {
			removeColumn(tran, table, name);
			updateTable(tran, table.num);
			tran.ck_complete();
		} finally {
			tran.abortIfNotComplete();
		}
	}

	private void removeColumn(Transaction tran, Table tbl, String name) {
		remove_any_record(tran, "columns", "table,column", key(tbl.num, name));
	}

	public void renameColumn(String tablename, String oldname, String newname) {
		if (oldname.equals(newname))
			return ;

		Table table = ck_getTable(tablename);
		if (is_system_column(tablename, oldname))
			throw new SuException("rename column: can't rename system column: "
					+ oldname + " in " + tablename);

		Column col = table.getColumn(oldname);
		if (col == null)
			throw new SuException("rename column: nonexistent column: "
					+ oldname + " in " + tablename);
		if (table.hasColumn(newname))
			throw new SuException("rename column: column already exists: "
					+ newname + " in " + tablename);

		Transaction tran = readwriteTran();
		try {
			update_any_record(tran, "columns", "table,column",
					key(table.num, oldname),
					Column.record(table.num, newname, col.num));

			// update any indexes that include this column
			for (Index index : table.indexes) {
				List<String> cols = commasToList(index.columns);
				int i = cols.indexOf(oldname);
				if (i < 0)
					continue ; // this index doesn't contain the column
				cols.set(i, newname);

				String newColumns = listToCommas(cols);
				Record newRecord = getBtreeIndex(index).withColumns(newColumns);
				update_any_record(tran, "indexes", "table,columns",
						key(table.num, index.columns), newRecord);
				}
			updateTable(tran, table.num);
			tran.ck_complete();
		} finally {
			tran.abortIfNotComplete();
		}
	}

	// indexes ======================================================

	public void addIndex(String tablename, String columns, boolean isKey) {
		addIndex(tablename, columns, isKey, false, false, null, null, 0);
	}

	public void addIndex(String tablename, String columns,
			boolean isKey, boolean unique, boolean lower, String fktablename,
			String fkcolumns, int fkmode) {
		if (fkcolumns == null || fkcolumns.equals(""))
			fkcolumns = columns;
		Table table = ck_getTable(tablename);
		ImmutableList<Integer> colnums = table.columns.nums(columns);
		if (table.hasIndex(columns))
			throw new SuException("add index: index already exists: " + columns
					+ " in " + tablename);
		BtreeIndex btreeIndex = new BtreeIndex(dest, table.num, columns, isKey,
				unique, fktablename, fkcolumns, fkmode);

		Tables originalTables = tables;
		Transaction tran = readwriteTran();
		try {
			add_any_record(tran, "indexes", btreeIndex.record);
			updateTable(tran, table.num);
			Table fktable = null;
			if (fktablename != null) {
				fktable = getTable(fktablename);
				if (fktable != null)
					updateTable(tran, fktable.num);
			}
			insertExistingRecords(tran, columns, table, colnums,
					fktablename, fktable, fkcolumns, btreeIndex);
			tran.complete();
		} catch (RuntimeException e) {
			tables = originalTables; // TODO temp till tables in tran
			throw e;
		} finally {
			tran.abortIfNotComplete();
		}
	}

	private void insertExistingRecords(Transaction tran, String columns,
			Table table, ImmutableList<Integer> colnums, String fktablename,
			Table fktable, String fkcolumns, BtreeIndex btreeIndex) {
		if (!table.hasIndexes())
			return;

		BtreeIndex.Iter iter =
				getBtreeIndex(table.firstIndex()).iter(tran).next();
		if (iter.eof())
			return;

		if (fktablename != null && fktable == null)
			throw new SuException("add index to " + table.name
					+ " blocked by foreign key to nonexistent table: "
					+ fktablename);

		for (; !iter.eof(); iter.next()) {
			Record rec = input(iter.keyadr());
			if (fktable != null)
				fkey_source_block1(tran, fktable, fkcolumns,
						rec.project(colnums), "add index to " + table.name);
			Record key = rec.project(colnums, iter.cur().keyadr());
			if (!btreeIndex.insert(tran, new Slot(key)))
				throw new SuException("add index: duplicate key: " + columns
						+ " = " + key + " in " + table.name);
		}
	}

	public void removeIndex(String tablename, String columns) {
		if (is_system_index(tablename, columns))
			throw new SuException("delete index: can't delete system index: "
					+ columns + " from " + tablename);
		Table table = ck_getTable(tablename);
		if (table.indexes.size() == 1)
			throw new SuException("delete index: can't delete last index from "
					+ tablename);
		Transaction tran = readwriteTran();
		try {
			removeIndex(tran, table, columns);
			updateTable(tran, table.num);
			tran.ck_complete();
		} finally {
			tran.abortIfNotComplete();
		}
	}

	private void removeIndex(Transaction tran, Table tbl, String columns) {
		if (!tbl.indexes.hasIndex(columns))
			throw new SuException("delete index: nonexistent index: " + columns
					+ " in " + tbl.name);

		remove_any_record(tran, "indexes", "table,columns",
				key(tbl.num, columns));
	}
*/
}
