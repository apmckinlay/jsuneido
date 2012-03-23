/* Copyright 2012 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.immudb;

import static suneido.immudb.ChunkedStorage.align;

import java.nio.ByteBuffer;
import java.util.Date;
import java.util.Iterator;

/** for debugging - prints info about raw data */
public class Dump2 {

	static void dump(Storage dstor, Storage istor) {
		System.out.println("index -----------------------------");
		Dump2.index(istor);
		System.out.println("data ------------------------------");
		Dump2.data(dstor);
	}

	static void data(Storage stor) {
		Iterator<ByteBuffer> iter = stor.iterator(Storage.FIRST_ADR);
		ByteBuffer buf = iter.next();
		do {
			buf = data1(stor, iter, buf);
			if (buf.remaining() == 0)
				if (iter.hasNext())
					buf = iter.next();
				else
					break;
		} while (buf.remaining() > 0);
	}

	private static ByteBuffer data1(
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
		int cksum = buf.getInt(); // checksum
		System.out.println("checksum " + Integer.toHexString(cksum));
		int size2 = buf.getInt();
		assert size2 == size;

		return buf;
	}

	static void index(Storage stor) {
		try {
			for (Check2.Iter iter = new Check2.Iter(stor, Storage.FIRST_ADR);
					! iter.eof(); iter.advance()) {
				System.out.println("size " + iter.size() +
						" cksum " + Integer.toHexString(iter.cksum()) +
						" date " + new Date(1000L * iter.date()) +
						" info " + iter.info());
			}
		} catch (Throwable e) {
			System.out.println(e);
		}
	}

}
