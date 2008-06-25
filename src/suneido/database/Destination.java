package suneido.database;

import java.nio.ByteBuffer;

interface Destination {

	long alloc(int size);
	
	ByteBuffer adr(long offset);
	
	long size();
	
}
