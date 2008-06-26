package suneido.database;

import java.nio.ByteBuffer;

interface Destination {

	long alloc(int size);
	
	ByteBuffer adr(long offset);
	
	long size();
	
	TranRead read_act(int tran, int tblnum, String index);

	boolean visible(int tran, long adr);

}
