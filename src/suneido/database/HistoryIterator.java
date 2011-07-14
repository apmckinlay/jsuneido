/* Copyright 2011 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.database;

import java.util.Date;

import suneido.util.ByteBuf;

public class HistoryIterator {
	private boolean rewound = true;
	private final Destination dest;
	private final int tblnum;
	private Mmfile.Iter iter;
	private Commit commit = null;
	private int ic;
	private int id;
	private enum Dir { NEXT, PREV };

	public HistoryIterator(suneido.Transaction tran, int tblnum) {
		dest = ((Transaction) tran).db.dest;
		this.tblnum = tblnum;
	}

	public void rewind() {
		rewound = true;
	}

	public Record[] getNext() {
		return get(Dir.NEXT);
	}

	public Record[] getPrev() {
		return get(Dir.PREV);
	}

	private Record[] get(Dir dir) {
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
		} while (buf.getInt(0) != tblnum);
		String action = dir == Dir.NEXT
				? 0 <= ic && ic < commit.getNCreates() ? "create" : "delete"
				: 0 <= id && id < commit.getNDeletes() ? "delete" : "create";
		Record r1 = new Record()
				.add(new Date(commit.getDate()))
				.add(action);
		Record r2 = new Record(buf.slice(4), offset);
		return new Record[] { r1, r2 };
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
