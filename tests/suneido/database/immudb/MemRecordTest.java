/* Copyright 2010 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.database.immudb;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import org.junit.Test;

public class MemRecordTest {

	@Test
	public void empty() {
		Record r = new MemRecord();
		assertThat(r.size(), is(0));
	}

	@Test
	public void test() {
		MemRecord r = new MemRecord();
		r.add(123);
		assertThat(r.size(), is(1));
		assertThat(r.get(0), is(Integer.class));
		assertThat((Integer) r.get(0), is(123));
		r.add("hello");
		assertThat(r.size(), is(2));
		assertThat(r.get(1), is(String.class));
		assertThat((String) r.get(1), is("hello"));
	}

}
