/* Copyright 2010 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.database.immudb;

import java.util.*;

import com.google.common.collect.Ordering;

/**
 * Abstract base class for database records.
 * Field values are {@link Data} sub-classes.
 * Record also extends Data so that records can be stored in records.
 */
public abstract class Record extends Data implements List<Data> {

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
	public int compareTo(Data other) {
//		return cmp.compare(this, (Record) that);
		Record that = (Record) other;
		int len1 = this.size();
		int len2 = that.size();
		int n = Math.min(len1, len2);
		for (int i = 0; i < n; ++i) {
			int cmp = this.get(i).compareTo(that.get(i));
			if (cmp != 0)
				return cmp;
		}
		return len1 - len2;
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

	@Override
	public void add(int index, Data element) {
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean addAll(Collection<? extends Data> c) {
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean addAll(int index, Collection<? extends Data> c) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void clear() {
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean contains(Object o) {
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean containsAll(Collection<?> c) {
		throw new UnsupportedOperationException();
	}

	@Override
	public int indexOf(Object o) {
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean isEmpty() {
		throw new UnsupportedOperationException();
	}

	@Override
	public int lastIndexOf(Object o) {
		throw new UnsupportedOperationException();
	}

	@Override
	public ListIterator<Data> listIterator() {
		throw new UnsupportedOperationException();
	}

	@Override
	public ListIterator<Data> listIterator(int index) {
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean remove(Object o) {
		throw new UnsupportedOperationException();
	}

	@Override
	public Data remove(int index) {
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean removeAll(Collection<?> c) {
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean retainAll(Collection<?> c) {
		throw new UnsupportedOperationException();
	}

	@Override
	public Data set(int index, Data element) {
		throw new UnsupportedOperationException();
	}

	@Override
	public List<Data> subList(int fromIndex, int toIndex) {
		throw new UnsupportedOperationException();
	}

	@Override
	public Object[] toArray() {
		throw new UnsupportedOperationException();
	}

	@Override
	public <T> T[] toArray(T[] a) {
		throw new UnsupportedOperationException();
	}

}
