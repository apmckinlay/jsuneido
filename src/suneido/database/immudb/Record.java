/* Copyright 2010 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.database.immudb;

import java.util.AbstractList;
import java.util.Iterator;

import com.google.common.collect.Ordering;

/**
 * Abstract base class for database records.
 * Field values are {@link Data} sub-classes.
 * Record also extends Data so that records can be stored in records.
 */
public abstract class Record extends AbstractList<Data> implements Data {

	@Override
	public abstract boolean add(Data buf);

	@Override
	public abstract Data get(int i);

	/** @return the number of data elements */
	@Override
	public abstract int size();

	@Override
	public Iterator<Data> iterator() {
		return new Iter();
	}

	@Override
	public boolean equals(Object that) {
		if (this == that)
			return true;
		if (that instanceof Record)
			return 0 == cmp.compare(this, (Record) that);
		return false;
	}

	public static final Ordering<Iterable<Data>> cmp =
		Ordering.natural().lexicographical();

	@Override
	public int compareTo(Data that) {
		return cmp.compare(this, (Record) that);
	}

	private class Iter implements Iterator<Data> {
		private int i = 0;

		@Override
		public boolean hasNext() {
			return i < size();
		}

		@Override
		public Data next() {
			return get(i++);
		}

		@Override
		public void remove() {
			throw new UnsupportedOperationException("Record iterator does not support remove");
		}

	}

}
