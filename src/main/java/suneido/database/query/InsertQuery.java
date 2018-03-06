/* Copyright 2009 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.database.query;

import java.util.List;

import suneido.database.immudb.RecordBuilder;
import suneido.database.immudb.Transaction;

public class InsertQuery extends QueryAction {
	private final Transaction tran;
	private final String table;

	InsertQuery(Transaction tran, Query source, String table) {
		super(source);
		this.tran = tran;
		this.table = table;
	}

	@Override
	public String toString() {
		return "INSERT " + source + " INTO " + table;
	}

	@Override
	public int execute() {
		Query q = source.setup(tran);
		Header hdr = q.header();
		List<String> fields = tran.ck_getTable(table).getFields();
		Row row;
		int n = 0;
		for (; null != (row = q.get(Dir.NEXT)); ++n) {
			RecordBuilder rb = new RecordBuilder();
			for (String f : fields)
				if (f.equals("-"))
					rb.addMin();
				else
					rb.add(row.getraw(hdr, f));
			tran.addRecord(table, rb.build());
		}
		return n;
	}

}
