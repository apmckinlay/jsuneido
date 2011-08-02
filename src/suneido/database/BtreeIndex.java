/* Copyright 2008 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.database;

import static suneido.SuException.verify;
import static suneido.SuException.verifyEquals;
import static suneido.database.Index.*;
import suneido.intfc.database.IndexIter;

import com.google.common.base.Objects;

/**
 * Wraps a {@link Btree} to implement database table indexes.
 * Adds transaction stuff.
 * Almost immutable but update will change record, and btree info may change.
 */
class BtreeIndex {
	final Record record;
	private Destination dest;
	private final Btree bt;
	final boolean iskey;
	final boolean unique;
	final int tblnum;
	final String columns;

	/** Create a new index */
	BtreeIndex(Destination dest, int tblnum, String indexColumns,
			boolean isKey, boolean unique) {
		this(dest, tblnum, indexColumns, isKey, unique, "", "", 0);
	}

	/** Create a new index */
	BtreeIndex(Destination dest, int tblnum, String columns,
			boolean isKey, boolean unique,
			String fktable, String fkcolumns, int fkmode) {
		Btree bt = new Btree(dest);
		this.dest = dest;
		this.record = record(bt, tblnum, columns, isKey, unique,
				fktable, fkcolumns, fkmode);
		this.bt = bt;
		this.iskey = isKey;
		this.unique = unique;
		this.tblnum = tblnum;
		this.columns = columns;
	}

	/** Open an existing index */
	BtreeIndex(Destination dest, Record record) {
		this.dest = dest;
		this.record = record;
		this.bt = new Btree(dest, record.getMmoffset(ROOT),
				record.getInt(TREELEVELS), record.getInt(NNODES));
		Object key = record.get(KEY);
		this.iskey = key == Boolean.TRUE;
		this.unique = key.equals(UNIQUE);
		this.tblnum = record.getInt(TBLNUM);
		this.columns = record.getString(COLUMNS);
	}

	/** Copy constructor, used by {@link Transaction} */
	BtreeIndex(BtreeIndex bti, Destination dest) {
		this.dest = dest;
		record = bti.record;
		bt = new Btree(bti.bt, dest);
		iskey = bti.iskey;
		unique = bti.unique;
		tblnum = bti.tblnum;
		columns = bti.columns;
	}

	static void rebuildCreate(Destination dest, Record rec) {
		new BtreeIndex(rec, dest);
	}

	private BtreeIndex(Record rec, Destination dest) {
		tblnum = rec.getInt(Index.TBLNUM);
		columns = rec.getString(Index.COLUMNS);
		Object key = rec.get(Index.KEY);
		iskey = key == Boolean.TRUE;
		unique = key.equals(Index.UNIQUE);
		this.record = rec;
		bt = new Btree(dest);
		btreeInfo(bt, rec);
	}

	Destination getDest() {
		return dest;
	}
	void setDest(Destination dest) {
		this.dest = dest;
		bt.setDest(dest);
	}

	boolean differsFrom(BtreeIndex bti) {
		return bt.differsFrom(bti.bt);
	}

	boolean update(BtreeIndex btiOld, BtreeIndex btiNew) {
		return bt.update(btiOld.bt, btiNew.bt);
	}

	/** updates the record in place */
	void update() {
		verify(record.offset() != 0);
		btreeInfo(bt, record);
	}

	Record record(String fktable, String fkcolumns, int fkmode) {
		return record(bt, tblnum, columns, iskey, unique, fktable,
				fkcolumns, fkmode);
	}

	static Record record(Btree bt, int tblnum, String indexColumns,
			boolean iskey, boolean unique,
			String fktable, String fkcolumns, int fkmode) {
		Record r = new Record()
			.add(tblnum)
			.add(indexColumns)
			.add(iskey ? Boolean.TRUE :	unique ? Index.UNIQUE : Boolean.FALSE)
			.add(fktable).add(fkcolumns).add(fkmode);
		r.add(Mmfile.offsetToInt(bt.root()));
		r.add(bt.treelevels());
		r.add(bt.nnodes());
		r.alloc(24); // 24 = 3 fields * max int packsize - min int packsize
		return r;
	}

	private static void btreeInfo(Btree bt, Record r) {
		int n = r.packSize();
		r.truncate(Index.ROOT);
		r.add(Mmfile.offsetToInt(bt.root()));
		r.add(bt.treelevels());
		r.add(bt.nnodes());
		r.alloc(n - r.packSize() - (n < 256 ? 1 : 2));
		verifyEquals(n, r.packSize());
	}

