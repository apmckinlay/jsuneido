package suneido.database.server;

import static java.util.Arrays.asList;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static suneido.Suneido.dbpkg;
import static suneido.util.Util.bufferToString;
import static suneido.util.Util.stringToBuffer;

import java.nio.ByteBuffer;
import java.util.LinkedList;
import java.util.List;

import org.junit.Before;
import org.junit.Test;

import suneido.SuException;
import suneido.TheDbms;
import suneido.database.query.Header;
import suneido.database.query.Row;
import suneido.intfc.database.Record;
import suneido.intfc.database.RecordBuilder;
import suneido.language.Ops;
import suneido.language.Pack;
import suneido.util.NetworkOutput;

public class CommandTest {

	@Before
	public void clearServerData() {
		ServerData.threadLocal.set(new ServerData());
	}

	@Before
	public void setQuoting() {
		Ops.default_single_quotes = true;
		TheDbms.set(dbpkg.testdb());
	}

	@Test
	public void getnum() {
		ByteBuffer buf;
		buf = stringToBuffer("");
		assertEquals(-1, Command.getnum('T', buf));

		buf = stringToBuffer("  A123  B-4  xyz");
		assertEquals(-1, Command.getnum('X', buf));
		assertEquals(0, buf.position());
		assertEquals(123, Command.getnum('A', buf));
		assertEquals(8, buf.position());
		assertEquals(-1, Command.getnum('X', buf));
		assertEquals(8, buf.position());
		assertEquals(-4, Command.getnum('B', buf));

		assertEquals("xyz", bufferToString(buf.slice()));
	}

	@Test
	public void badcmd() {
		Output output = new Output();
		ByteBuffer hello = stringToBuffer("hello world");
		ByteBuffer buf = Command.BADCMD.execute(hello, null, output);
		assertEquals("ERR bad command: ", bufferToString(output.get(0)));
		assertEquals(hello, buf);
	}

	@Test
	public void transaction() {
		ByteBuffer buf = Command.TRANSACTION.execute(READ, null, null);
		assertEquals("T12\r\n", bufferToString(buf));
		buf.rewind();
		buf = Command.ABORT.execute(buf, null, null);
		assertEquals("OK\r\n", bufferToString(buf));
		assertTrue(ServerData.forThread().isEmpty());

		buf = Command.TRANSACTION.execute(UPDATE, null, null);
		assertEquals("T13\r\n", bufferToString(buf));
		buf.rewind();
		buf = Command.COMMIT.execute(buf, null, null);
		assertEquals("OK\r\n", bufferToString(buf));
		assertTrue(ServerData.forThread().isEmpty());
	}

	@Test
	public void cursor() {
		ServerData serverData = new ServerData();

		ByteBuffer buf = Command.CURSOR.execute(null, stringToBuffer("tables"),
				null);
		assertEquals("C0\r\n", bufferToString(buf));

		buf.rewind();
		buf = Command.CLOSE.execute(buf, null, null);
		assertEquals("OK\r\n", bufferToString(buf));
		assertTrue(serverData.isEmpty());
	}

	@Test(expected = SuException.class)
	public void badcursor() {
		Command.CURSOR.execute(null,
				stringToBuffer("tables sort totalsize"),
				null);
	}

	@Test
	public void query() {
		ServerData serverData = new ServerData();

		assertEquals(7, Command.QUERY.extra(stringToBuffer("T0 Q7")));

		ByteBuffer tbuf = Command.TRANSACTION.execute(READ, null, null);
		assertEquals("T12\r\n", bufferToString(tbuf));

		ByteBuffer buf = Command.QUERY.execute(stringToBuffer("T12 Q7"),
				stringToBuffer("tables"), null);
		assertEquals("Q0\r\n", bufferToString(buf));

		buf.rewind();
		buf = Command.CLOSE.execute(buf, null, null);
		assertEquals("OK\r\n", bufferToString(buf));
		tbuf.rewind();
		buf = Command.COMMIT.execute(tbuf, null, null);
		assertEquals("OK\r\n", bufferToString(buf));
		assertTrue(serverData.isEmpty());
	}

	@Test
	public void header() {
		ByteBuffer buf = Command.CURSOR.execute(null, stringToBuffer("tables"),
				null);
		assertEquals("C0\r\n", bufferToString(buf));

		buf.rewind();
		buf = Command.HEADER.execute(buf, null, null);
		assertEquals("(table,tablename,nextfield,nrows,totalsize)\r\n",
				bufferToString(buf));
	}

	@Test
	public void order() {
		ByteBuffer tbuf = Command.TRANSACTION.execute(READ, null, null);
		assertEquals("T12\r\n", bufferToString(tbuf));

		ByteBuffer buf = Command.QUERY.execute(stringToBuffer("T12 Q27"),
				stringToBuffer("tables sort nrows,totalsize"), null);
		assertEquals("Q0\r\n", bufferToString(buf));

		buf.rewind();
		buf = Command.ORDER.execute(buf, null, null);
		assertEquals("(nrows,totalsize)\r\n", bufferToString(buf));
	}

