/* Copyright 2011 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.immudb;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;

import java.util.Iterator;

import org.junit.Test;

import suneido.immudb.*;

import com.google.common.base.Strings;

public class StoredRecordIteratorTest {
	Storage stor = new TestStorage(32, 32);

	@Test
	public void test() {
		record(13);
		int first = record(9);
		record(17);
		int last = record(11);
		Iterator<Record> iter = new StoredRecordIterator(stor, first, last);
		assertThat(iter.next().length(), is(9));
		assertThat(iter.next().length(), is(17));
		assertThat(iter.next().length(), is(11));
		assertFalse(iter.hasNext());
	}

	private int record(int len) {
		Record r = new RecordBuilder()
			.add(Strings.repeat("x", len - 5))
			.build();
		assertThat(r.length(), is(len));
		return r.store(stor);
	}

}