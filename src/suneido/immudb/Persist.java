/* Copyright 2012 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.immudb;

import suneido.immudb.DbHashTrie.Entry;

import com.google.common.primitives.Ints;

/**
 * Save dbinfo and btrees to storage (periodically).
 */
public class Persist {
	static final int HEAD_SIZE = 2 * Ints.BYTES; // size and datetime
	static final int TAIL_SIZE = 2 * Ints.BYTES; // checksum and size
	{ assert TAIL_SIZE == MmapFile.align(TAIL_SIZE); }
	private final DbHashTrie dbinfo;
	private final Storage stor;
	private int head_adr = 0;

	static void persist(Database2 db) {
		Persist p = new Persist(db);
		p.run();
	}

	public Persist(Database2 db) {
		dbinfo = db.state.dbinfo;
		stor = db.istor;
	}

	private void run() {
		start();
		storeBtrees();
		//storeDbinfo();
		finish();
	}

	private void start() {
		head_adr = stor.alloc(HEAD_SIZE); // to hold size and datetime
	}

	private void storeBtrees() {
		dbinfo.traverseChanges(proc);
	}
	DbHashTrie.Process proc = new DbHashTrie.Process() {
		@Override
		public void apply(Entry e) {
			if (e instanceof TableInfo) {
				TableInfo ti = (TableInfo) e;
System.out.println(ti);
				for (IndexInfo ii : ti.indexInfo)
					if (ii.rootNode != null) {
System.out.println("\t" + ii);
						storeBtree(ii.rootNode);
					}
			}
		}

		private void storeBtree(BtreeNode rootNode) {
			// TODO Auto-generated method stub

		}
	};

	void finish() {
		int tail_adr = stor.alloc(TAIL_SIZE);
		int size = (int) stor.sizeFrom(head_adr);
		stor.buffer(head_adr).putInt(size).putInt(Tran.datetime());

		int cksum = Tran.checksum(stor.iterator(head_adr));
		stor.buffer(tail_adr).putInt(cksum).putInt(size);
		stor.protectAll(); // can't output outside tran
	}
}
