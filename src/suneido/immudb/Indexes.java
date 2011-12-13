/* Copyright 2011 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.immudb;

import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import javax.annotation.concurrent.Immutable;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;

@Immutable
class Indexes implements Iterable<Index> {
	final ImmutableList<Index> indexes;

	Indexes(ImmutableList<Index> indexes) {
		this.indexes = indexes;
	}

	@Override
	public Iterator<Index> iterator() {
		return indexes.iterator();
	}

	boolean isEmpty() {
		return indexes.isEmpty();
	}

	Index first() {
		return indexes.get(0);
	}

	Index getIndex(int[] colNums) {
		for (Index index : indexes)
			if (Arrays.equals(colNums, index.colNums))
				return index;
		return null;
	}

	int size() {
		return indexes.size();
	}

	Index firstKey() {
		for (Index index : indexes)
			if (index.isKey())
				return index;
		return null;
	}

	List<List<String>> columns(List<String> fields) {
		return columns(fields, false);
	}

	List<List<String>> keysColumns(List<String> fields) {
		return columns(fields, true);
	}

	private List<List<String>> columns(List<String> fields, boolean justKeys) {
		ImmutableList.Builder<List<String>> list = ImmutableList.builder();
		for (Index index : indexes)
			if (!justKeys || index.isKey())
				list.add(index.columns(fields));
				// note: can't use commasToList because it does "" => empty list
		return list.build();
	}

	@Override
	public String toString() {
		return "Indexes " + Iterables.toString(this);
	}

}
