/* Copyright 2008 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.database.query;

import java.util.*;

import suneido.database.*;
import suneido.util.ByteBuf;

import com.google.common.collect.ImmutableList;

public class History extends Query {
	String tablename;
	final suneido.database.Table tbl;
	private boolean rewound = true;
	private List<String> columns = null;
	private Header header = null;
	private List<List<String>> indexes;
	private List<List<String>> keys;
	private final Destination dest;
	private Mmfile.Iter iter;
	private Commit commit = null;
	private int ic;
	private int id;

	History(Transaction tran, String tablename) {
		this.tablename = tablename;
		tbl = tran.ck_getTable(tablename);
		dest = tran.db.dest;
	}

	@Override
	public String toString() {
		return "history(" + tablename + ")";
	}

	@Override
	List<String> columns() {
		if (columns  == null)
			columns = new ImmutableList.Builder<String>()
					.addAll(tbl.getColumns())
					.add("_action")
					.add("_date")
					.build();
		return columns;
	}

	@Override
	int columnsize() {
		return 10;
	}

	@Override
	public Header header() {
		if (header == null)
			header = new Header(
					ImmutableList.of(
						noFields, ImmutableList.of("_date", "_action"),
						noFields, tbl.getColumns()),
					columns());
		return header;
	}

	@Override
	List<List<String>> indexes() {
		if (indexes == null)
			indexes = ImmutableList.of((List<String>) ImmutableList.of("_date"));
		return indexes;
	}

	@Override
	public List<List<String>> keys() {
		if (keys == null)
			keys = ImmutableList.of((List<String>) ImmutableList.of("_date"));
		return keys;
	}

	@Override
	double nrecords() {
		return 1000;
	}

	@Override
	double optimize2(List<String> index, Set<String> needs,
			Set<String> firstneeds, boolean is_cursor, boolean freeze) {
		return 100000;
	}

	@Override
	int recordsize() {
		return 100;
	}

	@Override
	public void rewind() {
		rewound  = true;
	}

	@Override
	void select(List<String> index, Record from, Record to) {
		rewound = true;
	}

	@Override
	public void setTransaction(Transaction tran) {
	}

	@Override
	public Row get(Dir dir) {
		if (rewound) {
			iter = ((Mmfile) dest).iterator();
			commit = null;
			rewound = false;
		}
		ByteBuf buf;
		long offset;
		do 	{
			offset = dir == Dir.NEXT ? next() : prev();
			assert ! iter.corrupt();
			if (offset == 0)
				return null;
			buf = dest.adr(offset - 4);
		} while (buf.getInt(0) != tbl.num);
		String action = dir == Dir.NEXT
				? 0 <= ic && ic < commit.getNCreates() ? "create" : "delete"
				: 0 <= id && id < commit.getNDeletes() ? "delete" : "create";
		Record r1 = new Record()
				.add(new Date(commit.getDate()))
				.add(action);
		Record r2 = new Record(buf.slice(4), offset);
		return new Row(Record.MINREC, r1, Record.MINREC, r2);
	}

	private long next() {
		while (true) {
			if (commit != null) {
				if (id + 1 < commit.getNDeletes())
					return commit.getDelete(++id);
				id = commit.getNDeletes();
				if (ic + 1 < commit.getNCreates())
					return commit.getCreate(++ic);
				commit = null;
			}
			do {
				if (! iter.next())
					return 0;
			} while (iter.type() != Mmfile.COMMIT);
			commit = new Commit(iter.current());
			id = ic = -1;
		}
	}

	private long prev() {
		while (true) {
			if (commit != null) {
				if (ic > 0)
					return commit.getCreate(--ic);
				ic = -1;
				if (id > 0)
					return commit.getDelete(--id);
				commit = null;
			}
			do {
				if (! iter.prev())
					return 0;
			} while (iter.type() != Mmfile.COMMIT);
			commit = new Commit(iter.current());
			ic = commit.getNCreates();
			id = commit.getNDeletes();
		}
	}

}