	Record withColumns(String newColumns) {
		Record r = new Record();
		for (int i = 0; i <= NNODES; ++i)
			if (i == COLUMNS)
				r.add(newColumns);
			else
				r.add(record.getRaw(i));
		return r;
	}

	int nnodes() {
		return bt.nnodes();
	}

	int totalSize() {
//		int usage = (nnodes() <= 1 ? 4 : 2);
		return nnodes() * Btree.NODESIZE;// / usage;
	}

	boolean insert(Transaction tran, Slot x) {
		if (iskey || (unique && !isEmpty(x.key)))
			if (find(tran, stripAddress(x.key)) != null)
				return false;
		return bt.insert(x);
	}

	static Record stripAddress(Record key) {
		// PERF avoid dup - maybe some kind of slice/view
		return key.dup().truncate(key.size() - 1);
	}

	boolean remove(Record key) {
		return bt.remove(key);
	}

	boolean isValid() {
		return bt.isValid();
	}

	float rangefrac(Record from, Record to) {
		float f = bt.rangefrac(from, to);
		return f < .001 ? (float) .001 : f;
	}

	Slot find(Transaction tran, Record key) {
		Iter iter = iter(key);
		iter.next();
		if (iter.eof())
			return null;
		Slot cur = iter.cur();
		return cur.key.hasPrefix(key) ? cur : null;
	}

	static boolean isEmpty(Record key) {
		int n = key.size() - 1; // - 1 to ignore record address at end
		if (n <= 0)
			return true;
		for (int i = 0; i < n; ++i)
			if (key.fieldSize(i) != 0)
				return false;
		return true;
	}

	Iter iter() {
		return new Iter(Record.MINREC, Record.MAXREC);
	}

	Iter iter(Record key) {
		return new Iter(key, key);
	}

	Iter iter(Record from, Record to) {
		return new Iter(from, to);
	}

	Iter iter(IndexIter iter) {
		return new Iter((Iter) iter);
	}

	// adds from/to range,
	// and prevsize to skip records added/updated during iteration
	class Iter implements IndexIter {
		Record from;
		Record to;
		boolean rewound = true;
		Btree.Iter iter = null;
		long prevsize = Long.MAX_VALUE;

		private Iter(Record from, Record to) {
			this.from = from;
			this.to = to;
		}

		private Iter(Iter iter) {
			from = iter.from;
			to = iter.to;
			this.iter = bt.atCur(iter.cur());
			prevsize = iter.prevsize;
			rewound = false;
		}

		@Override
		public boolean eof() {
			return iter.eof();
		}

		Slot cur() {
			verify(!rewound);
			return iter.cur();
		}

		@Override
		public Record curKey() {
			return cur().key;
		}

		@Override
		public int keyadr() {
			return cur().keyRecAdr();
		}

		long keyoff() {
			return cur().keyRecOff();
		}

		void reset_prevsize() {
			prevsize = Long.MAX_VALUE;
		}

		@Override
		public void next() {
			boolean first = true;
			Record prevkey = Record.MINREC;
			if (rewound) {
				iter = bt.locate(from);
				rewound = false;
			} else if (!iter.eof()) {
				prevkey = iter.key();
				first = false;
				iter.next();
			}
			while (!iter.eof() && iter.cur().keyRecOff() >= prevsize)
				iter.next();
			// PERF skip prefixgt if to is max
			if (!iter.eof() && iter.key().prefixgt(to))
				iter.seteof();
			if (!iter.eof() && (iskey || first || !eq(iter.key(), prevkey)))
				prevsize = dest.size();
		}

		@Override
		public void prev() {
			if (rewound) {
				iter = bt.locate(to.dup(8).addMax());
				if (iter.eof())
					iter = bt.last();
				else
					while (!iter.eof() && iter.key().prefixgt(to))
						iter.prev();
				rewound = false;
			} else if (!iter.eof())
				iter.prev();
			prevsize = dest.size();
			if (!iter.eof() && iter.key().compareTo(from) < 0)
				iter.seteof();
		}

	}

	/**
	 * @return True if the records are equal not including the last field.
	 */
	private static boolean eq(Record r1, Record r2) {
		int n = r1.size() - 1;
		if (n != r2.size() - 1)
			return false;
		for (int i = 0; i < n; ++i)
			if (r1.getRaw(i) != r2.getRaw(i))
				return false;
		return true;
	}

	@Override
	public String toString() {
		return Objects.toStringHelper(this)
		       .add("tblnum", tblnum)
		       .add("columns", columns)
		       .addValue(bt.toString())
		       .toString();
	}
}
