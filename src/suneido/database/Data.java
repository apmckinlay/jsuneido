package suneido.database;

import static suneido.Suneido.verify;
import static suneido.database.Schema.checkForSystemTable;

import javax.annotation.concurrent.ThreadSafe;

import suneido.SuException;

import com.google.common.collect.ImmutableList;

/**
 * Methods to add/update/remove records.
 *
 * @author Andrew McKinlay
 */
@ThreadSafe
class Data {

	// add record ===================================================

	static void addRecord(Transaction tran, String tablename, Record r) {
		checkForSystemTable(tablename, "add record to");
		add_any_record(tran, tablename, r);
	}

	static void add_any_record(Transaction tran, String tablename, Record r) {
		add_any_record(tran, tran.ck_getTable(tablename), r);
	}

	static void add_any_record(Transaction tran, Table table, Record rec) {
		if (tran.isReadonly())
			throw new SuException("can't output from read-only transaction to "
					+ table.name);
		assert (table != null);
		verify(!table.indexes.isEmpty());

		if (!tran.db.loading)
			fkey_source_block(tran, table, rec, "add record to " + table.name);

		long adr = tran.db.output(table.num, rec);
		add_index_entries(tran, table, rec, adr);
		tran.create_act(table.num, adr);

		if (!tran.db.loading)
			Triggers.call(tran, table, null, rec);

		if (tran.isAborted())
			throw new SuException("add record to " + table.name
					+ " transaction conflict: " + tran.conflict());
	}

	static void add_index_entries(Transaction tran, Table table, Record rec, long adr) {
		for (Index index : table.indexes) {
			BtreeIndex btreeIndex = tran.getBtreeIndex(index);
			Record key = rec.project(index.colnums, adr);
			// handle insert failing due to duplicate key
			if (!btreeIndex.insert(tran, new Slot(key))) {
				// delete from previous indexes
				for (Index j : table.indexes) {
					if (j == index)
						break;
					key = rec.project(j.colnums, adr);
					btreeIndex = tran.getBtreeIndex(j);
					verify(btreeIndex.remove(key));
				}
				throw new SuException("duplicate key: " + index.columns + " = "
						+ key + " in " + table.name);
			}
		}

		tran.updateTableData(tran.getTableData(table.num).with(rec.bufSize()));
	}

	// update record ================================================

	static long updateRecord(Transaction tran, long recadr, Record rec) {
		verify(recadr > 0);
		int tblnum = tran.db.adr(recadr - 4).getInt(0);
		Table table = tran.ck_getTable(tblnum);
		checkForSystemTable(table.name, "update record in");
		return update_record(tran, table, tran.db.input(recadr), rec, true);
	}

	static void updateRecord(Transaction tran, String table, String index,
			Record key, Record newrec) {
		checkForSystemTable(table, "update record in");
		update_any_record(tran, table, index, key, newrec);
	}

	static void update_any_record(Transaction tran, String tablename,
			String indexcolumns, Record key, Record newrec) {
		Table table = tran.ck_getTable(tablename);
		Index index = getIndex(table, indexcolumns);
		Record oldrec = find(tran, index, key);
		if (oldrec == null)
			throw new SuException("update record: can't find record in "
					+ tablename);
		update_record(tran, table, oldrec, newrec, true);
	}

	static long update_record(Transaction tran, Table table, Record oldrec,
			Record newrec, boolean srcblock) {
		if (tran.isReadonly())
			throw new SuException("can't update from read-only transaction in "
					+ table.name);

		long oldoff = oldrec.off();

		// check foreign keys
		for (Index i : table.indexes) {
			if ((!srcblock || i.fksrc == null) && i.fkdsts.isEmpty())
				continue; // no foreign keys for this index
			Record oldkey = oldrec.project(i.colnums);
			Record newkey = newrec.project(i.colnums);
			if (oldkey.equals(newkey))
				continue;
			if (srcblock && i.fksrc != null)
				fkey_source_block1(tran, tran.getTable(i.fksrc.tablename),
						i.fksrc.columns,
						newkey, "update record in " + table.name);
			fkey_target_block1(tran, i, oldkey, newkey, "update record in "
					+ table.name);
		}

		remove_index_entries(tran, table, oldrec);

		tran.delete_act(table.num, oldoff);

		// do the update
		long newoff = tran.db.output(table.num, newrec); // output new version

		// update indexes
		for (Index index : table.indexes) {
			Record newkey = newrec.project(index.colnums, newoff);
			BtreeIndex btreeIndex = tran.getBtreeIndex(index);
			if (!btreeIndex.insert(tran, new Slot(newkey))) {
				// undo previous
				for (Index j : table.indexes) {
					if (j == index)
						break;
					btreeIndex = tran.getBtreeIndex(j);
					verify(btreeIndex.remove(newrec.project(j.colnums, newoff)));
				}
				add_index_entries(tran, table, oldrec, oldoff);
				tran.undo_delete_act(table.num, oldoff);
				throw new SuException("update record: duplicate key: "
						+ index.columns + " = " + newkey + " in " + table.name);
			}
		}
		tran.create_act(table.num, newoff);
		tran.updateTableData(tran.getTableData(table.num)
				.withReplace(oldrec.bufSize(), newrec.bufSize()));

		Triggers.call(tran, table, oldrec, newrec);
		return newoff;
	}

