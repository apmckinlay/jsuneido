/* Copyright 2011 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.immudb;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertTrue;
import suneido.intfc.database.IndexIter;

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
			assertTrue("expected eof, got " + iter.curKey(), iter.eof());
		else
			assertThat(iter.curKey().getInt(0), equalTo(n));
	}

	protected static void checkPrev(int[] result, IndexIter iter) {
		int i = result.length - 1;
		for (iter.prev(); ! iter.eof(); iter.prev(), --i)
			assertThat(iter.curKey().getInt(0), equalTo(result[i]));
		assertThat(i, equalTo(-1));
	}

	protected static void checkNext(int[] result, IndexIter iter) {
		int i = 0;
		for (iter.next(); ! iter.eof(); iter.next(), ++i)
			assertThat(iter.curKey().getInt(0), equalTo(result[i]));
		assertThat(i, equalTo(result.length));
	}

	protected static int[] a(int... a) {
		return a;
	}

	protected static TranIndex.Iter iter(int... ints) {
		Record[] recs = new Record[ints.length];
		for (int i = 0; i < ints.length; ++i)
			recs[i] = new RecordBuilder().add(ints[i]).build();
		return new SimpleIndexIter(recs);
	}

	static class SimpleIndexIter implements TranIndex.Iter {
		protected final Record[] values;
		private boolean rewound = true;
		protected int i = -1;

		public SimpleIndexIter(Record... values) {
			this.values = values;
		}

		@Override
		public boolean eof() {
			return i < 0 || values.length <= i;
		}

		@Override
		public Record curKey() {
			return eof() ? null : values[i];
		}

		@Override
		public int keyadr() {
			return curKey().getInt(0);
		}

		@Override
		public void next() {
			if (rewound) {
				i = -1;
				rewound = false;
			} else if (eof())
				return;
			++i;
		}

		@Override
		public void prev() {
			if (rewound) {
				i = values.length;
				rewound = false;
			} else if (eof())
				return;
			--i;
		}

		@Override
		public void rewind() {
			rewound = true;
			i = -1;
		}

	}

}
