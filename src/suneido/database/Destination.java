package suneido.database;

import java.nio.ByteBuffer;

abstract public class Destination {

	abstract public long alloc(int size);
	
	abstract public ByteBuffer adr(long offset);
	
}
