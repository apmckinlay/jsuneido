/* Copyright 2010 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.database.immudb;

/**
 * Transaction "context".
 */
public class Tran {
	private static final ThreadLocal<Tran> t = new ThreadLocal<Tran>() {
		@Override
		protected Tran initialValue() {
			return new Tran();
		};
	};
	private MmapFile mmf;
	private final IntRefs intrefs = new IntRefs();
	private Redirects redirs = new Redirects();

	private Tran() {
	}

	public static MmapFile mmf() {
		return t.get().mmf;
	}
	public static void mmf(MmapFile mmf) {
		t.get().mmf = mmf;
	}

	public static void remove() {
		t.remove();
	}

	public static int refToInt(Object ref) {
		return t.get().intrefs.refToInt(ref);
	}

	public static Object intToRef(int intref) {
		return t.get().intrefs.intToRef(intref);
	}

	public static int redir(int from) {
		return t.get().redirs.get(from);
	}

	public static void redir(int from, Object ref) {
		assert(! (ref instanceof Number));
		if (IntRefs.isIntRef(from))
			t.get().intrefs.update(from, ref);
		else
			t.get().redirs.put(from, refToInt(ref));
	}

	public static Redirects redirs() {
		return t.get().redirs;
	}

	public static void setRedirs(Redirects redirs) {
		t.get().redirs = redirs;
	}

	public static void startPersist() {
		t.get().intrefs.startPersist();
	}

	public static void setAdr(int intref, int adr) {
System.out.println("setAdr " + Integer.toHexString(intref) + " to " + Integer.toHexString(adr));
		t.get().intrefs.setAdr(intref, adr);
	}

	public static int getAdr(int intref) {
		return t.get().intrefs.getAdr(intref);
	}

}
