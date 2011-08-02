/* Copyright 2010 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.immudb;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;

import org.junit.Test;

public class RecordTest {

	@Test
	public void main() {
		Record r = record("one", "two", "three");
		assertEquals(3, r.size());
		assertEquals("one", r.get(0));
		assertEquals("two", r.get(1));
		assertEquals("three", r.get(2));
	}

	@Test
	public void test() {
		Record r = new RecordBuilder().add(123).add("hello").build();
		assertThat(r.size(), is(2));
		assertThat(r.get(0), is(Integer.class));
		assertThat((Integer) r.get(0), is(123));
		assertThat(r.get(1), is(String.class));
		assertThat((String) r.get(1), is("hello"));
	}

	@Test
	public void compare() {
		Record[] data = new Record[] {
				record(), record("one"), record("one", "three"),
				record("one", "two"), record("three"), record("two") };
		for (int i = 0; i < data.length; ++i)
			for (int j = 0; j < data.length; ++j)
				assertEquals("i " + i + " j " + j, Integer.signum(i - j),
						Integer.signum(data[i].compareTo(data[j])));
	}

	@Test
	public void int_pack() {
		Record r = record("one", 9, 0xffff0000);
		assertEquals("one", r.get(0));
		assertEquals(9, r.get(1));
		assertEquals(0xffff0000L, r.get(2));
	}

	@Test
	public void length() {
		assertThat(RecordBuilder.length(0, 0), is(3));
		assertThat(RecordBuilder.length(1, 1), is(5));
		assertThat(RecordBuilder.length(1, 200), is(204));
		assertThat(RecordBuilder.length(1, 250), is(254));

		assertThat(RecordBuilder.length(1, 252), is(258));
		assertThat(RecordBuilder.length(1, 300), is(306));

		assertThat(RecordBuilder.length(1, 0x10000), is(0x1000a));
	}

	@Test
	public void prefixSize() {
		Record rec = record("hi", "world");
		assertThat(rec.prefixSize(0), is(0));
		assertThat(rec.prefixSize(1), is(3));
		assertThat(rec.prefixSize(2), is(9));
	}

	public static Record record(Object... data) {
		RecordBuilder rb = new RecordBuilder();
		for (Object d : data)
			if (d instanceof String)
				rb.add(d);
			else if (d instanceof Integer)
				rb.add((int)(Integer) d);
		return rb.build();
	}

}
