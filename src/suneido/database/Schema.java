/* Copyright 2008 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.database;

import static suneido.database.Database.key;
import static suneido.util.Util.commasToList;
import static suneido.util.Util.listToCommas;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.concurrent.ThreadSafe;

import suneido.SuException;

import com.google.common.collect.ImmutableList;

/**
 * schema update operations
 */
@ThreadSafe // no state, all static methods
class Schema {

	// tables =======================================================

	static void addTable(Database db, String tablename) {
		if (!ensureTable(db, tablename))
			throw new SuException("add table: table already exists: " + tablename);
	}

	/** returns false if table already exists */
	static boolean ensureTable(Database db, String tablename) {
		int tblnum = db.getNextTableNum();
		Transaction tran = db.readwriteTran();
		try {
			if (tran.tableExists(tablename))
				return false;
			Record r = Table.record(tablename, tblnum, 0, 0);
			Data.add_any_record(tran, "tables", r);
			tran.updateTable(tblnum);
			tran.ck_complete();
		} finally {
			tran.abortIfNotComplete();
		}
		return true;
	}

	/** returns false if table does not exist */
	static boolean removeTable(Database db, String tablename) {
		checkForSystemTable(tablename, "drop");
		Transaction tran = db.readwriteTran();
		try {
			if (tran.getView(tablename) != null) {
				tran.removeView(tablename);
			} else {
				Table table = tran.getTable(tablename);
				if (table == null)
					return false;
				for (Index index : table.indexes)
					removeIndex(db, tran, table, index.columns);
				for (Column column : table.columns)
					removeColumn(db, tran, table, column.name);
				Data.remove_any_record(tran, "tables", "table", key(table.num));
				tran.deleteTable(table);
			}
			tran.ck_complete();
		} finally {
			tran.abortIfNotComplete();
		}
		return true;
	}

	static void checkForSystemTable(String tablename, String operation) {
		if (isSystemTable(tablename))
			throw new SuException("can't " + operation +
					" system table: " + tablename);
	}

	static boolean isSystemTable(String table) {
		return table.equals("tables") || table.equals("columns")
				|| table.equals("indexes") || table.equals("views");
	}

	private static boolean isSystemColumn(String table, String column) {
		return (table.equals("tables") && (column.equals("table")
				|| column.equals("nrows") || column.equals("totalsize")))
			|| (table.equals("columns") && (column.equals("table")
				|| column.equals("column") || column.equals("field")))
			|| (table.equals("indexes") && (column.equals("table")
				|| column.equals("columns") || column.equals("root")
				|| column.equals("treelevels") || column.equals("nnodes")));
	}

	private static boolean isSystemIndex(String table, String columns) {
		return (table.equals("tables") && columns.equals("table"))
				|| (table.equals("columns") && columns.equals("table,column"))
				|| (table.equals("indexes") && columns.equals("table,columns"));
	}

	static void renameTable(Database db, String oldname, String newname) {
		if (oldname.equals(newname))
			return ;

		checkForSystemTable(oldname, "rename");

		Transaction tran = db.readwriteTran();
		try {
			if (tran.tableExists(newname))
				throw new SuException("rename table: table already exists: " + newname);
			Table table = tran.ck_getTable(oldname);
			TableData td = tran.getTableData(table.num);
			Data.update_any_record(tran, "tables", "table", key(table.num),
					Table.record(newname, table.num, td.nextfield, td.nrecords, td.totalsize));
			tran.updateTable(table.num);
			tran.ck_complete();
		} finally {
			tran.abortIfNotComplete();
		}
	}

	// columns ======================================================

	static void addColumn(Database db, String tablename, String column) {
		if (!ensureColumn(db, tablename, column))
			throw new SuException("add column: column already exists: "
					+ column + " in " + tablename);
	}

	static boolean ensureColumn(Database db, String tablename, String column) {
		synchronized(db.commitLock) {
			Transaction tran = db.readwriteTran();
			try {
				Table table = tran.ck_getTable(tablename);
				TableData td = tran.getTableData(table.num);
				int fldnum = isRuleField(column) ? -1 : td.nextfield;
				// "-" is addition of deleted field used by dump/load
				if (!column.equals("-")) {
					if (fldnum == -1)
						column = column.substring(0, 1).toLowerCase()
								+ column.substring(1);
					if (table.hasColumn(column))
						return false;
					Record rec = Column.record(table.num, column, fldnum);
					Data.add_any_record(tran, "columns", rec);
					tran.updateTable(table.num);
				}
				if (fldnum >= 0)
					tran.updateTableData(td.withField());
				tran.ck_complete();
			} finally {
				tran.abortIfNotComplete();
			}
			return true;
		}
	}

	private static boolean isRuleField(String column) {
		return Character.isUpperCase(column.charAt(0));
	}

	static void removeColumn(Database db, String tablename, String name) {
		if (isSystemColumn(tablename, name))
			throw new SuException("delete column: can't delete system column: "
					+ name + " from " + tablename);

		synchronized(db.commitLock) {
			Transaction tran = db.readwriteTran();
			try {
				Table table = tran.ck_getTable(tablename);
				if (table.columns.find(name) == null)
					throw new SuException("delete column: nonexistent column: " + name
							+ " in " + tablename);
				for (Index index : table.indexes)
					if (index.hasColumn(name))
						throw new SuException(
								"delete column: can't delete column used in index: "
								+ name + " in " + tablename);

				removeColumn(db, tran, table, name);
				tran.updateTable(table.num);
				tran.ck_complete();
			} finally {
				tran.abortIfNotComplete();
			}
		}
	}

