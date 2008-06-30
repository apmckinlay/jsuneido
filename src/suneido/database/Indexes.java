package suneido.database;

import java.util.ArrayList;
import java.util.Iterator;

public class Indexes implements Iterable<Idx> {
	private final ArrayList<Idx> indexes = new ArrayList<Idx>();

	public void add(Idx idx) {
		indexes.add(idx);
	}

	public Iterator<Idx> iterator() {
		return indexes.iterator();
	}

	public boolean isEmpty() {
		return indexes.isEmpty();
	}

	public boolean hasIndex(String columns) {
		for (Idx idx : indexes)
			if (idx.columns.equals(columns))
				return true;
		return false;
	}

	public Idx first() {
		return indexes.get(0);
	}
}
