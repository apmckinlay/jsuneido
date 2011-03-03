/* Copyright 2010 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.database.immudb;

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
	public void types() {
		RecordBuilder rb;
		Record r;

		rb = new RecordBuilder();
		final int N_SMALL = 18;
		for (int i = 0; i < N_SMALL; ++i)
			rb.add("hello world");
		r = rb.build();
		assertThat((char) r.mode(), is('c'));
		for (int i = 0; i < N_SMALL; ++i)
			assertThat((String) r.get(i), is("hello world"));

		rb = new RecordBuilder();
		final int N_MEDIUM = 4000;
		for (int i = 0; i < N_MEDIUM; ++i)
			rb.add("hello world");
		r = rb.build();
		assertThat((char) r.mode(), is('s'));
		for (int i = 0; i < N_MEDIUM; ++i)
			assertThat((String) r.get(i), is("hello world"));

		final int N_LARGE = 5000;
		rb = new RecordBuilder();
		for (int i = 0; i < N_LARGE; ++i)
			rb.add("hello world");
		r = rb.build();
		assertThat((char) r.mode(), is('l'));
		for (int i = 0; i < N_LARGE; ++i)
			assertThat("i " + i, (String) r.get(i), is("hello world"));
	}

	@Test
	public void builder() {
		Record r1 = new RecordBuilder().add("one").add(123).build();
		Record r2 = new RecordBuilder().add(r1).build();
		assertThat(r2, is(r1));
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
