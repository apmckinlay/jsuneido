package suneido.database;

import java.nio.ByteBuffer;

public class DBServer {
	enum Cmd {
		TEXT, BINARY
	}
	public static int extra(ByteBuffer buf) {
//		Cmd cmd = Cmd.valueOf(firstWord(buf));
//		if (cmd.lineOnly())
			return 0;
	}
}
