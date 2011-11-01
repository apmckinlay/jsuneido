package suneido.database.server;

import static java.util.Arrays.asList;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.startsWith;
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
import org.mockito.internal.matchers.Matches;

import suneido.TheDbms;
import suneido.database.query.Header;
import suneido.database.query.Row;
import suneido.intfc.database.Record;
import suneido.intfc.database.RecordBuilder;
import suneido.language.Ops;
import suneido.language.Pack;
import suneido.util.NetworkOutput;

public class CommandTest {

	public static Matches matches(String regex){
		return new Matches(regex);
	}

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
		assertThat(bufferToString(buf), matches("T\\d+\r\n"));
		buf.rewind();
		buf = Command.ABORT.execute(buf, null, null);
		assertEquals("OK\r\n", bufferToString(buf));
		assertTrue(ServerData.forThread().isEmpty());

		buf = Command.TRANSACTION.execute(UPDATE, null, null);
		assertThat(bufferToString(buf), matches("T\\d+\r\n"));
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

	@Test(expected = RuntimeException.class)
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
		String t = bufferToString(tbuf).trim();
		assertThat(t, matches("T\\d+"));

		ByteBuffer buf = Command.QUERY.execute(stringToBuffer(t + " Q7"),
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
		assertThat(bufferToString(buf), startsWith("(table,tablename"));
	}

	@Test
	public void order() {
		ByteBuffer tbuf = Command.TRANSACTION.execute(READ, null, null);
		String t = bufferToString(tbuf).trim();
		assertThat(t, matches("T\\d+"));

		ByteBuffer buf = Command.QUERY.execute(stringToBuffer(t + " Q27"),
				stringToBuffer("indexes sort columns,fktable"), null);
		assertEquals("Q0\r\n", bufferToString(buf));

		buf.rewind();
		buf = Command.ORDER.execute(buf, null, null);
		assertEquals("(columns,fktable)\r\n", bufferToString(buf));
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
		String t = bufferToString(tbuf).trim();
		assertThat(t, startsWith("T"));

		assertEquals(7, Command.QUERY.extra(stringToBuffer(t + " Q7")));
		ByteBuffer buf = Command.QUERY.execute(stringToBuffer(t + " Q7"),
				stringToBuffer("tables"), null);
		assertEquals("Q0\r\n", bufferToString(buf));

		buf = Command.GET.execute(stringToBuffer("+ Q0"), null, output);
		assertNull(buf);
		assertThat(bufferToString(output.get(0)), matches("A\\d+ R\\d+\r\n"));
		Record rec = dbpkg.record(output.get(1));
		assertThat(rec.toString(), startsWith("[1,'tables'"));

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
		String t = bufferToString(tbuf).trim();
		assertThat(t, startsWith("T"));

		ByteBuffer line = stringToBuffer("+ " + t + " Q6");
		assertEquals(6, Command.GET1.extra(line));
		line.rewind();
		ByteBuffer buf = Command.GET1.execute(line,
				stringToBuffer("tables"),
				output);
		assertNull(buf);
		assertThat(bufferToString(output.get(0)),
				matches("A\\d+ R\\d+ \\(table,tablename.*\\)\r\n"));
		Record rec = dbpkg.record(output.get(1));
		assertThat(rec.toString(), startsWith("[1,'tables'"));

		tbuf.rewind();
		buf = Command.COMMIT.execute(tbuf, null, null);
		assertEquals("OK\r\n", bufferToString(buf));
		assertTrue(serverData.isEmpty());
	}

	@Test
	public void output_update_erase() {
		admin("create test (a, b, c) key(a)");

		// OUTPUT
		String t = updateTran();
		String q = query(t);
		output(q, make("a", "b", "c"));
		commit(t);

		// UPDATE
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
		Command.ADMIN.execute(stringToBuffer(s),
				null, null);
	}
	private static String updateTran() {
		ByteBuffer buf = Command.TRANSACTION.execute(UPDATE, null, null);
		String t = bufferToString(buf).trim();
		assertThat(t, startsWith("T"));
		return t;
	}
	private static String query(String t) {
		ByteBuffer buf = Command.QUERY.execute(stringToBuffer(t + " Q5"),
				stringToBuffer("test"), null);
		String q = bufferToString(buf).trim();
		assertThat(q, startsWith("Q"));
		return q;
	}
	private static void output(String q, Record rec) {
		Command.OUTPUT.execute(stringToBuffer(q + " R" + rec.packSize()),
				rec.getBuffer(), null);
	}
	private static void commit(String t) {
		ByteBuffer buf = Command.COMMIT.execute(stringToBuffer(t + "\r\n"), null, null);
		assertEquals("OK\r\n", bufferToString(buf));
	}
	private static String get(String t, String expected) {
		Output output = new Output();
		ByteBuffer buf = Command.GET1.execute(stringToBuffer("+ " + t + " Q4"),
				stringToBuffer("test"), output);
		assertNull(buf);
		String s = bufferToString(output.get(0));
		assertThat(s, matches("A[-0-9]+ R\\d+ \\(a,b,c\\)\r\n"));
		Record rec = dbpkg.record(output.get(1));
		assertThat(rec.toString(), is(expected));
		return s.substring(0, s.indexOf(' '));
	}
	private static void update(String t, String adr, Record rec) {
		ByteBuffer buf = Command.UPDATE.execute(
				stringToBuffer(t + " " + adr + " R" + rec.packSize()),
				rec.getBuffer(), null);
		assertThat(bufferToString(buf), startsWith("U"));
	}
	private static void erase(String t, String adr) {
		ByteBuffer buf = Command.ERASE.execute(stringToBuffer(t + " " + adr), null,
				null);
		assertEquals("OK\r\n", bufferToString(buf));
	}

	private static void geteof(String t) {
		Output output = new Output();
		Command.GET1.execute(stringToBuffer("+ " + t + " Q4"),
				stringToBuffer("test"), output);
		assertEquals("EOF\r\n", bufferToString(output.get(0)));
	}

	@Test
	public void libget() {
		final ServerData serverData = new ServerData();
		Output output = new Output();

		Command.ADMIN.execute(
				stringToBuffer("create stdlib (name,text,group) key(name,group)"),
				null, null);

		String t = updateTran();

		ByteBuffer buf = Command.QUERY.execute(stringToBuffer(t + " Q6"),
				stringToBuffer("stdlib"), null);
		assertEquals("Q0\r\n", bufferToString(buf));

		Record rec = make(-1, "Foo", "some text");
		buf = Command.OUTPUT.execute(
				stringToBuffer("Q0 R" + rec.packSize()), rec.getBuffer(), null);
		assertEquals("t\r\n", bufferToString(buf));

		rec = make(-1, "Bar", "other stuff");
		buf = Command.OUTPUT.execute(
				stringToBuffer("Q0 R" + rec.packSize()), rec.getBuffer(), null);
		assertEquals("t\r\n", bufferToString(buf));

		rec = make(1, "Foo", ""); // folder
		buf = Command.OUTPUT.execute(
				stringToBuffer("Q0 R" + rec.packSize()), rec.getBuffer(), null);
		assertEquals("t\r\n", bufferToString(buf));

		buf = Command.CLOSE.execute(stringToBuffer("Q0"), null, null);
		assertEquals("OK\r\n", bufferToString(buf));
		commit(t);
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
