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
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.*;

import org.junit.Test;
import org.mockito.ArgumentCaptor;

import suneido.immudb.IndexedData.Mode;

public class IndexedDataTest {
	Btree btree1 = mockBtree();
	Btree btree2 = mockBtree();
	Btree btree3 = mockBtree();
	Btree btree4 = mockBtree();
	RuntimeException e = new RuntimeException("aborted");
	UpdateTransaction t = mock(UpdateTransaction.class);
	{
		Tran tran = mock(Tran.class);
		when(t.tran()).thenReturn(tran);
		doThrow(e).when(t).abortThrow(anyString());
	}
	IndexedData id = new IndexedData(t)
		.index(btree1, Mode.KEY, 0)
		.index(btree2, Mode.DUPS, 2)
		.index(btree3, Mode.UNIQUE, 1, 2)
		.index(btree4, Mode.UNIQUE, 3);

	@Test
	public void test() {
		id.add(record("a", "b", "c"));

		ArgumentCaptor<BtreeKey> arg = ArgumentCaptor.forClass(BtreeKey.class);
		verify(btree1).add(arg.capture(), eq(true));
		assertThat(arg.getValue().key, is(record("a")));

		verify(btree2).add(arg.capture(), eq(false));
		assertThat(arg.getValue().key, is(record("c")));

		verify(btree3).add(arg.capture(), eq(true));
		assertThat(arg.getValue().key, is(record("b", "c")));

		verify(btree4).add(arg.capture(), eq(false));
		assertThat(arg.getValue().key, is(record("")));
	}

	@Test
	public void dup_undo() {
		when(btree3.add(any(BtreeKey.class), anyBoolean())).thenReturn(false);
		when(btree1.remove(any(BtreeKey.class))).thenReturn(true);
		when(btree2.remove(any(BtreeKey.class))).thenReturn(true);
		try {
			id.add(record("a", "b", "c"));
			fail();
		} catch (RuntimeException e) {
			assertTrue(e.toString(), e.toString().contains("duplicate key"));
		}
		verify(btree1).add(any(BtreeKey.class), anyBoolean());
		verify(btree2).add(any(BtreeKey.class), anyBoolean());
		verify(btree3).add(any(BtreeKey.class), anyBoolean());
		verify(btree4, never()).add(any(BtreeKey.class), anyBoolean());
		verify(btree1).remove(any(BtreeKey.class));
		verify(btree2).remove(any(BtreeKey.class));
		verify(btree3, never()).remove(any(BtreeKey.class));
		verify(btree4, never()).remove(any(BtreeKey.class));
	}

	private static Btree mockBtree() {
		Btree btree1 = mock(Btree.class);
		when(btree1.add(any(BtreeKey.class), anyBoolean())).thenReturn(true);
		return btree1;
	}

	private static Record record(Object... values) {
		RecordBuilder rb = new RecordBuilder();
		for (Object x : values)
			rb.add(x);
		DataRecord rec = rb.build();
		rec.address(1);
		return rec;
	}

}
