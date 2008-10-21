package suneido.database.server;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static suneido.Util.bufferToString;
import static suneido.Util.stringToBuffer;
import static suneido.database.Database.theDB;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.ByteChannel;

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
		ByteBuffer buf = Command.BADCMD.execute(stringToBuffer("hello"), null,
				output, null);
		assertEquals("ERR bad command: hello", output.content
				+ bufferToString(buf));
	}

	@Test
	public void transaction() {
		theDB = new Database(new DestMem(), Mode.CREATE);
		ServerData serverData = new ServerData();

		ByteBuffer buf = Command.TRANSACTION.execute(null, null, null,
				serverData);
		assertEquals("T0\r\n", bufferToString(buf));
		buf.rewind();
		buf = Command.ABORT.execute(buf, null, null, serverData);
		assertEquals("OK\r\n", bufferToString(buf));
		assertTrue(serverData.isEmpty());

		buf = Command.TRANSACTION.execute(null, null, null, serverData);
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

		ByteBuffer tbuf = Command.TRANSACTION.execute(null, null, null,
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

	static private class Output implements OutputQueue {
		public int drainTo(ByteChannel channel) throws IOException {
			return 0;
		}
		public boolean enqueue(ByteBuffer buf) {
			content += bufferToString(buf);
			return true;
		}
		public boolean isEmpty() {
			return false;
		}
		public String content = "";
	}
}
