/* Copyright 2012 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.immudb;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.nio.ByteBuffer;

import suneido.util.Util;

/** for debugging - prints info about file contents */
public class Dump {

	public static void dump() {
		Database db = Database.openReadonly("suneido.db");
	    db.dump(true);
	    db.close();
	    System.err.println("dumped suneido.dbd to dump-data.txt and "
	    		+ "suneido.dbi to dump-index.txt");
	}

	/** dump the entire contents */
	static void dump(Storage dstor, Storage istor, boolean detail) {
		try {
			dump(dstor, Storage.FIRST_ADR, istor, Storage.FIRST_ADR, detail);
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
	}

	/** dump from the specified addresses to the end */
	static void dump(Storage dstor, int dAdr, Storage istor, int iAdr, boolean detail)
			throws FileNotFoundException {
	    System.setOut(new PrintStream(new FileOutputStream("dump-index.txt")));
		indexFrom(istor, iAdr);
		System.out.close();
	    System.setOut(new PrintStream(new FileOutputStream("dump-data.txt")));
		dataFrom(dstor, dAdr, detail);
	}

	/** dump the last few commits */
	static void ending(Storage dstor, Storage istor, boolean detail)
			throws FileNotFoundException {
		int iAdr = findLast(istor, 4);
		int dAdr = findLast(dstor, 8);
		dump(dstor, dAdr, istor, iAdr, detail);
	}

	/** Scan backwards to find the n'th last commit */
	static int findLast(Storage stor, int nBlocks) {
		StorageIterReverse iter = new StorageIterReverse(stor);
		int adr = 0;
		for (int n = 0; n < nBlocks && iter.hasPrev(); ++n)
			adr = iter.prev();
		return adr;
	}

	static void indexFrom(Storage istor, int iAdr) {
		for (StorageIter iter = new StorageIter(istor, iAdr).dontChecksum();
				! iter.eof(); iter.advance2()) {
			assert iter.status() == StorageIter.Status.OK : "CORRUPT!";
			System.out.println(Storage.adrToOffset(iter.adr()) + ":" +
					" size " + iter.size() +
					" date " + Util.displayDate(iter.date()) +
					" checksum " + Integer.toHexString(iter.cksum()) +
					(iter.verifyChecksum() ? "" : " CHECKSUM MISMATCH") +
					"\n\t" + Check.info(istor, iter.adr(), iter.size()));
		}
	}

	static void dataFrom(Storage dstor, int dAdr, boolean detail) {
		for (StorageIter iter = new StorageIter(dstor, dAdr).dontChecksum();
				! iter.eof(); iter.advance2()) {
			ByteBuffer buf = dstor.buffer(iter.adr());
			int typeAdr = dstor.advance(iter.adr(), Tran.HEAD_SIZE);
			buf = dstor.buffer(typeAdr);
			char type = (char) buf.get();
			System.out.println(Storage.adrToOffset(iter.adr()) + ":" +
					" type " + type +
					" size " + iter.size() +
					" date " + (iter.date() == null ? "ABORTED" : Util.displayDate(iter.date())) +
					" checksum " + Integer.toHexString(iter.cksum()) +
					(iter.date() == null || iter.verifyChecksum() ? "" : " CHECKSUM MISMATCH"));
			if (detail)
				new Proc(dstor, iter.adr()).process();
		}
	}

	private static class Proc extends CommitProcessor {
		Proc(Storage stor, int adr) {
			super(stor, adr);
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
			System.out.println("===");
		}
		@Override
		void update(DataRecord from, DataRecord to) {
			System.out.println("update " + from.tblnum() + " " + from);
			System.out.println("    to " + to);
		}
	}

//	public static void main(String[] args) throws FileNotFoundException {
//		dump();
////		ending(db.dstor, db.istor, false);
//	}

}
