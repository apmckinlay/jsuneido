/* Copyright 2011 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.database.immudb.schema;

import java.util.Iterator;

import javax.annotation.concurrent.Immutable;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;

@Immutable
public class Indexes implements Iterable<Index> {
	final ImmutableList<Index> indexes;

	public Indexes(ImmutableList<Index> indexes) {
		this.indexes = indexes;
	}

	public Iterator<Index> iterator() {
		return indexes.iterator();
	}

	public boolean isEmpty() {
		return indexes.isEmpty();
	}

//	public boolean hasIndex(String columns) {
//		return get(columns) != null;
//	}

//	public Index get(String columns) {
//		for (Index index : indexes)
//			if (columns.equals(index.colNums))
//				return index;
//		return null;
//	}

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

//	public List<List<String>> columns() {
//		return columns(false);
//	}
//
//	public List<List<String>> keysColumns() {
//		return columns(true);
//	}

//	private List<List<String>> columns(boolean justKeys) {
//		ImmutableList.Builder<List<String>> list = ImmutableList.builder();
//		for (Index index : indexes)
//			if (!justKeys || index.isKey())
//				list.add(ImmutableList.copyOf(commaSplitter.split(index.columns)));
//				// note: can't use commasToList because it does "" => empty list
//		return list.build();
//	}

	@Override
	public String toString() {
		return "Indexes " + Iterables.toString(this);
	}

}
