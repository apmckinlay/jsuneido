package suneido.database.query;

import suneido.database.Record;

public class Keyrange {
	Record org;
	Record end;

	public Keyrange() {
		org = Record.MINREC;
		end = Record.MAXREC;
	}

	public Keyrange(Record org, Record end) {
		this.org = org;
		this.end = end;
	}

	public boolean isEmpty() {
		return org.compareTo(end) > 0;
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
}
