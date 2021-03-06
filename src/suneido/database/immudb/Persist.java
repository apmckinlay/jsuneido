/* Copyright 2012 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.database.immudb;

import static suneido.database.immudb.Storage.align;

import java.nio.ByteBuffer;

import com.google.common.collect.ImmutableList;

import suneido.database.immudb.DbHashTrie.Entry;
import suneido.database.immudb.DbHashTrie.IntEntry;
import suneido.database.immudb.DbHashTrie.StoredIntEntry;
import suneido.database.immudb.DbHashTrie.Translator;

/**
 * Save dbinfo and btrees to storage (periodically).
 * Uses the same storage layout as Tran
 * but saves to the index storage instead of the data storage.
 * <p>
 * Each persist consists of:<br>
 * header - size and timestamp (timestamp zero if aborted)<br>
 * body - btrees and dbinfo<br>
 * ending - dbinfo root, max table number, dbstate.lastcksum, last adr<br>
 * tail - checksum and size (checksum zero if aborted)
 */
class Persist {
	static final int HEAD_SIZE = 2 * Integer.BYTES; // size and timestamp
	static final int TAIL_SIZE = 2 * Integer.BYTES; // checksum and size
	{ assert TAIL_SIZE == align(TAIL_SIZE); }
	static final int ENDING_SIZE = align(4 * Integer.BYTES);
	private final Storage istor;
	private DbHashTrie dbinfo;
	private int head_adr = 0;
	private int dbinfoadr;

	static void persist(Database db) {
		db.withCommitLock(() -> {
			Persist p = new Persist(db.state.dbinfo, db.istor);
			p.run(db);
		});
	}

	Persist(DbHashTrie dbinfo, Storage istor) {
		this.dbinfo = dbinfo;
		this.istor = istor;
	}

	private void run(Database db) {
		Database.State dbstate = db.state;
		startStore();
		storeBtrees();
		finish(db, dbstate.schema, dbstate.lastcksum, dbstate.lastadr);
	}

	/** also called by BulkTransaction */
	void startStore() {
		head_adr = istor.alloc(HEAD_SIZE); // to hold size and datetime
	}

	/** used by BulkTransaction */
	DbHashTrie storeBtrees(DbHashTrie dbinfo) {
		this.dbinfo = dbinfo;
		storeBtrees();
		return this.dbinfo;
	}

	/** stores btrees and frees up memory */
	void storeBtrees() {
		dbinfo.traverseUnstored((Entry e) -> {
			if (e instanceof TableInfo) {
				boolean modified = false;
				TableInfo ti = (TableInfo) e;
				ImmutableList.Builder<IndexInfo> b = ImmutableList.builder();
				for (IndexInfo ii : ti.indexInfo)
					if (ii.rootNode != null) {
						BtreeDbNode root = ii.rootNode.store(this.istor);
						b.add(new IndexInfo(ii, root));
						modified = true;
					} else
						b.add(ii);
				if (modified) {
					TableInfo ti2 = new TableInfo(ti, b.build());
					this.dbinfo = this.dbinfo.with(ti2);
				}
			}
		});
	}


	/** also called by BulkTransaction */
	void finish(Database db, Tables schema, int lastcksum, int lastadr) {
		dbinfoadr = storeDbinfo();
		ending(dbinfoadr, schema.maxTblnum, lastcksum, lastadr);

		int tail_adr = istor.alloc(TAIL_SIZE);
		int size = Storage.sizeToInt(istor.sizeFrom(head_adr));
		istor.buffer(head_adr).putInt(size).putInt(Tran.datetime());

		int cksum = istor.checksum(head_adr);
		istor.buffer(tail_adr).putInt(cksum).putInt(size);
		istor.protect();

		db.setState(dbinfoadr, dbinfo, schema, lastcksum, lastadr);
		db.setPersistState();
	}

	private void ending(int dbinfoadr, int maxTblnum, int lastcksum, int lastadr) {
		istor.buffer(istor.alloc(ENDING_SIZE))
				.putInt(dbinfoadr).putInt(maxTblnum)
				.putInt(lastcksum).putInt(lastadr);
	}

	private int storeDbinfo() {
		return dbinfo.store(istor, new DbInfoStorer(istor));
	}

	private static class DbInfoStorer implements Translator {
		final Storage stor;

		public DbInfoStorer(Storage stor) {
			this.stor = stor;
		}

		@Override
		public Entry translate(Entry entry) {
			if (entry instanceof TableInfo) {
				((TableInfo) entry).store(stor);
				return entry;
			} else {
				IntEntry ie = (IntEntry) entry;
				return new StoredIntEntry(ie.key, ie.value);
			}
		}
	}

	/** used by BulkTransaction */
	void abort(Database.State dbstate) {
		ending(dbstate.dbinfoadr, dbstate.schema.maxTblnum,
				dbstate.lastcksum, dbstate.lastadr);
		int tail_adr = istor.alloc(TAIL_SIZE);
		int sizeInt = Storage.sizeToInt(istor.sizeFrom(head_adr));
		istor.buffer(head_adr).putInt(sizeInt).putInt(0);
		istor.buffer(tail_adr).putInt(0).putInt(sizeInt);
		istor.protect();
	}

	/** used by Database open */
	static int dbinfoadr(Storage istor) {
		ByteBuffer buf = istor.rbuffer(-(Persist.TAIL_SIZE + ENDING_SIZE));
		return buf.getInt();
	}
	static int maxTblnum(Storage istor) {
		ByteBuffer buf = istor.rbuffer(-(Persist.TAIL_SIZE + ENDING_SIZE));
		buf.getInt(); // dbinfoadr
		return buf.getInt();
	}

}
