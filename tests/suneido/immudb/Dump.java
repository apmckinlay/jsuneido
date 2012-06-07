/* Copyright 2012 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.immudb;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.nio.ByteBuffer;

import suneido.language.Ops;

import com.google.common.primitives.Ints;

/** for debugging - prints info about file contents */
class Dump {
	private static final int MIN_SIZE = Tran.HEAD_SIZE + Tran.TAIL_SIZE;

	static void dump(Storage dstor, Storage istor) {
		try {
			dump(dstor, Storage.FIRST_ADR, istor, Storage.FIRST_ADR);
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
	}

	static void dump(Storage dstor, int dAdr, Storage istor, int iAdr)
			throws FileNotFoundException {
	    System.setOut(new PrintStream(new FileOutputStream("dump-index.txt")));
		indexFrom(istor, iAdr);
		System.out.close();
	    System.setOut(new PrintStream(new FileOutputStream("dump-data.txt")));
		dataFrom(dstor, dAdr, false);
	}

	static void ending(Storage dstor, Storage istor) throws FileNotFoundException {
		int iAdr = findLast(istor, 4);
		int dAdr = findLast(dstor, 8);
		dump(dstor, dAdr, istor, iAdr);
	}

	/** Scan backwards to find the n'th last persist */
	static int findLast(Storage stor, int nChunks) {
		long fileSize = stor.sizeFrom(0);
		int pos = 0; // negative offset from end of file
		int n = 0;
		int size = 0;
		while (n < nChunks && fileSize + pos > MIN_SIZE) {
			ByteBuffer buf = stor.buffer(pos - Ints.BYTES);
			size = buf.getInt();
			pos -= size;
			++n;
		}
		return pos;
	}

	static void indexFrom(Storage istor, int iAdr) {
		for (StorageIter iter = new StorageIter(istor, iAdr); ! iter.eof(); iter.advance2()) {
			assert iter.status() == StorageIter.Status.OK : "CORRUPT!";
			System.out.println(Storage.adrToOffset(iter.adr()) +
					" size " + iter.size() +
					" cksum " + Integer.toHexString(iter.cksum()) +
					" date " + Ops.display(iter.date()) +
					"\n\t" + Check.info(istor, iter.adr(), iter.size()));
		}
	}

	static void dataFrom(Storage dstor, int dAdr, boolean detail) {
		for (StorageIter iter = new StorageIter(dstor, dAdr); ! iter.eof(); iter.advance2()) {
			ByteBuffer buf = dstor.buffer(iter.adr());
			char c = (char) buf.get(Tran.HEAD_SIZE);
			System.out.println(Storage.adrToOffset(iter.adr()) +
					" type " + c +
					" size " + iter.size() +
					" date " + Ops.display(iter.date()) +
					" checksum " + Integer.toHexString(iter.cksum()));
			if (detail)
				new Proc(dstor, iter.adr()).process();
		}
	}

	private static class Proc extends CommitProcessor {
		Proc(Storage stor, int adr) {
			super(stor, adr);
		}
		@Override
		void type(char c) {
			System.out.println("type " + c);
		}
		@Override
		void remove(DataRecord r) {
			System.out.println("remove " + r.tblnum() + " - " + r);
		}
		@Override
		void add(DataRecord r) {
			System.out.println("add " + r.tblnum() + " + " + r);
		}
		@Override
		void after() {
			System.out.println("--------------------------");
		}
		@Override
		void update(DataRecord from, DataRecord to) {
			System.out.println("update " + from.tblnum() + " " + from);
			System.out.println("    to " + to);
		}
	}

	public static void main(String[] args) throws FileNotFoundException {
		Database db = Database.openReadonly("suneido.db");
	    db.dump();
//		ending(db.dstor, db.istor);
	}

}
