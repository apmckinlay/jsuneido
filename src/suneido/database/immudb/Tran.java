/* Copyright 2010 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.database.immudb;

/**
 * Transaction "context".
 */
public class Tran implements Translator {
	public final Storage stor;
	final IntRefs intrefs = new IntRefs();
	private Redirects redirs;
	private final DataRecords datarecs = new DataRecords();

	public Tran(Storage stor) {
		this.stor = stor;
		redirs = new Redirects(DbHashTree.empty(stor));
	}

	public int refToInt(Object ref) {
		return intrefs.refToInt(ref);
	}

	public int refRecordToInt(Record rec) {
		int intref = refToInt(rec);
		datarecs.add(intref);
		return intref;
	}

	public Object intToRef(int intref) {
		return intrefs.intToRef(intref);
	}

	public int redir(int from) {
		return redirs.get(from);
	}

	public void redir(int from, Object ref) {
		assert(! (ref instanceof Number));
		if (IntRefs.isIntRef(from))
			intrefs.update(from, ref);
		else
			redirs.put(from, refToInt(ref));
	}

	public Redirects redirs() {
		return redirs;
	}

	public void setRedirs(Redirects redirs) {
		this.redirs = redirs;
	}

	public void startStore() {
		intrefs.startStore();
	}

	public void setAdr(int intref, int adr) {
		intrefs.setAdr(intref, adr);
	}

	public int getAdr(int intref) {
		return intrefs.getAdr(intref);
	}

	public void storeDataRecords() {
		datarecs.store(this);
	}

	public int storeRedirs() {
		return redirs.store(stor, this);
	}

	@Override
	public int translate(int x) {
		return getAdr(x);
	}

}
