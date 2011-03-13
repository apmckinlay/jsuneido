/* Copyright 2010 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.database.immudb;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;

import org.junit.Test;

import suneido.language.Pack;

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
		MemRecord mr;
		DbRecord r;

		mr = new MemRecord();
		final int N_SMALL = 18;
		for (int i = 0; i < N_SMALL; ++i)
			mr.add("hello world");
		r = toDbRecord(mr);
		assertThat((char) r.mode(), is('c'));
		for (int i = 0; i < N_SMALL; ++i)
			assertThat((String) r.get(i), is("hello world"));

		mr = new MemRecord();
		final int N_MEDIUM = 4000;
		for (int i = 0; i < N_MEDIUM; ++i)
			mr.add("hello world");
		r = toDbRecord(mr);
		assertThat((char) r.mode(), is('s'));
		for (int i = 0; i < N_MEDIUM; ++i)
			assertThat((String) r.get(i), is("hello world"));

		final int N_LARGE = 5000;
		mr = new MemRecord();
		for (int i = 0; i < N_LARGE; ++i)
			mr.add("hello world");
		r = toDbRecord(mr);
		assertThat((char) r.mode(), is('l'));
		for (int i = 0; i < N_LARGE; ++i)
			assertThat("i " + i, (String) r.get(i), is("hello world"));
	}

	DbRecord toDbRecord(Record mr) {
		return new DbRecord(Pack.pack(mr), 0);
	}

	public static Record record(Object... data) {
		MemRecord r = new MemRecord();
		for (Object d : data)
			if (d instanceof String)
				r.add(d);
			else if (d instanceof Integer)
				r.add((int)(Integer) d);
		return r;
	}

}
