package suneido.database;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

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
		return get(columns) != null;
	}

	public Index get(String columns) {
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

	public Index firstKey() {
		for (Index index : indexes)
			if (index.iskey())
				return index;
		return null;
	}

	public List<List<String>> columns() {
		ArrayList<List<String>> list = new ArrayList<List<String>>();
		for (Index index : indexes)
			list.add(Arrays.asList(index.columns.split(",")));
		return list;
	}

	public List<List<String>> keysColumns() {
		ArrayList<List<String>> list = new ArrayList<List<String>>();
		for (Index index : indexes)
			if (index.iskey())
				list.add(Arrays.asList(index.columns.split(",")));
		return list;
	}
}
