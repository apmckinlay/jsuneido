/* Copyright 2010 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.database.immudb;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import org.junit.Test;
import org.mockito.ArgumentCaptor;

public class IndexedDataTest {

	@Test
	public void test() {
		IndexedData id = new IndexedData();

		Btree btree1 = mock(Btree.class);
		Btree btree2 = mock(Btree.class);

		id.index(btree1, 0);
		id.index(btree2, 1, 2);

		Tran tran = mock(Tran.class);
		id.add(tran, record("a", "b", "c"));

		ArgumentCaptor<DbRecord> arg = ArgumentCaptor.forClass(DbRecord.class);
		verify(btree1).add(arg.capture());
		DbRecord r = arg.getValue();
		assertThat(r.size(), is(2));
		assertThat((String) r.get(0), is("a"));

		verify(btree2).add((DbRecord) anyObject());
	}

	private DbRecord record(Object... values) {
		RecordBuilder rb = new RecordBuilder();
		for (Object x : values)
			rb.add(x);
		return rb.build();
	}

}
