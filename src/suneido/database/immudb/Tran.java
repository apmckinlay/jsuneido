/* Copyright 2010 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.database.immudb;

/**
 * Transaction "context".
 */
public class Tran {
	private MmapFile mmf;
	private final IntRefs intrefs = new IntRefs();
	private Redirects redirs;
	private final DataRecords datarecs = new DataRecords();

	public Tran() {
		redirs = new Redirects(DbHashTree.empty(this));
	}

	public MmapFile mmf() {
		return mmf;
	}
	public void mmf(MmapFile mmf) {
		this.mmf = mmf;
	}

	public int refToInt(Object ref) {
		return intrefs.refToInt(ref);
	}

	public int refRecordToInt(Record ref) {
		int intref = refToInt(ref);
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

	public void startPersist() {
		intrefs.startPersist();
	}

	public void setAdr(int intref, int adr) {
		intrefs.setAdr(intref, adr);
	}

	public int getAdr(int intref) {
		return intrefs.getAdr(intref);
	}

	public void persistDataRecords() {
		datarecs.persist(this);
	}

	public int persistRedirs() {
		return redirs.persist(this);
	}

}
