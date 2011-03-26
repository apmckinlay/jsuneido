/* Copyright 2010 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.database.immudb;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import java.nio.ByteBuffer;

import org.junit.Test;

import suneido.language.Pack;

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

	@Test
	public void pack_empty() {
		MemRecord r = new MemRecord();
		ByteBuffer buf = Pack.pack(r);
		DbRecord r2 = new DbRecord(buf, 0);
		assertThat(r2, is((Record) r));
	}

	@Test
	public void pack_with_data() {
		MemRecord r = new MemRecord();
		r.add(123).add("hello");
		ByteBuffer buf = Pack.pack(r);
		DbRecord r2 = new DbRecord(buf, 0);
		assertThat(r2.get(0), is((Object) 123));
		assertThat(r2.get(1), is((Object) "hello"));
		assertThat(r2, is((Record) r));
	}

	@Test
	public void length() {
		assertThat(MemRecord.length(0, 0), is(5));
		assertThat(MemRecord.length(1, 1), is(7));
		assertThat(MemRecord.length(1, 200), is(206));
		assertThat(MemRecord.length(1, 249), is(255));

		assertThat(MemRecord.length(1, 250), is(258));
		assertThat(MemRecord.length(1, 300), is(308));

		assertThat(MemRecord.length(1, 0x10000), is(0x1000c));
	}

}
