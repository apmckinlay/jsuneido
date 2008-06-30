package suneido.database;

import java.util.ArrayList;
import java.util.Iterator;

/**
 * 
 * @author Andrew McKinlay
 * <p><small>Copyright 2008 Suneido Software Corp. All rights reserved. Licensed under GPLv2.</small></p>
 */
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
		return find(columns) != null;
	}

	public Index find(String columns) {
		for (Index index : indexes)
			if (columns.equals(index.columns))
				return index;
		return null;
	}

	public Index first() {
		return indexes.get(0);
	}

	public int size() {
		return indexes.size();
	}
}
