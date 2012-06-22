/* Copyright 2012 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

/*
QueryFirst('history(tables)')

QueryFirst('history(gl_transactions)')

QueryApply('tables') {|t| n = 0; QueryApply('history(' $ t.tablename $ ')') {|x| ++n}; Print(t.tablename,n) }
*/

package suneido.immudb;

import static suneido.immudb.UpdateTransaction.END;
import static suneido.immudb.UpdateTransaction.REMOVE;
import static suneido.immudb.UpdateTransaction.UPDATE;

import java.nio.ByteBuffer;

import suneido.intfc.database.Record;

import com.google.common.primitives.Ints;
import com.google.common.primitives.Shorts;

class HistoryIterator implements suneido.intfc.database.HistoryIterator {
	private final Storage dstor;
	private final int tblnum;
	private boolean rewound = true;
	private StorageIter iter;
	private int adr;
	private ByteBuffer buf;

	HistoryIterator(Storage dstor, int tblnum) {
		this.dstor = dstor;
		this.tblnum = tblnum;
	}

	@Override
	public void rewind() {
		rewound = true;
	}

	@Override
	public Record[] getNext() {
		if (rewound) {
			rewound = false;
			iter = new StorageIter(dstor, Storage.FIRST_ADR);
			buf = null;
		}
		while (! iter.eof()) { // iterate through commits
			if (buf == null) {
				adr = iter.adr();
				advance(Tran.HEAD_SIZE);
				advance(1); // commit type
			}
			while (true) { // iterate through adds/updates/deletes
				short b = buf.getShort();
				if (b == END) {
					iter.advance2();
					buf = null;
					break;
				} else if (b == REMOVE || b == UPDATE) {
					int recadr = buf.getInt();
					DataRecord r = new DataRecord(dstor, recadr);
					advance(Shorts.BYTES + Ints.BYTES);
					if (r.tblnum() != tblnum)
						continue;
					return result("delete", r);
					// create half of update will be handled next time as add
				} else { // add
					DataRecord r = new DataRecord(buf.slice());
					r.tblnum(b);
					advance(r.storSize());
					if (b != tblnum)
						continue;
					return result("create", r);
				}
			}
		}
		return null;
	}

	private Record[] result(String action, DataRecord r) {
		Record r1 = new RecordBuilder().add(iter.date()).add(action).build();
		return new Record[] { r1, r };
	}

	private void advance(int n) {
		adr = dstor.advance(adr, n);
		buf = dstor.buffer(adr);
	}

	@Override
	public Record[] getPrev() {
		if (rewound) {
			rewound = false;
//			iter = new StorageIter(dstor, Storage.LAST_ADR);
		}
		return null;
	}

}
