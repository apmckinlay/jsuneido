/* Copyright 2012 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.immudb;

import static suneido.immudb.ReadWriteTransaction.END;
import static suneido.immudb.ReadWriteTransaction.REMOVE;
import static suneido.immudb.ReadWriteTransaction.UPDATE;

import java.nio.ByteBuffer;

import com.google.common.primitives.Ints;
import com.google.common.primitives.Shorts;

abstract class CommitProcessor {
	private final Storage stor;
	protected final int commitAdr;
	private int adr;

	CommitProcessor(Storage stor, int adr) {
		this.stor = stor;
		this.commitAdr = adr;
		this.adr = adr;
	}

	void process() {
		if (stor.sizeFrom(adr) <= 0)
			return;
		ByteBuffer buf = stor.buffer(adr);
		buf = advance(Tran.HEAD_SIZE);

		char c = (char) buf.get();
		assert c == 'u' || c == 's' || c == 'b';
		type(c);
		buf = advance(1);

		Record from = null;
		while (true) {
			short b = buf.getShort();
			if (b == END)
				break;
			else if (b == REMOVE || b == UPDATE) {
				BufRecord r = new BufRecord(stor, buf.getInt());
				if (b == REMOVE)
					remove(r);
				else
					from = r;
				buf = advance(Shorts.BYTES + Ints.BYTES);
			} else { // add
				Record r = new BufRecord(buf.slice());
				r.tblnum = b;
				if (from == null)
					add(r);
				else {
					update(from, r);
					from = null;
				}
				buf = advance(r.storSize());
			}
		}
		after();
	}

	private ByteBuffer advance(int n) {
		adr = stor.advance(adr, n);
		return stor.buffer(adr);
	}

	abstract void type(char c);

	abstract void update(Record from, Record to);

	abstract void remove(Record r);

	abstract void add(Record r);

	abstract void after();

}
