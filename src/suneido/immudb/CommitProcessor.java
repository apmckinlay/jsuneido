/* Copyright 2012 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.immudb;

import static suneido.immudb.UpdateTransaction.END;
import static suneido.immudb.UpdateTransaction.REMOVE;
import static suneido.immudb.UpdateTransaction.UPDATE;

import java.nio.ByteBuffer;
import java.util.Date;

import com.google.common.primitives.Ints;
import com.google.common.primitives.Shorts;

abstract class CommitProcessor {
	private final Storage stor;
	protected final int commitAdr;
	private int adr;
	private DataRecord addrec;

	CommitProcessor(Storage stor, int adr) {
		this.stor = stor;
		this.commitAdr = adr;
		this.adr = adr;
	}

	void process() {
		if (stor.sizeFrom(adr) <= 0)
			return;
		ByteBuffer buf = stor.buffer(adr);
		buf.getInt(); // size
		int date = buf.getInt();
		if (date == 0) { // aborted
			date(null);
			return;
		}
		date(new Date(1000L * date));
		buf = advance(Tran.HEAD_SIZE);

		char c = (char) buf.get();
		assert c == 'u' || c == 's' || c == 'b';
		type(c);
		buf = advance(1);

		int from = 0;
		while (true) {
			short b = buf.getShort();
			if (b == END)
				break;
			else if (b == REMOVE || b == UPDATE) {
				assert from == 0;
				int recadr = buf.getInt();
				if (b == REMOVE)
					remove(recadr);
				else // UPDATE
					from = recadr;
				buf = advance(Shorts.BYTES + Ints.BYTES);
			} else { // add
				addrec = new DataRecord(buf.slice());
				addrec.tblnum(b);
				if (from == 0)
					add(b, adr);
				else {
					update(from, adr);
					from = 0;
				}
				buf = advance(addrec.storSize());
			}
		}
		after();
	}

	private ByteBuffer advance(int n) {
		adr = stor.advance(adr, n);
		return stor.buffer(adr);
	}

	void type(char c) {
	}

	void date(Date date) {
	}

	void add(int tblnum, int adr) {
		add(addrec);
	}
	void add(DataRecord r) {
	}

	void update(int from, int to) {
		update(new DataRecord(stor, from), addrec);
	}
	void update(DataRecord from, DataRecord to) {
	}

	void remove(int adr) {
		remove(new DataRecord(stor, adr));
	}
	void remove(DataRecord r) {
	}

	void after() {
	}

}
