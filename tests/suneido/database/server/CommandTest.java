package suneido.database.server;

import static org.junit.Assert.*;
import static suneido.database.Database.theDB;
import static suneido.util.Util.bufferToString;
import static suneido.util.Util.stringToBuffer;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.ByteChannel;
import java.util.ArrayList;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.ronsoft.nioserver.OutputQueue;

import suneido.SuException;
import suneido.database.*;
import suneido.language.Pack;

public class CommandTest {

	@Before
	public void clearServerData() {
		ServerData.threadLocal.set(new ServerData());
	}

	@Test
	public void getnum() {
		ByteBuffer buf;
		buf = stringToBuffer("");
		assertEquals(-1, Command.getnum('T', buf));

		buf = stringToBuffer("  A123  B4  xyz");
		assertEquals(-1, Command.getnum('X', buf));
		assertEquals(0, buf.position());
		assertEquals(123, Command.getnum('A', buf));
		assertEquals(8, buf.position());
		assertEquals(-1, Command.getnum('X', buf));
		assertEquals(8, buf.position());
		assertEquals(4, Command.getnum('B', buf));

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
		theDB = new Database(new DestMem(), Mode.CREATE);

		ByteBuffer buf = Command.TRANSACTION.execute(READ, null, null);
		assertEquals("T0\r\n", bufferToString(buf));
		buf.rewind();
		buf = Command.ABORT.execute(buf, null, null);
		assertEquals("OK\r\n", bufferToString(buf));
		assertTrue(ServerData.forThread().isEmpty());

		buf = Command.TRANSACTION.execute(UPDATE, null, null);
		assertEquals("T1\r\n", bufferToString(buf));
		buf.rewind();
		buf = Command.COMMIT.execute(buf, null, null);
		assertEquals("OK\r\n", bufferToString(buf));
		assertTrue(ServerData.forThread().isEmpty());
	}

	@Test
	public void cursor() {
		theDB = new Database(new DestMem(), Mode.CREATE);
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
		theDB = new Database(new DestMem(), Mode.CREATE);

		Command.CURSOR.execute(null,
				stringToBuffer("tables sort totalsize"),
				null);
	}

	@Test
	public void query() {
		theDB = new Database(new DestMem(), Mode.CREATE);
		ServerData serverData = new ServerData();

		assertEquals(7, Command.QUERY.extra(stringToBuffer("T0 Q7")));

		ByteBuffer tbuf = Command.TRANSACTION.execute(READ, null, null);
		assertEquals("T0\r\n", bufferToString(tbuf));

		ByteBuffer buf = Command.QUERY.execute(stringToBuffer("T0 Q7"),
				stringToBuffer("tables"), null);
		assertEquals("Q1\r\n", bufferToString(buf));

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
		theDB = new Database(new DestMem(), Mode.CREATE);

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
		theDB = new Database(new DestMem(), Mode.CREATE);

		ByteBuffer tbuf = Command.TRANSACTION.execute(READ, null, null);
		assertEquals("T0\r\n", bufferToString(tbuf));

		ByteBuffer buf = Command.QUERY.execute(stringToBuffer("T0 Q27"),
				stringToBuffer("tables sort nrows,totalsize"), null);
		assertEquals("Q1\r\n", bufferToString(buf));

		buf.rewind();
		buf = Command.ORDER.execute(buf, null, null);
		assertEquals("(nrows,totalsize)\r\n", bufferToString(buf));
	}

	@Test
	public void keys() {
		theDB = new Database(new DestMem(), Mode.CREATE);

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
		theDB = new Database(new DestMem(), Mode.CREATE);

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
		theDB = new Database(new DestMem(), Mode.CREATE);

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
		theDB = new Database(new DestMem(), Mode.CREATE);
		ServerData serverData = new ServerData();
		Output output = new Output();

		ByteBuffer tbuf = Command.TRANSACTION.execute(READ, null, null);
		assertEquals("T0\r\n", bufferToString(tbuf));

		assertEquals(7, Command.QUERY.extra(stringToBuffer("T0 Q7")));
		ByteBuffer buf = Command.QUERY.execute(stringToBuffer("T0 Q7"),
				stringToBuffer("tables"), null);
		assertEquals("Q1\r\n", bufferToString(buf));

		buf = Command.GET.execute(stringToBuffer("+ Q1"), null, output);
		assertNull(buf);
		assertEquals("A5 R33\r\n", bufferToString(output.get(0)));
		Record rec = new Record(output.get(1));
		assertEquals("[1,'tables',5,4,164]", rec.toString());
		assertEquals(33, rec.bufSize());

		buf = Command.CLOSE.execute(stringToBuffer("Q1"), null, null);
		assertEquals("OK\r\n", bufferToString(buf));

		tbuf.rewind();
		buf = Command.COMMIT.execute(tbuf, null, null);
		assertEquals("OK\r\n", bufferToString(buf));
		assertTrue(serverData.isEmpty());
	}

