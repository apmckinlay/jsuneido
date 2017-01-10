/* Copyright 2008 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.database.server;

import static java.util.Arrays.asList;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.core.StringStartsWith.startsWith;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static suneido.Suneido.dbpkg;
import static suneido.util.ByteBuffers.bufferToString;
import static suneido.util.ByteBuffers.stringToBuffer;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.internal.matchers.Matches;

import suneido.TheDbms;
import suneido.database.query.Header;
import suneido.database.query.Row;
import suneido.database.server.CommandText.Reader;
import suneido.intfc.database.Record;
import suneido.intfc.database.RecordBuilder;
import suneido.runtime.Ops;
import suneido.runtime.Pack;

public class CommandTextTest {

	public static Matches matches(String regex){
		return new Matches(regex);
	}

	@Before
	public void before() {
		ServerData.threadLocal.set(new ServerData());
		Ops.default_single_quotes = true;
		TheDbms.set(dbpkg.testdb());
	}

	@After
	public void after() {
		Ops.default_single_quotes = false;
		assertTrue(ServerData.forThread().isEmpty());
	}

	@Test
	public void getnum() {
		Reader rdr;
		rdr = new Reader("");
		assertEquals(-1, rdr.getnum('T'));

		rdr = new Reader("  A123  B-4  xyz");
		assertEquals(-1, rdr.getnum('X'));
		assertEquals(0, rdr.pos);
		assertEquals(123, rdr.getnum('A'));
		assertEquals(8, rdr.pos);
		assertEquals(-1, rdr.getnum('X'));
		assertEquals(8, rdr.pos);
		assertEquals(-4, rdr.getnum('B'));

		assertEquals("xyz", rdr.str.substring(rdr.pos));
	}

	@Test
	public void badcmd() {
		String s = bst(CommandText.BADCMD.execute("hello world", null, null));
		assertEquals("ERR bad command: hello world", s);
	}

	@Test
	public void transaction() {
		String s = bst(CommandText.TRANSACTION.execute("read", null, null));
		assertThat(s, matches("T\\d+"));
		s = bst(CommandText.ABORT.execute(s, null, null));
		assertEquals("OK", s);
		assertTrue(ServerData.forThread().isEmpty());

		s = bst(CommandText.TRANSACTION.execute("update", null, null));
		assertThat(s, matches("T\\d+"));
		s = bst(CommandText.COMMIT.execute(s, null, null));
		assertEquals("OK", s);
		assertTrue(ServerData.forThread().isEmpty());
	}

	@Test
	public void cursor() {
		String s = bst(CommandText.CURSOR.execute(null, stringToBuffer("tables"), null));
		assertEquals("C0", s);

		s = bst(CommandText.CLOSE.execute(s, null, null));
		assertEquals("OK", s);
	}

	@Test(expected = RuntimeException.class)
	public void badcursor() {
		CommandText.CURSOR.execute(null,
				stringToBuffer("tables sort totalsize"),
				null);
	}

	@Test
	public void query() {
		assertEquals(7, CommandText.QUERY.extra("T0 Q7"));

		String t = bst(CommandText.TRANSACTION.execute("read", null, null));
		assertThat(t, matches("T\\d+"));

		String s = bst(CommandText.QUERY.execute(t + " Q7", stringToBuffer("tables"), null));
		assertEquals("Q0", s);

		s = bst(CommandText.CLOSE.execute(s, null, null));
		assertEquals("OK", s);

		s = bst(CommandText.COMMIT.execute(t, null, null));
		assertEquals("OK", s);
	}

	@Test
	public void header() {
		String s = bst(CommandText.CURSOR.execute(null, stringToBuffer("tables"), null));
		assertEquals("C0", s);

		s = bst(CommandText.HEADER.execute(s, null, null));
		assertThat(s, startsWith("(table,tablename"));
	}

	@Test
	public void order() {
		String t = bst(CommandText.TRANSACTION.execute("read", null, null));
		assertThat(t, matches("T\\d+"));

		String s = bst(CommandText.QUERY.execute(t + " Q25",
				stringToBuffer("columns sort column,table"), null));
		assertEquals("Q0", s);

		s = bst(CommandText.ORDER.execute("Q0", null, null));
		assertEquals("(column,table)", s);

		s = bst(CommandText.COMMIT.execute(t, null, null));
		assertEquals("OK", s);
	}

	@Test
	public void keys() {
		String s = bst(CommandText.CURSOR.execute(null, stringToBuffer("tables"), null));
		assertEquals("C0", s);

		s = bst(CommandText.KEYS.execute("C0", null, null));
		assertEquals("((table),(tablename))", s);

		CommandText.CLOSE.execute("C0", null, null);
	}

	@Test
	public void explain() {
		String s = bst(CommandText.CURSOR.execute(null, stringToBuffer("tables"), null));
		assertEquals("C0", s);

		s = bst(CommandText.EXPLAIN.execute("C0", null, null));
		assertEquals("tables^(table)", s);

		CommandText.CLOSE.execute("C0", null, null);
	}

	@Test
	public void rewind() {
		String s = bst(CommandText.CURSOR.execute(null, stringToBuffer("tables"), null));
		assertEquals("C0", s);

		s = bst(CommandText.REWIND.execute("C0", null, null));
		assertEquals("OK", s);

		CommandText.CLOSE.execute("C0", null, null);
	}

	@Test
	public void get() {
		Output output = new Output();

		String t = bst(CommandText.TRANSACTION.execute("read", null, null));
		assertThat(t, startsWith("T"));

		assertEquals(7, CommandText.QUERY.extra(t + " Q7"));
		String s = bst(CommandText.QUERY.execute(t + " Q7", stringToBuffer("tables"), null));
		assertEquals("Q0", s);

		assertNull(CommandText.GET.execute("+ Q0", null, output));
		assertThat(bst(output.get(0)), matches("A\\d+ R\\d+"));
		Record rec = dbpkg.record(output.get(1));
		assertThat(rec.toString(), startsWith("[1,'tables'"));

		s = bst(CommandText.CLOSE.execute("Q0", null, null));
		assertEquals("OK", s);

		s = bst(CommandText.COMMIT.execute(t, null, null));
		assertEquals("OK", s);
	}

	@Test
	public void get1() {
		Output output = new Output();

		String t = bst(CommandText.TRANSACTION.execute("read", null, null));
		assertThat(t, startsWith("T"));

		String line = "+ " + t + " Q6";
		assertEquals(6, CommandText.GET1.extra(line));
		assertNull(CommandText.GET1.execute(line, stringToBuffer("tables"), output));
		assertThat(bst(output.get(0)),
				matches("A\\d+ R\\d+ \\(table,tablename.*\\)"));
		Record rec = dbpkg.record(output.get(1));
		assertThat(rec.toString(), startsWith("[1,'tables'"));

		String s = bst(CommandText.COMMIT.execute(t, null, null));
		assertEquals("OK", s);
	}

	@Test
	public void output_update_erase() {
		admin("create test (a, b, c) key(a)");

		// OUTPUT
		String t = updateTran();
		String q = query(t);
		output(q, make("a", "b", "c"));
		commit(t);

		// "update"
		t = updateTran();
		String adr = get(t, "['a','b','c']");
		update(t, adr, make("A", "B", "C"));
		get(t, "['A','B','C']");
		commit(t);

		// ERASE
		t = updateTran();
		adr = get(t, "['A','B','C']");
		erase(t, adr);
		geteof(t);
		commit(t);
	}
	private static void admin(String s) {
		CommandText.ADMIN.execute(s, null, null);
	}
	private static String updateTran() {
		String t = bst(CommandText.TRANSACTION.execute("update", null, null));
		assertThat(t, startsWith("T"));
		return t;
	}
	private static String query(String t) {
		String q = bst(CommandText.QUERY.execute(t + " Q5", stringToBuffer("test"), null));
		assertThat(q, startsWith("Q"));
		return q;
	}
	private static void output(String q, Record rec) {
		CommandText.OUTPUT.execute(q + " R" + rec.packSize(), rec.getBuffer(), null);
	}
	private static void commit(String t) {
		String s = bst(CommandText.COMMIT.execute(t, null, null));
		assertEquals("OK", s);
	}
	private static String get(String t, String expected) {
		Output output = new Output();
		assertNull(CommandText.GET1.execute("+ " + t + " Q4", stringToBuffer("test"), output));
		String s = bst(output.get(0));
		assertThat(s, matches("A[-0-9]+ R\\d+ \\(a,b,c\\)"));
		Record rec = dbpkg.record(output.get(1));
		assertThat(rec.toString(), equalTo(expected));
		return s.substring(0, s.indexOf(' '));
	}
	private static void update(String t, String adr, Record rec) {
		String s = bst(CommandText.UPDATE.execute(t + " " + adr + " R" + rec.packSize(),
				rec.getBuffer(), null));
		assertThat(s, startsWith("U"));
	}
	private static void erase(String t, String adr) {
		String s = bst(CommandText.ERASE.execute(t + " " + adr, null, null));
		assertEquals("OK", s);
	}

	private static void geteof(String t) {
		Output output = new Output();
		CommandText.GET1.execute("+ " + t + " Q4", stringToBuffer("test"), output);
		assertEquals("EOF", bst(output.get(0)));
	}

	@Test
	public void libget() {
		Output output = new Output();

		CommandText.ADMIN.execute("create stdlib (name,text,group) key(name,group)",
				null, null);

		String t = updateTran();

		String s = bst(CommandText.QUERY.execute(t + " Q6", stringToBuffer("stdlib"), null));
		assertEquals("Q0", s);

		Record rec = make(-1, "Foo", "some text");
		s = bst(CommandText.OUTPUT.execute("Q0 R" + rec.packSize(), rec.getBuffer(), null));
		assertEquals("t", s);

		rec = make(-1, "Bar", "other stuff");
		s = bst(CommandText.OUTPUT.execute("Q0 R" + rec.packSize(), rec.getBuffer(), null));
		assertEquals("t", s);

		rec = make(1, "Foo", ""); // folder
		s = bst(CommandText.OUTPUT.execute("Q0 R" + rec.packSize(), rec.getBuffer(), null));
		assertEquals("t", s);

		s = bst(CommandText.CLOSE.execute("Q0", null, null));
		assertEquals("OK", s);
		commit(t);

		assertNull(CommandText.LIBGET.execute("Foo", null, output));
		assertEquals("L10", bst(output.get(0)));
		assertEquals("stdlib", bst(output.get(1)));
		assertEquals("" + (char) Pack.Tag.STRING + "some text", bufferToString(output.get(2)));

		assertNull(CommandText.LIBGET.execute("Nil", null, output));
	}

	@Test
	public void libraries() {
		String s = bst(CommandText.LIBRARIES.execute(null, null, null));
		assertEquals("(stdlib)", s);
	}

	@Test
	public void simple_rowToRecord() {
		List<List<String>> flds = asList(asList("a"), asList("a", "b", "c"));
		List<String> cols = asList("a", "b", "me", "c");
		Header hdr = new Header(flds, cols);
		Record rec = dbpkg.recordBuilder().add(123).add("hello").build();
		Row row = new Row(rec);
		assertThat(CommandText.rowToRecord(row, hdr), equalTo(rec));
	}

	@Test
	public void multi_rowToRecord() {
		List<List<String>> flds = asList(asList("a"), asList("a", "b", "c"),
				asList("x"), asList("x", "y", "z"));
		List<String> cols = asList("a", "b", "me", "c", "x", "no", "y", "z");
		Header hdr = new Header(flds, cols);
		Record rec1 = dbpkg.recordBuilder().add(123).build();
		Record rec2 = dbpkg.recordBuilder().add(123).add("hi").build();
		Record rec3 = dbpkg.recordBuilder().add(456).build();
		Record rec4 = dbpkg.recordBuilder().add(456).add("bye").build();
		Record rec = dbpkg.recordBuilder().add(123).add("hi").addMin().add(456).add("bye").build();
		Row row = new Row(rec1, rec2, rec3, rec4);
		assertThat(CommandText.rowToRecord(row, hdr), equalTo(rec));
	}

	// =========================================================================

	public static class Output implements Consumer<ByteBuffer> {
		private final List<ByteBuffer> content = new ArrayList<>();

		@Override
		public void accept(ByteBuffer buf) {
			content.add(buf);
		}

		ByteBuffer get(int i) {
			return content.get(i);
		}
	}

	static Record make(String... args) {
		RecordBuilder r = dbpkg.recordBuilder();
		for (String s : args)
			r.add(s);
		return r.build();
	}

	static Record make(int n, String... args) {
		RecordBuilder r = dbpkg.recordBuilder();
		for (String s : args)
			r.add(s);
		r.add(n);
		return r.build();
	}

	static String bst(ByteBuffer buf) {
		return bufferToString(buf).trim();
	}

}
