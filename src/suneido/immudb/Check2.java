/* Copyright 2012 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.immudb;

import java.nio.ByteBuffer;


class Check2 {

	/** iterate through data commits or index persists */
	static class Iter {
		final Storage stor;
		int adr;
		int size;
		int cksum;

		Iter(Storage stor) {
			this.stor = stor;
			seek(Storage.FIRST_ADR);
		}

		void seek(int adr) {
			this.adr = adr;
			if (eof())
				return ;
			ByteBuffer buf = stor.buffer(adr);
			size = buf.getInt();
			ByteBuffer endbuf = stor.buffer(stor.advance(adr, size - Tran.TAIL_SIZE));
			cksum = endbuf.getInt();
			int endsize = endbuf.getInt();
			if (endsize != size)
				throw new RuntimeException("storage size mismatch");
		}

		boolean eof() {
			return stor.sizeFrom(adr) == 0;
		}

		void advance() {
			seek(stor.advance(adr, size));
		}

		int adr() {
			return adr;
		}

		int cksum() {
			return cksum;
		}

		/** Only works on index store */
		Tran.StoreInfo info() {
			ByteBuffer buf = stor.buffer(stor.advance(adr,
					size - ChunkedStorage.align(Persist.ENDING_SIZE + Persist.TAIL_SIZE)));
			buf.getInt(); // skip dbinfo adr
			int lastcksum = buf.getInt();
			int lastadr = buf.getInt();
			return new Tran.StoreInfo(lastcksum, lastadr);
		}

		@Override
		public String toString() {
			return "Iter(adr " + adr + (eof() ? " eof" : "") + ")";
		}

	}

}
