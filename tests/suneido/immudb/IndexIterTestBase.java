/* Copyright 2011 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.immudb;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import suneido.intfc.database.IndexIter;
import suneido.intfc.database.Record;

public abstract class IndexIterTestBase {

	protected static void testNext(IndexIter iter, int n) {
		iter.next();
		test(iter, n);
	}

	protected static void testPrev(IndexIter iter, int n) {
		iter.prev();
		test(iter, n);
	}

	protected static void test(IndexIter iter, int n) {
		if (n == -1)
			assertTrue(iter.eof());
		else
			assertThat(iter.curKey().getInt(0), is(n));
	}

	protected static void checkPrev(int[] result, IndexIter iter) {
		int i = result.length - 1;
		for (iter.prev(); ! iter.eof(); iter.prev(), --i)
			assertThat(iter.curKey().getInt(0), is(result[i]));
		assertThat(i, is(-1));
	}

	protected static void checkNext(int[] result, IndexIter iter) {
		int i = 0;
		for (iter.next(); ! iter.eof(); iter.next(), ++i)
			assertThat(iter.curKey().getInt(0), is(result[i]));
		assertThat(i, is(result.length));
	}

	protected static int[] a(int... a) {
		return a;
	}

	protected static IndexIter iter(int... ints) {
		Record[] recs = new Record[ints.length];
		for (int i = 0; i < ints.length; ++i)
			recs[i] = new RecordBuilder().add(ints[i]).build();
		return new SimpleIndexIter(recs);
	}

	static class SimpleIndexIter implements IndexIter {
		protected final Record[] values;
		protected int i = -1;

		public SimpleIndexIter(Record... values) {
			this.values = values;
		}

		@Override
		public boolean eof() {
			return i == -1;
		}

		@Override
		public Record curKey() {
			return values[i];
		}

		@Override
		public int keyadr() {
			return 0; // not used
		}

		@Override
		public void next() {
			++i;
			if (i >= values.length)
				i = -1;
		}

		@Override
		public void prev() {
			if (i == -1)
				i = values.length;
			--i;
		}

	}

}
