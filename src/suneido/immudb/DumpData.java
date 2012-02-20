/* Copyright 2012 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.immudb;

import static suneido.immudb.ChunkedStorage.align;

import java.nio.ByteBuffer;
import java.util.Date;
import java.util.Iterator;

public class DumpData {

	static void dump(Storage stor) {
		Iterator<ByteBuffer> iter = stor.iterator(Storage.FIRST_ADR);
		ByteBuffer buf = iter.next();
		do {
			buf = dump1(stor, iter, buf);
			if (buf.remaining() == 0)
				if (iter.hasNext())
					buf = iter.next();
				else
					break;
		} while (buf.remaining() > 0);
	}

	private static ByteBuffer dump1(
			Storage stor, Iterator<ByteBuffer> iter, ByteBuffer buf) {
		// header - one chunk
		int size = buf.getInt();
		int datetime = buf.getInt();
		System.out.println("size " + size +
				" date " + new Date(1000L * datetime));

		// added records - one chunk per record
		while (true) {
			short tblnum = buf.getShort();
			if (tblnum == 0) {
				buf = iter.next();
				continue;
			}
			if (tblnum == -1)
				break;
			Record r = new BufRecord(buf.slice());
			System.out.println("add table " + tblnum + " " + r);
			buf.position(align(buf.position() + r.bufSize()));
			if (buf.remaining() == 0)
				buf = iter.next();
		}

		// removes - one chunk
		int nremoves = buf.getInt();
		for (int i = 0; i < nremoves; ++i) {
			int adr = buf.getInt();
			Record r = new BufRecord(stor, adr);
			System.out.println("remove " + r);
		}
		buf.position(align(buf.position()));
		if (buf.remaining() == 0)
			buf = iter.next();

		// trailer - one chunk
		buf.getInt(); // checksum
		int size2 = buf.getInt();
		assert size2 == size;

		return buf;
	}

}
