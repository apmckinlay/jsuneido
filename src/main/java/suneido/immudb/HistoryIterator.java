/* Copyright 2012 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.immudb;

import static suneido.immudb.UpdateTransaction.REMOVE;

import java.nio.ByteBuffer;
import java.util.Date;

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
	private Date date;
	private IntArraysList rlist;
	private int ri;
	private int adr;

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
			if (rewound) {
				rewound = false;
				adr = Storage.FIRST_ADR;
			} else {
				long size = Storage.intToSize(dstor.buffer(adr).getInt());
				adr = dstor.advance(adr, size);
				if (!dstor.isValidAdr(adr))
					return false; // eof
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
			adr = dstor.upTo();
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
			if (adr == Storage.FIRST_ADR)
				return false; // eof
			int size;
			while (0 == (size = getPrevSize(adr)))
				--adr; // skip end of chunk padding
			adr -= size;
			readList();
		} while (date == null || rlist.size() == 0); // skip aborted or empty
		ri = rlist.size();
		return true;
	}

	int getPrevSize(int adr) {
		ByteBuffer buf = dstor.buffer(adr - 1);
		buf.getInt(); // skip checksum
		return buf.getInt();
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
		new Proc(dstor, adr).process();
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