	@Test
	public void get1() {
		theDB = new Database(new DestMem(), Mode.CREATE);
		ServerData serverData = new ServerData();
		Output output = new Output();

		ByteBuffer tbuf = Command.TRANSACTION.execute(READ, null, null);
		assertEquals("T0\r\n", bufferToString(tbuf));

		ByteBuffer line = stringToBuffer("+ T0 Q6");

		assertEquals(6, Command.GET1.extra(line));
		line.rewind();
		ByteBuffer buf = Command.GET1.execute(line,
				stringToBuffer("tables"),
				output);
		assertNull(buf);
		assertEquals("A5 R33 (table,tablename,nextfield,nrows,totalsize)\r\n",
				bufferToString(output.get(0)));
		Record rec = new Record(output.get(1));
		assertEquals("[1,'tables',5,4,164]", rec.toString());

		tbuf.rewind();
		buf = Command.COMMIT.execute(tbuf, null, null);
		assertEquals("OK\r\n", bufferToString(buf));
		assertTrue(serverData.isEmpty());
	}

	@Test
	public void output_update_erase() {
		theDB = new Database(new DestMem(), Mode.CREATE);
		final ServerData serverData = new ServerData();
		Output output = new Output();

		Command.ADMIN.execute(stringToBuffer("create test (a, b, c) key(a)"),
				null, null);

		ByteBuffer tbuf = Command.TRANSACTION.execute(UPDATE, null, null);
		assertEquals("T0\r\n", bufferToString(tbuf));

		ByteBuffer buf = Command.QUERY.execute(stringToBuffer("T0 Q5"),
				stringToBuffer("test"), null);
		assertEquals("Q1\r\n", bufferToString(buf));

		// OUTPUT
		Record rec = RecordTest.make("a", "b", "c");
		assertEquals(14, rec.packSize());
		Command.OUTPUT.execute(stringToBuffer("Q1 R14"), rec.getBuf(), null);

		buf = Command.GET1.execute(stringToBuffer("+ T0 Q5"),
				stringToBuffer("test"), output);
		assertNull(buf);
		assertEquals("A105 R14 (a,b,c)\r\n",
				bufferToString(output.get(0)));
		assertEquals("['a','b','c']", rec.toString());

		// UPDATE
		rec = RecordTest.make("A", "B", "C");
		buf = Command.UPDATE.execute(stringToBuffer("T0 A105 R14"),
				rec.getBuf(), null);
		assertEquals("U107\r\n", bufferToString(buf));

		buf = Command.REWIND.execute(stringToBuffer("Q1"), null, null);
		output = new Output();
		buf = Command.GET1.execute(stringToBuffer("+ T0 Q5"),
				stringToBuffer("test"), output);
		assertNull(buf);
		assertEquals("A107 R14 (a,b,c)\r\n",
				bufferToString(output.get(0)));
		assertEquals("['A','B','C']", rec.toString());

		// ERASE
		buf = Command.ERASE.execute(stringToBuffer("T0 A107"), rec.getBuf(),
				null);
		assertEquals("OK\r\n", bufferToString(buf));
		output = new Output();
		buf = Command.GET1.execute(stringToBuffer("+ T0 Q4"),
				stringToBuffer("test"), output);
		assertNull(buf);
		assertEquals("EOF\r\n", bufferToString(output.get(0)));

		buf = Command.CLOSE.execute(stringToBuffer("Q1"), null, null);
		assertEquals("OK\r\n", bufferToString(buf));

		tbuf.rewind();
		buf = Command.COMMIT.execute(tbuf, null, null);
		assertEquals("OK\r\n", bufferToString(buf));
		assertTrue(serverData.isEmpty());
	}

	@Test
	public void libget() {
		theDB = new Database(new DestMem(), Mode.CREATE);
		final ServerData serverData = new ServerData();
		Output output = new Output();

		Command.ADMIN.execute(
				stringToBuffer("create stdlib (name,text,group) key(name,group)"),
				null, null);

		ByteBuffer tbuf = Command.TRANSACTION.execute(UPDATE, null, null);
		assertEquals("T0\r\n", bufferToString(tbuf));

		ByteBuffer buf = Command.QUERY.execute(stringToBuffer("T0 Q6"),
				stringToBuffer("stdlib"), null);
		assertEquals("Q1\r\n", bufferToString(buf));

		Record rec = RecordTest.make("Foo", "some text");
		rec.add(-1);
		assertEquals(26, rec.packSize());
		buf = Command.OUTPUT.execute(stringToBuffer("Q1 R26"), rec.getBuf(),
				null);
		assertEquals("t\r\n", bufferToString(buf));

		rec = RecordTest.make("Bar", "other stuff");
		rec.add(-1);
		assertEquals(28, rec.packSize());
		buf = Command.OUTPUT.execute(stringToBuffer("Q1 R28"), rec.getBuf(),
				null);
		assertEquals("t\r\n", bufferToString(buf));

		rec = RecordTest.make("Foo", "");
		rec.add(1); // folder
		assertEquals(16, rec.packSize());
		buf = Command.OUTPUT.execute(stringToBuffer("Q1 R16"), rec.getBuf(),
				null);
		assertEquals("t\r\n", bufferToString(buf));

		buf = Command.CLOSE.execute(stringToBuffer("Q1"), null, null);
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

	// ===============================================================

	static private class Output implements OutputQueue {
		private final List<ByteBuffer> content = new ArrayList<ByteBuffer>();

		public int drainTo(ByteChannel channel) throws IOException {
			return 0;
		}
		public boolean enqueue(ByteBuffer buf) {
			content.add(buf);
			return true;
		}
		public boolean isEmpty() {
			return false;
		}
		public ByteBuffer get(int i) {
			return content.get(i);
		}
	}

	static private ByteBuffer READ = stringToBuffer("read");
	static private ByteBuffer UPDATE = stringToBuffer("update");

}
