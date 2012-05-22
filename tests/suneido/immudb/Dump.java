/* Copyright 2012 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.immudb;

import suneido.language.Ops;

/** for debugging - prints info about file contents */
class Dump {

	static void dump(Storage dstor, Storage istor) {
		System.out.println("index -----------------------------");
		Dump.index(istor);
		System.out.println("data ------------------------------");
		Dump.data(dstor);
	}

	static void data(Storage dstor) {
		for (StorageIter iter = new StorageIter(dstor); ! iter.eof(); iter.advance2()) {
			System.out.println("size " + iter.size +
					" date " + Ops.display(iter.date()) +
					" checksum " + Integer.toHexString(iter.cksum()));
			new Proc(dstor, iter.adr).process();
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
		void remove(Record r) {
			System.out.println("remove " + r.tblnum + " - " + r);
		}
		@Override
		void add(Record r) {
			System.out.println("add " + r.tblnum + " + " + r);
		}
		@Override
		void after() {
			System.out.println("--------------------------");
		}
		@Override
		void update(Record from, Record to) {
			System.out.println("update " + from.tblnum + " " + from);
			System.out.println("    to " + to);
		}
	}

	static void index(Storage istor) {
		for (StorageIter iter = new StorageIter(istor); ! iter.eof(); iter.advance2()) {
			assert iter.status == StorageIter.Status.OK : "CORRUPT!";
			System.out.println("size " + iter.size() +
					" cksum " + Integer.toHexString(iter.cksum()) +
					" date " + Ops.display(iter.date()) +
					" info " + Check.info(istor, iter.adr, iter.size));
		}
	}

	public static void main(String[] args) {
//		Database db = DatabasePackage.dbpkg.testdb();
		Database db = DatabasePackage.dbpkg.openReadonly("suneido.db");
		dump(db.dstor, db.istor);
//		data(db.dstor);
	}

}
