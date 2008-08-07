package suneido.database.query;

import suneido.database.Record;

public class Keyrange {
	/* package */Record org;
	/* package */Record end;

	public Keyrange() {
		setAll();
	}

	public Keyrange(Record org, Record end) {
		set(org, end);
	}

	public boolean isEmpty() {
		return org.compareTo(end) > 0;
	}

	public void set(Record org, Record end) {
		this.org = org;
		this.end = end;
	}

	public void setAll() {
		org = Record.MINREC;
		end = Record.MAXREC;
	}

	public void setNone() {
		org = Record.MAXREC;
		end = Record.MINREC;
	}

	public boolean equals(Record org, Record end) {
		return org.equals(this.org) && end.equals(this.end);
	}

	@Override
	public boolean equals(Object other) {
		if (this == other)
			return true;
		if (!(other instanceof Keyrange))
			return false;
		Keyrange kr = (Keyrange) other;
		return org.equals(kr.org) && end.equals(kr.org);
	}

	@Override
	public int hashCode() {
		return org.hashCode() + end.hashCode();
	}

	public boolean contains(Record key) {
		return org.compareTo(key) <= 0 && end.compareTo(key) >= 0;
	}

	public static Keyrange intersect(Keyrange r1, Keyrange r2) {
		return new Keyrange(min(r1.org, r2.org), max(r1.end, r2.end));
	}

	public static Record min(Record r1, Record r2) {
		return r1.compareTo(r2) <= 0 ? r1 : r2;
	}

	public static Record max(Record r1, Record r2) {
		return r1.compareTo(r2) >= 0 ? r1 : r2;
	}

}
