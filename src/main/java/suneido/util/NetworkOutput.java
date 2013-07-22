package suneido.util;

import java.nio.ByteBuffer;

public interface NetworkOutput {
	void add(ByteBuffer buf);
	void write();
	void close();
}
