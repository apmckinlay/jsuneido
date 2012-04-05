/* Copyright 2012 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.immudb;

import java.nio.ByteBuffer;

import com.google.common.primitives.Ints;

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

		// removes - one chunk
		char c = (char) buf.getShort();
		assert c == 'u' || c == 's';
		type(c);
		int nremoves = buf.getShort();
		for (int i = 0; i < nremoves; ++i)
			remove(new BufRecord(stor, buf.getInt()));
		buf = advance((nremoves + 1) * Ints.BYTES);

		// added records - one chunk per record
		while (true) {
			short tblnum = buf.getShort();
			if (tblnum == -1)
				break;
			assert tblnum > 0;
			Record r = new BufRecord(buf.slice());
			r.tblnum = tblnum;
			add(r);
			buf = advance(r.storSize());
		}
		after();
	}

	private ByteBuffer advance(int n) {
		adr = stor.advance(adr, n);
		return stor.buffer(adr);
	}

	abstract void type(char c);

	abstract void remove(Record r);

	abstract void add(Record r);

	abstract void after();

}
