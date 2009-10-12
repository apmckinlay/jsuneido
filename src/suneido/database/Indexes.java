package suneido.database;

import static suneido.util.Util.commaSplitter;

import java.util.*;

import com.google.common.collect.Lists;

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
			if (index.isKey())
				return index;
		return null;
	}

	public List<List<String>> columns() {
		return columns(false);
	}

	public List<List<String>> keysColumns() {
		return columns(true);
	}

	private List<List<String>> columns(boolean justKeys) {
		ArrayList<List<String>> list = new ArrayList<List<String>>();
		for (Index index : indexes)
			if (!justKeys || index.isKey())
				list.add(Lists.newArrayList(commaSplitter.split(index.columns)));
		// note: can't use commasToList because it does "" => empty list
		return list;
	}
}