	@Test
	public void keys() {
		ByteBuffer buf = Command.CURSOR.execute(null, stringToBuffer("tables"),
				null);
		assertEquals("C0\r\n", bufferToString(buf));

		buf.rewind();
		buf = Command.KEYS.execute(buf, null, null);
		assertEquals("((table),(tablename))\r\n", bufferToString(buf));

		Command.CLOSE.execute(stringToBuffer("C0"), null, null);
	}

	@Test
	public void explain() {
		ByteBuffer buf = Command.CURSOR.execute(null, stringToBuffer("tables"),
				null);
		assertEquals("C0\r\n", bufferToString(buf));

		buf.rewind();
		buf = Command.EXPLAIN.execute(buf, null, null);
		assertEquals("tables^(table)\r\n", bufferToString(buf));

		Command.CLOSE.execute(stringToBuffer("C0"), null, null);
	}

	@Test
	public void rewind() {
		ByteBuffer buf = Command.CURSOR.execute(null, stringToBuffer("tables"),
				null);
		assertEquals("C0\r\n", bufferToString(buf));

		buf.rewind();
		buf = Command.REWIND.execute(buf, null, null);
		assertEquals("OK\r\n", bufferToString(buf));

		Command.CLOSE.execute(stringToBuffer("C0"), null, null);
	}

	@Test
	public void get() {
		ServerData serverData = new ServerData();
		Output output = new Output();

		ByteBuffer tbuf = Command.TRANSACTION.execute(READ, null, null);
		assertEquals("T12\r\n", bufferToString(tbuf));

		assertEquals(7, Command.QUERY.extra(stringToBuffer("T12 Q7")));
		ByteBuffer buf = Command.QUERY.execute(stringToBuffer("T12 Q7"),
				stringToBuffer("tables"), null);
		assertEquals("Q0\r\n", bufferToString(buf));

		buf = Command.GET.execute(stringToBuffer("+ Q0"), null, output);
		assertNull(buf);
		assertEquals("A9 R58\r\n", bufferToString(output.get(0)));
		Record rec = dbpkg.record(output.get(1));
		assertEquals("[1,'tables',5,4,224,false]", rec.toString());
		assertEquals(58, rec.bufSize());

		buf = Command.CLOSE.execute(stringToBuffer("Q0"), null, null);
		assertEquals("OK\r\n", bufferToString(buf));

		tbuf.rewind();
		buf = Command.COMMIT.execute(tbuf, null, null);
		assertEquals("OK\r\n", bufferToString(buf));
		assertTrue(serverData.isEmpty());
	}

	@Test
	public void get1() {
		ServerData serverData = new ServerData();
		Output output = new Output();

		ByteBuffer tbuf = Command.TRANSACTION.execute(READ, null, null);
		assertEquals("T12\r\n", bufferToString(tbuf));

		ByteBuffer line = stringToBuffer("+ T12 Q6");

		assertEquals(6, Command.GET1.extra(line));
		line.rewind();
		ByteBuffer buf = Command.GET1.execute(line,
				stringToBuffer("tables"),
				output);
		assertNull(buf);
		assertEquals("A9 R58 (table,tablename,nextfield,nrows,totalsize)\r\n",
				bufferToString(output.get(0)));
		Record rec = dbpkg.record(output.get(1));
		assertEquals("[1,'tables',5,4,224,false]", rec.toString());

		tbuf.rewind();
		buf = Command.COMMIT.execute(tbuf, null, null);
		assertEquals("OK\r\n", bufferToString(buf));
		assertTrue(serverData.isEmpty());
	}

