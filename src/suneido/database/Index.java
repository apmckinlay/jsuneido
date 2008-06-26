package suneido.database;

import static suneido.Suneido.verify;

/**
 * Wraps a {@link Btree} to implement database table indexes.
 * Adds transaction stuff.
 * @author Andrew McKinlay
 * <p><small>Copyright 2008 Suneido Software Corp. All rights reserved. Licensed under GPLv2.</small></p>
 */
public class Index {
	Destination dest;
	Btree bt;
	boolean iskey;
	boolean unique;
	int tblnum;
	String index;
	
	/**
	 * Create a new index.
	 */
	public Index(Destination dest, int tblnum, String index, boolean iskey, boolean unique) {
		init(dest, tblnum, index, iskey, unique);
		bt = new Btree(dest);
	}
	/**
	 * Open an existing index.
	 */
	public Index(Destination dest, int tblnum, String index, boolean iskey, boolean unique,
			long root, int treelevels, int nnodes) {
		init(dest, tblnum, index, iskey, unique);
		bt = new Btree(dest, root, treelevels, nnodes);
	}
	private void init(Destination dest, int tblnum,
			String index, boolean iskey, boolean unique) {
		this.dest = dest;
		this.iskey = iskey;
		this.unique = unique;
		this.tblnum = tblnum;
		this.index = index;
	}
	
	long root() {
		return bt.root();
	}
	int nnodes() {
		return bt.nnodes();
	}
	int treelevels() {
		return bt.treelevels();
	}
	
	boolean insert(int tran, Slot x) {
//		if (lower)
//			lower_key(x.key);
		if (iskey || (unique && ! empty(x.key))) {
			Record key = x.key;
			key.reuse(key.size() - 1); // strip off record address
			boolean dup = ! find(tran, key).isEmpty();
			key.reuse(key.size() + 1); // put is back
			if (dup)
				return false;
			}
		return bt.insert(x);		
	}
	
	boolean erase(Record key) {
		return bt.erase(key);
	}
	
	float rangefrac(Record from, Record to) {
		float f = bt.rangefrac(from, to);
		return f < .001 ? (float) .001 : f;
	}
	
	Slot find(int tran, Record key) {
		Iter iter = iter(tran, key).next();
		if (iter.eof())
			return new Slot();
		Slot cur = iter.cur();
		return cur.key.hasPrefix(key) ? cur : new Slot();
	}

	private boolean empty(Record key) {
		int n = key.size() - 1; // - 1 to ignore record address at end
		if (n <= 0)
			return true;
		for (int i = 0; i < n; ++i)
			if (key.fieldSize(i) != 0)
				return false;
		return true;
	}
	
	Iter iter(int tran) {
		return new Iter(tran, Record.MINREC, Record.MAXREC); 
	}
	Iter iter(int tran, Record key) {
		return new Iter(tran, key, key); 
	}
	Iter iter(int tran, Record from, Record to) {
		return new Iter(tran, from, to); 
	}
	
	public class Iter {
		int tran;
		Record from;
		Record to;
		boolean rewound = true;
		Btree.Iter iter;
		TranRead tranread;
		long prevsize = Long.MAX_VALUE;
		
		Iter(int tran, Record from, Record to) {
			this.tran = tran;
			this.from = from;
			this.to = to;
			tranread = dest.read_act(tran, tblnum, index);
		}
		
		boolean eof() {
			return iter.eof();
		}
		Slot cur() {
			verify(! rewound);
			return iter.cur();
		}

		Iter next() {
			boolean first = true;
			Record prevkey = Record.MINREC;
			if (rewound)
				{
				iter = bt.locate(from);
				rewound = false;
				tranread.org = from;
				}
			else if (! iter.eof())
				{
				prevkey = iter.key();
				first = false;
				iter.next();
				}
//			while (! iter.eof() && 
//				(mmoffset(iter.key()) >= prevsize || ! visible()))
//				iter.next();
			if (! iter.eof() && iter.key().prefixgt(to))
				iter.seteof();
			if (! iter.eof() && (iskey || first || ! eq(iter.key(), prevkey)))
				prevsize = dest.size();
			if (iter.eof())
				tranread.end = to;
			else if (iter.key().compareTo(tranread.end) > 0)
				tranread.end = iter.key();
			return this;
		}

		Iter prev() {
			if (rewound) {
				iter = bt.locate(to.dup(8).addMax());
				if (iter.eof())
					iter = bt.last();
				else
					while (! iter.eof() && iter.key().prefixgt(to))
						iter.prev();
				rewound = false;
				if (tranread != null)
					tranread.end = to;
			}	
			else if (! iter.eof())
				iter.prev();
			while (! iter.eof() && ! visible())
				iter.prev();
			prevsize = dest.size();
			if (! iter.eof() && iter.key().compareTo(from) < 0)
				iter.seteof();
			if (iter.eof())
				tranread.org = from;
			else if (iter.key().compareTo(tranread.org) < 0)
				tranread.org = iter.key();
			return this;
		}

		private boolean visible() {
			return dest.visible(tran, mmoffset(iter.key()));
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
			if (r1.getraw(i) != r2.getraw(i))
				return false;
		return true;
	}

	private long mmoffset(Record key) {
		return key.getMmoffset(key.size() - 1);
	}
}
