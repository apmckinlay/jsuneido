/* Copyright 2012 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.immudb;

import static suneido.immudb.ChunkedStorage.align;

import java.io.IOException;
import java.io.PrintWriter;

import suneido.immudb.DbHashTrie.Entry;
import suneido.immudb.UpdateDbInfo.DbInfoTranslator;

import com.google.common.collect.ImmutableList;
import com.google.common.primitives.Ints;

/**
 * Save dbinfo and btrees to storage (periodically).
 */
public class Persist {
	static final int HEAD_SIZE = 2 * Ints.BYTES; // size and datetime
	static final int TAIL_SIZE = 2 * Ints.BYTES; // checksum and size
	{ assert TAIL_SIZE == MmapFile.align(TAIL_SIZE); }
	private final Database2 db;
	private final DbHashTrie dbinfo;
	private final Storage istor;
	private DbHashTrie newdbinfo;
	private int head_adr = 0;

	static void persist(Database2 db) {
		Persist p = new Persist(db);
		p.run();
	}

	public Persist(Database2 db) {
		this.db = db;
		dbinfo = newdbinfo = db.state.dbinfo;
		istor = db.istor;
	}

	private void run() {
		synchronized(db.commitLock) {
			start();
			storeBtrees();
			int adr = storeDbinfo();
			istor.buffer(istor.alloc(Ints.BYTES)).putInt(adr);
			finish();
			db.setState(newdbinfo, db.state.schema);
		}
	}

	static int dbinfoAdr(Storage istor) {
		return istor.buffer(-(Persist.TAIL_SIZE + align(Ints.BYTES))).getInt();
	}

	private void start() {
		istor.protect(); // enable output
		head_adr = istor.alloc(HEAD_SIZE); // to hold size and datetime
	}

	private void storeBtrees() {
		dbinfo.traverseChanges(proc);
	}
	DbHashTrie.Process proc = new DbHashTrie.Process() {
		@Override
		public void apply(Entry e) {
			if (e instanceof TableInfo) {
				TableInfo ti = (TableInfo) e;
//System.out.println(ti);
				ImmutableList.Builder<IndexInfo> b = ImmutableList.builder();
				for (IndexInfo ii : ti.indexInfo)
					if (ii.rootNode != null) {
//System.out.println("\t" + ii);
//System.out.println("BEFORE -------------------");
//print(ii);
						int root = ii.rootNode.store2(istor);
//System.out.println("AFTER --------------------");
//print(ii);
//print(Btree2.nodeAt(stor, 0, ii.rootNode.address()));
						b.add(new IndexInfo(ii, root));
					}
				newdbinfo = newdbinfo.with(new TableInfo(ti, b.build()));
			}
		}

		private void print(BtreeNode node) {
			try {
				PrintWriter writer = new PrintWriter(System.out);
				node.print2(writer, istor);
				writer.flush();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	};

	private int storeDbinfo() {
		return newdbinfo.store(istor, new DbInfoTranslator(istor));
	}

	void finish() {
		int tail_adr = istor.alloc(TAIL_SIZE);
		int size = (int) istor.sizeFrom(head_adr);
		istor.buffer(head_adr).putInt(size).putInt(Tran.datetime());

		int cksum = Tran.checksum(istor.iterator(head_adr));
		istor.buffer(tail_adr).putInt(cksum).putInt(size);
		istor.protectAll(); // can't output outside tran
	}
}
