/* Copyright 2012 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.immudb;

import static suneido.immudb.ChunkedStorage.align;

import java.nio.ByteBuffer;

import suneido.immudb.DbHashTrie.Entry;
import suneido.immudb.UpdateDbInfo.DbInfoTranslator;

import com.google.common.collect.ImmutableList;
import com.google.common.primitives.Ints;

/**
 * Save dbinfo and btrees to storage (periodically).
 * Uses a similar layout to Tran,
 * but saves to a separate file, not the data file.
 * <p>
 * Each persist consists of:<br>
 * header - size and datetime<br>
 * body - btrees and dbinfo<br>
 * dbinfo root and dbstate.lastcksum<br>
 * tail - checksum and size<br
 */
public class Persist {
	static final int HEAD_SIZE = 2 * Ints.BYTES; // size and datetime
	static final int TAIL_SIZE = 2 * Ints.BYTES; // checksum and size
	{ assert TAIL_SIZE == ChunkedStorage.align(TAIL_SIZE); }
	static final int ENDING_SIZE = ChunkedStorage.align(3 * Ints.BYTES);
	private final Database2 db;
	private final Database2.State dbstate;
	private final DbHashTrie dbinfo;
	private final Storage istor;
	private DbHashTrie newdbinfo;
	private int head_adr = 0;

	static Database2.State persist(Database2 db) {
		Persist p = new Persist(db);
		p.run();
		return p.dbstate;
	}

	private Persist(Database2 db) {
		this.db = db;
		dbstate = db.state;
		dbinfo = newdbinfo = dbstate.dbinfo;
		istor = db.istor;
	}

	private void run() {
		synchronized(db.commitLock) {
			start();
			storeBtrees();
			int adr = storeDbinfo();
			istor.buffer(istor.alloc(ENDING_SIZE))
					.putInt(adr).putInt(dbstate.lastcksum).putInt(dbstate.lastadr);
			finish();
			db.setState(newdbinfo, dbstate.schema, dbstate.lastcksum, dbstate.lastadr);
		}
	}

	static int dbinfoadr(Storage istor) {
		ByteBuffer buf = istor.buffer(-(Persist.TAIL_SIZE + align(ENDING_SIZE)));
		return buf.getInt();
	}

	static class Info {
		final int dbinfoadr;
		final int lastcksum;
		public Info(int dbinfoadr, int lastcksum) {
			this.dbinfoadr = dbinfoadr;
			this.lastcksum = lastcksum;
		}
	}

	private void start() {
		istor.protect(); // enable output
		head_adr = istor.alloc(HEAD_SIZE); // to hold size and datetime
	}

	private void storeBtrees() {
		dbinfo.traverseUnstored(proc);
	}

	DbHashTrie.Process proc = new DbHashTrie.Process() {
		@Override
		public void apply(Entry e) {
			if (e instanceof TableInfo) {
				TableInfo ti = (TableInfo) e;
				ImmutableList.Builder<IndexInfo> b = ImmutableList.builder();
				for (IndexInfo ii : ti.indexInfo)
					if (ii.rootNode != null) {
						int root = ii.rootNode.store2(istor);
						assert root != 0;
						b.add(new IndexInfo(ii, root));
					} else
						b.add(ii);
				TableInfo ti2 = new TableInfo(ti, b.build());
				newdbinfo = newdbinfo.with(ti2);
			}
		}};

	private int storeDbinfo() {
		return newdbinfo.store(istor, new DbInfoTranslator(istor));
	}

	void finish() {
		int tail_adr = istor.alloc(TAIL_SIZE);
		int size = (int) istor.sizeFrom(head_adr);
		istor.buffer(head_adr).putInt(size).putInt(Tran.datetime());

		int cksum = Tran.checksum(istor.iterator(head_adr));
		istor.buffer(tail_adr).putInt(cksum).putInt(size);
		istor.protectAll();
	}
}
