/* Copyright 2010 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.database.immudb;

import java.util.Iterator;

public abstract class Record implements Iterable<Data> {

	public abstract void add(Data buf);

	public abstract Data get(int i);

	/** @return the number of data elements */
	public abstract int size();

	@Override
	public Iterator<Data> iterator() {
		return new Iter();
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
