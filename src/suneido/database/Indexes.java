package suneido.database;

import java.util.ArrayList;
import java.util.Iterator;

public class Indexes implements Iterable<Index> {
	private final ArrayList<Index> indexes = new ArrayList<Index>();

	public void add(Index index) {
		indexes.add(index);
	}

	public Iterator<Index> iterator() {
		return indexes.iterator();
	}

	public boolean isEmpty() {
		return indexes.isEmpty();
	}

	public boolean hasIndex(String columns) {
		for (Index index : indexes)
			if (index.columns.equals(columns))
				return true;
		return false;
	}

	public Index first() {
		return indexes.get(0);
	}
}
