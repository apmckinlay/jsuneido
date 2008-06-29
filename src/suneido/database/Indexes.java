package suneido.database;

import java.util.ArrayList;
import java.util.Iterator;

public class Indexes implements Iterable<Index> {
	private final ArrayList<Index> indexes = new ArrayList<Index>();

	public Iterator<Index> iterator() {
		return indexes.iterator();
	}

	public boolean isEmpty() {
		return indexes.isEmpty();
	}
}