	// remove record ================================================

	static void removeRecord(Transaction tran, long recadr) {
		verify(recadr > 0);
		int tblnum = tran.db.adr(recadr - 4).getInt(0);
		Table tbl = tran.ck_getTable(tblnum);
		checkForSystemTable(tbl.name, "delete record from");
		remove_any_record(tran, tbl, tran.db.input(recadr));

	}

	static void removeRecord(Transaction tran, String tablename, String index,
			Record key) {
		checkForSystemTable(tablename, "delete record from");
		remove_any_record(tran, tablename, index, key);
	}

	static void remove_any_record(Transaction tran, String tablename,
			String indexcolumns, Record key) {
		Table table = tran.ck_getTable(tablename);
		// lookup key in given index
		Index index = table.indexes.get(indexcolumns);
		assert (index != null);
		Record rec = tran.db.find(tran, tran.getBtreeIndex(index), key);
		if (rec == null)
			throw new SuException("delete record: can't find record in "
					+ tablename + " " + indexcolumns + " " + key);
		remove_any_record(tran, table, rec);
	}

	static void remove_any_record(Transaction tran, Table table, Record rec) {
		if (tran.isReadonly())
			throw new SuException("can't delete from read-only transaction in "
					+ table.name);
		assert (table != null);
		assert (rec != null);

		fkey_target_block(tran, table, rec, "delete from " + table.name);

		tran.delete_act(table.num, rec.off());

		remove_index_entries(tran, table, rec);

		tran.updateTableData(tran.getTableData(table.num).without(rec.bufSize()));

		if (!tran.db.loading)
			Triggers.call(tran, table, rec, null);

		if (tran.isAborted())
			throw new SuException("delete record from " + table.name
					+ " transaction conflict: " + tran.conflict());
	}

	private static void remove_index_entries(Transaction tran, Table table, Record rec) {
		long off = rec.off();
		for (Index index : table.indexes) {
			Record key = rec.project(index.colnums, off);
			BtreeIndex btreeIndex = tran.getBtreeIndex(index);
			verify(btreeIndex.remove(key));
		}
	}

	// foreign keys =================================================

	private static void fkey_source_block(Transaction tran, Table table, Record rec, String action) {
		for (Index index : table.indexes)
			if (index.fksrc != null) {
				fkey_source_block1(tran, tran.getTable(index.fksrc.tablename),
						index.fksrc.columns, rec.project(index.colnums), action);
			}
	}

	static void fkey_source_block1(Transaction tran, Table fktbl,
			String fkcolumns, Record key, String action) {
		if (fkcolumns.equals("") || key.allEmpty())
			return;
		Index fkidx = getIndex(fktbl, fkcolumns);
		if (fkidx == null || find(tran, fkidx, key) == null)
			throw new SuException(action + " blocked by foreign key to "
					+ fktbl.name + " " + key);
	}

	static void fkey_target_block(Transaction tran, Table tbl, Record r, String action) {
		for (Index i : tbl.indexes)
			fkey_target_block1(tran, i, r.project(i.colnums), null, action);
	}

	private static void fkey_target_block1(Transaction tran, Index index, Record key,
			Record newkey, String action) {
		if (key.allEmpty())
			return;
		for (Index.ForeignKey fk : index.fkdsts) {
			Table fktbl = tran.getTable(fk.tblnum);
			Index fkidx = getIndex(fktbl, fk.columns); // TODO tran.getIndex ?
			if (fkidx == null)
				continue ;
			BtreeIndex.Iter iter =
					tran.getBtreeIndex(fkidx).iter(tran, key).next();
			if (newkey == null && (fk.mode & Index.CASCADE_DELETES) != 0)
				for (; ! iter.eof(); iter.next())
					cascade_delete(tran, fktbl, iter);
			else if (newkey != null && (fk.mode & Index.CASCADE_UPDATES) != 0)
				for (; !iter.eof(); iter.next())
					cascade_update(tran, newkey, fktbl, iter, fkidx.colnums);
			else // blocking
				if (! iter.eof())
					throw new SuException(action + " blocked by foreign key in "
							+ fktbl.name);
		}
	}

	private static void cascade_update(Transaction tran, Record newkey, Table fktbl,
			BtreeIndex.Iter iter, ImmutableList<Integer> colnums) {
		Record oldrec = tran.db.input(iter.keyadr());
		Record newrec = new Record();
		for (int i = 0; i < oldrec.size(); ++i) {
			int j = colnums.indexOf(i);
			if (j == -1)
				newrec.add(oldrec.get(i));
			else
				newrec.add(newkey.get(j));
		}
		update_record(tran, fktbl, oldrec, newrec, false);
	}

	private static void cascade_delete(Transaction tran, Table fktbl,
			BtreeIndex.Iter iter) {
		Record r = tran.db.input(iter.keyadr());
		remove_any_record(tran, fktbl, r);
		iter.reset_prevsize();
		// need to reset prevsize in case trigger updates other lines
		// otherwise iter doesn't "see" the updated lines
	}

	private static Record find(Transaction tran, Index index, Record key) {
		return tran.db.find(tran, tran.getBtreeIndex(index), key);
	}

	private static Index getIndex(Table table, String indexcolumns) {
		return table == null ? null : table.getIndex(indexcolumns);
	}

}
