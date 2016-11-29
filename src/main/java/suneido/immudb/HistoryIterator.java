/* Copyright 2012 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.immudb;

import static suneido.immudb.UpdateTransaction.REMOVE;

//WHY is this using end relative (negative) positions? (rpos)

import java.util.Date;

import com.google.common.primitives.Ints;

import suneido.SuDate;
import suneido.intfc.database.Record;
import suneido.util.IntArraysList;

/**
 * Each commit is read into memory as a list of addresses.
 * Note: This could be quite large for bulk transactions like an initial load.
 * This isn't necessary for reading forwards
 * but to handle changing direction it's easier to always do it.
 * Similar to {@link StorageIterator} but bidirectional.
 */
class HistoryIterator implements suneido.intfc.database.HistoryIterator {
	private final Storage dstor;
	private final int tblnum;
	private boolean rewound;
	private long rpos;
	private Date date;
	private IntArraysList rlist;
	private int ri;
	private static final long START = 1; // special rpos value

	HistoryIterator(Storage dstor, int tblnum) {
		this.dstor = dstor;
		this.tblnum = tblnum;
		rewind();
	}

	@Override
	public void rewind() {
		rewound = true;
		rlist = new IntArraysList();
		ri = -1;
	}

	@Override
	public Record[] getNext() {
		if (rewound) {
			rewound = false;
			rpos = START;
		}
		while (true) {
			if (ri + 1 >= rlist.size())
				if (! nextCommit()) {
					rewind();
					return null; // eof
				}
			int adr = rlist.get(++ri);
			if (adr == REMOVE) {
				adr = rlist.get(++ri);
				return result("delete", new DataRecord(dstor, adr));
				// create half of update will be handled next time as add
			} else // add
				return result("create", new DataRecord(dstor, adr));
		}
	}

	private boolean nextCommit() {
		do {
			if (rpos == START)
				rpos = -dstor.sizeFrom(0) + Storage.ALIGN;
			else {
				long size = Storage.intToSize(dstor.rbuffer(rpos).getInt());
				rpos += size;
				if (rpos >= 0)
					return false; // eof
				while (0 == dstor.rbuffer(rpos).getLong())
					rpos += Long.BYTES; // skip end of chunk padding
			}
			readList();
		} while (date == null || rlist.size() == 0); // skip aborted or empty
		ri = -1;
		return true;
	}

	@Override
	public Record[] getPrev() {
		if (rewound) {
			rewound = false;
			rpos = 0;
		}
		while (true) {
			if (ri <= 0) {
				if (! prevCommit()) {
					rewind();
					return null; // eof
				}
				assert ri > 0;
			}
			int adr = rlist.get(--ri);
			if (ri > 0 && rlist.get(ri - 1) == REMOVE) {
				--ri;
				return result("delete", new DataRecord(dstor, adr));
			} else // add
				return result("create", new DataRecord(dstor, adr));
		}
	}

	private boolean prevCommit() {
		do {
			if (! dstor.isValidPos(rpos - Ints.BYTES))
				return false; // eof
			int size;
			while (0 == (size = dstor.rbuffer(rpos - Ints.BYTES).getInt()))
				rpos -= Ints.BYTES; // skip end of chunk padding
			rpos -= Storage.intToSize(size);
			readList();
		} while (date == null || rlist.size() == 0); // skip aborted or empty
		ri = rlist.size();
		return true;
	}

	private Record[] result(String action, DataRecord r) {
		Record r1 = new RecordBuilder().
				add(SuDate.fromTime(date.getTime())).
				add(action).build();
		return new Record[] { r1, r };
	}

	/**
	 * read the contents of the commit into a list so we can iterate in reverse
	 * (can't iterate through stored commit in reverse)
	 */
	private void readList() {
		rlist = new IntArraysList();
		new Proc(dstor, dstor.rposToAdr(rpos)).process();
	}

	private class Proc extends CommitProcessor {
		Proc(Storage stor, int adr) {
			super(stor, adr);
		}
		@Override
		void date(Date d) {
			date = d; // used to skip aborted commits
		}
		@Override
		void add(int tn, int adr) {
			if (tn == tblnum)
				rlist.add(adr);
		}
		@Override
		void remove(int adr) {
			if (tableMatches(adr)) {
				rlist.add(REMOVE);
				rlist.add(adr);
			}
		}
		@Override
		void update(int from, int to) {
			if (tableMatches(from)) {
				// handle as remove and then add
				rlist.add(REMOVE);
				rlist.add(from);
				rlist.add(to);
			}
		}
		boolean tableMatches(int adr) {
			return dstor.buffer(adr).getShort() == tblnum;
		}
	}

}
