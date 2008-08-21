package suneido.database.server;

import static org.junit.Assert.assertEquals;
import static suneido.Util.bufferToString;
import static suneido.Util.stringToBuffer;

import java.nio.ByteBuffer;

import org.junit.Test;

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
}