	// used by removeColumn and removeTable
	private static void removeColumn(Database db, Transaction tran, Table tbl, String name) {
		Data.remove_any_record(tran, "columns", "table,column", key(tbl.num, name));
	}

	static void renameColumn(Database db, String tablename, String oldname, String newname) {
		if (oldname.equals(newname))
			return ;

		if (isSystemColumn(tablename, oldname))
			throw new SuException("rename column: can't rename system column: "
					+ oldname + " in " + tablename);

		synchronized(db.commitLock) {
			Transaction tran = db.readwriteTran();
			try {
				Table table = tran.ck_getTable(tablename);
				if (table.hasColumn(newname))
					throw new SuException("rename column: column already exists: "
							+ newname + " in " + tablename);
				Column col = table.getColumn(oldname);
				if (col == null)
					throw new SuException("rename column: nonexistent column: "
							+ oldname + " in " + tablename);

				Data.update_any_record(tran, "columns", "table,column",
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
					Record newRecord = tran.getBtreeIndex(index).withColumns(newColumns);
					Data.update_any_record(tran, "indexes", "table,columns",
							key(table.num, index.columns), newRecord);
					}
				List<BtreeIndex> btis = new ArrayList<BtreeIndex>();
				tran.updateTable(table.num, btis);
				for (BtreeIndex bti : btis)
					tran.addBtreeIndex(bti);
				tran.ck_complete();
			} finally {
				tran.abortIfNotComplete();
			}
		}
	}

	// indexes ======================================================

	static void addIndex(Database db, String tablename, String columns,
			boolean isKey, boolean unique, String fktablename,
			String fkcolumns, int fkmode) {
			if (!ensureIndex(db, tablename, columns, isKey, unique,
				fktablename, fkcolumns, fkmode))
				throw new SuException("add index: index already exists: " + columns
						+ " in " + tablename);
	}
	static boolean ensureIndex(Database db, String tablename, String columns,
			boolean isKey, boolean unique, String fktablename,
			String fkcolumns, int fkmode) {
		if (fkcolumns == null || fkcolumns.equals(""))
			fkcolumns = columns;

		synchronized(db.commitLock) {
			Transaction tran = db.readwriteTran();
			try {
				Table table = tran.ck_getTable(tablename);
				ImmutableList<Integer> colnums = table.columns.nums(columns);
				if (table.hasIndex(columns))
					return false;
				BtreeIndex btreeIndex = new BtreeIndex(db.dest, table.num, columns,
						isKey, unique, fktablename, fkcolumns, fkmode);
				Data.add_any_record(tran, "indexes", btreeIndex.record);
				List<BtreeIndex> btis = new ArrayList<BtreeIndex>();
				tran.updateTable(table.num, btis);
				btreeIndex = findBtreeIndex(btis, columns);
				Table fktable = null;
				if (fktablename != null) {
					fktable = tran.getTable(fktablename);
					if (fktable != null)
						tran.updateTable(fktable.num);
				}
				insertExistingRecords(db, tran, columns, table, colnums,
						fktablename, fktable, fkcolumns, btreeIndex);
				tran.addBtreeIndex(btreeIndex);
				tran.ck_complete();
			} finally {
				tran.abortIfNotComplete();
			}
			return true;
		}
	}
	private static BtreeIndex findBtreeIndex(List<BtreeIndex> btis, String columns) {
		for (BtreeIndex bti : btis)
			if (bti.columns.equals(columns))
				return bti;
		throw SuException.unreachable();
	}

	private static void insertExistingRecords(Database db, Transaction tran, String columns,
			Table table, ImmutableList<Integer> colnums, String fktablename,
			Table fktable, String fkcolumns, BtreeIndex btreeIndex) {
		if (!table.hasIndexes())
			return;

		Index index = table.firstIndex();
		if (index == null)
			return;
		BtreeIndex.Iter iter = tran.getBtreeIndex(index).iter();
		iter.next();
		if (iter.eof())
			return;

		if (fktablename != null && fktable == null)
			throw new SuException("add index to " + table.name
					+ " blocked by foreign key to nonexistent table: "
					+ fktablename);

		for (; !iter.eof(); iter.next()) {
			Record rec = db.input(iter.keyoff());
			if (fktable != null)
				Data.fkey_source_block1(tran, fktable, fkcolumns,
						rec.project(colnums), "add index to " + table.name);
			Record key = rec.project(colnums, iter.cur().keyRecOff());
			if (!btreeIndex.insert(tran, new Slot(key)))
				throw new SuException("add index: duplicate key: " + columns
						+ " = " + key + " in " + table.name);
		}
	}

	static void removeIndex(Database db, String tablename, String columns) {
		if (isSystemIndex(tablename, columns))
			throw new SuException("delete index: can't delete system index: "
					+ columns + " from " + tablename);
		synchronized(db.commitLock) {
			Transaction tran = db.readwriteTran();
			try {
				Table table = tran.ck_getTable(tablename);
				if (table.indexes.size() == 1)
					throw new SuException("delete index: can't delete last index from "
							+ tablename);
				removeIndex(db, tran, table, columns);
				tran.updateTable(table.num);
				tran.ck_complete();
			} finally {
				tran.abortIfNotComplete();
			}
		}
	}

	// used by removeIndex and removeTable
	private static void removeIndex(Database db, Transaction tran, Table table,
			String columns) {
		if (!table.indexes.hasIndex(columns))
			throw new SuException("delete index: nonexistent index: " + columns
					+ " in " + table.name);
		Data.remove_any_record(tran, "indexes", "table,columns",
				key(table.num, columns));
	}

}
