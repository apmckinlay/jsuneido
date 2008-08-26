package suneido.database.server;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static suneido.Util.bufferToString;
import static suneido.Util.stringToBuffer;
import static suneido.database.Database.theDB;

import java.nio.ByteBuffer;

import org.junit.Test;

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
	public void transaction() {
		theDB = new Database(new DestMem(), Mode.CREATE);
		ServerData serverData = new ServerData();
		ByteBuffer result = Command.TRANSACTION.execute(null, null, null, serverData);
		assertEquals("T0\r\n", bufferToString(result));
		result.rewind();
		result = Command.ABORT.execute(result, null, null, serverData);
		assertEquals("OK\r\n", bufferToString(result));
		assertTrue(serverData.isEmpty());
	}
}