	@Test
	public void output_update_erase() {
		final ServerData serverData = new ServerData();
		Output output = new Output();

		Command.ADMIN.execute(stringToBuffer("create test (a, b, c) key(a)"),
				null, null);

		ByteBuffer tbuf = Command.TRANSACTION.execute(UPDATE, null, null);
		assertEquals("T21\r\n", bufferToString(tbuf));

		ByteBuffer buf = Command.QUERY.execute(stringToBuffer("T21 Q5"),
				stringToBuffer("test"), null);
		assertEquals("Q0\r\n", bufferToString(buf));

		// OUTPUT
		Record rec = make("a", "b", "c");
		assertEquals(14, rec.packSize());
		Command.OUTPUT.execute(stringToBuffer("Q0 R14"), rec.getBuffer(), null);

		buf = Command.GET1.execute(stringToBuffer("+ T21 Q5"),
				stringToBuffer("test"), output);
		assertNull(buf);
		assertEquals("A105 R14 (a,b,c)\r\n",
				bufferToString(output.get(0)));
		assertEquals("['a','b','c']", rec.toString());

		// UPDATE
		rec = make("A", "B", "C");
		buf = Command.UPDATE.execute(stringToBuffer("T21 A105 R14"),
				rec.getBuffer(), null);
		assertEquals("U107\r\n", bufferToString(buf));

		buf = Command.REWIND.execute(stringToBuffer("Q0"), null, null);
		output = new Output();
		buf = Command.GET1.execute(stringToBuffer("+ T21 Q5"),
				stringToBuffer("test"), output);
		assertNull(buf);
		assertEquals("A107 R14 (a,b,c)\r\n",
				bufferToString(output.get(0)));
		assertEquals("['A','B','C']", rec.toString());

		// ERASE
		buf = Command.ERASE.execute(stringToBuffer("T21 A107"), rec.getBuffer(),
				null);
		assertEquals("OK\r\n", bufferToString(buf));
		output = new Output();
		buf = Command.GET1.execute(stringToBuffer("+ T21 Q4"),
				stringToBuffer("test"), output);
		assertNull(buf);
		assertEquals("EOF\r\n", bufferToString(output.get(0)));

		buf = Command.CLOSE.execute(stringToBuffer("Q0"), null, null);
		assertEquals("OK\r\n", bufferToString(buf));

		tbuf.rewind();
		buf = Command.COMMIT.execute(tbuf, null, null);
		assertEquals("OK\r\n", bufferToString(buf));
		assertTrue(serverData.isEmpty());
	}

	@Test
	public void libget() {
		final ServerData serverData = new ServerData();
		Output output = new Output();

		Command.ADMIN.execute(
				stringToBuffer("create stdlib (name,text,group) key(name,group)"),
				null, null);

		ByteBuffer tbuf = Command.TRANSACTION.execute(UPDATE, null, null);
		assertEquals("T21\r\n", bufferToString(tbuf));

		ByteBuffer buf = Command.QUERY.execute(stringToBuffer("T21 Q6"),
				stringToBuffer("stdlib"), null);
		assertEquals("Q0\r\n", bufferToString(buf));

		Record rec = make(-1, "Foo", "some text");
		assertEquals(26, rec.packSize());
		buf = Command.OUTPUT.execute(stringToBuffer("Q0 R26"), rec.getBuffer(),
				null);
		assertEquals("t\r\n", bufferToString(buf));

		rec = make(-1, "Bar", "other stuff");
		assertEquals(28, rec.packSize());
		buf = Command.OUTPUT.execute(stringToBuffer("Q0 R28"), rec.getBuffer(),
				null);
		assertEquals("t\r\n", bufferToString(buf));

		rec = make(1, "Foo", ""); // folder
		assertEquals(16, rec.packSize());
		buf = Command.OUTPUT.execute(stringToBuffer("Q0 R16"), rec.getBuffer(),
				null);
		assertEquals("t\r\n", bufferToString(buf));

		buf = Command.CLOSE.execute(stringToBuffer("Q0"), null, null);
		assertEquals("OK\r\n", bufferToString(buf));
		tbuf.rewind();
		buf = Command.COMMIT.execute(tbuf, null, null);
		assertEquals("OK\r\n", bufferToString(buf));
		assertTrue(serverData.isEmpty());

		buf = Command.LIBGET.execute(stringToBuffer("Foo"), null, output);
		assertEquals(null, buf);
		assertEquals("L10 \r\n", bufferToString(output.get(0)));
		assertEquals("stdlib\r\n", bufferToString(output
				.get(1)));
		assertEquals("" + (char) Pack.Tag.STRING + "some text",
				bufferToString(output.get(2)));

		buf = Command.LIBGET.execute(stringToBuffer("Nil"), null, output);
	}

	@Test
	public void libraries() {
		ByteBuffer buf = Command.LIBRARIES.execute(null, null, null);
		assertEquals("(stdlib)\r\n", bufferToString(buf));
	}

	@Test
	@SuppressWarnings("unchecked")
	public void simple_rowToRecord() {
		List<List<String>> flds = asList(asList("a"), asList("a", "b", "c"));
		List<String> cols = asList("a", "b", "me", "c");
		Header hdr = new Header(flds, cols);
		Record rec = dbpkg.recordBuilder().add(123).add("hello").build();
		Row row = new Row(rec);
		assertThat(Command.rowToRecord(row, hdr), is(rec));
	}

	@Test
	@SuppressWarnings("unchecked")
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
		assertThat(Command.rowToRecord(row, hdr), is(rec));
	}

	// =========================================================================

	public static class Output implements NetworkOutput {

		private final List<ByteBuffer> content = new LinkedList<ByteBuffer>();

		@Override
		public void add(ByteBuffer buf) {
			content.add(buf);
		}

		public ByteBuffer get(int i) {
			return content.get(i);
		}

		@Override
		public void write() {
		}

		@Override
		public void close() {
		}
	}

	private static ByteBuffer READ = stringToBuffer("read");
	private static ByteBuffer UPDATE = stringToBuffer("update");

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

}
