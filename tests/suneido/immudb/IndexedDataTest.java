/* Copyright 2010 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.immudb;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.Test;
import org.mockito.ArgumentCaptor;

import suneido.immudb.IndexedData.Mode;

public class IndexedDataTest {
	Btree btree1 = mockBtree();
	Btree btree2 = mockBtree();
	Btree btree3 = mockBtree();
	Btree btree4 = mockBtree();
	IndexedData id = new IndexedData(mock(Tran.class))
		.index(btree1, Mode.KEY, 0)
		.index(btree2, Mode.DUPS, 2)
		.index(btree3, Mode.UNIQUE, 1, 2)
		.index(btree4, Mode.UNIQUE, 3);

	@Test
	public void test() {
		id.add(record("a", "b", "c"));

		ArgumentCaptor<Record> arg = ArgumentCaptor.forClass(Record.class);
		verify(btree1).add(arg.capture(), eq(true));
		Record r = arg.getValue();
		assertThat(r.size(), is(2));
		assertThat((String) r.get(0), is("a"));

		verify(btree2).add(arg.capture(), eq(false));
		r = arg.getValue();
		assertThat(r.size(), is(2));
		assertThat((String) r.get(0), is("c"));

		verify(btree3).add(arg.capture(), eq(true));
		r = arg.getValue();
		assertThat(r.size(), is(3));
		assertThat((String) r.get(0), is("b"));
		assertThat((String) r.get(1), is("c"));

		verify(btree4).add(arg.capture(), eq(false));
		r = arg.getValue();
		assertThat(r.size(), is(2));
		assertThat((String) r.get(0), is(""));
	}

	@Test
	public void dup_undo() {
		when(btree3.add(any(Record.class), anyBoolean())).thenReturn(false);
		try {
			id.add(record("a", "b", "c"));
			fail();
		} catch (RuntimeException e) {
			assertTrue(e.toString().contains("duplicate key"));
		}
		verify(btree1).add(any(Record.class), anyBoolean());
		verify(btree2).add(any(Record.class), anyBoolean());
		verify(btree3).add(any(Record.class), anyBoolean());
		verify(btree4, never()).add(any(Record.class), anyBoolean());
		verify(btree1).remove(any(Record.class));
		verify(btree2).remove(any(Record.class));
		verify(btree3, never()).remove(any(Record.class));
		verify(btree4, never()).remove(any(Record.class));
	}

	private static Btree mockBtree() {
		Btree btree1 = mock(Btree.class);
		when(btree1.add(any(Record.class), anyBoolean())).thenReturn(true);
		return btree1;
	}

	private static Record record(Object... values) {
		RecordBuilder rb = new RecordBuilder();
		for (Object x : values)
			rb.add(x);
		return rb.build();
	}

}
