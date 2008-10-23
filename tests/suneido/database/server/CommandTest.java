package suneido.database.server;

import static org.junit.Assert.*;
import static suneido.Util.bufferToString;
import static suneido.Util.stringToBuffer;
import static suneido.database.Database.theDB;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.ByteChannel;
import java.util.ArrayList;
import java.util.List;

import org.junit.Test;
import org.ronsoft.nioserver.OutputQueue;

import suneido.database.*;

public class CommandTest {
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
		ByteBuffer buf = Command.BADCMD.execute(hello, null, output, null);
		assertEquals("ERR bad command: ", bufferToString(output.content.get(0)));
		assertEquals(hello, buf);
	}

	@Test
	public void transaction() {
		theDB = new Database(new DestMem(), Mode.CREATE);
		ServerData serverData = new ServerData();

		ByteBuffer buf = Command.TRANSACTION.execute(READ, null, null,
				serverData);
		assertEquals("T0\r\n", bufferToString(buf));
		buf.rewind();
		buf = Command.ABORT.execute(buf, null, null, serverData);
		assertEquals("OK\r\n", bufferToString(buf));
		assertTrue(serverData.isEmpty());

		buf = Command.TRANSACTION.execute(UPDATE, null, null, serverData);
		assertEquals("T1\r\n", bufferToString(buf));
		buf.rewind();
		buf = Command.COMMIT.execute(buf, null, null, serverData);
		assertEquals("OK\r\n", bufferToString(buf));
		assertTrue(serverData.isEmpty());
	}

	@Test
	public void cursor() {
		theDB = new Database(new DestMem(), Mode.CREATE);
		ServerData serverData = new ServerData();

		ByteBuffer buf = Command.CURSOR.execute(null, stringToBuffer("tables"),
				null, serverData);
		assertEquals("C0\r\n", bufferToString(buf));

		buf.rewind();
		buf = Command.CLOSE.execute(buf, null, null, serverData);
		assertEquals("OK\r\n", bufferToString(buf));
		assertTrue(serverData.isEmpty());
	}

	@Test
	public void query() {
		theDB = new Database(new DestMem(), Mode.CREATE);
		ServerData serverData = new ServerData();

		assertEquals(7, Command.QUERY.extra(stringToBuffer("T0 Q7")));

		ByteBuffer tbuf = Command.TRANSACTION.execute(READ, null, null,
				serverData);
		assertEquals("T0\r\n", bufferToString(tbuf));

		ByteBuffer buf = Command.QUERY.execute(stringToBuffer("T0 Q7"),
				stringToBuffer("tables"), null, serverData);
		assertEquals("Q1\r\n", bufferToString(buf));

		buf.rewind();
		buf = Command.CLOSE.execute(buf, null, null, serverData);
		assertEquals("OK\r\n", bufferToString(buf));
		tbuf.rewind();
		buf = Command.COMMIT.execute(tbuf, null, null, serverData);
		assertEquals("OK\r\n", bufferToString(buf));
		assertTrue(serverData.isEmpty());
	}

	@Test
	public void header() {
		theDB = new Database(new DestMem(), Mode.CREATE);
		ServerData serverData = new ServerData();

		ByteBuffer buf = Command.CURSOR.execute(null, stringToBuffer("tables"),
				null, serverData);
		assertEquals("C0\r\n", bufferToString(buf));

		buf.rewind();
		buf = Command.HEADER.execute(buf, null, null, serverData);
		assertEquals("(table,tablename,nextfield,nrows,totalsize)\r\n",
				bufferToString(buf));
	}

	@Test
	public void order() {
		theDB = new Database(new DestMem(), Mode.CREATE);
		ServerData serverData = new ServerData();

		ByteBuffer buf = Command.CURSOR.execute(null,
				stringToBuffer("tables sort nrows,totalsize"), null, serverData);
		assertEquals("C0\r\n", bufferToString(buf));

		buf.rewind();
		buf = Command.ORDER.execute(buf, null, null, serverData);
		assertEquals("(nrows,totalsize)\r\n", bufferToString(buf));
	}

	@Test
	public void keys() {
		theDB = new Database(new DestMem(), Mode.CREATE);
		ServerData serverData = new ServerData();

		ByteBuffer buf = Command.CURSOR.execute(null, stringToBuffer("tables"),
				null, serverData);
		assertEquals("C0\r\n", bufferToString(buf));

		buf.rewind();
		buf = Command.KEYS.execute(buf, null, null, serverData);
		assertEquals("([table],[tablename])\r\n", bufferToString(buf));
	}

	@Test
	public void explain() {
		theDB = new Database(new DestMem(), Mode.CREATE);
		ServerData serverData = new ServerData();

		ByteBuffer buf = Command.CURSOR.execute(null, stringToBuffer("tables"),
				null, serverData);
		assertEquals("C0\r\n", bufferToString(buf));

		buf.rewind();
		buf = Command.EXPLAIN.execute(buf, null, null, serverData);
		assertEquals("tables^(table)\r\n", bufferToString(buf));
	}

	@Test
	public void rewind() {
		theDB = new Database(new DestMem(), Mode.CREATE);
		ServerData serverData = new ServerData();

		ByteBuffer buf = Command.CURSOR.execute(null, stringToBuffer("tables"),
				null, serverData);
		assertEquals("C0\r\n", bufferToString(buf));

		buf.rewind();
		buf = Command.REWIND.execute(buf, null, null, serverData);
		assertEquals("OK\r\n", bufferToString(buf));
	}

	@Test
	public void get() {
		theDB = new Database(new DestMem(), Mode.CREATE);
		ServerData serverData = new ServerData();
		Output output = new Output();

		assertEquals(7, Command.QUERY.extra(stringToBuffer("T0 Q7")));

		ByteBuffer tbuf = Command.TRANSACTION.execute(READ, null, null,
				serverData);
		assertEquals("T0\r\n", bufferToString(tbuf));

		ByteBuffer buf = Command.QUERY.execute(stringToBuffer("T0 Q7"),
				stringToBuffer("tables"), null, serverData);
		assertEquals("Q1\r\n", bufferToString(buf));

		buf = Command.GET.execute(stringToBuffer("+ Q1"), null, output,
				serverData);
		assertNull(buf);
		assertEquals("A0 R29\r\n", bufferToString(output.content.get(0)));
		Record rec = new Record(output.content.get(1));
		assertEquals("[0,'tables',5,4,164]", rec.toString());

		buf = Command.CLOSE.execute(stringToBuffer("Q1"), null, null,
				serverData);
		assertEquals("OK\r\n", bufferToString(buf));

		tbuf.rewind();
		buf = Command.COMMIT.execute(tbuf, null, null, serverData);
		assertEquals("OK\r\n", bufferToString(buf));
		assertTrue(serverData.isEmpty());
	}

	@Test
	public void get1() {
		theDB = new Database(new DestMem(), Mode.CREATE);
		ServerData serverData = new ServerData();
		Output output = new Output();

		ByteBuffer tbuf = Command.TRANSACTION.execute(READ, null, null,
				serverData);
		assertEquals("T0\r\n", bufferToString(tbuf));

		ByteBuffer line = stringToBuffer("+1 T0 Q7");

		assertEquals(7, Command.GET1.extra(line));

		ByteBuffer buf = Command.GET1.execute(line,
				stringToBuffer("tables where table = 0"), output, serverData);
		assertNull(buf);
		assertEquals("A0 R29 (table,tablename,nextfield,nrows,totalsize)\r\n",
				bufferToString(output.content.get(0)));
		Record rec = new Record(output.content.get(1));
		assertEquals("[0,'tables',5,4,164]", rec.toString());

		tbuf.rewind();
		buf = Command.COMMIT.execute(tbuf, null, null, serverData);
		assertEquals("OK\r\n", bufferToString(buf));
		assertTrue(serverData.isEmpty());
	}

	@Test
	public void output_update_erase() {
		theDB = new Database(new DestMem(), Mode.CREATE);
		ServerData serverData = new ServerData();
		Output output = new Output();

		Command.ADMIN.execute(stringToBuffer("create test (a, b, c) key(a)"),
				null, null, null);

		ByteBuffer tbuf = Command.TRANSACTION.execute(UPDATE, null, null,
				serverData);
		assertEquals("T0\r\n", bufferToString(tbuf));

		ByteBuffer buf = Command.QUERY.execute(stringToBuffer("T0 Q5"),
				stringToBuffer("test"), null, serverData);
		assertEquals("Q1\r\n", bufferToString(buf));

		Record rec = RecordTest.make("a", "b", "c");
		assertEquals(13, rec.packSize());
		Command.OUTPUT.execute(stringToBuffer("Q1 R13"), rec.getBuf(), null,
				serverData);

		buf = Command.CLOSE.execute(stringToBuffer("Q1"), null, null,
				serverData);
		assertEquals("OK\r\n", bufferToString(buf));

		tbuf.rewind();
		buf = Command.COMMIT.execute(tbuf, null, null, serverData);
		assertEquals("OK\r\n", bufferToString(buf));
		assertTrue(serverData.isEmpty());
	}

	// ===============================================================

	static private class Output implements OutputQueue {
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
		public List<ByteBuffer> content = new ArrayList<ByteBuffer>();
	}

	static private ByteBuffer READ = stringToBuffer("read");
	static private ByteBuffer UPDATE = stringToBuffer("update");

}
